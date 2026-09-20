package cz.kocabek.animerecomedationsystem.recommendation.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import cz.kocabek.animerecomedationsystem.recommendation.dto.AnimeDto;
import cz.kocabek.animerecomedationsystem.recommendation.dto.AnimeOutDTO;
import cz.kocabek.animerecomedationsystem.recommendation.dto.RecommendationDTO;
import cz.kocabek.animerecomedationsystem.recommendation.service.db.AnimeGenreService;
import cz.kocabek.animerecomedationsystem.recommendation.service.db.AnimeService;
import cz.kocabek.animerecomedationsystem.recommendation.service.db.UserAnimeScoreService;
import lombok.AllArgsConstructor;

/**
 * A service for generating detail recommendations based on user preferences and
 * interactions. This service leverages user rating data, user-detail
 * associations, and recommendation algorithms to provide personalized detail
 * suggestions. The service interacts with multiple components to retrieve and
 * process data: - {@link AnimeService} for detail-related operations such as
 * retrieving detail details and IDs. - {@link UserAnimeScoreService} for
 * fetching user-detail relationship data and ratings. -
 * {@link RecommendationEngine} to execute the recommendation algorithm and
 * derive detail suggestions.
 */
@AllArgsConstructor
@Service
public class RecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationService.class);

    /* db services */
    AnimeService animeService;
    UserAnimeScoreService userAnimeScoreService;
    AnimeGenreService animeGenreService;
    //    algorithm service
    RecommendationEngine engine;
    //service for building output DTO
    DTOResultBuilder resultBuilder;


    /* public endpoints*/
    public RecommendationDTO getAnimeRecommendation(String name) {
        resultBuilder.init(name);
        return generateAnimeRecommendations();
    }

    public RecommendationDTO getAnimeRecommendation() {
        return generateAnimeRecommendations();
    }

    /*---*/
    /**
     * Generates anime recommendations for a given anime ID using an
     * intersection weight algorithm along with various processing and analysis
     * steps to refine the recommendations.
     *
     *
     * @return a {@link RecommendationDTO} object containing the input anime
     * names and a list of recommended anime's
     */
    private RecommendationDTO generateAnimeRecommendations() {
        //collecting and grouping data from the database into a list of users Anime lists
        long step1Start = System.nanoTime();
        final var usersAnimeLists = userAnimeScoreService.getUsersAnimeLists();
        long step1Duration = (System.nanoTime() - step1Start) / 1_000_000;
        logger.debug("Step 1 (collect users data) took: {} ms", step1Duration);
        logger.info("size of data after grouping: {}", usersAnimeLists.size());

        long step2Start = System.nanoTime();
        final var animeMap = engine.buildAnimeMap(usersAnimeLists);
        long step2Duration = (System.nanoTime() - step2Start) / 1_000_000;
        logger.debug("Step 2 (build anime occurrences map) took: {} ms", step2Duration);
        logger.debug("number of anime in map: {}", animeMap.size());
        final var mapWithDetails = enrichedMapByDetails(animeMap);
        final var processedMap = engine.filteredAndSortAnimeMap(usersAnimeLists, mapWithDetails);
        long step3Start = System.nanoTime();
        final var weightedAnime = engine.weightAnime(processedMap, AnimeScoreCalculator.compositeScoring);
        final var topRecommendations = engine.cutTheTopN(weightedAnime);//current final anime map with ids without detail yet
        long step3Duration = (System.nanoTime() - step3Start) / 1_000_000;
        logger.debug("Step 3 (weighted anime's) took: {} ms", step3Duration);
        logger.debug("size of shortened list: {}", topRecommendations.size());
        long step4Start = System.nanoTime();
        //get anime details separately
        final var animesInfo = getAnimeInfos(topRecommendations);

        long step4Duration = (System.nanoTime() - step4Start) / 1_000_000;
        logger.debug("Step 4 (get anime details) took: {} ms", step4Duration);
        logger.debug("recommended anime's: {}", animesInfo.size());
        return buildOutputList(animesInfo, topRecommendations);
    }

    private Collection<Long> getAnimeIDsFromDTO(Map<Long, AnimeOutDTO> animeMap) {
        return animeMap.keySet().stream().toList();
    }

    private RecommendationDTO buildOutputList(List<AnimeDto> animeDetails, Map<Long, AnimeOutDTO> outDTO) {
        for (AnimeDto detail : animeDetails) {
            outDTO.get(detail.id()).setAnimeInfo(detail);
        }
        return resultBuilder.addRecommendation(convertToAnimeList(outDTO)).build();
    }

    private List<AnimeOutDTO> convertToAnimeList(Map<Long, AnimeOutDTO> animeData) {
        animeData.forEach((key, value) -> value.setAnimeId(key));
        return animeData.values().stream().toList();
    }

    private Map<Long, AnimeOutDTO> enrichedMapByDetails(Map<Long, AnimeOutDTO> map) {
        final var genresDetail = animeGenreService.getGenresByAnimeIds(map.keySet());
        genresDetail.forEach((animeId, genres) -> map.get(animeId).setGenres(genres));
        return map;
    }

    private List<AnimeDto> getAnimeInfos(Map<Long, AnimeOutDTO> topRecommendations) {
        return animeService.retrieveAnimeByIdsSortedByScore(getAnimeIDsFromDTO(topRecommendations));
//        final var genresDetail = animeGenreService.getGenresForAnime(topRecommendations.keySet());
//        for (AnimeDto detail : details) {
//            detail.getGenres().addAll(genresDetail.get(detail.getId()));
//        }
//        return details;
    }
}
