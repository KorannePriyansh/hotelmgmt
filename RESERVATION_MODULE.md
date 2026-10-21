# RESERVATION MODULE — COMPLETE LOGIC & VIVA GUIDE

> **Author**: Member 2 (Reservations, Guests & Billing)  
> **Target Audience**: Complete beginner in Java  
> **Files Covered**: 
> 1. [Reservation.java](file:///c:/Users/DELL/OneDrive/Desktop/New%20folder%20(3)/src/model/Reservation.java) (Data Model)
> 2. [ReservationDAO.java](file:///c:/Users/DELL/OneDrive/Desktop/New%20folder%20(3)/src/dao/ReservationDAO.java) (Database Logic)
> 3. [ReservationPanel.java](file:///c:/Users/DELL/OneDrive/Desktop/New%20folder%20(3)/src/ui/ReservationPanel.java) (Graphical User Interface)

---

## 1. Fundamentals: Key Concepts Explained Simply

### 🔗 What is a Foreign Key (`guest_id`)?
- A **Primary Key** is a unique ID for a table (e.g., `guest_id` in `guests`).
- In the `reservations` table, we store `guest_id` so we know **which guest booked the room**.
- Because it points to another table's primary key, it is called a **Foreign Key**.
- **Benefit**: We don't have to duplicate the guest's name, phone, and email inside the reservations table. We simply link to their ID!

### 🤝 What is an SQL `JOIN`?
- In the `reservations` table in MySQL, we only have `guest_id: 4`.
- But humans don't want to see `"Booking for Guest #4"`; they want to see `"Booking for Aryan Patel"`.
- A **`JOIN`** connects both tables together in one query:
  ```sql
  SELECT r.*, g.full_name AS guest_name
  FROM reservations r
  JOIN guests g ON r.guest_id = g.guest_id
  ```
- **How to explain in viva**: *"We perform an INNER JOIN between reservations and guests on `guest_id` so we can display the guest's real name directly on our reservation table."*

### 📅 How Java calculates nights between dates (`LocalDate`)
- Java provides a modern date class: `java.time.LocalDate`.
- To find how many nights a guest stays between `2026-10-21` and `2026-10-25`:
  ```java
  long nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
  ```
- If someone books check-in and check-out on the same day, we make sure nights is at least `1`.

### 🔄 The Reservation Lifecycle (Status values):
A booking goes through distinct phases:
1. **`CONFIRMED`**: Room is booked in advance.
2. **`CHECKED_IN`**: Guest arrives at the hotel and receives room keys.
3. **`CHECKED_OUT`**: Guest pays their bill and leaves (triggers Billing module).
4. **`CANCELLED`**: Guest cancels the trip before arriving.

---

## 2. File 1: `Reservation.java` (The Model)

### Key Variables:
```java
private int reservationId;
private int guestId;
private String guestName;     // Joined from guests table for display
private String roomNumber;    // e.g. "101", "201"
private LocalDate checkInDate;
private LocalDate checkOutDate;
private BigDecimal roomRate;  // Price per night (e.g. 1500.00)
private String status;        // CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED
```

### Key Functions Explained:

1. **`getNumberOfNights()`**:
   ```java
   long nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
   return nights <= 0 ? 1 : nights;
   ```
   - Uses Java's `ChronoUnit.DAYS` to calculate how many days exist between check-in and check-out.

2. **`calculateRoomTotal()`**:
   ```java
   return roomRate.multiply(BigDecimal.valueOf(getNumberOfNights()));
   ```
   - Multiplies the nightly price by the number of nights. For example: ₹1,500 $\times$ 3 nights = ₹4,500.

3. **Getters and Setters**:
   - Provide standard access to fields (Encapsulation).

---

## 3. File 2: `ReservationDAO.java` (Database Operations)

### Functions Step-by-Step:

#### 1. `addReservation(Reservation res)`
- **Goal**: Insert a new booking into MySQL.
- **SQL Query**:
  ```sql
  INSERT INTO reservations 
  (guest_id, room_number, check_in_date, check_out_date, room_rate, status)
  VALUES (?, ?, ?, ?, ?, ?)
  ```
- **How dates are sent to MySQL**:
  - Java uses `LocalDate`, but MySQL expects `java.sql.Date`.
  - We convert it smoothly using: `Date.valueOf(res.getCheckInDate())`.

#### 2. `getAllReservations()`
- **Goal**: Fetch all bookings to display in the UI table.
- Uses the `JOIN` query explained above so `rs.getString("guest_name")` retrieves the real person's name!

#### 3. `updateStatus(int reservationId, String newStatus)`
- **Goal**: Quickly change a reservation's status (for example, clicking "Check-In" changes status to `CHECKED_IN`, or clicking "Cancel" changes it to `CANCELLED`).
- **SQL Query**:
  ```sql
  UPDATE reservations SET status = ? WHERE reservation_id = ?
  ```

#### 4. `getActiveReservations()`
- **Goal**: Pull only active reservations (`status IN ('CONFIRMED', 'CHECKED_IN')`).
- **Why?** When generating a bill in the Billing tab, we only want to show guests who are currently staying or checking out — not bookings that were already checked out or cancelled!

---

## 4. File 3: `ReservationPanel.java` (The UI Screen)

### Key UI Features:
- **`JComboBox<GuestComboItem>`**: A smart dropdown list of all registered guests.
  - *How it works*: Instead of forcing the user to remember guest IDs (e.g., "Guest #4"), the dropdown displays `"Aryan Patel (ID: 4)"`. When selected, it automatically knows the ID is `4`.
- **`JComboBox<String> roomBox`**: Pre-configured hotel rooms with their default prices:
  - `"101 - Deluxe Single (₹1500)"`
  - `"102 - Deluxe Double (₹2500)"`
  - `"201 - Executive Suite (₹4500)"`
  - `"202 - Presidential Suite (₹8000)"`
- **Dynamic Rate Updater**: When you select Room 201, the rate textbox automatically fills with `4500.00`!

### Key Action Functions:

1. **`bookRoom()`**:
   - Reads the selected guest, room, dates, and rate.
   - Validates that check-out date is not before check-in date.
   - Calls `reservationDAO.addReservation(res)`.
   - Table immediately refreshes to show the new booking.

2. **`markCheckIn()`**:
   - User clicks on a reservation in the table and clicks **Check-In Guest**.
   - If the guest is already checked in, warns them.
   - Otherwise, updates status to `CHECKED_IN` in MySQL.

3. **`cancelBooking()`**:
   - Pops up a confirmation dialog: *"Are you sure you want to cancel Reservation #3?"*.
   - If confirmed, updates status to `CANCELLED`.

4. **`reloadGuestsList()`**:
   - Pulls the latest guests from `GuestDAO` to make sure newly added guests appear immediately in the dropdown.

---

## 5. Top 5 Viva Questions For Reservation Module

| Question | Short Answer to Give |
| :--- | :--- |
| **Q1: How do you link a reservation to a guest?** | *"Using the `guest_id` as a Foreign Key in the `reservations` table, which references `guests.guest_id`."* |
| **Q2: Why use `java.time.LocalDate` instead of the old `java.util.Date`?** | *"Java 8's `LocalDate` is immutable, thread-safe, and provides simple methods like `ChronoUnit.DAYS.between()` for calculating date differences."* |
| **Q3: How do you show the guest's name on the reservation table?** | *"By performing an SQL `JOIN` between `reservations` and `guests` on `r.guest_id = g.guest_id` in `ReservationDAO.getAllReservations()`."* |
| **Q4: How do you prevent invalid bookings?** | *"In `readForm()`, we validate that check-out is after check-in, that a guest is selected, and that the nightly rate is greater than zero."* |
| **Q5: What are the possible statuses of a reservation?** | *"`CONFIRMED`, `CHECKED_IN`, `CHECKED_OUT`, and `CANCELLED`."* |
