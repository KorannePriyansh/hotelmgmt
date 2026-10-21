# GUEST MODULE — COMPLETE LOGIC & VIVA GUIDE

> **Author**: Member 2 (Reservations, Guests & Billing)  
> **Target Audience**: Complete beginner in Java  
> **Files Covered**: 
> 1. [Guest.java](file:///c:/Users/DELL/OneDrive/Desktop/New%20folder%20(3)/src/model/Guest.java) (Data Model)
> 2. [GuestDAO.java](file:///c:/Users/DELL/OneDrive/Desktop/New%20folder%20(3)/src/dao/GuestDAO.java) (Database Logic)
> 3. [GuestPanel.java](file:///c:/Users/DELL/OneDrive/Desktop/New%20folder%20(3)/src/ui/GuestPanel.java) (Graphical User Interface)

---

## 1. Fundamentals: What are `Connection`, `try`, and other JDBC keywords?

Before looking at the code, here are simple real-world explanations of the words you will see repeated everywhere:

### 📞 What is `Connection`?
- **Real-world Analogy**: Imagine your Java program is on a mobile phone, and the MySQL Database is a hotel reception desk in another city.
- In Java, a **`Connection`** is like making a **phone call** to the database.
- While the connection is open, Java can send requests ("save this guest", "give me all guests").
- When done, the call must be hung up (`close()`), otherwise your computer runs out of memory (connection leak).

### 🛡️ What is `try` and `try-with-resources`?
In ordinary Java, code can fail. For example, if MySQL is turned off or you type an invalid character, Java will crash with an error ("Exception").
- **`try { ... } catch (SQLException e) { ... }`**:
  - **`try`**: "Try running this code safely."
  - **`catch`**: "If something goes wrong (e.g., database is offline), do NOT crash! Catch the error and display a friendly message instead."
- **`try (Connection con = DBConnection.getConnection())`** *(called "try-with-resources")*:
  - Notice the parentheses `( ... )` right after `try`.
  - This is a special Java feature that says: *"Open this connection, and **automatically close it** when I'm finished, even if an error happens!"*
  - You never have to manually remember to write `con.close()`.

### 📝 What is `PreparedStatement`?
- It is a safe way to write SQL queries.
- Instead of stitching strings together, we put question marks `?` as placeholders:
  ```sql
  INSERT INTO guests (full_name, phone) VALUES (?, ?)
  ```
- Then we safely fill them: `ps.setString(1, "Rahul"); ps.setString(2, "9876543210");`
- **Why?** It prevents **SQL Injection** (hackers trying to type code into text fields).

### 📋 What is `ResultSet`?
- When you ask MySQL: `SELECT * FROM guests`, MySQL sends back rows of data.
- In Java, that table of returned rows is stored in a **`ResultSet`**.
- We use a while loop: `while (rs.next())` to read one row at a time.

---

## 2. File 1: `Guest.java` (The Model / POJO)

### What is its purpose?
A **POJO** (Plain Old Java Object) is just a digital container or blueprint representing one single guest.

### Key Variables (Encapsulation):
```java
private int guestId;
private String fullName;
private String email;
private String phone;
private String idProof;
private String address;
```
- They are marked `private` so outside code cannot accidentally corrupt them.

### Key Functions Explained:
1. **Constructors**:
   - `public Guest(fullName, email, phone, idProof, address)`: Used when a user types a new guest in the form. (We don't know the `guestId` yet because MySQL creates it).
   - `public Guest(guestId, fullName, email, phone, idProof, address)`: Used when reading an existing guest out of MySQL.
2. **Getters and Setters**:
   - `getFullName()`: Returns the guest's name.
   - `setFullName(String name)`: Allows updating the guest's name safely.
3. **`toString()`**:
   - Formats the guest as a readable string (e.g., `"Rahul Sharma (ID: 1 | Phone: 9876543210)"`), useful for dropdown lists.

---

## 3. File 2: `GuestDAO.java` (The Database Worker)

**DAO** stands for **Data Access Object**. It is the ONLY file in the entire module that speaks SQL to the database.

### Functions Step-by-Step:

#### 1. `addGuest(Guest guest)`
- **Goal**: Insert a new guest into MySQL.
- **How it works**:
  1. Opens a `Connection` to MySQL using `DBConnection.getConnection()`.
  2. Prepares the query: `INSERT INTO guests (full_name, email, phone, id_proof, address) VALUES (?, ?, ?, ?, ?)`.
  3. Fills in the 5 question marks `?` using the guest's getters (`guest.getFullName()`, etc.).
  4. Calls `ps.executeUpdate()`, which tells MySQL to insert the row.
  5. Uses `ps.getGeneratedKeys()` to grab the auto-incremented ID that MySQL created (e.g., ID 4) and assigns it back to the guest object.

#### 2. `getAllGuests()`
- **Goal**: Fetch every registered guest to show in the table.
- **How it works**:
  1. Runs `SELECT * FROM guests ORDER BY guest_id`.
  2. Gets back a `ResultSet rs`.
  3. Loops through each row with `while (rs.next())`.
  4. For every row, creates a `new Guest(...)` and adds it to an `ArrayList<Guest>`.
  5. Returns the complete list.

#### 3. `updateGuest(Guest guest)`
- **Goal**: Edit an existing guest's details without changing their ID.
- **How it works**:
  1. Runs:
     ```sql
     UPDATE guests 
     SET full_name = ?, email = ?, phone = ?, id_proof = ?, address = ? 
     WHERE guest_id = ?
     ```
  2. Notice that `guest_id` is at the very end in the `WHERE` clause.
  3. This ensures **only that specific guest's info is updated, while their ID stays exactly the same**.

#### 4. `deleteGuest(int guestId)`
- **Goal**: Delete a guest.
- **How it works**:
  1. Runs `DELETE FROM guests WHERE guest_id = ?`.
  2. Sets the question mark to the given ID and executes.

---

## 4. File 3: `GuestPanel.java` (The Swing UI Screen)

This file builds what you actually see on your monitor.

### Key Components:
- **`JTextField`**: Textboxes for Name, Phone, Email, ID Proof, Address.
- **`JTable`**: The table at the bottom displaying all guests.
- **`DefaultTableModel`**: The data holder behind the table.

### Key Functions Step-by-Step:

1. **`readForm()`**:
   - Takes whatever the user typed into the textboxes.
   - Checks: Is the name empty? Is the phone empty?
   - If empty, throws an `IllegalArgumentException("Name cannot be empty")`.
   - If valid, packs the values into a `new Guest(...)` object and returns it.

2. **`addGuest()`**:
   - Called when clicking the **Add Guest** button.
   - Calls `readForm()` to validate textboxes.
   - Calls `guestDAO.addGuest(guest)`.
   - Shows a green success popup: *"Guest added successfully! Generated ID: 4"*.
   - Calls `loadGuests()` to refresh the table.

3. **`updateGuest()`**:
   - Called when clicking the **Update Guest** button.
   - Checks if a row in the table is selected (`getSelectedRow()`).
   - Grabs the existing ID from column 0 of that row.
   - Calls `guestDAO.updateGuest(formGuest)` to update MySQL.

4. **`deleteGuest()`**:
   - Pops up a confirmation dialog: *"Are you sure you want to delete this guest?"*.
   - If the user clicks YES, calls `guestDAO.deleteGuest(guestId)`.

5. **`fillFormFromSelectedRow()`**:
   - Whenever you click a row in the table, this function reads the values from that row and automatically types them into the textboxes so you don't have to retype them!

6. **`loadGuests()`**:
   - Empties the table (`setRowCount(0)`).
   - Asks `guestDAO.getAllGuests()` for the latest records from MySQL.
   - Adds each guest as a new row in the table.

---

## 5. Top 5 Viva Questions For Guest Module

| Question | Short Answer to Give |
| :--- | :--- |
| **Q1: What does DAO stand for and why is it used?** | *"Data Access Object. It isolates all database SQL code from the user interface. If the database changes, UI code remains untouched."* |
| **Q2: Why is `guest_id` set to `AUTO_INCREMENT`?** | *"So MySQL automatically assigns unique IDs (1, 2, 3...) sequentially without us having to calculate or risk duplicate numbers."* |
| **Q3: What does `try-with-resources` do in `GuestDAO`?** | *"It automatically closes the database Connection and PreparedStatement when the block completes, preventing memory and connection leaks."* |
| **Q4: How do you prevent SQL Injection?** | *"By using `PreparedStatement` with parameterized queries (`?` placeholders) instead of concatenating raw strings."* |
| **Q5: How does selecting a row populate the textboxes?** | *"The `JTable` has a `ListSelectionListener`. When a row is clicked, `fillFormFromSelectedRow()` reads the values from that row index and sets them into the `JTextField`s."* |
