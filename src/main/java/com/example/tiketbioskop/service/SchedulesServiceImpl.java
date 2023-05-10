package com.example.tiketbioskop.service;

import com.example.tiketbioskop.model.Films;
import com.example.tiketbioskop.model.Schedules;
import com.example.tiketbioskop.repository.SchedulesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SchedulesServiceImpl implements SchedulesService {
    private final SchedulesRepository schedulesRepository;

    private final FilmsService filmsService;

    public SchedulesServiceImpl(SchedulesRepository schedulesRepository, FilmsService filmsService) {
        this.schedulesRepository = schedulesRepository;
        this.filmsService = filmsService;
    }

    @Transactional
    @Override
    public void addSchedule(Object filmId, Object filmDate, Object filmStartTime, Object filmEndTime, Object ticketPrice) {
        Schedules schedules = new Schedules();
        schedules.setFilmDate(filmDate.toString());
        schedules.setFilmStartTime(filmStartTime.toString());
        schedules.setFilmEndTime(filmEndTime.toString());
        schedules.setTicketPrice(Integer.parseInt(ticketPrice.toString()));
        Films films = filmsService.getFilmByFilmId(Integer.parseInt(filmId.toString()));
        if(films != null) {
            schedules.setFilmId(films);
        } else {
            filmsService.addFilm(films);
            schedules.setFilmId(filmsService.getFilmByFilmId(Integer.parseInt(filmId.toString())));
        }
        schedulesRepository.save(schedules);
    }

    @Override
    public List<Schedules> getSchedules() {
        return schedulesRepository.findAll();
    }

    @Override
    public Schedules getSchedulesByScheduleId(Integer schedulesId) {
        return schedulesRepository.findSchedulesByScheduleId(schedulesId);
    }
}
