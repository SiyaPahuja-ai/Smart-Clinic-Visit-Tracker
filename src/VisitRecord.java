/**
 * VisitRecord.java
 * Represents a completed clinic visit / consultation record.
 * Demonstrates historical data tracking and encapsulation.
 */
public class VisitRecord {
    // Private variables
    private String recordId;
    private String appointmentId;
    private String patientId;
    private String visitDate;
    private String diagnosis;
    private String prescription;
    private int waitingTimeMinutes;

    // Parameterized constructor
    public VisitRecord(String recordId, String appointmentId, String patientId,
                       String visitDate, String diagnosis, String prescription,
                       int waitingTimeMinutes) {
        this.recordId = recordId;
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.visitDate = visitDate;
        this.diagnosis = diagnosis;
        this.prescription = prescription;
        this.waitingTimeMinutes = waitingTimeMinutes;
    }

    // Getters and Setters
    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

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

    public String getVisitDate() {
        return visitDate;
    }

    public void setVisitDate(String visitDate) {
        this.visitDate = visitDate;
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

    public int getWaitingTimeMinutes() {
        return waitingTimeMinutes;
    }

    public void setWaitingTimeMinutes(int waitingTimeMinutes) {
        this.waitingTimeMinutes = waitingTimeMinutes;
    }

    @Override
    public String toString() {
        return String.format("Record: %-6s | Date: %-10s | Wait: %-3d mins | Diagnosis: %-18s | Rx: %s",
                recordId, visitDate, waitingTimeMinutes, diagnosis, prescription);
    }
}
