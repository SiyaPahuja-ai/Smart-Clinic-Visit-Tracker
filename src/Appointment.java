/**
 * Appointment.java
 * Represents a consultation appointment in the clinic.
 * Demonstrates OOP concepts: Encapsulation, State Management (Status), and Constructors.
 */
public class Appointment {
    // Status constants
    public static final String STATUS_SCHEDULED = "SCHEDULED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    // Private variables
    private String appointmentId;
    private String patientId;
    private String patientName;
    private int tokenNumber;          // Auto-generated token for FIFO queue
    private String appointmentDate;   // Format: YYYY-MM-DD
    private String appointmentTime;   // Format: HH:mm
    private String reason;
    private String status;            // SCHEDULED, COMPLETED, CANCELLED
    private int waitingTimeMinutes;   // Time spent waiting before consultation
    private String diagnosis;         // Medical problem / diagnosis for completed consultation
    private String prescription;      // Dawai / Medicines prescribed (Rx)

    // Parameterized constructor
    public Appointment(String appointmentId, String patientId, String patientName,
                       int tokenNumber, String appointmentDate, String appointmentTime,
                       String reason, String status, int waitingTimeMinutes) {
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.patientName = patientName;
        this.tokenNumber = tokenNumber;
        this.appointmentDate = appointmentDate;
        this.appointmentTime = appointmentTime;
        this.reason = reason;
        this.status = status;
        this.waitingTimeMinutes = waitingTimeMinutes;
    }

    // Secondary constructor for new bookings with default SCHEDULED status and 0 wait time
    public Appointment(String appointmentId, String patientId, String patientName,
                       int tokenNumber, String appointmentDate, String appointmentTime, String reason) {
        this(appointmentId, patientId, patientName, tokenNumber, appointmentDate, appointmentTime,
             reason, STATUS_SCHEDULED, 0);
    }

    // Getters and Setters
    public String getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public int getTokenNumber() {
        return tokenNumber;
    }

    public void setTokenNumber(int tokenNumber) {
        this.tokenNumber = tokenNumber;
    }

    public String getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(String appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public String getAppointmentTime() {
        return appointmentTime;
    }

    public void setAppointmentTime(String appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getWaitingTimeMinutes() {
        return waitingTimeMinutes;
    }

    public void setWaitingTimeMinutes(int waitingTimeMinutes) {
        this.waitingTimeMinutes = waitingTimeMinutes;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getPrescription() {
        return prescription;
    }

    public void setPrescription(String prescription) {
        this.prescription = prescription;
    }

    // Formatted details for display
    @Override
    public String toString() {
        return String.format("AptID: %-7s | Token: #%-4d | Date: %-10s | Time: %-5s | Patient: %-16s (ID: %-5s) | Status: %-9s | Reason: %s",
                appointmentId, tokenNumber, appointmentDate, appointmentTime, patientName, patientId, status, reason);
    }
}
