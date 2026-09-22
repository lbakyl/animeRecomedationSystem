package cz.kocabek.animerecomedationsystem.recommendation.service;

import cz.kocabek.animerecomedationsystem.recommendation.dto.AnimeOutDTO;
import cz.kocabek.animerecomedationsystem.recommendation.service.db.AnimeGenreService;
import cz.kocabek.animerecomedationsystem.recommendation.service.recommendationconfig.ConfigConstant;
import cz.kocabek.animerecomedationsystem.recommendation.service.recommendationconfig.RecommendationConfig;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnimePreprocessingService {

    private static final String ECCHI_GENRE = "Ecchi";

    final AnimeGenreService animeGenreService;
    final RecommendationConfig config;


    public AnimePreprocessingService(AnimeGenreService animeGenreService, RecommendationConfig config) {
        this.animeGenreService = animeGenreService;
        this.config = config;
    }

    public Map<Long, AnimeOutDTO> filterAnimeMap(Map<Long, AnimeOutDTO> animeMap) {
        final List<AnimePredicate> selectedFilters = selectFilters();
        return animeMap.entrySet().stream().filter(combineFilters(selectedFilters))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (_, b) -> b));
    }

    private List<AnimePredicate> selectFilters() {
        final var filters = new ArrayList<AnimePredicate>();
        if (config.isOnlyInAnimeGenres()) {
            final var genres = animeGenreService.getGenresForAnime(config.getAnimeId());
            filters.add(entry -> entry.getValue().getGenres().stream().anyMatch(genres::contains));
        }
        if (!config.getGenres().isEmpty()) {
            final var wanted = config.getGenres();
            filters.add(entry -> entry.getValue().getGenres().stream().anyMatch(wanted::contains));
        }
        if (!config.getTypes().isEmpty()) {
            final var wanted = config.getTypes();
            // wanted may be an immutable List.of(...), whose contains(null) throws instead of returning false
            filters.add(entry -> entry.getValue().getType() != null && wanted.contains(entry.getValue().getType()));
        }
        final var excludedContent = config.getExcludedContent();
        if (excludedContent.contains(ConfigConstant.EXCLUDE_ADULT)) {
            // MyAnimeList's own rating scale has one 18+/hentai tier, "Rx - Hentai"
            filters.add(entry -> {
                final var rating = entry.getValue().getRating();
                return rating == null || !rating.startsWith("Rx");
            });
        }
        if (excludedContent.contains(ConfigConstant.EXCLUDE_ECCHI)) {
            filters.add(entry -> entry.getValue().getGenres().stream().noneMatch(ECCHI_GENRE::equalsIgnoreCase));
        }
        return filters;
    }

    private AnimePredicate combineFilters(List<AnimePredicate> activeFilterList) {
        return activeFilterList.stream().reduce(_ -> true,
                (f1, f2) -> entry -> f1.test(entry) && f2.test(entry),
                (f1, _) -> f1);
    }
}
