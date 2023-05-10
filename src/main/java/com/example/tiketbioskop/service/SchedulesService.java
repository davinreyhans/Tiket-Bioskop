package com.example.tiketbioskop.service;

import com.example.tiketbioskop.model.Schedules;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface SchedulesService {
    void addSchedule(Object filmId, Object filmDate, Object filmStartTime, Object filmEndTime, Object ticketPrice);

    List<Schedules> getSchedules();

    Schedules getSchedulesByScheduleId(Integer schedulesId);

}
