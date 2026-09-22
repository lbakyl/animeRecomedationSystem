package cz.kocabek.animerecomedationsystem.recommendation.service.recommendationconfig;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import cz.kocabek.animerecomedationsystem.recommendation.dto.ConfigCacheKey;
import cz.kocabek.animerecomedationsystem.recommendation.dto.InputDTO;
import lombok.Data;

@Service
@SessionScope
@Data
public class RecommendationConfig {

    private String animeName = "";
    private Long animeId;
    private int minScore = ConfigConstant.MIN_INPUT_SCORE;
    private int maxUsers = ConfigConstant.MAX_USERS_PER_PAGE;
    private boolean onlyInAnimeGenres = false;
    private List<String> genres = List.of();
    private List<String> types = List.of();
    // matches the "Exclude Adult Content" checkbox being checked by default in fragments/header.html
    private List<String> excludedContent = List.of(ConfigConstant.EXCLUDE_ADULT);
    private InputDTO configForm = new InputDTO(this.animeName, this.minScore, this.maxUsers, this.onlyInAnimeGenres,
            null, this.genres, this.types, this.excludedContent);

    public void updateConfig(InputDTO formData) {
        this.configForm = formData;
        this.animeName = formData.animeName();
        this.minScore = formData.minRating();
        this.maxUsers = formData.maxUsers();
        this.onlyInAnimeGenres = formData.onlyInAnimeGenres();
        this.genres = formData.genres();
        this.types = formData.types();
        this.excludedContent = formData.excludedContent();
    }

    public void resetConfigForm() {
        this.configForm = new InputDTO(null, ConfigConstant.MIN_INPUT_SCORE, ConfigConstant.MAX_USERS_PER_PAGE, false,
                null, List.of(), List.of(), List.of(ConfigConstant.EXCLUDE_ADULT));
    }

    public ConfigCacheKey createCacheKey() {
        return new ConfigCacheKey(
                this.animeId,
                this.minScore,
                this.maxUsers
                );
    }

}
