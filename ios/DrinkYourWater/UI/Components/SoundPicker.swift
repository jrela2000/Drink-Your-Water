import AVFoundation
import SwiftUI

/// Plays the bundled notification tones so users can preview them.
@MainActor
final class SoundPreviewPlayer {
    static let shared = SoundPreviewPlayer()
    private var player: AVAudioPlayer?

    func play(_ sound: NotificationSound) {
        guard let url = Bundle.main.url(forResource: sound.rawValue, withExtension: "wav") else { return }
        try? AVAudioSession.sharedInstance().setCategory(.ambient)
        player = try? AVAudioPlayer(contentsOf: url)
        player?.play()
    }
}

struct SoundPickerSheet: View {
    let selected: NotificationSound
    let onSelect: (NotificationSound) -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                Section {
                    ForEach(NotificationSound.allCases) { sound in
                        Button {
                            SoundPreviewPlayer.shared.play(sound)
                            onSelect(sound)
                        } label: {
                            HStack(spacing: 12) {
                                IconBadge(
                                    systemName: "music.note",
                                    tint: sound == selected ? Palette.freshBlue : Palette.snoozeGray,
                                    size: 36
                                )
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(sound.title).font(.headline).foregroundStyle(Palette.onSurface)
                                    Text(sound.subtitle).font(.caption).foregroundStyle(Palette.onSurfaceVariant)
                                }
                                Spacer()
                                if sound == selected {
                                    Image(systemName: "checkmark.circle.fill").foregroundStyle(Palette.freshBlue)
                                }
                            }
                        }
                    }
                } footer: {
                    Text("Tap a tone to preview it. It plays when a reminder fires.")
                }
            }
            .navigationTitle("Notification Chime")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                }
            }
        }
        .presentationDetents([.medium, .large])
    }
}
