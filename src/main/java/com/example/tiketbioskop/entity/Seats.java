package com.example.tiketbioskop.entity;

import com.example.tiketbioskop.model.SeatsId;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@IdClass(SeatsId.class)
public class Seats {
    @Id
    private Character studioName;

    @Id
    private String seatsCode;
}
