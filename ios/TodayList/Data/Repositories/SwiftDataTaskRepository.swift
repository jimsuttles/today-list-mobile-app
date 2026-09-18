import Foundation
import SwiftData

@MainActor
final class SwiftDataTaskRepository: TaskRepository {
    private let modelContext: ModelContext
    private let recurrenceEngine: RecurrenceEngine
    private var locationContinuations: [TaskLocation: [UUID: AsyncStream<[TaskItem]>.Continuation]] = [
        .today: [:], .later: [:]
    ]

    init(modelContext: ModelContext, recurrenceEngine: RecurrenceEngine = DefaultRecurrenceEngine()) {
        self.modelContext = modelContext
        self.recurrenceEngine = recurrenceEngine
    }

    func observeTasks(location: TaskLocation) -> AsyncStream<[TaskItem]> {
        AsyncStream { [weak self] continuation in
            guard let self else {
                continuation.finish()
                return
            }
            let id = UUID()
            if self.locationContinuations[location] == nil {
                self.locationContinuations[location] = [:]
            }
            self.locationContinuations[location]?[id] = continuation
            continuation.yield(self.fetchTasks(location: location))
            continuation.onTermination = { _ in
                Task { @MainActor in
                    self.locationContinuations[location]?[id] = nil
                }
            }
        }
    }

    func getTask(id: String) async -> TaskItem? {
        guard let entity = try? fetchTaskEntity(id: id),
              entity.statusRaw != TaskStatus.deleted.rawValue else { return nil }
        return mapTask(entity)
    }

    func tasksScheduled(on date: Date) async -> [TaskItem] {
        let day = CalendarHelpers.startOfDay(date)
        let status = TaskStatus.today.rawValue
        let descriptor = FetchDescriptor<PersistedTask>(
            predicate: #Predicate { $0.statusRaw == status },
            sortBy: [SortDescriptor(\.sortOrder)]
        )
        let entities = (try? modelContext.fetch(descriptor)) ?? []
        return entities
            .map { mapTask($0) }
            .filter { task in
                guard let scheduled = task.scheduledDate else { return false }
                return CalendarHelpers.startOfDay(scheduled) == day
            }
    }

    func createTask(
        title: String,
        notes: String?,
        location: TaskLocation,
        reminderAt: Date?,
        scheduledDate: Date?,
        recurrence: RecurrenceRule?
    ) async throws -> TaskItem {
        let now = Date()
        let today = CalendarHelpers.today()
        let trimmed = title.trimmingCharacters(in: .whitespacesAndNewlines)
        let id = UUID().uuidString
        let status = TaskStatus.from(location)
        let sort = (try maxSortOrder(status: status)) + 1
        var recurrenceId: String?
        if var rule = recurrence {
            if rule.id.isEmpty { rule.id = UUID().uuidString }
            let persisted = PersistedRecurrence.from(rule)
            modelContext.insert(persisted)
            recurrenceId = rule.id
        }
        let scheduled = scheduledDate.map { CalendarHelpers.startOfDay($0) }
            ?? (location == .today ? today : nil)
        let task = PersistedTask(
            id: id,
            title: trimmed,
            notes: notes,
            statusRaw: status.rawValue,
            sortOrder: sort,
            createdAt: now,
            updatedAt: now,
            scheduledDate: scheduled,
            reminderAt: reminderAt,
            recurrenceId: recurrenceId
        )
        modelContext.insert(task)
        let occDate = scheduled ?? today
        modelContext.insert(PersistedOccurrence(
            taskId: id,
            occurrenceDate: occDate,
            movedToLater: location == .later,
            createdAt: now
        ))
        try modelContext.save()
        notify(location)
        return mapTask(task)
    }

    func updateTask(_ task: TaskItem) async throws {
        guard let entity = try fetchTaskEntity(id: task.id) else { return }
        entity.title = task.title.trimmingCharacters(in: .whitespacesAndNewlines)
        entity.notes = task.notes
        entity.updatedAt = Date()
        entity.scheduledDate = task.scheduledDate.map { CalendarHelpers.startOfDay($0) }
        entity.reminderAt = task.reminderAt
        let newStatus = TaskStatus.from(task.location)
        let oldLocation = TaskStatus(rawValue: entity.statusRaw)?.location
        if entity.statusRaw != newStatus.rawValue {
            entity.statusRaw = newStatus.rawValue
            entity.sortOrder = (try maxSortOrder(status: newStatus)) + 1
        }
        if let recurrence = task.recurrence {
            if let existingId = entity.recurrenceId, let existing = try fetchRecurrence(id: existingId) {
                existing.typeRaw = recurrence.type.rawValue
                existing.interval = recurrence.interval
                existing.startDate = CalendarHelpers.startOfDay(recurrence.startDate)
                existing.endDate = recurrence.endDate.map { CalendarHelpers.startOfDay($0) }
                existing.weekdaysCSV = recurrence.weekdays.sorted().map(String.init).joined(separator: ",")
                existing.dayOfMonth = recurrence.dayOfMonth
            } else {
                let persisted = PersistedRecurrence.from(recurrence)
                modelContext.insert(persisted)
                entity.recurrenceId = recurrence.id
            }
        } else if let rid = entity.recurrenceId {
            entity.recurrenceId = nil
            if let existing = try fetchRecurrence(id: rid) {
                modelContext.delete(existing)
            }
        }
        try modelContext.save()
        if let oldLocation { notify(oldLocation) }
        notify(task.location)
    }

    func moveToToday(taskId: String) async throws {
        guard let entity = try fetchTaskEntity(id: taskId) else { return }
        let from = TaskStatus(rawValue: entity.statusRaw)?.location
        entity.statusRaw = TaskStatus.today.rawValue
        entity.sortOrder = (try maxSortOrder(status: .today)) + 1
        entity.scheduledDate = CalendarHelpers.today()
        entity.updatedAt = Date()
        try modelContext.save()
        if let from { notify(from) }
        notify(.today)
    }

    func moveToLater(taskId: String) async throws {
        guard let entity = try fetchTaskEntity(id: taskId) else { return }
        let from = TaskStatus(rawValue: entity.statusRaw)?.location
        entity.statusRaw = TaskStatus.later.rawValue
        entity.sortOrder = (try maxSortOrder(status: .later)) + 1
        entity.updatedAt = Date()
        if let open = try openOccurrence(taskId: taskId) {
            open.movedToLater = true
        }
        try modelContext.save()
        if let from { notify(from) }
        notify(.later)
    }

    func reorderTasks(location: TaskLocation, orderedTaskIds: [String]) async throws {
        let status = TaskStatus.from(location)
        for (index, id) in orderedTaskIds.enumerated() {
            guard let entity = try fetchTaskEntity(id: id),
                  entity.statusRaw == status.rawValue else { continue }
            entity.sortOrder = index
            entity.updatedAt = Date()
        }
        try modelContext.save()
        notify(location)
    }

    func completeTask(taskId: String) async throws -> String? {
        guard let entity = try fetchTaskEntity(id: taskId),
              entity.statusRaw != TaskStatus.deleted.rawValue else { return nil }
        let now = Date()
        let today = CalendarHelpers.today()
        let from = TaskStatus(rawValue: entity.statusRaw)?.location

        var occurrence = try openOccurrence(taskId: taskId)
        if let occurrence {
            occurrence.completedAt = now
        } else {
            occurrence = PersistedOccurrence(
                taskId: taskId,
                occurrenceDate: entity.scheduledDate ?? today,
                completedAt: now,
                createdAt: now
            )
            modelContext.insert(occurrence!)
        }

        let eventId = UUID().uuidString
        let event = PersistedCompletion(
            id: eventId,
            taskId: taskId,
            occurrenceId: occurrence?.id,
            titleSnapshot: entity.title,
            completedAt: now,
            completionDate: today
        )
        modelContext.insert(event)

        if let rid = entity.recurrenceId, let ruleEntity = try fetchRecurrence(id: rid) {
            let rule = ruleEntity.toDomain()
            let after = occurrence?.occurrenceDate ?? today
            if let next = recurrenceEngine.nextOccurrence(rule: rule, after: after) {
                modelContext.insert(PersistedOccurrence(
                    taskId: taskId,
                    occurrenceDate: next,
                    createdAt: now
                ))
                let keepSort = entity.statusRaw == TaskStatus.today.rawValue
                entity.statusRaw = TaskStatus.today.rawValue
                entity.scheduledDate = next
                entity.reminderAt = nil
                entity.updatedAt = now
                if !keepSort {
                    entity.sortOrder = (try maxSortOrder(status: .today)) + 1
                }
            } else {
                entity.statusRaw = TaskStatus.deleted.rawValue
                entity.updatedAt = now
            }
        } else {
            entity.statusRaw = TaskStatus.deleted.rawValue
            entity.updatedAt = now
        }

        try modelContext.save()
        if let from { notify(from) }
        notify(.today)
        notify(.later)
        return eventId
    }

    func uncompleteTask(completionEventId: String, restoreTo: TaskLocation) async throws {
        guard let event = try fetchCompletion(id: completionEventId) else { return }
        defer {
            modelContext.delete(event)
        }
        if let taskId = event.taskId, let entity = try fetchTaskEntity(id: taskId) {
            if let occId = event.occurrenceId, let occ = try fetchOccurrence(id: occId) {
                // Delete open occurrences after completed date
                let after = occ.occurrenceDate
                let opens = try openOccurrences(taskId: taskId).filter { $0.occurrenceDate > after }
                for o in opens { modelContext.delete(o) }
                occ.completedAt = nil
                occ.movedToLater = restoreTo == .later
                entity.scheduledDate = occ.occurrenceDate
            } else if restoreTo == .today {
                entity.scheduledDate = CalendarHelpers.today()
            }
            let status = TaskStatus.from(restoreTo)
            entity.statusRaw = status.rawValue
            entity.sortOrder = (try maxSortOrder(status: status)) + 1
            entity.updatedAt = Date()
        }
        try modelContext.save()
        notify(.today)
        notify(.later)
    }

    func deleteTask(taskId: String, scope: DeleteScope) async throws {
        guard let entity = try fetchTaskEntity(id: taskId) else { return }
        let from = TaskStatus(rawValue: entity.statusRaw)?.location
        let rid = entity.recurrenceId
        modelContext.delete(entity)
        let occs = try occurrences(taskId: taskId)
        for o in occs { modelContext.delete(o) }
        if scope == .entireSeries, let rid, let rule = try fetchRecurrence(id: rid) {
            modelContext.delete(rule)
        }
        try modelContext.save()
        if let from { notify(from) }
    }

    func keepOnTodayForNewDay(taskIds: [String], today: Date) async throws {
        let day = CalendarHelpers.startOfDay(today)
        for id in taskIds {
            guard let entity = try fetchTaskEntity(id: id),
                  entity.statusRaw == TaskStatus.today.rawValue else { continue }
            entity.scheduledDate = day
            entity.updatedAt = Date()
        }
        try modelContext.save()
        notify(.today)
    }

    func deleteAllTasks() async throws {
        try modelContext.delete(model: PersistedTask.self)
        try modelContext.delete(model: PersistedOccurrence.self)
        try modelContext.delete(model: PersistedRecurrence.self)
        try modelContext.delete(model: PersistedCompletion.self)
        try modelContext.save()
        notify(.today)
        notify(.later)
    }

    // MARK: - Private

    private func notify(_ location: TaskLocation) {
        let tasks = fetchTasks(location: location)
        guard let conts = locationContinuations[location] else { return }
        for c in conts.values {
            c.yield(tasks)
        }
    }

    private func fetchTasks(location: TaskLocation) -> [TaskItem] {
        let status = TaskStatus.from(location).rawValue
        let descriptor = FetchDescriptor<PersistedTask>(
            predicate: #Predicate { $0.statusRaw == status },
            sortBy: [SortDescriptor(\.sortOrder)]
        )
        let entities = (try? modelContext.fetch(descriptor)) ?? []
        let mapped = entities.map { mapTask($0) }
        guard location == .today else { return mapped }
        // Recurring completions schedule the next occurrence on Today with a future
        // date; hide those until their day so Today only shows what's due now.
        let today = CalendarHelpers.today()
        return mapped.filter { task in
            guard let scheduled = task.scheduledDate else { return true }
            return CalendarHelpers.startOfDay(scheduled) <= today
        }
    }

    private func mapTask(_ entity: PersistedTask) -> TaskItem {
        let location = TaskStatus(rawValue: entity.statusRaw)?.location ?? .today
        var recurrence: RecurrenceRule?
        if let rid = entity.recurrenceId {
            recurrence = try? fetchRecurrence(id: rid)?.toDomain()
        }
        return TaskItem(
            id: entity.id,
            title: entity.title,
            notes: entity.notes,
            location: location,
            sortOrder: entity.sortOrder,
            createdAt: entity.createdAt,
            updatedAt: entity.updatedAt,
            scheduledDate: entity.scheduledDate,
            reminderAt: entity.reminderAt,
            recurrence: recurrence
        )
    }

    private func maxSortOrder(status: TaskStatus) throws -> Int {
        let raw = status.rawValue
        let descriptor = FetchDescriptor<PersistedTask>(
            predicate: #Predicate { $0.statusRaw == raw },
            sortBy: [SortDescriptor(\.sortOrder, order: .reverse)]
        )
        return try modelContext.fetch(descriptor).first?.sortOrder ?? -1
    }

    private func fetchTaskEntity(id: String) throws -> PersistedTask? {
        let descriptor = FetchDescriptor<PersistedTask>(predicate: #Predicate { $0.id == id })
        return try modelContext.fetch(descriptor).first
    }

    private func fetchRecurrence(id: String) throws -> PersistedRecurrence? {
        let descriptor = FetchDescriptor<PersistedRecurrence>(predicate: #Predicate { $0.id == id })
        return try modelContext.fetch(descriptor).first
    }

    private func fetchOccurrence(id: String) throws -> PersistedOccurrence? {
        let descriptor = FetchDescriptor<PersistedOccurrence>(predicate: #Predicate { $0.id == id })
        return try modelContext.fetch(descriptor).first
    }

    private func fetchCompletion(id: String) throws -> PersistedCompletion? {
        let descriptor = FetchDescriptor<PersistedCompletion>(predicate: #Predicate { $0.id == id })
        return try modelContext.fetch(descriptor).first
    }

    private func openOccurrence(taskId: String) throws -> PersistedOccurrence? {
        try openOccurrences(taskId: taskId).first
    }

    private func openOccurrences(taskId: String) throws -> [PersistedOccurrence] {
        let descriptor = FetchDescriptor<PersistedOccurrence>(
            predicate: #Predicate { $0.taskId == taskId && $0.completedAt == nil },
            sortBy: [SortDescriptor(\.occurrenceDate, order: .reverse)]
        )
        return try modelContext.fetch(descriptor)
    }

    private func occurrences(taskId: String) throws -> [PersistedOccurrence] {
        let descriptor = FetchDescriptor<PersistedOccurrence>(predicate: #Predicate { $0.taskId == taskId })
        return try modelContext.fetch(descriptor)
    }
}
