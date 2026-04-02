import SwiftUI

struct RequestFormView: View {
    private enum Field: Hashable {
        case name
        case lineDescription(UUID)
    }

    @Environment(\.dismiss) private var dismiss
    @ObservedObject var store: LeadStore
    let detail: OpportunityDetail

    @State private var name: String = ""
    @State private var description: String = ""
    @State private var lines: [OpportunityRequestLineInput] = [
        OpportunityRequestLineInput(description: "", quantity: nil, unitPrice: nil)
    ]
    @State private var isSaving = false
    @State private var errorMessage: String?
    @State private var attemptedSubmit = false
    @FocusState private var focusedField: Field?

    private var isLocked: Bool { detail.request?.isLocked == true }
    private var trimmedName: String { name.trimmingCharacters(in: .whitespacesAndNewlines) }
    private var nameError: String? {
        attemptedSubmit && trimmedName.isEmpty ? "Name ist erforderlich." : nil
    }
    private var lineItemsError: String? {
        attemptedSubmit && lines.isEmpty ? "Mindestens eine Position ist erforderlich." : nil
    }

    var body: some View {
        Form {
            Section("Anfrage") {
                VStack(alignment: .leading, spacing: 4) {
                    TextField("Name", text: $name)
                    .focused($focusedField, equals: .name)
                    .submitLabel(.next)
                    .onSubmit {
                        if let firstLine = lines.first {
                            focusedField = .lineDescription(firstLine.id)
                        }
                    }
                    .disabled(isLocked)

                    if let nameError {
                        Text(nameError)
                            .font(.caption)
                            .foregroundStyle(.red)
                    }
                }
                TextField("Beschreibung", text: $description, axis: .vertical)
                    .disabled(isLocked)
            }

            Section("Positionen") {
                ForEach($lines) { $line in
                    VStack(alignment: .leading, spacing: 4) {
                        HStack(alignment: .firstTextBaseline, spacing: 12) {
                            TextField("Beschreibung", text: $line.description)
                            .focused($focusedField, equals: .lineDescription(line.id))
                            .submitLabel(.next)

                            Button(role: .destructive) {
                                deleteLine(id: line.id)
                            } label: {
                                Image(systemName: "trash")
                            }
                            .buttonStyle(.borderless)
                            .disabled(isLocked)
                        }
                        TextField("Menge", value: $line.quantity, format: .number)
                            .keyboardType(.decimalPad)
                        TextField("Einzelpreis", value: $line.unitPrice, format: .currency(code: "EUR"))
                            .keyboardType(.decimalPad)

                        if attemptedSubmit && line.description.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                            Text("Jede Position braucht eine Beschreibung.")
                                .font(.caption)
                                .foregroundStyle(.red)
                        }
                        if attemptedSubmit && line.quantity == nil {
                            Text("Jede Position braucht eine Menge.")
                                .font(.caption)
                                .foregroundStyle(.red)
                        }
                        if attemptedSubmit && line.unitPrice == nil {
                            Text("Jede Position braucht einen Einzelpreis.")
                                .font(.caption)
                                .foregroundStyle(.red)
                        }
                    }
                    .disabled(isLocked)
                }
                .onDelete { indexSet in
                    guard !isLocked else { return }
                    lines.remove(atOffsets: indexSet)
                }

                if let lineItemsError {
                    Text(lineItemsError)
                        .font(.caption)
                        .foregroundStyle(.red)
                }

                Button("Position hinzufuegen") {
                    lines.append(OpportunityRequestLineInput(description: "", quantity: nil, unitPrice: nil))
                }
                .disabled(isLocked)
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
                    Text(isSaving ? "Speichern…" : "Anfrage speichern")
                        .frame(maxWidth: .infinity)
                }
                .disabled(isSaving || isLocked)
                .primaryActionButtonStyle()
            }
        }
        .navigationTitle("Kundenanfrage")
        .onAppear {
            if let existing = detail.request {
                name = existing.name
                description = existing.description ?? ""
                if !existing.lines.isEmpty {
                    lines = existing.lines.map {
                        OpportunityRequestLineInput(
                            description: $0.description,
                            productId: $0.productId,
                            quantity: $0.quantity,
                            unitPrice: $0.unitPrice,
                            story: $0.story
                        )
                    }
                }
            }
        }
    }

    private func firstInvalidField() -> Field? {
        if trimmedName.isEmpty { return .name }
        if let line = lines.first(where: {
            $0.description.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                || $0.quantity == nil
                || $0.unitPrice == nil
        }) {
            return .lineDescription(line.id)
        }
        return nil
    }

    private func save() async {
        attemptedSubmit = true
        errorMessage = nil

        if let invalidField = firstInvalidField() {
            focusedField = invalidField
            return
        }

        if lines.isEmpty {
            return
        }

        isSaving = true
        defer { isSaving = false }
        do {
            let payload = OpportunityRequestInput(
                name: name,
                description: description.isEmpty ? nil : description,
                story: nil,
                lines: lines
            )
            _ = try await store.saveRequest(detail.partyId, request: payload)
            dismiss()
        } catch {
            errorMessage = (error as? ApiErrorResponse)?.message ?? error.localizedDescription
        }
    }

    private func deleteLine(id: UUID) {
        guard !isLocked else { return }
        lines.removeAll { $0.id == id }
    }
}
