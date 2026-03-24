FROM node:22-bookworm AS deps

WORKDIR /app
COPY modern/web/package.json /app/package.json
COPY modern/web/package-lock.json /app/package-lock.json

RUN npm ci

FROM node:22-bookworm AS build

WORKDIR /app
ENV NEXT_TELEMETRY_DISABLED=1

COPY --from=deps /app/node_modules /app/node_modules
COPY modern/web /app

RUN npm run build

FROM node:22-bookworm-slim

WORKDIR /app
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1

COPY --from=build /app /app

EXPOSE 3000
CMD ["npm", "run", "start", "--", "--hostname", "0.0.0.0", "--port", "3000"]
