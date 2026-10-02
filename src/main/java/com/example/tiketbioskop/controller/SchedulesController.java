package com.example.tiketbioskop.controller;

import com.example.tiketbioskop.entity.Schedules;
import com.example.tiketbioskop.usecase.schedule.ScheduleRequest;
import com.example.tiketbioskop.usecase.schedule.SchedulesUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/schedules")
@RequiredArgsConstructor
public class SchedulesController {
    private final SchedulesUseCase schedulesUseCase;

    // Optional filters: /schedules?filmId=1&date=2026-10-01&page=0&size=20
    @GetMapping
    public Page<Schedules> getSchedules(@RequestParam(required = false) Integer filmId,
                                        @RequestParam(required = false) LocalDate date,
                                        @SortDefault(sort = {"filmDate", "filmStartTime"}) Pageable pageable) {
        return schedulesUseCase.getSchedules(filmId, date, pageable);
    }

    @GetMapping("/{scheduleId}")
    public Schedules getSchedule(@PathVariable Integer scheduleId) {
        return schedulesUseCase.getSchedulesByScheduleId(scheduleId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Schedules addSchedule(@Valid @RequestBody ScheduleRequest request) {
        return schedulesUseCase.addSchedule(request);
    }

    @PutMapping("/{scheduleId}")
    public Schedules updateSchedule(@PathVariable Integer scheduleId, @Valid @RequestBody ScheduleRequest request) {
        return schedulesUseCase.updateSchedule(scheduleId, request);
    }

    @DeleteMapping("/{scheduleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSchedule(@PathVariable Integer scheduleId) {
        schedulesUseCase.deleteSchedule(scheduleId);
    }
}
