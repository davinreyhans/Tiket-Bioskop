package com.example.tiketbioskop.controller;

import com.example.tiketbioskop.entity.Films;
import com.example.tiketbioskop.usecase.film.FilmRequest;
import com.example.tiketbioskop.usecase.film.FilmsUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/films")
@RequiredArgsConstructor
public class FilmsController {
    private final FilmsUseCase filmsUseCase;

    // Get all films, optionally filtered: /films?showing=true&page=0&size=20
    @GetMapping
    public Page<Films> getFilms(@RequestParam(required = false) Boolean showing,
                                @SortDefault(sort = "filmId") Pageable pageable) {
        return filmsUseCase.getFilms(showing, pageable);
    }

    // Search films by (part of) name: /films/search?name=avengers
    @GetMapping("/search")
    public Page<Films> searchFilms(@RequestParam String name, @SortDefault(sort = "filmName") Pageable pageable) {
        return filmsUseCase.searchFilmsByName(name, pageable);
    }

    @GetMapping("/{filmId}")
    public Films getFilm(@PathVariable Integer filmId) {
        return filmsUseCase.getFilmByFilmId(filmId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Films addFilm(@Valid @RequestBody FilmRequest request) {
        return filmsUseCase.addFilm(request);
    }

    @PutMapping("/{filmId}")
    public Films updateFilm(@PathVariable Integer filmId, @Valid @RequestBody FilmRequest request) {
        return filmsUseCase.updateFilm(filmId, request);
    }

    @DeleteMapping("/{filmId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFilm(@PathVariable Integer filmId) {
        filmsUseCase.deleteFilm(filmId);
    }
}
