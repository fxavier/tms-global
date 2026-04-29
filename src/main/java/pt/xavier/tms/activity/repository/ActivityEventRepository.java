package pt.xavier.tms.activity.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import pt.xavier.tms.activity.entity.ActivityEvent;

public interface ActivityEventRepository extends JpaRepository<ActivityEvent, UUID> {

    List<ActivityEvent> findByActivityIdOrderByPerformedAtAsc(UUID activityId);
}
