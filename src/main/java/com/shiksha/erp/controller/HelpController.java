package com.shiksha.erp.controller;

import com.shiksha.erp.dto.TicketCreateDto;
import com.shiksha.erp.dto.TicketResponseDto;
import com.shiksha.erp.entity.User;
import com.shiksha.erp.repository.UserRepository;
import com.shiksha.erp.service.HelpTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/help")
@RequiredArgsConstructor
public class HelpController {

    private final HelpTicketService helpTicketService;
    private final UserRepository userRepository;

    @GetMapping
    public String helpIndex(Model model, Authentication auth) {
        User currentUser = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new com.shiksha.erp.exception.ResourceNotFoundException("User", "username", auth.getName()));

        List<TicketResponseDto> myTickets = helpTicketService.getMyTickets(currentUser);

        model.addAttribute("ticketCreateDto", new TicketCreateDto());
        model.addAttribute("myTickets", myTickets);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("activePage", "help");

        return "help/index";
    }

    @PostMapping("/ticket")
    public String submitTicket(
            @Valid @ModelAttribute("ticketCreateDto") TicketCreateDto dto,
            BindingResult bindingResult,
            Authentication auth,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        User currentUser = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new com.shiksha.erp.exception.ResourceNotFoundException("User", "username", auth.getName()));

        if (bindingResult.hasErrors()) {
            model.addAttribute("myTickets", helpTicketService.getMyTickets(currentUser));
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("activePage", "help");
            return "help/index";
        }

        helpTicketService.raiseTicket(dto, currentUser);
        redirectAttributes.addFlashAttribute("successMsg", "Support ticket raised successfully! Administration will respond soon.");
        return "redirect:/help";
    }
}
