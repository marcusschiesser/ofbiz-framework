import Foundation

struct OpportunityAPIClient {
    let listOpportunities: () async throws -> [OpportunityListItem]
    let getOpportunity: (_ partyId: String) async throws -> OpportunityDetail
    let createOpportunity: (_ request: CreateLeadRequest) async throws -> OpportunityDetail
    let saveRequest: (_ partyId: String, _ request: OpportunityRequestInput) async throws -> OpportunityDetail

    static let live: OpportunityAPIClient = {
        let service = OpportunityAPIService(baseURL: URL(string: "http://localhost:8080")!)
        return OpportunityAPIClient(
            listOpportunities: { try await service.send(.listOpportunities, decode: OpportunityListResponse.self).opportunities },
            getOpportunity: { try await service.send(.getOpportunity($0), decode: OpportunityDetail.self) },
            createOpportunity: { try await service.send(.createOpportunity($0), decode: OpportunityDetail.self) },
            saveRequest: { try await service.send(.saveRequest($0, $1), decode: OpportunityDetail.self) }
        )
    }()
}

enum OpportunityEndpoint {
    case listOpportunities
    case getOpportunity(String)
    case createOpportunity(CreateLeadRequest)
    case saveRequest(String, OpportunityRequestInput)

    var request: URLRequest {
        switch self {
        case .listOpportunities:
            return URLRequest(url: URL(string: "/api/opportunities", relativeTo: OpportunityAPIService.baseURL)!)
        case let .getOpportunity(partyId):
            return URLRequest(url: URL(string: "/api/opportunities/\(partyId)", relativeTo: OpportunityAPIService.baseURL)!)
        case let .createOpportunity(payload):
            var request = URLRequest(url: URL(string: "/api/opportunities", relativeTo: OpportunityAPIService.baseURL)!)
            request.httpMethod = "POST"
            request.httpBody = try? OpportunityAPIService.encoder.encode(payload)
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            return request
        case let .saveRequest(partyId, payload):
            var request = URLRequest(url: URL(string: "/api/opportunities/\(partyId)/request", relativeTo: OpportunityAPIService.baseURL)!)
            request.httpMethod = "PUT"
            request.httpBody = try? OpportunityAPIService.encoder.encode(payload)
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            return request
        }
    }
}

final class OpportunityAPIService {
    static var baseURL: URL = URL(string: "http://localhost:8080")!
    static let decoder: JSONDecoder = {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        return decoder
    }()
    static let encoder: JSONEncoder = {
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        return encoder
    }()

    private let session: URLSession

    init(baseURL: URL, session: URLSession = .shared) {
        Self.baseURL = baseURL
        self.session = session
    }

    func send<Response: Decodable>(_ endpoint: OpportunityEndpoint, decode type: Response.Type) async throws -> Response {
        let (data, response) = try await session.data(for: endpoint.request)
        guard let http = response as? HTTPURLResponse else {
            throw URLError(.badServerResponse)
        }

        guard (200..<300).contains(http.statusCode) else {
            if let apiError = try? Self.decoder.decode(ApiErrorResponse.self, from: data) {
                throw apiError
            }
            throw URLError(.badServerResponse)
        }

        return try Self.decoder.decode(type, from: data)
    }
}
