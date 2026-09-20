package cz.kocabek.animerecomedationsystem.recommendation.service.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;

import cz.kocabek.animerecomedationsystem.recommendation.dto.AnimeSuggestionDto;
import cz.kocabek.animerecomedationsystem.recommendation.search.AnimeSearchException;
import cz.kocabek.animerecomedationsystem.recommendation.search.AnimeSearchException.Reason;
import cz.kocabek.animerecomedationsystem.recommendation.search.AnimeSearchRepository;
import cz.kocabek.animerecomedationsystem.recommendation.search.SearchText;
import jakarta.validation.ValidationException;

class AnimeSearchServiceTest {

    private static AnimeSuggestionDto anime(long id, String name, String english, String type, int popularity) {
        return new AnimeSuggestionDto(id, name, english, type, 8.0, popularity);
    }

    private final List<AnimeSuggestionDto> data = List.of(
            anime(1, "Cowboy Bebop", "Cowboy Bebop", "TV", 39),
            anime(5, "Cowboy Bebop: Tengoku no Tobira", "Cowboy Bebop: The Movie", "Movie", 400),
            anime(20, "Naruto", "Naruto", "TV", 17),
            anime(55453, "Naruto (2023)", "Naruto", "TV", 5000),
            anime(199, "Sen to Chihiro no Kamikakushi", "Spirited Away", "Movie", 30),
            anime(32281, "Kimi no Na wa.", "Your Name.", "Movie", 20),
            anime(9253, "Steins;Gate", "Steins;Gate", "TV", 15),
            anime(16498, "Shingeki no Kyojin", "Attack on Titan", "TV", 1));

    /** Behaves like the SQL lookup: every / any word must be contained in the name or the English name. */
    private final AnimeSearchRepository fakeRepository = new AnimeSearchRepository() {
        @Override
        public List<AnimeSuggestionDto> findCandidates(List<String> words, boolean allWords, int limit) {
            return data.stream()
                    .filter(a -> {
                        final var text = SearchText.normalize(a.name()) + " | " + SearchText.normalize(a.englishName());
                        return allWords ? words.stream().allMatch(text::contains) : words.stream().anyMatch(text::contains);
                    })
                    .sorted(Comparator.comparing(AnimeSuggestionDto::popularity))
                    .limit(limit)
                    .toList();
        }
    };

    private final AnimeSearchService service = new AnimeSearchService(fakeRepository);

    // ---- resolveId

    @Test
    void exactTitleGoesStraightToTheAnime() {
        assertThat(service.resolveId("Cowboy Bebop")).isEqualTo(1L);
    }

    @Test
    void exactTitleWinsEvenWhenAnotherAnimeSharesTheEnglishName() {
        // "Naruto (2023)" has the English name "Naruto" too, but the real name match is preferred
        assertThat(service.resolveId("Naruto")).isEqualTo(20L);
    }

    @Test
    void caseAndSpacingDoNotMatter() {
        assertThat(service.resolveId("  cowboy   BEBOP ")).isEqualTo(1L);
    }

    @Test
    void englishTitleWorksAndTrailingPunctuationCanBeLeftOut() {
        assertThat(service.resolveId("Spirited Away")).isEqualTo(199L);
        assertThat(service.resolveId("Your Name")).isEqualTo(32281L);
    }

    @Test
    void punctuationInsideATitleCanBeReplacedBySpaces() {
        assertThat(service.resolveId("steins gate")).isEqualTo(9253L);
    }

    @Test
    void partOfATitleWithASingleMatchGoesStraightToTheAnime() {
        assertThat(service.resolveId("chihiro")).isEqualTo(199L);
        assertThat(service.resolveId("attack titan")).isEqualTo(16498L);
    }

    @Test
    void severalMatchesProduceAPickListInPopularityOrder() {
        assertThatThrownBy(() -> service.resolveId("cowboy"))
                .isInstanceOfSatisfying(AnimeSearchException.class, e -> {
                    assertThat(e.getReason()).isEqualTo(Reason.AMBIGUOUS);
                    assertThat(e.getMessage()).contains("cowboy");
                    assertThat(e.getCandidates()).extracting(AnimeSuggestionDto::id).containsExactly(1L, 5L);
                });
    }

    @Test
    void unknownTitleIsNotFoundWithoutSuggestions() {
        assertThatThrownBy(() -> service.resolveId("zzzz qqqq"))
                .isInstanceOfSatisfying(AnimeSearchException.class, e -> {
                    assertThat(e.getReason()).isEqualTo(Reason.NOT_FOUND);
                    assertThat(e.getCandidates()).isEmpty();
                });
    }

    @Test
    void aTypoInOneWordOffersDidYouMean() {
        assertThatThrownBy(() -> service.resolveId("Cowboy Bepop"))
                .isInstanceOfSatisfying(AnimeSearchException.class, e -> {
                    assertThat(e.getReason()).isEqualTo(Reason.NOT_FOUND);
                    assertThat(e.getMessage()).contains("Did you mean");
                    assertThat(e.getCandidates()).extracting(AnimeSuggestionDto::id).contains(1L);
                });
    }

    @Test
    void blankInputIsAValidationErrorNotASearch() {
        assertThatThrownBy(() -> service.resolveId("   "))
                .isInstanceOf(ValidationException.class)
                .isNotInstanceOf(AnimeSearchException.class)
                .hasMessageContaining("blank");
        assertThatThrownBy(() -> service.resolveId("..."))
                .isInstanceOf(ValidationException.class)
                .isNotInstanceOf(AnimeSearchException.class);
    }

    // ---- suggest

    @Test
    void suggestNeedsAtLeastTwoCharacters() {
        assertThat(service.suggest("c")).isEmpty();
        assertThat(service.suggest(" ")).isEmpty();
        assertThat(service.suggest(null)).isEmpty();
    }

    @Test
    void suggestReturnsTheBestMatchesFirst() {
        final var suggestions = service.suggest("na");
        // "Naruto" (name prefix) first, the other prefix match after it, then titles with "na" inside
        assertThat(suggestions).isNotEmpty();
        assertThat(suggestions.getFirst().id()).isEqualTo(20L);
    }

    @Test
    void suggestIsLimited() {
        final var many = new java.util.ArrayList<AnimeSuggestionDto>();
        for (int i = 0; i < 30; i++) {
            many.add(anime(1000 + i, "Gundam " + i, null, "TV", i));
        }
        final var bigService = new AnimeSearchService(new AnimeSearchRepository() {
            @Override
            public List<AnimeSuggestionDto> findCandidates(List<String> words, boolean allWords, int limit) {
                return many;
            }
        });
        assertThat(bigService.suggest("gundam")).hasSize(AnimeSearchService.SUGGESTION_LIMIT);
    }
}
