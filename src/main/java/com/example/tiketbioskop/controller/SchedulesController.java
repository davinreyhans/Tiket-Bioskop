package com.example.tiketbioskop.controller;

import com.example.tiketbioskop.service.SchedulesService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/schedules")
public class SchedulesController {
    private final SchedulesService schedulesService;

    public SchedulesController(SchedulesService schedulesService) {
        this.schedulesService = schedulesService;
    }

    //Add Schedule
    //Add Schedule
    @PostMapping("/add-schedule")
    public ResponseEntity<Map<String, Object>> addSchedule(@RequestBody Map<String, Object> schedules){
        schedulesService.addSchedule(
                schedules.get("filmId").toString(),
                schedules.get("filmDate").toString(),
                schedules.get("filmStartTime").toString(),
                schedules.get("filmEndTime").toString(),
                schedules.get("ticketPrice").toString());

        Map<String, Object> respBody = new HashMap<>();
        respBody.put("Tanggal Tayang", schedules.get("filmDate"));
        respBody.put("Jam Mulai", schedules.get("filmStartTime"));
        respBody.put("Jam Selesai", schedules.get("filmEndTime"));
        respBody.put("Harga Tiket", schedules.get("ticketPrice"));
        return ResponseEntity.status(HttpStatus.CREATED).body(respBody);
    }
}
