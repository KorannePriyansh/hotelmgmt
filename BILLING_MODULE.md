# BILLING & CHECKOUT MODULE — COMPLETE LOGIC & VIVA GUIDE

> **Author**: Member 2 (Reservations, Guests & Billing)  
> **Target Audience**: Complete beginner in Java  
> **Files Covered**: 
> 1. [Billing.java](file:///c:/Users/DELL/OneDrive/Desktop/New%20folder%20(3)/src/model/Billing.java) (Data Model)
> 2. [BillingDAO.java](file:///c:/Users/DELL/OneDrive/Desktop/New%20folder%20(3)/src/dao/BillingDAO.java) (Database Logic)
> 3. [BillingPanel.java](file:///c:/Users/DELL/OneDrive/Desktop/New%20folder%20(3)/src/ui/BillingPanel.java) (Graphical User Interface)

---

## 1. Fundamentals: Key Concepts Explained Simply

### 💳 What is a Database Transaction (`setAutoCommit(false)`)?
- Imagine visiting an ATM. The ATM machine must:
  1. Deduct ₹2,000 from your bank account.
  2. Dispense ₹2,000 cash from the machine.
- What if the power cuts off after Step 1? Your account is deducted, but you got no cash!
- A **Transaction** bundles both actions together into an **"All-or-Nothing"** rule.
  - If both succeed $\rightarrow$ **`commit()`** (save changes permanently).
  - If either fails $\rightarrow$ **`rollback()`** (undo everything as if nothing happened).

### 🏨 Why do we use a Transaction in Hotel Checkout?
When a guest checks out:
1. We must **insert the new bill** into the `bills` table.
2. We must **update the reservation status** to `'CHECKED_OUT'` in the `reservations` table.
- If we saved the bill, but failed to mark the reservation as checked out, the room would remain occupied in the computer forever!
- By putting both in one transaction in `BillingDAO.java`:
  ```java
  con.setAutoCommit(false); // Turn off auto-saving
  // Step 1: Insert Bill
  // Step 2: Update Reservation to CHECKED_OUT
  con.commit();             // Save both together
  ```

### 💰 Why `BigDecimal` instead of `double` or `float`?
- Computers store `double` using binary fractions.
- In `double`, simple calculations can have tiny rounding errors:
  ```java
  System.out.println(0.1 + 0.2); // Prints 0.30000000000000004 !
  ```
- In banking and hotel billing, losing 1 paisa or gaining 0.0000004 creates tax audit discrepancies!
- **`BigDecimal`** represents exact decimal numbers with exact precision, making it the industry standard for financial calculations.

### 🧮 The Billing Math Formula:
The bill is calculated in 4 clear steps:
1. **$\text{Room Charges} = \text{Nights} \times \text{Rate Per Night}$**  
   *(e.g., 3 nights $\times$ ₹1,500 = ₹4,500)*
2. **$\text{Subtotal} = \text{Room Charges} + \text{Service Charges} - \text{Discount}$**  
   *(e.g., ₹4,500 + ₹500 services - ₹200 discount = ₹4,800)*
3. **$\text{Tax Amount} = \text{Subtotal} \times \left(\frac{\text{Tax Rate \%}}{100}\right)$**  
   *(e.g., 12% GST of ₹4,800 = ₹576)*
4. **$\text{Total Bill} = \text{Subtotal} + \text{Tax Amount}$**  
   *(e.g., ₹4,800 + ₹576 = ₹5,376)*

---

## 2. File 1: `Billing.java` (The Model)

### Key Variables:
```java
private int billId;
private int reservationId;
private String guestName;          // Display helper
private String roomNumber;         // Display helper
private BigDecimal roomCharges;    // e.g. 4500.00
private BigDecimal serviceCharges; // Food, laundry (from Member 3)
private BigDecimal discount;       // Promotional discount
private BigDecimal taxAmount;      // GST amount
private BigDecimal totalAmount;    // Final payable amount
private String paymentMode;        // CASH, CREDIT_CARD, UPI, NET_BANKING
private String paymentStatus;      // PAID, PENDING
private LocalDate billingDate;     // Date invoice was generated
```

### Key Functions Explained:
- **`calculateTotal(room, service, discount, taxRatePercent)`**:
  - A static utility method that applies the formula above using `BigDecimal` arithmetic methods (`.add()`, `.subtract()`, `.multiply()`, `.divide()`).
- **Getters and Setters**: Standard methods to read and write fields.

---

## 3. File 2: `BillingDAO.java` (Database Operations)

### Functions Step-by-Step:

#### 1. `createBill(Billing bill)`
- **Goal**: Atomically save the invoice AND mark the room reservation as `CHECKED_OUT`.
- **How it works**:
  1. `Connection con = DBConnection.getConnection()`
  2. `con.setAutoCommit(false)`: Disables automatic saving.
  3. Prepares two SQL queries:
     - **Query 1**:
       ```sql
       INSERT INTO bills 
       (reservation_id, room_charges, service_charges, discount, tax_amount, total_amount, payment_mode, payment_status)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?)
       ```
     - **Query 2**:
       ```sql
       UPDATE reservations SET status = 'CHECKED_OUT' WHERE reservation_id = ?
       ```
  4. Runs Query 1, captures the generated `bill_id`.
  5. Runs Query 2 with the same `reservation_id`.
  6. Calls `con.commit()`: Commits both updates permanently.
  7. If anything throws an error: `con.rollback()` undoes everything.
  8. In the `finally` block: resets `con.setAutoCommit(true)`.

#### 2. `getAllBills()`
- **Goal**: Fetch complete billing history for the table.
- **SQL Query**:
  ```sql
  SELECT b.*, g.full_name AS guest_name, r.room_number
  FROM bills b
  JOIN reservations r ON b.reservation_id = r.reservation_id
  JOIN guests g ON r.guest_id = g.guest_id
  ORDER BY b.bill_id DESC
  ```
  *(Uses a 3-way `JOIN` across `bills`, `reservations`, and `guests` to display both the guest's name and their room number).*

---

## 4. File 3: `BillingPanel.java` (The UI Screen)

### Key UI Features:
1. **Reservation Dropdown (`resComboBox`)**:
   - Shows active bookings: `"Booking #1 - Rahul Sharma (Room 101 | 3 nights)"`.
   - When selected, it **automatically calculates the Room Charges** and fills the textbox!
2. **Real-time Recalculation Listeners**:
   - As soon as you type in Service Charges (e.g., `500`), change the Discount, or pick `18% GST`, the **Total Amount (₹)** textbox updates instantly!
   - The Total textbox is set to `totalField.setEditable(false)` so users cannot manually tamper with the final calculated price.
3. **Printable Receipt Popup Dialog**:
   - When you click **Generate Bill & Checkout**, a formatted ASCII receipt pops up on the screen in a clean monospaced font (`Consolas`).

### Key Action Functions:

1. **`calculateTotal()`**:
   - Reads `roomChargesField`, `serviceChargesField`, `discountField`, and `taxBox`.
   - Computes Subtotal and GST.
   - Sets the result into `totalField`.

2. **`generateBill()`**:
   - Validates that an active reservation is chosen.
   - Creates a `new Billing(...)` object.
   - Calls `billingDAO.createBill(bill)`.
   - Calls `showReceiptDialog(...)` to show the invoice popup on screen.
   - Calls `reloadActiveReservations()` to remove the checked-out reservation from the dropdown, and `loadBills()` to refresh the history table.

3. **`showReceiptDialog(...)`**:
   - Builds a hotel invoice receipt string:
     ```
     ====================================================
                 GRAND HOTEL & RESORTS INNAURA           
                    FINAL CHECKOUT RECEIPT               
     ====================================================
     Invoice Number : #BILL-00001
     Guest Name     : Rahul Sharma
     Room Number    : Room 101
     Duration       : 3 Night(s)
     Payment Mode   : CREDIT_CARD
     ----------------------------------------------------
     Room Charges                     : ₹ 4500.00
     Room Service / Amenities         : ₹ 500.00
     Special Discount                 : - ₹ 200.00
     ----------------------------------------------------
     GST / Tax Amount                 : + ₹ 576.00
     ====================================================
     TOTAL AMOUNT PAID                : ₹ 5376.00
     ====================================================
     ```
   - Displays it inside an interactive `JOptionPane.showMessageDialog`.

---

## 5. Top 5 Viva Questions For Billing Module

| Question | Short Answer to Give |
| :--- | :--- |
| **Q1: Why did you use transactions during checkout?** | *"To ensure ACID properties. We must both save the bill and set the reservation to CHECKED_OUT. Using `setAutoCommit(false)` and `commit()`, either both succeed or both are rolled back."* |
| **Q2: Why use `BigDecimal` instead of `double`?** | *"`double` produces binary floating-point rounding errors (e.g. `0.1 + 0.2 = 0.30000000000000004`). `BigDecimal` provides exact precision, which is required for currency and tax auditing."* |
| **Q3: How does your module connect to Member 3 (Hotel Services)?** | *"Member 3 records room amenities and food orders in the `hotel_services` table. During billing, the service charges are entered and added directly to the guest's checkout bill."* |
| **Q4: How does the UI prevent total tampering?** | *"The `totalField` is set to `setEditable(false)`. It is automatically calculated using Java logic from room rate, nights, services, discount, and tax dropdown."* |
| **Q5: What happens when a checkout succeeds?** | *"The bill is saved in the `bills` table, the reservation status changes from `CHECKED_IN` to `CHECKED_OUT`, the booking disappears from the active dropdown, and an invoice receipt popup is displayed."* |
