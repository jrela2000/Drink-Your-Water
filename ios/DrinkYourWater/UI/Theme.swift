import SwiftUI
import UIKit

extension Color {
    init(hex: UInt32, opacity: Double = 1) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255,
            opacity: opacity
        )
    }

    init(light: UInt32, dark: UInt32) {
        func ui(_ hex: UInt32) -> UIColor {
            UIColor(
                red: CGFloat((hex >> 16) & 0xFF) / 255,
                green: CGFloat((hex >> 8) & 0xFF) / 255,
                blue: CGFloat(hex & 0xFF) / 255,
                alpha: 1
            )
        }
        self.init(uiColor: UIColor { $0.userInterfaceStyle == .dark ? ui(dark) : ui(light) })
    }
}

/// The "Cold Water" palette from the Android app.
enum Palette {
    static let iceWhite = Color(hex: 0xF0F8FF)
    static let glacierBlue = Color(hex: 0xE8F4FD)
    static let clearWater = Color(hex: 0xB8DFF0)
    static let freshBlue = Color(hex: 0x4FB3E8)
    static let deepWater = Color(hex: 0x1A6FA8)
    static let midnightWater = Color(hex: 0x0D3B6B)
    static let iceTeal = Color(hex: 0x48CAE4)
    static let coolMint = Color(hex: 0xA8E6CF)
    static let coldMist = Color(hex: 0xCAE9FF)
    static let confirmGreen = Color(hex: 0x52C788)
    static let snoozeGray = Color(hex: 0x8EAFC2)
    static let dangerRed = Color(hex: 0xE05A5A)

    static let background = Color(light: 0xF0F8FF, dark: 0x0F172A)
    static let surface = Color(light: 0xFFFFFF, dark: 0x1E293B)
    static let surfaceVariant = Color(light: 0xE8F4FD, dark: 0x334155)
    static let onBackground = Color(light: 0x0D3B6B, dark: 0xF0F8FF)
    static let onSurface = Color(light: 0x1A6FA8, dark: 0xF0F8FF)
    static let onSurfaceVariant = Color(light: 0x1A6FA8, dark: 0xCAE9FF)
    static let outline = Color(light: 0xB8DFF0, dark: 0x475569)
    static let track = Color(light: 0xCAE9FF, dark: 0x334155)
}

extension OverlayTheme {
    var gradient: LinearGradient {
        let hex = gradientHex
        return LinearGradient(colors: [Color(hex: hex.top), Color(hex: hex.bottom)], startPoint: .top, endPoint: .bottom)
    }
}

struct PrimaryButtonStyle: ButtonStyle {
    var color: Color = Palette.freshBlue

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.headline)
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity, minHeight: 54)
            .background(color.opacity(configuration.isPressed ? 0.8 : 1), in: RoundedRectangle(cornerRadius: 18))
            .scaleEffect(configuration.isPressed ? 0.98 : 1)
    }
}

struct CardBackground: ViewModifier {
    var color: Color = Palette.surfaceVariant

    func body(content: Content) -> some View {
        content
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(color, in: RoundedRectangle(cornerRadius: 20))
    }
}

extension View {
    func card(_ color: Color = Palette.surfaceVariant) -> some View {
        modifier(CardBackground(color: color))
    }
}

/// Small round icon badge used across cards.
struct IconBadge: View {
    let systemName: String
    var tint: Color = Palette.freshBlue
    var size: CGFloat = 42

    var body: some View {
        Image(systemName: systemName)
            .font(.system(size: size * 0.45, weight: .semibold))
            .foregroundStyle(tint)
            .frame(width: size, height: size)
            .background(tint.opacity(0.15), in: Circle())
    }
}
