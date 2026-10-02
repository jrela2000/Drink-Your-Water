import SwiftUI

struct ProgressRing: View {
    let completed: Int
    let total: Int
    let streak: Int
    var size: CGFloat = 200
    var lineWidth: CGFloat = 14

    @State private var animatedFraction: Double = 0

    private var fraction: Double {
        total > 0 ? min(Double(completed) / Double(total), 1) : 0
    }

    var body: some View {
        ZStack {
            Circle()
                .stroke(Palette.track, lineWidth: lineWidth)
            Circle()
                .trim(from: 0, to: animatedFraction)
                .stroke(
                    AngularGradient(colors: [Palette.freshBlue, Palette.iceTeal, Palette.freshBlue], center: .center),
                    style: StrokeStyle(lineWidth: lineWidth, lineCap: .round)
                )
                .rotationEffect(.degrees(-90))

            VStack(spacing: 4) {
                Text("\(completed)/\(total)")
                    .font(.system(size: 30, weight: .bold, design: .rounded))
                    .foregroundStyle(Palette.onBackground)
                Text("Check-ins Today")
                    .font(.caption)
                    .foregroundStyle(Palette.onSurfaceVariant)
                Text("🔥 \(streak)-Day Streak")
                    .font(.caption2.bold())
                    .foregroundStyle(Palette.freshBlue)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(Palette.freshBlue.opacity(0.15), in: Capsule())
                    .padding(.top, 2)
            }
        }
        .frame(width: size, height: size)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(completed) of \(total) check-ins today, \(streak) day streak")
        .onAppear { withAnimation(.easeOut(duration: 1)) { animatedFraction = fraction } }
        .onChange(of: fraction) { _, new in withAnimation(.easeOut(duration: 0.6)) { animatedFraction = new } }
    }
}
