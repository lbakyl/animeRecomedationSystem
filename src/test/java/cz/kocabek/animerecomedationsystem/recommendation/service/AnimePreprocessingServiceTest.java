package cz.kocabek.animerecomedationsystem.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cz.kocabek.animerecomedationsystem.recommendation.dto.AnimeOutDTO;
import cz.kocabek.animerecomedationsystem.recommendation.service.db.AnimeGenreService;
import cz.kocabek.animerecomedationsystem.recommendation.service.recommendationconfig.ConfigConstant;
import cz.kocabek.animerecomedationsystem.recommendation.service.recommendationconfig.RecommendationConfig;

/**
 * The "Genres", "Type" and "Content Filters" advanced options, applied in {@link AnimePreprocessingService}.
 */
class AnimePreprocessingServiceTest {

    private AnimeGenreService animeGenreService;
    private RecommendationConfig config;
    private AnimePreprocessingService service;

    @BeforeEach
    void setUp() {
        animeGenreService = mock(AnimeGenreService.class);
        config = new RecommendationConfig();
        config.setExcludedContent(List.of()); // the default "exclude adult" would interfere with most tests below
        service = new AnimePreprocessingService(animeGenreService, config);
    }

    private static AnimeOutDTO anime(List<String> genres, String type, String rating) {
        final var dto = new AnimeOutDTO(0, 0);
        dto.setGenres(genres);
        dto.setType(type);
        dto.setRating(rating);
        return dto;
    }

    private Map<Long, AnimeOutDTO> sampleMap() {
        final var map = new HashMap<Long, AnimeOutDTO>();
        map.put(1L, anime(List.of("Action", "Comedy"), "TV", "PG-13 - Teens 13 or older"));
        map.put(2L, anime(List.of("Romance", "Ecchi"), "OVA", "R+ - Mild Nudity"));
        map.put(3L, anime(List.of("Hentai"), "OVA", "Rx - Hentai"));
        map.put(4L, anime(List.of(), null, null));
        return map;
    }

    @Test
    void noFiltersReturnsEveryAnime() {
        assertThat(service.filterAnimeMap(sampleMap())).containsOnlyKeys(1L, 2L, 3L, 4L);
    }

    @Test
    void genreFilterKeepsOnlyAnimeWithASelectedGenre() {
        config.setGenres(List.of("Romance"));

        assertThat(service.filterAnimeMap(sampleMap())).containsOnlyKeys(2L);
    }

    @Test
    void typeFilterKeepsOnlyAnimeOfASelectedType() {
        config.setTypes(List.of("TV"));

        assertThat(service.filterAnimeMap(sampleMap())).containsOnlyKeys(1L);
    }

    @Test
    void excludeAdultRemovesOnlyTheRxRatingTier() {
        config.setExcludedContent(List.of(ConfigConstant.EXCLUDE_ADULT));

        // #3 is "Rx - Hentai"; #4 has no rating at all and is kept, not treated as adult
        assertThat(service.filterAnimeMap(sampleMap())).containsOnlyKeys(1L, 2L, 4L);
    }

    @Test
    void excludeEcchiRemovesAnimeTaggedWithTheEcchiGenre() {
        config.setExcludedContent(List.of(ConfigConstant.EXCLUDE_ECCHI));

        assertThat(service.filterAnimeMap(sampleMap())).containsOnlyKeys(1L, 3L, 4L);
    }

    @Test
    void excludeEcchiIsCaseInsensitive() {
        final var map = Map.of(1L, anime(List.of("ecchi"), "TV", null));
        config.setExcludedContent(List.of(ConfigConstant.EXCLUDE_ECCHI));

        assertThat(service.filterAnimeMap(map)).isEmpty();
    }

    @Test
    void onlyInAnimeGenresAndTheNewGenreFilterCombine() {
        config.setOnlyInAnimeGenres(true);
        config.setAnimeId(99L);
        config.setGenres(List.of("Comedy"));
        when(animeGenreService.getGenresForAnime(99L)).thenReturn(List.of("Action"));

        // #1 has both "Action" (input anime genre) and "Comedy" (selected genre); #2/#3 have neither
        assertThat(service.filterAnimeMap(sampleMap())).containsOnlyKeys(1L);
    }
}
