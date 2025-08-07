package com.sport.event_service.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.sport.event_service.model.EventStatus
import com.sport.event_service.model.SportEvent
import com.sport.event_service.model.SportEventDto
import com.sport.event_service.model.SportType
import com.sport.event_service.repository.EventRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification

import java.time.Instant
import java.time.temporal.ChronoUnit

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class EventControllerIntegrationSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    EventRepository eventRepository

    @Autowired
    ObjectMapper objectMapper

    def setup() {
        eventRepository.deleteAll()
    }

    def "should create a new sport event"() {
        given: "a valid sport event dto"
        def futureDate = Date.from(Instant.now().plus(1, ChronoUnit.DAYS))
        def sportEventDto = new SportEventDto(SportType.FOOTBALL, EventStatus.INACTIVE, futureDate)
        def jsonContent = objectMapper.writeValueAsString(sportEventDto)

        when: "creating a new event"
        def result = mockMvc.perform(post("/events/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent))

        then: "event is created successfully"
        result.andExpect(status().isOk())
              .andExpect(content().string({ it.toLong() > 0 }))

        and: "event is persisted in database"
        eventRepository.count() == 1
        def savedEvent = eventRepository.findAll().get(0)
        savedEvent.sportType == SportType.FOOTBALL
        savedEvent.eventStatus == EventStatus.INACTIVE
    }

    def "should retrieve all events without filters"() {
        given:
        SportEvent event1 = SportEvent.builder()
                .id(1L)
                .sportType(SportType.FOOTBALL)
                .eventStatus(EventStatus.ACTIVE)
                .startDate(new Date())
                .build()
        SportEvent event2 = SportEvent.builder()
                .id(2L)
                .sportType(SportType.BASKETBALL)
                .eventStatus(EventStatus.INACTIVE)
                .startDate(new Date())
                .build()
        
        eventRepository.saveAll([event1, event2])

        when:
        def result = mockMvc.perform(get("/events"))

        then:
        result.andExpect(status().isOk())
              .andExpect(jsonPath('$.length()').value(2))
              .andExpect(jsonPath('$[0].sportType').value('FOOTBALL'))
              .andExpect(jsonPath('$[1].sportType').value('BASKETBALL'))
    }

    def "should retrieve events filtered by status and sport type"() {
        given:
        SportEvent activeFootballEvent = SportEvent.builder()
                .id(1L)
                .sportType(SportType.FOOTBALL)
                .eventStatus(EventStatus.ACTIVE)
                .startDate(new Date())
                .build()
        SportEvent inactiveFootballEvent = SportEvent.builder()
                .id(2L)
                .sportType(SportType.FOOTBALL)
                .eventStatus(EventStatus.INACTIVE)
                .startDate(new Date())
                .build()
        SportEvent activeBasketballEvent = SportEvent.builder()
                .id(3L)
                .sportType(SportType.BASKETBALL)
                .eventStatus(EventStatus.ACTIVE)
                .startDate(new Date())
                .build()
        
        eventRepository.saveAll([activeFootballEvent, inactiveFootballEvent, activeBasketballEvent])

        when:
        def result = mockMvc.perform(get("/events")
                .param("status", "ACTIVE")
                .param("sport", "FOOTBALL"))

        then:
        result.andExpect(status().isOk())
              .andExpect(jsonPath('$.length()').value(1))
              .andExpect(jsonPath('$[0].sportType').value('FOOTBALL'))
              .andExpect(jsonPath('$[0].eventStatus').value('ACTIVE'))
    }

    def "should retrieve specific event by id"() {
        given:
        SportEvent event = SportEvent.builder()
                .id(1L)
                .sportType(SportType.TENNIS)
                .eventStatus(EventStatus.FINISHED)
                .startDate(new Date())
                .build()
        SportEvent savedEvent = eventRepository.save(event)

        when:
        def result = mockMvc.perform(get("/events/{id}", savedEvent.id))

        then:
        result.andExpect(status().isOk())
              .andExpect(jsonPath('$.id').value(savedEvent.id))
              .andExpect(jsonPath('$.sportType').value('TENNIS'))
              .andExpect(jsonPath('$.eventStatus').value('FINISHED'))
    }

    def "should return 500 when event not found by id"() {
        given:

        when:
        def result = mockMvc.perform(get("/events/{id}", 999L))

        then:
        result.andExpect(status().isInternalServerError())
    }

    def "should successfully update event status from INACTIVE to ACTIVE"() {
        given:
        Date futureDate = Date.from(Instant.now().plus(1, ChronoUnit.DAYS))
        SportEvent event = SportEvent.builder()
                .id(1L)
                .sportType(SportType.HOCKEY)
                .eventStatus(EventStatus.INACTIVE)
                .startDate(futureDate)
                .build()
        SportEvent savedEvent = eventRepository.save(event)

        when:
        def result = mockMvc.perform(patch("/events/{id}/status", savedEvent.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content('"ACTIVE"'))

        then:
        result.andExpect(status().isOk())
              .andExpect(jsonPath('$.eventStatus').value('ACTIVE'))

        and:
        SportEvent updatedEvent = eventRepository.findById(savedEvent.id).get()
        updatedEvent.eventStatus == EventStatus.ACTIVE
    }

    def "should successfully update event status from ACTIVE to FINISHED"() {
        given: "an active event"
        def event = SportEvent.builder()
                .id(1L)
                .sportType(SportType.CRICKET)
                .eventStatus(EventStatus.ACTIVE)
                .startDate(new Date())
                .build()
        def savedEvent = eventRepository.save(event)

        when: "updating status to FINISHED"
        def result = mockMvc.perform(patch("/events/{id}/status", savedEvent.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content('"FINISHED"'))

        then: "status is updated successfully"
        result.andExpect(status().isOk())
              .andExpect(jsonPath('$.eventStatus').value('FINISHED'))
    }

    def "should reject invalid status transitions"() {
        given: "an inactive event with past start date"
        def pastDate = Date.from(Instant.now().minus(1, ChronoUnit.DAYS))
        def event = SportEvent.builder()
                .id(1L)
                .sportType(SportType.VOLLEYBALL)
                .eventStatus(EventStatus.INACTIVE)
                .startDate(pastDate)
                .build()
        def savedEvent = eventRepository.save(event)

        when: "trying to update to ACTIVE with past start date"
        def result = mockMvc.perform(patch("/events/{id}/status", savedEvent.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content('"ACTIVE"'))

        then: "request is rejected"
        result.andExpect(status().isInternalServerError())
    }

    def "should handle Server-Sent Events subscription endpoint"() {
        when: "accessing the subscription endpoint"
        def result = mockMvc.perform(get("/events/subscribe")
                .accept(MediaType.TEXT_EVENT_STREAM))

        then: "connection is established"
        result.andExpect(status().isOk())
              .andExpect(header().string("Content-Type", "text/event-stream;charset=UTF-8"))
    }

    def "should handle different sport types"() {
        given: "events for each sport type"
        def sportTypes = [
            SportType.FOOTBALL, SportType.HOCKEY, SportType.CRICKET, 
            SportType.BASKETBALL, SportType.TENNIS, SportType.FIELD_HOCKEY,
            SportType.VOLLEYBALL, SportType.TABLE_TENNIS, SportType.BASEBALL, SportType.GOLF
        ]
        
        def events = sportTypes.withIndex().collect { sportType, index ->
            SportEvent.builder()
                    .id((index + 1) as Long)
                    .sportType(sportType)
                    .eventStatus(EventStatus.INACTIVE)
                    .startDate(new Date())
                    .build()
        }
        
        eventRepository.saveAll(events)

        when: "requesting events for a specific sport"
        def result = mockMvc.perform(get("/events")
                .param("sport", "TENNIS")
                .param("status", "INACTIVE"))

        then: "only tennis events are returned"
        result.andExpect(status().isOk())
              .andExpected(jsonPath('$.length()').value(1))
              .andExpect(jsonPath('$[0].sportType').value('TENNIS'))
    }
}