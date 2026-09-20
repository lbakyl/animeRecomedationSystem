package cz.kocabek.animerecomedationsystem.recommendation.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import cz.kocabek.animerecomedationsystem.recommendation.dto.AnimeDto;
import cz.kocabek.animerecomedationsystem.recommendation.entity.Anime;

public interface AnimeRepository extends JpaRepository<Anime, Long> {

    Iterable<Anime> findTop5ByGenres_GenreName(@NonNull String genreName);

    @Query("""
            select new cz.kocabek.animerecomedationsystem.recommendation.dto.AnimeDto(a.id,a.name,a.score,a.imageURL)
                        from Anime a
                        where a.id  in :animeIds
                        order by a.score DESC""")
    List<AnimeDto> getAnimeInfoListOrderByScore(@Param("animeIds") @NonNull Collection<Long> animeIds);

    @Query("select a.name from Anime a where a.id in :animeIds")
    Collection<String> getAnimeNamesByIds(Collection<Long> animeIds);

    @Query("select a.name from Anime a where a.id =:animeId")
    String getAnimeNameById(@Param("animeId") Long id);

    @Query("""
            select a from Anime a
            JOIN FETCH a.genres
            where a.id =:animeId""")
    Optional<Anime> fetchByIdWithGenres(@Param("animeId") Long id);
}
