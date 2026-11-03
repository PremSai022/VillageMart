# VillageMart database

## Database and migrations

Production uses MySQL 8 with Flyway as the schema authority. The schema is under
`backend/src/main/resources/db/migration`. Hibernate runs with `ddl-auto: validate`; it does not create or
modify production tables. Money is `DECIMAL(14,2)`, quantities are `DECIMAL(14,3)`, and unit costs use
`DECIMAL(16,4)`. Timestamps are persisted in UTC and business dates use `Asia/Kolkata`.

| Migration | Purpose |
|---|---|
| `V1__initial_schema.sql` | Owner/security, business settings, catalog, trolley location, purchases, inventory/movements, sales, returns, payments, expenses, notifications and idempotency |
| `V2__seed_reference_data.sql` | Editable default categories, units, expense categories, trolley location and document-number sequences |

## Entity groups and relationships

- **Owner and settings:** `users`, `refresh_tokens`, `business_settings`.
- **Catalog:** `categories` and `units` are referenced by `products`; product SKU and barcode have unique constraints.
- **Inventory:** `stock_locations` represents the current trolley; `inventory` has a unique `(product_id, location_id)` row; `stock_movements` is append-only audit history.
- **Purchases:** `suppliers` → `purchases` → `purchase_items`; purchase returns reference the original purchase and items.
- **Sales:** `customers` and `villages` are optional links from `sales`; `sales` → `sale_items`; sales returns reference their original sale and sale items.
- **Payments:** each payment records method, direction, type, date and optional sale/customer/return references. Credit owed is tracked separately from cash received.
- **Expenses:** expense categories and `expenses`, optionally associated with village.
- **Notifications:** persisted notification records and device tokens support read/unread state and transition-based stock alerts.
- **Idempotency and numbering:** `idempotency_records` enforces operation/key uniqueness; `document_sequences` allocates invoice and return numbers under a lock.

Foreign keys, status fields, non-negative quantity checks, unique keys and indexes are defined in V1. Completed
business transactions are not physically deleted; correction is done through cancel/return workflows.

## Inventory and cost calculations

All stock changes use the transactional `InventoryService`. It locks inventory rows for update, applies movements,
rejects negative quantities, and records the previous/new quantity and transaction reference. A sale snapshots the
weighted-average unit cost before stock deduction.

For a completed purchase, transport/loading and other landed costs are allocated to item lines. The weighted average
cost for an inbound movement is:

```text
new average = ((old quantity × old average) + (received quantity × landed unit cost))
              / (old quantity + received quantity)
```

Sale gross profit uses the sale-time cost snapshot, not the current product price or latest supplier price. A saleable
sales return restores stock; damaged goods do not. Purchase returns and cancellations reverse stock through audited
movements.

## Test database

Backend integration tests use H2 in MySQL compatibility mode and execute the same Flyway migrations. A MySQL 8
Testcontainers dependency is available for environments with Docker; this workstation has no Docker daemon, so a live
MySQL/Testcontainers run was not performed.
