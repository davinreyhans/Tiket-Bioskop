package com.example.tiketbioskop.controller;

import com.example.tiketbioskop.model.Films;
import com.example.tiketbioskop.service.FilmsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/films")
public class FilmsController {
    private final FilmsService filmsService;

    public FilmsController(FilmsService filmsService) {
        this.filmsService = filmsService;
    }

    // Get All Films
    @GetMapping("/get-all-film")
    public ResponseEntity<List<Films>> getAllFilms() {
        List<Films> films = filmsService.getAllFilms();
        return new ResponseEntity<>(films, HttpStatus.OK);
    }

    // Get All Showing Film (true)
    @GetMapping("/get-all-showing-film")
    public ResponseEntity<List<Films>> getAllShowingFilm() {
        List<Films> films = filmsService.getAllShowingFilm();
        return new ResponseEntity<>(films, HttpStatus.OK);
    }

    // Get All Not Showing Film (false)
    @GetMapping("/get-all-not-showing-film")
    public ResponseEntity<List<Films>> getAllNotShowingFilm() {
        List<Films> films = filmsService.getAllNotShowingFilm();
        return new ResponseEntity<>(films, HttpStatus.OK);
    }

    // Get Film by Name
    @GetMapping("/get-film-by-name")
    public ResponseEntity<Map<String, Object>> getFilmByName(String filmName) {
        Films films = filmsService.getFilmByFilmName(filmName);
        Map<String, Object> respBody = new HashMap<>();
        respBody.put("ID Film", films.getFilmId());
        respBody.put("Film Code", films.getFilmCode());
        respBody.put("Film Name", films.getFilmName());
        respBody.put("Is Showing", films.getIsShowing());
        return new ResponseEntity<>(respBody, HttpStatus.FOUND);
    }

    // Add Film with Postman
    @PostMapping("/add-film")
    public ResponseEntity<Films> addFilm(@RequestBody Films films) {
        filmsService.addFilm(films);
        return new ResponseEntity<>(films, HttpStatus.CREATED);
    }

    // Update User
    @PutMapping("/update-film")
    public ResponseEntity<Films> updateUser(@RequestBody Films films) {
        filmsService.updateFilmById(films);
        return new ResponseEntity<>(films, HttpStatus.ACCEPTED);
    }

    // Delete User
    @DeleteMapping("/delete-film/{filmId}")
    public ResponseEntity<String> deleteUser(@PathVariable Integer filmId) {
        filmsService.deleteFilm(filmId);
        return new ResponseEntity<>("Successfully deleted film_id " + filmId + "!", HttpStatus.ACCEPTED);
    }
}
