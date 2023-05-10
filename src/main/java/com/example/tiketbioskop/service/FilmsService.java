package com.example.tiketbioskop.service;

import com.example.tiketbioskop.model.Films;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface FilmsService {
    List<Films> getAllFilms();

    Films getFilmByFilmId(Integer filmId);

    Films getFilmByFilmCode(String filmCode);

    Films getFilmByFilmName(String filmName);

    List<Films> getAllShowingFilm();

    List<Films> getAllNotShowingFilm();

    void getSchedulesFilms(Integer filmId);

    Films addFilm(Films films);

    Films updateFilmById(Films films);

    String deleteFilm(int filmId);
}
