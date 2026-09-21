package cz.kocabek.animerecomedationsystem.recommendation.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import cz.kocabek.animerecomedationsystem.account.service.AccService;
import cz.kocabek.animerecomedationsystem.account.service.WatchListService;
import cz.kocabek.animerecomedationsystem.recommendation.dto.AnimeSuggestionDto;
import cz.kocabek.animerecomedationsystem.recommendation.search.AnimeSearchException;
import cz.kocabek.animerecomedationsystem.recommendation.search.AnimeSearchException.Reason;
import cz.kocabek.animerecomedationsystem.recommendation.service.DTOResultBuilder;
import cz.kocabek.animerecomedationsystem.recommendation.service.RecommendationService;
import cz.kocabek.animerecomedationsystem.recommendation.service.db.AnimeSearchService;
import cz.kocabek.animerecomedationsystem.recommendation.service.db.AnimeService;
import cz.kocabek.animerecomedationsystem.recommendation.service.recommendationconfig.RecommendationConfig;

/**
 * Search related behaviour of the {@link ViewController}; the services are mocked, no database is needed.
 */
class ViewControllerSearchTest {

    private AnimeService animeService;
    private AnimeSearchService searchService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        animeService = mock(AnimeService.class);
        searchService = mock(AnimeSearchService.class);
        final var controller = new ViewController(mock(AccService.class), mock(RecommendationService.class),
                animeService, searchService, mock(DTOResultBuilder.class), new RecommendationConfig(),
                mock(WatchListService.class));
        // templates are not rendered here (default resolver only forwards / redirects),
        // the view name, redirects and the model are checked
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private static AnimeSuggestionDto anime(long id, String name) {
        return new AnimeSuggestionDto(id, name, name, "TV", 8.0, 1);
    }

    @Test
    void suggestReturnsTheFragmentWithTheMatches() throws Exception {
        when(searchService.suggest("cow")).thenReturn(List.of(anime(1, "Cowboy Bebop")));

        mockMvc.perform(get("/search/suggest").param("animeName", "cow"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/suggestions :: suggestions"))
                .andExpect(model().attribute("items", List.of(anime(1, "Cowboy Bebop"))))
                .andExpect(model().attributeDoesNotExist("notice"));
    }

    @Test
    void suggestTellsWhenNothingMatches() throws Exception {
        when(searchService.suggest("zzz")).thenReturn(List.of());

        mockMvc.perform(get("/search/suggest").param("animeName", "zzz"))
                .andExpect(model().attribute("notice", "No titles match yet."));
    }

    @Test
    void suggestIgnoresTooShortInputWithoutTouchingTheDatabase() throws Exception {
        mockMvc.perform(get("/search/suggest").param("animeName", "c"))
                .andExpect(view().name("fragments/suggestions :: none"));
        mockMvc.perform(get("/search/suggest"))
                .andExpect(view().name("fragments/suggestions :: none"));
        org.mockito.Mockito.verifyNoInteractions(searchService);
    }

    @Test
    void submitRedirectsToTheResultOfTheFoundAnime() throws Exception {
        when(animeService.getAnimeIdForForm(eq("Naruto"), eq(null))).thenReturn(20L);
        when(animeService.getAnimeNameById(20L)).thenReturn("Naruto");

        mockMvc.perform(post("/submit").param("animeName", "Naruto").param("minRating", "8").param("maxUsers", "500").param("_onlyInAnimeGenres", "on"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/result?id=20"));
    }

    @Test
    void submitWithAPickedIdPassesTheIdOn() throws Exception {
        when(animeService.getAnimeIdForForm(eq("Spirited Away"), eq(199L))).thenReturn(199L);
        when(animeService.getAnimeNameById(199L)).thenReturn("Sen to Chihiro no Kamikakushi");

        mockMvc.perform(post("/submit").param("animeName", "Spirited Away").param("animeId", "199")
                        .param("minRating", "8").param("maxUsers", "500").param("_onlyInAnimeGenres", "on"))
                .andExpect(redirectedUrl("/result?id=199"));
    }

    @Test
    void ambiguousTitleShowsThePickListOnTheSamePage() throws Exception {
        final var candidates = List.of(anime(1, "Cowboy Bebop"), anime(5, "Cowboy Bebop: The Movie"));
        when(animeService.getAnimeIdForForm(eq("cowboy"), eq(null)))
                .thenThrow(new AnimeSearchException(Reason.AMBIGUOUS, "Several anime match \"cowboy\". Pick one:", candidates));

        mockMvc.perform(post("/submit").param("animeName", "cowboy").param("minRating", "8").param("maxUsers", "500").param("_onlyInAnimeGenres", "on"))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attribute("candidates", candidates))
                .andExpect(model().attribute("searchNotice", "Several anime match \"cowboy\". Pick one:"))
                .andExpect(model().attribute("action", "/submit"))
                .andExpect(model().attributeHasNoErrors("anime"));
    }

    @Test
    void blankTitleStaysAFieldError() throws Exception {
        when(animeService.getAnimeIdForForm(anyString(), eq(null)))
                .thenThrow(new jakarta.validation.ValidationException("Anime name cannot be blank"));

        mockMvc.perform(post("/submit").param("animeName", "...").param("minRating", "8").param("maxUsers", "500").param("_onlyInAnimeGenres", "on"))
                .andExpect(view().name("main"))
                .andExpect(model().attributeHasFieldErrors("anime", "animeName"))
                .andExpect(model().attributeDoesNotExist("candidates"));
    }

    @Test
    void unknownAnimeDetailPageIsA404NotAServerError() throws Exception {
        when(animeService.getAnimeByIdWithGenres(999L)).thenThrow(new IllegalArgumentException("Anime not found"));

        mockMvc.perform(get("/anime/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void unknownResultIdGoesBackToTheSearchWithAMessage() throws Exception {
        when(animeService.getAnimeNameById(anyLong())).thenReturn(null);

        mockMvc.perform(get("/result").param("id", "999999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/main"))
                .andExpect(flash().attributeExists("searchError"));
    }
}
