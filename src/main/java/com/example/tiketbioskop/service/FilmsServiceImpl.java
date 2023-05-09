package com.example.tiketbioskop.service;

import com.example.tiketbioskop.model.Films;
import com.example.tiketbioskop.model.Schedules;
import com.example.tiketbioskop.repository.FilmsRepository;
import com.example.tiketbioskop.repository.SchedulesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FilmsServiceImpl implements FilmsService {
    @Autowired
    private FilmsRepository filmsRepository;

    @Autowired
    private SchedulesRepository schedulesRepository;

    //GET
    @Override
    public List<Films> getAllFilms() {
        return filmsRepository.findAll();
    }
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
    @Override
    public void getSchedulesFilms(Integer filmId) {
        List<Schedules> listSchedules = schedulesRepository.findSchedulesByFilmId(filmId);
        listSchedules.forEach(schedules -> System.out.println(listSchedules.toString()));
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
        return "Delete film id '" + filmId + "' has been successful!";
    }
}