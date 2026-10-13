# Livora E-commerce Backend (updated)

This update preserves the existing Spring Boot/JPA code and adds JDBC-backed endpoints for **Shopping Cart, Inventory, Orders, simulated Payments/Refunds, Delivery, and Product Feedback**. The Promotions module is intentionally not included.

## Requirements
- Java 17 or later
- Maven 3.9+
- MySQL 8+
- Frontend runs at `http://localhost:5173`

## 1. Create/update the database
1. Back up any existing `ecommerce_db` database.
2. Open MySQL Workbench or the MySQL command line.
3. Run `sql/ecommerce_db.sql`.
4. If you already have data, inspect the existing table definitions first. The script uses `CREATE TABLE IF NOT EXISTS`; it does not alter an existing table to add missing columns.
5. Existing product rows receive demo stock of 10 units on the first run of the inventory seed statement. Change these quantities to match your actual inventory. Newly created products start at zero stock.

The script excludes Promotions/coupons. It uses the existing project model in which price is stored on `products` and inventory is keyed directly to `product_id`; this is intentionally compatible with the current source ZIP and is a simpler model than the documentation's future variant/SKU design.

## 2. Configure the database password
Set environment variables before running the app. Example in PowerShell:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/ecommerce_db"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your-local-mysql-password"
```

The committed `application.properties` contains no real database password.

## 3. Run the backend
From this folder:

```bash
mvn clean spring-boot:run
```

The API listens on port `8090`.

## 4. Run the frontend
In the frontend project folder:

```bash
npm install
npm run dev
```

Set `VITE_API_BASE`, `VITE_AUTH_BASE`, `VITE_CUSTOMER_BASE`, or `VITE_COMMERCE_BASE` in `.env` if your local endpoints differ. The defaults match the supplied source ZIP.

## Added endpoints
- `GET /rest/api/v1/me/cart?userId={id}`
- `POST /rest/api/v1/me/cart/items?userId={id}` with `{ "productId": 1, "quantity": 1 }`
- `PATCH /rest/api/v1/me/cart/items/{productId}?userId={id}` with `{ "quantity": 2 }`
- `DELETE /rest/api/v1/me/cart/items/{productId}?userId={id}`
- `GET /rest/api/v1/inventory?userId={adminId}`
- `PATCH /rest/api/v1/inventory/{productId}?userId={adminId}` with `{ "delta": 5 }`
- `POST /rest/api/v1/me/orders?userId={id}` with `addressId`, `paymentMethod`, and `items` containing `productId`/`quantity`
- `GET /rest/api/v1/me/orders?userId={id}` and `GET /rest/api/v1/me/orders/{orderId}?userId={id}`
- `POST /rest/api/v1/me/orders/{orderId}/cancellation?userId={id}` (full cancellation only while status is CONFIRMED; simulated refund and stock restoration)
- `PATCH /rest/api/v1/admin/shipments/{shipmentId}?userId={adminId}` with `{ "status": "SHIPPED" }`
- `GET /rest/api/v1/products/{productId}/reviews`
- `POST /rest/api/v1/products/{productId}/reviews?userId={id}` with `{ "rating": 5, "title": "Great", "comment": "..." }` (requires a delivered order)

## Important limitations
- Payments are simulated only; no real payment provider is contacted.
- The original application uses client-supplied `userId` query parameters rather than a complete Spring Security session/JWT authorization model. Do not deploy this as-is to production. Production work must enforce identity/ownership on the server and add authorization, validation, rate limiting, and automated tests.
- Inventory and order logic use the current single-product price model, not product variants. The original documentation describes a richer variant/SKU schema; implementing that fully would require a catalogue migration and frontend variant selection.
- Run tests and verify your local MySQL schema before demonstration. This archive was not compiled in this environment because Maven is not installed here.


## Admin dashboard and customer feedback update
- Admin dashboard endpoints: `/rest/api/v1/admin/dashboard`, `/admin/users`, `/admin/orders`, and `/admin/feedback` (each requires `?userId=<admin user id>`).
- Customer feedback endpoint: `POST /rest/api/v1/feedback?userId=<customer id>`.
- Existing databases: run `sql/update_customer_feedback.sql` once before using the dashboard feedback panel. The full `sql/ecommerce_db.sql` also includes the table for fresh installs. Back up your database before changing schemas.
- Admin checkout/cart mutation endpoints are rejected server-side; the UI hides cart/purchase actions for Admin. Customer accounts retain checkout.
- Payments are a demonstration simulation, not a real payment gateway. Do not collect real card details or deploy as a production payment system.
