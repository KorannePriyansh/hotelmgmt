import ui.BillingPanel;
import ui.GuestPanel;
import ui.ReservationPanel;

import javax.swing.*;

/**
 * Standalone entry point for the Reservations, Guests, and Billing module.
 * Coordinates GuestPanel, ReservationPanel, and BillingPanel inside a tabbed view.
 */
public class Main {

    public static void main(String[] args) {

        // Use system look and feel for native OS appearance
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {

            JFrame frame = new JFrame("Hotel Management System - Reservations, Guests & Billing");

            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1150, 780);
            frame.setLocationRelativeTo(null);

            JTabbedPane tabs = new JTabbedPane();

            GuestPanel guestPanel = new GuestPanel();
            ReservationPanel reservationPanel = new ReservationPanel();
            BillingPanel billingPanel = new BillingPanel();

            // Synchronize tab data when switching views
            tabs.addChangeListener(e -> {
                int selectedIndex = tabs.getSelectedIndex();
                if (selectedIndex == 0) {
                    guestPanel.loadGuests();
                } else if (selectedIndex == 1) {
                    reservationPanel.reloadGuestsList();
                    reservationPanel.loadReservations();
                } else if (selectedIndex == 2) {
                    billingPanel.reloadActiveReservations();
                    billingPanel.loadBills();
                }
            });

            // Register module panels
            tabs.addTab("Guest Management", guestPanel);
            tabs.addTab("Room Reservations", reservationPanel);
            tabs.addTab("Billing & Checkout", billingPanel);

            frame.setContentPane(tabs);
            frame.setVisible(true);
        });
    }
}
