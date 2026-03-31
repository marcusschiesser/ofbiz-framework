import SwiftUI

struct RequestFormView: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var store: LeadStore
    let detail: OpportunityDetail

    @State private var name: String = ""
    @State private var description: String = ""
    @State private var story: String = ""
    @State private var lines: [OpportunityRequestLineInput] = [
        OpportunityRequestLineInput(description: "", quantity: 1, unitPrice: 0)
    ]
    @State private var isSaving = false
    @State private var errorMessage: String?

    private var isLocked: Bool { detail.request?.isLocked == true }

    var body: some View {
        Form {
            Section("Request") {
                TextField("Request name", text: $name)
                    .disabled(isLocked)
                TextField("Description", text: $description, axis: .vertical)
                    .disabled(isLocked)
                TextField("Story", text: $story, axis: .vertical)
                    .disabled(isLocked)
            }

            Section("Line Items") {
                ForEach($lines) { $line in
                    VStack(alignment: .leading) {
                        TextField("Description", text: $line.description)
                        TextField("Quantity", value: $line.quantity, format: .number)
                            .keyboardType(.decimalPad)
                        TextField("Unit price", value: $line.unitPrice, format: .currency(code: "USD"))
                            .keyboardType(.decimalPad)
                    }
                    .disabled(isLocked)
                }
                .onDelete { indexSet in
                    guard !isLocked else { return }
                    lines.remove(atOffsets: indexSet)
                }

                Button("Add line") {
                    lines.append(OpportunityRequestLineInput(description: "", quantity: 1, unitPrice: 0))
                }
                .disabled(isLocked)
            }

            if let errorMessage {
                Section {
                    Text(errorMessage).foregroundStyle(.red)
                }
            }

            Section {
                Button(isSaving ? "Saving…" : "Save request") {
                    Task { await save() }
                }
                .disabled(isSaving || isLocked)
            }
        }
        .navigationTitle("Customer Request")
        .onAppear {
            if let existing = detail.request {
                name = existing.name
                description = existing.description ?? ""
                story = existing.story ?? ""
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

    private func validate() -> String? {
        if name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { return "Request name is required." }
        if lines.isEmpty { return "At least one line item is required." }
        if lines.contains(where: { $0.description.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }) { return "Each line needs a description." }
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
            let payload = OpportunityRequestInput(
                name: name,
                description: description.isEmpty ? nil : description,
                story: story.isEmpty ? nil : story,
                lines: lines
            )
            _ = try await store.saveRequest(detail.partyId, request: payload)
            dismiss()
        } catch {
            errorMessage = (error as? ApiErrorResponse)?.message ?? error.localizedDescription
        }
    }
}
