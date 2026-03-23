# Order Lifecycle View Migration

This directory contains a new cloud-native application that extracts only the OFBiz order lifecycle view into a focused bounded context.

The stack is:

- Spring Boot 4.0.x with Kotlin and Java 21
- Next.js 16 with TypeScript, React 19, and shadcn-style components
- PostgreSQL 18
- Docker Compose for local development
- Helm for Kubernetes deployment

The application intentionally covers only this slice of order management:

- create order
- track status through `PENDING -> PAID -> SHIPPED -> COMPLETED`
- view timeline and event history
- perform basic edits: update, cancel, refund

## Structure

- `backend/`: Spring Boot API, Flyway migrations, seed logic, tests
- `frontend/`: Next.js 16 App Router UI, server actions, tests, Dockerfile
- `infra/helm/order-lifecycle/`: Kubernetes chart
- `docs/ofbiz-mapping.md`: OFBiz-to-new-domain mapping
- `compose.yaml`: local development stack

## Prerequisites

- Java 21
- Node.js 22
- Docker and Docker Compose
- PostgreSQL 18 if you want to run without Compose

## Local Development

### Option 1: Docker Compose

From `migration/`:

```bash
docker compose up --build
```

Endpoints:

- Frontend: [http://localhost:3000](http://localhost:3000)
- Backend API: [http://localhost:8080](http://localhost:8080)
- OpenAPI: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- Swagger UI: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

The backend starts with deterministic demo data enabled in Compose.

### Option 2: Run services directly

Backend:

```bash
cd backend
./gradlew bootRun
```

Frontend:

```bash
cd frontend
npm ci
BACKEND_URL=http://localhost:8080 npm run dev
```

Default database configuration:

- host: `localhost`
- port: `5432`
- database: `order_lifecycle`
- username: `order_lifecycle`
- password: `order_lifecycle`

## Demo Data

Deterministic demo data is loaded explicitly through the backend seed mode. The dataset includes:

- one pending order
- one paid order
- one shipped order
- one completed order
- one cancelled order
- one completed order with a partial refund
- three customers
- three products

Run the seed task locally:

```bash
cd backend
./gradlew seedDemoData
```

Or run the application once in seed-only mode:

```bash
APP_SEED_ENABLED=true APP_SEED_ONLY=true ./gradlew bootRun
```

The seed logic is idempotent for already-populated environments.

## API and OpenAPI Client

The backend exposes:

- `POST /api/orders`
- `GET /api/orders`
- `GET /api/orders/{id}`
- `PATCH /api/orders/{id}`
- `POST /api/orders/{id}/actions/pay`
- `POST /api/orders/{id}/actions/ship`
- `POST /api/orders/{id}/actions/complete`
- `POST /api/orders/{id}/actions/cancel`
- `POST /api/orders/{id}/actions/refund`

Frontend types are generated from the backend OpenAPI spec:

```bash
cd frontend
OPENAPI_URL=http://localhost:8080/v3/api-docs npm run generate:api
```

The checked-in `src/lib/api/generated.ts` is the current generated snapshot used by the UI.

## Testing

Backend unit tests:

```bash
cd backend
./gradlew test
```

Backend integration tests with Docker and Testcontainers:

```bash
cd backend
./gradlew integrationTest
```

Run the full backend verification suite:

```bash
cd backend
./gradlew check
```

Frontend:

```bash
cd frontend
npm ci
npm run lint
npm run typecheck
npm run test
npm run build
```

Playwright end-to-end tests:

```bash
cd frontend
PLAYWRIGHT_BASE_URL=http://localhost:3000 npm run test:e2e
```

## Building Images

Backend images use Spring Boot OCI image support:

```bash
cd backend
./gradlew bootBuildImage --imageName order-lifecycle-backend:local
```

Frontend images use the multi-stage Dockerfile around Next.js standalone output:

```bash
docker build -t order-lifecycle-frontend:local frontend
```

## Deployment

The Helm chart is at `infra/helm/order-lifecycle`.

Render and lint:

```bash
helm lint infra/helm/order-lifecycle
helm template order-lifecycle infra/helm/order-lifecycle
```

Install:

```bash
helm upgrade --install order-lifecycle infra/helm/order-lifecycle \
  --set frontend.image.repository=ghcr.io/your-org/order-lifecycle-frontend \
  --set frontend.image.tag=latest \
  --set backend.image.repository=ghcr.io/your-org/order-lifecycle-backend \
  --set backend.image.tag=latest \
  --set database.host=postgres.example.internal \
  --set database.name=order_lifecycle \
  --set database.existingSecret.name=order-lifecycle-db \
  --set ingress.host=orders.example.com
```

Enable seed loading during install or upgrade:

```bash
helm upgrade --install order-lifecycle infra/helm/order-lifecycle \
  --set seed.enabled=true
```

The chart expects PostgreSQL to already exist in production. Only local Compose provisions PostgreSQL directly.

## Environment Variables

Backend:

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`
- `SERVER_PORT`
- `FRONTEND_ORIGIN`
- `APP_SEED_ENABLED`
- `APP_SEED_ONLY`

Frontend:

- `BACKEND_URL`
- `PORT`

## CI

The root workflow [`.github/workflows/migration-order-lifecycle.yml`](/Users/marcus/code/schiesser/ofbiz-framework/.github/workflows/migration-order-lifecycle.yml) runs only when `migration/**` changes and covers:

- backend unit tests via `./gradlew test`
- backend Docker-backed integration tests via `./gradlew integrationTest`
- frontend lint, typecheck, tests, and build
- Playwright E2E
- Helm lint and render validation
- backend and frontend image builds

## Assumptions

- no authentication in v1
- no live migration of OFBiz order data in v1
- simplified lifecycle values instead of OFBiz status ids
- refunds stay financial and auditable instead of becoming lifecycle states
- editing is restricted to pre-shipment states

## References

- [Spring Boot 4.0 release blog](https://spring.io/blog/2025/11/20/spring-boot-4-0-0-available-now/)
- [Spring Boot container images docs](https://docs.spring.io/spring-boot/reference/packaging/container-images/index.html)
- [Spring Boot Kotlin support docs](https://docs.spring.io/spring-boot/reference/features/kotlin.html)
- [Spring Boot Testcontainers docs](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html)
- [Next.js 16 upgrade guide](https://nextjs.org/docs/app/guides/upgrading/version-16)
- [Next.js self-hosting guide](https://nextjs.org/docs/app/guides/self-hosting)
- [Next.js output standalone docs](https://nextjs.org/docs/app/api-reference/config/next-config-js/output)
- [Next.js testing overview](https://nextjs.org/docs/app/guides/testing)
- [shadcn/ui Next.js install docs](https://ui.shadcn.com/docs/installation/next)
- [shadcn/ui Tailwind v4 docs](https://ui.shadcn.com/docs/tailwind-v4)
- [PostgreSQL downloads](https://www.postgresql.org/download/)
