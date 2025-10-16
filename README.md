# Resident HealthCare System (RMIT Care Home)

## Overview
This project implements a **Resident HealthCare System** for the *RMIT Care Home*, developed in Java as part of **COSC1295 – Advanced Programming (Semester 2, 2025)**.  
It demonstrates Object-Oriented Design principles, JavaFX GUI, collections, exception handling, serialization, JDBC (SQLite), and unit testing using JUnit.

The system supports management of residents, staff, wards, and prescriptions through a clean, modern GUI interface and console fallback.

Github Link: https://github.com/COSC1295-advanced-programming-2025-s2/cosc1295-assignment-2-semester-2-2025-Sodium-Man

---

## 🏗 Features

- **Role-based access control**  
  - Manager: Add/modify staff, add residents, assign beds and shifts.  
  - Doctor: Create prescriptions.  
  - Nurse: Administer medicines, move or discharge residents.

- **Ward Visualization (JavaFX)**  
  - 2 wards, each with 6 rooms (1–4 beds).  
  - Occupied/vacant beds color-coded (red/blue).

- **Data Management**
  - Persistent object data via serialization (`carehome.ser`).
  - Archived resident and prescription data in SQLite (`carehome.db`).

- **Audit Logging**
  - All staff actions (add, update, discharge, administer) logged with timestamps.

- **Compliance**
  - Shift rules enforced for nurses and doctors.
  - Exceptions raised for unauthorized or off-duty actions.

---

## 📸 GUI Screenshots

Login Page 

<img width="385" height="245" alt="image" src="https://github.com/user-attachments/assets/1b086621-bb9a-4496-ab19-e14a027dc92d" />

Ward Overview

<img width="327" height="607" alt="image" src="https://github.com/user-attachments/assets/7d381dbb-00b6-445d-ae77-e43e970ee483" />

Staff Overview

<img width="372" height="391" alt="image" src="https://github.com/user-attachments/assets/1c437671-e85a-40a1-9f69-b9577ec46b20" />
 

---

## 🧮 System Requirements

- **Java:** JDK 17 or higher  
- **JavaFX:** JavaFX 17+  
- **Database:** SQLite (included via `org.sqlite.JDBC`)  
- **Build Tool:** Gradle or direct javac/java  
- **JUnit 5:** For testing  

---

## ▶️ Running the Application

### **Option 1: GUI Mode**
```bash
# Compile and run
javac -d bin -cp ".;lib/sqlite-jdbc.jar;lib/javafx/*" app/*.java
java -cp "bin;lib/sqlite-jdbc.jar;lib/javafx/*" app.App
