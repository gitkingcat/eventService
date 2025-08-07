package com.sport.event_service.controller;

import com.sport.event_service.model.EventStatus;
import com.sport.event_service.model.SportEvent;
import com.sport.event_service.model.SportEventDto;
import com.sport.event_service.model.SportType;
import com.sport.event_service.service.SportEventService;
import com.sport.event_service.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/events")
@AllArgsConstructor
public class EventController {

    private final SportEventService eventService;

    private final SubscriptionService subscriptionService;

    @PostMapping("/create")
    public ResponseEntity<Long> createEvent(@RequestBody SportEventDto event) {
        return ResponseEntity.ok(eventService.createEvent(event));
    }

    @GetMapping
    public ResponseEntity<List<SportEvent>> getAllEvents(
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false) SportType sport) {
        return ResponseEntity.ok(eventService.getAllEvents(status, sport));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SportEvent> getEventById(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<SportEvent> updateEventStatus(
            @PathVariable Long id,
            @RequestBody @NonNull EventStatus request) {
        return ResponseEntity.ok(eventService.tryToUpdateEvent(id, request));
    }

    @GetMapping(path = "/subscribe")
    public Flux<ServerSentEvent<String>> streamEvents() {
        return subscriptionService.subscribe();
    }

}
