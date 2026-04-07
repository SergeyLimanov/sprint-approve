package org.example.sprint.repository;

import org.example.sprint.entity.Sprint;
import org.example.sprint.entity.SprintStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, Long> {
    List<Sprint> findByTeamId(Long teamId);
    List<Sprint> findByStatus(SprintStatus status);
    List<Sprint> findByTeamIdAndStatus(Long teamId, SprintStatus status);
}
