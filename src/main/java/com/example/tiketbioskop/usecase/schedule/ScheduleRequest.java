package com.example.tiketbioskop.usecase.schedule;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

public record ScheduleRequest(
        @NotNull Integer filmId,
        @NotNull Character studioName,
        @NotNull LocalDate filmDate,
        @NotNull LocalTime filmStartTime,
        @NotNull LocalTime filmEndTime,
        @NotNull @PositiveOrZero Integer ticketPrice) {

    // An end time before the start time means the show ends the next day (23:00 - 01:00).
    // The cap catches swapped times: 21:00 - 19:00 would otherwise be a 22-hour show.
    private static final Duration MAX_DURATION = Duration.ofHours(6);

    @AssertTrue(message = "show must last at most 6 hours; an end time before the start time means it ends the next day")
    public boolean isValidDuration() {
        if (filmStartTime == null || filmEndTime == null) {
            return true;
        }
        Duration duration = Duration.between(filmStartTime, filmEndTime);
        if (duration.isNegative() || duration.isZero()) {
            duration = duration.plusDays(1);
        }
        return duration.compareTo(MAX_DURATION) <= 0;
    }
}
