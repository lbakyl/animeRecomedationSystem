package cz.kocabek.animerecomedationsystem.recommendation.service.recommendationconfig;

public final class ConfigConstant {
    //    basic db values
    public static final int MAX_SCORE = 10;// max available score in db
    public static final int MIN_SCORE = 0;//min available score in db
    //user form config base value
    //min and max user score for input anime
    public static final int MIN_INPUT_SCORE = 8;
    public static final int MAX_INPUT_SCORE = 10;
    //number of users id fetch in one page
    public static final int MAX_USERS_PER_PAGE = 1500;
    //filter ratings values
    public static final int LOWEST_ACCEPTABLE_RATING = 8;
    public static final int MAX_ACCEPTABLE_RATING = 10;
    //algorithm base values
    //weight of occurrence value in the recommendation algorithm
    public static final double OCCURRENCE_WEIGHT = 0.7;
    //weight of average score value in the recommendation algorithm
    public static final double SCORE_WEIGHT = 0.7;
    //final size of detail list for the recommendation in the UI
    public static final int FINAL_ANIME_LIST_SIZE = 50;
    //values posted by the "Content Filters" checkboxes, see fragments/header.html
    public static final String EXCLUDE_ADULT = "adult";
    public static final String EXCLUDE_ECCHI = "ecchi";

    private ConfigConstant() {
    }
}
