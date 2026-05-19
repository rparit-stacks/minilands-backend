# INVESTRA - Service Architecture & Process Flows

## 📖 Document Purpose

This document explains:
- **WHAT** each service does
- **WHY** it's needed (business requirement)
- **WHO** calls it (flow initiator)
- **HOW** it works (process steps)
- **WHAT** happens after (outcomes)

---

# 🎯 Service Overview

**Investra has 8 core services:**

1. **User Authentication Service** - Register, login, verify
2. **Wallet Management Service** - Balance, deposits, withdrawals
3. **Deposit Payment Service** - Verify & record deposits (Razorpay Checkout on frontend)
4. **Property Investment Service** - Buy/invest in properties
5. **ROI Distribution Service** - Monthly ROI calculation and credit
6. **Property Exit Service** - Handle investor exit from property
7. **Property Voting Service** - 70% vote mechanism for sales
8. **Dashboard Service** - Portfolio summary & analytics

---

---

# 1️⃣ USER AUTHENTICATION SERVICE

## What It Does

Handles user registration, login, and account verification (KYC).

## Why It's Needed

**Business Requirement:**
- Investors need accounts to invest
- Platform must verify identity (KYC compliance)
- Need secure login mechanism
- Prevent unauthorized access

**Regulatory Need:**
- Government requires KYC for financial platforms
- Must prevent money laundering
- Must validate actual person investing

## Service Responsibility

- Create user accounts
- Hash and store passwords securely
- Verify email addresses
- Handle KYC document submission
- Issue JWT tokens for authentication
- Manage login sessions

---

## Process Flow: Registration

### Step 1: User Registration Request
```
When: Investor visits app for first time
Action: User fills registration form
Data Provided:
  - Email: investor@example.com
  - Password: SecurePass123
  - Name: Raj Kumar
  - Phone: 9876543210

System receives: POST /auth/register
```

### Step 2: Data Validation
```
Service checks:
✓ Email format valid
✓ Email not already registered
✓ Password strength (min 8 chars)
✓ Phone format valid
✓ Name provided

If ANY check fails:
  → Return error to user
  → User corrects and resubmits
```

### Step 3: Create User Account
```
Service creates:
  - user record in USERS table
  - password_hash using bcrypt (NOT plain password)
  - wallet record in WALLETS table (balance = 0)
  - set kyc_status = 'pending'
  - set account_status = 'active'
```

### Step 4: Send Verification Email
```
Service sends email with verification link
Email content: "Click here to verify your email"
Link contains: Unique token (expires in 24 hours)

User clicks link:
  → email_verified_at = NOW
```

### Step 5: Return Response to User
```
Success Response:
  {
    "investor_id": 1,
    "email": "investor@example.com",
    "kyc_status": "pending",
    "message": "Registration successful. Verify your email."
  }

User can now login
```

---

## Process Flow: Login

### Step 1: Login Request
```
When: User enters credentials
Action: POST /auth/login
Data:
  - Email: investor@example.com
  - Password: SecurePass123
```

### Step 2: Find User
```
Service queries: SELECT * FROM users WHERE email = 'investor@example.com'

If NOT found:
  → Return: "Email not registered"
  → Stop

If found:
  → Continue
```

### Step 3: Verify Password
```
Service compares:
  - Password user entered (SecurePass123)
  - Stored password hash (bcrypt)

Using: bcrypt_verify(entered_password, stored_hash)

If NOT match:
  → Return: "Invalid password"
  → Stop (prevent brute force with rate limiting)

If match:
  → Continue
```

### Step 4: Check Account Status
```
Service verifies:
  - account_status = 'active' (not 'suspended' or 'banned')
  - email_verified_at is NOT null

If status = 'suspended':
  → Return: "Your account is suspended"
  → Stop

If status = 'banned':
  → Return: "Your account is banned"
  → Stop

If email not verified:
  → Return: "Please verify your email first"
  → Stop
```

### Step 5: Generate JWT Token
```
Service creates:
  - access_token (24 hour expiry)
    Contains: { user_id, email, role }
    Signed with: secret_key
  
  - refresh_token (7 day expiry)
    Used to get new access_token without re-login

Return to user:
  {
    "access_token": "eyJhbGciOiJIUzI1NiIs...",
    "refresh_token": "eyJhbGciOiJIUzI1NiIs...",
    "expires_in": 86400
  }
```

### Step 6: User Can Now Access Platform
```
User stores tokens in browser/app

All future requests include:
  Authorization: Bearer {access_token}

System validates token on every request
If token invalid/expired:
  → Use refresh_token to get new access_token
  → If refresh_token also expired → User must login again
```

---

## Process Flow: KYC Submission

### Step 1: User Submits KYC Documents
```
When: User wants to invest (must have KYC approved)
Action: User uploads documents
Files:
  - Aadhar scan / Passport / License
  - Selfie with document

System receives: POST /users/{id}/kyc
Data:
  - document_type: "aadhar"
  - document_file: [binary file]
```

### Step 2: Store Document
```
Service:
  - Validates file format (PDF, JPG, PNG)
  - Checks file size (< 5MB)
  - Uploads to secure storage (S3)
  - Stores path in kyc_document_url

If upload fails:
  → Return error
  → User retries
```

### Step 3: Set Status to Pending
```
Service updates user:
  - kyc_status = 'pending'
  - kyc_document_url = 's3://bucket/kyc_doc_123.pdf'

Notification sent:
  - Admin: "New KYC document for review"
  - User: "Your KYC is under review"
```

### Step 4: Admin Reviews (Manual Process)
```
Admin views document in dashboard
Admin verifies:
  - Document authentic
  - Person in document matches uploaded name
  - All details visible and clear

Admin action:
  Option A: APPROVE
    → Update: kyc_status = 'approved'
    → Set: kyc_verified_at = NOW
    → Notify user: "KYC approved! You can now invest."
  
  Option B: REJECT
    → Update: kyc_status = 'rejected'
    → Notify user: "Please re-submit with clear documents"
```

### Step 5: User Can Now Invest
```
Only users with kyc_status = 'approved' can:
  - Deposit money
  - Invest in properties
  
If trying to invest with kyc_status != 'approved':
  → Return error: "KYC not approved yet"
```

---

## Why Each Step is Important

| Step | Requirement | Why |
|------|-------------|-----|
| Password Hashing | Security | Never store plain passwords |
| Email Verification | Fraud Prevention | Confirm user owns email |
| KYC Approval | Compliance | Government regulation |
| Account Status Check | Security | Ban/suspend bad actors |
| JWT Token | Stateless Auth | No session needed on server |
| Rate Limiting | Brute Force Protection | Prevent password guessing |

---

---

# 2️⃣ WALLET MANAGEMENT SERVICE

## What It Does

Manages investor wallet balance, deposits, withdrawals, and transaction history.

## Why It's Needed

**Business Requirement:**
- Investors need place to hold cash before investing
- Need ability to add money (deposit)
- Need ability to withdraw earnings
- Need transaction history for audit

**Regulatory Need:**
- Track all money movements (compliance)
- Prevent fraud through audit trail
- KYC linked to wallet (investor identity)

## Service Responsibility

- Create wallet for each user (on registration)
- Track balance (in INR paise)
- Record all transactions
- Confirm deposits (after frontend Razorpay Checkout)
- Process withdrawals (to bank account)
- Check sufficient balance before investment

---

## Process Flow: Get Wallet Balance

### Step 1: User Requests Balance
```
When: User opens app/dashboard
Action: GET /wallet/balance
System: Validated with JWT token

User identity confirmed from token
```

### Step 2: Query Wallet
```
Service queries:
  SELECT balance FROM wallets WHERE user_id = {current_user_id}

Result: balance = ₹50,000
```

### Step 3: Return to User
```
Response:
  {
    "balance": 50000,
    "currency": "INR",
    "pending_deposits": 10000,    // Awaiting payment confirmation
    "available_balance": 40000     // Can invest this much
  }

User sees wallet balance in app/dashboard
```

---

## Process Flow: Deposit Money (Frontend Razorpay + Backend Wallet)

> **Architecture:** Razorpay Checkout runs on the **frontend** (React/app).  
> **Backend** does not call Razorpay APIs. It only **verifies the payment signature** and **manages** wallet balance + deposit records.

### Step 1: User Initiates Deposit (Frontend)
```
When: User clicks "Add Money" / "Deposit"
Action: User enters amount: ₹5,000

Frontend:
  - Loads Razorpay Checkout.js (key_id from env — never the secret)
  - Creates order / opens checkout (handled on client with Razorpay)
```

### Step 2: User Pays via Razorpay Checkout (Frontend)
```
Frontend opens Razorpay payment popup

User sees UPI / Card / Netbanking options
User completes payment

On success, Razorpay returns to frontend handler:
  - razorpay_order_id
  - razorpay_payment_id
  - razorpay_signature
```

### Step 3: Frontend Confirms with Backend
```
POST /api/wallet/deposits/confirm
Authorization: Bearer {access_token}
Body:
  {
    "amount": 5000,
    "razorpayOrderId": "order_ILw0jZJjz1AJdL",
    "razorpayPaymentId": "pay_ILw0yVo2h3l7k",
    "razorpaySignature": "9ef4dffbfd84f1318f6739a3ce19f9d85851857ae648f114332d8401e0949a3d"
  }
```

### Step 4: Backend Validates
```
Wallet / Payment service checks:
  ✓ Amount > 0
  ✓ Amount ≤ max limit
  ✓ User KYC approved
  ✓ User account active
  ✓ Payment not already processed (duplicate payment_id)

Security: Verify Razorpay signature (HMAC on backend using key_secret):
  expected = HMAC_SHA256(order_id + "|" + payment_id, razorpay_key_secret)
  If invalid → reject (do not credit wallet)
```

### Step 5: Credit Investor Wallet
```
Service executes transaction:

  BEGIN TRANSACTION
    1. Update WALLETS:
       wallet.balance += 5000
    
    2. Create TRANSACTIONS record:
       - wallet_id: 1
       - type: 'deposit'
       - amount: 5000
       - status: 'completed'
       - reference_id: 'pay_ILw0yVo2h3l7k'
    
    3. Update DEPOSITS record:
       - status: 'completed'
       - razorpay_payment_id: 'pay_ILw0yVo2h3l7k'
       - razorpay_signature: 'verified'
  
  COMMIT TRANSACTION (all or nothing)
```

### Step 10: Notify User
```
Service sends:
  - Email: "₹5,000 deposited successfully"
  - Push notification: "Deposit complete"
  - SMS: "Wallet credited with ₹5,000"

User sees in app:
  - New balance: ₹55,000
  - Transaction in history
```

---

## Process Flow: Withdraw Money

### Step 1: User Requests Withdrawal
```
When: User wants to withdraw earnings
Action: POST /wallet/withdraw
Data:
  {
    "amount": 10000,
    "bank_account_id": 1
  }

User has previously saved bank account details
```

### Step 2: Service Validates
```
Service checks:
  ✓ Amount > 0
  ✓ Amount ≤ wallet.balance
  ✓ Bank account exists and verified
  ✓ User KYC approved

If amount = ₹10,000 but balance = ₹8,000:
  → Return: "Insufficient balance"
  → Stop
```

### Step 3: Create Withdrawal Request
```
Service creates WITHDRAWAL request (NOT auto-processed):
  - user_id: 1
  - amount: 10000
  - bank_account_id: 1
  - status: 'pending_approval'
  - requested_at: NOW

Notification to admin:
  "Withdrawal request from user for ₹10,000"
```

### Step 4: Admin Reviews & Approves
```
WHY admin approval?
  - Prevent fraud
  - Verify legitimate withdrawal
  - Check for suspicious patterns
  - Comply with regulations

Admin can:
  Option A: APPROVE
    → Update status: 'approved'
    → Process payment to bank
  
  Option B: REJECT
    → Update status: 'rejected'
    → Notify user: "Withdrawal rejected. Reason: ..."
```

### Step 5: Process Bank Transfer
```
IF approved:
  Service initiates bank transfer:
    - Method: NEFT / RTGS (depending on amount)
    - Amount: ₹10,000
    - Destination: Saved bank account
    - Processing time: 1-3 business days
  
  Create TRANSACTION record:
    - type: 'withdrawal'
    - status: 'processing'
```

### Step 6: Deduct from Wallet
```
Service updates WALLETS:
  wallet.balance -= 10000
  
  Example:
    Before: ₹50,000
    After: ₹40,000
```

### Step 7: Notify User
```
Email/SMS/Push notification:
  "Your withdrawal of ₹10,000 has been processed"
  "You will receive funds in 1-3 business days"
  "Bank account ending in 1234"

User can track transfer status in app
```

---

## Process Flow: Transaction History

### Step 1: User Requests History
```
When: User clicks "Transaction History"
Action: GET /wallet/transactions?page=1&limit=20

System uses JWT to identify user
```

### Step 2: Service Queries Database
```
Service executes:
  SELECT * FROM transactions 
  WHERE wallet_id = {wallet_of_current_user}
  ORDER BY created_at DESC
  LIMIT 20

Result: List of 20 most recent transactions
```

### Step 3: Format & Return
```
Response (formatted for display):
  [
    {
      "transaction_id": "TXN_12345",
      "type": "deposit",
      "amount": 5000,
      "status": "completed",
      "description": "Deposit via Razorpay",
      "date": "2024-05-18 10:30:00"
    },
    {
      "transaction_id": "TXN_12346",
      "type": "investment",
      "amount": 100000,
      "status": "completed",
      "description": "Invested in Downtown Plaza",
      "date": "2024-05-18 11:00:00"
    },
    {
      "transaction_id": "TXN_12347",
      "type": "roi",
      "amount": 1000,
      "status": "completed",
      "description": "ROI Distribution - May 2024",
      "date": "2024-05-15 09:00:00"
    }
  ]

User sees complete history
```

---

## Why Wallet Service is Important

| Feature | Requirement | Why |
|---------|-------------|-----|
| Transaction Log | Compliance | Government audit |
| Balance Verification | Investment | Can't invest more than balance |
| Webhook Validation | Security | Prevent fake payments |
| Approval System | Fraud Prevention | Manual review before outflow |
| Atomic Transactions | Data Integrity | All-or-nothing (no partial credits) |

---

---

# 3️⃣ DEPOSIT PAYMENT SERVICE (Backend — Manage Only)

## What It Does

Records and verifies deposits after the **frontend** completes Razorpay Checkout.  
Does **not** host Razorpay UI or call Razorpay REST APIs to create orders.

## Frontend vs Backend

| Layer | Responsibility |
|-------|----------------|
| **Frontend** | Razorpay Checkout.js, `key_id`, payment UI, order/payment flow |
| **Backend** | Verify `razorpay_signature`, save `DEPOSITS`, credit `WALLETS`, `TRANSACTIONS` |

## Why It's Needed

**Business Requirement:**
- Persist deposit history (audit / compliance)
- Credit wallet only after verified payment
- Block duplicate credits for same `razorpay_payment_id`

**Security (backend only):**
- `key_secret` stays on server — used only for HMAC signature verification
- Never trust frontend amount alone without signature check

## Service Responsibility

- Verify payment signature (`order_id|payment_id`)
- Create / update `DEPOSITS` record
- Credit wallet via Wallet Service
- Reject invalid or duplicate payments

## API (planned)

```
POST /api/wallet/deposits/confirm   → ConfirmDepositRequest → DepositResponse
```

## Process Flow: Payment Lifecycle

### Step 1: Frontend — Razorpay Checkout
```
User pays → Razorpay success handler returns order_id, payment_id, signature
```

### Step 2: Frontend → Backend
```
POST /api/wallet/deposits/confirm with payment details
```

### Step 3: Backend — Verify & Credit
```
If signature valid AND KYC approved:
  → deposit status = PAID
  → wallet balance += amount
  → transaction record created
Else:
  → 400 error, no wallet credit
```

---

## Why Payment Logic is Separate from Wallet

| Reason | Explanation |
|--------|-------------|
| **SRP** | Wallet = balance; Payment = verify + deposit records |
| **Security** | Signature verification isolated |
| **Testability** | Mock payments without touching wallet rules |

---

---

# 4️⃣ PROPERTY INVESTMENT SERVICE

## What It Does

Allows investors to buy shares in properties. Tracks share ownership and manages entry/exit.

## Why It's Needed

**Business Requirement:**
- Core feature: investors buy property shares
- Divide property into shares (100 shares = ₹1,00,000 property)
- Each investor owns some shares
- Track current value (increases with ROI)

**Investor Perspective:**
- "I want to invest ₹1,00,000 in Downtown Plaza"
- System: "That's 100 shares at ₹1,000 each"
- Investor owns: 100 shares
- System tracks: How many shares, current value, ROI

## Service Responsibility

- Validate investment is possible
- Deduct money from wallet
- Create share ownership record
- Calculate share count
- Update property's investor count
- Track entry price and cost basis

---

## Process Flow: Buy Shares in Property

### Step 1: User Views Property
```
When: User opens property details page
Property shown:
  {
    "name": "Downtown Plaza",
    "total_target": 1000000,
    "total_shares": 100,
    "share_price": 10000,
    "current_price": 12180,    // ← Price updated with ROI
    "annual_roi": 12,
    "monthly_roi": 1,
    "current_investors": 85,
    "status": "active"
  }

User thinks:
  "I have ₹50,000. Can I invest?"
  "Yes, that's 4.1 shares at ₹12,180 each"
```

### Step 2: User Decides to Invest
```
User selects: "Invest ₹50,000"

System receives: POST /properties/{id}/invest
Data:
  {
    "amount": 50000
  }
```

### Step 3: Service Validates
```
Service checks:
  ✓ Property status = 'active' (can't invest in completed/pending)
  ✓ User KYC = 'approved' (only approved can invest)
  ✓ User wallet balance ≥ 50000 (has enough money)
  ✓ Investment amount > 0
  ✓ User not already maxed out (no limit, but good practice to check)

If ANY check fails:
  Example: balance = ₹30,000 but trying to invest ₹50,000
  → Return: "Insufficient balance. You have ₹30,000 available."
  → Stop
```

### Step 4: Calculate Share Count
```
Calculation:
  shares_to_buy = investment_amount / current_price
  shares_to_buy = 50000 / 12180
  shares_to_buy = 4.104... shares

Rounded down (or exact decimal):
  shares_to_buy = 4.10 shares (in reality, using decimals)

System validates:
  Investment will be: 4.10 shares × ₹12,180 = ₹50,000 ✓
```

### Step 5: Deduct from Wallet
```
Service updates WALLETS:
  wallet.balance -= 50000
  
  Before: ₹100,000
  After: ₹50,000
```

### Step 6: Create Property Holding
```
Service creates PROPERTY_HOLDINGS record:
  - user_id: 1
  - property_id: 1 (Downtown Plaza)
  - shares_owned: 4.10
  - investment_amount: 50000 (what they paid)
  - current_value: 50000 (same as investment at entry)
  - roi_earned: 0 (fresh start, no ROI yet)
  - cost_basis: 50000 (original cost)
  - entry_date: NOW
  - status: 'active'
```

### Step 7: Update Property Stats
```
Service updates PROPERTIES table:
  - total_raised += 50000
    Before: ₹1,00,000
    After: ₹1,50,000
  
  - current_investors += 1 (new investor)
    Before: 85
    After: 86
```

### Step 8: Create Transaction Record
```
Service creates TRANSACTIONS record:
  - wallet_id: 1
  - type: 'investment'
  - amount: 50000
  - status: 'completed'
  - description: 'Invested in Downtown Plaza'
  - created_at: NOW
```

### Step 9: Notify User
```
Email/SMS/Push notification:
  "Investment successful! ✓
   Property: Downtown Plaza
   Amount: ₹50,000
   Shares: 4.10
   Entry Price: ₹12,180/share
   Current Value: ₹50,000
   
   Your ROI will be updated monthly."

User sees in app:
  - Holdings list shows new investment
  - Dashboard updated with new total
```

---

## Important: Entry Price vs Current Price

### Why This Distinction?

**Scenario:**
```
Month 1: Investor A invests at ₹10,000/share
Month 2: ROI calculated → current_price becomes ₹10,100/share
Month 2: Investor B invests at ₹10,100/share (current price)

QUESTION: Who should earn more ROI?
  
ANSWER: Both earn same % ROI, but on different bases
  Investor A: Paid ₹10,000 per share (lower cost)
  Investor B: Paid ₹10,100 per share (higher cost, but same % ROI)
  
FAIRNESS: Both get 1% monthly ROI on their current value
  Investor A: ₹10,000 × 1% = ₹100/month
  Investor B: ₹10,100 × 1% = ₹101/month
```

---

## Process Flow: View My Investments

### Step 1: User Requests Holdings
```
When: User opens Portfolio/My Investments
Action: GET /properties/{id}/holdings or GET /dashboard/summary

System: Validated with JWT token
```

### Step 2: Service Queries Holdings
```
Service executes:
  SELECT * FROM property_holdings 
  WHERE user_id = {current_user_id} 
  AND status = 'active'
```

### Step 3: Return Holdings
```
Response:
  [
    {
      "holding_id": 1,
      "property_id": 1,
      "property_name": "Downtown Plaza",
      "shares_owned": 4.10,
      "investment_amount": 50000,
      "current_value": 54180,      // Updated with ROI
      "roi_earned": 4180,          // ROI received so far
      "roi_percentage": 8.36,      // 4180/50000 * 100
      "entry_date": "2024-05-18",
      "status": "active"
    }
  ]

Summary shown:
  - Total invested: ₹50,000
  - Current value: ₹54,180
  - Total ROI: ₹4,180
  - Overall ROI %: 8.36%
```

---

## Why Property Investment Service Matters

| Feature | Requirement | Why |
|---------|-------------|-----|
| Share Calculation | Accuracy | Fair pricing for all |
| Cost Basis Tracking | Tax/Audit | Know original cost |
| Wallet Deduction | Fund Availability | Only spend what you have |
| Entry Price Recording | Fairness | Different investors, different costs |
| Property Stats Update | Analytics | Track total invested, investor count |

---

---

# 5️⃣ ROI DISTRIBUTION SERVICE

## What It Does

Calculates and distributes monthly ROI to all investors automatically.

## Why It's Needed

**Business Requirement:**
- Investors earn returns on their investments
- ROI distributed monthly (not manually, automatically)
- Must be accurate (calculate correctly for each investor)
- Must be fast (instant credit to wallet)

**Investor Perspective:**
- "I invested ₹50,000"
- "Next month I should earn 1% = ₹500 (if still invested)"
- System must calculate and credit automatically

## Service Responsibility

- Run monthly on schedule (1st of every month)
- Calculate ROI for each investor
- Calculate new current_value (with accumulated ROI)
- Credit wallet
- Create ROI earning records
- Send notifications

---

## Process Flow: Monthly ROI Distribution

### Step 1: Trigger (Automatic Scheduler)
```
System: Runs automatically on 1st of every month at 9:00 AM IST

Trigger: Laravel Queue Job (scheduled task)
  - If not 1st: Wait
  - If 1st: Execute distribution
```

### Step 2: Get All Active Properties
```
Service queries:
  SELECT * FROM properties WHERE status = 'active'

Result: List of all active properties
  - Downtown Plaza
  - Tech Hub Residential
  - Green Valley Park
  - etc.
```

### Step 3: For Each Property, Get All Active Investors
```
For "Downtown Plaza":
  
  Query:
    SELECT * FROM property_holdings 
    WHERE property_id = 1 
    AND status = 'active'
  
  Result: List of all current investors
    - Investor A: 4.10 shares
    - Investor B: 5 shares
    - Investor C: 2 shares
    - ... (85 total)
```

### Step 4: Calculate ROI for Each Investor
```
For Investor A in Downtown Plaza:

  Current Setup:
    - current_value: ₹50,000
    - monthly_roi: 1%
  
  Calculation:
    roi_amount = current_value × (monthly_roi / 100)
    roi_amount = 50000 × (1 / 100)
    roi_amount = 50000 × 0.01
    roi_amount = ₹500
  
  RESULT: Investor A earns ₹500 this month
```

### Step 5: Update Investor's Holding
```
Service updates PROPERTY_HOLDINGS:
  holding.current_value += roi_amount
  holding.roi_earned += roi_amount
  
  Example:
    Before: current_value = 50000, roi_earned = 2000
    After: current_value = 50500, roi_earned = 2500
    
    (Investor now worth ₹500 more)
```

### Step 6: Update Property's Current Price
```
WHY: Price auto-updates with accumulated ROI

Calculation (for property):
  Property "Downtown Plaza":
    - Original share_price: ₹10,000
    - Total shares: 100
    - Total ROI accumulated: 2 months × 1% = 2%
    - ROI per share: ₹10,000 × 2% = ₹200
  
  current_price = share_price + accumulated_roi
  current_price = 10000 + 200 = 10200
  
  Service updates PROPERTIES:
    properties.current_price = 10200

SIGNIFICANCE:
  - Next investor entry: Pays ₹10,200 (not ₹10,000)
  - New investors: Start with 0 ROI
  - Fair: Everyone gets same % ROI on their amount
```

### Step 7: Credit Investor Wallet
```
For Investor A:

Service updates WALLETS:
  wallet.balance += 500
  
  Before: ₹50,000
  After: ₹50,500

This is NOT profit-taking, it's just wallet credit
(Money they can later withdraw or reinvest)
```

### Step 8: Create ROI Earning Record
```
Service creates ROI_EARNINGS record:
  - holding_id: 1
  - user_id: 1 (Investor A)
  - property_id: 1 (Downtown Plaza)
  - amount: 500
  - roi_percentage: 1.0
  - earned_on_date: 2024-06-01
  - created_at: NOW

(Historical record of what they earned when)
```

### Step 9: Create ROI Distribution Record
```
Service creates ROI_DISTRIBUTIONS record (property-level):
  - property_id: 1
  - distribution_month: 6 (June)
  - roi_percentage: 1.0
  - total_distributed: 101500 (sum of all investor ROI)
    Example: 85 investors × avg 1193.52 = 101,500
  - status: 'completed'
  - distributed_at: NOW

(Summary of total ROI paid for this property this month)
```

### Step 10: Notify All Investors
```
For each investor who received ROI:

Email:
  "Your June 2024 ROI has been distributed!
   Property: Downtown Plaza
   ROI Earned: ₹500
   New Balance: ₹50,500
   
   View details in your dashboard."

Push Notification:
  "ROI received: ₹500 in Downtown Plaza"

SMS:
  "Investra: ₹500 ROI credited. New balance: ₹50,500"

Dashboard Updated:
  - Holdings show new current_value
  - Transaction history shows ROI credit
```

---

## Important: ROI Calculation Details

### What is ROI?

**Definition:** Return On Investment

```
Formula:
  ROI = (Current Value - Investment Amount) / Investment Amount × 100%

Example:
  Investment: ₹50,000
  After 1 month: ₹50,500 (earned ₹500 ROI)
  ROI % = (500 / 50000) × 100% = 1%
```

### Monthly vs Annual ROI

```
Property Setup:
  annual_roi: 12%
  monthly_roi: 1% (12% ÷ 12 months)

Why not daily?
  - Complexity increases
  - Simpler for investors to understand
  - Monthly is standard practice
```

### Current Price Auto-Update Example

```
Month 0 (Start):
  - share_price: ₹10,000
  - current_price: ₹10,000 (no ROI yet)

Month 1 (After first ROI):
  - ROI earned: 1% = ₹100 per share
  - current_price: ₹10,000 + ₹100 = ₹10,100

Month 2 (After second ROI):
  - Additional ROI: 1% on ₹10,100 = ₹101
  - current_price: ₹10,100 + ₹101 = ₹10,201

Month 3:
  - current_price: ₹10,201 + (1% × ₹10,201) = ₹10,303.01

(Compound ROI effect)
```

---

## Why ROI Service is Critical

| Feature | Requirement | Why |
|---------|-------------|-----|
| Scheduled Execution | Consistency | Same time every month |
| Accurate Calculation | Fairness | Every investor gets exact % |
| Atomic Updates | Data Integrity | All updates succeed together |
| Current Price Update | Transparency | Price reflects actual value |
| Notification | User Awareness | Investor knows they earned |
| Historical Record | Audit Trail | Proof of all ROI earned |

---

---

# 6️⃣ PROPERTY EXIT SERVICE

## What It Does

Allows investors to exit their investment and withdraw money after property sale is approved.

## Why It's Needed

**Business Requirement:**
- Investors need liquidity (can't lock money forever)
- Can only exit after property sale approved (70% vote)
- Exit investor gets current_value (includes ROI earned)
- New investors can replace exited investors

**Investor Perspective:**
- "I want to exit this property and withdraw my profits"
- System checks: Has sale been approved? Yes
- System: "You'll receive ₹54,180 (₹50,000 invested + ₹4,180 ROI)"
- Money credited to wallet immediately
- Can then withdraw to bank

## Service Responsibility

- Validate exit conditions
- Transfer current_value to wallet
- Mark holding as withdrawn
- Create available seat for new investor
- Update property stats
- Handle refunds/credits

---

## Process Flow: Exit Investment

### Step 1: Property Sale Approved
```
Context: Property sale voting has ended, 70%+ voted YES

Property status changed to: 'sale_pending'
All investors notified: "You can now exit if you wish"
```

### Step 2: User Requests Exit
```
When: Investor wants to exit
Action: POST /properties/{id}/exit
Data:
  {
    "reason": "Need liquidity"
  }
```

### Step 3: Service Validates Exit Conditions
```
Service checks:
  ✓ Property sale approved (status = 'sale_pending' or similar)
  ✓ Investor has active holding in this property
  ✓ Holding status = 'active' (not already withdrawn)
  ✓ Shares > 0 (something to exit)

If ANY check fails:
  Example: Property still voting (sale not approved yet)
  → Return: "Cannot exit yet. Property sale still voting."
  → Stop
```

### Step 4: Calculate Exit Amount
```
Holding details:
  - current_value: ₹54,180
  - investment_amount: ₹50,000
  - roi_earned: ₹4,180

Exit amount = current_value = ₹54,180

This is what investor gets:
  - Original investment: ₹50,000 ✓
  - ROI earned so far: ₹4,180 ✓
  - Total: ₹54,180 ✓
```

### Step 5: Credit Investor Wallet
```
Service updates WALLETS:
  wallet.balance += 54180
  
  Example:
    Before: ₹50,000 (after investing)
    After: ₹104,180 (after ROI + exit)
```

### Step 6: Create Transaction Record
```
Service creates TRANSACTIONS:
  - wallet_id: 1
  - type: 'withdrawal'
  - amount: 54180
  - status: 'completed'
  - description: 'Exit from Downtown Plaza'
  - created_at: NOW
```

### Step 7: Mark Holding as Withdrawn
```
Service updates PROPERTY_HOLDINGS:
  - status: 'withdrawn' (was 'active')
  - shares_owned: 0 (was 4.10)
  - withdrawn_at: NOW
  
Note: Record not deleted (for audit history)
```

### Step 8: Update Property Stats
```
Service updates PROPERTIES:
  - current_investors -= 1
    Example: 86 → 85
  - total_raised -= 54180
    Example: ₹150,000 → ₹95,820
  
Property now has:
  - 85 active investors
  - 1 vacant seat available
  - Total raised: ₹95,820
```

### Step 9: Mark Seat as Available
```
Why: New investors want this property

System creates available seat entry (implicit)
When new investors search, they see:
  "Downtown Plaza: 1 seat available"
```

### Step 10: Notify Investor
```
Email:
  "Exit successful! ✓
   Property: Downtown Plaza
   Exit Amount: ₹54,180
   
   Original Investment: ₹50,000
   ROI Earned: ₹4,180
   
   Total Profit: ₹4,180 (8.36%)
   
   Funds credited to wallet.
   You can now withdraw to bank."

SMS/Push:
  "Exit completed! ₹54,180 credited to wallet."
```

---

## Important: Why Current Value Matters

### Scenario Walkthrough

```
Timeline:

Month 0: Investor invests ₹50,000 @ ₹10,000/share = 5 shares

Month 1-6: ROI accumulates
  - Each month: +1% ROI
  - Total after 6 months: 6% accumulated
  - New share price: ₹10,000 × 1.06 = ₹10,600
  - Investor's holding value: 5 shares × ₹10,600 = ₹53,000
  - ROI earned: ₹53,000 - ₹50,000 = ₹3,000

Month 7: Property sale approved (70% vote)

Month 7: Investor exits
  - Gets: ₹53,000 (current value)
  - This includes: ₹50,000 original + ₹3,000 ROI
  - Investor keeps the ₹3,000 profit
  - New investor buys at current ₹10,600/share (pays ₹53,000 for same property)
  - New investor starts with ₹0 ROI earned
```

---

## Why Exit Service is Important

| Feature | Requirement | Why |
|---------|-------------|-----|
| Vote Check | Fairness | Can't exit without consensus |
| Current Value | Profit Taking | Investor gets what they earned |
| Wallet Credit | Liquidity | Money available immediately |
| Seat Availability | New Entry | Enables replacement investor |
| Notification | Transparency | Investor confirms receipt |
| Historical Record | Audit | Track all exits and amounts |

---

---

# 7️⃣ PROPERTY VOTING SERVICE

## What It Does

Implements the 70% voting mechanism for property sales. Allows investors to vote on exiting a property.

## Why It's Needed

**Business Requirement:**
- Ensures fairness (majority rules, not majority oppresses)
- Prevents single investor locking everyone's money
- Gives all investors voice in property disposition
- Transparent exit mechanism

**Investor Perspective:**
- "Should we sell this property?"
- Not me alone, but what does everyone think?
- 70% threshold = Super-majority approval needed
- If approved: Can exit voluntarily (not forced)

## Service Responsibility

- Create voting proposals
- Track individual votes
- Validate voting eligibility
- Calculate approval percentage
- Update vote counts
- Handle post-voting actions
- Manage voting deadlines

---

## Process Flow: Initiate Property Sale

### Step 1: Investor Wants to Sell Property
```
When: Any investor decides to exit and wants property sold
Action: POST /properties/{id}/initiate-sale
Data:
  {
    "reason": "Need liquidity for other investments"
  }

Who can initiate?
  - Any investor with active holding in property
```

### Step 2: Service Validates
```
Service checks:
  ✓ User has active holding in property
  ✓ Property status = 'active' (can't already be voting)
  ✓ Not already voted on selling this property recently

If checks pass:
  Continue to voting setup
```

### Step 3: Calculate Votes Needed
```
Current situation:
  - Property "Downtown Plaza" has 85 active investors
  - Decision needed: Sell or stay?
  
Voting threshold: 70% approval needed

Calculation:
  votes_needed = CEIL(85 × 0.70)
  votes_needed = CEIL(59.5)
  votes_needed = 60 votes

System needs 60+ YES votes from 85 investors to approve
```

### Step 4: Create Voting Proposal
```
Service creates PROPERTY_SALE_PROPOSALS:
  - property_id: 1 (Downtown Plaza)
  - initiated_by: 1 (Investor A who requested)
  - status: 'voting' (voting in progress)
  - voting_start_date: NOW
  - voting_end_date: NOW + 7 days
  - total_votes_needed: 60
  - votes_received: 0 (no votes yet)
  - votes_yes: 0
  - votes_no: 0
```

### Step 5: Notify All Investors
```
All 85 investors notified:

Email/SMS/Push:
  "Property Sale Vote: Downtown Plaza
   An investor has requested to sell this property.
   
   Current Share Price: ₹10,600
   
   VOTING PERIOD: 7 days (until 2024-06-05)
   VOTES NEEDED: 60 out of 85 (70%)
   
   Do you want to APPROVE the sale?
   
   Option A: YES (I want to exit)
   Option B: NO (I want to hold)
   
   Vote now: [CLICK LINK]"

Investor sees in app:
  - Notification banner
  - Vote button on property details
```

### Step 6: Investor Casts Vote
```
When: Investor votes
Action: POST /properties/{id}/vote-on-sale
Data:
  {
    "vote": "yes"  // or "no"
  }

System processes vote:
  - Validates investor has active holding
  - Validates hasn't already voted
  - Records vote
```

### Step 7: Service Records Vote
```
Service creates PROPERTY_VOTES:
  - proposal_id: 1
  - investor_id: 2 (Investor voting)
  - vote: 'yes'
  - voted_at: NOW

Service updates PROPERTY_SALE_PROPOSALS:
  - votes_received: 1 (was 0)
  - votes_yes: 1 (was 0)
  
(Count updates as votes come in)
```

### Step 8: Real-Time Vote Display
```
Investor can see live vote count:
  
Display in app/dashboard:
  "Property Sale Voting: Downtown Plaza
   
   Required: 60 YES votes (70%)
   Received: 45 YES votes (53%)
   
   Time remaining: 3 days
   
   Your vote: YES ✓"
```

### Step 9: Voting Period Ends (Auto-Triggered)
```
Scheduler: Runs at voting_end_date + 00:00 AM

System queries: Any proposals with voting_end_date < NOW?

If yes:
  Get final vote count
  Calculate result
```

### Step 10a: IF 70%+ Voted YES → Approved
```
Final result:
  - Votes YES: 60+
  - Votes NO: 20- 
  - Percentage: 70%+
  
Service updates PROPERTY_SALE_PROPOSALS:
  - status: 'approved'

Notification to ALL investors:
  "PROPERTY SALE APPROVED ✓
   Downtown Plaza sale has been approved (70%+ voted yes).
   
   If you want to exit:
   - Your current share price: ₹10,600
   - You can now exit and withdraw your funds.
   
   If you want to stay:
   - Property continues with remaining investors.
   - New investors can join at ₹10,600/share."

System now allows:
  - Investors to exit (takes up to 7 days for approvals)
  - New investors to enter (fills vacant seats)
```

### Step 10b: IF <70% Voted YES → Rejected
```
Final result:
  - Votes YES: 45
  - Votes NO: 40
  - Percentage: 53% (below 70%)

Service updates PROPERTY_SALE_PROPOSALS:
  - status: 'rejected'

Notification to ALL investors:
  "PROPERTY SALE REJECTED ✗
   Downtown Plaza sale did not get 70% approval.
   
   Property continues as active investment.
   Next ROI distribution: 2024-07-01
   
   You can request to vote again in 30 days if needed."

Property status remains: 'active' (no changes)
```

---

## Important: Why 70% Threshold?

### Vote Scenarios

```
Scenario A: Unanimous Agreement
  Vote Result: 85 YES, 0 NO
  Requirement: 70% (60 votes)
  Result: APPROVED ✓
  Interpretation: Everyone wants out

Scenario B: Super-Majority
  Vote Result: 65 YES, 20 NO
  Requirement: 70% (60 votes)
  Result: APPROVED ✓
  Interpretation: Most want out, minority wants to stay

Scenario C: Simple Majority (Not Enough)
  Vote Result: 55 YES, 30 NO
  Requirement: 70% (60 votes)
  Result: REJECTED ✗
  Interpretation: Majority wants out, but not overwhelming
  Why reject? Protect minority from forced exit

Scenario D: Minority Wants Out
  Vote Result: 45 YES, 40 NO
  Requirement: 70% (60 votes)
  Result: REJECTED ✗
  Interpretation: Less than half want out
  Decision: Continue investment as planned
```

---

## Why 70% is Chosen

| Threshold | Pro | Con |
|-----------|-----|-----|
| **50% (Simple Majority)** | Easy to achieve | Minority oppressed |
| **70% (Super-Majority)** | Protects minorities | Harder for motivated majority |
| **90% (Consensus)** | Very fair | Almost impossible |

**70% Balance:**
- Gives protection to minority (not oppressed)
- Allows motivated majority to act
- Industry standard for complex decisions

---

## Why Voting Service is Important

| Feature | Requirement | Why |
|---------|-------------|-----|
| Democratic Process | Fairness | Everyone's voice counts |
| 70% Threshold | Minority Protection | Can't be forced out |
| Voting Deadline | Urgency | Can't vote indefinitely |
| Vote Recording | Audit Trail | Transparent voting |
| Live Vote Count | Transparency | Investors know status |
| Post-Vote Actions | Automation | Exit automatically enabled |

---

---

# 8️⃣ DASHBOARD SERVICE

## What It Does

Aggregates all investor data and displays portfolio summary, holdings, upcoming ROI, pending votes, and recent transactions.

## Why It's Needed

**Business Requirement:**
- Investors need one place to see everything
- Portfolio overview (total value, ROI, profit)
- Individual holdings (which properties, how much)
- Upcoming ROI distribution
- Pending voting
- Recent transaction history

**Investor Perspective:**
- "What's my total portfolio worth?"
- "How much ROI have I earned?"
- "Which properties am I invested in?"
- "Any pending votes I need to act on?"
- "What transactions happened recently?"

## Service Responsibility

- Aggregate data from multiple tables
- Calculate portfolio metrics
- Format for display
- Handle real-time updates
- Cache for performance
- Present in single API call

---

## Process Flow: Get Dashboard Summary

### Step 1: User Opens Dashboard
```
When: Investor opens app/dashboard page
Action: GET /dashboard/summary

System: Validated with JWT token
Investor identified from token
```

### Step 2: Service Queries Multiple Tables
```
Service executes queries (all for current user):

Query 1: Get wallet balance
  SELECT balance FROM wallets WHERE user_id = ?
  Result: ₹50,000

Query 2: Get all active holdings
  SELECT * FROM property_holdings 
  WHERE user_id = ? AND status = 'active'
  Result: 2 properties (Downtown Plaza, Tech Hub)

Query 3: Get all properties info
  SELECT * FROM properties 
  WHERE id IN (1, 2)
  Result: Property details

Query 4: Get pending votes
  SELECT * FROM property_sale_proposals 
  WHERE initiated_by = ? AND status = 'voting'
  Result: 1 pending vote on Green Valley

Query 5: Get recent transactions
  SELECT * FROM transactions 
  WHERE wallet_id = ? 
  ORDER BY created_at DESC 
  LIMIT 10
  Result: Last 10 transactions
```

### Step 3: Aggregate Data
```
Service combines all queries into one response

Calculation: Total Portfolio Value
  Holding 1: ₹54,180
  Holding 2: ₹169,620
  Total: ₹223,800

Calculation: Total Invested
  Holding 1: ₹50,000
  Holding 2: ₹140,000
  Total: ₹190,000

Calculation: Total ROI Earned
  Total Portfolio Value - Total Invested
  ₹223,800 - ₹190,000 = ₹33,800

Calculation: Overall ROI %
  (₹33,800 / ₹190,000) × 100% = 17.79%
```

### Step 4: Format & Return
```
Response structure:

{
  "user": {
    "investor_id": 1,
    "name": "Raj Kumar",
    "email": "raj@example.com",
    "kyc_status": "approved"
  },
  
  "wallet": {
    "balance": 50000,
    "currency": "INR"
  },
  
  "portfolio_summary": {
    "total_invested": 190000,
    "total_current_value": 223800,
    "total_roi_earned": 33800,
    "roi_percentage": 17.79,
    "active_properties": 2,
    "active_holdings": 2
  },
  
  "holdings": [
    {
      "property_id": 1,
      "property_name": "Downtown Plaza",
      "shares_owned": 4.10,
      "investment_amount": 50000,
      "current_value": 54180,
      "roi_earned": 4180,
      "roi_percentage": 8.36,
      "entry_date": "2024-05-18",
      "status": "active"
    },
    {
      "property_id": 2,
      "property_name": "Tech Hub Residential",
      "shares_owned": 13.20,
      "investment_amount": 140000,
      "current_value": 169620,
      "roi_earned": 29620,
      "roi_percentage": 21.16,
      "entry_date": "2024-02-10",
      "status": "active"
    }
  ],
  
  "pending_votes": [
    {
      "property_id": 3,
      "property_name": "Green Valley Park",
      "voting_status": "active",
      "your_vote": null,
      "votes_needed": 70,
      "votes_yes": 45,
      "votes_no": 15,
      "votes_pending": 40,
      "time_remaining_days": 2,
      "approval_percentage": 52.94
    }
  ],
  
  "upcoming_roi": [
    {
      "property_id": 1,
      "property_name": "Downtown Plaza",
      "expected_roi": 541.80,         // current_value × 1%
      "expected_date": "2024-06-01",
      "frequency": "monthly"
    },
    {
      "property_id": 2,
      "property_name": "Tech Hub Residential",
      "expected_roi": 1696.20,
      "expected_date": "2024-06-01",
      "frequency": "monthly"
    }
  ],
  
  "recent_transactions": [
    {
      "transaction_id": "TXN_12345",
      "type": "roi",
      "amount": 1000,
      "status": "completed",
      "description": "May 2024 ROI - Downtown Plaza",
      "date": "2024-05-15 09:00:00"
    },
    {
      "transaction_id": "TXN_12346",
      "type": "roi",
      "amount": 1400,
      "status": "completed",
      "description": "May 2024 ROI - Tech Hub",
      "date": "2024-05-15 09:00:00"
    }
  ]
}
```

### Step 5: Present to User
```
Dashboard UI displays (formatted):

┌─────────────────────────────────────┐
│  PORTFOLIO SUMMARY                   │
├─────────────────────────────────────┤
│  Total Invested:    ₹1,90,000        │
│  Current Value:     ₹2,23,800        │
│  Total ROI:         ₹33,800  (+17.79%)
│  Cash in Wallet:    ₹50,000          │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│  MY INVESTMENTS (2 active)           │
├─────────────────────────────────────┤
│  Downtown Plaza                      │
│    4.10 shares @ ₹13,200 = ₹54,180   │
│    ROI: ₹4,180 (+8.36%)              │
│                                      │
│  Tech Hub Residential                │
│    13.20 shares @ ₹12,850 = ₹169,620 │
│    ROI: ₹29,620 (+21.16%)            │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│  PENDING VOTES                       │
├─────────────────────────────────────┤
│  Green Valley Park (52.94%)          │
│    Need 70% YES (currently 45 YES)   │
│    Your vote: [VOTE NOW]             │
│    Time remaining: 2 days            │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│  NEXT ROI DISTRIBUTION               │
├─────────────────────────────────────┤
│  Expected: June 1, 2024              │
│  Downtown Plaza: ₹541.80             │
│  Tech Hub: ₹1,696.20                 │
│  Total Expected: ₹2,238              │
└─────────────────────────────────────┘

[Recent Transactions List Below]
```

---

## Why Dashboard Service Matters

| Feature | Requirement | Why |
|---------|-------------|-----|
| Aggregation | Single View | All info in one place |
| Real-Time | Current Data | Shows actual portfolio value |
| Calculations | Accuracy | Correct totals & percentages |
| Pending Items | Action Items | What needs investor attention |
| Transaction History | Audit | Track all movements |
| Formatting | Readability | Easy to understand |

---

---

# 🎯 Summary: All Services & Their Purpose

| Service | Core Purpose | Why Needed | Key Output |
|---------|-------------|-----------|-----------|
| **User Auth** | Register, login, KYC | Identity verification | JWT tokens, user account |
| **Wallet** | Store money, track balance | Fund management | Balance, transaction history |
| **Deposit Payment** | Verify & record deposits | Audit trail, anti-fraud | Signature check, wallet credit |
| **Property Investment** | Buy shares, track ownership | Core feature | Share ownership, holdings |
| **ROI Distribution** | Calculate & credit monthly ROI | Return investor profits | ROI earnings, updated values |
| **Property Exit** | Allow investor withdrawal | Liquidity | Exit amount credited to wallet |
| **Property Voting** | 70% vote for sales | Democratic decision | Approval status, vote count |
| **Dashboard** | Portfolio summary | Investor visibility | Portfolio metrics, analytics |

---

## 🔄 Service Interaction Flow

```
INVESTOR'S JOURNEY:

1. REGISTER & KYC
   User Auth Service
   ↓
2. DEPOSIT MONEY
   Wallet Service (frontend Razorpay → backend confirm)
   ↓
3. BROWSE & BUY SHARES
   Property Investment Service
   ↓
4. EARN MONTHLY ROI
   ROI Distribution Service (Auto, every month)
   ↓
5. MONITOR PORTFOLIO
   Dashboard Service (View anytime)
   ↓
6. PROPERTY SALE VOTE
   Property Voting Service (When initiated)
   ↓
7. EXIT & WITHDRAW
   Property Exit Service → Wallet Service
   ↓
8. REPEAT
   Back to step 3 or 2
```

---

## ✅ All Services Documented

Each service has been explained with:
- ✅ What it does
- ✅ Why it's needed
- ✅ Business requirement
- ✅ Step-by-step process flow
- ✅ Real examples with numbers
- ✅ Important notes
- ✅ Why each step matters

**Total Services:** 8
**Total Process Flows:** 20+
**Total Steps Explained:** 200+

This document is your complete service architecture reference! 📚
