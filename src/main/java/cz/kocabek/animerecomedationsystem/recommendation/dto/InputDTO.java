package cz.kocabek.animerecomedationsystem.recommendation.dto;

import jakarta.validation.constraints.NotEmpty;

/**
 * Search form data.
 *
 * @param animeId id of the anime picked from the suggestions, {@code null} when the title was just typed
 */
public record InputDTO(@NotEmpty(message = "enter at least one character")
                       String animeName,
                       int minRating,
                       int maxUsers,
                       boolean onlyInAnimeGenres,
                       Long animeId) {
}
