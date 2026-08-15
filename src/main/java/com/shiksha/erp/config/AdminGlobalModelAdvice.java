package com.shiksha.erp.config;

import com.shiksha.erp.service.HelpTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackages = "com.shiksha.erp.controller")
@RequiredArgsConstructor
public class AdminGlobalModelAdvice {

    private final HelpTicketService helpTicketService;

    @ModelAttribute("openTicketCount")
    public long populateOpenTicketCount() {
        try {
            return helpTicketService.getOpenCount();
        } catch (Exception e) {
            return 0;
        }
    }
}
