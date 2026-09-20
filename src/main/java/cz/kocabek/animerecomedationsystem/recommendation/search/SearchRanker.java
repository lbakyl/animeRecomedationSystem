package cz.kocabek.animerecomedationsystem.recommendation.search;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import cz.kocabek.animerecomedationsystem.recommendation.dto.AnimeSuggestionDto;

/**
 * Orders search candidates from the best to the weakest match. Pure logic, no database access.
 */
public final class SearchRanker {

    /** Normalised name equals the query. */
    public static final int NAME_EXACT = 0;
    /** Normalised English name equals the query. */
    public static final int ENGLISH_EXACT = 1;
    public static final int NAME_PREFIX = 2;
    public static final int ENGLISH_PREFIX = 3;
    /** The query starts at a word boundary inside a title. */
    public static final int WORD_START = 4;
    public static final int CONTAINS = 5;
    /** All query words are present, but not next to each other. */
    public static final int ALL_WORDS = 6;

    private static final Comparator<Ranked> ORDER = Comparator
            .comparingInt(Ranked::tier)
            .thenComparing(r -> r.anime().popularity(), Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(r -> r.anime().score(), Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(r -> r.anime().name(), Comparator.nullsLast(Comparator.naturalOrder()));

    private SearchRanker() {
    }

    public record Ranked(AnimeSuggestionDto anime, int tier) {
        /** Exact title match (Japanese or English name). */
        public boolean isExact() {
            return tier <= ENGLISH_EXACT;
        }
    }

    public static List<Ranked> rank(String normalizedQuery, Collection<AnimeSuggestionDto> candidates) {
        return candidates.stream()
                .map(c -> new Ranked(c, tierOf(normalizedQuery, c)))
                .sorted(ORDER)
                .toList();
    }

    static int tierOf(String query, AnimeSuggestionDto anime) {
        final var name = SearchText.normalize(anime.name());
        final var english = SearchText.normalize(anime.englishName());
        if (name.equals(query)) {
            return NAME_EXACT;
        }
        if (english.equals(query)) {
            return ENGLISH_EXACT;
        }
        if (name.startsWith(query)) {
            return NAME_PREFIX;
        }
        if (english.startsWith(query)) {
            return ENGLISH_PREFIX;
        }
        if (name.contains(" " + query) || english.contains(" " + query)) {
            return WORD_START;
        }
        if (name.contains(query) || english.contains(query)) {
            return CONTAINS;
        }
        return ALL_WORDS;
    }
}
