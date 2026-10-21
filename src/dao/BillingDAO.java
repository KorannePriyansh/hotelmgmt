package dao;

import database.DBConnection;
import model.Billing;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * BillingDAO - Data Access Object for Bills and Invoicing.
 * Handles bill generation, tax/discount calculation storage, and checkout status update.
 */
public class BillingDAO {

    // CREATE: Generate and save a new bill
    public boolean createBill(Billing bill) throws SQLException {
        String insertBillSql = """
            INSERT INTO bills
            (reservation_id, room_charges, service_charges, discount, tax_amount, total_amount, payment_mode, payment_status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        String updateResSql = "UPDATE reservations SET status = 'CHECKED_OUT' WHERE reservation_id = ?";

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false); // Begin transaction

            try (PreparedStatement psBill = con.prepareStatement(insertBillSql, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement psRes = con.prepareStatement(updateResSql)) {

                psBill.setInt(1, bill.getReservationId());
                psBill.setBigDecimal(2, bill.getRoomCharges());
                psBill.setBigDecimal(3, bill.getServiceCharges());
                psBill.setBigDecimal(4, bill.getDiscount());
                psBill.setBigDecimal(5, bill.getTaxAmount());
                psBill.setBigDecimal(6, bill.getTotalAmount());
                psBill.setString(7, bill.getPaymentMode());
                psBill.setString(8, bill.getPaymentStatus());

                int affected = psBill.executeUpdate();
                if (affected > 0) {
                    try (ResultSet generatedKeys = psBill.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            bill.setBillId(generatedKeys.getInt(1));
                        }
                    }

                    // Automatically update reservation status to CHECKED_OUT
                    psRes.setInt(1, bill.getReservationId());
                    psRes.executeUpdate();

                    con.commit(); // Commit transaction
                    return true;
                } else {
                    con.rollback();
                    return false;
                }
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    // READ: Retrieve all billing history with guest name and room number joined
    public List<Billing> getAllBills() throws SQLException {
        List<Billing> list = new ArrayList<>();
        String sql = """
            SELECT b.bill_id, b.reservation_id, g.full_name AS guest_name, r.room_number,
                   b.room_charges, b.service_charges, b.discount, b.tax_amount, b.total_amount,
                   b.payment_mode, b.payment_status, DATE(b.billing_date) AS bill_date
            FROM bills b
            JOIN reservations r ON b.reservation_id = r.reservation_id
            JOIN guests g ON r.guest_id = g.guest_id
            ORDER BY b.bill_id DESC
            """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Date d = rs.getDate("bill_date");
                Billing bill = new Billing(
                    rs.getInt("bill_id"),
                    rs.getInt("reservation_id"),
                    rs.getString("guest_name"),
                    rs.getString("room_number"),
                    rs.getBigDecimal("room_charges"),
                    rs.getBigDecimal("service_charges"),
                    rs.getBigDecimal("discount"),
                    rs.getBigDecimal("tax_amount"),
                    rs.getBigDecimal("total_amount"),
                    rs.getString("payment_mode"),
                    rs.getString("payment_status"),
                    d != null ? d.toLocalDate() : null
                );
                list.add(bill);
            }
        }
        return list;
    }

    // UPDATE: Update payment status (e.g. from PENDING to PAID)
    public boolean updatePaymentStatus(int billId, String paymentStatus, String paymentMode) throws SQLException {
        String sql = "UPDATE bills SET payment_status = ?, payment_mode = ? WHERE bill_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, paymentStatus);
            ps.setString(2, paymentMode);
            ps.setInt(3, billId);

            return ps.executeUpdate() > 0;
        }
    }
}
