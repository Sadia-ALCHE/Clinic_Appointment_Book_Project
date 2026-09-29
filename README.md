# MediCare Clinic Appointment Book

A desktop application designed for clinic front-desk attendants and physicians to manage daily appointments, patient records, doctor timetables, and billing. Built with **JavaFX** and a local **SQLite** database.

---

## What the App Does

The application is split into three main areas accessible from the sidebar:

1. **Patients Directory**:
   - Register new patients with their contact details, date of birth, and blood group.
   - Search patients instantly by full name, phone number, or email.
   - Delete patients (with safety checks that prevent deleting a patient who has active appointments or unpaid invoices).

2. **Doctor Timetable & Booking**:
   - Filter schedules by physician and date.
   - View hourly slots from 08:00 to 17:00 with real-time status badges (Available, Occupied, or Completed).
   - Book new consultations or link follow-up visits directly to earlier appointments.
   - Built-in conflict prevention: stops double-booking the same physician at overlapping times.
   - Click **Complete Visit** once a patient has seen the doctor, or **Cancel Slot** if an appointment is called off.

3. **Invoices & Billing**:
   - An invoice is created automatically whenever an appointment is marked as **Completed**.
   - Review pending and settled invoices, complete with itemized charges (consultation fees, lab panels, dressings).
   - If a patient comes for multiple connected visits (e.g. an initial triage followed by a suture check), the app automatically groups them under a **Treatment Plan** banner showing total visits and combined cost.
   - Record patient payments via Mauritian payment methods (MCB Juice, Card, Cash) with receipt references.

All prices, consultation rates, and bills are in **Mauritian Rupees (MUR / Rs)**.

---

## What You Need Before You Start

Before building or running the project, make sure you have:

- **Java JDK 21 or newer** (tested and verified on Java 25 / 26).
- **IntelliJ IDEA** (Community or Ultimate edition) or any standard Java IDE with Maven support.
- **Git** (if you cloned or are committing changes).

You do **not** need to install MySQL, PostgreSQL, or any database server. The app uses an embedded SQLite database that runs entirely on its own.

---

## How to Open and Build the Project

### 1. Open in IntelliJ IDEA
1. Launch IntelliJ IDEA.
2. Click **File → Open...** and select the `clinic-appointment-book` folder.
3. IntelliJ will automatically detect the `pom.xml` file and start importing dependencies.
4. Give it a few seconds to download JavaFX and SQLite libraries. If it doesn't sync automatically, click the small floating Maven icon or open the **Maven** tab on the right sidebar and click **Reload All Maven Projects** (<kbd>Ctrl+Shift+O</kbd>).

### 2. Compile the Code
- Press <kbd>Ctrl+F9</kbd> (or click **Build → Build Project**).
- Alternatively, open the built-in terminal (<kbd>Alt+F12</kbd>) and run:
  ```powershell
  mvn clean compile
  ```
- You should see `BUILD SUCCESS`.

---

## How to Run the Application

### Important: Always Run `Main.java`, Never `App.java`
Because JavaFX runs as standard classpath libraries, launching JavaFX directly from `App.java` can cause Java to complain about missing runtime components. 

To avoid this, use the launcher class **`Main.java`**:

1. In the Project tool window (<kbd>Alt+1</kbd>), navigate to:
   ```
   src/main/java/com/clinic/Main.java
   ```
2. Right-click `Main.java` and select **Run 'Main.main()'** (or press <kbd>Ctrl+Shift+F10</kbd>).
3. The desktop window will open titled **MediCare Clinic Appointment Book · ALCHE Mauritius**.

---

## Database Configuration

### How SQLite Works Here
- The database is stored in a single file named **`clinic.db`** located right in the project root folder.
- **Zero installation**: There is no database service or daemon to start. Java connects to `clinic.db` via JDBC (`jdbc:sqlite:clinic.db`).
- **Automatic Setup**: When you launch the app, `DatabaseConnection.java` automatically runs `src/main/resources/schema.sql`. It creates all necessary tables (`patients`, `doctors`, `appointments`, `invoices`, `invoice_items`) and indexes if they do not already exist, and inserts default doctors.

### "No Data Sources Configured" in IntelliJ?
If you open `schema.sql` in IntelliJ IDEA Ultimate and see a yellow banner across the top saying:
> *"No data sources configured to run this SQL and provide advanced code assistance."*

**This is completely safe to ignore.** IntelliJ is simply offering to connect its own built-in database viewer tool to your database file. The actual application does not rely on IntelliJ's database tool—it manages all table creation and queries programmatically through Java code.

### How to Reset the Database
If you ever want to start completely fresh with clean sample data:
1. Close the running application.
2. Delete the `clinic.db` file from the project root.
3. Re-run `Main.java`.
4. A brand new `clinic.db` will be created automatically with the default physicians and clean tables.

---

## How to Run the Tests

The project includes unit and integration tests covering patient validation, conflict detection, care-chain recursion, database transactions, and financial auditing.

- **From IntelliJ IDEA**: Right-click the `src/test/java` directory and choose **Run 'All Tests'**.
- **From the Terminal**:
  ```powershell
  mvn test
  ```
- All tests will run against isolated in-memory or dedicated test databases so your main `clinic.db` is never affected.

---

## Project Structure Overview

Here is a quick map of how the code is organized:

```
clinic-appointment-book/
├── clinic.db                             # Local SQLite database file
├── pom.xml                               # Maven project dependencies and build plugins
├── src/
│   ├── main/
│   │   ├── java/com/clinic/
│   │   │   ├── Main.java                 # Safe trampoline launcher (run this!)
│   │   │   ├── App.java                  # Main JavaFX composition root and view wiring
│   │   │   ├── dao/                      # Data Access Objects (interfaces & SQLite queries)
│   │   │   ├── model/                    # Data entities (Patient, Doctor, Appointment, Invoice)
│   │   │   ├── service/                  # Business logic (ScheduleValidator, BillingService)
│   │   │   └── ui/                       # JavaFX screens (PatientView, ScheduleView, BillingView)
│   │   └── resources/
│   │       ├── schema.sql                # Table definitions, constraints, and initial seed doctors
│   │       └── style.css                 # Clean modern UI styling for the desktop app
│   └── test/                             # 57 automated unit & integration test suites
└── README.md                             # This file
```

---

## How To Use

- **Booking an Appointment**: Pick a doctor and date first. Click **Book Slot** on an available hour, select the patient, enter a reason, and confirm.
- **Booking a Follow-Up Visit**: When booking, switch the *Consultation Type* to **Follow-Up Visit**. A dropdown will appear listing that patient's earlier visits so you can link them together.
- **Completing a Visit**: Once the patient has seen the doctor, find the slot on the schedule and click **Complete Visit**. This marks the visit done and instantly sends the bill to the **Invoices & Billing** tab.
- **Collecting Payments**: Go to **Invoices & Billing**, click **Record Payment** on any pending invoice, choose whether the patient paid via MCB Juice, Card, or Cash, and enter the reference number.