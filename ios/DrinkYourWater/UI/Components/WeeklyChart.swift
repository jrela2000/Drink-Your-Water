import SwiftUI

struct WeeklyChart: View {
    let days: [DaySummary]

    private var maxValue: Int {
        max(days.map { max($0.completed, $0.planned) }.max() ?? 1, 1)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("This Week")
                .font(.headline)
                .foregroundStyle(Palette.onSurface)
            Text("Confirmed check-ins per day")
                .font(.caption)
                .foregroundStyle(Palette.onSurfaceVariant)

            HStack(alignment: .bottom, spacing: 10) {
                ForEach(days) { day in
                    VStack(spacing: 6) {
                        Text("\(day.completed)")
                            .font(.caption2.bold())
                            .foregroundStyle(day.isToday ? Palette.freshBlue : Palette.onSurfaceVariant)
                        RoundedRectangle(cornerRadius: 6)
                            .fill(barColor(day))
                            .frame(height: max(8, 110 * CGFloat(day.completed) / CGFloat(maxValue)))
                        Text(day.label)
                            .font(.caption2)
                            .fontWeight(day.isToday ? .bold : .regular)
                            .foregroundStyle(day.isToday ? Palette.freshBlue : Palette.onSurfaceVariant)
                    }
                    .frame(maxWidth: .infinity)
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel("\(day.label): \(day.completed) of \(day.planned)")
                }
            }
            .frame(height: 150, alignment: .bottom)
        }
        .card()
    }

    private func barColor(_ day: DaySummary) -> Color {
        if day.isToday { return Palette.freshBlue }
        if day.planned > 0 && day.completed >= day.planned { return Palette.iceTeal }
        return Palette.clearWater
    }
}
