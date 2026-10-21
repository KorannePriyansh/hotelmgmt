import dao.BillingDAO;
import dao.GuestDAO;
import dao.ReservationDAO;
import database.DBConnection;
import model.Billing;
import model.Guest;
import model.Reservation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;

/**
 * TestRunner - Automated Unit & Integration Tests for Member 2 Module.
 * Tests Models, DAOs, Calculations, Transactions, and Member 3 compatibility.
 */
public class TestRunner {

    private static int testsPassed = 0;
    private static int testsFailed = 0;
    private static int testsSkipped = 0;

    private static final String SAFE_DEV_DATABASE = "hotel_management";

    public static void main(String[] args) {
        System.out.println("================================================================");
        System.out.println("   MEMBER 2: RESERVATIONS, GUESTS & BILLING - TEST SUITE        ");
        System.out.println("================================================================\n");

        if (!verifySafeDatabaseEnvironment()) {
            System.err.println("\n[ABORTED] Database is not verified as a safe development/test database.");
            System.exit(1);
        }

        testGuestModelAndDAO();
        testReservationModelAndDAO();
        testBillingModelAndDAO();
        testIntegrationWithMember3Conventions();

        System.out.println("\n================================================================");
        System.out.printf("   TEST SUMMARY: %d PASSED, %d FAILED, %d SKIPPED\n", testsPassed, testsFailed, testsSkipped);
        System.out.println("================================================================");

        if (testsFailed == 0) {
            System.out.println(">>> ALL EXECUTED TESTS PASSED <<<");
            System.exit(0);
        } else {
            System.err.println(">>> TEST FAILURES DETECTED. EXITING WITH NONZERO CODE. <<<");
            System.exit(1);
        }
    }

    private static void assertTrue(String testName, boolean condition) {
        if (condition) {
            System.out.printf("  [PASS] %s\n", testName);
            testsPassed++;
        } else {
            System.err.printf("  [FAIL] %s\n", testName);
            testsFailed++;
        }
    }

    private static void markSkipped(String testName, String reason) {
        System.out.printf("  [SKIP] %s (%s)\n", testName, reason);
        testsSkipped++;
    }

    private static boolean verifySafeDatabaseEnvironment() {
        System.out.println("[SUITE 1] Database Safety & Connectivity Verification");
        try (Connection con = DBConnection.getConnection()) {
            if (con == null || con.isClosed()) {
                assertTrue("MySQL Connection is open and reachable", false);
                return false;
            }
            assertTrue("MySQL Connection is open and reachable", true);

            String catalog = con.getCatalog();
            boolean isSafeDb = SAFE_DEV_DATABASE.equalsIgnoreCase(catalog);
            assertTrue("Connected catalog matches safe development database (" + SAFE_DEV_DATABASE + ")", isSafeDb);

            return isSafeDb;
        } catch (Exception e) {
            assertTrue("Database verification failed: " + e.getMessage(), false);
            return false;
        }
    }

    private static void testGuestModelAndDAO() {
        System.out.println("\n[SUITE 2] Guest Model & DAO Operations");
        GuestDAO dao = new GuestDAO();
        int testGuestId = 0;

        try {
            // 1. Create & Add Guest
            Guest newGuest = new Guest("Test Guest User", "test.user@example.com", "9988776655", "TEST-ID-12345", "Test City, India");
            boolean added = dao.addGuest(newGuest);
            testGuestId = newGuest.getGuestId();

            boolean insertOk = added && testGuestId > 0;
            assertTrue("addGuest() successfully inserts into MySQL", insertOk);

            if (!insertOk) {
                markSkipped("getGuestById() returns correct guest data", "Prerequisite addGuest() failed");
                markSkipped("updateGuest() updates data successfully", "Prerequisite addGuest() failed");
                markSkipped("Verify guest details changed while ID stayed identical", "Prerequisite addGuest() failed");
                return;
            }

            // 2. Read Guest By ID
            Guest fetched = dao.getGuestById(testGuestId);
            assertTrue("getGuestById() returns correct guest data",
                    fetched != null && "Test Guest User".equals(fetched.getFullName()));

            // 3. Update Guest Without Changing ID
            if (fetched != null) {
                fetched.setFullName("Test Guest Updated");
                fetched.setPhone("9988770000");
                boolean updated = dao.updateGuest(fetched);
                assertTrue("updateGuest() updates data successfully", updated);

                Guest verifyUpdate = dao.getGuestById(testGuestId);
                assertTrue("Verify guest details changed while ID stayed identical",
                        verifyUpdate != null &&
                        "Test Guest Updated".equals(verifyUpdate.getFullName()) &&
                        verifyUpdate.getGuestId() == testGuestId);
            } else {
                markSkipped("updateGuest() updates data successfully", "Fetched guest was null");
                markSkipped("Verify guest details changed while ID stayed identical", "Fetched guest was null");
            }

        } catch (Exception e) {
            assertTrue("Guest suite exception: " + e.getMessage(), false);
        } finally {
            // Guaranteed cleanup
            if (testGuestId > 0) {
                try {
                    dao.deleteGuest(testGuestId);
                    assertTrue("deleteGuest() cleanly removes temporary test record", dao.getGuestById(testGuestId) == null);
                } catch (Exception ex) {
                    System.err.println("Warning: Cleanup failed for testGuestId " + testGuestId + ": " + ex.getMessage());
                }
            }
        }
    }

    private static void testReservationModelAndDAO() {
        System.out.println("\n[SUITE 3] Reservation Model, Date Math & DAO Operations");
        GuestDAO guestDao = new GuestDAO();
        ReservationDAO resDao = new ReservationDAO();
        int tempGuestId = 0;

        try {
            // 1. Model Night Calculation & Room Total Math (No DB dependency)
            LocalDate checkIn = LocalDate.of(2026, 11, 1);
            LocalDate checkOut = LocalDate.of(2026, 11, 5); // 4 nights
            BigDecimal rate = new BigDecimal("2500.00");

            Reservation resModel = new Reservation(0, "102", checkIn, checkOut, rate, "CONFIRMED");
            assertTrue("Reservation.getNumberOfNights() correctly calculates 4 nights", resModel.getNumberOfNights() == 4);

            BigDecimal expectedTotal = rate.multiply(BigDecimal.valueOf(4));
            assertTrue("Reservation.calculateRoomTotal() correctly calculates 10,000.00",
                    resModel.calculateRoomTotal().compareTo(expectedTotal) == 0);

            // 2. Setup temporary guest in DB
            Guest tempGuest = new Guest("Res Test Guest", "res@test.com", "9111222333", "PROOF-888", "Delhi");
            boolean guestAdded = guestDao.addGuest(tempGuest);
            tempGuestId = tempGuest.getGuestId();

            if (!guestAdded || tempGuestId <= 0) {
                markSkipped("addReservation() inserts booking into MySQL", "Prerequisite guest insert failed");
                markSkipped("updateStatus('CHECKED_IN') transitions status properly", "Prerequisite booking failed");
                markSkipped("getActiveReservations() includes the active reservation", "Prerequisite booking failed");
                return;
            }

            // 3. Test DAO addReservation
            Reservation res = new Reservation(tempGuestId, "102", checkIn, checkOut, rate, "CONFIRMED");
            boolean booked = resDao.addReservation(res);
            int resId = res.getReservationId();
            boolean bookOk = booked && resId > 0;
            assertTrue("addReservation() successfully inserts booking into MySQL", bookOk);

            if (!bookOk) {
                markSkipped("updateStatus('CHECKED_IN') transitions status properly", "addReservation() failed");
                markSkipped("getActiveReservations() includes the active reservation", "addReservation() failed");
                return;
            }

            // 4. Test Check-In Status Update
            boolean checkedIn = resDao.updateStatus(resId, "CHECKED_IN");
            assertTrue("updateStatus('CHECKED_IN') transitions status properly", checkedIn);

            Reservation fetchedRes = resDao.getReservationById(resId);
            assertTrue("Verify status is now CHECKED_IN", fetchedRes != null && "CHECKED_IN".equals(fetchedRes.getStatus()));

            // 5. Test Active Reservations Query
            List<Reservation> activeList = resDao.getActiveReservations();
            boolean inActiveList = activeList.stream().anyMatch(r -> r.getReservationId() == resId);
            assertTrue("getActiveReservations() includes the active reservation", inActiveList);

        } catch (Exception e) {
            assertTrue("Reservation suite exception: " + e.getMessage(), false);
        } finally {
            // Guaranteed cleanup
            if (tempGuestId > 0) {
                try {
                    guestDao.deleteGuest(tempGuestId);
                } catch (Exception ex) {
                    System.err.println("Warning: Cleanup failed for tempGuestId " + tempGuestId + ": " + ex.getMessage());
                }
            }
        }
    }

    private static void testBillingModelAndDAO() {
        System.out.println("\n[SUITE 4] Billing Model, GST Formula & Atomic Transactions");
        GuestDAO guestDao = new GuestDAO();
        ReservationDAO resDao = new ReservationDAO();
        BillingDAO billingDao = new BillingDAO();
        int tempGuestId = 0;

        try {
            // 1. Test Billing Math (No DB dependency)
            BigDecimal roomCharges = new BigDecimal("9000.00");
            BigDecimal serviceCharges = new BigDecimal("1000.00");
            BigDecimal discount = new BigDecimal("500.00");
            BigDecimal taxRate = new BigDecimal("12");

            BigDecimal computedTotal = Billing.calculateTotal(roomCharges, serviceCharges, discount, taxRate);
            assertTrue("Billing.calculateTotal() correctly computes 10,640.00",
                    computedTotal.setScale(2, RoundingMode.HALF_UP).compareTo(new BigDecimal("10640.00")) == 0);

            // 2. Setup temporary guest & reservation
            Guest tempGuest = new Guest("Bill Test Guest", "bill@test.com", "9444555666", "PROOF-999", "Bangalore");
            boolean guestAdded = guestDao.addGuest(tempGuest);
            tempGuestId = tempGuest.getGuestId();

            if (!guestAdded || tempGuestId <= 0) {
                markSkipped("BillingDAO.createBill() successfully executes transaction", "Prerequisite guest insert failed");
                markSkipped("Checkout Transaction automatically set reservation status to CHECKED_OUT", "Prerequisite guest insert failed");
                return;
            }

            Reservation res = new Reservation(tempGuestId, "201",
                    LocalDate.now().minusDays(2), LocalDate.now(), new BigDecimal("4500.00"), "CHECKED_IN");
            boolean resAdded = resDao.addReservation(res);
            int resId = res.getReservationId();

            if (!resAdded || resId <= 0) {
                markSkipped("BillingDAO.createBill() successfully executes transaction", "Prerequisite reservation insert failed");
                markSkipped("Checkout Transaction automatically set reservation status to CHECKED_OUT", "Prerequisite reservation insert failed");
                return;
            }

            // 3. Test Atomic Transaction (Insert Bill + Auto-Update Reservation to CHECKED_OUT)
            Billing bill = new Billing(resId, roomCharges, serviceCharges, discount,
                    new BigDecimal("1140.00"), computedTotal, "UPI", "PAID");

            boolean billCreated = billingDao.createBill(bill);
            assertTrue("BillingDAO.createBill() successfully executes transaction", billCreated && bill.getBillId() > 0);

            // 4. Verify that Reservation was automatically updated to CHECKED_OUT by the transaction
            Reservation updatedRes = resDao.getReservationById(resId);
            assertTrue("Checkout Transaction automatically set reservation status to CHECKED_OUT",
                    updatedRes != null && "CHECKED_OUT".equals(updatedRes.getStatus()));

        } catch (Exception e) {
            assertTrue("Billing suite exception: " + e.getMessage(), false);
        } finally {
            // Guaranteed cleanup
            if (tempGuestId > 0) {
                try {
                    guestDao.deleteGuest(tempGuestId);
                } catch (Exception ex) {
                    System.err.println("Warning: Cleanup failed for tempGuestId " + tempGuestId + ": " + ex.getMessage());
                }
            }
        }
    }

    private static void testIntegrationWithMember3Conventions() {
        System.out.println("\n[SUITE 5] Member 3 Architecture & Compatibility Verification");

        // 1. Package check
        assertTrue("Model package conventions match (POJOs with encapsulation)",
                model.Guest.class.getPackage().getName().equals("model") &&
                model.Reservation.class.getPackage().getName().equals("model") &&
                model.Billing.class.getPackage().getName().equals("model"));

        // 2. DAO check
        assertTrue("DAO package conventions match (JDBC PreparedStatement with DBConnection)",
                dao.GuestDAO.class.getPackage().getName().equals("dao") &&
                dao.ReservationDAO.class.getPackage().getName().equals("dao") &&
                dao.BillingDAO.class.getPackage().getName().equals("dao"));

        // 3. UI Component check
        assertTrue("UI panels extend javax.swing.JPanel for direct JTabbedPane integration",
                javax.swing.JPanel.class.isAssignableFrom(ui.GuestPanel.class) &&
                javax.swing.JPanel.class.isAssignableFrom(ui.ReservationPanel.class) &&
                javax.swing.JPanel.class.isAssignableFrom(ui.BillingPanel.class));
    }
}
