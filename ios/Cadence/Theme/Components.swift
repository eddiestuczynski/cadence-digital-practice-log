import SwiftUI
import UIKit

// MARK: - Card

struct CadenceCard<Content: View>: View {
    @Environment(\.cadencePalette) private var palette

    var padding: CGFloat
    var cornerRadius: CGFloat
    var fill: Color?
    var borderColor: Color?
    var borderWidth: CGFloat
    let content: Content

    init(
        padding: CGFloat = 16,
        cornerRadius: CGFloat = 18,
        fill: Color? = nil,
        borderColor: Color? = nil,
        borderWidth: CGFloat = 1,
        @ViewBuilder content: () -> Content
    ) {
        self.padding = padding
        self.cornerRadius = cornerRadius
        self.fill = fill
        self.borderColor = borderColor
        self.borderWidth = borderWidth
        self.content = content()
    }

    var body: some View {
        content
            .padding(padding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .fill(fill ?? palette.surface)
            )
            .overlay(
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .stroke(borderColor ?? palette.outline.opacity(0.5), lineWidth: borderWidth)
            )
    }
}

// MARK: - Chips

struct ChipButton: View {
    @Environment(\.cadencePalette) private var palette

    let title: String
    let isSelected: Bool
    var action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.footnote.weight(.semibold))
                .lineLimit(1)
                .padding(.horizontal, 12)
                .padding(.vertical, 7)
                .foregroundStyle(isSelected ? palette.onPrimaryContainer : palette.inkPrimary)
                .background(
                    Capsule().fill(isSelected ? palette.primaryContainer : palette.surfaceVariant.opacity(0.8))
                )
                .overlay(
                    Capsule().stroke(
                        isSelected ? palette.primary.opacity(0.7) : palette.outline.opacity(0.5),
                        lineWidth: 1
                    )
                )
        }
        .buttonStyle(.plain)
    }
}

struct CadencePill: View {
    let text: String
    let foreground: Color
    let background: Color

    var body: some View {
        Text(text)
            .font(.caption2.weight(.bold))
            .foregroundStyle(foreground)
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .background(Capsule().fill(background))
    }
}

// MARK: - Progress

struct CadenceProgressBar: View {
    @Environment(\.cadencePalette) private var palette

    let value: Double
    var color: Color?
    var height: CGFloat = 8

    var body: some View {
        GeometryReader { geometry in
            ZStack(alignment: .leading) {
                Capsule().fill(palette.surfaceVariant)
                Capsule()
                    .fill(color ?? palette.primary)
                    .frame(width: geometry.size.width * min(max(value, 0), 1))
            }
        }
        .frame(height: height)
    }
}

// MARK: - Stars

struct StarRatingView: View {
    @Environment(\.cadencePalette) private var palette

    let rating: Int
    var size: CGFloat = 22
    var onSelect: ((Int) -> Void)?

    var body: some View {
        HStack(spacing: 4) {
            ForEach(1...5, id: \.self) { star in
                Button {
                    onSelect?(star)
                } label: {
                    Image(systemName: star <= rating ? "star.fill" : "star")
                        .font(.system(size: size))
                        .foregroundStyle(star <= rating ? palette.secondary : palette.outline)
                }
                .buttonStyle(.plain)
                .disabled(onSelect == nil)
            }
        }
    }
}

// MARK: - Text field

struct CadenceTextField: View {
    @Environment(\.cadencePalette) private var palette

    let label: String
    var placeholder: String = ""
    @Binding var text: String
    var lineLimit: ClosedRange<Int> = 1...1
    var keyboardType: UIKeyboardType = .default

    private var axis: Axis {
        lineLimit.upperBound > 1 ? .vertical : .horizontal
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 5) {
            Text(label.uppercased())
                .font(.caption2.weight(.semibold))
                .foregroundStyle(palette.inkSecondary)
            TextField(placeholder.isEmpty ? label : placeholder, text: $text, axis: axis)
                .font(.body)
                .foregroundStyle(palette.inkPrimary)
                .lineLimit(lineLimit)
                .keyboardType(keyboardType)
                .padding(10)
                .background(
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .fill(palette.surfaceVariant.opacity(0.65))
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .stroke(palette.outline.opacity(0.5), lineWidth: 1)
                )
        }
    }
}

// MARK: - Metric card

struct MetricSummaryCard: View {
    @Environment(\.cadencePalette) private var palette

    let title: String
    let value: String
    let subtitle: String
    let systemImage: String
    var tint: Color?

    var body: some View {
        CadenceCard(padding: 12, cornerRadius: 16) {
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 6) {
                    Image(systemName: systemImage)
                        .font(.caption)
                        .foregroundStyle(tint ?? palette.secondary)
                    Text(title)
                        .font(.caption2)
                        .foregroundStyle(palette.inkSecondary)
                }
                Text(value)
                    .font(.title3.weight(.bold))
                    .foregroundStyle(palette.inkPrimary)
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
                Text(subtitle)
                    .font(.caption2)
                    .foregroundStyle(palette.inkSecondary)
            }
        }
    }
}

// MARK: - Banner

struct CadenceBanner: View {
    @Environment(\.cadencePalette) private var palette

    let message: String
    var onDismiss: (() -> Void)?

    var body: some View {
        Button {
            onDismiss?()
        } label: {
            HStack(spacing: 10) {
                Image(systemName: "checkmark.circle.fill")
                    .foregroundStyle(palette.onTertiaryContainer)
                Text(message)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(palette.onTertiaryContainer)
                    .multilineTextAlignment(.leading)
                Spacer(minLength: 0)
            }
            .padding(12)
            .background(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(palette.tertiaryContainer)
            )
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Section label

struct CadenceSectionLabel: View {
    @Environment(\.cadencePalette) private var palette
    let text: String

    var body: some View {
        Text(text.uppercased())
            .font(.caption.weight(.semibold))
            .foregroundStyle(palette.inkSecondary)
    }
}

// MARK: - Flow layout

/// A wrapping HStack used for tag clouds.
struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let maxWidth = proposal.width ?? .infinity
        var x: CGFloat = 0
        var y: CGFloat = 0
        var rowHeight: CGFloat = 0
        var widest: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x > 0 && x + size.width > maxWidth {
                x = 0
                y += rowHeight + spacing
                rowHeight = 0
            }
            x += size.width + spacing
            widest = max(widest, x - spacing)
            rowHeight = max(rowHeight, size.height)
        }

        let width = proposal.width ?? widest
        return CGSize(width: width, height: y + rowHeight)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.minX
        var y = bounds.minY
        var rowHeight: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x > bounds.minX && x + size.width > bounds.maxX {
                x = bounds.minX
                y += rowHeight + spacing
                rowHeight = 0
            }
            subview.place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(size))
            x += size.width + spacing

            rowHeight = max(rowHeight, size.height)
        }
    }
}

// MARK: - Small helper views

struct MeterPreset: Identifiable {
    let beats: Int
    let noteValue: Int
    let label: String

    var id: String { label }
}

struct HorizontalChips<Content: View>: View {
    let content: Content

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                content
            }
        }
    }
}
