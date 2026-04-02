import Foundation

@MainActor
final class LeadStore: ObservableObject {
    @Published var leads: [OpportunityListItem] = []
    @Published var isLoading: Bool = false
    @Published var errorMessage: String?

    private let apiClient: OpportunityAPIClient

    init(apiClient: OpportunityAPIClient) {
        self.apiClient = apiClient
    }

    func loadLeads() async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }
        do {
            leads = try await apiClient.listOpportunities()
        } catch {
            errorMessage = (error as? ApiErrorResponse)?.message ?? error.localizedDescription
        }
    }

    func createLead(_ request: CreateLeadRequest) async throws -> OpportunityDetail {
        let created = try await apiClient.createOpportunity(request)
        await loadLeads()
        return created
    }

    func loadDetail(_ partyId: String) async throws -> OpportunityDetail {
        try await apiClient.getOpportunity(partyId)
    }

    func saveRequest(_ partyId: String, request: OpportunityRequestInput) async throws -> OpportunityDetail {
        let updated = try await apiClient.saveRequest(partyId, request)
        await loadLeads()
        return updated
    }
}
