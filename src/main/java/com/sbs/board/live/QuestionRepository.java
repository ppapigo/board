package com.sbs.board.live;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question ,Long> {

    @EntityGraph(attributePaths = "author")
    List<Question> findByEventId(Long eventId);

    @EntityGraph(attributePaths = "event")
    Optional<Question> findWithEventById(Long id);

    @Query("select new com.sbs.board.live.QuestionOwnership(q.author.id, q.event.host.id) "
            + "from Question q where q.id = :id")
    Optional<QuestionOwnership> findOwnershipById(@Param("id")Long id);


}
