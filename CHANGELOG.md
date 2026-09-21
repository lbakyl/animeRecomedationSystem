# Changelog

All notable changes of the `fix-search` branch are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased] - fix-search

Base: `last-version` (11 Aug 2026).

This branch contains two groups of changes: the **search improvement** (first section, most of the text
below) and **production clean-up** found while moving the application to a new server (last section).

### Why (search)

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

## Production clean-up

Found while running the application in production behind nginx and Cloudflare on a new server.

### Fixed

- **Redirect to `http://` after a search.** The application ignored the `X-Forwarded-Proto: https` header of
  the reverse proxy, so the redirect after `POST /submit` pointed to `http://...` and browsers showed
  "This site doesn't support a secure connection". `application-prod.yml` now sets
  `server.forward-headers-strategy: framework`.
- **Session cookie** is now `Secure` and `SameSite=Lax` in the production profile (`HttpOnly` was already the
  default). `SESSION_COOKIE_SECURE=false` switches `Secure` off to try the prod profile over plain http locally.
- **Noisy production log.** `logging.level.com.zaxxer.hikari: DEBUG` in `application-prod.yml` printed the
  connection pool housekeeping every 30 seconds, and the timing of every recommendation step was logged as
  `WARN`, which made healthy requests look like problems. Hikari is back to the default level and the step
  timings (`UserAnimeScoreService`, `RecommendationService`) are `DEBUG`; use
  `LOGGING_LEVEL_CZ_KOCABEK_ANIMERECOMEDATIONSYSTEM=DEBUG` to see them again.
- **Thymeleaf deprecation warning.** `watchlist.html` used the unwrapped fragment expression
  `fragments/htmxFragment :: htmxFragment`, which made Thymeleaf log a `WARN` on every render of the watchlist
  page and is to be removed in a future Thymeleaf version. It is now `~{fragments/htmxFragment :: htmxFragment}`.
  It was the only occurrence in the templates.

### Added

- `compose.prod.yaml`: MariaDB 11.8 and the application, ports not published publicly, secrets from files.
  The database keeps its cache across restarts: MariaDB already saved a quarter of the buffer pool on shutdown
  (`innodb_buffer_pool_dump_pct=25`) and Docker gave it 10 s to stop; now the whole pool is saved and the
  shutdown may take up to 120 s. This should keep the first search after a database restart fast.
- `docs/deployment.md`: production guide (configuration, data load, nginx with TLS, rate limits and a restricted
  actuator, Cloudflare notes, update and rollback, backup).
- `ProductionConfigTest` keeps the settings above from being lost again.
- **Favicon.** Browsers ask for `/favicon.ico` on every visit and got a 404 (or, when not logged in, a redirect to
  the login page). Added a simple placeholder icon (a yellow rating star on a dark rounded square, the yellow is
  the accent colour already used by the sliders): `static/favicon.ico` (16, 32 and 48 px) and
  `static/assets/image/favicon.svg`, linked in the shared `<head>` (`fragments/core.html`) so every page gets it,
  and `/favicon.ico` is permitted for anonymous visitors in `AuthConfig`. Replace the two files to use a real logo.
- `TemplatesAndAssetsTest`: no template may use the deprecated unwrapped fragment syntax again, and the favicon
  files and their links must exist and be valid.

### Changed

- `readme.md`: versions (Spring Boot 3.5.13, Java 25), MariaDB in production, the endpoints and the deployment
  section (the referenced `docker-compose.prod.yml` never existed).

### How the clean-up was verified

- 47 unit tests pass, including `ProductionConfigTest` and `TemplatesAndAssetsTest`.
- The commit was built with `-Pprod,container-build-base` and started as a staging container **without** any
  extra environment variable, against a copy of the production database:
  - with the headers of the reverse proxy (`Host`, `X-Forwarded-Proto: https`) the redirect after a search is
    `https://...`;
  - the session cookie is `Secure; HttpOnly; SameSite=Lax`;
  - the container log at default level is about 17 readable lines (no Hikari `DEBUG`, no step timings, no
    `WARN`), and `LOGGING_LEVEL_CZ_KOCABEK_ANIMERECOMEDATIONSYSTEM=DEBUG` brings the step timings back;
  - all search flows of the first section still behave the same.
- `compose.prod.yaml` passes `docker compose config`.
- Favicon on the staging container: `/favicon.ico` answers 200 (`image/x-icon`, valid ICO) without logging in, the
  SVG is served, and every page (including the login page) links both. The search flows and the log (0 `WARN`) are
  unchanged.
- The Thymeleaf warning only appears when the watchlist page is rendered for a logged-in user, which was not
  reproduced on staging (no test account). Instead the pattern used by `TemplatesAndAssetsTest` was checked to
  flag the old `watchlist.html` (the exact expression from the production log) and to pass the fixed one.
- Note: this branch is based on `last-version`, which already uses Spring Boot 3.5.13. An installation that was
  built from older code (3.5.7) is upgraded to it when it deploys this branch.

## Known issues that were not touched

These tests fail on the unmodified `last-version` as well and are unrelated to this branch:

- `AnimeScoreTest.perfectScore` expects a score of `1.0` but gets `1.4`. `ConfigConstant.OCCURRENCE_WEIGHT`
  (0.7) and `SCORE_WEIGHT` (0.7) add up to 1.4; the README documents these weights, so most likely the
  test is outdated. Either the test or the weights need a decision by the author.
- `RecommendationEngineTest.buildAnimeOccurrencesMap` (order of the result) and
  `RecommendationEngineTest.weightAnime` (`NullPointerException`, `getAverageRating()` is `null`).
- `AnimeRecommendationAppTests.contextLoads` starts the whole application and was not run for this branch
  (the application itself was verified on the staging container described above).
