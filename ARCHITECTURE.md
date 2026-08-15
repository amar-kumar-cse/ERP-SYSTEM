# Shiksha ERP — System Architecture & Technical Specification

## 1. High-Level Architecture Overview

**Shiksha ERP** is an enterprise-grade Coaching Institute Management System designed with Spring Boot 3.3, Spring Security 6, Spring Data JPA, and Thymeleaf + Modern Bento Glassmorphic CSS.

The application follows a clean 3-tier Layered Architecture:

```mermaid
graph TD
    Client[Browser / Client Device] -->|HTTPS Requests| FilterChain[Spring Security 6 Filter Chain]
    FilterChain -->|RBAC / Session Auth| RateLimit[Login Rate Limiting & Lockout]
    RateLimit --> Controllers[Spring MVC Controllers]
    Controllers --> ServiceLayer[Service & Business Logic Layer]
    ServiceLayer --> AccessHelpers[TeacherAccessHelper & ParentStudentHelper]
    ServiceLayer --> Repositories[Spring Data JPA Repositories]
    Repositories --> Database[(MySQL 8 / H2 Database)]
    ServiceLayer --> AsyncEvents[Async Email Service & Schedulers]
    ServiceLayer --> OpenPDF[OpenPDF Export Engine]
    ServiceLayer --> PaymentGateway[Razorpay Payment Engine]
```

---

## 2. Comprehensive Entity Relationship (ER) Diagram

```mermaid
erDiagram
    USERS ||--o| TEACHERS : "has profile"
    USERS ||--o{ STUDENTS : "parent of"
    USERS ||--o{ HELP_TICKETS : "submits"
    USERS ||--o{ TICKET_REPLIES : "writes"
    USERS ||--o{ PASSWORD_RESET_TOKENS : "owns"

    CLASS_BATCHES ||--o{ STUDENTS : "enrolls"
    CLASS_BATCHES ||--o{ TEACHER_BATCHES : "assigned to"
    CLASS_BATCHES ||--o{ ATTENDANCE : "class sessions"
    CLASS_BATCHES ||--o{ REPORTS : "exam records"
    CLASS_BATCHES ||--o{ RESOURCES : "study materials"

    TEACHERS ||--o{ TEACHER_BATCHES : "teaches"
    TEACHERS ||--o{ ATTENDANCE : "marks"
    TEACHERS ||--o{ REPORTS : "evaluates"
    TEACHERS ||--o{ RESOURCES : "uploads"

    STUDENTS ||--o{ ATTENDANCE : "has daily record"
    STUDENTS ||--o{ REPORTS : "scores"
    STUDENTS ||--o{ FEES : "billed"

    HELP_TICKETS ||--o{ TICKET_REPLIES : "contains"

    USERS {
        bigint id PK
        varchar username UK
        varchar password
        varchar email UK
        varchar role "ADMIN | TEACHER | PARENT"
        boolean enabled
        timestamp created_at
        timestamp updated_at
    }

    TEACHERS {
        bigint id PK
        bigint user_id FK
        varchar phone
        varchar subject
        date joining_date
        timestamp created_at
    }

    STUDENTS {
        bigint id PK
        varchar name
        varchar roll_no UK
        varchar parent_name
        varchar parent_phone
        bigint parent_user_id FK
        bigint class_batch_id FK
        timestamp created_at
    }

    CLASS_BATCHES {
        bigint id PK
        varchar batch_name
        varchar timing
        varchar days
        timestamp created_at
    }

    TEACHER_BATCHES {
        bigint id PK
        bigint teacher_id FK
        bigint class_batch_id FK
        timestamp assigned_at
    }

    ATTENDANCE {
        bigint id PK
        bigint student_id FK
        bigint class_batch_id FK
        bigint marked_by_teacher_id FK
        date date
        varchar status "PRESENT | ABSENT"
        timestamp created_at
    }

    REPORTS {
        bigint id PK
        bigint student_id FK
        bigint class_batch_id FK
        bigint uploaded_by_teacher_id FK
        varchar subject
        date exam_date
        int marks
        int max_marks
        varchar remarks
        timestamp created_at
    }

    FEES {
        bigint id PK
        bigint student_id FK
        int month
        int year
        decimal amount_due
        decimal amount_paid
        date due_date
        date paid_date
        varchar status "DUE | PARTIAL | PAID | OVERDUE"
        varchar payment_mode "CASH | UPI | RAZORPAY | NET_BANKING"
        varchar order_id
        varchar transaction_id
        varchar remarks
        timestamp created_at
        timestamp updated_at
    }

    RESOURCES {
        bigint id PK
        varchar title
        varchar resource_type "FILE | LINK"
        varchar file_url
        varchar original_file_name
        bigint file_size
        varchar subject
        varchar description
        bigint class_batch_id FK
        bigint uploaded_by_teacher_id FK
        timestamp uploaded_at
    }

    HELP_TICKETS {
        bigint id PK
        varchar ticket_number UK
        bigint user_id FK
        varchar subject
        varchar category "FEES | ATTENDANCE | ACADEMIC | TECHNICAL | OTHER"
        varchar priority "LOW | MEDIUM | HIGH | URGENT"
        varchar status "OPEN | IN_PROGRESS | RESOLVED | CLOSED"
        text description
        timestamp created_at
        timestamp updated_at
    }

    TICKET_REPLIES {
        bigint id PK
        bigint ticket_id FK
        bigint user_id FK
        text message
        timestamp created_at
    }

    PASSWORD_RESET_TOKENS {
        bigint id PK
        varchar token UK
        bigint user_id FK
        timestamp expiry_date
        timestamp created_at
    }
```

---

## 3. Security & Access Control Architecture

### 3.1 Role-Based Access Control (RBAC)
- **ROLE_ADMIN**: Complete management rights for students, teachers, batches, batch allocations, monthly fee generation, ticket resolutions, and institute metrics.
- **ROLE_TEACHER**: Isolated to assigned batches only via `TeacherAccessHelper`. Can mark attendance, upload test scores, and distribute study materials. Cannot view or modify data for batches they do not teach.
- **ROLE_PARENT**: Isolated to linked child's data only via `ParentStudentHelper`. Can view attendance records, academic report cards, batch resources, make online fee payments, download official PDF receipts, and file support tickets.

### 3.2 Login Rate Limiting & Account Protection
- In-memory sliding window tracker (`LoginAttemptService`) limits failed login attempts to 5 per 15 minutes.
- Temporary lockouts automatically trigger redirect to `/login?locked=true`.
- Password reset flow uses time-limited, single-use UUID tokens (`PasswordResetToken`).

---

## 4. Asynchronous Events & Notifications

- Powered by `@EnableAsync` and Spring Boot's task executor.
- HTML email templates rendered via Thymeleaf `TemplateEngine`.
- Events:
  1. **Monthly Fee Generated**: Sent to parent with due date & invoice details.
  2. **Overdue Fee Alert**: Triggered by automated daily cron scheduler at 01:00 AM.
  3. **New Support Ticket**: Alerts administration team.
  4. **Support Ticket Resolved**: Notifies ticket author with administrator's remarks.
  5. **Password Reset Token**: Delivers secure, expiring link.

---

## 5. Razorpay Online Payment Flow

```mermaid
sequenceDiagram
    autonumber
    actor Parent as Parent / Student
    participant Controller as PaymentController
    participant Service as PaymentService
    participant Gateway as Razorpay API
    participant DB as MySQL Database

    Parent->>Controller: Click "Pay Online" (POST /student/fee/pay/{feeId})
    Controller->>Service: createPaymentOrder(feeId, parentUsername)
    Service->>Gateway: Create Order (amount in paise, receipt id)
    Gateway-->>Service: Return Order ID (rzp_order_xxx)
    Service-->>Controller: Return Order Payload
    Controller-->>Parent: Open Razorpay Checkout Modal
    Parent->>Gateway: Complete Payment (UPI / Card / NetBanking)
    Gateway-->>Parent: Return Payment ID & Signature
    Parent->>Controller: Submit Verification (POST /student/fee/verify)
    Controller->>Service: verifyAndCompletePayment(...)
    Service->>Service: Verify HMAC-SHA256 Signature
    Service->>DB: Update Fee (status=PAID, txId, paidDate=NOW)
    Controller-->>Parent: Redirect /student/fee?paymentSuccess=true
```
