package cz.kocabek.animerecomedationsystem.recommendation.search;

import java.util.List;

import cz.kocabek.animerecomedationsystem.recommendation.dto.AnimeSuggestionDto;
import jakarta.validation.ValidationException;

/**
 * The typed text did not lead to exactly one anime. It carries the titles the user can pick from
 * (several matches) or that may have been meant (no match).
 */
public class AnimeSearchException extends ValidationException {

    public enum Reason {AMBIGUOUS, NOT_FOUND}

    private final transient List<AnimeSuggestionDto> candidates;
    private final Reason reason;

    public AnimeSearchException(Reason reason, String message, List<AnimeSuggestionDto> candidates) {
        super(message);
        this.reason = reason;
        this.candidates = List.copyOf(candidates);
    }

    public List<AnimeSuggestionDto> getCandidates() {
        return candidates;
    }

    public Reason getReason() {
        return reason;
    }
}
