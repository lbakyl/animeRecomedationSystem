package cz.kocabek.animerecomedationsystem.recommendation.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

/**
 * Search form data.
 *
 * @param animeId id of the anime picked from the suggestions, {@code null} when the title was just typed
 * @param genres selected "Genres" advanced option; empty means no genre filter
 * @param types selected "Type" advanced option (TV, Movie, ...); empty means no type filter
 * @param excludedContent content tags to hide ("adult", "ecchi"); see {@code AnimePreprocessingService}
 */
public record InputDTO(@NotEmpty(message = "enter at least one character")
                       String animeName,
                       int minRating,
                       int maxUsers,
                       boolean onlyInAnimeGenres,
                       Long animeId,
                       List<String> genres,
                       List<String> types,
                       List<String> excludedContent) {

    public InputDTO {
        // an unchecked/empty multi-select posts no value at all, not an empty list
        genres = genres == null ? List.of() : genres;
        types = types == null ? List.of() : types;
        excludedContent = excludedContent == null ? List.of() : excludedContent;
    }
}
