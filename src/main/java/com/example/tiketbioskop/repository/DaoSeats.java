package com.example.tiketbioskop.repository;

import com.example.tiketbioskop.entity.Seats;
import com.example.tiketbioskop.model.SeatsId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DaoSeats extends JpaRepository<Seats, SeatsId> {
    List<Seats> findByStudioName(Character studioName);

    boolean existsByStudioName(Character studioName);
}
