package com.example.tiketbioskop.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;


@Getter
@Setter
@Entity
@Table(name = "Schedules")
public class Schedules {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id", nullable = false)
    private Integer scheduleId;

    @ManyToOne
    @JoinColumn(name = "film_id", nullable = false)
    private Films film;

    @Column(name = "studio_name", nullable = false)
    private Character studioName;

    @Column(name = "film_date", nullable = false)
    private LocalDate filmDate;

    @Column(name = "film_start_time", nullable = false)
    private LocalTime filmStartTime;

    @Column(name = "film_end_time", nullable = false)
    private LocalTime filmEndTime;

    @Column(name = "ticket_price", nullable = false)
    private Integer ticketPrice;

    // not getX(), so Jackson and Hibernate leave them alone
    public LocalDateTime startsAt() {
        return LocalDateTime.of(filmDate, filmStartTime);
    }

    // an end time at or before the start time means the show ends the next day
    public LocalDateTime endsAt() {
        LocalDate endDate = filmEndTime.isAfter(filmStartTime) ? filmDate : filmDate.plusDays(1);
        return LocalDateTime.of(endDate, filmEndTime);
    }
}
