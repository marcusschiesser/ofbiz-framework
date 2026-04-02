import SwiftUI

struct NewLeadView: View {
    private enum Field: Hashable {
        case firstName
        case lastName
        case email
    }

    @Environment(\.dismiss) private var dismiss
    @ObservedObject var store: LeadStore

    @State private var model = CreateLeadRequest()
    @State private var isSaving = false
    @State private var errorMessage: String?
    @State private var createdDetail: OpportunityDetail?
    @State private var attemptedSubmit = false
    @FocusState private var focusedField: Field?

    private var trimmedFirstName: String {
        model.firstName.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var trimmedLastName: String {
        model.lastName.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var trimmedEmail: String {
        model.email.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var hasValidEmailFormat: Bool {
        guard !trimmedEmail.isEmpty else { return false }
        let parts = trimmedEmail.split(separator: "@")
        guard parts.count == 2,
              !parts[0].isEmpty,
              !parts[1].isEmpty,
              parts[1].contains("."),
              !parts[1].hasPrefix("."),
              !parts[1].hasSuffix(".")
        else {
            return false
        }
        return true
    }

    private var firstNameError: String? {
        attemptedSubmit && trimmedFirstName.isEmpty ? "Vorname ist erforderlich." : nil
    }

    private var lastNameError: String? {
        attemptedSubmit && trimmedLastName.isEmpty ? "Nachname ist erforderlich." : nil
    }

    private var emailError: String? {
        guard attemptedSubmit else { return nil }
        if trimmedEmail.isEmpty { return "E-Mail ist erforderlich." }
        if !hasValidEmailFormat { return "Bitte eine gueltige E-Mail-Adresse eingeben." }
        return nil
    }

    var body: some View {
        Form {
            Section("Pflichtfelder") {
                VStack(alignment: .leading, spacing: 4) {
                    TextField(text: $model.firstName, prompt: Text("Vorname")) {
                        Text("Vorname")
                    }
                    .focused($focusedField, equals: .firstName)
                    .textContentType(.givenName)
                    .submitLabel(.next)
                    .onSubmit { focusedField = .lastName }

                    if let firstNameError {
                        Text(firstNameError)
                            .font(.caption)
                            .foregroundStyle(.red)
                    }
                }

                VStack(alignment: .leading, spacing: 4) {
                    TextField(text: $model.lastName, prompt: Text("Nachname")) {
                        Text("Nachname")
                    }
                    .focused($focusedField, equals: .lastName)
                    .textContentType(.familyName)
                    .submitLabel(.next)
                    .onSubmit { focusedField = .email }

                    if let lastNameError {
                        Text(lastNameError)
                            .font(.caption)
                            .foregroundStyle(.red)
                    }
                }

                VStack(alignment: .leading, spacing: 4) {
                    TextField(text: $model.email, prompt: Text("E-Mail")) {
                        Text("E-Mail")
                    }
                    .focused($focusedField, equals: .email)
                    .textInputAutocapitalization(.never)
                    .disableAutocorrection(true)
                    .keyboardType(.emailAddress)
                    .textContentType(.emailAddress)
                    .submitLabel(.done)
                    .onSubmit { Task { await save() } }

                    if let emailError {
                        Text(emailError)
                            .font(.caption)
                            .foregroundStyle(.red)
                    }
                }
            }
            Section("Optional") {
                TextField("Unternehmen", text: $model.companyName)
                    .textContentType(.organizationName)
                TextField("Position", text: $model.title)
                    .textContentType(.jobTitle)
            }

            if let errorMessage {
                Section {
                    Text(errorMessage).foregroundStyle(.red)
                }
            }

            Section {
                Button {
                    Task { await save() }
                } label: {
                    Text(isSaving ? "Speichern…" : "Lead erstellen")
                        .frame(maxWidth: .infinity)
                }
                .disabled(isSaving)
                .primaryActionButtonStyle()
            }
        }
        .navigationTitle("Neuer Lead")
        .navigationDestination(item: $createdDetail) { detail in
            LeadDetailView(store: store, partyId: detail.partyId)
        }
    }

    private func firstInvalidField() -> Field? {
        if trimmedFirstName.isEmpty { return .firstName }
        if trimmedLastName.isEmpty { return .lastName }
        if trimmedEmail.isEmpty || !hasValidEmailFormat { return .email }
        return nil
    }

    private func save() async {
        attemptedSubmit = true
        errorMessage = nil

        if let invalidField = firstInvalidField() {
            focusedField = invalidField
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
