# Importing the Sequence Diagram into Draw.io

Because generating raw `.drawio` XML files directly from code produces heavily distorted visual layouts (since XML requires exact pixel X/Y coordinates for hundreds of boxes and lines), the easiest and cleanest way to get this into **draw.io** is using its built-in text-to-diagram engines (PlantUML or Mermaid). 

I have generated a **Mermaid** version of your sequence diagram for this purpose.

## Step-by-Step Instructions

1. Open [app.diagrams.net (draw.io)](https://app.diagrams.net/).
2. Create or open a blank diagram.
3. In the top toolbar, go to **Arrange** ➔ **Insert** ➔ **Advanced** ➔ **Mermaid...** (Depending on your Draw.io version, this might also be under the "+" icon in the top toolbar ➔ **Advanced** ➔ **Mermaid**).
4. **Copy the code block below** and paste it into the text box that appears.
5. Click **Insert**. Draw.io will instantly generate the sequence diagram and automatically apply a professional layout for you.

---

### Copy this code:

```mermaid
sequenceDiagram
    autonumber
    actor Customer as Customer
    actor Admin as Admin
    participant FE as Frontend (Next.js)
    participant BE as Backend API (Node.js)
    participant DB as PostgreSQL Database
    participant IBCF as Recommendation Engine

    %% Flow 1: Authentication
    Note over Customer, IBCF: FLOW 1: Authentication

    Customer->>FE: Open Registration / Login Page
    alt Registration
        Customer->>FE: Submit {name, email, password}
        FE->>BE: POST /api/auth/register
        BE->>BE: Hash password, Validate
        BE->>DB: INSERT INTO users
        DB-->>BE: 201 Created {userId}
        BE-->>FE: JWT & Refresh Token
        FE-->>Customer: Redirect to Home
    else Login
        Customer->>FE: Submit {email, password}
        FE->>BE: POST /api/auth/login
        BE->>DB: SELECT * FROM users WHERE email=?
        DB-->>BE: User record
        BE->>BE: Verify credentials
        alt Valid
            BE-->>FE: 200 OK, JWT & Refresh Token
            FE-->>Customer: Redirect to Dashboard
        else Invalid
            BE-->>FE: 401 Unauthorized
            FE-->>Customer: Show error notification
        end
    end

    %% Flow 2: Product Browsing & IBCF
    Note over Customer, IBCF: FLOW 2: Product Browsing & IBCF Recommendations
    Customer->>FE: Browse / Search Products
    FE->>BE: GET /api/products
    BE->>DB: SELECT products, stock
    DB-->>BE: Product list
    BE-->>FE: Return products
    FE-->>Customer: Render grid
    Note over FE,BE: User interaction triggers IBCF
    FE->>BE: POST /api/recommendations {userId, viewedProductId}
    BE->>IBCF: triggerRecommendations(userId)
    IBCF->>DB: Fetch user_interactions
    DB-->>IBCF: Interaction matrix
    IBCF->>IBCF: Compute Cosine Similarity, Filter, Rank
    IBCF-->>BE: [recommendedProductIds]
    BE->>DB: Fetch recommended products
    DB-->>BE: Product details
    BE-->>FE: return recommendations
    FE-->>Customer: Render "Recommended For You"

    %% Flow 3: Cart Management & Checkout
    Note over Customer, IBCF: FLOW 3: Cart Management & Checkout
    Customer->>FE: Add item to Cart
    FE->>BE: POST /api/cart/add
    BE->>DB: UPDATE cart_items
    DB-->>BE: Cart state
    BE-->>FE: 200 OK {cart}
    Customer->>FE: Proceed to Checkout
    FE->>BE: GET /api/cart/summary
    BE->>DB: Fetch cart items & verify stock
    DB-->>BE: Summary + stock
    BE-->>FE: Return summary
    Customer->>FE: Enter Shipping Details
    FE->>BE: POST /api/orders/initiate {shippingDetails}
    BE->>DB: INSERT INTO orders (status=PENDING)
    DB-->>BE: orderId
    BE-->>FE: orderId, amount, payment methods
    FE-->>Customer: Show Payment Options (COD/eSewa)

    %% Flow 5: Admin
    Note over Customer, IBCF: FLOW 5: Admin Product & Order Management
    Admin->>FE: Login to Dashboard
    FE->>BE: POST /api/auth/login
    BE->>DB: Check Role
    DB-->>BE: Role: ADMIN
    BE-->>FE: JWT (Admin)
    Admin->>FE: Add/Update Product
    FE->>BE: POST /api/admin/products
    BE->>DB: UPSERT products
    BE-->>FE: OK
    Admin->>FE: View Orders
    FE->>BE: GET /api/admin/orders
    BE->>DB: Fetch orders
    DB-->>BE: Orders list
    BE-->>FE: Return list
    Admin->>FE: Update Order Status (e.g. SHIPPED)
    FE->>BE: PATCH /api/admin/orders/:id/status
    BE->>DB: UPDATE orders SET status=SHIPPED
    BE-->>FE: 200 OK
```
