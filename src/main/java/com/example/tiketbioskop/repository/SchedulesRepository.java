package com.example.tiketbioskop.repository;

import com.example.tiketbioskop.model.Schedules;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Transactional
public interface SchedulesRepository extends JpaRepository<Schedules, Integer> {
    Schedules findSchedulesByScheduleId(Integer scheduleId);

    @Query(value = "select s.*, f.film_name from schedules s "
            + "inner join films f on s.film_id = f.film_id " +
            "where f.film_id = :film_id", nativeQuery = true)
    List<Schedules> findSchedulesByFilmId(@Param("film_id") Integer filmId);
}
