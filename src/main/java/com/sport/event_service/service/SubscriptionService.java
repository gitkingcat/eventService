package com.sport.event_service.service;

import com.sport.event_service.model.SportEvent;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class SubscriptionService {

    private final Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();

    public Flux<ServerSentEvent<String>> subscribe() {
        return sink.asFlux()
                .map(data -> ServerSentEvent.<String>builder()
                        .event("sport-event-update")
                        .data(data)
                        .build());
    }


    public void notifySubscribers(SportEvent event) {
        sink.asFlux()
                .map(data -> ServerSentEvent.<String>builder()
                        .event("sport-event-update")
                        .data(String.valueOf(event))
                        .build());
    }
}
