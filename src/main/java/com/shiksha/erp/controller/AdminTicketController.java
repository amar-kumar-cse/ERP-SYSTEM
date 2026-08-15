package com.shiksha.erp.controller;

import com.shiksha.erp.dto.TicketResolveDto;
import com.shiksha.erp.dto.TicketResponseDto;
import com.shiksha.erp.enums.TicketStatus;
import com.shiksha.erp.service.HelpTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/tickets")
@RequiredArgsConstructor
public class AdminTicketController {

    private final HelpTicketService helpTicketService;

    @GetMapping
    public String ticketList(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        TicketStatus filterStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                filterStatus = TicketStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        Page<TicketResponseDto> ticketsPage = helpTicketService.getAllTickets(filterStatus, PageRequest.of(page, 15));
        long openTicketsCount = helpTicketService.getOpenCount();

        model.addAttribute("ticketsPage", ticketsPage);
        model.addAttribute("tickets", ticketsPage.getContent());
        model.addAttribute("selectedStatus", status != null ? status : "ALL");
        model.addAttribute("openTicketsCount", openTicketsCount);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", ticketsPage.getTotalPages());
        model.addAttribute("activePage", "tickets");

        return "admin/tickets/list";
    }

    @GetMapping("/{id}")
    public String viewTicket(@PathVariable Long id, Model model) {
        TicketResponseDto ticket = helpTicketService.getTicketById(id);

        TicketResolveDto resolveDto = TicketResolveDto.builder()
                .adminNote(ticket.getAdminNote() != null ? ticket.getAdminNote() : "")
                .build();

        model.addAttribute("ticket", ticket);
        model.addAttribute("ticketResolveDto", resolveDto);
        model.addAttribute("activePage", "tickets");

        return "admin/tickets/view";
    }

    @PostMapping("/{id}/resolve")
    public String resolveTicket(
            @PathVariable Long id,
            @Valid @ModelAttribute("ticketResolveDto") TicketResolveDto dto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            TicketResponseDto ticket = helpTicketService.getTicketById(id);
            model.addAttribute("ticket", ticket);
            model.addAttribute("activePage", "tickets");
            return "admin/tickets/view";
        }

        try {
            helpTicketService.resolveTicket(id, dto);
            redirectAttributes.addFlashAttribute("successMsg", "Ticket #" + id + " marked as resolved with response note.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMsg", ex.getMessage());
        }

        return "redirect:/admin/tickets/" + id;
    }

    @PostMapping("/{id}/reopen")
    public String reopenTicket(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            helpTicketService.reopenTicket(id);
            redirectAttributes.addFlashAttribute("successMsg", "Ticket #" + id + " reopened successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMsg", ex.getMessage());
        }

        return "redirect:/admin/tickets/" + id;
    }
}
