package com.shiksha.erp.service;

import com.shiksha.erp.dto.TicketCreateDto;
import com.shiksha.erp.dto.TicketResolveDto;
import com.shiksha.erp.dto.TicketResponseDto;
import com.shiksha.erp.entity.HelpTicket;
import com.shiksha.erp.entity.User;
import com.shiksha.erp.enums.TicketStatus;
import com.shiksha.erp.repository.HelpTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HelpTicketService {

    private final HelpTicketRepository helpTicketRepository;

    @Transactional
    public HelpTicket raiseTicket(TicketCreateDto dto, User user) {
        HelpTicket ticket = HelpTicket.builder()
                .raisedBy(user)
                .title(dto.getTitle().trim())
                .message(dto.getMessage().trim())
                .status(TicketStatus.OPEN)
                .build();

        return helpTicketRepository.save(ticket);
    }

    @Transactional
    public void resolveTicket(Long id, TicketResolveDto dto) {
        HelpTicket ticket = helpTicketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Help ticket not found with id: " + id));

        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setAdminNote(dto.getAdminNote().trim());
        ticket.setResolvedAt(LocalDateTime.now());

        helpTicketRepository.save(ticket);
    }

    @Transactional
    public void reopenTicket(Long id) {
        HelpTicket ticket = helpTicketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Help ticket not found with id: " + id));

        ticket.setStatus(TicketStatus.OPEN);
        ticket.setResolvedAt(null);

        helpTicketRepository.save(ticket);
    }

    @Transactional(readOnly = true)
    public List<TicketResponseDto> getMyTickets(User user) {
        return helpTicketRepository.findByRaisedByOrderByCreatedAtDesc(user)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<TicketResponseDto> getAllTickets(TicketStatus filter, Pageable pageable) {
        Page<HelpTicket> page;
        if (filter != null) {
            page = helpTicketRepository.findByStatusOrderByCreatedAtDesc(filter, pageable);
        } else {
            page = helpTicketRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        return page.map(this::toResponseDto);
    }

    @Transactional(readOnly = true)
    public TicketResponseDto getTicketById(Long id) {
        HelpTicket ticket = helpTicketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Help ticket not found with id: " + id));
        return toResponseDto(ticket);
    }

    @Transactional(readOnly = true)
    public long getOpenCount() {
        return helpTicketRepository.countByStatus(TicketStatus.OPEN);
    }

    private TicketResponseDto toResponseDto(HelpTicket t) {
        return TicketResponseDto.builder()
                .id(t.getId())
                .raisedByUsername(t.getRaisedBy().getUsername())
                .raisedByRole(t.getRaisedBy().getRole().name())
                .title(t.getTitle())
                .message(t.getMessage())
                .status(t.getStatus())
                .adminNote(t.getAdminNote())
                .createdAt(t.getCreatedAt())
                .resolvedAt(t.getResolvedAt())
                .build();
    }
}
