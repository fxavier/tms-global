package pt.xavier.tms.activity.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import pt.xavier.tms.activity.entity.Activity;

public interface ActivityRepository extends JpaRepository<Activity, UUID> {

    long countByCodeStartingWith(String prefix);
}
