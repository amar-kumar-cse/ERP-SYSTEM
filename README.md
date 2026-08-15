# Shiksha ERP — Coaching Institute Management System

A production-ready Coaching Institute Enterprise Resource Planning (ERP) web application built with **Spring Boot 3**, **Spring Security 6**, **Spring Data JPA**, and **Thymeleaf + Bootstrap 5**.

---

## 🚀 Key Modules & Features

### 1. 🛡️ Role-Based Access Control (RBAC)
- **Administrator (`ROLE_ADMIN`)**:
  - Full institute analytics, student and faculty management.
  - Class batch scheduling and assignment.
  - Monthly fee generation, payment logging, and overdue tracking.
  - Centralized help desk & query resolution ticket management.
- **Faculty / Teacher (`ROLE_TEACHER`)**:
  - Class batch rosters and student listings.
  - Daily bulk attendance marking (Present/Absent toggles) & historical attendance analytics.
  - Test/Exam marks entry with live percentage calculator and report card generation.
  - Study material upload (PDFs, docs, assignments) and external lecture link sharing.
- **Student / Parent (`ROLE_PARENT`)**:
  - 30-day attendance metrics & monthly calendar view.
  - Comprehensive academic report cards & score trends.
  - Digital fee invoices, payment receipts, and balance status.
  - Downloadable study notes and direct access to institute support desk.

---

## 🛠️ Technology Stack

- **Backend**: Java 17+, Spring Boot 3.3.x
- **Security**: Spring Security 6 (BCrypt Password Hashing, Session Management)
- **Database**: H2 Database (File-persistent mode enabled by default) / MySQL 8.x ready
- **ORM**: Spring Data JPA / Hibernate
- **Frontend**: Thymeleaf, Bootstrap 5.3, Bootstrap Icons
- **Build Tool**: Maven

---

## 🔑 Default Demo Credentials

| Role | Username | Password |
| :--- | :--- | :--- |
| **Administrator** | `admin` | `admin123` |
| **Faculty (Maths)** | `teacher1` | `teacher123` |
| **Faculty (Physics)** | `teacher2` | `teacher123` |
| **Parent / Student** | `parent1` | `parent123` |
| **Parent / Student** | `parent2` | `parent123` |
| **Parent / Student** | `parent3` | `parent123` |

---

## ⚙️ Running Locally

1. **Clone the repository**:
   ```bash
   git clone https://github.com/amar-kumar-cse/ERP-SYSTEM.git
   cd ERP-SYSTEM
   ```

2. **Build and Run with Maven**:
   ```bash
   mvn clean spring-boot:run
   ```

3. **Access Application**:
   - Open browser at `http://localhost:8080`
   - H2 Database Console available at `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:file:./data/shiksha_erp`)

---

## 📄 License
This project is open source and available under the [MIT License](LICENSE).
