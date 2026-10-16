package com.sbs.board.live;

import com.sbs.board.auth.UserRepository;
import com.sbs.board.global.entity.User;
import com.sbs.board.global.exception.BusinessException;
import com.sbs.board.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class LiveStatePersistenceTest {
    @Autowired UserRepository userRepository;
    @Autowired LiveEventRepository eventRepository;
    @Autowired PollRepository pollRepository;
    @Autowired EntityManager entityManager;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void newEventIsOpenAfterSavingAndReloading(boolean useBuilder) {
        User host = host();
        LiveEvent event = useBuilder
                ? LiveEvent.builder().host(host).title("Live event").code("ABC123").build()
                : new LiveEvent(host, "Live event", "ABC123");

        Long id = eventRepository.saveAndFlush(event).getId();
        entityManager.clear();
        LiveEvent saved = eventRepository.findById(id).orElseThrow();

        assertThat(saved.getStatus()).isEqualTo(LiveStatus.OPEN);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThatCode(saved::assertOpen).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void newPollIsOpenAfterSavingAndReloading(boolean useBuilder) {
        LiveEvent event = eventRepository.save(new LiveEvent(host(), "Live event", "ABC123"));
        Poll poll;
        if (useBuilder) {
            poll = Poll.builder().event(event).title("Poll").build();
            poll.getOptions().add(new PollOption(poll, "First", 0));
            poll.getOptions().add(new PollOption(poll, "Second", 1));
        } else {
            poll = new Poll(event, "Poll", List.of("First", "Second"));
        }

        Long id = pollRepository.saveAndFlush(poll).getId();
        entityManager.clear();
        Poll saved = pollRepository.findById(id).orElseThrow();

        assertThat(saved.getStatus()).isEqualTo(LiveStatus.OPEN);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getOptions()).hasSize(2);
        assertThatCode(saved::assertOpen).doesNotThrowAnyException();
        assertThatCode(saved.getEvent()::assertOpen).doesNotThrowAnyException();
    }

    @Test
    void closedStatesSurviveReloadAndRejectParticipation() {
        LiveEvent event = eventRepository.save(new LiveEvent(host(), "Live event", "ABC123"));
        Poll poll = pollRepository.save(new Poll(event, "Poll", List.of("First", "Second")));
        event.close();
        poll.close();
        entityManager.flush();
        entityManager.clear();

        LiveEvent savedEvent = eventRepository.findById(event.getId()).orElseThrow();
        Poll savedPoll = pollRepository.findById(poll.getId()).orElseThrow();

        assertThat(savedEvent.getStatus()).isEqualTo(LiveStatus.CLOSED);
        assertThat(savedPoll.getStatus()).isEqualTo(LiveStatus.CLOSED);
        assertThatThrownBy(savedEvent::assertOpen).isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.LIVE_EVENT_CLOSED));
        assertThatThrownBy(savedPoll::assertOpen).isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.POLL_CLOSED));
    }

    private User host() {
        return userRepository.save(User.from("host@example.com", "Host", "unused-password", User.Role.USER));
    }
}
