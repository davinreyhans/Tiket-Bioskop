package com.example.tiketbioskop.usecase.film;

import com.example.tiketbioskop.entity.Films;
import com.example.tiketbioskop.exception.ConflictException;
import com.example.tiketbioskop.exception.NotFoundException;
import com.example.tiketbioskop.repository.DaoFilms;
import com.example.tiketbioskop.repository.DaoSchedules;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FilmsUseCase {

    private final DaoFilms daoFilms;

    private final DaoSchedules daoSchedules;

    // GET
    public Page<Films> getFilms(Boolean isShowing, Pageable pageable) {
        return isShowing == null ? daoFilms.findAll(pageable) : daoFilms.findByIsShowing(isShowing, pageable);
    }

    public Page<Films> searchFilmsByName(String filmName, Pageable pageable) {
        return daoFilms.findByFilmNameContainingIgnoreCase(filmName, pageable);
    }

    public Films getFilmByFilmId(Integer filmId) {
        return daoFilms.findById(filmId)
                .orElseThrow(() -> new NotFoundException("Film id '" + filmId + "' not found."));
    }

    // POST
    public Films addFilm(FilmRequest request) {
        Films films = new Films();
        apply(films, request);
        return daoFilms.save(films);
    }

    // PUT
    public Films updateFilm(Integer filmId, FilmRequest request) {
        Films films = getFilmByFilmId(filmId);
        apply(films, request);
        return daoFilms.save(films);
    }

    // DELETE
    public void deleteFilm(Integer filmId) {
        Films films = getFilmByFilmId(filmId);
        if (daoSchedules.existsByFilmFilmId(filmId)) {
            throw new ConflictException("Film id '" + filmId + "' still has schedules, delete them first"
                    + " (or set isShowing to false to hide it).");
        }
        daoFilms.delete(films);
    }

    private static void apply(Films films, FilmRequest request) {
        films.setFilmCode(request.filmCode());
        films.setFilmName(request.filmName());
        films.setIsShowing(request.isShowing());
    }
}
