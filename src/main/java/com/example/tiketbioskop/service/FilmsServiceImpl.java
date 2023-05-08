package com.example.tiketbioskop.service;

import com.example.tiketbioskop.model.Films;
import com.example.tiketbioskop.repository.FilmsRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FilmsServiceImpl implements FilmsService {
    private final FilmsRepository filmsRepository;
    public FilmsServiceImpl(FilmsRepository filmsRepository) {
        this.filmsRepository = filmsRepository;
    }

    //GET
//    @Override
//    public List<Films> getAllFilms() {
//        return filmsRepository.findAll();
//    }
    @Override
    public Films getFilmById(Integer filmId) {
        return filmsRepository.findFilmByFilmId(filmId);
    }
    @Override
    public Films getFilmByCode(String filmCode) {
        return filmsRepository.findFilmByFilmCode(filmCode);
    }
    @Override
    public Films getFilmByName(String filmName) {
        return filmsRepository.findFilmByFilmName(filmName);
    }

    //POST
    @Override
    public Films addFilm(Films films) {
        return filmsRepository.save(films);
    }

    //PUT
    @Override
    public Films updateFilmById(Films films) {
        Films updateFilm = filmsRepository.findFilmByFilmId(films.getFilmId());
        updateFilm.setFilmName(films.getFilmName());
        updateFilm.setFilmCode(films.getFilmCode());
        updateFilm.setIsShowing(films.getIsShowing());
        return filmsRepository.save(updateFilm);
    }

    //DELETE
    @Override
    public String deleteFilm(int filmId) {
        filmsRepository.deleteById((filmId));
        return "Delete user id '" + filmId + "' has been successful!";
    }
}