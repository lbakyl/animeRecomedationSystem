package cz.kocabek.animerecomedationsystem.recommendation.dto;

/**
 * Projection of the columns the "Type" and "Content Filters" advanced options need, without loading
 * the whole {@link cz.kocabek.animerecomedationsystem.recommendation.entity.Anime} entity.
 */
public record AnimeTypeRatingInfo(Long id, String type, String rating) {
}
