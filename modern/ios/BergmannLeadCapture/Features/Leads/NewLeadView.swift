import SwiftUI

struct NewLeadView: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var store: LeadStore

    @State private var model = CreateLeadRequest()
    @State private var isSaving = false
    @State private var errorMessage: String?
    @State private var createdDetail: OpportunityDetail?

    var body: some View {
        Form {
            Section("Required") {
                TextField("First name", text: $model.firstName)
                TextField("Last name", text: $model.lastName)
                TextField("Email", text: $model.email)
                    .textInputAutocapitalization(.never)
                    .keyboardType(.emailAddress)
            }
            Section("Optional") {
                TextField("Company", text: $model.companyName)
                TextField("Title", text: $model.title)
                TextField("Data source", text: $model.dataSourceId)
            }

            if let errorMessage {
                Section {
                    Text(errorMessage).foregroundStyle(.red)
                }
            }

            Section {
                Button(isSaving ? "Saving…" : "Create Lead") {
                    Task { await save() }
                }
                .disabled(isSaving)
            }
        }
        .navigationTitle("New Lead")
        .navigationDestination(item: $createdDetail) { detail in
            LeadDetailView(store: store, partyId: detail.partyId)
        }
    }

    private func validate() -> String? {
        if model.firstName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { return "First name is required." }
        if model.lastName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { return "Last name is required." }
        if model.email.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { return "Email is required." }
        return nil
    }

    private func save() async {
        if let validation = validate() {
            errorMessage = validation
            return
        }

        isSaving = true
        defer { isSaving = false }

        do {
            let created = try await store.createLead(model)
            createdDetail = created
        } catch {
            errorMessage = (error as? ApiErrorResponse)?.message ?? error.localizedDescription
        }
    }
}
