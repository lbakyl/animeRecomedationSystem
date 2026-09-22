package cz.kocabek.animerecomedationsystem.recommendation.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * An unchecked multi-select / checkbox group posts no request parameter at all, not an empty one, so the
 * binder passes {@code null} for genres/types/excludedContent; the record must not propagate that null.
 */
class InputDTOTest {

    @Test
    void missingMultiSelectValuesBecomeEmptyListsInsteadOfNull() {
        final var form = new InputDTO("Naruto", 8, 500, false, null, null, null, null);

        assertThat(form.genres()).isEmpty();
        assertThat(form.types()).isEmpty();
        assertThat(form.excludedContent()).isEmpty();
    }

    @Test
    void suppliedValuesArePreserved() {
        final var form = new InputDTO("Naruto", 8, 500, false, null,
                List.of("Action"), List.of("TV"), List.of("adult", "ecchi"));

        assertThat(form.genres()).containsExactly("Action");
        assertThat(form.types()).containsExactly("TV");
        assertThat(form.excludedContent()).containsExactly("adult", "ecchi");
    }
}
