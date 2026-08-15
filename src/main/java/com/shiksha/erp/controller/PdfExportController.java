package com.shiksha.erp.controller;

import com.shiksha.erp.entity.Student;
import com.shiksha.erp.service.ParentStudentHelper;
import com.shiksha.erp.service.PdfExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
@Slf4j
public class PdfExportController {

    private final PdfExportService pdfExportService;
    private final ParentStudentHelper parentStudentHelper;

    /**
     * Parent download fee receipt PDF
     */
    @GetMapping("/student/fee/receipt/{feeId}/pdf")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<byte[]> downloadParentFeeReceipt(@PathVariable Long feeId, Authentication authentication) {
        byte[] pdfBytes = pdfExportService.generateFeeReceiptPdf(feeId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Fee_Receipt_" + feeId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    /**
     * Admin download fee receipt PDF
     */
    @GetMapping("/admin/fee/receipt/{feeId}/pdf")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> downloadAdminFeeReceipt(@PathVariable Long feeId) {
        byte[] pdfBytes = pdfExportService.generateFeeReceiptPdf(feeId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Fee_Receipt_" + feeId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    /**
     * Parent download student academic report card PDF
     */
    @GetMapping("/student/reports/pdf")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<byte[]> downloadParentReportCard(Authentication authentication) {
        Student student = parentStudentHelper.getStudentForParent(authentication.getName());
        byte[] pdfBytes = pdfExportService.generateStudentReportCardPdf(student.getId());

        String filename = "Report_Card_" + student.getRollNo().replace("-", "_") + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    /**
     * Admin or Teacher download student report card PDF
     */
    @GetMapping("/admin/reports/student/{studentId}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<byte[]> downloadStudentReportCardByStaff(@PathVariable Long studentId) {
        byte[] pdfBytes = pdfExportService.generateStudentReportCardPdf(studentId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Report_Card_Student_" + studentId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
