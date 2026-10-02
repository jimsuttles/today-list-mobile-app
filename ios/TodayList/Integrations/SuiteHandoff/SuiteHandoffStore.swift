import CryptoKit
import Foundation

struct SuiteHandoffPayload: Codable, Identifiable, Equatable {
    let id: UUID
    let version: Int
    let sourceApp: String
    let destinationApp: String
    let createdAt: Date
    let title: String
    let notes: String?
    let metadata: [String: String]
}

enum SuiteHandoffStore {
    static let appGroupIdentifier = "group.com.4ctech.productivitysuite"

    static func top3Payload(for task: TaskItem, createdAt: Date = Date()) -> SuiteHandoffPayload {
        SuiteHandoffPayload(
            id: top3HandoffID(sourceTaskID: task.id),
            version: 1,
            sourceApp: "todaylist",
            destinationApp: "top3",
            createdAt: createdAt,
            title: task.title,
            notes: task.notes,
            metadata: ["sourceTaskId": task.id]
        )
    }

    static func top3HandoffID(sourceTaskID: String) -> UUID {
        let canonicalValue = "todaylist|\(sourceTaskID)|top3"
        var bytes = Array(SHA256.hash(data: Data(canonicalValue.utf8)).prefix(16))
        bytes[6] = (bytes[6] & 0x0F) | 0x50
        bytes[8] = (bytes[8] & 0x3F) | 0x80
        return UUID(uuid: (
            bytes[0], bytes[1], bytes[2], bytes[3],
            bytes[4], bytes[5], bytes[6], bytes[7],
            bytes[8], bytes[9], bytes[10], bytes[11],
            bytes[12], bytes[13], bytes[14], bytes[15]
        ))
    }

    static func top3URL(for id: UUID) -> URL {
        URL(string: "top3://v1/handoff/\(id.uuidString)")!
    }

    static func writePending(_ payload: SuiteHandoffPayload) throws {
        try writePending(payload, containerURL: suiteContainerURL())
    }

    static func loadPending(id: UUID) throws -> SuiteHandoffPayload {
        let url = try pendingDirectory().appendingPathComponent("\(id.uuidString).json")
        let data = try Data(contentsOf: url)
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        return try decoder.decode(SuiteHandoffPayload.self, from: data)
    }

    static func markCompleted(id: UUID) throws {
        let pendingURL = try pendingDirectory().appendingPathComponent("\(id.uuidString).json")
        guard FileManager.default.fileExists(atPath: pendingURL.path) else { return }
        let completedURL = try completedDirectory().appendingPathComponent("\(id.uuidString).json")
        if FileManager.default.fileExists(atPath: completedURL.path) {
            try FileManager.default.removeItem(at: pendingURL)
        } else {
            try FileManager.default.moveItem(at: pendingURL, to: completedURL)
        }
    }

    static func writePending(_ payload: SuiteHandoffPayload, containerURL: URL) throws {
        let completedURL = try directory(named: "Completed", containerURL: containerURL)
            .appendingPathComponent("\(payload.id.uuidString).json")
        guard !FileManager.default.fileExists(atPath: completedURL.path) else {
            throw SuiteHandoffError.alreadyCompleted
        }

        let pendingURL = try directory(named: "Pending", containerURL: containerURL)
            .appendingPathComponent("\(payload.id.uuidString).json")
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        encoder.outputFormatting = [.sortedKeys]
        try encoder.encode(payload).write(to: pendingURL, options: .atomic)
    }

    static func loadPending(id: UUID, containerURL: URL) throws -> SuiteHandoffPayload {
        let url = try directory(named: "Pending", containerURL: containerURL)
            .appendingPathComponent("\(id.uuidString).json")
        let data = try Data(contentsOf: url)
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        return try decoder.decode(SuiteHandoffPayload.self, from: data)
    }

    private static func pendingDirectory() throws -> URL {
        try directory(named: "Pending")
    }

    private static func completedDirectory() throws -> URL {
        try directory(named: "Completed")
    }

    private static func directory(named name: String) throws -> URL {
        try directory(named: name, containerURL: suiteContainerURL())
    }

    private static func suiteContainerURL() throws -> URL {
        guard let containerURL = FileManager.default.containerURL(
            forSecurityApplicationGroupIdentifier: appGroupIdentifier
        ) else {
            throw SuiteHandoffError.appGroupUnavailable
        }
        return containerURL
    }

    private static func directory(named name: String, containerURL: URL) throws -> URL {
        let directory = containerURL
            .appendingPathComponent("Handoffs", isDirectory: true)
            .appendingPathComponent(name, isDirectory: true)
        try FileManager.default.createDirectory(
            at: directory,
            withIntermediateDirectories: true
        )
        return directory
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
