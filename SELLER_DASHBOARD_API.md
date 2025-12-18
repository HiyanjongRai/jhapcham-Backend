# Seller Dashboard API Documentation

## Overview

Comprehensive seller dashboard endpoint that provides income tracking, order statistics, and product metrics.

---

## Endpoint: Get Seller Dashboard Statistics

### Request

```
GET /api/seller/{sellerUserId}/dashboard
```

**Path Parameters:**

- `sellerUserId` (Long) - The user ID of the seller

**No request body required**

---

### Response (Success - 200 OK)

```json
{
  "totalIncome": 150000.0,
  "totalShippingCost": 15000.0,
  "netIncome": 135000.0,
  "totalOrders": 45,
  "deliveredOrders": 38,
  "pendingOrders": 3,
  "processingOrders": 2,
  "shippedOrders": 1,
  "canceledOrders": 1,
  "totalProducts": 25,
  "activeProducts": 22,
  "inactiveProducts": 3,
  "last30DaysIncome": 25000.0,
  "last30DaysOrders": 8
}
```

**Response Fields:**

### Income Metrics

- `totalIncome` (BigDecimal) - Total gross revenue from all delivered orders
- `totalShippingCost` (BigDecimal) - Total shipping charges from all delivered orders
- `netIncome` (BigDecimal) - Net income after subtracting shipping costs from total income

### Order Statistics

- `totalOrders` (Long) - Total number of orders (all statuses)
- `deliveredOrders` (Long) - Number of successfully delivered orders
- `pendingOrders` (Long) - Orders with status "NEW"
- `processingOrders` (Long) - Orders with status "PROCESSING"
- `shippedOrders` (Long) - Orders with status "SHIPPED_TO_BRANCH" or "OUT_FOR_DELIVERY"
- `canceledOrders` (Long) - Orders with status "CANCELED"

### Product Metrics

- `totalProducts` (Long) - Total number of products in seller's catalog
- `activeProducts` (Long) - Products with status "ACTIVE"
- `inactiveProducts` (Long) - Products with status "INACTIVE"

### Recent Activity (Last 30 Days)

- `last30DaysIncome` (BigDecimal) - Net income from orders delivered in the last 30 days
- `last30DaysOrders` (Long) - Count of orders delivered in the last 30 days

---

### Response (Error - 400 Bad Request)

```json
{
  "message": "Seller not found"
}
```

or

```json
{
  "message": "Seller profile not found"
}
```

---

### Response (Error - 500 Internal Server Error)

```json
{
  "message": "Failed to load dashboard statistics"
}
```

---

## How Income is Calculated

### Income Tracking Flow

1. **When an order is delivered:**

   - The `applySellerAccounting()` method is called in `OrderService`
   - This happens when branch updates order status to "DELIVERED"

2. **Per-order calculations:**

   - `sellerGrossAmount` = Sum of all order item line totals
   - `sellerShippingCharge` = Order's shipping fee
   - `sellerNetAmount` = Gross amount - Shipping charge

3. **Profile accumulation:**

   - `totalIncome` += seller gross amount (revenue)
   - `totalShippingCost` += seller shipping charge
   - `netIncome` += seller net amount (actual earnings)

4. **Key points:**
   - Income is only counted for DELIVERED orders
   - Each order is only accounted once (checked via `sellerAccounted` flag)
   - Shipping costs are subtracted from revenue to get net income

---

## Example Frontend Usage

### Fetch Dashboard Stats

```javascript
async function fetchSellerDashboard(sellerUserId) {
  try {
    const response = await fetch(
      `http://localhost:8080/api/seller/${sellerUserId}/dashboard`
    );

    if (!response.ok) {
      throw new Error("Failed to fetch dashboard stats");
    }

    const stats = await response.json();
    console.log("Dashboard Stats:", stats);

    // Display income summary
    console.log(`Total Revenue: Rs. ${stats.totalIncome}`);
    console.log(`Shipping Costs: Rs. ${stats.totalShippingCost}`);
    console.log(`Net Income: Rs. ${stats.netIncome}`);

    // Display order summary
    console.log(`Total Orders: ${stats.totalOrders}`);
    console.log(`Delivered: ${stats.deliveredOrders}`);
    console.log(`Pending: ${stats.pendingOrders}`);

    return stats;
  } catch (error) {
    console.error("Error:", error);
    throw error;
  }
}
```

### Display Income Card (React Example)

```jsx
function IncomeCard({ stats }) {
  return (
    <div className="income-card">
      <h3>Income Overview</h3>

      <div className="metric">
        <span className="label">Total Revenue</span>
        <span className="value">Rs. {stats.totalIncome.toLocaleString()}</span>
      </div>

      <div className="metric">
        <span className="label">Shipping Charges</span>
        <span className="value negative">
          - Rs. {stats.totalShippingCost.toLocaleString()}
        </span>
      </div>

      <div className="metric highlight">
        <span className="label">Net Income</span>
        <span className="value">Rs. {stats.netIncome.toLocaleString()}</span>
      </div>

      <div className="metric-small">
        <span>From {stats.deliveredOrders} delivered orders</span>
      </div>
    </div>
  );
}
```

---

## Business Logic Notes

### Income Calculation

- **Total Income** = Sum of all product sales from delivered orders
- **Shipping Costs** = Accumulated shipping fees that the seller needs to pay
- **Net Income** = What the seller actually earns (Total Income - Shipping Costs)

### Order Status Flow

```
NEW → PROCESSING → SHIPPED_TO_BRANCH → OUT_FOR_DELIVERY → DELIVERED
                                                                ↓
                                                    Income is accounted here
```

### When Orders Can Be Canceled

- By Customer: When status is NEW or PROCESSING
- By Seller: At any status before DELIVERED
- Canceled orders DO NOT contribute to income

---

## Database Schema

### SellerProfile Table (Income Columns)

```sql
totalIncome         DECIMAL(38,2) NOT NULL DEFAULT 0,
totalShippingCost   DECIMAL(38,2) NOT NULL DEFAULT 0,
netIncome           DECIMAL(38,2) NOT NULL DEFAULT 0
```

### Order Table (Accounting Columns)

```sql
sellerGrossAmount    DECIMAL(38,2) NOT NULL,
sellerShippingCharge DECIMAL(38,2) NOT NULL,
sellerNetAmount      DECIMAL(38,2) NOT NULL,
sellerAccounted      BOOLEAN NOT NULL DEFAULT FALSE
```

---

## Related Endpoints

### Get Simple Income

```
GET /api/seller/{sellerUserId}/income
```

Returns only income metrics (legacy endpoint).

### Get Seller Profile

```
GET /api/seller/{sellerUserId}
```

Returns full seller profile with products list.

---

## Testing

### Manual Test with cURL

```bash
curl -X GET "http://localhost:8080/api/seller/1/dashboard" \
  -H "Content-Type: application/json"
```

### Expected Test Scenario

1. **Initial State**: All values should be 0 for new seller
2. **After First Delivery**: Income values should reflect the first order
3. **After Cancellation**: Canceled orders should not affect income
4. **30-Day Stats**: Should only count recent deliveries

---

## Future Enhancements (Suggestions)

- Add date range filtering for custom period statistics
- Include average order value
- Add revenue trends (week-over-week, month-over-month)
- Include customer retention metrics
- Add product performance rankings
- Include refund/return statistics
