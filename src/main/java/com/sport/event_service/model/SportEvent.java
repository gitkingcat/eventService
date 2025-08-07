package com.sport.event_service.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SportEvent {

    @Id
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    private SportType sportType;

    @Enumerated(EnumType.STRING)
    private EventStatus eventStatus;

    @Column(name = "startDate")
    private Date startDate;


}
