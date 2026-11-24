import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DatabaseHelper.java
 * Handles SQLite database operations using JDBC.
 * Implements persistent storage for Patients, Appointments, and Visit Records.
 * Uses PreparedStatements to prevent SQL injection and ensure clean database execution.
 */
public class DatabaseHelper {
    // SQLite connection string - creates clinic.db in current working directory
    private static final String DB_URL = "jdbc:sqlite:clinic.db";

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("[Database Error] SQLite JDBC Driver not found on classpath: " + e.getMessage());
        }
    }

    /**
     * Get a new connection to the SQLite database
     */
    public Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    /**
     * Initialize database tables if they do not already exist.
     */
    public void initDatabase() {
        String createPatientsTable = "CREATE TABLE IF NOT EXISTS patients (" +
                "id TEXT PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "age INTEGER, " +
                "gender TEXT, " +
                "phone TEXT" +
                ");";

        String createAppointmentsTable = "CREATE TABLE IF NOT EXISTS appointments (" +
                "appointment_id TEXT PRIMARY KEY, " +
                "patient_id TEXT NOT NULL, " +
                "patient_name TEXT NOT NULL, " +
                "token_number INTEGER NOT NULL, " +
                "appointment_date TEXT NOT NULL, " +
                "appointment_time TEXT NOT NULL, " +
                "reason TEXT, " +
                "status TEXT NOT NULL, " +
                "waiting_time_minutes INTEGER DEFAULT 0, " +
                "FOREIGN KEY (patient_id) REFERENCES patients(id)" +
                ");";

        String createVisitsTable = "CREATE TABLE IF NOT EXISTS visits (" +
                "record_id TEXT PRIMARY KEY, " +
                "appointment_id TEXT, " +
                "patient_id TEXT NOT NULL, " +
                "visit_date TEXT NOT NULL, " +
                "diagnosis TEXT, " +
                "prescription TEXT, " +
                "waiting_time_minutes INTEGER, " +
                "FOREIGN KEY (patient_id) REFERENCES patients(id)" +
                ");";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createPatientsTable);
            stmt.execute(createAppointmentsTable);
            stmt.execute(createVisitsTable);
            // System.out.println("[Database] Tables verified and ready.");
        } catch (SQLException e) {
            System.err.println("[Database Error] Initialization failed: " + e.getMessage());
        }
    }

    // ==========================================
    // PATIENT CRUD OPERATIONS
    // ==========================================

    public void insertPatient(Patient p) {
        String sql = "INSERT OR REPLACE INTO patients(id, name, age, gender, phone) VALUES(?, ?, ?, ?, ?)";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, p.getId());
            pstmt.setString(2, p.getName());
            pstmt.setInt(3, p.getAge());
            pstmt.setString(4, p.getGender());
            pstmt.setString(5, p.getPhone());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[Database Error] Insert Patient failed: " + e.getMessage());
        }
    }

    public List<Patient> getAllPatients() {
        List<Patient> list = new ArrayList<>();
        String sql = "SELECT id, name, age, gender, phone FROM patients ORDER BY id";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Patient p = new Patient(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getInt("age"),
                        rs.getString("gender"),
                        rs.getString("phone")
                );
                list.add(p);
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Get Patients failed: " + e.getMessage());
        }
        return list;
    }

    // ==========================================
    // APPOINTMENT CRUD OPERATIONS
    // ==========================================

    public void insertAppointment(Appointment a) {
        String sql = "INSERT OR REPLACE INTO appointments(appointment_id, patient_id, patient_name, " +
                "token_number, appointment_date, appointment_time, reason, status, waiting_time_minutes) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, a.getAppointmentId());
            pstmt.setString(2, a.getPatientId());
            pstmt.setString(3, a.getPatientName());
            pstmt.setInt(4, a.getTokenNumber());
            pstmt.setString(5, a.getAppointmentDate());
            pstmt.setString(6, a.getAppointmentTime());
            pstmt.setString(7, a.getReason());
            pstmt.setString(8, a.getStatus());
            pstmt.setInt(9, a.getWaitingTimeMinutes());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[Database Error] Insert Appointment failed: " + e.getMessage());
        }
    }

    public void updateAppointmentStatusAndTime(String appointmentId, String newDate, String newTime, String newStatus, int waitTime) {
        String sql = "UPDATE appointments SET appointment_date = ?, appointment_time = ?, status = ?, waiting_time_minutes = ? WHERE appointment_id = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newDate);
            pstmt.setString(2, newTime);
            pstmt.setString(3, newStatus);
            pstmt.setInt(4, waitTime);
            pstmt.setString(5, appointmentId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[Database Error] Update Appointment failed: " + e.getMessage());
        }
    }

    public List<Appointment> getAllAppointments() {
        List<Appointment> list = new ArrayList<>();
        String sql = "SELECT appointment_id, patient_id, patient_name, token_number, appointment_date, " +
                "appointment_time, reason, status, waiting_time_minutes FROM appointments ORDER BY token_number";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Appointment a = new Appointment(
                        rs.getString("appointment_id"),
                        rs.getString("patient_id"),
                        rs.getString("patient_name"),
                        rs.getInt("token_number"),
                        rs.getString("appointment_date"),
                        rs.getString("appointment_time"),
                        rs.getString("reason"),
                        rs.getString("status"),
                        rs.getInt("waiting_time_minutes")
                );
                list.add(a);
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Get Appointments failed: " + e.getMessage());
        }
        return list;
    }

    // ==========================================
    // VISIT RECORD CRUD OPERATIONS
    // ==========================================

    public void insertVisitRecord(VisitRecord vr) {
        String sql = "INSERT OR REPLACE INTO visits(record_id, appointment_id, patient_id, visit_date, " +
                "diagnosis, prescription, waiting_time_minutes) VALUES(?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, vr.getRecordId());
            pstmt.setString(2, vr.getAppointmentId());
            pstmt.setString(3, vr.getPatientId());
            pstmt.setString(4, vr.getVisitDate());
            pstmt.setString(5, vr.getDiagnosis());
            pstmt.setString(6, vr.getPrescription());
            pstmt.setInt(7, vr.getWaitingTimeMinutes());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[Database Error] Insert Visit Record failed: " + e.getMessage());
        }
    }

    public List<VisitRecord> getVisitsForPatient(String patientId) {
        List<VisitRecord> list = new ArrayList<>();
        String sql = "SELECT record_id, appointment_id, patient_id, visit_date, diagnosis, prescription, " +
                "waiting_time_minutes FROM visits WHERE patient_id = ? ORDER BY record_id";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, patientId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    VisitRecord vr = new VisitRecord(
                            rs.getString("record_id"),
                            rs.getString("appointment_id"),
                            rs.getString("patient_id"),
                            rs.getString("visit_date"),
                            rs.getString("diagnosis"),
                            rs.getString("prescription"),
                            rs.getInt("waiting_time_minutes")
                    );
                    list.add(vr);
                }
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Get Visits failed: " + e.getMessage());
        }
        return list;
    }

    public List<VisitRecord> getAllVisits() {
        List<VisitRecord> list = new ArrayList<>();
        String sql = "SELECT record_id, appointment_id, patient_id, visit_date, diagnosis, prescription, " +
                "waiting_time_minutes FROM visits ORDER BY record_id";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                VisitRecord vr = new VisitRecord(
                        rs.getString("record_id"),
                        rs.getString("appointment_id"),
                        rs.getString("patient_id"),
                        rs.getString("visit_date"),
                        rs.getString("diagnosis"),
                        rs.getString("prescription"),
                        rs.getInt("waiting_time_minutes")
                );
                list.add(vr);
            }
        } catch (SQLException e) {
            System.err.println("[Database Error] Get All Visits failed: " + e.getMessage());
        }
        return list;
    }
}
