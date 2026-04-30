package pt.xavier.tms.hr.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

final class Pageables {

    private static final int MIN_SIZE = 10;
    private static final int MAX_SIZE = 100;

    private Pageables() {
    }

    static Pageable of(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, MIN_SIZE), MAX_SIZE);
        return PageRequest.of(safePage, safeSize);
    }
}
