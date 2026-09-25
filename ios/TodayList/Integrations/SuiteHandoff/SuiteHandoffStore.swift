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

    private static func pendingDirectory() throws -> URL {
        try directory(named: "Pending")
    }

    private static func completedDirectory() throws -> URL {
        try directory(named: "Completed")
    }

    private static func directory(named name: String) throws -> URL {
        guard let container = FileManager.default.containerURL(
            forSecurityApplicationGroupIdentifier: appGroupIdentifier
        ) else {
            throw SuiteHandoffError.appGroupUnavailable
        }
        let directory = container
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
    case invalidPayload
    case unsupportedVersion

    var errorDescription: String? {
        switch self {
        case .appGroupUnavailable:
            "The suite shared container is unavailable."
        case .invalidPayload:
            "The handoff payload is not valid for Today List."
        case .unsupportedVersion:
            "This handoff was created by an unsupported suite version."
        }
    }
}
