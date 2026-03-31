import SwiftUI

struct LeadListView: View {
    @ObservedObject var store: LeadStore
    @State private var searchText: String = ""

    private var filteredLeads: [OpportunityListItem] {
        guard !searchText.isEmpty else { return store.leads }
        return store.leads.filter {
            $0.displayName.localizedCaseInsensitiveContains(searchText)
                || ($0.companyName?.localizedCaseInsensitiveContains(searchText) ?? false)
                || ($0.email?.localizedCaseInsensitiveContains(searchText) ?? false)
        }
    }

    var body: some View {
        List(filteredLeads) { lead in
            NavigationLink(value: lead.partyId) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(lead.displayName).font(.headline)
                    Text(lead.companyName ?? "No company").font(.subheadline)
                    Text(lead.email ?? "No email").font(.caption).foregroundStyle(.secondary)
                    HStack {
                        Text(lead.stage.badgeTitle)
                            .font(.caption)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 2)
                            .background(.blue.opacity(0.1), in: Capsule())
                        Text(lead.nextAction).font(.caption).foregroundStyle(.secondary)
                    }
                }
                .padding(.vertical, 4)
            }
        }
        .navigationDestination(for: String.self) { partyId in
            LeadDetailView(store: store, partyId: partyId)
        }
        .searchable(text: $searchText, prompt: "Search name, company, email")
        .navigationTitle("Leads")
        .toolbar {
            NavigationLink("New Lead") {
                NewLeadView(store: store)
            }
        }
        .overlay {
            if store.isLoading { ProgressView("Loading leads…") }
        }
        .task { await store.loadLeads() }
        .refreshable { await store.loadLeads() }
        .alert("Error", isPresented: .constant(store.errorMessage != nil), actions: {
            Button("OK") { store.errorMessage = nil }
        }, message: {
            Text(store.errorMessage ?? "")
        })
    }
}
