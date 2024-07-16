package com.example.tiketbioskop.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(name = "Films")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "film_code"))
public class Films {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "film_id")
    private Integer filmId;

    @Column(name = "film_code")
    private String filmCode;

    @Column(name = "film_name")
    private String filmName;

    @Column(name = "is_showing")
    private Boolean isShowing;
}