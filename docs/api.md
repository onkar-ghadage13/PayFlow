# API overview

The payment core and insights service use the `/api` prefix. JSON errors include a `message` when the backend can provide one. Register or sign in first; protected routes require `Authorization: Bearer <token>`. Insights endpoints are served by the separate insights service; the other routes below are served by payment core.

| Area | Routes | Purpose |
| --- | --- | --- |
| Authentication | `POST /api/auth/register`, `POST /api/auth/login` | Create an account or receive an access token |
| Profile | `GET /api/users/me`, `PUT /api/users/me` | Get or update the authenticated user's profile |
| Recipient lookup | `GET /api/users/lookup?emailToFind=...` | Find a registered transfer recipient |
| Wallet | `GET /api/wallet/{userId}`, `POST /api/wallet/deposit`, `POST /api/wallet/withdraw` | Read own balance and perform simulated wallet operations |
| Transfers | `POST /api/payments` | Send money to another registered user; include an `Idempotency-Key` header |
| Payments | `GET /api/payments/user/{userId}`, `GET /api/payments/{transactionId}` | List own transactions or read an individual payment |
| Journey | `GET /api/transactions/{transactionId}/events` | Read events for a payment journey |
| Notifications | `GET /api/notifications` | List the user's in-app notifications |
| Notifications | `PUT /api/notifications/{notificationId}/read`, `PUT /api/notifications/read-all` | Mark one or all of the user's notifications as read |
| Dashboard | `GET /api/dashboard` | Read wallet and transaction summary values |
| Insights | `GET /api/insights/summary?period=day\|month\|year`, `GET /api/insights/categories?period=...` | Read categorized spending summaries |
| Admin | `GET /api/admin/users`, `PUT /api/admin/users/{userId}/role?role=USER\|ADMIN`, `GET /api/admin/payments` | Admin-only account and recent payment review |

The payment core publishes successful and failed payment events to Kafka topic `payflow.payment.events` after the database transaction commits. Notification service consumes the topic and writes in-app notifications. Insights service consumes successful-payment events and updates the category using Gemini when configured, then serves the authenticated summary endpoints. Without `GEMINI_API_KEY`, it uses the local category rules and comparison-based suggestions.

The frontend service module is the source of request shapes used by the UI. Deposit and withdrawal flows are simulated. Do not connect real payment handling without a suitable payment provider and its security/compliance requirements.

The first administrator is bootstrapped by setting `PAYFLOW_ADMIN_EMAIL` to an already registered account's email and restarting the backend. Admin access is enforced on the server; registering through the public form never grants the ADMIN role.


## Failure recovery and reconciliation

- GET /api/transactions/{transactionId}/recovery assesses a transaction using its status and lifecycle events, then returns a reason and recommended action: WAIT, RETRY, REVERSE, MANUAL_REVIEW, or NONE. Recommendations are advisory; the service does not move funds or retry automatically.
- GET /api/admin/reconciliation is admin-only and read-only. It checks payment terminal events, user references, amounts, and compares successful payment ledger totals with wallet balances.
