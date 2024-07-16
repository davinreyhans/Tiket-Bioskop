package com.example.tiketbioskop.business.schedule;

import com.example.tiketbioskop.entity.Schedules;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface SchedulesService {
    void addSchedule(Object filmId, Object filmDate, Object filmStartTime, Object filmEndTime, Object ticketPrice);

    List<Schedules> getSchedules();

    Schedules getSchedulesByScheduleId(Integer schedulesId);

}
