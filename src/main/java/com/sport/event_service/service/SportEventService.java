package com.sport.event_service.service;

import com.sport.event_service.model.EventStatus;
import com.sport.event_service.model.SportEvent;
import com.sport.event_service.model.SportEventDto;
import com.sport.event_service.model.SportType;
import com.sport.event_service.repository.EventRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

import static com.sport.event_service.model.EventStatus.*;

@Service
@AllArgsConstructor
public class SportEventService {

    private final EventRepository eventRepository;

    private final SubscriptionService subscriptionService;

    public Long createEvent(SportEventDto event) {
        SportEvent sportEvent = SportEvent.builder()
                .eventStatus(event.getEventStatus())
                .sportType(event.getSportType())
                .startDate(event.getStartTime())
                .build();
        eventRepository.save(sportEvent);
        return sportEvent.getId();
    }

    public List<SportEvent> getAllEvents(EventStatus status, SportType sport) {
        return eventRepository.findByEventStatusAndSportType(status, sport);
    }

    public SportEvent getEventById(Long id) {
        return eventRepository.findById(id).orElseThrow(() -> new RuntimeException("Event not found"));
    }

    @Transactional
    public SportEvent tryToUpdateEvent(Long id, EventStatus newStatus) {
        SportEvent sportEvent = eventRepository.findById(id).orElseThrow(() -> new RuntimeException("Event not found"));
        changeEventStatus(sportEvent, newStatus);
        eventRepository.save(sportEvent);
        subscriptionService.notifySubscribers(sportEvent);
        return sportEvent;
    }

    private void changeEventStatus(SportEvent sportEvent, EventStatus newStatus) {
        EventStatus current = sportEvent.getEventStatus();
        if ((current == INACTIVE && newStatus == ACTIVE && sportEvent.getStartDate().after(new Date()))
                || (current == ACTIVE && newStatus == FINISHED)) {
            sportEvent.setEventStatus(newStatus);
            return;
        }
        throw new IllegalStateException(String.format("Status %s cannot be changed to %s", current, newStatus));
    }
}
