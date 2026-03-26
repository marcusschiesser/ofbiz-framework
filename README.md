# OFBiz Docker Demo

Start the PostgreSQL-backed OFBiz demo from the repository root:

```bash
docker compose -f docker/examples/postgres-demo/docker-compose.yml up --build
```

Wait until the OFBiz logs show `is started and ready.`, then open:

- [https://localhost:8443/partymgr](https://localhost:8443/partymgr)
- Login: `admin` / `ofbiz`

Useful docs:

- Docker docs: [DOCKER.adoc](/Users/marcus/code/schiesser/ofbiz-framework/DOCKER.adoc)
- Online docs pointer: [OFBiz-Online-Documentation.adoc](/Users/marcus/code/schiesser/ofbiz-framework/OFBiz-Online-Documentation.adoc)
- Original root README backup: [README.adoc.bak](/Users/marcus/code/schiesser/ofbiz-framework/README.adoc.bak)

## Spring Boot 4 Backend

The repo now also contains a new Kotlin Spring Boot 4 backend in [modern/backend](/Users/marcus/.codex/worktrees/83b1/ofbiz-framework/modern/backend). It implements the v1 opportunity flow on top of the OFBiz data model with these endpoints:

- `POST /api/opportunities`
- `PUT /api/opportunities/{partyId}/request`
- `GET /api/opportunities/{partyId}`

By default the backend listens on `http://localhost:8081` and connects to the OFBiz Derby database at `runtime/data/derby/ofbiz`.

### Start the Backend

Start OFBiz first so the Derby database exists and is ready, then start the backend from the repository root:

```bash
modern/backend/gradlew -p modern/backend bootRun
```

Useful environment variables:

- `SERVER_PORT`: backend HTTP port, default `8081`
- `LEADFLOW_DB_URL`: JDBC URL, default `jdbc:derby:../runtime/data/derby/ofbiz;create=false`
- `LEADFLOW_DB_DRIVER`: JDBC driver, default `org.apache.derby.jdbc.EmbeddedDriver`
- `LEADFLOW_DB_SCHEMA`: schema, default `OFBIZ`
- `LEADFLOW_OFBIZ_BASE_URL`: OFBiz base URL used by compatibility checks, default `https://localhost:8443`

### Use the API

Create an opportunity shell:

```bash
curl -X POST http://localhost:8081/api/opportunities \
  -H 'Content-Type: application/json' \
  -d '{
    "firstName": "Ada",
    "lastName": "Lovelace",
    "email": "ada@example.com",
    "companyName": "Analytical Engines Ltd",
    "title": "Procurement Lead",
    "dataSourceId": "WEB_SITE"
  }'
```

Add or rewrite the request for an opportunity:

```bash
curl -X PUT http://localhost:8081/api/opportunities/PARTY_ID/request \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "Warehouse Equipment Request",
    "description": "Customer needs pricing for warehouse equipment.",
    "story": "Initial sales request",
    "lines": [
      {
        "description": "Round gizmo package",
        "quantity": 2,
        "unitPrice": 24.50,
        "story": "Primary request line"
      }
    ]
  }'
```

Read the assembled opportunity state:

```bash
curl http://localhost:8081/api/opportunities/PARTY_ID
```

### How Correctness Is Verified

The implementation is validated with OFBiz-runtime parity tests. These tests run the legacy OFBiz dispatcher flow and the new Spring Boot REST flow against the same Derby test database, read both results with the same snapshot reader, and assert that the normalized persisted business state is identical.

The main parity suite is [LeadCaptureParityTests.groovy](/Users/marcus/.codex/worktrees/83b1/ofbiz-framework/applications/marketing/src/main/groovy/org/apache/ofbiz/marketing/sfa/lead/test/LeadCaptureParityTests.groovy). The shared snapshot contract and reader used by both the legacy baseline and the parity suite are in [LeadCaptureSnapshots.groovy](/Users/marcus/.codex/worktrees/83b1/ofbiz-framework/applications/marketing/src/main/groovy/org/apache/ofbiz/marketing/sfa/lead/test/LeadCaptureSnapshots.groovy).

Run the parity suite from the repository root:

```bash
./gradlew -I /tmp/ofbiz-githooks-worktree.init.gradle \
  "ofbiz --test component=marketing --test suitename=leadcaptureparitytests --portoffset 100"
```
