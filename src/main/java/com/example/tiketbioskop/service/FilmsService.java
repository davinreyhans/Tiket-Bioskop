package com.example.tiketbioskop.service;

import com.example.tiketbioskop.model.Films;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface FilmsService {
    List<Films> getAllFilms();

    Films getFilmById(Integer filmId);

    Films getFilmByCode(String filmCode);

    Films getFilmByName(String filmName);

    void getSchedulesFilms(Integer filmId);

    Films addFilm(Films films);

    Films updateFilmById(Films films);

    String deleteFilm(int filmId);
}
