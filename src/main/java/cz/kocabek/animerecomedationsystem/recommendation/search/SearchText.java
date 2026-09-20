package cz.kocabek.animerecomedationsystem.recommendation.search;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normalisation of anime titles and user queries so that they can be compared
 * ignoring case, Latin accents and punctuation ("Your Name" == "Your Name.",
 * "Steins Gate" == "Steins;Gate", "Pokemon" == "Pokémon").
 */
public final class SearchText {

    /** Longer user input is cut off; it protects the database and the log. */
    public static final int MAX_QUERY_LENGTH = 100;
    /** Number of query words that take part in the database lookup. */
    public static final int MAX_TOKENS = 6;

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^\\p{L}\\p{N}]+");

    private SearchText() {
    }

    /** Normalises a stored title or any already trusted text. */
    public static String normalize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        final var decomposed = Normalizer.normalize(text, Normalizer.Form.NFKD);
        final var sb = new StringBuilder(decomposed.length());
        char previousBase = 0;
        for (int i = 0; i < decomposed.length(); i++) {
            final char c = decomposed.charAt(i);
            final int type = Character.getType(c);
            final boolean combiningMark = type == Character.NON_SPACING_MARK
                    || type == Character.COMBINING_SPACING_MARK
                    || type == Character.ENCLOSING_MARK;
            // strip accents only from Latin letters, kana/hangul marks (e.g. dakuten) must stay
            if (combiningMark && isLatinLetter(previousBase)) {
                continue;
            }
            sb.append(c);
            previousBase = c;
        }
        final var recomposed = Normalizer.normalize(sb, Normalizer.Form.NFC);
        return NON_ALPHANUMERIC.matcher(recomposed.toLowerCase(Locale.ROOT)).replaceAll(" ").trim();
    }

    /** Normalises untrusted user input (length is limited first). */
    public static String normalizeQuery(String rawQuery) {
        if (rawQuery == null) {
            return "";
        }
        final var limited = rawQuery.length() > MAX_QUERY_LENGTH ? rawQuery.substring(0, MAX_QUERY_LENGTH) : rawQuery;
        return normalize(limited);
    }

    /** Words of an already normalised text. */
    public static List<String> tokens(String normalized) {
        if (normalized == null || normalized.isBlank()) {
            return List.of();
        }
        return Arrays.asList(normalized.split(" "));
    }

    /**
     * Words used for the database lookup: single letters are dropped when there is anything longer
     * (they match almost every title), and the count is capped.
     */
    public static List<String> lookupTokens(String normalizedQuery) {
        final var all = tokens(normalizedQuery);
        final var longer = all.stream().filter(t -> t.length() >= 2).toList();
        final var chosen = longer.isEmpty() ? all : longer;
        return chosen.size() > MAX_TOKENS ? chosen.subList(0, MAX_TOKENS) : chosen;
    }

    private static boolean isLatinLetter(char c) {
        return c != 0 && Character.isLetter(c) && Character.UnicodeScript.of(c) == Character.UnicodeScript.LATIN;
    }
}
