package cz.kocabek.animerecomedationsystem.recommendation.search;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SearchTextTest {

    @Test
    void normalize_ignoresCaseAndTrailingPunctuation() {
        assertThat(SearchText.normalize("Your Name.")).isEqualTo("your name");
        assertThat(SearchText.normalize("your name")).isEqualTo("your name");
    }

    @Test
    void normalize_turnsPunctuationIntoWordBreaks() {
        assertThat(SearchText.normalize("Steins;Gate")).isEqualTo("steins gate");
        assertThat(SearchText.normalize("Fullmetal Alchemist: Brotherhood")).isEqualTo("fullmetal alchemist brotherhood");
        assertThat(SearchText.normalize("  many   spaces  ")).isEqualTo("many spaces");
    }

    @Test
    void normalize_stripsLatinAccents() {
        assertThat(SearchText.normalize("Pokémon")).isEqualTo("pokemon");
    }

    @Test
    void normalize_keepsJapaneseTextIntact() {
        // voiced kana (dakuten) are a part of the letter and must not be stripped
        assertThat(SearchText.normalize("ガンダム")).isEqualTo("ガンダム");
        assertThat(SearchText.normalize("君の名は。")).isEqualTo("君の名は");
    }

    @Test
    void normalize_handlesNullAndEmpty() {
        assertThat(SearchText.normalize(null)).isEmpty();
        assertThat(SearchText.normalize("")).isEmpty();
        assertThat(SearchText.normalize("...")).isEmpty();
    }

    @Test
    void normalizeQuery_cutsOverlongInput() {
        final var longInput = "a".repeat(SearchText.MAX_QUERY_LENGTH + 50);
        assertThat(SearchText.normalizeQuery(longInput)).hasSize(SearchText.MAX_QUERY_LENGTH);
    }

    @Test
    void lookupTokens_dropsSingleLettersWhenThereAreLongerWords() {
        assertThat(SearchText.lookupTokens("k on")).containsExactly("on");
        assertThat(SearchText.lookupTokens("k")).containsExactly("k");
    }

    @Test
    void lookupTokens_capsTheNumberOfWords() {
        assertThat(SearchText.lookupTokens("aa bb cc dd ee ff gg hh")).hasSize(SearchText.MAX_TOKENS);
    }

    @Test
    void tokens_ofBlankTextIsEmpty() {
        assertThat(SearchText.tokens("")).isEmpty();
        assertThat(SearchText.tokens(null)).isEmpty();
    }
}
