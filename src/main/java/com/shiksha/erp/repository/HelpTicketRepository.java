package com.shiksha.erp.repository;

import com.shiksha.erp.entity.HelpTicket;
import com.shiksha.erp.entity.User;
import com.shiksha.erp.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HelpTicketRepository extends JpaRepository<HelpTicket, Long> {

    List<HelpTicket> findByRaisedByOrderByCreatedAtDesc(User user);

    List<HelpTicket> findByRaisedByIdOrderByCreatedAtDesc(Long userId);

    List<HelpTicket> findByStatusOrderByCreatedAtDesc(TicketStatus status);

    Page<HelpTicket> findByStatusOrderByCreatedAtDesc(TicketStatus status, Pageable pageable);

    Page<HelpTicket> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(TicketStatus status);
}
