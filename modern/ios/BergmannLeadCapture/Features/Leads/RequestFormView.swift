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
        attemptedSubmit && trimmedName.isEmpty ? "Request name is required." : nil
    }
    private var lineItemsError: String? {
        attemptedSubmit && lines.isEmpty ? "At least one line item is required." : nil
    }

    var body: some View {
        Form {
            Section("Request") {
                VStack(alignment: .leading, spacing: 4) {
                    TextField("Request name", text: $name)
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
                TextField("Description", text: $description, axis: .vertical)
                    .disabled(isLocked)
            }

            Section("Line Items") {
                ForEach($lines) { $line in
                    VStack(alignment: .leading, spacing: 4) {
                        HStack(alignment: .firstTextBaseline, spacing: 12) {
                            TextField("Description", text: $line.description)
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
                        TextField("Quantity", value: $line.quantity, format: .number)
                            .keyboardType(.decimalPad)
                        TextField("Unit price", value: $line.unitPrice, format: .currency(code: "EUR"))
                            .keyboardType(.decimalPad)

                        if attemptedSubmit && line.description.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                            Text("Each line needs a description.")
                                .font(.caption)
                                .foregroundStyle(.red)
                        }
                        if attemptedSubmit && line.quantity == nil {
                            Text("Each line needs a quantity.")
                                .font(.caption)
                                .foregroundStyle(.red)
                        }
                        if attemptedSubmit && line.unitPrice == nil {
                            Text("Each line needs a unit price.")
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

                Button("Add line") {
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
                    Text(isSaving ? "Saving…" : "Save request")
                        .frame(maxWidth: .infinity)
                }
                .disabled(isSaving || isLocked)
                .primaryActionButtonStyle()
            }
        }
        .navigationTitle("Customer Request")
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
