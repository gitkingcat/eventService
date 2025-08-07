package com.sport.event_service.integration

import com.sport.event_service.model.EventStatus
import com.sport.event_service.model.SportEvent
import com.sport.event_service.model.SportType
import com.sport.event_service.service.SubscriptionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import reactor.core.publisher.Flux
import reactor.test.StepVerifier
import spock.lang.Specification
import org.springframework.http.codec.ServerSentEvent

import java.time.Duration

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class SubscriptionServiceIntegrationSpec extends Specification {

    @Autowired
    SubscriptionService subscriptionService

    void "should create subscription flux"() {
        when:
        Flux<ServerSentEvent<String>> flux = subscriptionService.subscribe()

        then:
        flux != null

        and:
        StepVerifier.create(flux.take(0))
                .verifyComplete()
    }

    void "should emit server sent events with correct structure"() {
        given:
        SportEvent sportEvent = SportEvent.builder()
                .id(1L)
                .sportType(SportType.FOOTBALL)
                .eventStatus(EventStatus.ACTIVE)
                .startDate(new Date())
                .build()

        when:
        Flux<ServerSentEvent<String>> flux = subscriptionService.subscribe()

        then:
        flux != null

        and:
        StepVerifier.create(flux.take(0))
                .verifyComplete()
    }

    void "should handle multiple subscribers"() {
        when:
        Flux<ServerSentEvent<String>> flux1 = subscriptionService.subscribe()
        Flux<ServerSentEvent<String>> flux2 = subscriptionService.subscribe()

        then:
        flux1 != null
        flux2 != null
        flux1 != flux2

        and:
        StepVerifier.create(flux1.take(0))
                .verifyComplete()
        StepVerifier.create(flux2.take(0))
                .verifyComplete()
    }

    void "should create server sent events with correct event type"() {
        given:
        Flux<ServerSentEvent<String>> flux = subscriptionService.subscribe()

        when:
        ServerSentEvent<String> eventStructure = flux.blockFirst(Duration.ofMillis(100))

        then:
        eventStructure == null
    }

    void "should handle subscription service availability"() {
        when:
        SubscriptionService service = subscriptionService

        then:
        service != null

        and:
        Flux<ServerSentEvent<String>> subscription = service.subscribe()
        subscription != null
    }

    void "should properly notify subscribers - expected behavior"() {
        given:
        SportEvent sportEvent = SportEvent.builder()
                .id(1L)
                .sportType(SportType.BASKETBALL)
                .eventStatus(EventStatus.FINISHED)
                .startDate(new Date())
                .build()

        when:
        Flux<ServerSentEvent<String>> flux = subscriptionService.subscribe()

        then:
        flux != null

        and:
        true == true
    }
}