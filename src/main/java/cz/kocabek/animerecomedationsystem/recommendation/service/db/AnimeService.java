package cz.kocabek.animerecomedationsystem.recommendation.service.db;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Service;

import cz.kocabek.animerecomedationsystem.recommendation.dto.AnimeDto;
import cz.kocabek.animerecomedationsystem.recommendation.entity.Anime;
import cz.kocabek.animerecomedationsystem.recommendation.repository.AnimeRepository;
import cz.kocabek.animerecomedationsystem.recommendation.search.SearchText;
import jakarta.validation.ValidationException;

@Service
public class AnimeService {

    AnimeRepository animeRepository;
    AnimeSearchService searchService;

    public AnimeService(AnimeRepository animeRepository, AnimeSearchService searchService) {
        this.animeRepository = animeRepository;
        this.searchService = searchService;
    }

    public Anime getAnimeByIdWithGenres(Long id) throws IllegalArgumentException {
        return animeRepository.fetchByIdWithGenres(id).orElseThrow(() -> new IllegalArgumentException("Anime not found"));
    }

    public Iterable<Anime> getAnimeByGenre(String genre) {
        return animeRepository.findTop5ByGenres_GenreName(genre);
    }

    /**
     * Finds the anime for the typed title, see {@link AnimeSearchService#resolveId(String)}.
     *
     * @throws ValidationException when the title is blank; {@link cz.kocabek.animerecomedationsystem.recommendation.search.AnimeSearchException}
     *                             (a subtype) when there is no single match
     */
    public Long getAnimeIdByName(String animeName) throws ValidationException {
        return searchService.resolveId(animeName);
    }

    /**
     * Finds the anime for the submitted search form. An id picked from the suggestions is trusted only
     * while the text field still holds that title, otherwise (stale id, edited text) the text wins.
     */
    public Long getAnimeIdForForm(String animeName, Long pickedId) throws ValidationException {
        if (pickedId != null) {
            final var typed = SearchText.normalizeQuery(animeName);
            final var picked = animeRepository.findById(pickedId);
            if (picked.isPresent() && !typed.isEmpty()
                    && (typed.equals(SearchText.normalize(picked.get().getName()))
                    || typed.equals(SearchText.normalize(picked.get().getEnglishName())))) {
                return pickedId;
            }
        }
        return searchService.resolveId(animeName);
    }

    public List<AnimeDto> retrieveAnimeByIdsSortedByScore(Collection<Long> ids) {
        return animeRepository.getAnimeInfoListOrderByScore(ids);
    }

    public Collection<String> getAnimeNamesByIds(Collection<Long> animeIds) {
        return animeRepository.getAnimeNamesByIds(animeIds);
    }

    public String getAnimeNameById(Long id) {
        return animeRepository.getAnimeNameById(id);
    }
}
