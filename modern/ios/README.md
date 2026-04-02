# Bergmann Lead Capture (Native iOS)

This folder contains the native SwiftUI client for Bergmann sales reps.

## V1 scope
- No authentication (internal distribution only)
- No offline mode
- No edit/delete lifecycle flows
- Uses Spring Boot backend (`modern/backend`) as the single system of record

## App structure
- `BergmannLeadCapture/App`: app entrypoint and startup navigation
- `BergmannLeadCapture/Models`: DTOs aligned with `/api/opportunities` contracts
- `BergmannLeadCapture/Networking`: typed API client
- `BergmannLeadCapture/Features/Leads`: lead list, detail, new lead, and request form flows
- `BergmannLeadCaptureTests`: initial test target scaffolding

## Baseline target
Recommended iOS deployment target: **iOS 17+** for modern internal distribution.

## Backend endpoints used
- `GET /api/opportunities`
- `POST /api/opportunities`
- `GET /api/opportunities/{partyId}`
- `PUT /api/opportunities/{partyId}/request`
