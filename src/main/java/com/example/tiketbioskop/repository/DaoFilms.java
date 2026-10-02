package com.example.tiketbioskop.repository;

import com.example.tiketbioskop.entity.Films;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DaoFilms extends JpaRepository<Films, Integer> {
    // film_name is not unique, so a name lookup can match many films
    Page<Films> findByFilmNameContainingIgnoreCase(String filmName, Pageable pageable);

    Page<Films> findByIsShowing(Boolean isShowing, Pageable pageable);
}
