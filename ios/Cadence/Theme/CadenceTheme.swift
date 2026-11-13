import SwiftUI

/// A complete palette so every custom control can match the Android
/// Material 3 colour scheme without relying on the system tint.
struct CadencePalette {
    let background: Color
    let surface: Color
    let surfaceVariant: Color
    let inkPrimary: Color
    let inkSecondary: Color
    let primary: Color
    let onPrimary: Color
    let primaryContainer: Color
    let onPrimaryContainer: Color
    let secondary: Color
    let onSecondary: Color
    let secondaryContainer: Color
    let onSecondaryContainer: Color
    let tertiary: Color
    let onTertiary: Color
    let tertiaryContainer: Color
    let onTertiaryContainer: Color
    let outline: Color
}

enum CadenceTheme {

    // MARK: - Day ("Sheet Music Paper")

    static let day = CadencePalette(
        background: Color(hex: 0xFDFBF6),
        surface: Color(hex: 0xF5F0E6),
        surfaceVariant: Color(hex: 0xEBE4D5),
        inkPrimary: Color(hex: 0x1A1614),
        inkSecondary: Color(hex: 0x534A43),
        primary: Color(hex: 0x8C3B2B),
        onPrimary: .white,
        primaryContainer: Color(hex: 0xFADCD4),
        onPrimaryContainer: Color(hex: 0x361008),
        secondary: Color(hex: 0x966714),
        onSecondary: .white,
        secondaryContainer: Color(hex: 0xFCE6BE),
        onSecondaryContainer: Color(hex: 0x332000),
        tertiary: Color(hex: 0x286649),
        onTertiary: .white,
        tertiaryContainer: Color(hex: 0xC7EBD7),
        onTertiaryContainer: Color(hex: 0x062918),
        outline: Color(hex: 0xC4B9A2)
    )

    // MARK: - Night Stage ("Stage Amber")

    static let nightStage = CadencePalette(
        background: Color(hex: 0x101318),
        surface: Color(hex: 0x191E26),
        surfaceVariant: Color(hex: 0x242B36),
        inkPrimary: Color(hex: 0xF4EFE6),
        inkSecondary: Color(hex: 0xBDB5A8),
        primary: Color(hex: 0xF4B860),
        onPrimary: Color(hex: 0x2B1B00),
        primaryContainer: Color(hex: 0x52390D),
        onPrimaryContainer: Color(hex: 0xFFDFB3),
        secondary: Color(hex: 0xE88D72),
        onSecondary: Color(hex: 0x2B1B00),
        secondaryContainer: Color(hex: 0x572B1E),
        onSecondaryContainer: Color(hex: 0xFFDBD0),
        tertiary: Color(hex: 0x6CD4A2),
        onTertiary: Color(hex: 0x2B1B00),
        tertiaryContainer: Color(hex: 0x154730),
        onTertiaryContainer: Color(hex: 0xB3F5D2),
        outline: Color(hex: 0x3D4657)
    )

    // MARK: - Night OLED ("OLED Pit")

    static let nightOLED = CadencePalette(
        background: Color(hex: 0x000000),
        surface: Color(hex: 0x0C0E12),
        surfaceVariant: Color(hex: 0x171B22),
        inkPrimary: Color(hex: 0xF7F3EB),
        inkSecondary: Color(hex: 0xC4BCB0),
        primary: Color(hex: 0xFFC46B),
        onPrimary: Color(hex: 0x261700),
        primaryContainer: Color(hex: 0x473108),
        onPrimaryContainer: Color(hex: 0xFFE2B8),
        secondary: Color(hex: 0xE88D72),
        onSecondary: Color(hex: 0x2B1B00),
        secondaryContainer: Color(hex: 0x572B1E),
        onSecondaryContainer: Color(hex: 0xFFDBD0),
        tertiary: Color(hex: 0x6CD4A2),
        onTertiary: Color(hex: 0x2B1B00),
        tertiaryContainer: Color(hex: 0x154730),
        onTertiaryContainer: Color(hex: 0xB3F5D2),
        outline: Color(hex: 0x2E3542)
    )

    static func palette(for mode: ThemeMode, variant: DarkVariant, systemDark: Bool) -> CadencePalette {
        switch mode {
        case .DAY:
            return day
        case .NIGHT_STAGE:
            return nightStage
        case .NIGHT_OLED:
            return nightOLED
        case .SYSTEM:
            if !systemDark { return day }
            return variant == .OLED_BLACK ? nightOLED : nightStage
        }
    }

    static func isDark(for mode: ThemeMode, variant: DarkVariant, systemDark: Bool) -> Bool {
        switch mode {
        case .DAY:
            return false
        case .NIGHT_STAGE, .NIGHT_OLED:
            return true
        case .SYSTEM:
            return systemDark
        }
    }

    static func subtitle(for mode: ThemeMode, variant: DarkVariant, systemDark: Bool) -> String {
        switch mode {
        case .DAY: return "Day • Sheet Music Mode"
        case .NIGHT_STAGE: return "Night • Stage Amber Mode"
        case .NIGHT_OLED: return "Night • OLED Pit Black"
        case .SYSTEM:
            guard systemDark else { return "Auto • Sheet Music Day" }
            return variant == .OLED_BLACK ? "Auto • OLED Pit Black" : "Auto • Stage Amber Mode"
        }
    }
}

// MARK: - Environment

private struct CadencePaletteKey: EnvironmentKey {
    static let defaultValue = CadenceTheme.day
}

extension EnvironmentValues {
    var cadencePalette: CadencePalette {
        get { self[CadencePaletteKey.self] }
        set { self[CadencePaletteKey.self] = newValue }
    }
}

// MARK: - Hex helper

extension Color {
    init(hex: UInt32, alpha: Double = 1.0) {
        let red = Double((hex >> 16) & 0xFF) / 255.0
        let green = Double((hex >> 8) & 0xFF) / 255.0
        let blue = Double(hex & 0xFF) / 255.0
        self.init(.sRGB, red: red, green: green, blue: blue, opacity: alpha)
    }
}
