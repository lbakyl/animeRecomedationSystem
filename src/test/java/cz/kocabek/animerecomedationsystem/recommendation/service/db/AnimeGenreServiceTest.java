package cz.kocabek.animerecomedationsystem.recommendation.service.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import cz.kocabek.animerecomedationsystem.recommendation.repository.AnimeGenreRepository;
import cz.kocabek.animerecomedationsystem.recommendation.repository.GenreRepository;

class AnimeGenreServiceTest {

    @Test
    void allGenreNamesComeFromTheRepository() {
        final var genreRepository = mock(GenreRepository.class);
        when(genreRepository.findAllGenreNames()).thenReturn(List.of("Action", "Comedy", "Ecchi"));
        final var service = new AnimeGenreService(mock(AnimeGenreRepository.class), genreRepository);

        assertThat(service.getAllGenreNames()).containsExactly("Action", "Comedy", "Ecchi");
    }
}
