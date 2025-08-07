
package com.sport.event_service.integration

import com.sport.event_service.model.EventStatus
import com.sport.event_service.model.SportEvent
import com.sport.event_service.model.SportEventDto
import com.sport.event_service.model.SportType
import com.sport.event_service.repository.EventRepository
import com.sport.event_service.service.SportEventService
import com.sport.event_service.service.SubscriptionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification

import java.time.Instant
import java.time.temporal.ChronoUnit

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class SportEventServiceIntegrationSpec extends Specification {

    @Autowired
    SportEventService sportEventService

    @Autowired
    EventRepository eventRepository

    @Autowired
    SubscriptionService subscriptionService

    void setup() {
        eventRepository.deleteAll()
    }

    void "should create event and return generated id"() {
        given:
        Date futureDate = Date.from(Instant.now().plus(2, ChronoUnit.HOURS))
        SportEventDto dto = new SportEventDto(SportType.FOOTBALL, EventStatus.INACTIVE, futureDate)

        when:
        Long eventId = sportEventService.createEvent(dto)

        then:
        eventId != null
        eventId > 0

        and:
        SportEvent savedEvent = eventRepository.findById(eventId).orElse(null)
        savedEvent != null
        savedEvent.sportType == SportType.FOOTBALL
        savedEvent.eventStatus == EventStatus.INACTIVE
        savedEvent.startDate == futureDate
    }

    void "should get all events when no filters provided"() {
        given:
        List<SportEvent> events = [
                createTestEvent(1L, SportType.BASKETBALL, EventStatus.ACTIVE),
                createTestEvent(2L, SportType.TENNIS, EventStatus.INACTIVE),
                createTestEvent(3L, SportType.FOOTBALL, EventStatus.FINISHED)
        ]
        eventRepository.saveAll(events)

        when:
        List<SportEvent> result = sportEventService.getAllEvents(null, null)

        then:
        result.size() == 3
        result*.sportType.containsAll([SportType.BASKETBALL, SportType.TENNIS, SportType.FOOTBALL])
    }

    void "should filter events by status only"() {
        given:
        List<SportEvent> events = [
                createTestEvent(1L, SportType.BASKETBALL, EventStatus.ACTIVE),
                createTestEvent(2L, SportType.TENNIS, EventStatus.ACTIVE),
                createTestEvent(3L, SportType.FOOTBALL, EventStatus.FINISHED)
        ]
        eventRepository.saveAll(events)

        when:
        List<SportEvent> result = sportEventService.getAllEvents(EventStatus.ACTIVE, null)

        then:
        result.size() == 2
        result.every { it.eventStatus == EventStatus.ACTIVE }
    }

    void "should filter events by sport type only"() {
        given:
        List<SportEvent> events = [
                createTestEvent(1L, SportType.BASKETBALL, EventStatus.ACTIVE),
                createTestEvent(2L, SportType.BASKETBALL, EventStatus.FINISHED),
                createTestEvent(3L, SportType.FOOTBALL, EventStatus.ACTIVE)
        ]
        eventRepository.saveAll(events)

        when:
        List<SportEvent> result = sportEventService.getAllEvents(null, SportType.BASKETBALL)

        then:
        result.size() == 2
        result.every { it.sportType == SportType.BASKETBALL }
    }

    void "should filter events by both status and sport type"() {
        given:
        List<SportEvent> events = [
                createTestEvent(1L, SportType.BASKETBALL, EventStatus.ACTIVE),
                createTestEvent(2L, SportType.BASKETBALL, EventStatus.FINISHED),
                createTestEvent(3L, SportType.FOOTBALL, EventStatus.ACTIVE),
                createTestEvent(4L, SportType.FOOTBALL, EventStatus.FINISHED)
        ]
        eventRepository.saveAll(events)

        when:
        List<SportEvent> result = sportEventService.getAllEvents(EventStatus.ACTIVE, SportType.BASKETBALL)

        then:
        result.size() == 1
        result[0].sportType == SportType.BASKETBALL
        result[0].eventStatus == EventStatus.ACTIVE
    }

    void "should get event by id successfully"() {
        given:
        SportEvent event = createTestEvent(1L, SportType.HOCKEY, EventStatus.INACTIVE)
        SportEvent savedEvent = eventRepository.save(event)

        when:
        SportEvent result = sportEventService.getEventById(savedEvent.id)

        then:
        result.id == savedEvent.id
        result.sportType == SportType.HOCKEY
        result.eventStatus == EventStatus.INACTIVE
    }

    void "should throw exception when event not found by id"() {
        given:

        when:
        sportEventService.getEventById(999L)

        then:
        RuntimeException exception = thrown(RuntimeException)
        exception.message == "Event not found"
    }

    void "should successfully update event status from INACTIVE to ACTIVE"() {
        given:
        Date futureDate = Date.from(Instant.now().plus(1, ChronoUnit.HOURS))
        SportEvent event = createTestEvent(1L, SportType.TENNIS, EventStatus.INACTIVE)
        event.startDate = futureDate
        SportEvent savedEvent = eventRepository.save(event)

        when:
        SportEvent updatedEvent = sportEventService.tryToUpdateEvent(savedEvent.id, EventStatus.ACTIVE)

        then:
        updatedEvent.eventStatus == EventStatus.ACTIVE

        and:
        SportEvent persistedEvent = eventRepository.findById(savedEvent.id).get()
        persistedEvent.eventStatus == EventStatus.ACTIVE
    }

    void "should successfully update event status from ACTIVE to FINISHED"() {
        given:
        SportEvent event = createTestEvent(1L, SportType.CRICKET, EventStatus.ACTIVE)
        SportEvent savedEvent = eventRepository.save(event)

        when:
        SportEvent updatedEvent = sportEventService.tryToUpdateEvent(savedEvent.id, EventStatus.FINISHED)

        then:
        updatedEvent.eventStatus == EventStatus.FINISHED

        and:
        SportEvent persistedEvent = eventRepository.findById(savedEvent.id).get()
        persistedEvent.eventStatus == EventStatus.FINISHED
    }

    void "should reject invalid status transition from INACTIVE to ACTIVE with past start date"() {
        given:
        Date pastDate = Date.from(Instant.now().minus(1, ChronoUnit.HOURS))
        SportEvent event = createTestEvent(1L, SportType.VOLLEYBALL, EventStatus.INACTIVE)
        event.startDate = pastDate
        SportEvent savedEvent = eventRepository.save(event)

        when:
        sportEventService.tryToUpdateEvent(savedEvent.id, EventStatus.ACTIVE)

        then:
        IllegalStateException exception = thrown(IllegalStateException)
        exception.message.contains("INACTIVE")
        exception.message.contains("ACTIVE")
    }

    void "should reject invalid status transition from FINISHED to ACTIVE"() {
        given:
        SportEvent event = createTestEvent(1L, SportType.BASEBALL, EventStatus.FINISHED)
        SportEvent savedEvent = eventRepository.save(event)

        when:
        sportEventService.tryToUpdateEvent(savedEvent.id, EventStatus.ACTIVE)

        then:
        IllegalStateException exception = thrown(IllegalStateException)
        exception.message.contains("FINISHED")
        exception.message.contains("ACTIVE")
    }

    void "should reject direct transition from INACTIVE to FINISHED"() {
        given:
        SportEvent event = createTestEvent(1L, SportType.GOLF, EventStatus.INACTIVE)
        SportEvent savedEvent = eventRepository.save(event)

        when:
        sportEventService.tryToUpdateEvent(savedEvent.id, EventStatus.FINISHED)

        then:
        IllegalStateException exception = thrown(IllegalStateException)
        exception.message.contains("INACTIVE")
        exception.message.contains("FINISHED")
    }

    void "should handle all sport types correctly"() {
        given:
        SportType[] allSportTypes = SportType.values()
        List<SportEvent> events = allSportTypes.withIndex().collect { sportType, index ->
            createTestEvent((index + 1) as Long, sportType, EventStatus.INACTIVE)
        }
        eventRepository.saveAll(events)

        when:
        Map<SportType, List<SportEvent>> results = allSportTypes.collectEntries { sportType ->
            [sportType, sportEventService.getAllEvents(null, sportType)]
        }

        then:
        results.each { sportType, eventList ->
            assert eventList.size() == 1
            assert eventList[0].sportType == sportType
        }
    }

    def "should throw exception when trying to update non-existent event"() {
        given:

        when:
        sportEventService.tryToUpdateEvent(999L, EventStatus.ACTIVE)

        then:
        RuntimeException exception = thrown(RuntimeException)
        exception.message == "Event not found"
    }

    private SportEvent createTestEvent(Long id, SportType sportType, EventStatus eventStatus) {
        return SportEvent.builder()
                .id(id)
                .sportType(sportType)
                .eventStatus(eventStatus)
                .startDate(new Date())
                .build()
    }
}