import SwiftUI

enum TLTheme {
    static let primary = Color(red: 0x1B/255, green: 0x4F/255, blue: 0x72/255)
    static let primaryDark = Color(red: 0x7E/255, green: 0xC4/255, blue: 0xE8/255)
    static let secondary = Color(red: 0xD9/255, green: 0x77/255, blue: 0x57/255)
    static let secondaryDark = Color(red: 0xF0/255, green: 0xA8/255, blue: 0x8A/255)
    static let tertiary = Color(red: 0x3D/255, green: 0x8B/255, blue: 0x9C/255)
    static let background = Color(red: 0xEE/255, green: 0xF4/255, blue: 0xF8/255)
    static let backgroundDark = Color(red: 0x0E/255, green: 0x14/255, blue: 0x19/255)
    static let surface = Color.white
    static let surfaceDark = Color(red: 0x16/255, green: 0x20/255, blue: 0x28/255)
    static let surfaceVariant = Color(red: 0xDC/255, green: 0xE8/255, blue: 0xF0/255)
    static let surfaceVariantDark = Color(red: 0x24/255, green: 0x34/255, blue: 0x40/255)
    static let onBackground = Color(red: 0x1A/255, green: 0x1F/255, blue: 0x24/255)
    static let onBackgroundDark = Color(red: 0xE8/255, green: 0xEE/255, blue: 0xF3/255)
    static let outline = Color(red: 0x6B/255, green: 0x7C/255, blue: 0x8A/255)
}

extension Color {
    static let tlPrimary = TLTheme.primary
    static let tlSecondary = TLTheme.secondary
    static let tlTertiary = TLTheme.tertiary
    static let tlBackground = TLTheme.background
    static let tlSurface = TLTheme.surface
    static let tlSurfaceVariant = TLTheme.surfaceVariant
    static let tlOnBackground = TLTheme.onBackground
    static let tlOutline = TLTheme.outline
}
