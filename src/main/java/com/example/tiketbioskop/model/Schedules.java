package com.example.tiketbioskop.model;

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
    @Column(name = "schedules_id")
    private Integer schedulesId;

    @ManyToOne(targetEntity = Films.class)
    @JoinColumn(name = "film_id", nullable = false)
    private Films filmId;

    @Column(name = "film_date")
    private String filmDate;

    @Column(name = "film_start_time")
    private String filmStartTime;

    @Column(name = "film_end_time")
    private String filmEndTime;

    @Column(name = "ticket_price")
    private Integer ticketPrice;
}
