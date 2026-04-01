import SwiftUI

struct LeadListView: View {
    @ObservedObject var store: LeadStore
    @State private var searchText: String = ""

    private let stageOrder: [OpportunityStage] = [.NEW, .REQUEST_READY, .QUOTE_READY]

    private var filteredLeads: [OpportunityListItem] {
        guard !searchText.isEmpty else { return store.leads }
        return store.leads.filter {
            $0.displayName.localizedCaseInsensitiveContains(searchText)
                || ($0.companyName?.localizedCaseInsensitiveContains(searchText) ?? false)
                || ($0.email?.localizedCaseInsensitiveContains(searchText) ?? false)
        }
    }

    private var groupedLeads: [(stage: OpportunityStage, leads: [OpportunityListItem])] {
        let grouped = Dictionary(grouping: filteredLeads, by: \.stage)

        return stageOrder.compactMap { stage in
            guard let leads = grouped[stage], !leads.isEmpty else { return nil }

            return (
                stage,
                leads.sorted {
                    $0.displayName.localizedCaseInsensitiveCompare($1.displayName) == .orderedAscending
                }
            )
        }
    }

    private func stageTint(for stage: OpportunityStage) -> Color {
        switch stage {
        case .NEW:
            return .blue
        case .REQUEST_READY:
            return .orange
        case .QUOTE_READY:
            return .green
        }
    }

    var body: some View {
        List {
            ForEach(groupedLeads, id: \.stage) { group in
                Section(group.stage.badgeTitle) {
                    ForEach(group.leads) { lead in
                        NavigationLink(value: lead.partyId) {
                            VStack(alignment: .leading, spacing: 4) {
                                HStack(alignment: .firstTextBaseline, spacing: 8) {
                                    Text(lead.displayName).font(.headline)
                                    Text(lead.stage.badgeTitle)
                                        .font(.caption)
                                        .foregroundStyle(stageTint(for: lead.stage))
                                        .padding(.horizontal, 8)
                                        .padding(.vertical, 2)
                                        .background(stageTint(for: lead.stage).opacity(0.12), in: Capsule())
                                }
                                Text(lead.companyName ?? "No company").font(.subheadline)
                                if let email = lead.email, !email.isEmpty {
                                    Text(email).font(.caption).foregroundStyle(.secondary)
                                }
                            }
                            .padding(.vertical, 4)
                        }
                    }
                }
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
