package com.example.tiketbioskop.repository;

import com.example.tiketbioskop.model.Seats;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@Transactional
public interface SeatsRepository extends JpaRepository <Seats, Integer> {
    Seats findSeatsBySeatsCode(String seatsCode);
}
