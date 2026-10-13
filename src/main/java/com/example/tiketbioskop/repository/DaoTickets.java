package com.example.tiketbioskop.repository;

import com.example.tiketbioskop.entity.Bookings;
import com.example.tiketbioskop.entity.Tickets;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DaoTickets extends JpaRepository<Tickets, Integer> {
    // only paid tickets; reservations waiting for payment are listed as bookings
    @Query("select t from Tickets t where t.user.username = :username and t.booking.status = 'PAID' order by t.ticketId")
    List<Tickets> findPaidByUsername(String username);

    // A ticket holds its seat while its booking is paid or still within its reservation time.
    // Expired reservations count as free right away, without waiting for anything to delete them.
    @Query("""
            select t.seatsCode from Tickets t
            where t.schedule.scheduleId = :scheduleId
              and (t.booking.status = 'PAID' or t.booking.expiresAt > :now)""")
    List<String> findHeldSeats(Integer scheduleId, LocalDateTime now);

    boolean existsByBooking(Bookings booking);

    boolean existsByScheduleScheduleId(Integer scheduleId);

    boolean existsByUserUserId(Integer userId);
}
