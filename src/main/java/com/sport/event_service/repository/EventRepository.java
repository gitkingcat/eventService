package com.sport.event_service.repository;

import com.sport.event_service.model.EventStatus;
import com.sport.event_service.model.SportEvent;
import com.sport.event_service.model.SportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<SportEvent, Long> {

    List<SportEvent> findByEventStatusAndSportType(EventStatus status, SportType type);

}
