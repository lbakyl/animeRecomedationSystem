package cz.kocabek.animerecomedationsystem.recommendation.service.recommendationconfig;

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
    private InputDTO configForm = new InputDTO(this.animeName, this.minScore, this.maxUsers, this.onlyInAnimeGenres, null);

    public void updateConfig(InputDTO formData) {
        this.configForm = formData;
        this.animeName = formData.animeName();
        this.minScore = formData.minRating();
        this.maxUsers = formData.maxUsers();
        this.onlyInAnimeGenres = formData.onlyInAnimeGenres();
    }

    public void resetConfigForm() {
        this.configForm = new InputDTO(null, ConfigConstant.MIN_INPUT_SCORE, ConfigConstant.MAX_USERS_PER_PAGE, false, null);
    }

    public ConfigCacheKey createCacheKey() {
        return new ConfigCacheKey(
                this.animeId,
                this.minScore,
                this.maxUsers
                );
    }

}
