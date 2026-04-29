package pt.xavier.tms.shared.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class PagedResponseTests {

    @Test
    void mapsSpringPageMetadata() {
        PageImpl<String> page = new PageImpl<>(List.of("A", "B"), PageRequest.of(1, 2), 5);

        PagedResponse<String> response = PagedResponse.from(page);

        assertThat(response.content()).containsExactly("A", "B");
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(5);
        assertThat(response.totalPages()).isEqualTo(3);
    }
}
