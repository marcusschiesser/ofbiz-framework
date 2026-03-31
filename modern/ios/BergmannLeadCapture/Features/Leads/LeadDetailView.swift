import SwiftUI

struct LeadDetailView: View {
    @ObservedObject var store: LeadStore
    let partyId: String

    @State private var detail: OpportunityDetail?
    @State private var isLoading = false
    @State private var errorMessage: String?

    var body: some View {
        Group {
            if let detail {
                Form {
                    Section("Lead") {
                        Text(detail.displayName)
                        Text(detail.email)
                        Text(detail.companyName ?? "No company")
                    }
                    Section("Pipeline") {
                        Text(detail.stage.badgeTitle)
                        Text(detail.nextAction)
                    }
                    if let request = detail.request {
                        Section("Request") {
                            Text(request.name)
                            if let description = request.description { Text(description) }
                            Text(request.isLocked ? "Read-only" : "Editable")
                        }
                    }
                }
            } else if isLoading {
                ProgressView("Loading…")
            } else {
                ContentUnavailableView("Lead unavailable", systemImage: "person.fill.xmark")
            }
        }
        .navigationTitle("Lead Detail")
        .toolbar {
            if let detail {
                NavigationLink("Add Request") {
                    RequestFormView(store: store, detail: detail)
                }
            }
        }
        .task { await refresh() }
        .alert("Error", isPresented: .constant(errorMessage != nil), actions: {
            Button("OK") { errorMessage = nil }
        }, message: {
            Text(errorMessage ?? "")
        })
    }

    private func refresh() async {
        isLoading = true
        defer { isLoading = false }
        do {
            detail = try await store.loadDetail(partyId)
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
