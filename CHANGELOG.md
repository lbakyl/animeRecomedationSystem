# Changelog

All notable changes of the `fix-search` branch are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased] - fix-search

Base: `last-version` (11 Aug 2026).

### Why

Searching for an anime required the **complete title, typed exactly**: the app ran
`where name = ?` and then `where englishName = ?`. Anything else ("Cowboy", "Your Name" for the stored
"Your Name.", "steins gate" for "Steins;Gate") ended with "This anime was not found", even though the
anime is in the database. Users could not find titles they knew existed, and a failed search gave no hint
what to try instead.

### Added

- **Type-ahead suggestions.** After two typed characters the search box shows up to 8 matching titles
  (`GET /search/suggest`, loaded with htmx, 250 ms debounce). Picking one searches for exactly that anime by
  its id, so the slider settings of the form are kept and same-named anime cannot be confused.
- **Tolerant title matching** (`recommendation/search/SearchText`). Case, Latin accents and punctuation are
  ignored, punctuation counts as a word break: "Your Name" finds "Your Name.", "steins gate" finds
  "Steins;Gate", "pokemon" finds "Pokémon". Japanese text is left intact (voiced kana keep their marks).
  Both the Japanese and the English name are searched, and a title may be given in part ("chihiro").
- **Ranking of the matches** (`SearchRanker`): exact name, exact English name, name starts with, English name
  starts with, word starts with, contains, words apart. Ties go to the more popular anime.
- **Pick-list for ambiguous titles.** A title with several matches ("cowboy") shows the candidates below the
  search box instead of failing or silently choosing one. The exact title still wins when it is unique
  ("Naruto" goes to *Naruto*, not to *Naruto (2023)*, which only shares the English name).
- **"Did you mean" for titles that are not found.** A typo in one word ("Cowboy Bepop") offers the anime
  that contain the longest word of the query.
- **Message for an unknown result page.** `/result?id=<unknown>` now returns to the search with
  "The anime with id N was not found." This resolves the `TODO` in `ViewController.getResultPage`.
- `InputDTO` has an optional `animeId` (the picked suggestion). It is trusted only while the text field still
  holds that title; a stale id (text edited, JavaScript disabled) is ignored and the text is searched.
- Tests: `SearchTextTest`, `SearchRankerTest`, `AnimeSearchServiceTest`, `AnimeServiceFormTest`,
  `ViewControllerSearchTest` (39 tests, no database needed).

### Changed

- `AnimeService.getAnimeIdByName` delegates to the new `AnimeSearchService`.
- After a successful search the result page and the search box show the real title of the anime, not the
  spelling that was typed.
- The error paths of `POST /submit` and `POST /result/submit` now always put the form `action` into the model
  (it was missing when the page was re-rendered after an error).
- `/search/suggest` is permitted for anonymous users (`AuthConfig`), like the other search endpoints.
- `htmx` is now also loaded on `main.html` and `detail.html`, because the shared search header needs it there.

### Removed

- The exact-match repository queries `AnimeRepository.getAnimeIdByName` and `getAnimeIdByEnglishName`. They
  are replaced by the search above and could fail when two anime share a title.

### Implementation notes

- The database lookup (`AnimeSearchRepository`, JPA Criteria) uses `LIKE '%word%'` on the name and the English
  name and relies on the case/accent-insensitive column collation. The query words are reduced to letters and
  digits first, so there are no `LIKE` wildcards and all values are bound parameters. The `anime` table has
  about 25 000 rows; suggestions take roughly 20 ms. No schema change and no new index are needed.
- Untrusted input is cut to 100 characters, at most 6 words are used, results are limited (100 candidates
  loaded, 8 suggestions, 10 in the pick-list), and Thymeleaf escapes every title it prints.
- A view name returned from a controller may not carry Thymeleaf fragment parameters, therefore
  `fragments/suggestions.html` has a parameter-less entry fragment `suggestions` that reads the model and
  delegates to `list(items, notice)`.

### How it was verified

- 39 new unit tests pass (`mvn test` with JDK 25).
- The branch was built with `-Pprod,container-build-base` and run as a staging container against a copy of
  the production database with a read-only user: type-ahead, hostile input (`%`, `_`, quotes, `<script>`,
  SQL fragments), all submit flows with CSRF and session, the unknown-id message, and the page markup.
- The type-ahead, click-to-search and the pick-list were driven in a real browser.
  (Submitting with the Enter key was not covered by the browser automation, only the Search button.)

### Known issues that were not touched

These tests fail on the unmodified `last-version` as well and are unrelated to this branch:

- `AnimeScoreTest.perfectScore` expects `1.0` but gets `1.4`. `ConfigConstant.OCCURRENCE_WEIGHT` (0.7) and
  `SCORE_WEIGHT` (0.7) add up to 1.4, not to 1.
- `RecommendationEngineTest.buildAnimeOccurrencesMap` (order of the result) and
  `RecommendationEngineTest.weightAnime` (`NullPointerException`, `getAverageRating()` is `null`).
- `AnimeRecommendationAppTests.contextLoads` starts the whole application and was not run for this branch
  (the application itself was verified on the staging container described above).
