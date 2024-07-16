package com.example.tiketbioskop.business.film;

import com.example.tiketbioskop.entity.Films;
import com.example.tiketbioskop.entity.Schedules;
import com.example.tiketbioskop.repository.DaoFilms;
import com.example.tiketbioskop.repository.DaoSchedules;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FilmsServiceImpl implements FilmsService {

    @Autowired
    private DaoFilms daoFilms;

    @Autowired
    private DaoSchedules daoSchedules;

    // GET
    @Override
    public List<Films> getAllFilms() {
        return daoFilms.findAll();
    }

    @Override
    public List<Films> getAllShowingFilm() {
        return daoFilms.getAllShowingFilm();
    }

    @Override
    public List<Films> getAllNotShowingFilm() {
        return daoFilms.getAllNotShowingFilm();
    }

    @Override
    public Films getFilmByFilmName(String filmName) {
        return daoFilms.findFilmByFilmName(filmName);
    }

    @Override
    public Films getFilmByFilmId(Integer filmId) {
        return daoFilms.findFilmByFilmId(filmId);
    }

    @Override
    public Films getFilmByFilmCode(String filmCode) {
        return daoFilms.findFilmByFilmCode(filmCode);
    }

    @Override
    public void getSchedulesFilms(Integer filmId) {
        List<Schedules> listSchedules = daoSchedules.findSchedulesByFilmId(filmId);
        listSchedules.forEach(schedules -> System.out.println(listSchedules.toString()));
    }

    // POST
    @Override
    public Films addFilm(Films films) {
        return daoFilms.save(films);
    }

    // PUT
    @Override
    public Films updateFilmById(Films films) {
        Films updateFilm = daoFilms.findFilmByFilmId(films.getFilmId());
        updateFilm.setFilmName(films.getFilmName());
        updateFilm.setFilmCode(films.getFilmCode());
        updateFilm.setIsShowing(films.getIsShowing());
        return daoFilms.save(updateFilm);
    }

    // DELETE
    @Override
    public String deleteFilm(int filmId) {
        daoFilms.deleteById((filmId));
        return "Delete film id '" + filmId + "' has been successful!";
    }
}