-- ========================================================
-- HOTEL MANAGEMENT SYSTEM - DATABASE SCHEMA
-- Database: hotel_management
-- Member 2 Module: Guests, Reservations & Billing
-- Fully compatible with Member 1, Member 3 & Member 4
-- ========================================================

CREATE DATABASE IF NOT EXISTS hotel_management;
USE hotel_management;

-- --------------------------------------------------------
-- 1. GUESTS TABLE
-- Stores personal and contact details of hotel guests
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS guests (
    guest_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20) NOT NULL,
    id_proof VARCHAR(50),
    address VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- --------------------------------------------------------
-- 2. ROOMS TABLE (Reference Table for Reservations)
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS rooms (
    room_number VARCHAR(10) PRIMARY KEY,
    room_type VARCHAR(50) NOT NULL,
    price_per_night DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) DEFAULT 'AVAILABLE'
);

-- --------------------------------------------------------
-- 3. RESERVATIONS TABLE
-- Tracks room bookings for each guest
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS reservations (
    reservation_id INT AUTO_INCREMENT PRIMARY KEY,
    guest_id INT NOT NULL,
    room_number VARCHAR(10) NOT NULL,
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    room_rate DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) DEFAULT 'CONFIRMED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (guest_id) REFERENCES guests(guest_id) ON DELETE CASCADE
);

-- --------------------------------------------------------
-- 4. BILLS TABLE
-- Stores invoice details upon checkout
-- --------------------------------------------------------
CREATE TABLE IF NOT EXISTS bills (
    bill_id INT AUTO_INCREMENT PRIMARY KEY,
    reservation_id INT NOT NULL,
    room_charges DECIMAL(10, 2) NOT NULL,
    service_charges DECIMAL(10, 2) DEFAULT 0.00,
    discount DECIMAL(10, 2) DEFAULT 0.00,
    tax_amount DECIMAL(10, 2) NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    payment_mode VARCHAR(30) DEFAULT 'CASH',
    payment_status VARCHAR(20) DEFAULT 'PAID',
    billing_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (reservation_id) REFERENCES reservations(reservation_id) ON DELETE CASCADE
);

-- --------------------------------------------------------
-- SAMPLE SEED DATA FOR TESTING / VIVA DEMO
-- --------------------------------------------------------

-- Insert sample rooms
INSERT IGNORE INTO rooms (room_number, room_type, price_per_night, status) VALUES
('101', 'Deluxe Single', 1500.00, 'AVAILABLE'),
('102', 'Deluxe Double', 2500.00, 'AVAILABLE'),
('201', 'Executive Suite', 4500.00, 'AVAILABLE'),
('202', 'Presidential Suite', 8000.00, 'AVAILABLE'),
('301', 'Standard Single', 1200.00, 'AVAILABLE');

-- Insert sample guests
INSERT IGNORE INTO guests (guest_id, full_name, email, phone, id_proof, address) VALUES
(1, 'Rahul Sharma', 'rahul.sharma@example.com', '9876543210', 'AADHAAR-9812-4412', 'Mumbai, India'),
(2, 'Ananya Patel', 'ananya.p@example.com', '9823456781', 'PASSPORT-Z781290', 'Ahmedabad, India'),
(3, 'Vikram Singh', 'vikram.singh@example.com', '9123456789', 'DL-DL042019992', 'New Delhi, India');

-- Insert sample reservations
INSERT IGNORE INTO reservations (reservation_id, guest_id, room_number, check_in_date, check_out_date, room_rate, status) VALUES
(1, 1, '101', '2026-10-20', '2026-10-23', 1500.00, 'CHECKED_IN'),
(2, 2, '201', '2026-10-21', '2026-10-25', 4500.00, 'CONFIRMED');

-- Insert sample bill
INSERT IGNORE INTO bills (bill_id, reservation_id, room_charges, service_charges, discount, tax_amount, total_amount, payment_mode, payment_status) VALUES
(1, 1, 4500.00, 500.00, 200.00, 576.00, 5376.00, 'CREDIT_CARD', 'PAID');
