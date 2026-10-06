package com.example.tiketbioskop.repository;

import com.example.tiketbioskop.entity.Tickets;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DaoTickets extends JpaRepository<Tickets, Integer> {
    List<Tickets> findByUserUsernameOrderByTicketId(String username);

    List<Tickets> findByScheduleScheduleId(Integer scheduleId);

    boolean existsByScheduleScheduleId(Integer scheduleId);

    boolean existsByUserUserId(Integer userId);
}
