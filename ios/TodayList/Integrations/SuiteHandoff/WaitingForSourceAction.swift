import Foundation
import ProductivitySuiteCore

enum WaitingForSourceAction {
    case keep
    case markDone
    case remove
}

struct WaitingForPreparedHandoff {
    let payload: SuiteHandoffPayload
    let url: URL
}

struct WaitingForSourceHandoffCoordinator {
    let dispositionStore: WaitingForSourceDispositionStore

    func prepare(
        task: TaskItem,
        createdAt: Date = Date(),
        containerURL: URL? = nil
    ) throws -> WaitingForPreparedHandoff {
        let disposition = dispositionStore.prepareAttempt(
            sourceTaskID: task.id,
            createdAt: createdAt
        )
        let payload = SuiteHandoffStore.waitingForPayload(
            for: task,
            handoffID: disposition.handoffID,
            createdAt: createdAt
        )
        if let containerURL {
            try SuiteHandoffStore.writePending(payload, containerURL: containerURL)
        } else {
            try SuiteHandoffStore.writePending(payload)
        }
        return WaitingForPreparedHandoff(
            payload: payload,
            url: SuiteHandoffStore.waitingForURL(for: payload.id)
        )
    }

    func recordDestinationOpenResult(_ accepted: Bool, handoffID: UUID) {
        guard accepted else { return }
        dispositionStore.markDestinationOpenAccepted(handoffID: handoffID)
    }
}
