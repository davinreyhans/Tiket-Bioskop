package com.example.tiketbioskop.business.schedule;

import com.example.tiketbioskop.business.film.FilmsService;
import com.example.tiketbioskop.entity.Films;
import com.example.tiketbioskop.entity.Schedules;
import com.example.tiketbioskop.repository.DaoSchedules;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SchedulesServiceImpl implements SchedulesService {

    @Autowired
    private DaoSchedules daoSchedules;

    @Autowired
    private FilmsService filmsService;

    @Transactional
    @Override
    public void addSchedule(Object filmId, Object filmDate, Object filmStartTime, Object filmEndTime,
            Object ticketPrice) {
        Schedules schedules = new Schedules();
        schedules.setFilmDate(filmDate.toString());
        schedules.setFilmStartTime(filmStartTime.toString());
        schedules.setFilmEndTime(filmEndTime.toString());
        schedules.setTicketPrice(Integer.parseInt(ticketPrice.toString()));
        Films films = filmsService.getFilmByFilmId(Integer.parseInt(filmId.toString()));
        if (films != null) {
            schedules.setFilmId(films);
        } else {
            filmsService.addFilm(films);
            schedules.setFilmId(filmsService.getFilmByFilmId(Integer.parseInt(filmId.toString())));
        }
        daoSchedules.save(schedules);
    }

    @Override
    public List<Schedules> getSchedules() {
        return daoSchedules.findAll();
    }

    @Override
    public Schedules getSchedulesByScheduleId(Integer schedulesId) {
        return daoSchedules.findSchedulesByScheduleId(schedulesId);
    }
}
