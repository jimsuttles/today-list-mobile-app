import Foundation

enum Analytics {
    static func log(_ name: String, parameters: [String: String] = [:]) {
        #if DEBUG
        print("[analytics] \(name) \(parameters)")
        #endif
    }
}
