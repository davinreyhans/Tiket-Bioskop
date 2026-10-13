package com.example.tiketbioskop.repository;

import com.example.tiketbioskop.entity.Bookings;
import com.example.tiketbioskop.entity.Schedules;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DaoBookings extends JpaRepository<Bookings, Integer> {
    // unpaid reservations past their 5 minutes; their tickets go with them (on delete cascade)
    @Modifying
    @Query("delete from Bookings b where b.schedule = :schedule and b.status = 'RESERVED' and b.expiresAt <= :now")
    int deleteExpired(Schedules schedule, LocalDateTime now);

    @Modifying
    @Query("delete from Bookings b where b.status = 'RESERVED' and b.expiresAt <= :now")
    int deleteAllExpired(LocalDateTime now);

    // reservations still waiting for payment, the one running out first on top
    List<Bookings> findByUserUsernameAndStatusAndExpiresAtAfterOrderByExpiresAt(
            String username, String status, LocalDateTime now);

    // One conditional update, so a payment can't slip past expiresAt while another booking clears
    // the expired reservation: exactly one of the two statements wins. 0 = not payable (anymore).
    @Modifying(clearAutomatically = true)
    @Query("""
            update Bookings b set b.status = 'PAID', b.paidAt = :now
            where b.bookingId = :bookingId and b.status = 'RESERVED' and b.expiresAt > :now""")
    int markPaid(Integer bookingId, LocalDateTime now);
}
