# VillageMart API

Base path: `/api/v1`. JSON requests and responses use UTF-8. Except for login/refresh and health, requests require
`Authorization: Bearer <accessToken>`. Sale creation requires an `Idempotency-Key` header. List APIs are paginated
unless the endpoint returns a small reference list. Dates use ISO-8601 (`YYYY-MM-DD`).

## Authentication and owner

| Method | Path | Purpose |
|---|---|---|
| POST | `/auth/login` | Owner login (`login`, `password`) |
| GET | `/auth/registration-status` | Public; `{"registrationOpen": true}` only while no owner exists |
| POST | `/auth/register` | Public, single-owner: creates the first OWNER (`fullName`, `mobileNumber`, optional `email`, `password`, `businessName`, optional `businessAddress`, `acceptTerms`) and returns tokens (201). 403 `REGISTRATION_CLOSED` once an owner exists; 409 `MOBILE_ALREADY_REGISTERED`/`EMAIL_ALREADY_REGISTERED`; 400 `VALIDATION_FAILED` |
| POST | `/auth/refresh` | Rotate refresh token (`refreshToken`) |
| POST | `/auth/logout` | Revoke refresh token |
| GET | `/auth/me` | Current owner |
| POST | `/auth/change-password` | Change owner password |

## Dashboard and catalog

| Method | Path | Purpose |
|---|---|---|
| GET | `/dashboard/summary?period=TODAY` | Date-filtered dashboard figures and live stock alerts |
| GET/POST/PUT/DELETE | `/categories[/{id}]` | Category list/create/update/delete |
| GET/POST | `/units` | Unit list/create |
| GET/POST | `/products` | Search/filter/paginated products; create product |
| GET/PUT/DELETE | `/products/{id}` | Product detail/update; delete only when unused |
| GET | `/products/barcode/{barcode}` | Barcode lookup |
| PATCH | `/products/{id}/status` | Activate/deactivate |
| GET/PUT | `/settings/business` | Read/update invoice and GST settings |

Product search accepts `q`, `categoryId`, `active`, `page`, `size`, and `sort`.

## Suppliers, purchases and inventory

| Method | Path | Purpose |
|---|---|---|
| GET/POST | `/suppliers` | Search and create suppliers |
| GET/PUT | `/suppliers/{id}` | Supplier detail/update |
| GET | `/suppliers/{id}/purchases` | Supplier purchase history |
| GET/POST | `/purchases` | Purchase list/create (create may complete immediately) |
| GET/PUT | `/purchases/{id}` | Purchase detail/update draft |
| POST | `/purchases/{id}/complete` | Complete a draft purchase |
| POST | `/purchases/{id}/cancel` | Cancel with mandatory reason and stock reversal |
| POST/GET | `/purchases/{id}/returns` | Record/list purchase returns |
| GET | `/inventory` | Filtered/paginated stock |
| GET | `/inventory/summary` | Inventory totals |
| GET | `/inventory/low-stock` | Current low stock |
| GET | `/inventory/out-of-stock` | Current zero stock |
| GET | `/inventory/movements` | Paginated stock movement history |
| GET | `/inventory/{productId}` | Product inventory |
| GET | `/inventory/{productId}/movements` | Product movement history |
| POST | `/inventory/adjustments` | Reason-required audited adjustment |

## Sales, customers, villages, payments and expenses

| Method | Path | Purpose |
|---|---|---|
| GET/POST | `/sales` | Search sales/create an idempotent sale |
| GET | `/sales/{id}` | Sale/invoice detail |
| POST | `/sales/{id}/cancel` | Cancel and reverse stock/payment |
| POST/GET | `/sales/{id}/returns` | Create/list controlled sales returns |
| GET/POST | `/customers` | Search/create customers |
| GET/PUT | `/customers/{id}` | Customer detail/update |
| GET | `/customers/{id}/history` | Customer sales history |
| GET/POST | `/customers/{id}/payments` | View history/collect customer credit |
| GET/POST/PUT | `/villages[/{id}]` | Village list/create/update |
| GET/POST/PUT/DELETE | `/expenses[/{id}]` | Expenses CRUD (deleted records are soft-deleted) |
| GET/POST/PUT | `/expenses/categories[/{id}]` | Expense category list/create/update |
| GET | `/payments` | Filter payment ledger by customer/method/type/date |

The sale request includes `items`, `paymentMode` (`CASH`, `UPI`, `CREDIT`, `SPLIT_PAYMENT`), optional customer/village,
discounts, optional payment lines, and `priceOverrideConfirmed`. A repeated key and identical request returns the
original sale; reusing the key with a different payload returns an idempotency conflict.

## Reports, invoices and notifications

Report endpoints accept `format=json|pdf|xlsx` (default JSON) and date presets or `from`/`to`:

- `GET /reports/daily-sales` (alias `/reports/sales`)
- `GET /reports/monthly-sales`
- `GET /reports/product-sales`
- `GET /reports/category-sales`
- `GET /reports/village-sales` and `/reports/village-sales/{villageId}`
- `GET /reports/purchases`, `/stock`, `/low-stock`, `/out-of-stock`, `/expenses`
- `GET /reports/profit` (aliases `/gross-profit`, `/net-profit`)
- `GET /reports/customer-outstanding`, `/payment-collections`
- `GET /sales/{id}/invoice?inline=false` — PDF invoice

Date presets include `TODAY`, `YESTERDAY`, `THIS_WEEK`, `LAST_7_DAYS`, `THIS_MONTH`, `LAST_MONTH`,
`LAST_30_DAYS`, and `THIS_YEAR`.

| Method | Path | Purpose |
|---|---|---|
| GET | `/notifications` | Paginated notifications; optional `unreadOnly` |
| GET | `/notifications/active-alerts` | Active stock alerts |
| GET | `/notifications/unread-count` | Unread count |
| PATCH | `/notifications/{id}/read` | Mark one read |
| PATCH | `/notifications/read-all` | Mark all read |
| POST | `/notifications/devices` | Register FCM token |
| POST | `/notifications/devices/unregister` | Unregister FCM token |

## Common errors

```json
{
  "timestamp": "2026-11-03T10:15:30Z",
  "status": 422,
  "code": "INSUFFICIENT_STOCK",
  "message": "Requested quantity exceeds available stock",
  "path": "/api/v1/sales",
  "fieldErrors": []
}
```

Validation errors include `fieldErrors`. Business errors use 4xx responses; clients should display `message` and
must not assume a failed request succeeded.
