package com.example.tiketbioskop.repository;

import com.example.tiketbioskop.entity.Schedules;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DaoSchedules extends JpaRepository<Schedules, Integer> {
    // null filter = not filtered
    @Query("""
            select s from Schedules s
            where (:filmId is null or s.film.filmId = :filmId)
              and (:filmDate is null or s.filmDate = :filmDate)""")
    Page<Schedules> search(Integer filmId, LocalDate filmDate, Pageable pageable);

    List<Schedules> findByStudioNameAndFilmDateBetween(Character studioName, LocalDate from, LocalDate to);

    boolean existsByFilmFilmId(Integer filmId);
}
