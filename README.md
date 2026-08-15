# 🎓 Shiksha ERP — Coaching Institute Enterprise Management System

[![Build & Test](https://github.com/amar-kumar-cse/ERP-SYSTEM/actions/workflows/ci.yml/badge.svg)](https://github.com/amar-kumar-cse/ERP-SYSTEM/actions)
[![Java](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-6.x-green.svg)](https://spring.io/projects/spring-security)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

An enterprise-ready, full-featured Coaching Institute Enterprise Resource Planning (ERP) platform built with **Spring Boot 3.3**, **Spring Security 6**, **Spring Data JPA**, **MySQL 8 / H2**, **Thymeleaf**, **OpenPDF**, **Razorpay**, and a **Modern Glassmorphic Bento UI**.

---

## 🌟 Key Highlights & Features

### 1. 🛡️ Multi-Tier Role-Based Access Control (RBAC) & Security
- **Administrator (`ROLE_ADMIN`)**:
  - Global institute analytics, faculty and student registration.
  - Class batch scheduling and faculty batch allocation.
  - Bulk monthly fee invoice generation, cash ledger management, and overdue tracking.
  - Centralized support desk & query resolution ticket management.
- **Faculty / Teacher (`ROLE_TEACHER`)**:
  - Class batch rosters isolated strictly to assigned batches.
  - Daily bulk attendance marking with duplicate-day prevention.
  - Exam marks recording with live percentage computation.
  - Study material upload (PDFs, docs, PPTs, links) with MIME/extension sanitization.
- **Student / Parent (`ROLE_PARENT`)**:
  - 30-day attendance metrics & monthly breakdown calendar.
  - Academic report cards with score progression analytics.
  - Razorpay online fee checkout modal with sandbox support.
  - One-click branded PDF download for Fee Invoices and Academic Report Cards.
  - Support ticket submission and real-time thread replies.
- **Security Hardening**:
  - In-memory rate limiting and brute force protection (5 failed attempts / 15-minute lockouts).
  - Password recovery flow with single-use, time-limited tokens.
  - Global `X-Frame-Options: SAMEORIGIN` security header.

### 2. 💳 Real Fee Payment Engine (Razorpay & Cash)
- Integrated Razorpay checkout with HMAC-SHA256 signature verification.
- Seamless sandbox simulated fallback mode for local testing without external credentials.
- Dual-channel support: Retains manual cash/cheque logging for administrative staff alongside digital checkout.

### 3. 📄 Automated PDF Export Engine (OpenPDF)
- Branded, professional **Fee Receipts** with invoice numbers, payment modes, and balance breakdowns.
- Official **Academic Report Cards** displaying test date, subject scores, and teacher remarks.

### 4. 📧 Asynchronous Notifications (Spring Mail)
- Non-blocking email dispatch (`@Async`) for:
  - Monthly fee invoice issuance.
  - Automated overdue fee reminders (01:00 AM daily scheduler).
  - New support ticket alerts to administration.
  - Ticket resolution notifications to parents.
  - Secure password reset links.

### 5. 📊 Interactive Dashboard Analytics (Chart.js)
- **Admin**: 6-Month Fee Collection Trends (Paid vs Due) & Support Desk Donut.
- **Teacher**: Batch Attendance Performance Distribution.
- **Student**: 5-Test Academic Score Progression Line Chart.

---

## 🏗️ Architecture & Database

Comprehensive technical documentation and Mermaid ER diagrams are available in [ARCHITECTURE.md](ARCHITECTURE.md).

```
Shiksha ERP 
├── Controllers (Admin, Teacher, Student, Payment, PdfExport, ForgotPassword)
├── Services (Fee, Attendance, Report, Payment, Email, Resource, Ticket, PdfExport)
├── Access Isolation (TeacherAccessHelper, ParentStudentHelper)
├── Security (Spring Security 6, LoginAttemptService, BCrypt)
└── Repositories & Entities (11 JPA entities, MySQL 8 / H2)
```

---

## 🔑 Default Credentials (Demo / Development)

| Role | Username | Password | Email |
| :--- | :--- | :--- | :--- |
| **Administrator** | `admin` | `admin123` | `admin@shikshaerp.com` |
| **Faculty (Maths)** | `teacher1` | `teacher123` | `teacher1@shikshaerp.com` |
| **Faculty (Physics)** | `teacher2` | `teacher123` | `teacher2@shikshaerp.com` |
| **Parent / Student (Class 10)** | `parent1` | `parent123` | `parent1@example.com` |
| **Parent / Student (Class 12)** | `parent2` | `parent123` | `parent2@example.com` |
| **Parent / Student (Class 9)** | `parent3` | `parent123` | `parent3@example.com` |

---

## 🚀 Getting Started

### Prerequisites
- **Java 17** or higher
- **Maven 3.8+**
- **Docker & Docker Compose** (Optional for containerized run)

### Method 1: Run Locally with Maven
```bash
# 1. Clone repository
git clone https://github.com/amar-kumar-cse/ERP-SYSTEM.git
cd ERP-SYSTEM

# 2. Run unit & integration test suite (100% passing)
mvn clean verify

# 3. Launch Spring Boot application
mvn spring-boot:run
```
- Open your browser at: `http://localhost:8080`
- Access H2 DB Console at: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:file:./data/shiksha_erp`)

---

### Method 2: Run with Docker Compose (Production Setup)
```bash
# Launch Spring Boot App + MySQL 8.0 Containers
docker-compose up -d --build
```
- Application is live at: `http://localhost:8080`
- Database runs on port: `3306`

---

## 🧪 Automated Testing Suite

The repository includes a comprehensive testing suite mirroring the application structure:

```bash
mvn test
```

### Coverage:
- `FeeServiceTest`: Fee generation, skip-if-exists duplicate handling, status auto-computation (DUE/PARTIAL/PAID/OVERDUE), overdue marking.
- `AttendanceServiceTest`: Bulk attendance marking, same-date update prevention, future-date rejection.
- `ReportServiceTest`: Test score calculation, percentage math, max marks validation.
- `AccessControlTest`: Teacher batch data isolation and Parent student boundary protection.
- `PaymentServiceTest`: Razorpay order creation and HMAC-SHA256 verification.
- `ForgotPasswordServiceTest`: Token lifecycle, expiry validation, and password reset.
- `SecurityIntegrationTest`: MockMvc RBAC role checks (403 assertions), login redirects, public routes.
- `ResourceUploadTest`: File extension restrictions (.exe, .sh rejection), size limit checks, and valid PDF uploads.

---

## 📄 License
This project is licensed under the [MIT License](LICENSE).
