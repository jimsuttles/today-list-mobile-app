import Foundation

enum TodayListRoute: Equatable {
    case today
    case later
    case add(TaskLocation)
    case endMyDay
    case task(String)

    init?(url: URL) {
        guard url.scheme?.lowercased() == "todaylist" else { return nil }

        // Preserve the production task deep link: todaylist://task/<id>
        if url.host?.lowercased() == "task" {
            let id = url.pathComponents.first(where: { $0 != "/" }) ?? url.lastPathComponent
            guard !id.isEmpty else { return nil }
            self = .task(id)
            return
        }

        guard url.host?.lowercased() == "v1" else { return nil }
        let component = url.pathComponents.first(where: { $0 != "/" })?.lowercased()

        switch component {
        case "today":
            self = .today
        case "later":
            self = .later
        case "add":
            let query = URLComponents(url: url, resolvingAgainstBaseURL: false)
            let destination = query?.queryItems?
                .first(where: { $0.name == "destination" })?
                .value?
                .lowercased()
            self = .add(destination == "later" ? .later : .today)
        case "end-my-day":
            self = .endMyDay
        default:
            return nil
        }
    }
}
