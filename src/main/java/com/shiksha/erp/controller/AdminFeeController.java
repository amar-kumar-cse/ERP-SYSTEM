package com.shiksha.erp.controller;

import com.shiksha.erp.dto.FeeGenerateDto;
import com.shiksha.erp.dto.FeeResponseDto;
import com.shiksha.erp.dto.FeeUpdateDto;
import com.shiksha.erp.entity.ClassBatch;
import com.shiksha.erp.repository.ClassBatchRepository;
import com.shiksha.erp.service.FeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Controller
@RequestMapping("/admin/fee")
@RequiredArgsConstructor
public class AdminFeeController {

    private final FeeService feeService;
    private final ClassBatchRepository classBatchRepository;

    @Value("${app.fee.default-amount:1500}")
    private BigDecimal defaultAmount;

    @GetMapping
    public String feeList(
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            Model model
    ) {
        LocalDate now = LocalDate.now();
        int selectedMonth = (month != null && month >= 1 && month <= 12) ? month : now.getMonthValue();
        int selectedYear = (year != null && year >= 2020 && year <= 2030) ? year : now.getYear();

        List<ClassBatch> batches = classBatchRepository.findAll();
        List<FeeResponseDto> feeList = feeService.getFeesByBatchAndMonth(batchId, selectedMonth, selectedYear);

        long dueCount = feeService.getDueCountForMonth(selectedMonth, selectedYear);
        long paidCount = feeService.getPaidCountForMonth(selectedMonth, selectedYear);
        long totalOverdueCount = feeService.getOverdueCount();

        model.addAttribute("batches", batches);
        model.addAttribute("selectedBatchId", batchId);
        model.addAttribute("selectedMonth", selectedMonth);
        model.addAttribute("selectedYear", selectedYear);
        model.addAttribute("feeList", feeList);
        model.addAttribute("dueCount", dueCount);
        model.addAttribute("paidCount", paidCount);
        model.addAttribute("totalOverdueCount", totalOverdueCount);
        model.addAttribute("activePage", "fee");

        return "admin/fee/list";
    }

    @GetMapping("/generate")
    public String generateForm(Model model) {
        LocalDate now = LocalDate.now();
        YearMonth ym = YearMonth.of(now.getYear(), now.getMonthValue());

        FeeGenerateDto dto = FeeGenerateDto.builder()
                .month(now.getMonthValue())
                .year(now.getYear())
                .amountDue(defaultAmount)
                .dueDate(ym.atEndOfMonth())
                .build();

        model.addAttribute("feeGenerateDto", dto);
        model.addAttribute("batches", classBatchRepository.findAll());
        model.addAttribute("activePage", "fee");

        return "admin/fee/generate";
    }

    @PostMapping("/generate")
    public String handleGenerate(
            @Valid @ModelAttribute("feeGenerateDto") FeeGenerateDto dto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("batches", classBatchRepository.findAll());
            model.addAttribute("activePage", "fee");
            return "admin/fee/generate";
        }

        int count = feeService.generateMonthlyFees(dto);
        redirectAttributes.addFlashAttribute("successMsg", count + " new student fee records generated successfully!");
        return "redirect:/admin/fee?month=" + dto.getMonth() + "&year=" + dto.getYear() + (dto.getClassBatchId() != null ? "&batchId=" + dto.getClassBatchId() : "");
    }

    @PostMapping("/update/{feeId}")
    public String updateFee(
            @PathVariable Long feeId,
            @Valid @ModelAttribute FeeUpdateDto dto,
            BindingResult bindingResult,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMsg", "Invalid fee payment details provided");
            return "redirect:/admin/fee" + (month != null && year != null ? "?month=" + month + "&year=" + year : "");
        }
        try {
            feeService.updateFee(feeId, dto);
            redirectAttributes.addFlashAttribute("successMsg", "Fee payment record updated successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMsg", ex.getMessage());
        }

        String redirectUrl = "/admin/fee";
        if (month != null && year != null) {
            redirectUrl += "?month=" + month + "&year=" + year;
            if (batchId != null) {
                redirectUrl += "&batchId=" + batchId;
            }
        }

        return "redirect:" + redirectUrl;
    }

    @PostMapping("/generate-overdue")
    public String manualMarkOverdue(RedirectAttributes redirectAttributes) {
        int updated = feeService.markOverdueFees();
        redirectAttributes.addFlashAttribute("successMsg", "Overdue status scan complete. " + updated + " records marked OVERDUE.");
        return "redirect:/admin/fee";
    }
}
