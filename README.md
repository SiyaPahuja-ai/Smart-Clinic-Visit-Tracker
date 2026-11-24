# Smart Clinic Visit Tracker (JAVA-P002) - Dr. Siya's Clinic

A comprehensive, student-friendly Java application developed to streamline outpatient clinic workflows, automate patient queues using First-In-First-Out (FIFO) token allocation, track medical visit histories, and generate analytics using Java Streams and SQLite JDBC persistence with a modern Web Dashboard.

---

## 1. Project Introduction
In traditional outpatient clinics, manual token management often leads to queue disputes, untracked waiting times, and misplaced medical records. **Smart Clinic Visit Tracker** (Dr. Siya's OPD Clinic) solves these problems by providing an automated system that tracks patient visits from registration and scheduling to queue management, consultation completion, and reporting.

---

## 2. Problem Statement
Clinics handle many walk-in and scheduled patients daily. Key challenges include:
- Inefficient token distribution and line skipping.
- Lack of consolidated consultation history for returning patients.
- Inability to calculate real-time clinic efficiency metrics (e.g., patient waiting time, completion percentage).
- Data loss when applications shut down if proper database persistence is not implemented.

---

## 3. Objectives
- Provide a clean, robust, and student-level Java application using standard OOP design.
- Implement token-based queue management using Java's `Queue` interface (`LinkedList`).
- Use `ArrayList` for dynamic in-memory collections of patients and appointments.
- Provide customizable sorting using Java `Comparator`.
- Use Java 8+ `Streams` API for real-time statistical reports and filtering.
- Persist all records across restarts using `JDBC` with a lightweight, serverless `SQLite` database.
- Present a modern, clinical Web Dashboard interface with an Emerald/Dark Medical Green theme.

---

## 4. Functional Requirements
1. **Patient Registration**: Capture Patient ID (auto-generated, e.g. `P101`), Name, Age, Gender, and Contact Number.
2. **Appointment Lifecycle**:
   - **Schedule**: Assign appointment ID (e.g. `APT101`), assign sequential token number (e.g. `#101`), set date/time/symptoms, and push into the waiting queue.
   - **Reschedule**: Change date and time for scheduled appointments.
   - **Cancel**: Flag appointment as `CANCELLED` and safely skip it in the consultation queue.
3. **Queue-Based Token Processing**: Process patients strictly in FIFO order. Skips cancelled and completed visits automatically.
4. **Consultation & Visit History**: Complete consultations by recording waiting time, diagnosis, and prescriptions. Stores these records under the patient's permanent history.
5. **Daily Reports & Analytics**:
   - Total patient and appointment counts.
   - Breakdown of Scheduled, Completed, and Cancelled appointments with percentages.
   - Average waiting time computation using Java Streams.

---

## 5. Technologies Used
- **Backend**: Core Java (JDK 17) with built-in lightweight `com.sun.net.httpserver.HttpServer`
- **Frontend**: HTML5, CSS3, JavaScript (ES6+), Bootstrap 5, Chart.js
- **Design Theme**: Executive Medical Dark Forest & Emerald Green (`#0A3622`, `#10B981`, `#E8F7F0`)
- **Doctor / Consultant**: Dr. Siya (M.B.B.S., M.D. - Chief Medical Officer)
- **Database**: SQLite 3 (`clinic.db`) with `sqlite-jdbc-3.36.0.3.jar` (Zero setup / zero external server needed)
- **Architecture**: Multi-tier MVC (Presentation, Controller, Persistence, Domain Models)
- **No Heavy Frameworks**: No Spring Boot, No Hibernate — 100% Core Java and easily explainable for college evaluation!

---

## 6. Web Dashboard - 8 Core Pages & Features

1. **Dashboard (`/`)**:
   - **Doctor Overview Banner**: Dr. Siya (M.B.B.S., M.D.) active OPD session banner with quick-action buttons.
   - **4 KPI Statistic Cards**: Total Patients, Today's Appointments, Completed Consultations, Waiting Patients in Queue.
   - **Real-Time Visualizations**: Chart.js Bar Chart (Appointment Overview) and Doughnut Chart (Status Distribution).
   - **Recent Appointments Table**: Real-time list of latest tokens with quick action triggers and `💊 Dawa` view button.

2. **Patient Registration**:
   - Clean, dedicated registration form with validated input fields (Name, Age, Gender, Phone, Emergency City, Initial Symptoms).
   - Step-by-step registration guidelines detailing auto-generated IDs (e.g. `P101`) and instant SQLite database persistence.

3. **Patient Records**:
   - Comprehensive searchable directory of all registered clinic patients.
   - Live search filter by Name, Patient ID, or Phone.
   - Quick action buttons: Book Appointment, Edit Demographics, and `💊 Dawa / History`.

4. **Appointment Management**:
   - Schedule consultation visits with automatic token allocation and FIFO queue placement.
   - Filter by status (Scheduled / Completed / Cancelled) and Date.
   - Sort using Java `Comparator` (by Token, Time, or Patient Name).
   - For completed appointments, displays the patient's medical problem/diagnosis and a dedicated `💊 View Dawa` prescription button.

5. **Token Queue (FIFO)**:
   - **Current Calling Token Hero Card**: Displays active calling token number in large typography with patient details.
   - **"CALL NEXT PATIENT (FIFO)" Button**: Executes Java `Queue<Appointment>.poll()` in true FIFO sequence, skipping cancelled visits.
   - Waiting Queue Table showing real-time queue positions.

6. **Patient Visit History**:
   - Patient dropdown selector loading historical consultation records.
   - Clean medical timeline detailing past consultation visits, diagnosis, and prescribed dawai / medications (Rx).

7. **Reports & Analytics**:
   - Operational clinic metrics computed in real-time using Java 8+ Streams API.
   - Average waiting time calculation across all completed visits.
   - Status summary breakdown with printable report format.

8. **Settings**:
   - Clinic metadata, working hours, doctor credentials (Dr. Siya), and system architecture information.

---

## 7. Java Concepts & Why They Were Selected

| Java Concept | Usage in Project | Why Selected? |
| :--- | :--- | :--- |
| **OOP (Encapsulation)** | `Patient`, `Appointment`, `VisitRecord` classes with private fields and getters/setters | Keeps data secure, validates inputs, and separates concerns into clear domain models. |
| **ArrayList** | In-memory lists of patients, appointments, and visit histories | Provides fast index-based lookups and dynamically expands as patients register. |
| **Queue (FIFO)** | `Queue<Appointment> tokenQueue = new LinkedList<>()` | Models a real-life clinic line: First patient to receive a token is the first called into the doctor's room. |
| **Comparator** | Sorting appointments by Token Number, Date/Time, or Patient Name | Decouples sorting logic from domain classes, allowing flexible multi-criteria sorting. |
| **Java Streams** | Calculating statistics, filtering by status, and computing average waiting time | Replaces complex nested `for` loops with readable, declarative, functional-style aggregation. |
| **JDBC (PreparedStatement)** | Database connectivity to SQLite via `DatabaseHelper.java` | Standard Java API for SQL database interaction; `PreparedStatement` prevents SQL injection. |

---

## 7. Data Structures Explanation
- **`ArrayList<Patient>`**: Chosen because we need quick random access when searching by ID and sequential iteration when displaying all registered patients.
- **`Queue<Appointment>` (LinkedList implementation)**: Chosen for token management. A queue enforces the **FIFO (First-In, First-Out)** principle:
  - `offer()` adds a new token to the back of the line.
  - `poll()` retrieves and removes the patient at the front of the line.
  - `peek()` inspects the current front without modifying the queue.

---

## 8. Class Responsibilities

```
src/
├── Patient.java         -> Domain model: ID, name, age, phone, and list of VisitRecords.
├── Appointment.java     -> Domain model: AptID, patient info, token #, date/time, status, wait time.
├── VisitRecord.java     -> Domain model: Completed consultation details (diagnosis, prescription, date).
├── DatabaseHelper.java  -> JDBC layer: Creates tables and performs CRUD operations on SQLite.
├── ClinicManager.java   -> Controller layer: Orchestrates ArrayLists, Queue, Comparator, and Streams.
└── Main.java            -> Presentation layer: Console menu (Scanner + switch-case) and display logic.
```

---

## 9. Database Structure (SQLite)

### Table: `patients`
| Column | Type | Constraints |
| :--- | :--- | :--- |
| `id` | TEXT | PRIMARY KEY |
| `name` | TEXT | NOT NULL |
| `age` | INTEGER | |
| `gender` | TEXT | |
| `phone` | TEXT | |

### Table: `appointments`
| Column | Type | Constraints |
| :--- | :--- | :--- |
| `appointment_id` | TEXT | PRIMARY KEY |
| `patient_id` | TEXT | FOREIGN KEY -> patients(id) |
| `patient_name` | TEXT | NOT NULL |
| `token_number` | INTEGER | NOT NULL |
| `appointment_date` | TEXT | NOT NULL |
| `appointment_time` | TEXT | NOT NULL |
| `reason` | TEXT | |
| `status` | TEXT | NOT NULL (SCHEDULED/COMPLETED/CANCELLED) |
| `waiting_time_minutes` | INTEGER | DEFAULT 0 |

### Table: `visits`
| Column | Type | Constraints |
| :--- | :--- | :--- |
| `record_id` | TEXT | PRIMARY KEY |
| `appointment_id` | TEXT | |
| `patient_id` | TEXT | FOREIGN KEY -> patients(id) |
| `visit_date` | TEXT | NOT NULL |
| `diagnosis` | TEXT | |
| `prescription` | TEXT | |
| `waiting_time_minutes` | INTEGER | |

---

## 10. Application Workflow Diagram

```
+-------------------------------------------------------------------+
|                        1. Register Patient                        |
|                     (Saved to DB & ArrayList)                     |
+---------------------------------+---------------------------------+
                                  |
                                  v
+---------------------------------+---------------------------------+
|                    2. Schedule Appointment                        |
|            - Auto-generates Token Number (e.g. #101)              |
|            - Appended to FIFO Queue (tokenQueue.offer)            |
+---------------------------------+---------------------------------+
                                  |
                                  v
+---------------------------------+---------------------------------+
|                    3. Call Next Patient (FIFO)                    |
|           - Next in queue called into cabin (tokenQueue.poll)     |
|           - Skips Cancelled / Completed appointments              |
+---------------------------------+---------------------------------+
                                  |
                                  v
+---------------------------------+---------------------------------+
|                     4. Complete Consultation                      |
|         - Doctor inputs Diagnosis, Prescription & Wait Time       |
|         - Status -> COMPLETED                                     |
|         - VisitRecord appended to Patient Visit History           |
+---------------------------------+---------------------------------+
                                  |
                                  v
+---------------------------------+---------------------------------+
|                     5. Analytics & Reports                        |
|        - Java Streams compute status breakdown & average wait     |
|        - Comparator sorts records by Token / Time / Name          |
+-------------------------------------------------------------------+
```

---

## 11. How to Compile and Run

### Prerequisites
- JDK 17 (or JDK 11+) installed on your machine.
- SQLite JDBC driver (already provided in the `lib/` directory).

### Option A: Using Windows 1-Click Batch Scripts (Recommended)
1. **Launch Modern Web Dashboard**: Double-click `run-web.bat`. It will compile the backend, start the HTTP server on port 8080, and automatically open `http://localhost:8080/` in your browser!
2. **Launch Console Application**: Double-click `run.bat` to interact with the traditional menu-driven terminal interface.
3. **Recompile Everything**: Double-click `compile.bat`.

### Option B: Manual Command Line Execution
```bash
# 1. Compile Java source files
javac -encoding UTF-8 -cp "lib/*" -d bin src/*.java

# 2. Run the Modern Web Dashboard Server
java -cp "bin;lib/*" WebServer

# 3. Or run the Console Application
java -cp "bin;lib/*" Main
```

---

## 12. Verification and Testing Results
The application has been verified against all edge cases:
- [x] **Automatic ID Generation**: IDs (`P101`, `APT101`, `REC101`) increment without collisions.
- [x] **Queue Integrity**: When an appointment is cancelled, it is automatically skipped by `callNextPatient()`.
- [x] **Data Persistence**: Tested shutting down the application and restarting; all patients, appointments, and visits were automatically reloaded from `clinic.db`.
- [x] **Streams Computation**: Verified accurate calculation of percentage distributions and average waiting times.

---

## 13. Future Scope
- Integration of a graphical user interface (JavaFX or Swing).
- Multi-doctor department routing (e.g., General Physician, Dermatology, Pediatrics).
- SMS or WhatsApp API notifications when a patient's token is about to be called.
#   S m a r t - C l i n i c - V i s i t - T r a c k e r  
 