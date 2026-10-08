# Architecture

## Applications

- **Frontend:** React 19 and Vite. `src/context/AuthContext.jsx` owns the signed-in session, while `src/services/api.js` adds the bearer token and calls the REST API. Vite proxies `/api` to the local backend; the Docker Nginx configuration provides the same proxy.
- **Payment core:** Spring Boot 3 REST API, Spring Security, JWT, Spring Data JPA, and Bean Validation. It owns users, wallets, payments, idempotency, and payment journey records.
- **Notification service:** Separately runnable Spring Boot Kafka consumer. It turns payment events into in-app notifications; the current starter stores them in the shared MySQL schema.
- **Insights service:** Separately runnable Spring Boot REST/Kafka service. It validates JWTs for insights requests, aggregates spending, and uses Gemini for payment description classification and natural language insights when configured.
- **Storage:** MySQL 8.4 is the only database. Flyway manages the schema for the core API, notifications, and insights services.

## Request flow

The browser sends API requests through `/api`; Nginx/Vite route insights calls to the independent insights service. Public registration/login endpoints issue a JWT. The core validates the token, resolves the authenticated user, and updates wallets/payments transactionally. After commit it publishes a keyed payment event. Notification and insights consumers process that event independently; the notification consumer persists messages, while the insights consumer classifies successful descriptions with Gemini and updates the payment category.

## Local development and boundaries

Run the payment core, notification service, insights service, MySQL, and Kafka for the integrated workflow; Compose starts the whole stack. `GEMINI_API_KEY` stays server-side. When set, payment descriptions and aggregated spending totals are sent to Google's Gemini API. Without it, the insights service uses a local category classifier and deterministic comparison text. Simulated wallet credits are not real payment processing. The services currently share a MySQL schema, and event publishing is after-commit but does not yet use a durable outbox.
