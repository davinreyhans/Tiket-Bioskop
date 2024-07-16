package com.example.tiketbioskop.repository;

import com.example.tiketbioskop.entity.Films;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Transactional
public interface DaoFilms extends JpaRepository<Films, Integer> {
    List<Films> findAll();

    Films findFilmByFilmId(Integer filmId);

    Films findFilmByFilmCode(String filmCode);

    Films findFilmByFilmName(String filmName);

    @Query(value = "select * from Films f where f.is_showing=true", nativeQuery = true)
    List<Films> getAllShowingFilm();

    @Query(value = "select * from Films f where f.is_showing=false", nativeQuery = true)
    List<Films> getAllNotShowingFilm();
}