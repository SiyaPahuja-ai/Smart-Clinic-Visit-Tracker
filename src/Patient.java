import java.util.ArrayList;
import java.util.List;

/**
 * Patient.java
 * Represents a patient registered in the clinic.
 * Demonstrates OOP concepts: Encapsulation, Constructors, Getters/Setters.
 */
public class Patient {
    // Private variables (Encapsulation)
    private String id;
    private String name;
    private int age;
    private String gender;
    private String phone;
    private List<VisitRecord> visitHistory; // Stores past visit history

    // Parameterized constructor
    public Patient(String id, String name, int age, String gender, String phone) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.phone = phone;
        this.visitHistory = new ArrayList<>();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public List<VisitRecord> getVisitHistory() {
        return visitHistory;
    }

    public void setVisitHistory(List<VisitRecord> visitHistory) {
        this.visitHistory = visitHistory;
    }

    // Add a completed visit to history
    public void addVisitRecord(VisitRecord record) {
        this.visitHistory.add(record);
    }

    // Formatted details for display
    @Override
    public String toString() {
        return String.format("ID: %-6s | Name: %-18s | Age: %-3d | Gender: %-6s | Phone: %s",
                id, name, age, gender, phone);
    }
}
