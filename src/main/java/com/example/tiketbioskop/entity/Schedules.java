package com.example.tiketbioskop.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Entity
@Table(name = "Schedules")
public class Schedules {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id", nullable = false)
    private Integer scheduleId;

    @ManyToOne(targetEntity = Films.class)
    @JoinColumn(name = "film_id", nullable = false)
    private Films filmId;

    @Column(name = "film_date", nullable = false)
    private String filmDate;

    @Column(name = "film_start_time", nullable = false)
    private String filmStartTime;

    @Column(name = "film_end_time", nullable = false)
    private String filmEndTime;

    @Column(name = "ticket_price", nullable = false)
    private Integer ticketPrice;
}
