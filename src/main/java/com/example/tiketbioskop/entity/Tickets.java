package com.example.tiketbioskop.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tickets")
public class Tickets {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Integer ticketId;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private Bookings booking;

    @ManyToOne
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedules schedule;

    // the booking itself would loop back to its tickets in JSON, so only its id is sent
    public Integer getBookingId() {
        return booking.getBookingId();
    }

    // Always the schedule's studio; the DB foreign keys keep studio + seat consistent
    @Column(name = "studio_name", nullable = false)
    private Character studioName;

    @Column(name = "seats_code", nullable = false)
    private String seatsCode;
}
