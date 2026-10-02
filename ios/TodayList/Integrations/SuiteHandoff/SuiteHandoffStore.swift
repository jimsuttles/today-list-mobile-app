import Foundation
import ProductivitySuiteCore

enum SuiteHandoffStore {
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
