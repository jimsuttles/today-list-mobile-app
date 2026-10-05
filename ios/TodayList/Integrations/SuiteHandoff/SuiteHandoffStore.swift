import Foundation
import ProductivitySuiteCore

struct TodayListSuiteImport: Equatable {
    let title: String
    let notes: String?
    let location: TaskLocation
}

enum SuiteHandoffStore {
    static func todayListImport(from payload: SuiteHandoffPayload) throws -> TodayListSuiteImport {
        guard payload.version == SuiteHandoffConstants.schemaVersion else {
            throw SuiteHandoffError.unsupportedVersion
        }
        guard payload.destinationApp == .todayList else {
            throw SuiteHandoffError.invalidPayload
        }

        let title = payload.title.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !title.isEmpty else {
            throw SuiteHandoffError.invalidPayload
        }

        let destination = payload.metadata["destination"]?.lowercased()
        let location: TaskLocation

        switch payload.sourceApp {
        case .quickCapture:
            location = destination == "later" ? .later : .today

        case .waitingFor:
            guard let sourceFollowUpID = payload.metadata["sourceFollowUpId"],
                  !sourceFollowUpID.isEmpty,
                  destination == "today" || destination == "later" else {
                throw SuiteHandoffError.invalidPayload
            }
            location = destination == "later" ? .later : .today

        case .todayList, .top3, .dailyDecision:
            throw SuiteHandoffError.invalidPayload
        }

        return TodayListSuiteImport(
            title: title,
            notes: payload.notes,
            location: location
        )
    }

    static func top3Payload(for task: TaskItem, createdAt: Date = Date()) -> SuiteHandoffPayload {
        SuiteHandoffPayload(
            id: top3HandoffID(sourceTaskID: task.id),
            sourceApp: .todayList,
            destinationApp: .top3,
            createdAt: createdAt,
            title: task.title,
            notes: task.notes,
            metadata: ["sourceTaskId": task.id]
        )
    }

    static func top3HandoffID(sourceTaskID: String) -> UUID {
        SuiteHandoffID.generate(
            sourceApp: .todayList,
            sourceEntityID: sourceTaskID,
            destinationApp: .top3
        )
    }

    static func waitingForPayload(
        for task: TaskItem,
        handoffID: UUID? = nil,
        createdAt: Date = Date()
    ) -> SuiteHandoffPayload {
        SuiteHandoffPayload(
            id: handoffID ?? waitingForHandoffID(sourceTaskID: task.id),
            sourceApp: .todayList,
            destinationApp: .waitingFor,
            createdAt: createdAt,
            title: task.title,
            notes: task.notes,
            metadata: [
                "sourceTaskId": task.id,
                "sourceLocation": task.location == .today ? "today" : "later",
                "requestedDate": createdAt.ISO8601Format()
            ]
        )
    }

    static func waitingForHandoffID(sourceTaskID: String, attempt: Int = 1) -> UUID {
        let sourceEntityID = attempt == 1
            ? sourceTaskID
            : "\(sourceTaskID)|waitingfor|attempt-\(attempt)"
        return SuiteHandoffID.generate(
            sourceApp: .todayList,
            sourceEntityID: sourceEntityID,
            destinationApp: .waitingFor
        )
    }

    static func waitingForURL(for id: UUID) -> URL {
        do {
            return try SuiteHandoffURL.makeURL(destination: .waitingFor, handoffID: id)
        } catch {
            preconditionFailure("ProductivitySuiteCore must support the Waiting For handoff URL.")
        }
    }

    static func isCompleted(id: UUID) -> Bool {
        (try? SuiteHandoffTransport().isCompleted(handoffID: id)) ?? false
    }

    static func top3URL(for id: UUID) -> URL {
        do {
            return try SuiteHandoffURL.makeURL(destination: .top3, handoffID: id)
        } catch {
            preconditionFailure("ProductivitySuiteCore must support the Top 3 handoff URL.")
        }
    }

    static func writePending(_ payload: SuiteHandoffPayload) throws {
        do {
            try SuiteHandoffTransport().writePending(payload)
        } catch {
            throw appError(for: error)
        }
    }

    static func loadPending(id: UUID) throws -> SuiteHandoffPayload {
        do {
            return try SuiteHandoffTransport().loadPending(handoffID: id)
        } catch {
            throw appError(for: error)
        }
    }

    static func markCompleted(id: UUID) throws {
        do {
            try markCompleted(id: id, transport: SuiteHandoffTransport())
        } catch {
            throw appError(for: error)
        }
    }

    static func writePending(_ payload: SuiteHandoffPayload, containerURL: URL) throws {
        do {
            try SuiteHandoffTransport(containerURL: containerURL).writePending(payload)
        } catch {
            throw appError(for: error)
        }
    }

    static func loadPending(id: UUID, containerURL: URL) throws -> SuiteHandoffPayload {
        do {
            return try SuiteHandoffTransport(containerURL: containerURL).loadPending(handoffID: id)
        } catch {
            throw appError(for: error)
        }
    }

    private static func markCompleted(id: UUID, transport: SuiteHandoffTransport) throws {
        let pendingURL = transport.pendingURL(for: id)
        guard FileManager.default.fileExists(atPath: pendingURL.path) else { return }

        if transport.isCompleted(handoffID: id) {
            try FileManager.default.removeItem(at: pendingURL)
        } else {
            try transport.complete(handoffID: id)
        }
    }

    private static func appError(for error: Error) -> Error {
        guard let coreError = error as? SuiteHandoffCoreError else { return error }

        return switch coreError {
        case .appGroupUnavailable:
            SuiteHandoffError.appGroupUnavailable
        case .alreadyCompleted:
            SuiteHandoffError.alreadyCompleted
        case .invalidPayload:
            SuiteHandoffError.invalidPayload
        case .payloadMissing, .fileOperationFailed, .invalidDestinationScheme:
            coreError
        }
    }
}

enum SuiteHandoffError: LocalizedError {
    case appGroupUnavailable
    case alreadyCompleted
    case invalidPayload
    case unsupportedVersion

    var errorDescription: String? {
        switch self {
        case .appGroupUnavailable:
            "The suite shared container is unavailable."
        case .alreadyCompleted:
            "This task was already promoted to Top 3."
        case .invalidPayload:
            "The handoff payload is not valid for Today List."
        case .unsupportedVersion:
            "This handoff was created by an unsupported suite version."
        }
    }
}
