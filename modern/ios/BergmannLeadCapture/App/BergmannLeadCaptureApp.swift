import SwiftUI

@main
struct BergmannLeadCaptureApp: App {
    @StateObject private var leadStore = LeadStore(apiClient: OpportunityAPIClient.live)

    var body: some Scene {
        WindowGroup {
            NavigationStack {
                LeadListView(store: leadStore)
            }
        }
    }
}
