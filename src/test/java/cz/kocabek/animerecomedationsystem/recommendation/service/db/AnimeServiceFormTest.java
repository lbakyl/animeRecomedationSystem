package cz.kocabek.animerecomedationsystem.recommendation.service.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cz.kocabek.animerecomedationsystem.recommendation.entity.Anime;
import cz.kocabek.animerecomedationsystem.recommendation.repository.AnimeRepository;

/**
 * The id of an anime picked from the suggestions is trusted only while the text field still holds that title.
 */
class AnimeServiceFormTest {

    private AnimeRepository animeRepository;
    private AnimeSearchService searchService;
    private AnimeService animeService;

    @BeforeEach
    void setUp() {
        animeRepository = mock(AnimeRepository.class);
        searchService = mock(AnimeSearchService.class);
        animeService = new AnimeService(animeRepository, searchService);
    }

    private static Anime anime(long id, String name, String english) {
        final var anime = new Anime();
        anime.setId(id);
        anime.setName(name);
        anime.setEnglishName(english);
        return anime;
    }

    @Test
    void pickedIdIsUsedWhenTheTextStillMatchesTheTitle() {
        when(animeRepository.findById(199L)).thenReturn(Optional.of(anime(199, "Sen to Chihiro no Kamikakushi", "Spirited Away")));

        assertThat(animeService.getAnimeIdForForm("Sen to Chihiro no Kamikakushi", 199L)).isEqualTo(199L);
        assertThat(animeService.getAnimeIdForForm("spirited away", 199L)).isEqualTo(199L);
        verify(searchService, never()).resolveId(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void staleIdIsIgnoredWhenTheTextWasChanged() {
        when(animeRepository.findById(199L)).thenReturn(Optional.of(anime(199, "Sen to Chihiro no Kamikakushi", "Spirited Away")));
        when(searchService.resolveId("Naruto")).thenReturn(20L);

        assertThat(animeService.getAnimeIdForForm("Naruto", 199L)).isEqualTo(20L);
    }

    @Test
    void unknownIdFallsBackToTheTypedText() {
        when(animeRepository.findById(999L)).thenReturn(Optional.empty());
        when(searchService.resolveId("Naruto")).thenReturn(20L);

        assertThat(animeService.getAnimeIdForForm("Naruto", 999L)).isEqualTo(20L);
    }

    @Test
    void withoutAnIdTheTypedTextIsSearched() {
        when(searchService.resolveId("Naruto")).thenReturn(20L);

        assertThat(animeService.getAnimeIdForForm("Naruto", null)).isEqualTo(20L);
        verify(animeRepository, never()).findById(org.mockito.ArgumentMatchers.anyLong());
    }
}
