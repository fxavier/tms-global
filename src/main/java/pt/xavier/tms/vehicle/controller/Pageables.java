package pt.xavier.tms.vehicle.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

final class Pageables {

    private static final int MIN_SIZE = 10;
    private static final int MAX_SIZE = 100;

    private Pageables() {
    }

    static Pageable of(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, MIN_SIZE), MAX_SIZE));
    }
}
