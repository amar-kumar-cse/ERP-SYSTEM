package com.shiksha.erp.config;

import com.shiksha.erp.service.HelpTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.concurrent.atomic.AtomicLong;

@ControllerAdvice(basePackages = "com.shiksha.erp.controller")
@RequiredArgsConstructor
public class AdminGlobalModelAdvice {

    private final HelpTicketService helpTicketService;

    private static final long CACHE_TTL_MS = 30_000L;
    private final AtomicLong lastFetchTime = new AtomicLong(0);
    private final AtomicLong cachedCount = new AtomicLong(0);

    @ModelAttribute("openTicketCount")
    public long populateOpenTicketCount() {
        long now = System.currentTimeMillis();
        long last = lastFetchTime.get();
        if (now - last > CACHE_TTL_MS) {
            try {
                long freshCount = helpTicketService.getOpenCount();
                cachedCount.set(freshCount);
                lastFetchTime.set(now);
                return freshCount;
            } catch (Exception e) {
                return cachedCount.get();
            }
        }
        return cachedCount.get();
    }
}
