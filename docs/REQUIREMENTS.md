# VillageMart – Requirements

## 1. Business context

VillageMart supports a single owner who buys groceries, household products and fancy items in bulk from
wholesale shops in town, loads them into one trolley/vehicle and sells them while travelling between villages.

| Setting | Value |
|---|---|
| Users | Single owner (role `OWNER`); architecture allows more users later |
| Platform | Android (Kotlin, Jetpack Compose) + Spring Boot REST backend + MySQL 8 |
| Inventory locations | One (`TROLLEY`); schema has `stock_locations` for future vehicles/warehouses |
| Currency / Country / TZ | INR (₹) / India / Asia/Kolkata |
| Language | English |
| Stock costing | Weighted average cost (landed cost) |
| Connectivity | Online-first, tolerant of temporary failures |
| GST | Optional and configurable (business is **not** assumed to be GST-registered) |

## 2. Functional requirements

### FR-1 Authentication
- Owner logs in with username **or** mobile number and password.
- BCrypt password hashing; JWT access token (short-lived) + opaque refresh token (stored hashed, rotated on use).
- Logout revokes the refresh token. No public registration.
- Initial owner is created at first start-up from environment variables (`OWNER_USERNAME`, `OWNER_PASSWORD`,
  `OWNER_MOBILE`, `OWNER_NAME`). If no owner exists and these are missing, the app logs a warning and nobody can log in.

### FR-2 Master data
- **Categories** – CRUD, seeded with: Rice and grains, Pulses and dal, Oil and ghee, Sugar and salt, Spices, Snacks,
  Beverages, Household products, Bath and personal care, Fancy items, Kitchen accessories, Other. Owner may add more.
- **Units** – KG, Gram, Litre, ML, Piece, Packet, Box, Bundle, Bottle, Bag, Dozen (each flags whether decimals are allowed).
- **Products** – name, SKU (unique), barcode (unique, optional), category, unit, description, image URL, purchase price,
  default selling price, optional minimum selling price, minimum stock, optional tax rate, active flag, timestamps.
  Search by name/SKU/barcode, filter by category/status, paginated. Delete only when never used; otherwise deactivate.
- **Suppliers** – name, mobile, address, optional GSTIN, notes; supplier-wise purchase history.
- **Customers** – name, mobile, village, address, notes; purchase history, outstanding balance, payment history.
- **Villages** – name, optional district, state, notes; village-wise sales, profit and customer count.
- **Expense categories** – Fuel, Vehicle maintenance, Loading, Unloading, Food, Miscellaneous (+ custom).

### FR-3 Wholesale purchases
- Create purchase as `DRAFT` or directly `COMPLETED`: supplier, optional supplier invoice number, purchase date,
  multiple lines (quantity, unit price, line discount), transport/loading charges, other charges, notes.
- Totals: line total = qty × price − discount; subtotal = Σ line totals; total = subtotal + transport + other charges.
- Only `COMPLETED` purchases affect inventory. Transport/other charges are allocated to lines proportionally
  to line value to obtain the **landed unit cost** used for weighted-average costing.
- Cancel a completed purchase → stock reversal (rejected if current stock is lower than the quantity to reverse).
- Duplicate supplier invoice numbers (same supplier, not cancelled) are rejected. Completed purchases are never deleted.
- Purchase returns reduce stock and record supplier return details.

### FR-4 Inventory
- One inventory row per product per location: available quantity, weighted average cost, stock status, last updated.
- All changes go through `InventoryService` with row-level locks (`SELECT … FOR UPDATE`) and create a
  `stock_movements` audit row (type, change, previous, new, reference, reason, user, timestamp).
- Movement types: `OPENING_STOCK`, `PURCHASE`, `SALE`, `SALE_RETURN`, `PURCHASE_RETURN`, `DAMAGE`,
  `MANUAL_ADJUSTMENT`, `CANCELLATION_REVERSAL`.
- Stock can never go negative. Manual adjustments require a reason.

### FR-5 Low-stock alerts
- `OUT_OF_STOCK` when quantity = 0; `LOW_STOCK` when 0 < quantity ≤ minimum stock; otherwise `NORMAL`.
- Status is recomputed on every movement; **transitions** into LOW/OUT create a persisted notification
  (and an FCM push when configured). Returning to NORMAL resolves the active alert; a later drop raises a new one.
- Suggested replenishment quantity = (2 × minimum stock) − available quantity (never negative).
- Endpoints: `GET /api/v1/inventory/low-stock`, `GET /api/v1/inventory/out-of-stock`.

### FR-6 Sales & billing
- Direct (anonymous) sale or customer sale; optional village; quick product search with category filter.
- Price defaults to product selling price; overriding requires an explicit `priceOverrideConfirmed=true` flag and
  may not go below the minimum selling price.
- Item discount and bill discount, optional GST, grand total.
- Payment modes: `CASH`, `UPI`, `CREDIT`, `SPLIT_PAYMENT` (multiple payment lines). Credit requires a customer.
- Snapshot of product name, SKU, unit, price and weighted cost on every sale item.
- Sale creation requires an `Idempotency-Key` header; repeating the same key returns the original invoice.
- Statuses: `COMPLETED`, `CANCELLED`, `PARTIALLY_RETURNED`, `FULLY_RETURNED`.
- Cancellation restores stock and reverses payments; editing completed sales is not allowed.
- Sales returns: per item, quantity ≤ remaining returnable quantity; saleable items are restocked,
  damaged ones are not. Refund reduces outstanding credit first, then is paid out.
- Customer credit collection: `POST /customers/{id}/payments`, allocated to oldest outstanding invoices.

### FR-7 Expenses
- Category, amount, date, description, optional receipt image URL, optional village. Soft-deletable.

### FR-8 Reports (JSON + PDF + Excel)
Today / yesterday / weekly / monthly / custom range sales, product-wise, category-wise, village-wise, purchases,
stock, low stock, out of stock, expenses, gross profit, net profit, customer outstanding, payment collections.

### FR-9 Dashboard
Today’s sales, gross & net profit, products, inventory quantity & value, low/out-of-stock counts and lists,
recent sales and purchases, today’s expenses, best sellers, village summary – all from backend, with date filter.

### FR-10 Invoices
Backend-generated PDF invoice (business name/address/mobile, invoice number/date, village, customer, lines with
qty/unit/rate/discount/total, payment method, paid, outstanding, thank-you note, GST fields when configured).
Shared from Android via share sheet / WhatsApp.

### FR-11 Notifications
Persisted notifications, unread count, mark read, FCM device-token registration, push retry for failures.

## 3. Business rules (non-negotiable)
1. No negative stock.
2. No duplicate invoice/payment processing (idempotency keys, unique constraints).
3. Every stock change has an audit row.
4. Completed transactions are never physically deleted.
5. Cancellation reverses stock and payments.
6. Returns reference the original invoice/purchase.
7. Historical prices are snapshots.
8. All money/quantity math uses `BigDecimal` (`DECIMAL` in MySQL).
9. Stock updates are transactional with pessimistic row locks.
10. Backend validates all quantities and prices.
11. Weighted average cost: `((oldQty × oldAvg) + (newQty × landedUnitCost)) / (oldQty + newQty)`.
12. Gross profit = revenue excluding tax − cost of goods sold (weighted cost snapshot).
13. Net profit = gross profit − expenses. Credit/outstanding is never counted as cash collected.

## 4. Non-functional requirements
- **Security**: JWT, BCrypt, secrets only via environment variables, encrypted token storage on Android, CORS restricted.
- **Reliability**: transactional services, idempotent sale creation, WorkManager retries on Android.
- **Performance**: indexed queries, pagination (default 20, max 100).
- **Maintainability**: layered packages per feature, DTOs + MapStruct, Flyway migrations, tests per module.
- **Observability**: Actuator health, structured logs, consistent error body (`ApiError`).
- **Usability**: billing in ≤ 3 screens, large touch targets, light/dark themes, loading/empty/error states.
