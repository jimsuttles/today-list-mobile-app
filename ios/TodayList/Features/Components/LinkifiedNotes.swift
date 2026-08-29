import Foundation
import SwiftUI

enum LinkDetector {
    static func urls(in text: String) -> [URL] {
        guard let detector = try? NSDataDetector(types: NSTextCheckingResult.CheckingType.link.rawValue) else {
            return []
        }
        let range = NSRange(text.startIndex..<text.endIndex, in: text)
        return detector.matches(in: text, options: [], range: range).compactMap { match in
            match.url
        }
    }

    static func attributed(_ text: String) -> AttributedString {
        var attributed = AttributedString(text)
        guard let detector = try? NSDataDetector(types: NSTextCheckingResult.CheckingType.link.rawValue) else {
            return attributed
        }
        let ns = text as NSString
        let matches = detector.matches(in: text, options: [], range: NSRange(location: 0, length: ns.length))
        for match in matches {
            guard let url = match.url,
                  let range = Range(match.range, in: text),
                  let attrRange = Range(range, in: attributed) else { continue }
            attributed[attrRange].link = url
            attributed[attrRange].foregroundColor = Color.tlPrimary
            attributed[attrRange].underlineStyle = .single
        }
        return attributed
    }
}
