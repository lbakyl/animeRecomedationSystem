package cz.kocabek.animerecomedationsystem.recommendation.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import cz.kocabek.animerecomedationsystem.recommendation.dto.AnimeSuggestionDto;
import cz.kocabek.animerecomedationsystem.recommendation.search.SearchRanker.Ranked;

class SearchRankerTest {

    private static AnimeSuggestionDto anime(long id, String name, String english, Integer popularity) {
        return new AnimeSuggestionDto(id, name, english, "TV", 8.0, popularity);
    }

    @Test
    void exactNameBeatsExactEnglishNameBeatsPrefixBeatsContains() {
        final var contains = anime(4, "The Big Naruto Show", null, 1);
        final var prefix = anime(3, "Naruto Spin-off", null, 2);
        final var englishExact = anime(2, "Naruto (2023)", "Naruto", 3);
        final var nameExact = anime(1, "Naruto", "Naruto", 4);

        final var ranked = SearchRanker.rank("naruto", List.of(contains, prefix, englishExact, nameExact));

        assertThat(ranked).extracting(r -> r.anime().id()).containsExactly(1L, 2L, 3L, 4L);
        assertThat(ranked).extracting(Ranked::tier)
                .containsExactly(SearchRanker.NAME_EXACT, SearchRanker.ENGLISH_EXACT,
                        SearchRanker.NAME_PREFIX, SearchRanker.WORD_START);
    }

    @Test
    void trailingPunctuationDoesNotPreventAnExactMatch() {
        final var yourName = anime(32281, "Kimi no Na wa.", "Your Name.", 20);
        final var ranked = SearchRanker.rank("your name", List.of(yourName));
        assertThat(ranked.getFirst().tier()).isEqualTo(SearchRanker.ENGLISH_EXACT);
        assertThat(ranked.getFirst().isExact()).isTrue();
    }

    @Test
    void sameTierIsOrderedByPopularityWithUnknownLast() {
        final var unknown = anime(1, "Cowboy A", null, null);
        final var popular = anime(2, "Cowboy B", null, 10);
        final var lessPopular = anime(3, "Cowboy C", null, 500);

        final var ranked = SearchRanker.rank("cowboy", List.of(unknown, lessPopular, popular));

        assertThat(ranked).extracting(r -> r.anime().id()).containsExactly(2L, 3L, 1L);
    }

    @Test
    void wordsThatAreNotNextToEachOtherRankLast() {
        final var apart = anime(1, "Attack of the Giant Titan", null, 1);
        final var together = anime(2, "Giant Titan Attack", null, 900);

        final var ranked = SearchRanker.rank("attack titan", List.of(apart, together));

        assertThat(ranked).extracting(Ranked::tier).containsOnly(SearchRanker.ALL_WORDS);
    }

    @Test
    void containsInTheMiddleOfAWordIsWeakerThanWordStart() {
        final var wordStart = anime(1, "Great Teacher Onizuka", null, 900);
        final var inside = anime(2, "Superteacher Days", null, 1);

        final var ranked = SearchRanker.rank("teacher", List.of(inside, wordStart));

        assertThat(ranked).extracting(r -> r.anime().id()).containsExactly(1L, 2L);
    }
}
