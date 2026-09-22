package cz.kocabek.animerecomedationsystem.recommendation.service.db;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import cz.kocabek.animerecomedationsystem.recommendation.dto.AnimeGenreInfo;
import cz.kocabek.animerecomedationsystem.recommendation.repository.AnimeGenreRepository;
import cz.kocabek.animerecomedationsystem.recommendation.repository.GenreRepository;

@Service
public class AnimeGenreService {

    AnimeGenreRepository repository;
    GenreRepository genreRepository;

    public AnimeGenreService(AnimeGenreRepository repository, GenreRepository genreRepository) {
        this.repository = repository;
        this.genreRepository = genreRepository;
    }

    public Map<Long, List<String>> getGenresByAnimeIds(Collection<Long> animeIds) {
        final var animeGenreInfos = repository.getById_AnimeIdIn(animeIds);
        return groupGenrePerAnime(animeGenreInfos);
    }

    public List<String> getGenresForAnime(Long animeId) {
        return repository.getAnimeGenresByAnimeId(animeId);
    }

    /**
     * All genre names, for the "Genres" advanced-options list; the full list rarely changes, so it is cached.
     */
    @Cacheable("genreNames")
    public List<String> getAllGenreNames() {
        return genreRepository.findAllGenreNames();
    }

    private Map<Long, List<String>> groupGenrePerAnime(List<AnimeGenreInfo> genreInfos) {
        return genreInfos.stream().collect(
                Collectors.groupingBy(info -> info.getAnime().getId(),
                        Collectors.mapping(info -> info.getGenre().getGenreName(),
                                Collectors.toList())
                ));
    }
}
