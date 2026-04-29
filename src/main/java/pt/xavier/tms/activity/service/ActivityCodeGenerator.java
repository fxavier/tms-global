package pt.xavier.tms.activity.service;

import java.time.Year;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.xavier.tms.activity.repository.ActivityRepository;

@Service
public class ActivityCodeGenerator {

    private final ActivityRepository activityRepository;

    public ActivityCodeGenerator(ActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }

    @Transactional
    public String generateActivityCode() {
        int year = Year.now().getValue();
        String prefix = "ACT-" + year + "-";
        long sequence = activityRepository.countByCodeStartingWith(prefix) + 1;
        return String.format("ACT-%d-%04d", year, sequence);
    }
}
