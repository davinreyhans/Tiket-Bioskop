package com.example.tiketbioskop.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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

    @NotNull
    @Column(name = "film_code", length = 3)
    private String filmCode;

    @Column(name = "film_name")
    private String filmName;

    @NotNull
    @Column(name = "is_showing")
    private Boolean isShowing;
}