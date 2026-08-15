package com.shiksha.erp.config;

import com.shiksha.erp.entity.*;
import com.shiksha.erp.enums.*;
import com.shiksha.erp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Profile("!test & !prod")
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final ClassBatchRepository classBatchRepository;
    private final TeacherBatchRepository teacherBatchRepository;
    private final AttendanceRepository attendanceRepository;
    private final ReportRepository reportRepository;
    private final ResourceRepository resourceRepository;
    private final FeeRepository feeRepository;
    private final HelpTicketRepository helpTicketRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already seeded. Skipping initial data seeding.");
            return;
        }

        log.info("Starting Shiksha ERP comprehensive demo data seeding...");

        // 1. Admin User
        User admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .email("admin@shiksha.com")
                .role(Role.ADMIN)
                .enabled(true)
                .build();
        userRepository.save(admin);

        // 2. Class Batches
        ClassBatch batch1 = ClassBatch.builder()
                .batchName("Class 10 - Batch A (Maths & Science)")
                .timing("4:00 PM - 6:00 PM")
                .days("Mon, Wed, Fri")
                .build();

        ClassBatch batch2 = ClassBatch.builder()
                .batchName("Class 12 - JEE Advanced FastTrack")
                .timing("6:30 PM - 8:30 PM")
                .days("Tue, Thu, Sat")
                .build();

        ClassBatch batch3 = ClassBatch.builder()
                .batchName("Class 9 - Foundation Batch")
                .timing("2:00 PM - 4:00 PM")
                .days("Mon, Wed, Fri")
                .build();

        ClassBatch savedBatch1 = classBatchRepository.save(batch1);
        ClassBatch savedBatch2 = classBatchRepository.save(batch2);
        ClassBatch savedBatch3 = classBatchRepository.save(batch3);

        // 3. Faculty / Teachers
        User teacherUser1 = User.builder()
                .username("teacher1")
                .password(passwordEncoder.encode("teacher123"))
                .email("t1@shiksha.com")
                .role(Role.TEACHER)
                .enabled(true)
                .build();

        Teacher teacher1 = Teacher.builder()
                .user(teacherUser1)
                .firstName("Ramesh")
                .lastName("Verma")
                .phone("9811223344")
                .subject("Mathematics")
                .joiningDate(LocalDate.now().minusMonths(6))
                .build();
        Teacher savedTeacher1 = teacherRepository.save(teacher1);

        User teacherUser2 = User.builder()
                .username("teacher2")
                .password(passwordEncoder.encode("teacher123"))
                .email("t2@shiksha.com")
                .role(Role.TEACHER)
                .enabled(true)
                .build();

        Teacher teacher2 = Teacher.builder()
                .user(teacherUser2)
                .firstName("Sunita")
                .lastName("Sharma")
                .phone("9822334455")
                .subject("Physics")
                .joiningDate(LocalDate.now().minusMonths(4))
                .build();
        Teacher savedTeacher2 = teacherRepository.save(teacher2);

        // Assign Batches to Teachers
        teacherBatchRepository.save(TeacherBatch.builder().teacher(savedTeacher1).classBatch(savedBatch1).build());
        teacherBatchRepository.save(TeacherBatch.builder().teacher(savedTeacher1).classBatch(savedBatch2).build());
        teacherBatchRepository.save(TeacherBatch.builder().teacher(savedTeacher2).classBatch(savedBatch1).build());

        // 4. Parents & Students
        User parentUser1 = User.builder()
                .username("parent1")
                .password(passwordEncoder.encode("parent123"))
                .email("p1@shiksha.com")
                .role(Role.PARENT)
                .enabled(true)
                .build();
        userRepository.save(parentUser1);

        Student student1 = Student.builder()
                .name("Aarav Sharma")
                .rollNo("SHK-2026-001")
                .parentName("Rajesh Sharma")
                .parentPhone("9876543210")
                .parentUser(parentUser1)
                .classBatch(savedBatch1)
                .build();
        Student savedStudent1 = studentRepository.save(student1);

        User parentUser2 = User.builder()
                .username("parent2")
                .password(passwordEncoder.encode("parent123"))
                .email("p2@shiksha.com")
                .role(Role.PARENT)
                .enabled(true)
                .build();
        userRepository.save(parentUser2);

        Student student2 = Student.builder()
                .name("Priya Patel")
                .rollNo("SHK-2026-002")
                .parentName("Dinesh Patel")
                .parentPhone("9876543211")
                .parentUser(parentUser2)
                .classBatch(savedBatch1)
                .build();
        Student savedStudent2 = studentRepository.save(student2);

        User parentUser3 = User.builder()
                .username("parent3")
                .password(passwordEncoder.encode("parent123"))
                .email("p3@shiksha.com")
                .role(Role.PARENT)
                .enabled(true)
                .build();
        userRepository.save(parentUser3);

        Student student3 = Student.builder()
                .name("Rohan Verma")
                .rollNo("SHK-2026-003")
                .parentName("Mukesh Verma")
                .parentPhone("9876543212")
                .parentUser(parentUser3)
                .classBatch(savedBatch1)
                .build();
        Student savedStudent3 = studentRepository.save(student3);

        Student student4 = Student.builder()
                .name("Sneha Gupta")
                .rollNo("SHK-2026-004")
                .parentName("Rajesh Sharma")
                .parentPhone("9876543210")
                .parentUser(parentUser1)
                .classBatch(savedBatch2)
                .build();
        studentRepository.save(student4);

        // 5. Past Attendance Records (Class 10)
        LocalDate today = LocalDate.now();
        List<LocalDate> pastDates = List.of(
                today.minusDays(7),
                today.minusDays(5),
                today.minusDays(3),
                today.minusDays(1)
        );

        for (LocalDate date : pastDates) {
            attendanceRepository.save(Attendance.builder()
                    .student(savedStudent1).classBatch(savedBatch1).date(date).status(AttendanceStatus.PRESENT).markedBy(savedTeacher1).build());
            attendanceRepository.save(Attendance.builder()
                    .student(savedStudent2).classBatch(savedBatch1).date(date).status(AttendanceStatus.PRESENT).markedBy(savedTeacher1).build());
            attendanceRepository.save(Attendance.builder()
                    .student(savedStudent3).classBatch(savedBatch1).date(date).status(date.equals(today.minusDays(5)) ? AttendanceStatus.ABSENT : AttendanceStatus.PRESENT).markedBy(savedTeacher1).build());
        }

        // 6. Test Reports
        reportRepository.save(Report.builder()
                .student(savedStudent1).classBatch(savedBatch1).subject("Mathematics")
                .examDate(today.minusDays(10)).marks(92).maxMarks(100).remarks("Outstanding conceptual clarity in Algebra").uploadedBy(savedTeacher1).build());
        reportRepository.save(Report.builder()
                .student(savedStudent2).classBatch(savedBatch1).subject("Mathematics")
                .examDate(today.minusDays(10)).marks(85).maxMarks(100).remarks("Good calculation speed").uploadedBy(savedTeacher1).build());
        reportRepository.save(Report.builder()
                .student(savedStudent3).classBatch(savedBatch1).subject("Mathematics")
                .examDate(today.minusDays(10)).marks(74).maxMarks(100).remarks("Needs practice in Quadratic formulas").uploadedBy(savedTeacher1).build());

        reportRepository.save(Report.builder()
                .student(savedStudent1).classBatch(savedBatch1).subject("Physics")
                .examDate(today.minusDays(4)).marks(88).maxMarks(100).remarks("Excellent work in Optics numericals").uploadedBy(savedTeacher2).build());
        reportRepository.save(Report.builder()
                .student(savedStudent2).classBatch(savedBatch1).subject("Physics")
                .examDate(today.minusDays(4)).marks(79).maxMarks(100).remarks("Revise Ray Diagrams").uploadedBy(savedTeacher2).build());

        // 7. Study Resources
        resourceRepository.save(Resource.builder()
                .title("Class 10 Quadratic Equations Theory & Formula Sheet")
                .resourceType(ResourceType.LINK)
                .fileUrl("https://youtube.com")
                .classBatch(savedBatch1)
                .subject("Mathematics")
                .description("Comprehensive chapter summary, derivation of quadratic formula, and 20 practice questions.")
                .uploadedBy(savedTeacher1)
                .build());

        resourceRepository.save(Resource.builder()
                .title("Light: Reflection & Refraction Question Bank")
                .resourceType(ResourceType.LINK)
                .fileUrl("https://drive.google.com")
                .classBatch(savedBatch1)
                .subject("Physics")
                .description("Previous year board questions with detailed step-by-step solutions.")
                .uploadedBy(savedTeacher2)
                .build());

        // 8. Fee Invoices
        int curMonth = today.getMonthValue();
        int curYear = today.getYear();
        LocalDate due = today.withDayOfMonth(today.lengthOfMonth());

        // Current Month: Aarav PAID, Priya DUE, Rohan OVERDUE
        feeRepository.save(Fee.builder()
                .student(savedStudent1)
                .month(curMonth)
                .year(curYear)
                .amountDue(new BigDecimal("1500.00"))
                .amountPaid(new BigDecimal("1500.00"))
                .status(FeeStatus.PAID)
                .dueDate(due)
                .paidDate(today.minusDays(2))
                .paymentMode("UPI")
                .remarks("GPay Ref #8839201948")
                .build());

        feeRepository.save(Fee.builder()
                .student(savedStudent2)
                .month(curMonth)
                .year(curYear)
                .amountDue(new BigDecimal("1500.00"))
                .amountPaid(BigDecimal.ZERO)
                .status(FeeStatus.DUE)
                .dueDate(due)
                .build());

        feeRepository.save(Fee.builder()
                .student(savedStudent3)
                .month(curMonth)
                .year(curYear)
                .amountDue(new BigDecimal("1500.00"))
                .amountPaid(new BigDecimal("500.00"))
                .status(FeeStatus.PARTIAL)
                .dueDate(due)
                .paidDate(today.minusDays(4))
                .paymentMode("Cash")
                .remarks("Partial token advance paid at counter")
                .build());

        // 9. Help Center Tickets
        helpTicketRepository.save(HelpTicket.builder()
                .raisedBy(parentUser1)
                .title("Request for Parent-Teacher Meeting slot")
                .message("Hello Admin, Can we schedule a 15-min meeting with Mathematics faculty this Saturday?")
                .status(TicketStatus.OPEN)
                .build());

        helpTicketRepository.save(HelpTicket.builder()
                .raisedBy(parentUser2)
                .title("Access inquiry for Physics study material")
                .message("We were looking for the optics question bank link.")
                .status(TicketStatus.RESOLVED)
                .adminNote("The study material link has been updated in the Resources tab.")
                .resolvedAt(LocalDateTime.now().minusDays(1))
                .build());

        log.info("Shiksha ERP initial seeding completed successfully with all modules connected.");
    }
}
