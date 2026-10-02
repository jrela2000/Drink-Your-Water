import Foundation

/// Reads and writes `AppData` as JSON in Application Support.
struct DataFile {
    let url: URL

    static var defaultURL: URL {
        let base = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        return base.appendingPathComponent("DrinkYourWater", isDirectory: true)
            .appendingPathComponent("data.json")
    }

    private static let encoder: JSONEncoder = {
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        encoder.outputFormatting = [.sortedKeys]
        return encoder
    }()

    private static let decoder: JSONDecoder = {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        return decoder
    }()

    /// nil when there's no file yet or it can't be read.
    func load() -> AppData? {
        guard let data = try? Data(contentsOf: url) else { return nil }
        return try? Self.decoder.decode(AppData.self, from: data)
    }

    func save(_ appData: AppData) throws {
        try FileManager.default.createDirectory(
            at: url.deletingLastPathComponent(), withIntermediateDirectories: true
        )
        let data = try Self.encoder.encode(appData)
        try data.write(to: url, options: [.atomic])
    }
}
