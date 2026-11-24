import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

/**
 * Main.java
 * Console-based User Interface for Smart Clinic Visit Tracker.
 * Demonstrates:
 * - Interactive Menu-driven system using Scanner and switch-case.
 * - Input validation and graceful error handling.
 * - Clean terminal presentation with headings and tables.
 */
public class Main {
    private static ClinicManager manager;
    private static Scanner scanner;

    public static void main(String[] args) {
        scanner = new Scanner(System.in);
        manager = new ClinicManager();

        // Seed demo data if database is fresh/empty for instant college viva demo
        seedDemoDataIfEmpty();

        boolean running = true;
        while (running) {
            printMainMenu();
            System.out.print(">> Enter your choice (1-12): ");
            String input = scanner.nextLine().trim();

            int choice;
            try {
                choice = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("\n[ERROR] Invalid input! Please enter a number between 1 and 12.\n");
                continue;
            }

            switch (choice) {
                case 1:
                    registerPatient();
                    break;
                case 2:
                    viewAllPatients();
                    break;
                case 3:
                    searchPatient();
                    break;
                case 4:
                    scheduleAppointment();
                    break;
                case 5:
                    rescheduleAppointment();
                    break;
                case 6:
                    cancelAppointment();
                    break;
                case 7:
                    viewAppointments();
                    break;
                case 8:
                    callNextPatientInQueue();
                    break;
                case 9:
                    completeConsultation();
                    break;
                case 10:
                    viewPatientVisitHistory();
                    break;
                case 11:
                    generateDailyReports();
                    break;
                case 12:
                    System.out.println("\n========================================================");
                    System.out.println("  Thank you for using Smart Clinic Visit Tracker!");
                    System.out.println("  All records safely stored in SQLite Database.");
                    System.out.println("========================================================\n");
                    running = false;
                    break;
                default:
                    System.out.println("\n[ERROR] Choice out of range! Please choose between 1 and 12.\n");
            }
        }
        scanner.close();
    }

    private static void printMainMenu() {
        System.out.println("\n=================================================================");
        System.out.println("              SMART CLINIC VISIT TRACKER (JAVA-P002)             ");
        System.out.println("=================================================================");
        System.out.println("  1.  Register New Patient");
        System.out.println("  2.  View All Patients");
        System.out.println("  3.  Search Patient by ID");
        System.out.println("  4.  Schedule Appointment (Generates Token)");
        System.out.println("  5.  Reschedule Appointment");
        System.out.println("  6.  Cancel Appointment");
        System.out.println("  7.  View Appointment List (Sort by Token/Time/Name)");
        System.out.println("  8.  Call Next Patient in Queue (FIFO Token Process)");
        System.out.println("  9.  Complete Consultation & Add Doctor Notes");
        System.out.println("  10. View Patient Visit History");
        System.out.println("  11. Generate Daily Reports & Analytics (Streams)");
        System.out.println("  12. Exit Application");
        System.out.println("=================================================================");
    }

    // 1. Register Patient
    private static void registerPatient() {
        System.out.println("\n--- [1] REGISTER NEW PATIENT ---");
        System.out.print("Enter Patient Name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            System.out.println("[ERROR] Patient name cannot be empty.");
            return;
        }

        System.out.print("Enter Age: ");
        int age;
        try {
            age = Integer.parseInt(scanner.nextLine().trim());
            if (age <= 0 || age > 130) {
                System.out.println("[ERROR] Please enter a valid age.");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("[ERROR] Invalid age format.");
            return;
        }

        System.out.print("Enter Gender (Male/Female/Other): ");
        String gender = scanner.nextLine().trim();

        System.out.print("Enter Phone Number: ");
        String phone = scanner.nextLine().trim();

        Patient p = manager.registerPatient(name, age, gender, phone);
        System.out.println("\n[SUCCESS] Patient Registered Successfully!");
        System.out.println("   Assigned ID   : " + p.getId());
        System.out.println("   Name          : " + p.getName());
        System.out.println("   Phone         : " + p.getPhone());
    }

    // 2. View All Patients
    private static void viewAllPatients() {
        System.out.println("\n--- [2] ALL REGISTERED PATIENTS ---");
        List<Patient> list = manager.getAllPatients();
        if (list.isEmpty()) {
            System.out.println("No patients found in the system.");
            return;
        }
        System.out.println("----------------------------------------------------------------------------------------------------");
        System.out.printf("%-8s | %-20s | %-5s | %-8s | %-15s | %-12s\n", "ID", "Name", "Age", "Gender", "Phone", "Visits Done");
        System.out.println("----------------------------------------------------------------------------------------------------");
        for (Patient p : list) {
            System.out.printf("%-8s | %-20s | %-5d | %-8s | %-15s | %-12d\n",
                    p.getId(), p.getName(), p.getAge(), p.getGender(), p.getPhone(), p.getVisitHistory().size());
        }
        System.out.println("----------------------------------------------------------------------------------------------------");
        System.out.println("Total Patients Registered: " + list.size());
    }

    // 3. Search Patient
    private static void searchPatient() {
        System.out.println("\n--- [3] SEARCH PATIENT BY ID ---");
        System.out.print("Enter Patient ID (e.g. P101): ");
        String id = scanner.nextLine().trim();
        Patient p = manager.findPatientById(id);
        if (p == null) {
            System.out.println("[ERROR] Patient with ID '" + id + "' not found.");
            return;
        }
        System.out.println("\n[FOUND] Patient Details:");
        System.out.println("   ID            : " + p.getId());
        System.out.println("   Name          : " + p.getName());
        System.out.println("   Age           : " + p.getAge());
        System.out.println("   Gender        : " + p.getGender());
        System.out.println("   Phone         : " + p.getPhone());
        System.out.println("   Total Visits  : " + p.getVisitHistory().size());
    }

    // 4. Schedule Appointment
    private static void scheduleAppointment() {
        System.out.println("\n--- [4] SCHEDULE NEW APPOINTMENT ---");
        System.out.print("Enter Patient ID (e.g. P101): ");
        String pid = scanner.nextLine().trim();
        Patient p = manager.findPatientById(pid);
        if (p == null) {
            System.out.println("[ERROR] Patient not found. Please register the patient first.");
            return;
        }

        System.out.print("Enter Appointment Date (YYYY-MM-DD) [Press Enter for Today]: ");
        String date = scanner.nextLine().trim();
        if (date.isEmpty()) {
            date = LocalDate.now().toString();
        }

        System.out.print("Enter Appointment Time (HH:mm, e.g. 10:30): ");
        String time = scanner.nextLine().trim();
        if (time.isEmpty()) {
            time = "10:00";
        }

        System.out.print("Enter Reason for Visit / Symptoms: ");
        String reason = scanner.nextLine().trim();

        Appointment apt = manager.scheduleAppointment(pid, date, time, reason);
        if (apt != null) {
            System.out.println("\n[SUCCESS] Appointment Scheduled Successfully!");
            System.out.println("   Appointment ID: " + apt.getAppointmentId());
            System.out.println("   Patient Name  : " + apt.getPatientName());
            System.out.println("   Token Number  : #" + apt.getTokenNumber() + " (Added to FIFO Queue)");
            System.out.println("   Date & Time   : " + apt.getAppointmentDate() + " at " + apt.getAppointmentTime());
            System.out.println("   Status        : " + apt.getStatus());
        }
    }

    // 5. Reschedule Appointment
    private static void rescheduleAppointment() {
        System.out.println("\n--- [5] RESCHEDULE APPOINTMENT ---");
        System.out.print("Enter Appointment ID (e.g. APT101): ");
        String aptId = scanner.nextLine().trim();

        Appointment apt = manager.findAppointmentById(aptId);
        if (apt == null) {
            System.out.println("[ERROR] Appointment ID not found.");
            return;
        }

        if (Appointment.STATUS_COMPLETED.equalsIgnoreCase(apt.getStatus()) ||
            Appointment.STATUS_CANCELLED.equalsIgnoreCase(apt.getStatus())) {
            System.out.println("[ERROR] Cannot reschedule! This appointment is already " + apt.getStatus() + ".");
            return;
        }

        System.out.println("Current Schedule: " + apt.getAppointmentDate() + " at " + apt.getAppointmentTime());
        System.out.print("Enter New Date (YYYY-MM-DD): ");
        String newDate = scanner.nextLine().trim();
        System.out.print("Enter New Time (HH:mm): ");
        String newTime = scanner.nextLine().trim();

        boolean success = manager.rescheduleAppointment(aptId, newDate, newTime);
        if (success) {
            System.out.println("[SUCCESS] Appointment rescheduled successfully to " + newDate + " at " + newTime + ".");
        } else {
            System.out.println("[ERROR] Reschedule failed.");
        }
    }

    // 6. Cancel Appointment
    private static void cancelAppointment() {
        System.out.println("\n--- [6] CANCEL APPOINTMENT ---");
        System.out.print("Enter Appointment ID to Cancel (e.g. APT101): ");
        String aptId = scanner.nextLine().trim();

        Appointment apt = manager.findAppointmentById(aptId);
        if (apt == null) {
            System.out.println("[ERROR] Appointment ID not found.");
            return;
        }

        if (Appointment.STATUS_COMPLETED.equalsIgnoreCase(apt.getStatus())) {
            System.out.println("[ERROR] Cannot cancel! This consultation is already COMPLETED.");
            return;
        }

        System.out.print("Are you sure you want to cancel appointment for " + apt.getPatientName() + "? (Y/N): ");
        String confirm = scanner.nextLine().trim();
        if (confirm.equalsIgnoreCase("Y")) {
            manager.cancelAppointment(aptId);
            System.out.println("[SUCCESS] Appointment " + aptId + " has been CANCELLED and removed from active queue.");
        } else {
            System.out.println("Operation aborted.");
        }
    }

    // 7. View Appointments (with Comparator Sorting)
    private static void viewAppointments() {
        System.out.println("\n--- [7] VIEW APPOINTMENTS (COMPARATOR SORTING) ---");
        System.out.println("Choose sorting order:");
        System.out.println("  1. Sort by Token Number (Default FIFO)");
        System.out.println("  2. Sort by Appointment Date & Time");
        System.out.println("  3. Sort by Patient Name");
        System.out.print("Enter choice (1-3): ");
        String sortChoice = scanner.nextLine().trim();

        Comparator<Appointment> comparator = ClinicManager.BY_TOKEN;
        String sortLabel = "Token Number";
        if ("2".equals(sortChoice)) {
            comparator = ClinicManager.BY_TIME;
            sortLabel = "Date & Time";
        } else if ("3".equals(sortChoice)) {
            comparator = ClinicManager.BY_PATIENT_NAME;
            sortLabel = "Patient Name";
        }

        List<Appointment> list = manager.getAppointmentsSorted(comparator);
        if (list.isEmpty()) {
            System.out.println("No appointments recorded yet.");
            return;
        }

        System.out.println("\nShowing appointments sorted by: " + sortLabel);
        System.out.println("---------------------------------------------------------------------------------------------------------------------------");
        System.out.printf("%-8s | %-6s | %-11s | %-6s | %-18s | %-6s | %-11s | %-20s\n",
                "Apt ID", "Token", "Date", "Time", "Patient Name", "PID", "Status", "Reason");
        System.out.println("---------------------------------------------------------------------------------------------------------------------------");
        for (Appointment a : list) {
            System.out.printf("%-8s | #%-5d | %-11s | %-6s | %-18s | %-6s | %-11s | %-20s\n",
                    a.getAppointmentId(), a.getTokenNumber(), a.getAppointmentDate(), a.getAppointmentTime(),
                    a.getPatientName(), a.getPatientId(), a.getStatus(), a.getReason());
        }
        System.out.println("---------------------------------------------------------------------------------------------------------------------------");
    }

    // 8. Call Next Patient (FIFO Token Queue)
    private static void callNextPatientInQueue() {
        System.out.println("\n--- [8] CALL NEXT PATIENT (FIFO QUEUE PROCESSING) ---");
        Appointment next = manager.callNextPatient();
        if (next == null) {
            System.out.println("[INFO] The waiting line is empty! No scheduled patients currently in queue.");
            return;
        }

        System.out.println("==========================================================");
        System.out.println(">> NOW CALLING INTO DOCTOR'S CABIN:");
        System.out.println("   Token Number   : #" + next.getTokenNumber());
        System.out.println("   Appointment ID : " + next.getAppointmentId());
        System.out.println("   Patient Name   : " + next.getPatientName() + " (ID: " + next.getPatientId() + ")");
        System.out.println("   Scheduled Time : " + next.getAppointmentTime());
        System.out.println("   Reason / Compr : " + next.getReason());
        System.out.println("==========================================================");
        System.out.println(">> Note: After consultation finishes, use Option [9] to record diagnosis.");
    }

    // 9. Complete Consultation & Record Notes
    private static void completeConsultation() {
        System.out.println("\n--- [9] COMPLETE CONSULTATION & RECORD VISIT ---");
        System.out.print("Enter Appointment ID (e.g. APT101): ");
        String aptId = scanner.nextLine().trim();

        Appointment apt = manager.findAppointmentById(aptId);
        if (apt == null) {
            System.out.println("[ERROR] Appointment ID not found.");
            return;
        }

        if (Appointment.STATUS_COMPLETED.equalsIgnoreCase(apt.getStatus())) {
            System.out.println("[ERROR] This consultation is already completed.");
            return;
        }
        if (Appointment.STATUS_CANCELLED.equalsIgnoreCase(apt.getStatus())) {
            System.out.println("[ERROR] This appointment was cancelled.");
            return;
        }

        System.out.println("Patient: " + apt.getPatientName() + " (Token: #" + apt.getTokenNumber() + ")");

        System.out.print("Enter Waiting Time (in minutes, e.g. 15): ");
        int waitTime = 10;
        try {
            waitTime = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException ignored) {}

        System.out.print("Enter Doctor Diagnosis: ");
        String diagnosis = scanner.nextLine().trim();

        System.out.print("Enter Doctor Prescription / Medications: ");
        String prescription = scanner.nextLine().trim();

        VisitRecord record = manager.completeConsultation(aptId, diagnosis, prescription, waitTime);
        if (record != null) {
            System.out.println("\n[SUCCESS] Consultation Recorded Successfully!");
            System.out.println("   Visit Record ID : " + record.getRecordId());
            System.out.println("   Status Changed  : COMPLETED");
            System.out.println("   Waiting Time    : " + waitTime + " mins");
            System.out.println("   Visit record added to patient's medical history.");
        }
    }

    // 10. View Patient Visit History
    private static void viewPatientVisitHistory() {
        System.out.println("\n--- [10] PATIENT VISIT HISTORY ---");
        System.out.print("Enter Patient ID (e.g. P101): ");
        String pid = scanner.nextLine().trim();

        Patient p = manager.findPatientById(pid);
        if (p == null) {
            System.out.println("[ERROR] Patient with ID '" + pid + "' not found.");
            return;
        }

        List<VisitRecord> history = p.getVisitHistory();
        System.out.println("\nMedical Visit History for: " + p.getName() + " (ID: " + p.getId() + ")");
        if (history.isEmpty()) {
            System.out.println("[INFO] No past consultations recorded for this patient yet.");
            return;
        }

        System.out.println("------------------------------------------------------------------------------------------------------------------------");
        System.out.printf("%-8s | %-10s | %-11s | %-10s | %-25s | %-30s\n",
                "Rec ID", "Apt ID", "Visit Date", "Wait Time", "Diagnosis", "Prescription");
        System.out.println("------------------------------------------------------------------------------------------------------------------------");
        for (VisitRecord vr : history) {
            System.out.printf("%-8s | %-10s | %-11s | %-3d mins   | %-25s | %-30s\n",
                    vr.getRecordId(), vr.getAppointmentId(), vr.getVisitDate(),
                    vr.getWaitingTimeMinutes(), vr.getDiagnosis(), vr.getPrescription());
        }
        System.out.println("------------------------------------------------------------------------------------------------------------------------");
    }

    // 11. Generate Daily Reports & Analytics using Java Streams
    private static void generateDailyReports() {
        System.out.println("\n=================================================================");
        System.out.println("          CLINIC ANALYTICS & DAILY REPORTS (JAVA STREAMS)        ");
        System.out.println("=================================================================");

        long totalPatients = manager.getTotalPatientCount();
        long totalAppointments = manager.getTotalAppointmentCount();
        long scheduledCount = manager.getCountByStatus(Appointment.STATUS_SCHEDULED);
        long completedCount = manager.getCountByStatus(Appointment.STATUS_COMPLETED);
        long cancelledCount = manager.getCountByStatus(Appointment.STATUS_CANCELLED);
        double avgWaitTime = manager.getAverageWaitingTime();

        System.out.println("  1. PATIENT SUMMARY:");
        System.out.println("     * Total Registered Patients : " + totalPatients);

        System.out.println("\n  2. APPOINTMENT STATUS BREAKDOWN (Computed via Java Streams):");
        System.out.println("     * Total Appointments Booked: " + totalAppointments);
        System.out.printf ("     * [WAITING]   Scheduled / In Queue : %d (%.1f%%)\n",
                scheduledCount, totalAppointments > 0 ? (scheduledCount * 100.0 / totalAppointments) : 0);
        System.out.printf ("     * [COMPLETED] Consultations Done   : %d (%.1f%%)\n",
                completedCount, totalAppointments > 0 ? (completedCount * 100.0 / totalAppointments) : 0);
        System.out.printf ("     * [CANCELLED] Cancelled Records    : %d (%.1f%%)\n",
                cancelledCount, totalAppointments > 0 ? (cancelledCount * 100.0 / totalAppointments) : 0);

        System.out.println("\n  3. CLINIC PERFORMANCE METRICS:");
        System.out.printf ("     * Average Patient Waiting Time: %.1f minutes\n", avgWaitTime);

        System.out.println("\n  4. CURRENT QUEUE STATUS:");
        System.out.println("     * Patients waiting in queue: " + scheduledCount);

        System.out.println("=================================================================\n");
    }

    /**
     * Seeds initial demo data if the system has 0 patients.
     * Ensures immediate readiness for viva demonstration.
     */
    private static void seedDemoDataIfEmpty() {
        if (manager.getAllPatients().isEmpty()) {
            System.out.println("[Init] Fresh database detected. Loading demonstration data for Dr. Siya's Clinic...");
            manager.seedRichClinicalData();
            System.out.println("[Init] Demonstration data populated successfully!");
        }
    }
}
