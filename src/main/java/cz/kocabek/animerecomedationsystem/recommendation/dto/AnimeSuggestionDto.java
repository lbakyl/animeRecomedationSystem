package cz.kocabek.animerecomedationsystem.recommendation.dto;

/**
 * Light-weight anime description used by the search suggestions and the pick-list.
 */
public record AnimeSuggestionDto(Long id,
                                 String name,
                                 String englishName,
                                 String type,
                                 Double score,
                                 Integer popularity) {

    /** "Sen to Chihiro no Kamikakushi (Spirited Away)" - the English name is shown only when it differs. */
    public String label() {
        if (englishName == null || englishName.isBlank() || englishName.equalsIgnoreCase(name)) {
            return name;
        }
        return name + " (" + englishName + ")";
    }
}
