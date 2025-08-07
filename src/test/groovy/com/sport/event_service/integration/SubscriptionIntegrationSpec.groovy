package com.sport.event_service.integration

import com.sport.event_service.model.EventStatus
import com.sport.event_service.model.SportEvent
import com.sport.event_service.model.SportType
import com.sport.event_service.repository.EventRepository
import com.sport.event_service.service.SportEventService
import com.sport.event_service.service.SubscriptionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.codec.ServerSentEvent
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import spock.lang.Specification

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class SubscriptionIntegrationSpec extends Specification {

    @Autowired
    SubscriptionService subscriptionService

    @Autowired
    SportEventService sportEventService

    @Autowired
    EventRepository eventRepository

    void setup() {
        eventRepository.deleteAll()
    }

    void "should receive event notification when subscribing and updating event"() {
        given:
        Date futureDate = Date.from(Instant.now().plus(1, ChronoUnit.HOURS))
        SportEvent event = SportEvent.builder()
                .id(1L)
                .sportType(SportType.FOOTBALL)
                .eventStatus(EventStatus.INACTIVE)
                .startDate(futureDate)
                .build()
        SportEvent savedEvent = eventRepository.save(event)

        Flux<ServerSentEvent<String>> subscription = subscriptionService.subscribe()
        List<ServerSentEvent<String>> receivedEvents = []
        CountDownLatch latch = new CountDownLatch(1)

        subscription.subscribe(
                { receivedEvent ->
                    receivedEvents.add(receivedEvent)
                    latch.countDown()
                },
                { error -> latch.countDown() },
                { latch.countDown() }
        )

        when:
        sportEventService.tryToUpdateEvent(savedEvent.id, EventStatus.ACTIVE)
        boolean eventReceived = latch.await(3, TimeUnit.SECONDS)

        then:
        eventReceived
        receivedEvents.size() == 1
        receivedEvents[0].event() == "sport-event-update"
        receivedEvents[0].data().contains("ACTIVE")
        receivedEvents[0].data().contains("FOOTBALL")
    }

    void "should handle multiple subscribers receiving same event"() {
        given:
        Date futureDate = Date.from(Instant.now().plus(1, ChronoUnit.HOURS))
        SportEvent event = SportEvent.builder()
                .id(1L)
                .sportType(SportType.BASKETBALL)
                .eventStatus(EventStatus.INACTIVE)
                .startDate(futureDate)
                .build()
        SportEvent savedEvent = eventRepository.save(event)

        Flux<ServerSentEvent<String>> subscription1 = subscriptionService.subscribe()
        Flux<ServerSentEvent<String>> subscription2 = subscriptionService.subscribe()

        List<ServerSentEvent<String>> receivedEvents1 = []
        List<ServerSentEvent<String>> receivedEvents2 = []
        CountDownLatch latch1 = new CountDownLatch(1)
        CountDownLatch latch2 = new CountDownLatch(1)

        subscription1.subscribe(
                { receivedEvent ->
                    receivedEvents1.add(receivedEvent)
                    latch1.countDown()
                },
                { error -> latch1.countDown() },
                { latch1.countDown() }
        )

        subscription2.subscribe(
                { receivedEvent ->
                    receivedEvents2.add(receivedEvent)
                    latch2.countDown()
                },
                { error -> latch2.countDown() },
                { latch2.countDown() }
        )

        when:
        sportEventService.tryToUpdateEvent(savedEvent.id, EventStatus.ACTIVE)
        boolean event1Received = latch1.await(3, TimeUnit.SECONDS)
        boolean event2Received = latch2.await(3, TimeUnit.SECONDS)

        then:
        event1Received
        event2Received
        receivedEvents1.size() == 1
        receivedEvents2.size() == 1
        receivedEvents1[0].event() == "sport-event-update"
        receivedEvents2[0].event() == "sport-event-update"
        receivedEvents1[0].data().contains("BASKETBALL")
        receivedEvents2[0].data().contains("BASKETBALL")
    }

    void "should receive multiple notifications for sequential updates"() {
        given:
        Date futureDate = Date.from(Instant.now().plus(1, ChronoUnit.HOURS))
        SportEvent event = SportEvent.builder()
                .id(1L)
                .sportType(SportType.TENNIS)
                .eventStatus(EventStatus.INACTIVE)
                .startDate(futureDate)
                .build()
        SportEvent savedEvent = eventRepository.save(event)

        Flux<ServerSentEvent<String>> subscription = subscriptionService.subscribe()
        List<ServerSentEvent<String>> receivedEvents = []
        CountDownLatch latch = new CountDownLatch(2)

        subscription.subscribe(
                { receivedEvent ->
                    receivedEvents.add(receivedEvent)
                    latch.countDown()
                },
                { error -> latch.countDown() },
                { latch.countDown() }
        )

        when:
        sportEventService.tryToUpdateEvent(savedEvent.id, EventStatus.ACTIVE)
        Thread.sleep(100)
        sportEventService.tryToUpdateEvent(savedEvent.id, EventStatus.FINISHED)
        boolean allEventsReceived = latch.await(5, TimeUnit.SECONDS)

        then:
        allEventsReceived
        receivedEvents.size() == 2
        receivedEvents[0].event() == "sport-event-update"
        receivedEvents[1].event() == "sport-event-update"
        receivedEvents[0].data().contains("ACTIVE")
        receivedEvents[1].data().contains("FINISHED")
    }

    void "should not receive events before subscription is established"() {
        given:
        Date futureDate = Date.from(Instant.now().plus(1, ChronoUnit.HOURS))
        SportEvent event = SportEvent.builder()
                .id(1L)
                .sportType(SportType.HOCKEY)
                .eventStatus(EventStatus.INACTIVE)
                .startDate(futureDate)
                .build()
        SportEvent savedEvent = eventRepository.save(event)

        when:
        sportEventService.tryToUpdateEvent(savedEvent.id, EventStatus.ACTIVE)

        Flux<ServerSentEvent<String>> subscription = subscriptionService.subscribe()
        List<ServerSentEvent<String>> receivedEvents = []
        CountDownLatch latch = new CountDownLatch(1)

        subscription.subscribe(
                { receivedEvent ->
                    receivedEvents.add(receivedEvent)
                    latch.countDown()
                },
                { error -> latch.countDown() },
                { latch.countDown() }
        )

        boolean eventReceived = latch.await(1, TimeUnit.SECONDS)

        then:
        !eventReceived
        receivedEvents.size() == 0
    }
}