import SwiftData
import SwiftUI

@main
struct OstinatoApp: App {

    private let container: ModelContainer

    @State private var store: OstinatoStore
    @State private var preferences: PreferencesStore
    @State private var studio: StudioViewModel

    init() {
        let schema = Schema([
            RepertoirePiece.self,
            PieceSection.self,
            PracticeSession.self,
            RoutineTemplate.self,
            RoutineTemplateItem.self
        ])

        let configuration = ModelConfiguration("Ostinato", schema: schema)
        let container: ModelContainer
        do {
            container = try ModelContainer(for: schema, configurations: [configuration])
        } catch {
            // Never fail to launch because of a corrupt/unmigratable store.
            let memory = ModelConfiguration(isStoredInMemoryOnly: true)
            container = try! ModelContainer(for: schema, configurations: [memory])
            print("Ostinato fell back to an in-memory store: \(error)")
        }
        self.container = container

        let preferences = PreferencesStore()
        let store = OstinatoStore(context: container.mainContext)

        if !preferences.initialSeedCompleted && store.pieces.isEmpty {
            DemoData.seed(into: store)
            preferences.markInitialSeedCompleted()
        }

        _store = State(initialValue: store)
        _preferences = State(initialValue: preferences)
        _studio = State(initialValue: StudioViewModel(store: store, preferences: preferences))
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .environment(store)
                .environment(preferences)
                .environment(studio)
        }
        .modelContainer(container)
    }
}
