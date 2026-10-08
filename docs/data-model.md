# Data model

The JPA entities are grouped in `backend/src/main/java/com/playflow/entity/Entities.java`.

| Entity | Role | Ownership / relationship |
| --- | --- | --- |
| User | Login identity, display name, email, password hash, and role | Owns a wallet and user-scoped records |
| Wallet | Currency and current balance | Belongs to one user |
| Payment | Deposit, withdrawal, or transfer amount, status, parties, and timestamps | Associated with the initiating user and, for transfers, the recipient |
| JourneyEvent | Ordered progress events for a payment | Belongs to a payment |
| Notification | In-app message and read state | Belongs to a user |

MySQL initializes from `backend/src/main/resources/db/migration/mysql/V1__initial_schema.sql` and validates the mapping at startup. Add a new Flyway migration for subsequent MySQL schema changes rather than editing a migration already used by an environment.
