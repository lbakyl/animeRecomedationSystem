package cz.kocabek.animerecomedationsystem.recommendation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import cz.kocabek.animerecomedationsystem.recommendation.entity.Genre;

public interface GenreRepository extends JpaRepository<Genre, Integer> {

    @Query("select g.genreName from Genre g order by g.genreName")
    List<String> findAllGenreNames();
}
