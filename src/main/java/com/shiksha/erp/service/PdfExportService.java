package com.shiksha.erp.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.shiksha.erp.entity.Fee;
import com.shiksha.erp.entity.Report;
import com.shiksha.erp.entity.Student;
import com.shiksha.erp.repository.FeeRepository;
import com.shiksha.erp.repository.ReportRepository;
import com.shiksha.erp.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfExportService {

    private final FeeRepository feeRepository;
    private final ReportRepository reportRepository;
    private final StudentRepository studentRepository;

    @Value("${app.institute.name:Shiksha Academy}")
    private String instituteName;

    @Value("${app.institute.tagline:Empowering Students to Excel}")
    private String instituteTagline;

    @Value("${app.institute.phone:+91 98765 43210}")
    private String institutePhone;

    @Value("${app.institute.email:support@shikshaerp.com}")
    private String instituteEmail;

    @Value("${app.institute.address:102 Knowledge Park, Tech City, New Delhi - 110001}")
    private String instituteAddress;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMMM yyyy");

    /**
     * Generates a branded Fee Payment Receipt PDF.
     */
    @Transactional(readOnly = true)
    public byte[] generateFeeReceiptPdf(Long feeId) {
        Fee fee = feeRepository.findById(feeId)
                .orElseThrow(() -> new IllegalArgumentException("Fee invoice not found with ID: " + feeId));

        Student student = fee.getStudent();
        String monthName = Month.of(fee.getMonth()).getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            // Fonts
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(37, 99, 235));
            Font subHeaderFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(100, 116, 139));
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(15, 23, 42));
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(15, 23, 42));
            Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(51, 65, 85));

            // 1. Header Banner
            Paragraph instName = new Paragraph(instituteName, headerFont);
            instName.setAlignment(Element.ALIGN_CENTER);
            document.add(instName);

            Paragraph instTag = new Paragraph(instituteTagline, subHeaderFont);
            instTag.setAlignment(Element.ALIGN_CENTER);
            document.add(instTag);

            Paragraph instAddr = new Paragraph(instituteAddress + " | Tel: " + institutePhone + " | Email: " + instituteEmail, subHeaderFont);
            instAddr.setAlignment(Element.ALIGN_CENTER);
            instAddr.setSpacingAfter(15);
            document.add(instAddr);

            // Divider
            PdfPTable divider = new PdfPTable(1);
            divider.setWidthPercentage(100);
            PdfPCell divCell = new PdfPCell(new Phrase(""));
            divCell.setBorder(Rectangle.BOTTOM);
            divCell.setBorderColor(new Color(226, 232, 240));
            divCell.setBorderWidth(2);
            divCell.setPadding(0);
            divider.addCell(divCell);
            document.add(divider);

            // Receipt Title & Metadata Table
            Paragraph receiptTitle = new Paragraph("OFFICIAL FEE RECEIPT", titleFont);
            receiptTitle.setAlignment(Element.ALIGN_CENTER);
            receiptTitle.setSpacingBefore(15);
            receiptTitle.setSpacingAfter(15);
            document.add(receiptTitle);

            PdfPTable metaTable = new PdfPTable(2);
            metaTable.setWidthPercentage(100);
            metaTable.setSpacingAfter(15);

            String receiptNo = "REC-" + fee.getYear() + String.format("%02d", fee.getMonth()) + "-" + fee.getId();
            addMetaRow(metaTable, "Receipt No:", receiptNo, "Issue Date:", LocalDate.now().format(DATE_FMT), boldFont, regularFont);
            addMetaRow(metaTable, "Student Name:", student.getName(), "Roll Number:", student.getRollNo(), boldFont, regularFont);
            addMetaRow(metaTable, "Batch Enrolled:", student.getClassBatch() != null ? student.getClassBatch().getBatchName() : "N/A", "Parent Name:", student.getParentName() != null ? student.getParentName() : "N/A", boldFont, regularFont);
            addMetaRow(metaTable, "Fee Period:", monthName + " " + fee.getYear(), "Payment Status:", fee.getStatus().name(), boldFont, regularFont);
            document.add(metaTable);

            // Itemized Payment Table
            PdfPTable itemTable = new PdfPTable(4);
            itemTable.setWidthPercentage(100);
            itemTable.setWidths(new float[]{40, 20, 20, 20});
            itemTable.setSpacingAfter(20);

            // Header cells
            addTableHeaderCell(itemTable, "Description", boldFont);
            addTableHeaderCell(itemTable, "Amount Due", boldFont);
            addTableHeaderCell(itemTable, "Amount Paid", boldFont);
            addTableHeaderCell(itemTable, "Balance", boldFont);

            // Data Row
            BigDecimal due = fee.getAmountDue();
            BigDecimal paid = fee.getAmountPaid() != null ? fee.getAmountPaid() : BigDecimal.ZERO;
            BigDecimal balance = due.subtract(paid).max(BigDecimal.ZERO);

            addTableCell(itemTable, "Monthly Tuition Fee - " + monthName + " " + fee.getYear(), regularFont, Element.ALIGN_LEFT);
            addTableCell(itemTable, "₹" + due.toString(), regularFont, Element.ALIGN_RIGHT);
            addTableCell(itemTable, "₹" + paid.toString(), boldFont, Element.ALIGN_RIGHT);
            addTableCell(itemTable, "₹" + balance.toString(), regularFont, Element.ALIGN_RIGHT);

            document.add(itemTable);

            // Transaction Details Box
            PdfPTable txnTable = new PdfPTable(2);
            txnTable.setWidthPercentage(100);
            txnTable.setSpacingAfter(35);

            String payMode = fee.getPaymentMode() != null ? fee.getPaymentMode() : "Online / Cash";
            String txnId = fee.getTransactionId() != null ? fee.getTransactionId() : (fee.getOrderId() != null ? fee.getOrderId() : "N/A");
            String paidOn = fee.getPaidDate() != null ? fee.getPaidDate().format(DATE_FMT) : LocalDate.now().format(DATE_FMT);

            addMetaRow(txnTable, "Payment Mode:", payMode, "Payment Date:", paidOn, boldFont, regularFont);
            addMetaRow(txnTable, "Transaction ID / Ref:", txnId, "Remarks:", fee.getRemarks() != null ? fee.getRemarks() : "Fee Cleared", boldFont, regularFont);
            document.add(txnTable);

            // Signatures & Stamp Section
            PdfPTable signTable = new PdfPTable(2);
            signTable.setWidthPercentage(100);

            PdfPCell stampCell = new PdfPCell(new Phrase("Status: " + fee.getStatus().name() + " ✓\n(Computer Generated Document)", regularFont));
            stampCell.setBorder(Rectangle.NO_BORDER);
            stampCell.setHorizontalAlignment(Element.ALIGN_LEFT);
            signTable.addCell(stampCell);

            PdfPCell signCell = new PdfPCell(new Phrase("__________________________\nAuthorized Signatory\nShiksha Academy", boldFont));
            signCell.setBorder(Rectangle.NO_BORDER);
            signCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            signTable.addCell(signCell);

            document.add(signTable);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate Fee PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Error generating Fee Receipt PDF: " + e.getMessage());
        }
    }

    /**
     * Generates a comprehensive Academic Report Card PDF for a student.
     */
    @Transactional(readOnly = true)
    public byte[] generateStudentReportCardPdf(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + studentId));

        List<Report> reports = reportRepository.findByStudentIdOrderByExamDateDesc(studentId);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            // Fonts
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(37, 99, 235));
            Font subHeaderFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(100, 116, 139));
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(15, 23, 42));
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(15, 23, 42));
            Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(51, 65, 85));

            // Header
            Paragraph instName = new Paragraph(instituteName, headerFont);
            instName.setAlignment(Element.ALIGN_CENTER);
            document.add(instName);

            Paragraph instTag = new Paragraph("Academic Performance & Progress Evaluation", subHeaderFont);
            instTag.setAlignment(Element.ALIGN_CENTER);
            document.add(instTag);

            Paragraph instAddr = new Paragraph(instituteAddress + " | Tel: " + institutePhone, subHeaderFont);
            instAddr.setAlignment(Element.ALIGN_CENTER);
            instAddr.setSpacingAfter(15);
            document.add(instAddr);

            Paragraph cardTitle = new Paragraph("STUDENT ACADEMIC REPORT CARD", titleFont);
            cardTitle.setAlignment(Element.ALIGN_CENTER);
            cardTitle.setSpacingAfter(15);
            document.add(cardTitle);

            // Student Meta
            PdfPTable metaTable = new PdfPTable(2);
            metaTable.setWidthPercentage(100);
            metaTable.setSpacingAfter(15);
            addMetaRow(metaTable, "Student Name:", student.getName(), "Roll Number:", student.getRollNo(), boldFont, regularFont);
            addMetaRow(metaTable, "Enrolled Batch:", student.getClassBatch() != null ? student.getClassBatch().getBatchName() : "N/A", "Parent Name:", student.getParentName() != null ? student.getParentName() : "N/A", boldFont, regularFont);
            document.add(metaTable);

            // Marks Table
            PdfPTable marksTable = new PdfPTable(6);
            marksTable.setWidthPercentage(100);
            marksTable.setWidths(new float[]{25, 20, 15, 15, 15, 10});
            marksTable.setSpacingAfter(20);

            addTableHeaderCell(marksTable, "Subject", boldFont);
            addTableHeaderCell(marksTable, "Exam Date", boldFont);
            addTableHeaderCell(marksTable, "Max Marks", boldFont);
            addTableHeaderCell(marksTable, "Obtained", boldFont);
            addTableHeaderCell(marksTable, "Percentage", boldFont);
            addTableHeaderCell(marksTable, "Grade", boldFont);

            double totalMax = 0;
            double totalObtained = 0;

            for (Report r : reports) {
                totalMax += r.getMaxMarks();
                totalObtained += r.getMarks();

                addTableCell(marksTable, r.getSubject(), boldFont, Element.ALIGN_LEFT);
                addTableCell(marksTable, r.getExamDate().format(DATE_FMT), regularFont, Element.ALIGN_CENTER);
                addTableCell(marksTable, String.valueOf(r.getMaxMarks()), regularFont, Element.ALIGN_RIGHT);
                addTableCell(marksTable, String.valueOf(r.getMarks()), boldFont, Element.ALIGN_RIGHT);
                addTableCell(marksTable, String.format("%.1f%%", r.getPercentage()), regularFont, Element.ALIGN_RIGHT);
                addTableCell(marksTable, calculateGrade(r.getPercentage()), boldFont, Element.ALIGN_CENTER);
            }

            if (reports.isEmpty()) {
                PdfPCell emptyCell = new PdfPCell(new Phrase("No examination records available for this student.", regularFont));
                emptyCell.setColspan(6);
                emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                emptyCell.setPadding(12);
                marksTable.addCell(emptyCell);
            }

            document.add(marksTable);

            // Cumulative Summary Box
            if (!reports.isEmpty() && totalMax > 0) {
                double aggregatePercentage = (totalObtained / totalMax) * 100.0;
                String overallGrade = calculateGrade(aggregatePercentage);

                PdfPTable summaryTable = new PdfPTable(4);
                summaryTable.setWidthPercentage(100);
                summaryTable.setSpacingAfter(35);

                addTableHeaderCell(summaryTable, "Total Max Marks", boldFont);
                addTableHeaderCell(summaryTable, "Total Score", boldFont);
                addTableHeaderCell(summaryTable, "Cumulative %", boldFont);
                addTableHeaderCell(summaryTable, "Overall Grade", boldFont);

                addTableCell(summaryTable, String.valueOf((int) totalMax), boldFont, Element.ALIGN_CENTER);
                addTableCell(summaryTable, String.valueOf((int) totalObtained), boldFont, Element.ALIGN_CENTER);
                addTableCell(summaryTable, String.format("%.2f%%", aggregatePercentage), boldFont, Element.ALIGN_CENTER);
                addTableCell(summaryTable, overallGrade, boldFont, Element.ALIGN_CENTER);

                document.add(summaryTable);
            }

            // Signatures
            PdfPTable signTable = new PdfPTable(2);
            signTable.setWidthPercentage(100);

            PdfPCell teacherSign = new PdfPCell(new Phrase("__________________________\nSubject Faculty Signature", boldFont));
            teacherSign.setBorder(Rectangle.NO_BORDER);
            teacherSign.setHorizontalAlignment(Element.ALIGN_LEFT);
            signTable.addCell(teacherSign);

            PdfPCell principalSign = new PdfPCell(new Phrase("__________________________\nDirector / Principal Stamp\nShiksha Academy", boldFont));
            principalSign.setBorder(Rectangle.NO_BORDER);
            principalSign.setHorizontalAlignment(Element.ALIGN_RIGHT);
            signTable.addCell(principalSign);

            document.add(signTable);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate Report Card PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Error generating Report Card PDF: " + e.getMessage());
        }
    }

    private String calculateGrade(double percentage) {
        if (percentage >= 90) return "A+";
        if (percentage >= 80) return "A";
        if (percentage >= 70) return "B";
        if (percentage >= 60) return "C";
        if (percentage >= 50) return "D";
        return "F";
    }

    private void addMetaRow(PdfPTable table, String l1, String v1, String l2, String v2, Font bold, Font regular) {
        PdfPCell c1 = new PdfPCell(new Phrase(l1 + " " + v1, bold));
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setPadding(3);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(l2 + " " + v2, bold));
        c2.setBorder(Rectangle.NO_BORDER);
        c2.setPadding(3);
        table.addCell(c2);
    }

    private void addTableHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(241, 245, 249));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(6);
        cell.setBorderColor(new Color(203, 213, 225));
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(align);
        cell.setPadding(6);
        cell.setBorderColor(new Color(226, 232, 240));
        table.addCell(cell);
    }
}
