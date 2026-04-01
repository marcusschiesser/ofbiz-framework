import SwiftUI

struct LeadDetailView: View {
    @ObservedObject var store: LeadStore
    let partyId: String

    @State private var detail: OpportunityDetail?
    @State private var isLoading = false
    @State private var errorMessage: String?
    @State private var isShowingRequestForm = false

    private var stageTint: Color {
        guard let detail else { return .blue }

        switch detail.stage {
        case .NEW:
            return .blue
        case .REQUEST_READY:
            return .orange
        case .QUOTE_READY:
            return .green
        }
    }

    private var requestActionTitle: String {
        guard let detail else { return "Add Request" }
        return detail.request == nil ? "Add Request" : "Edit Request"
    }

    var body: some View {
        Group {
            if let detail {
                Form {
                    Section {
                        HStack(alignment: .firstTextBaseline, spacing: 8) {
                            Text(detail.displayName)
                            Text(detail.stage.badgeTitle)
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(stageTint)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 6)
                                .background(stageTint.opacity(0.12), in: Capsule())
                        }
                        if !detail.email.isEmpty {
                            Text(detail.email)
                        }
                        Text(detail.companyName ?? "No company")
                    } header: {
                        HStack {
                            Text("Lead")
                            Spacer()
                        }
                    }
                    if let request = detail.request {
                        Section("Request") {
                            Text(request.name)
                            if let description = request.description { Text(description) }
                        }
                    }
                    Section {
                        Button {
                            isShowingRequestForm = true
                        } label: {
                            Text(requestActionTitle)
                                .frame(maxWidth: .infinity)
                        }
                        .primaryActionButtonStyle()
                    }
                }
            } else if isLoading {
                ProgressView("Loading…")
            } else {
                ContentUnavailableView("Lead unavailable", systemImage: "person.fill.xmark")
            }
        }
        .navigationTitle("Lead Detail")
        .navigationDestination(isPresented: $isShowingRequestForm) {
            if let detail {
                RequestFormView(store: store, detail: detail)
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
