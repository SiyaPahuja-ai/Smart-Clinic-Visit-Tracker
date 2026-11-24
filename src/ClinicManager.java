import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.stream.Collectors;

/**
 * ClinicManager.java
 * The central controller that manages clinic operations.
 * Demonstrates:
 * - ArrayList for dynamic collections of Patients and Appointments.
 * - Queue (FIFO) for Token Management and doctor call line.
 * - Comparator for sorting appointments.
 * - Java Streams for filtering and generating analytical reports.
 * - Integration with DatabaseHelper for persistent SQLite storage.
 */
public class ClinicManager {
    // In-memory collections (Data Structures)
    private List<Patient> patients;               // ArrayList storing all patients
    private List<Appointment> appointments;       // ArrayList storing all appointments
    private Queue<Appointment> tokenQueue;        // FIFO Queue for processing patient visits

    // Database access
    private DatabaseHelper dbHelper;

    // Auto-increment counters for IDs
    private int nextPatientNumber = 101;
    private int nextAppointmentNumber = 101;
    private int nextTokenNumber = 101;
    private int nextRecordNumber = 101;

    public ClinicManager() {
        this.patients = new ArrayList<>();
        this.appointments = new ArrayList<>();
        this.tokenQueue = new LinkedList<>();
        this.dbHelper = new DatabaseHelper();

        // 1. Initialize SQLite tables
        dbHelper.initDatabase();

        // 2. Load existing data from DB into in-memory data structures
        loadDataFromDatabase();
    }

    /**
     * Synchronize database records into ArrayLists and Queue on startup.
     */
    private void loadDataFromDatabase() {
        List<Patient> dbPatients = dbHelper.getAllPatients();
        this.patients.addAll(dbPatients);

        List<Appointment> dbAppointments = dbHelper.getAllAppointments();
        this.appointments.addAll(dbAppointments);

        // Load visit histories into patients
        for (Patient p : this.patients) {
            List<VisitRecord> patientVisits = dbHelper.getVisitsForPatient(p.getId());
            p.setVisitHistory(patientVisits);
        }

        // Enqueue all active SCHEDULED appointments into the FIFO queue in token order
        List<Appointment> scheduledList = this.appointments.stream()
                .filter(a -> Appointment.STATUS_SCHEDULED.equalsIgnoreCase(a.getStatus()))
                .sorted(Comparator.comparingInt(Appointment::getTokenNumber))
                .collect(Collectors.toList());

        for (Appointment a : scheduledList) {
            tokenQueue.offer(a);
        }

        // Adjust ID counters based on highest existing numbers
        for (Patient p : this.patients) {
            try {
                int num = Integer.parseInt(p.getId().replaceAll("[^0-9]", ""));
                if (num >= nextPatientNumber) nextPatientNumber = num + 1;
            } catch (Exception ignored) {}
        }

        for (Appointment a : this.appointments) {
            try {
                int aptNum = Integer.parseInt(a.getAppointmentId().replaceAll("[^0-9]", ""));
                if (aptNum >= nextAppointmentNumber) nextAppointmentNumber = aptNum + 1;
                if (a.getTokenNumber() >= nextTokenNumber) nextTokenNumber = a.getTokenNumber() + 1;
            } catch (Exception ignored) {}
        }

        List<VisitRecord> allVisits = dbHelper.getAllVisits();
        for (VisitRecord vr : allVisits) {
            try {
                int recNum = Integer.parseInt(vr.getRecordId().replaceAll("[^0-9]", ""));
                if (recNum >= nextRecordNumber) nextRecordNumber = recNum + 1;
            } catch (Exception ignored) {}

            // Link diagnosis and prescription (dawai) to completed appointments
            Appointment apt = findAppointmentById(vr.getAppointmentId());
            if (apt != null) {
                apt.setDiagnosis(vr.getDiagnosis());
                apt.setPrescription(vr.getPrescription());
            }
        }

        // Auto-seed rich clinical dataset if empty or has minimal records (< 6)
        if (this.patients.size() < 6) {
            seedRichClinicalData();
        }
    }

    /**
     * Seeds comprehensive, realistic OPD clinical dataset for Dr. Siya's clinic.
     * Demonstrates Queue FIFO order, multiple visit histories, and various statuses.
     */
    public void seedRichClinicalData() {
        String today = LocalDate.now().toString();

        // 1. Register 12 diverse patients
        Patient p1 = registerPatient("Aarav Sharma", 29, "Male", "9876543210");
        Patient p2 = registerPatient("Priya Patel", 34, "Female", "9123456780");
        Patient p3 = registerPatient("Rohan Gupta", 48, "Male", "9988776655");
        Patient p4 = registerPatient("Ananya Verma", 22, "Female", "9811223344");
        Patient p5 = registerPatient("Kabir Mehta", 31, "Male", "9898989898");
        Patient p6 = registerPatient("Meera Nair", 56, "Female", "9845123456");
        Patient p7 = registerPatient("Vikram Singh", 42, "Male", "9711223344");
        Patient p8 = registerPatient("Sanya Malhotra", 26, "Female", "9654321098");
        Patient p9 = registerPatient("Rajesh Kumar", 63, "Male", "9810123456");
        Patient p10 = registerPatient("Deepa Iyer", 38, "Female", "9765432190");
        Patient p11 = registerPatient("Arjun Reddy", 19, "Male", "9944556677");
        Patient p12 = registerPatient("Sunita Devi", 51, "Female", "9871122334");

        // 2. Schedule appointments
        Appointment a1 = scheduleAppointment(p1.getId(), today, "09:30", "Acute Viral Bronchitis & Cold");
        Appointment a2 = scheduleAppointment(p2.getId(), today, "10:00", "Chronic Migraine & Tension Headache");
        Appointment a3 = scheduleAppointment(p3.getId(), today, "10:30", "Hypertension & Diabetes Review");
        Appointment a4 = scheduleAppointment(p4.getId(), today, "11:00", "Contact Dermatitis & Eczema");
        Appointment a5 = scheduleAppointment(p5.getId(), today, "11:30", "Seasonal Asthma & Chest Congestion");
        Appointment a6 = scheduleAppointment(p6.getId(), today, "11:45", "Bilateral Knee Osteoarthritis");
        Appointment a7 = scheduleAppointment(p7.getId(), today, "12:00", "GERD & Persistent Hyperacidity");
        Appointment a8 = scheduleAppointment(p8.getId(), today, "12:15", "Preventive Annual Health Checkup");
        Appointment a9 = scheduleAppointment(p9.getId(), today, "12:30", "Post-Cardiac Angina Follow-up");
        Appointment a10 = scheduleAppointment(p10.getId(), today, "12:45", "Allergic Rhinitis & Eye Strain");
        Appointment a11 = scheduleAppointment(p11.getId(), today, "13:00", "Right Ankle Sports Sprain");
        Appointment a12 = scheduleAppointment(p12.getId(), today, "14:00", "Hypothyroidism & Chronic Fatigue");

        // 3. Mark completed consultations with realistic diagnosis and prescriptions by Dr. Siya
        completeConsultation(a1.getAppointmentId(), "Upper Respiratory Infection with Bronchospasm",
                "Tab Azithromycin 500mg OD x 3d, Syrup Ascoril 10ml TDS, Steam Inhalation", 14);

        completeConsultation(a2.getAppointmentId(), "Tension-type Headache with cervical stiffness",
                "Tab Naproxen 500mg SOS, Tab Propranolol 20mg BD, Sleep hygiene counseling", 20);

        completeConsultation(a4.getAppointmentId(), "Acute Allergic Contact Dermatitis",
                "Hydrocortisone cream 1% topical BD, Tab Bilastine 20mg OD x 7 days", 16);

        completeConsultation(a6.getAppointmentId(), "Bilateral Knee Osteoarthritis & Joint Pain",
                "Tab Paracetamol 650mg TDS, Tab Glucosamine Sulfate 500mg BD, Diclofenac Gel topical BD", 18);

        completeConsultation(a7.getAppointmentId(), "Gastroesophageal Reflux (GERD) & Hyperacidity",
                "Cap Pantoprazole 40mg OD before breakfast x 14d, Syrup Digene 10ml after meals", 12);

        // Cancel a couple to reflect real clinic scenarios
        cancelAppointment(a3.getAppointmentId());
        cancelAppointment(a11.getAppointmentId());
    }

    // ========================================================
    // PATIENT MANAGEMENT
    // ========================================================

    /**
     * Register a new patient and persist to Database.
     */
    public Patient registerPatient(String name, int age, String gender, String phone) {
        String id = "P" + nextPatientNumber++;
        Patient patient = new Patient(id, name, age, gender, phone);
        patients.add(patient);
        dbHelper.insertPatient(patient);
        return patient;
    }

    /**
     * Find patient by ID using linear search in ArrayList.
     */
    public Patient findPatientById(String id) {
        for (Patient p : patients) {
            if (p.getId().equalsIgnoreCase(id.trim())) {
                return p;
            }
        }
        return null;
    }

    public List<Patient> getAllPatients() {
        return patients;
    }

    /**
     * Update an existing patient's details and persist to Database.
     */
    public boolean updatePatient(String id, String name, int age, String gender, String phone) {
        Patient p = findPatientById(id);
        if (p == null) {
            return false;
        }
        p.setName(name);
        p.setAge(age);
        p.setGender(gender);
        p.setPhone(phone);
        dbHelper.insertPatient(p);
        return true;
    }

    /**
     * Search patients by name or ID (case-insensitive) using Streams.
     */
    public List<Patient> searchPatients(String query) {
        if (query == null || query.trim().isEmpty()) {
            return patients;
        }
        String q = query.trim().toLowerCase();
        return patients.stream()
                .filter(p -> p.getId().toLowerCase().contains(q) || p.getName().toLowerCase().contains(q) || p.getPhone().contains(q))
                .collect(Collectors.toList());
    }

    // ========================================================
    // APPOINTMENT SCHEDULING & MANAGEMENT
    // ========================================================

    /**
     * Schedule a new appointment, generate Token, add to FIFO Queue, and save to DB.
     */
    public Appointment scheduleAppointment(String patientId, String date, String time, String reason) {
        Patient patient = findPatientById(patientId);
        if (patient == null) {
            return null;
        }

        String aptId = "APT" + nextAppointmentNumber++;
        int token = nextTokenNumber++;

        Appointment appointment = new Appointment(aptId, patient.getId(), patient.getName(),
                token, date, time, reason);

        // Add to main ArrayList
        appointments.add(appointment);

        // Add to FIFO Queue for consultation
        tokenQueue.offer(appointment);

        // Save to Database
        dbHelper.insertAppointment(appointment);

        return appointment;
    }

    /**
     * Reschedule appointment to a new date and time.
     */
    public boolean rescheduleAppointment(String appointmentId, String newDate, String newTime) {
        Appointment apt = findAppointmentById(appointmentId);
        if (apt == null) {
            return false;
        }

        if (Appointment.STATUS_CANCELLED.equalsIgnoreCase(apt.getStatus()) ||
            Appointment.STATUS_COMPLETED.equalsIgnoreCase(apt.getStatus())) {
            return false; // Cannot reschedule completed or cancelled appointment
        }

        apt.setAppointmentDate(newDate);
        apt.setAppointmentTime(newTime);

        // Update in database
        dbHelper.updateAppointmentStatusAndTime(apt.getAppointmentId(), newDate, newTime, apt.getStatus(), apt.getWaitingTimeMinutes());
        return true;
    }

    /**
     * Cancel an appointment.
     */
    public boolean cancelAppointment(String appointmentId) {
        Appointment apt = findAppointmentById(appointmentId);
        if (apt == null) {
            return false;
        }

        if (Appointment.STATUS_COMPLETED.equalsIgnoreCase(apt.getStatus())) {
            return false; // Cannot cancel completed consultation
        }

        apt.setStatus(Appointment.STATUS_CANCELLED);

        // Update in database
        dbHelper.updateAppointmentStatusAndTime(apt.getAppointmentId(), apt.getAppointmentDate(),
                apt.getAppointmentTime(), Appointment.STATUS_CANCELLED, apt.getWaitingTimeMinutes());

        // Note: FIFO queue automatically skips cancelled appointments when calling next patient
        return true;
    }

    /**
     * Find appointment by ID.
     */
    public Appointment findAppointmentById(String appointmentId) {
        for (Appointment a : appointments) {
            if (a.getAppointmentId().equalsIgnoreCase(appointmentId.trim())) {
                return a;
            }
        }
        return null;
    }

    public List<Appointment> getAllAppointments() {
        return appointments;
    }

    // ========================================================
    // QUEUE & TOKEN PROCESSING (FIFO)
    // ========================================================

    /**
     * Peek at the next patient in the queue without removing.
     */
    public Appointment peekNextInQueue() {
        while (!tokenQueue.isEmpty()) {
            Appointment front = tokenQueue.peek();
            // If the appointment in front was cancelled, remove it from queue
            if (Appointment.STATUS_CANCELLED.equalsIgnoreCase(front.getStatus()) ||
                Appointment.STATUS_COMPLETED.equalsIgnoreCase(front.getStatus())) {
                tokenQueue.poll();
            } else {
                return front;
            }
        }
        return null;
    }

    /**
     * Call the next patient in the FIFO Queue for consultation.
     */
    public Appointment callNextPatient() {
        while (!tokenQueue.isEmpty()) {
            Appointment next = tokenQueue.poll();
            // Skip cancelled or already completed
            if (!Appointment.STATUS_CANCELLED.equalsIgnoreCase(next.getStatus()) &&
                !Appointment.STATUS_COMPLETED.equalsIgnoreCase(next.getStatus())) {
                return next;
            }
        }
        return null;
    }

    public int getQueueLength() {
        // Count active scheduled items remaining in queue
        return (int) appointments.stream()
                .filter(a -> Appointment.STATUS_SCHEDULED.equalsIgnoreCase(a.getStatus()))
                .count();
    }

    /**
     * Returns all scheduled appointments currently waiting in queue order.
     */
    public List<Appointment> getActiveQueueList() {
        return appointments.stream()
                .filter(a -> Appointment.STATUS_SCHEDULED.equalsIgnoreCase(a.getStatus()))
                .sorted(BY_TOKEN)
                .collect(Collectors.toList());
    }

    // ========================================================
    // CONSULTATION COMPLETION & VISIT HISTORY
    // ========================================================

    /**
     * Complete consultation for an appointment, record diagnosis, prescription and wait time.
     */
    public VisitRecord completeConsultation(String appointmentId, String diagnosis,
                                            String prescription, int waitingTimeMinutes) {
        Appointment apt = findAppointmentById(appointmentId);
        if (apt == null) {
            return null;
        }

        // Update appointment status, wait time, and medical prescription (dawai)
        apt.setStatus(Appointment.STATUS_COMPLETED);
        apt.setWaitingTimeMinutes(waitingTimeMinutes);
        apt.setDiagnosis(diagnosis);
        apt.setPrescription(prescription);

        // Generate visit record
        String recId = "REC" + nextRecordNumber++;
        String visitDate = apt.getAppointmentDate();
        if (visitDate == null || visitDate.isEmpty()) {
            visitDate = LocalDate.now().toString();
        }

        VisitRecord visitRecord = new VisitRecord(recId, apt.getAppointmentId(), apt.getPatientId(),
                visitDate, diagnosis, prescription, waitingTimeMinutes);

        // Add to patient's history
        Patient patient = findPatientById(apt.getPatientId());
        if (patient != null) {
            patient.addVisitRecord(visitRecord);
        }

        // Save to Database
        dbHelper.updateAppointmentStatusAndTime(apt.getAppointmentId(), apt.getAppointmentDate(),
                apt.getAppointmentTime(), Appointment.STATUS_COMPLETED, waitingTimeMinutes);
        dbHelper.insertVisitRecord(visitRecord);

        return visitRecord;
    }

    /**
     * Get visit history for a specific patient.
     */
    public List<VisitRecord> getPatientHistory(String patientId) {
        Patient patient = findPatientById(patientId);
        if (patient != null) {
            return patient.getVisitHistory();
        }
        return Collections.emptyList();
    }

    // ========================================================
    // COMPARATOR SORTING
    // ========================================================

    /**
     * Returns appointments sorted using a custom Comparator.
     */
    public List<Appointment> getAppointmentsSorted(Comparator<Appointment> comparator) {
        List<Appointment> sortedList = new ArrayList<>(this.appointments);
        sortedList.sort(comparator);
        return sortedList;
    }

    // Comparator implementations for convenience
    public static final Comparator<Appointment> BY_TOKEN = Comparator.comparingInt(Appointment::getTokenNumber);
    public static final Comparator<Appointment> BY_TIME = Comparator.comparing(Appointment::getAppointmentDate)
            .thenComparing(Appointment::getAppointmentTime);
    public static final Comparator<Appointment> BY_PATIENT_NAME = Comparator.comparing(Appointment::getPatientName, String.CASE_INSENSITIVE_ORDER);

    // ========================================================
    // JAVA STREAMS - REPORTS & ANALYTICS
    // ========================================================

    public long getTotalPatientCount() {
        return patients.size();
    }

    public long getTotalAppointmentCount() {
        return appointments.size();
    }

    public long getCountByStatus(String status) {
        return appointments.stream()
                .filter(a -> status.equalsIgnoreCase(a.getStatus()))
                .count();
    }

    /**
     * Calculate average waiting time for completed visits using Streams.
     */
    public double getAverageWaitingTime() {
        return appointments.stream()
                .filter(a -> Appointment.STATUS_COMPLETED.equalsIgnoreCase(a.getStatus()))
                .mapToInt(Appointment::getWaitingTimeMinutes)
                .average()
                .orElse(0.0);
    }

    /**
     * Filter appointments by date using Java Streams.
     */
    public List<Appointment> getAppointmentsByDate(String date) {
        return appointments.stream()
                .filter(a -> date.equals(a.getAppointmentDate()))
                .sorted(BY_TOKEN)
                .collect(Collectors.toList());
    }

    /**
     * Filter appointments by status using Java Streams.
     */
    public List<Appointment> getAppointmentsByStatus(String status) {
        return appointments.stream()
                .filter(a -> status.equalsIgnoreCase(a.getStatus()))
                .sorted(BY_TOKEN)
                .collect(Collectors.toList());
    }
}
