package cz.kocabek.animerecomedationsystem.recommendation.service.db;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import cz.kocabek.animerecomedationsystem.recommendation.dto.AnimeSuggestionDto;
import cz.kocabek.animerecomedationsystem.recommendation.search.AnimeSearchException;
import cz.kocabek.animerecomedationsystem.recommendation.search.AnimeSearchException.Reason;
import cz.kocabek.animerecomedationsystem.recommendation.search.AnimeSearchRepository;
import cz.kocabek.animerecomedationsystem.recommendation.search.SearchRanker;
import cz.kocabek.animerecomedationsystem.recommendation.search.SearchRanker.Ranked;
import cz.kocabek.animerecomedationsystem.recommendation.search.SearchText;
import jakarta.validation.ValidationException;

/**
 * Title search: type-ahead suggestions and resolving a typed title to an anime id.
 */
@Service
public class AnimeSearchService {

    /** Rows loaded from the database before ranking. */
    static final int CANDIDATE_LIMIT = 100;
    /** Entries of the pick-list shown for an ambiguous title. */
    static final int PICK_LIST_SIZE = 10;
    /** Entries of the "did you mean" list. */
    static final int DID_YOU_MEAN_SIZE = 5;
    /** Type-ahead starts with this number of characters. */
    public static final int MIN_SUGGEST_LENGTH = 2;
    public static final int SUGGESTION_LIMIT = 8;

    private final AnimeSearchRepository repository;

    public AnimeSearchService(AnimeSearchRepository repository) {
        this.repository = repository;
    }

    /** Best matching titles for the type-ahead dropdown (may be empty). */
    public List<AnimeSuggestionDto> suggest(String rawQuery) {
        final var query = SearchText.normalizeQuery(rawQuery);
        if (query.length() < MIN_SUGGEST_LENGTH) {
            return List.of();
        }
        return search(query).stream().limit(SUGGESTION_LIMIT).map(Ranked::anime).toList();
    }

    /**
     * Resolves a typed title to an anime id.
     * <ul>
     *   <li>one match, or one clearly exact title: its id</li>
     *   <li>several matches: {@link AnimeSearchException} with the pick-list</li>
     *   <li>no match: {@link AnimeSearchException} with "did you mean" titles</li>
     * </ul>
     */
    public Long resolveId(String rawName) throws ValidationException {
        final var query = SearchText.normalizeQuery(rawName);
        if (query.isBlank()) {
            throw new ValidationException("Anime name cannot be blank");
        }
        final var ranked = search(query);
        if (ranked.size() == 1) {
            return ranked.getFirst().anime().id();
        }
        if (!ranked.isEmpty()) {
            final var best = ranked.getFirst();
            if (best.isExact() && ranked.stream().filter(r -> r.tier() == best.tier()).count() == 1) {
                return best.anime().id();
            }
            throw new AnimeSearchException(Reason.AMBIGUOUS,
                    "Several anime match \"%s\". Pick one:".formatted(rawName.trim()),
                    ranked.stream().limit(PICK_LIST_SIZE).map(Ranked::anime).toList());
        }
        final var similar = didYouMean(query);
        throw new AnimeSearchException(Reason.NOT_FOUND,
                similar.isEmpty() ? "This anime was not found. Try a different name."
                        : "This anime was not found. Did you mean:",
                similar);
    }

    private List<Ranked> search(String query) {
        final var candidates = repository.findCandidates(SearchText.lookupTokens(query), true, CANDIDATE_LIMIT);
        return SearchRanker.rank(query, candidates);
    }

    /** Typos and half-remembered titles: look for the longest word of the query on its own. */
    private List<AnimeSuggestionDto> didYouMean(String query) {
        final var longest = SearchText.tokens(query).stream()
                .filter(t -> t.length() >= 3)
                .max(Comparator.comparingInt(String::length));
        if (longest.isEmpty()) {
            return List.of();
        }
        final var candidates = repository.findCandidates(List.of(longest.get()), true, CANDIDATE_LIMIT);
        return SearchRanker.rank(longest.get(), candidates).stream()
                .limit(DID_YOU_MEAN_SIZE).map(Ranked::anime).toList();
    }
}
