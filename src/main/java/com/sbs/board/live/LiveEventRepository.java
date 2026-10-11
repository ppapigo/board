package com.sbs.board.live;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LiveEventRepository extends JpaRepository<LiveEvent, Long> {

    @EntityGraph(attributePaths = "host")
    Optional<LiveEvent> findByCode(String code);

    boolean existsByCode(String code);

    @EntityGraph(attributePaths = "host")
    List<LiveEvent> findByHostIdOrderByIdDesc(Long hostId);

    @Query("select e.host.id from LiveEvent e where e.code =:code")
    Optional<Long> findHostIdByCode(@Param("code") String code);
}
