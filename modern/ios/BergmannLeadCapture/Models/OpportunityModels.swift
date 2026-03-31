import Foundation

struct OpportunityListResponse: Codable {
    let opportunities: [OpportunityListItem]
}

struct OpportunityListItem: Codable, Identifiable, Hashable {
    let partyId: String
    let displayName: String
    let email: String?
    let companyName: String?
    let stage: OpportunityStage
    let nextAction: String
    let hasRequest: Bool
    let createdAt: Date?

    var id: String { partyId }
}

struct OpportunityDetail: Codable, Identifiable {
    let partyId: String
    let displayName: String
    let email: String
    let companyPartyId: String?
    let companyName: String?
    let stage: OpportunityStage
    let nextAction: String
    let request: OpportunityRequestDetail?

    var id: String { partyId }
}

enum OpportunityStage: String, Codable {
    case NEW
    case REQUEST_READY
    case QUOTE_READY

    var badgeTitle: String {
        switch self {
        case .NEW: return "New"
        case .REQUEST_READY: return "Request Ready"
        case .QUOTE_READY: return "Quote Ready"
        }
    }
}

struct CreateLeadRequest: Codable {
    var firstName: String = ""
    var lastName: String = ""
    var email: String = ""
    var companyName: String = ""
    var title: String = ""
    var dataSourceId: String = ""
}

struct OpportunityRequestInput: Codable {
    var name: String
    var description: String?
    var story: String?
    var lines: [OpportunityRequestLineInput]
}

struct OpportunityRequestLineInput: Codable, Identifiable {
    var id: UUID = UUID()
    var description: String
    var productId: String?
    var quantity: Decimal
    var unitPrice: Decimal
    var story: String?

    enum CodingKeys: String, CodingKey {
        case description, productId, quantity, unitPrice, story
    }
}

struct OpportunityRequestDetail: Codable {
    let requestId: String
    let name: String
    let description: String?
    let story: String?
    let lines: [OpportunityRequestLineDetail]
    let isLocked: Bool

    enum CodingKeys: String, CodingKey {
        case requestId
        case name
        case description
        case story
        case lines
        case isLocked = "locked"
    }
}

struct OpportunityRequestLineDetail: Codable, Identifiable {
    let seqId: String
    let description: String
    let productId: String?
    let quantity: Decimal
    let unitPrice: Decimal
    let story: String?
    let statusId: String

    var id: String { seqId }
}

struct ApiErrorResponse: Codable, Error {
    let code: String
    let message: String
    let details: [String]?
}
