import XCTest
@testable import BergmannLeadCapture

@MainActor
final class LeadStoreTests: XCTestCase {
    func testNewLeadValidationHappyPath() async throws {
        let store = LeadStore(apiClient: .init(
            listOpportunities: { [] },
            getOpportunity: { _ in throw URLError(.badURL) },
            createOpportunity: { _ in
                OpportunityDetail(
                    partyId: "P100",
                    displayName: "New Lead",
                    email: "new@example.com",
                    companyPartyId: nil,
                    companyName: nil,
                    stage: .NEW,
                    nextAction: "Add request",
                    request: nil
                )
            },
            saveRequest: { _, _ in throw URLError(.badURL) }
        ))

        let created = try await store.createLead(CreateLeadRequest(firstName: "A", lastName: "B", email: "a@b.com"))
        XCTAssertEqual(created.partyId, "P100")
    }
}
