package com.example.tiketbioskop.usecase.schedule;

import com.example.tiketbioskop.exception.ConflictException;
import com.example.tiketbioskop.repository.DaoSeats;
import com.example.tiketbioskop.repository.DaoTickets;
import com.example.tiketbioskop.usecase.film.FilmsUseCase;
import com.example.tiketbioskop.entity.Schedules;
import com.example.tiketbioskop.exception.NotFoundException;
import com.example.tiketbioskop.repository.DaoSchedules;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class SchedulesUseCase {

    private final DaoSchedules daoSchedules;

    private final DaoSeats daoSeats;

    private final DaoTickets daoTickets;

    private final FilmsUseCase filmsUseCase;

    @Transactional
    public Schedules addSchedule(ScheduleRequest request) {
        Schedules schedules = new Schedules();
        apply(schedules, request);
        assertStudioFree(schedules);
        return daoSchedules.save(schedules);
    }

    @Transactional
    public Schedules updateSchedule(Integer scheduleId, ScheduleRequest request) {
        Schedules schedules = getSchedulesByScheduleId(scheduleId);
        // booked seats belong to the old studio, so moving studios would orphan them
        if (!schedules.getStudioName().equals(request.studioName()) && daoTickets.existsByScheduleScheduleId(scheduleId)) {
            throw new ConflictException("Schedule id '" + scheduleId + "' has booked tickets, its studio can't be changed.");
        }
        apply(schedules, request);
        assertStudioFree(schedules);
        return daoSchedules.save(schedules);
    }

    @Transactional
    public void deleteSchedule(Integer scheduleId) {
        Schedules schedules = getSchedulesByScheduleId(scheduleId);
        // never silently drop people's tickets: they are cancelled first, one by one
        if (daoTickets.existsByScheduleScheduleId(scheduleId)) {
            throw new ConflictException("Schedule id '" + scheduleId + "' has booked tickets, cancel them first.");
        }
        daoSchedules.delete(schedules);
    }

    public Page<Schedules> getSchedules(Integer filmId, LocalDate filmDate, Pageable pageable) {
        return daoSchedules.search(filmId, filmDate, pageable);
    }

    public Schedules getSchedulesByScheduleId(Integer schedulesId) {
        return daoSchedules.findById(schedulesId)
                .orElseThrow(() -> new NotFoundException("Schedule id '" + schedulesId + "' not found."));
    }

    // ponytail: app-level check, two admins saving at the same instant can still overlap;
    // a Postgres exclusion constraint (btree_gist + tsrange) closes that if it ever matters
    private void assertStudioFree(Schedules schedules) {
        // shows last at most 6 hours, so only the day before and after can reach into this one
        daoSchedules.findByStudioNameAndFilmDateBetween(schedules.getStudioName(),
                        schedules.getFilmDate().minusDays(1), schedules.getFilmDate().plusDays(1)).stream()
                .filter(other -> !other.getScheduleId().equals(schedules.getScheduleId()))
                .filter(other -> other.startsAt().isBefore(schedules.endsAt())
                        && schedules.startsAt().isBefore(other.endsAt()))
                .findFirst()
                .ifPresent(other -> {
                    throw new ConflictException("Studio " + schedules.getStudioName() + " is already used by schedule id '"
                            + other.getScheduleId() + "' (" + other.startsAt() + " to " + other.endsAt() + ").");
                });
    }

    private void apply(Schedules schedules, ScheduleRequest request) {
        if (!daoSeats.existsByStudioName(request.studioName())) {
            throw new NotFoundException("Studio '" + request.studioName() + "' not found.");
        }
        schedules.setFilm(filmsUseCase.getFilmByFilmId(request.filmId()));
        schedules.setStudioName(request.studioName());
        schedules.setFilmDate(request.filmDate());
        schedules.setFilmStartTime(request.filmStartTime());
        schedules.setFilmEndTime(request.filmEndTime());
        schedules.setTicketPrice(request.ticketPrice());
    }
}
