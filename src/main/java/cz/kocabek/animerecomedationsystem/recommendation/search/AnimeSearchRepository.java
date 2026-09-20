package cz.kocabek.animerecomedationsystem.recommendation.search;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

import cz.kocabek.animerecomedationsystem.recommendation.dto.AnimeSuggestionDto;
import cz.kocabek.animerecomedationsystem.recommendation.entity.Anime;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.Predicate;

/**
 * Finds anime whose name or English name contains the given words. Matching is case and accent
 * insensitive because of the column collation. The words come from {@link SearchText} and
 * therefore contain only letters and digits (no LIKE wildcards).
 */
@Repository
public class AnimeSearchRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * @param words    normalised words, see {@link SearchText#lookupTokens(String)}
     * @param allWords true = every word must be present, false = at least one word
     * @param limit    maximal number of rows, the most popular anime first
     */
    public List<AnimeSuggestionDto> findCandidates(List<String> words, boolean allWords, int limit) {
        if (words.isEmpty()) {
            return List.of();
        }
        final var cb = entityManager.getCriteriaBuilder();
        final var query = cb.createQuery(AnimeSuggestionDto.class);
        final var anime = query.from(Anime.class);

        final List<Predicate> perWord = new ArrayList<>();
        for (final String word : words) {
            final var pattern = "%" + word + "%";
            perWord.add(cb.or(
                    cb.like(anime.<String>get("name"), pattern),
                    cb.like(anime.<String>get("englishName"), pattern)));
        }
        final var where = allWords
                ? cb.and(perWord.toArray(Predicate[]::new))
                : cb.or(perWord.toArray(Predicate[]::new));

        query.select(cb.construct(AnimeSuggestionDto.class,
                        anime.get("id"), anime.get("name"), anime.get("englishName"),
                        anime.get("type"), anime.get("score"), anime.get("popularity")))
                .where(where)
                .orderBy(cb.asc(cb.coalesce(anime.<Integer>get("popularity"), Integer.MAX_VALUE)),
                        cb.asc(anime.get("id")));
        return entityManager.createQuery(query).setMaxResults(limit).getResultList();
    }
}
