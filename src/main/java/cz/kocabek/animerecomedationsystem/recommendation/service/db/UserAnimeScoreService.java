package cz.kocabek.animerecomedationsystem.recommendation.service.db;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import cz.kocabek.animerecomedationsystem.recommendation.dto.ConfigCacheKey;
import cz.kocabek.animerecomedationsystem.recommendation.dto.UserAnimeList;
import cz.kocabek.animerecomedationsystem.recommendation.dto.UsersAnimeScoreDto;
import cz.kocabek.animerecomedationsystem.recommendation.service.recommendationconfig.RecommendationConfig;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class UserAnimeScoreService {

    private static final Logger logger = LoggerFactory.getLogger(UserAnimeScoreService.class);

    private final RecommendationConfig config;
    private final CacheableAnimeDataProvider cacheableAnimeDataProvider;

    public List<UserAnimeList> getUsersAnimeLists() {
        final ConfigCacheKey cacheKey = config.createCacheKey();
        long step1_1Start = System.nanoTime();
        final var usersId = cacheableAnimeDataProvider.getUsersIdWhoRatedGivenAnime(cacheKey);
        long step1_1Duration = (System.nanoTime() - step1_1Start) / 1_000_000;
        logger.debug("Step 1.1 (collect users with detail) took: {} ms", step1_1Duration);
        logger.info("Users with detail after service: {}", usersId.size());

        long step1_2Start = System.nanoTime();
        final var userRatingsData = cacheableAnimeDataProvider.fetchRatedAnimeByUsers(usersId, cacheKey);
        long step1_2Duration = (System.nanoTime() - step1_2Start) / 1_000_000;
        logger.debug("Step 1.2 (fetch user ratings) took: {} ms", step1_2Duration);
        return groupUserByID(userRatingsData);
    }

    /**
     * Groups the provided data of detail scores by user ID and constructs a
     * list of {@link UserAnimeList} objects containing user IDs and their
     * respective detail ratings.
     *
     * @param data a {@link Slice} of {@link UsersAnimeScoreDto} objects
     * representing user ratings for various detail
     * @return a {@link List} of {@link UserAnimeList} containing user IDs and
     * their mapped detail scores
     */
    private List<UserAnimeList> groupUserByID(Slice<UsersAnimeScoreDto> data) {
        // grouping records based UserID Map<Long, List<UsersAnimeScoreDto>>
        // making name and rating from list -> Map.Entry<Long, Map<String, Integer>>
        return data.get()
                .collect(Collectors.groupingBy(UsersAnimeScoreDto::userId)) // grouping records based UserID Map<Long, List<UsersAnimeScoreDto>>
                .entrySet().stream().
                map(e -> {
                    final var map = e.getValue().stream().collect(Collectors.toMap(UsersAnimeScoreDto::animeId, UsersAnimeScoreDto::rating, (_, b) -> b));
                    return Map.entry(e.getKey(), map);
                    // making name and rating from list -> Map.Entry<Long, Map<String, Integer>>
                })
                .map(entry -> new UserAnimeList(entry.getKey(), entry.getValue()))
                .toList();
    }
}
