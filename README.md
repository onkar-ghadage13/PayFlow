# PayFlow

PayFlow is a digital wallet application with user management, wallets, transfers, notifications, payment journey tracking, a transaction dashboard, and spending insights. The workspace contains a Spring Boot payment core plus independently runnable notification and insights services, with a React/Vite frontend.

## Project layout

```text
PlayFlow/
├── backend/                 Spring Boot payment core: users, wallets, payments, tracking
├── notification-service/    Kafka consumer that persists in-app notifications
├── insights-service/        Gemini-powered classification and spending summaries
├── frontend/                React + Vite single-page application
├── docs/                    Architecture, API, and data model notes
├── docker-compose.yml       Optional MySQL + backend + frontend stack
└── pom.xml                  IntelliJ/Maven workspace aggregator
```

The root Maven aggregator imports the backend and both independently runnable Spring Boot services. The payment core publishes payment events to Kafka after its MySQL transaction commits. Notification service consumes those events and writes in-app notifications; insights service consumes them to classify descriptions and provides the insights REST endpoint. All services currently use the same MySQL schema for this starter deployment. JWTs are checked by the core API and insights API.

## Run from IntelliJ IDEA

1. Open the `PlayFlow` folder, or open its root `pom.xml`, in IntelliJ IDEA and set Java 17.
2. Start MySQL and Kafka dependencies with `docker compose up -d db kafka`.
3. Run `PayFlowApplication` with `DB_HOST=localhost`, `DB_PORT=3306`, `DB_NAME=payflow`, `DB_USER=payflow`, `DB_PASSWORD=payflow-local`, `KAFKA_BOOTSTRAP_SERVERS=localhost:29092`, and a shared `PAYFLOW_JWT_SECRET`. MySQL is the default and only database; Flyway creates its schema on first startup.
4. Run `NotificationApplication` and `InsightsApplication` from their respective modules with the same MySQL/Kafka settings. Set `GEMINI_API_KEY` on InsightsApplication to enable Gemini.
5. In `frontend`, run `npm install` once, then `npm run dev`; open the URL Vite prints (usually `http://localhost:5173`).

The login screen's “Failed to fetch” message means the frontend cannot reach the API; start the Spring Boot app on port 8080. When Kafka was downloaded and started separately (outside Docker), set `KAFKA_BOOTSTRAP_SERVERS=localhost:9092` for all three Spring Boot run configurations; Docker Compose uses host port `29092` and its containers use `kafka:9092`.

The dashboard reads the signed-in user's actual wallet and payment records. A new user's wallet starts at ₹0 and their editable spending targets start at ₹5,000/day and ₹50,000/month. Use **Wallet → Add money** for a simulated credit, then make transfers to populate the dashboard. In **Insights**, adjust the daily/monthly limits, review outgoing transfers and incoming transfers separately, and see seven-day spend trends. Spending at 80% of a target is marked near-limit; reaching or exceeding it triggers a budget warning and suggestion. Wallet top-ups do not count as spending, and the app does not block transfers when a budget is exceeded. Deposits are simulated and do not move real money.

To bootstrap an administrator locally, register the account first, set `PAYFLOW_ADMIN_EMAIL` to that account's email in the backend run configuration, then restart Spring Boot and sign in again. The Admin page and `/api/admin/**` routes are restricted to ADMIN users. In Docker Compose, set `PAYFLOW_ADMIN_EMAIL` before starting or restart the backend after registering that account.

## Optional Docker stack (MySQL)

From the project root, set optional `GEMINI_API_KEY` and `PAYFLOW_ADMIN_EMAIL`, then run `docker compose up --build` and open `http://localhost:3000`. Compose starts MySQL, Kafka, the payment core, notification service, insights service, and the Nginx frontend. Local-only defaults are provided for `DB_PASSWORD`, `MYSQL_ROOT_PASSWORD`, and `PAYFLOW_JWT_SECRET`; set your own values before using this outside local development. Keep the JWT secret identical for the payment core and insights service.

## Implemented first phase

- User registration, login, profile, JWT authentication, and personal wallet creation.
- Wallet balance, simulated add-money and withdrawal flows.
- User-to-user transfers with an idempotency key and atomic wallet updates.
- Transaction history, payment journey events, and in-app notifications.
- Transaction dashboard, editable daily/monthly spending limits, transfer-in/out summaries, seven-day trends, and category-based day/month/year insights.

Failed transfers caused by insufficient balance are recorded with a failure journey and Kafka-generated notification. Retrying the same idempotency key returns the same result; reusing it with different payment details is rejected. Notification read states and dashboard failed counts are available. With `GEMINI_API_KEY`, Insights service classifies successful payment descriptions and generates personalized summaries for day/month/year totals, including daily/monthly limit context. Without a key or when Gemini is unavailable, it uses local description rules and budget-based guidance. When configured, transaction descriptions and aggregated spending totals are sent to Google's Gemini API for these features.

Real payments still require integration with an appropriate regulated payment provider. The current services share one MySQL schema; fully isolated per-service databases and an outbox/reconciliation pipeline are future hardening work.

See [docs/architecture.md](docs/architecture.md), [docs/api.md](docs/api.md), and [docs/data-model.md](docs/data-model.md) for the project details. Reconciliation, payment failure recovery, isolated per-service databases, a durable outbox, and deployment hardening remain future phases.
