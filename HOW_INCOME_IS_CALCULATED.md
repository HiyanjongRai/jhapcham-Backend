# 💰 How Total Income is Calculated

## Quick Answer

**Total Income** = Sum of all product sales from **DELIVERED** orders only

**Net Income** = Total Income - Total Shipping Costs

---

## Step-by-Step Flow

### 1️⃣ **Order is Created**

When a customer places an order:

- Order items are saved with their prices
- Order status is set to `NEW`
- **Income is NOT counted yet** ❌

### 2️⃣ **Order Progresses Through Statuses**

Order moves through different stages:

```
NEW → PROCESSING → SHIPPED_TO_BRANCH → OUT_FOR_DELIVERY
```

- **Income is still NOT counted** ❌

### 3️⃣ **Order is Delivered** ✅

When the delivery branch marks order as `DELIVERED`:

- Method `branchUpdateStatus()` is called
- Status changes to `DELIVERED`
- **`applySellerAccounting()` method is triggered**
- **This is when income is calculated and added!** ✅

---

## The Income Calculation Code

### Location

**File:** `OrderService.java`  
**Method:** `applySellerAccounting(Order order)` (lines 570-614)

### The Code Explained

```java
private void applySellerAccounting(Order order) {

    // Step 1: Check if already accounted (prevent double-counting)
    if (order.isSellerAccounted()) {
        return;  // Already counted, skip
    }

    // Step 2: Get the seller
    SellerProfile seller = order.getItems()
        .get(0)
        .getProduct()
        .getSellerProfile();

    // Step 3: Calculate GROSS amount (sum of all product prices)
    BigDecimal gross = BigDecimal.ZERO;
    for (OrderItem item : order.getItems()) {
        gross = gross.add(item.getLineTotal());
    }
    // gross = item1.price + item2.price + item3.price...

    // Step 4: Get shipping fee
    BigDecimal shipping = order.getShippingFee() != null
        ? order.getShippingFee()
        : BigDecimal.ZERO;

    // Step 5: Calculate NET amount (what seller actually earns)
    BigDecimal net = gross.subtract(shipping);

    // Step 6: Save to order (for record-keeping)
    order.setSellerGrossAmount(gross);
    order.setSellerShippingCharge(shipping);
    order.setSellerNetAmount(net);
    order.setSellerAccounted(true);  // Mark as counted

    // Step 7: ADD to seller's accumulated totals
    seller.setTotalIncome(
        seller.getTotalIncome().add(gross)  // OLD total + NEW gross
    );

    seller.setTotalShippingCost(
        seller.getTotalShippingCost().add(shipping)  // OLD shipping + NEW shipping
    );

    seller.setNetIncome(
        seller.getNetIncome().add(net)  // OLD net + NEW net
    );

    // Step 8: Save to database
    sellerProfileRepository.save(seller);
    orderRepository.save(order);
}
```

---

## Formula Breakdown

### Per Order (when delivered):

```
GROSS AMOUNT = Item1.lineTotal + Item2.lineTotal + ... + ItemN.lineTotal

Where:
  lineTotal = unitPrice × quantity

SHIPPING CHARGE = Order's shipping fee (based on location)

NET AMOUNT = GROSS AMOUNT - SHIPPING CHARGE
```

### Accumulated in Seller Profile:

```
TOTAL INCOME = Previous Total + New Gross Amount
TOTAL SHIPPING COST = Previous Total + New Shipping
NET INCOME = Previous Net + New Net Amount

Or simply:
NET INCOME = TOTAL INCOME - TOTAL SHIPPING COST
```

---

## Example Calculation

### Scenario: Seller has 3 delivered orders

#### **Order 1** (delivered yesterday)

- Product A: Rs. 1,000 × 2 = Rs. 2,000
- Product B: Rs. 500 × 1 = Rs. 500
- **Gross**: Rs. 2,500
- **Shipping**: Rs. 150
- **Net**: Rs. 2,350

#### **Order 2** (delivered today)

- Product C: Rs. 3,000 × 1 = Rs. 3,000
- **Gross**: Rs. 3,000
- **Shipping**: Rs. 200
- **Net**: Rs. 2,800

#### **Order 3** (delivered today)

- Product D: Rs. 800 × 3 = Rs. 2,400
- Product E: Rs. 1,200 × 1 = Rs. 1,200
- **Gross**: Rs. 3,600
- **Shipping**: Rs. 150
- **Net**: Rs. 3,450

### Seller's Profile Totals:

```
TOTAL INCOME = 2,500 + 3,000 + 3,600 = Rs. 9,100
TOTAL SHIPPING COST = 150 + 200 + 150 = Rs. 500
NET INCOME = 2,350 + 2,800 + 3,450 = Rs. 8,600

Or: NET INCOME = 9,100 - 500 = Rs. 8,600 ✓
```

---

## What Gets Counted vs What Doesn't

### ✅ **Counted as Income:**

- Orders with status `DELIVERED` ✓
- All product sales prices (including discounted prices)
- Multiple items in same order

### ❌ **NOT Counted as Income:**

- Orders with status `NEW` ❌
- Orders with status `PROCESSING` ❌
- Orders with status `SHIPPED_TO_BRANCH` ❌
- Orders with status `OUT_FOR_DELIVERY` ❌
- Orders with status `CANCELED` ❌
- Orders not yet delivered

### 🔒 **Prevents Double Counting:**

- Each order has a flag: `sellerAccounted`
- Once income is added from an order, `sellerAccounted = true`
- If method runs again, it checks this flag and skips (line 572-574)

---

## When Is Income Added?

Income is added **ONLY** when:

1. ✅ Order status is currently `OUT_FOR_DELIVERY`
2. ✅ Branch updates status to `DELIVERED`
3. ✅ Method `branchUpdateStatus()` is called
4. ✅ Line 217: `applySellerAccounting(order)` executes

### Code Location:

```java
// OrderService.java, line 199-224
@Transactional
public OrderSummaryDTO branchUpdateStatus(...) {
    ...
    if (order.getStatus() == OrderStatus.OUT_FOR_DELIVERY
        && next == OrderStatus.DELIVERED) {

        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredBranch(branch);

        applySellerAccounting(order);  // ← Income added HERE
    }
    ...
}
```

---

## Database Structure

### SellerProfile Table

Stores cumulative totals for each seller:

```sql
CREATE TABLE seller_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    store_name VARCHAR(255) NOT NULL,

    -- Income tracking fields:
    total_income DECIMAL(38,2) NOT NULL DEFAULT 0,      -- Sum of all gross
    total_shipping_cost DECIMAL(38,2) NOT NULL DEFAULT 0,  -- Sum of all shipping
    net_income DECIMAL(38,2) NOT NULL DEFAULT 0,           -- Sum of all net

    ...
);
```

### Order Table

Stores per-order amounts (for audit trail):

```sql
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    status VARCHAR(50) NOT NULL,

    -- Order totals (what customer pays):
    items_total DECIMAL(38,2) NOT NULL,
    shipping_fee DECIMAL(38,2) NOT NULL,
    grand_total DECIMAL(38,2) NOT NULL,

    -- Seller accounting (what seller earns):
    seller_gross_amount DECIMAL(38,2) NOT NULL,      -- Same as items_total
    seller_shipping_charge DECIMAL(38,2) NOT NULL,   -- Same as shipping_fee
    seller_net_amount DECIMAL(38,2) NOT NULL,        -- Gross - Shipping
    seller_accounted BOOLEAN NOT NULL DEFAULT FALSE, -- Prevents double-counting

    ...
);
```

---

## Important Notes

### 📌 Key Points:

1. **Only Delivered Orders Count**

   - Pending, processing, or shipped orders don't affect income
   - Canceled orders never count

2. **Shipping is a Cost**

   - Total Income includes shipping fees collected from customer
   - But Net Income subtracts them (seller pays for shipping)

3. **Cumulative Tracking**

   - SellerProfile stores running totals
   - Each delivered order adds to the total
   - Never decreases (except manual database edits)

4. **No Double Counting**

   - `sellerAccounted` flag ensures each order counted only once
   - Even if status changes multiple times

5. **Transactional Safety**
   - All updates are wrapped in `@Transactional`
   - If any part fails, entire operation rolls back
   - Ensures data consistency

---

## Testing Income Calculation

### Manual Test Steps:

1. **Check initial state:**

   ```sql
   SELECT total_income, total_shipping_cost, net_income
   FROM seller_profiles
   WHERE id = 1;
   ```

2. **Create and deliver an order:**

   - Customer places order (Rs. 5,000, shipping Rs. 200)
   - Status: NEW → PROCESSING → SHIPPED → OUT_FOR_DELIVERY
   - Branch marks as DELIVERED

3. **Check updated state:**

   ```sql
   SELECT total_income, total_shipping_cost, net_income
   FROM seller_profiles
   WHERE id = 1;
   ```

4. **Expected result:**
   ```
   total_income: +5,000 (previous + 5,000)
   total_shipping_cost: +200 (previous + 200)
   net_income: +4,800 (previous + 4,800)
   ```

---

## API Response

When you call the dashboard endpoint, you get:

```json
GET /api/seller/1/dashboard

{
  "totalIncome": 125000.00,        // ← Sum from ALL delivered orders
  "totalShippingCost": 12000.00,   // ← Sum of shipping from delivered orders
  "netIncome": 113000.00,          // ← totalIncome - totalShippingCost

  "deliveredOrders": 45,           // How many orders contributed to this
  ...
}
```

---

## Business Logic Summary

```
┌─────────────────────────────────────────────┐
│             ORDER LIFECYCLE                  │
├─────────────────────────────────────────────┤
│  NEW                                         │
│   ↓                         Income: ❌       │
│  PROCESSING                                  │
│   ↓                         Income: ❌       │
│  SHIPPED_TO_BRANCH                           │
│   ↓                         Income: ❌       │
│  OUT_FOR_DELIVERY                            │
│   ↓                         Income: ❌       │
│  DELIVERED ←────────────────Income: ✅       │
│               applySellerAccounting()        │
│               - Calculate gross              │
│               - Get shipping                 │
│               - Calculate net                │
│               - Add to seller totals         │
└─────────────────────────────────────────────┘
```

---

## Summary

**Total Income** is the cumulative sum of gross amounts from all delivered orders. Each time an order is marked as delivered, the seller's total income increases by the order's product total. Shipping costs are tracked separately and subtracted to give the net income (actual earnings).

The calculation happens automatically when the delivery branch confirms delivery, ensuring accurate and timely income tracking for sellers.
