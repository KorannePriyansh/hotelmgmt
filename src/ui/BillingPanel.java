package ui;

import dao.BillingDAO;
import dao.ReservationDAO;
import model.Billing;
import model.Reservation;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.List;

/**
 * BillingPanel - UI for Generating Invoices and Processing Guest Checkout.
 * Calculates room nights * rate, taxes, discounts, and prints receipt dialogs.
 */
public class BillingPanel extends JPanel {

    private final BillingDAO billingDAO;
    private final ReservationDAO reservationDAO;

    // Form Inputs
    private final JComboBox<ResComboItem> resComboBox = new JComboBox<>();
    private final JTextField roomChargesField = new JTextField(10);
    private final JTextField serviceChargesField = new JTextField("0.00", 10);
    private final JTextField discountField = new JTextField("0.00", 10);
    private final JComboBox<String> taxBox = new JComboBox<>(new String[]{"12% GST", "18% GST", "5% GST", "0% None"});
    private final JTextField totalField = new JTextField("0.00", 12);
    private final JComboBox<String> paymentModeBox = new JComboBox<>(new String[]{
        "CASH", "CREDIT_CARD", "DEBIT_CARD", "UPI", "NET_BANKING"
    });
    private final JComboBox<String> paymentStatusBox = new JComboBox<>(new String[]{"PAID", "PENDING"});

    // Table
    private final DefaultTableModel tableModel;
    private final JTable billTable;

    // Helper item for Reservation dropdown
    private static class ResComboItem {
        final int id;
        final String label;
        final BigDecimal calculatedRoomCharges;
        final String guestName;
        final String roomNumber;
        final long nights;

        ResComboItem(Reservation r) {
            this.id = r.getReservationId();
            this.nights = r.getNumberOfNights();
            this.calculatedRoomCharges = r.calculateRoomTotal();
            this.guestName = r.getGuestName();
            this.roomNumber = r.getRoomNumber();
            this.label = "Booking #" + id + " - " + guestName + " (Room " + roomNumber + " | " + nights + " nights)";
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public BillingPanel() {
        this.billingDAO = new BillingDAO();
        this.reservationDAO = new ReservationDAO();

        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);

        // 1. Header
        add(UITheme.createHeader("Billing & Checkout", "Calculate stay charges, add taxes & discounts, and process guest checkout."), BorderLayout.NORTH);

        // 2. Center Content: Form + Table
        JPanel centerPanel = new JPanel(new BorderLayout(10, 15));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 10, 20));

        centerPanel.add(createFormPanel(), BorderLayout.NORTH);

        // Table setup
        String[] columnNames = {"Bill #", "Res #", "Guest Name", "Room", "Room (₹)", "Services (₹)", "Discount", "Tax (₹)", "Total (₹)", "Mode", "Status", "Date"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        billTable = new JTable(tableModel);
        UITheme.styleTable(billTable);

        JScrollPane scrollPane = new JScrollPane(billTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UITheme.COLOR_BORDER),
                "Billing & Invoicing History", 0, 0, UITheme.FONT_SECTION, new Color(50, 60, 75)
        ));
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // 3. Bottom Action Buttons
        add(createButtonsPanel(), BorderLayout.SOUTH);

        // Event listeners to auto-recalculate total whenever amounts change
        resComboBox.addActionListener(e -> onReservationSelected());
        serviceChargesField.addActionListener(e -> calculateTotal());
        discountField.addActionListener(e -> calculateTotal());
        taxBox.addActionListener(e -> calculateTotal());

        // Make Total field distinct and uneditable
        totalField.setEditable(false);
        totalField.setFont(new Font("Segoe UI", Font.BOLD, 14));
        totalField.setForeground(new Color(22, 101, 116));

        // Initial Data Load
        reloadActiveReservations();
        loadBills();
    }

    private JPanel createFormPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UITheme.COLOR_CARD_BG);
        form.setBorder(UITheme.createCardBorder("Checkout & Invoice Details"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 10, 6, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Column 1 (Left): Reservation, Room Charges, Service Charges, Discount
        addFormField(form, gbc, 0, 0, "Select Booking:", resComboBox);
        addFormField(form, gbc, 0, 1, "Room Charges (₹):", roomChargesField);
        addFormField(form, gbc, 0, 2, "Service Charges (₹):", serviceChargesField);
        addFormField(form, gbc, 0, 3, "Discount (₹):", discountField);

        // Column 2 (Right): Tax, Total, Payment Mode, Status
        addFormField(form, gbc, 2, 0, "Tax / GST Rate:", taxBox);
        addFormField(form, gbc, 2, 1, "Total Amount (₹):", totalField);
        addFormField(form, gbc, 2, 2, "Payment Mode:", paymentModeBox);
        addFormField(form, gbc, 2, 3, "Payment Status:", paymentStatusBox);

        return form;
    }

    private void addFormField(JPanel panel, GridBagConstraints gbc, int col, int row, String label, Component input) {
        gbc.gridx = col;
        gbc.gridy = row;
        gbc.weightx = 0;
        JLabel lbl = new JLabel(label);
        lbl.setFont(UITheme.FONT_LABEL);
        panel.add(lbl, gbc);

        gbc.gridx = col + 1;
        gbc.weightx = 1.0;
        input.setFont(UITheme.FONT_INPUT);
        panel.add(input, gbc);
    }

    private JPanel createButtonsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 20, 15, 20));

        JButton checkoutBtn = UITheme.createButton("Generate Bill & Checkout", UITheme.COLOR_PRIMARY, Color.WHITE);
        JButton receiptBtn = UITheme.createButton("View Receipt", UITheme.COLOR_NAVY, Color.WHITE);
        JButton calculateBtn = UITheme.createButton("Calculate Total", new Color(240, 243, 246), new Color(40, 50, 60));
        JButton refreshBtn = UITheme.createButton("Refresh", new Color(240, 243, 246), new Color(40, 50, 60));

        checkoutBtn.addActionListener(e -> generateBill());
        receiptBtn.addActionListener(e -> showSelectedReceipt());
        calculateBtn.addActionListener(e -> calculateTotal());
        refreshBtn.addActionListener(e -> {
            reloadActiveReservations();
            loadBills();
        });

        panel.add(calculateBtn);
        panel.add(checkoutBtn);
        panel.add(receiptBtn);
        panel.add(refreshBtn);

        return panel;
    }

    public void reloadActiveReservations() {
        try {
            resComboBox.removeAllItems();
            List<Reservation> activeList = reservationDAO.getActiveReservations();
            for (Reservation r : activeList) {
                resComboBox.addItem(new ResComboItem(r));
            }
            if (resComboBox.getItemCount() > 0) {
                onReservationSelected();
            } else {
                roomChargesField.setText("0.00");
                totalField.setText("0.00");
            }
        } catch (SQLException e) {
            // silent or handled on table load
        }
    }

    private void onReservationSelected() {
        ResComboItem item = (ResComboItem) resComboBox.getSelectedItem();
        if (item != null) {
            roomChargesField.setText(item.calculatedRoomCharges.setScale(2, RoundingMode.HALF_UP).toString());
            calculateTotal();
        }
    }

    private BigDecimal calculateTotal() {
        try {
            BigDecimal room = new BigDecimal(roomChargesField.getText().trim());
            BigDecimal service = new BigDecimal(serviceChargesField.getText().trim());
            BigDecimal discount = new BigDecimal(discountField.getText().trim());

            String taxStr = (String) taxBox.getSelectedItem();
            BigDecimal taxRate = BigDecimal.ZERO;
            if (taxStr != null && taxStr.contains("%")) {
                String pct = taxStr.substring(0, taxStr.indexOf('%')).trim();
                taxRate = new BigDecimal(pct);
            }

            BigDecimal subtotal = room.add(service).subtract(discount);
            if (subtotal.compareTo(BigDecimal.ZERO) < 0) {
                subtotal = BigDecimal.ZERO;
            }

            BigDecimal taxAmt = subtotal.multiply(taxRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal total = subtotal.add(taxAmt);

            totalField.setText(total.setScale(2, RoundingMode.HALF_UP).toString());
            return total;
        } catch (Exception ex) {
            totalField.setText("0.00");
            return BigDecimal.ZERO;
        }
    }

    private void generateBill() {
        ResComboItem selectedRes = (ResComboItem) resComboBox.getSelectedItem();
        if (selectedRes == null) {
            JOptionPane.showMessageDialog(this, "No active reservation selected for checkout.", "Notice", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            BigDecimal room = new BigDecimal(roomChargesField.getText().trim());
            BigDecimal service = new BigDecimal(serviceChargesField.getText().trim());
            BigDecimal discount = new BigDecimal(discountField.getText().trim());
            BigDecimal total = calculateTotal();

            String taxStr = (String) taxBox.getSelectedItem();
            BigDecimal taxRate = BigDecimal.ZERO;
            if (taxStr != null && taxStr.contains("%")) {
                taxRate = new BigDecimal(taxStr.substring(0, taxStr.indexOf('%')).trim());
            }
            BigDecimal subtotal = room.add(service).subtract(discount);
            BigDecimal taxAmt = subtotal.multiply(taxRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            String mode = (String) paymentModeBox.getSelectedItem();
            String status = (String) paymentStatusBox.getSelectedItem();

            Billing bill = new Billing(
                selectedRes.id, room, service, discount, taxAmt, total,
                mode != null ? mode : "CASH", status != null ? status : "PAID"
            );

            if (billingDAO.createBill(bill)) {
                showReceiptDialog(selectedRes.guestName, selectedRes.roomNumber, selectedRes.nights,
                                 room, service, discount, taxAmt, total, mode, bill.getBillId());

                reloadActiveReservations();
                loadBills();
            }
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Input Validation", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    private void showReceiptDialog(String guest, String room, long nights, BigDecimal roomCost,
                                   BigDecimal serviceCost, BigDecimal discount, BigDecimal tax,
                                   BigDecimal total, String mode, int billId) {

        String receipt = String.format("""
            ====================================================
                        GRAND HOTEL & RESORTS INNAURA           
                           FINAL CHECKOUT RECEIPT               
            ====================================================
            Invoice Number : #BILL-%05d
            Guest Name     : %s
            Room Number    : Room %s
            Duration       : %d Night(s)
            Payment Mode   : %s
            ----------------------------------------------------
            Room Charges                     : ₹ %.2f
            Room Service / Amenities         : ₹ %.2f
            Special Discount                 : - ₹ %.2f
            ----------------------------------------------------
            GST / Tax Amount                 : + ₹ %.2f
            ====================================================
            TOTAL AMOUNT PAID                : ₹ %.2f
            ====================================================
                   Thank you for staying with us!               
                     We hope to see you again soon!             
            ====================================================
            """, billId, guest, room, nights, mode, roomCost, serviceCost, discount, tax, total);

        JTextArea area = new JTextArea(receipt);
        area.setFont(new Font("Consolas", Font.PLAIN, 13));
        area.setEditable(false);
        area.setBackground(new Color(250, 250, 250));
        area.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Checkout Completed - Receipt #BILL-" + billId, JOptionPane.INFORMATION_MESSAGE);
    }

    private void showSelectedReceipt() {
        int row = billTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a bill from the history table below to view its receipt.", "Select a Bill", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int billId = (int) tableModel.getValueAt(row, 0);
        String guest = String.valueOf(tableModel.getValueAt(row, 2));
        String room = String.valueOf(tableModel.getValueAt(row, 3));
        BigDecimal roomCost = (BigDecimal) tableModel.getValueAt(row, 4);
        BigDecimal serviceCost = (BigDecimal) tableModel.getValueAt(row, 5);
        BigDecimal discount = (BigDecimal) tableModel.getValueAt(row, 6);
        BigDecimal tax = (BigDecimal) tableModel.getValueAt(row, 7);
        BigDecimal total = (BigDecimal) tableModel.getValueAt(row, 8);
        String mode = String.valueOf(tableModel.getValueAt(row, 9));

        showReceiptDialog(guest, room, 1, roomCost, serviceCost, discount, tax, total, mode, billId);
    }

    public void loadBills() {
        try {
            tableModel.setRowCount(0);
            for (Billing b : billingDAO.getAllBills()) {
                tableModel.addRow(new Object[]{
                    b.getBillId(),
                    b.getReservationId(),
                    b.getGuestName(),
                    b.getRoomNumber(),
                    b.getRoomCharges(),
                    b.getServiceCharges(),
                    b.getDiscount(),
                    b.getTaxAmount(),
                    b.getTotalAmount(),
                    b.getPaymentMode(),
                    b.getPaymentStatus(),
                    b.getBillingDate()
                });
            }
        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    private void showDatabaseError(SQLException ex) {
        JOptionPane.showMessageDialog(this, "Database error:\n" + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
    }
}
