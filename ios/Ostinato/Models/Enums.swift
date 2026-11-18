import Foundation

// MARK: - Repertoire enums

enum PieceStatus: String, Codable, CaseIterable, Identifiable {
    case LEARNING
    case POLISHING
    case MASTERED

    var id: String { rawValue }

    var label: String {
        switch self {
        case .LEARNING: return "Learning"
        case .POLISHING: return "Polishing"
        case .MASTERED: return "Mastered"
        }
    }

    var next: PieceStatus {
        switch self {
        case .LEARNING: return .POLISHING
        case .POLISHING: return .MASTERED
        case .MASTERED: return .LEARNING
        }
    }
}

enum PieceCategory: String, Codable, CaseIterable, Identifiable {
    case REPERTOIRE
    case ETUDE
    case TECHNIQUE

    var id: String { rawValue }

    var label: String {
        switch self {
        case .REPERTOIRE: return "Repertoire Piece"
        case .ETUDE: return "Etude / Study"
        case .TECHNIQUE: return "Warmup / Scale / Technique"
        }
    }

    var shortLabel: String {
        switch self {
        case .REPERTOIRE: return "Repertoire"
        case .ETUDE: return "Etude"
        case .TECHNIQUE: return "Technique"
        }
    }
}

// MARK: - Visual modes

enum ThemeMode: String, CaseIterable, Identifiable {
    case DAY
    case NIGHT_STAGE
    case NIGHT_OLED
    case SYSTEM

    var id: String { rawValue }

    var label: String {
        switch self {
        case .DAY: return "Day Mode (Sheet Music Paper)"
        case .NIGHT_STAGE: return "Night Mode (Stage Amber)"
        case .NIGHT_OLED: return "Night Mode (OLED Pit Black)"
        case .SYSTEM: return "System Auto"
        }
    }

    var shortLabel: String {
        switch self {
        case .DAY: return "Day"
        case .NIGHT_STAGE: return "Stage"
        case .NIGHT_OLED: return "OLED"
        case .SYSTEM: return "Auto"
        }
    }

    var systemImage: String {
        switch self {
        case .DAY: return "sun.max.fill"
        case .NIGHT_STAGE: return "moon.fill"
        case .NIGHT_OLED: return "circle.lefthalf.filled"
        case .SYSTEM: return "sparkles"
        }
    }
}

enum DarkVariant: String, CaseIterable, Identifiable {
    case STAGE_AMBER
    case OLED_BLACK

    var id: String { rawValue }

    var label: String {
        switch self {
        case .STAGE_AMBER: return "Stage Amber (Warm Slate)"
        case .OLED_BLACK: return "OLED Pit (True Black #000000)"
        }
    }

    var shortLabel: String {
        switch self {
        case .STAGE_AMBER: return "Stage Amber"
        case .OLED_BLACK: return "OLED Pit"
        }
    }
}

// MARK: - Metronome enums

enum SubdivisionMode: String, Codable, CaseIterable, Identifiable {
    case QUARTER
    case EIGHTH
    case TRIPLET
    case SIXTEENTH
    case SWING_EIGHTH

    var id: String { rawValue }

    var label: String {
        switch self {
        case .QUARTER: return "Quarter Notes"
        case .EIGHTH: return "Eighth Notes"
        case .TRIPLET: return "Triplets"
        case .SIXTEENTH: return "Sixteenth Notes"
        case .SWING_EIGHTH: return "Swing Eighths"
        }
    }

    var shortLabel: String {
        switch self {
        case .QUARTER: return "1x"
        case .EIGHTH: return "2x"
        case .TRIPLET: return "3x"
        case .SIXTEENTH: return "4x"
        case .SWING_EIGHTH: return "Swing"
        }
    }

    var subdivisionsPerBeat: Int {
        switch self {
        case .QUARTER: return 1
        case .EIGHTH, .SWING_EIGHTH: return 2
        case .TRIPLET: return 3
        case .SIXTEENTH: return 4
        }
    }
}

enum BeatAccentLevel: String, Codable, CaseIterable, Identifiable {
    case ACCENT
    case NORMAL
    case MUTE

    var id: String { rawValue }

    var label: String {
        switch self {
        case .ACCENT: return "Loud Accent"
        case .NORMAL: return "Normal Click"
        case .MUTE: return "Silent Beat"
        }
    }

    var next: BeatAccentLevel {
        switch self {
        case .ACCENT: return .NORMAL
        case .NORMAL: return .MUTE
        case .MUTE: return .ACCENT
        }
    }
}
