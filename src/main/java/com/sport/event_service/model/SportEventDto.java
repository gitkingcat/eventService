package com.sport.event_service.model;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

import java.util.Date;

@Data
@AllArgsConstructor
public class SportEventDto {

    @NonNull
    @Valid
    private SportType sportType;

    @NonNull
    private EventStatus eventStatus;

    @NonNull
    private Date startTime;
}
