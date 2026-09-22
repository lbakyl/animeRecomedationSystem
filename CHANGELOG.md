# Changelog

Changes of the `fix-search` branch of the fork `lbakyl/animeRecomedationSystem` on top of the author's
`last-version` branch (last commit `bfc2110`, 11 Aug 2026). The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

**About the version numbers.** `pom.xml` still says `0.0.1` and no git tags exist. The numbers below are proposed
release numbers: every time the code was put on the live site (https://anime.bachelor-tech.com) counts as a
release, so each version is one deployment. They are here to talk about the changes; rename or drop them as you
like. All times are CEST (UTC+2) as in `git log`, deployment times are given in UTC as well.

## Release history at a glance

| Version | Released (live site) | Commits | What it is |
|---|---|---|---|
| [0.6.0](#060) | 2026-09-22 20:43 CEST (18:43 UTC) | 1 | Advanced options: real Genres/Type filters and Content Filters |
| [0.5.0](#050) | 2026-09-21 23:15 CEST (21:15 UTC) | 1 | Recommendation cards: aligned AVG/MAL badges, wider meter, equal poster height |
| [0.4.0](#040) | 2026-09-21 23:01 CEST (21:01 UTC) | 2 | Signing in is optional: the site opens in the app, menu has Sign In / Sign Up; release history |
| [0.3.1](#031) | 2026-09-21 22:47 CEST (20:47 UTC) | 2 | Dark text in the search box |
| [0.3.0](#030) | 2026-09-21 22:03 CEST (20:03 UTC) | 3 | Custom error page, unknown anime is a 404 |
| [0.2.0](#020) | 2026-09-21 17:55 CEST (15:55 UTC) | 4 | Favicon, static file caching, mobile menu fix, Thymeleaf warning |
| [0.1.0](#010) | 2026-09-21 00:07 CEST (2026-09-20 22:07 UTC) | 3 | Search rewrite and production clean-up |

16 commits (as of `f6f45ad`), 63 files changed (27 new, 36 modified, none deleted), +2582 / -142 lines, of which
+1028 / -130 in `src/main` and +1013 / -1 in `src/test` (11 new test classes, 80 tests). The rest is documentation.

## For the call: what to look at

### Changes that touch existing code
- **`InputDTO` record has a 5th component `Long animeId`** (0.1.0). Everything that creates an `InputDTO` must pass
  it (`RecommendationConfig` was adjusted, `null` = title just typed).
- **`AnimeService`** takes an `AnimeSearchService` in its constructor and has a new `getAnimeIdForForm`;
  `getAnimeIdByName` now delegates to the search. The exact-match repository methods
  `AnimeRepository.getAnimeIdByName` / `getAnimeIdByEnglishName` were **removed** (0.1.0).
- **`ViewController`** got a new dependency (`AnimeSearchService`, it is `@AllArgsConstructor`), the new endpoint
  `GET /search/suggest`, and a different handling of the "not found / several matches" cases (0.1.0). The unknown
  `/anime/{id}` throws `ResponseStatusException(404)` instead of the `IllegalArgumentException` (0.3.0).
- **The site root changed (0.4.0):** `GET /` is a redirect to `/main`; the sign-in page moved to `GET /login`
  (`AuthConfig.loginPage("/login")`). Failed sign-in goes to `/login?error`, sign-out to `/login?logout`.
  `AuthenticationController`, `SettingPageController` (after a password change) and the registration page point
  to `/login` now. Old links such as `/?error` do not show the sign-in page any more.
- **`InputDTO` grew 3 more components: `genres`, `types`, `excludedContent`** (0.6.0), all `List<String>`, `null`
  from an empty multi-select/no checkbox checked becomes `List.of()` in a compact constructor.
  `RecommendationConfig` and every place that builds an `InputDTO` were adjusted. `AnimeOutDTO` grew `type` and
  `rating`, filled in `RecommendationService.enrichedMapByDetails` next to `genres`, i.e. before filtering runs.
  `ViewController` got one more dependency, `AnimeGenreService` (still `@AllArgsConstructor`), and two
  `@ModelAttribute` methods that run for every request the controller handles, including `/search/suggest` and
  `/remove_uiitem` (both results are `@Cacheable`, so this is a map lookup after the first request, not a query
  on every keystroke).
- **`application-prod.yml`** (0.1.0 and 0.2.0): `forward-headers-strategy: framework`, `Secure` + `SameSite=Lax`
  session cookie, no Hikari `DEBUG`, 30-day cache for static files and content-hash file names below `/assets/**`.
  Only the `prod` profile is affected.
- **Templates / CSS:** `fragments/header.html` (search box attributes, new hook classes, hidden `animeId`, the
  advanced-options genre/type selects and content-filter checkboxes are real `th:field` inputs now instead of
  static markup), `fragments/core.html` (favicon links, one more script), `fragments/menuContent.html` (guest
  menu), `main.html`/`detail.html` (htmx script), `watchlist.html` (one attribute), `auth/registration.html`
  (one link), `main.css` (a media-query block for the header, rules for the recommendation cards, the
  "coming soon" placeholder style removed), `result.html` (the meter's inline size removed, one class on two
  badges, one on their row), new `error.html` and `fragments/suggestions.html`.

### What users notice
Search finds partial and differently punctuated titles and suggests while typing; the site opens without the
sign-in page; the mobile menu works; pages load faster (cached files); friendly error pages; a favicon; dark
text in the search box; tidy recommendation cards (equal posters, aligned badges, a meter the percentage fits);
the "Genres", "Type" and "Content Filters" advanced options actually do something now.

### Outside the repository (nothing of this is in the code)
- The application runs on a small VPS (Fastcom) behind Cloudflare and nginx, with MariaDB 11.8 in Docker.
  `docs/deployment.md` and `compose.prod.yaml` describe this generically.
- **Four redundant indexes of `users_anime_score` were not created in the migrated copy of the database** (their
  columns are prefixes of other indexes, and the application's two queries use only
  `idx_users_anime_score_anime_rating_range` and `idx_critical_user_exclude_anime`): `idx_users_anime_score_anime_rating`,
  `..._user_anime`, `..._user_rating` and `idx_optimized_user_rating_fetch`. This made the database about half
  the size (7 GB -> 4 GB). The original database was not changed. Since `ddl-auto` is `none`, the schema lives in
  the database, not in the code.

### Questions for the author
1. Should this go back as one pull request into `last-version`, or split by topic? The commits are already
   separable: search (0.1.0), production settings (0.1.0/0.2.0), UI fixes (0.2.0, 0.3.1), error pages (0.3.0),
   optional sign-in (0.4.0).
2. **Three tests fail on his unmodified `last-version` too** (see "Known issues"). The README documents the score
   weights 0.7 + 0.7, so the tests are probably outdated. Which one is right?
3. Is the new `InputDTO` signature and the removal of the exact-match repository methods acceptable?
4. Optional sign-in: is `/` -> `/main` and the sign-in page at `/login` what he wants?
5. The sign-in form has a "Remember me" checkbox (checked by default), but `AuthConfig` does not enable
   remember-me, so it looks like it does nothing (read from the code, not tested).
6. The favicon is a placeholder (a yellow star), a real logo can replace two files.
7. `management.endpoints.web.exposure.include` has `health,info,metrics,prometheus` in the prod profile. The
   reverse proxy hides everything but `health`, but when the application is reached directly the metrics are
   open. Restrict to `health` unless Prometheus is used?
8. The `users_anime_score.anime title` column holds a copy of the anime name on all 24 million rows (about
   440 MB) and is mapped by the entity, so it was left as it is.
9. **Genre pills stick out of the recommendation cards on mid-size screens (existing, not caused by this
   branch).** With 4 cards per row at 992-1199 px (and 3 per row at 768-991 px) a card is only about 216 px
   wide, and the genre pills ("AWARD WINNING", "SUPERNATURAL") overshoot the card body by up to 32 px on 33 of
   50 cards. The left column (meter + DETAILS button) is 91 px wide because of the DETAILS button, so the old
   and the new meter width make no difference. A fix would be the breakpoints in `result.html`:
   `col-sm-6 col-md-4 col-lg-3` -> `col-sm-6 col-lg-4 col-xl-3` (2 cards per row on tablets, 3 on small
   desktops, 4 from 1200 px). It changes how many cards fit in a row, so it was not done without asking.
10. **"Exclude Adult Content" and "Exclude Ecchi" (0.6.0) assume a data convention that is not written down
    anywhere in the code**: adult content = `Anime.rating` starting with `"Rx"` (MyAnimeList's "Rx - Hentai"
    tier), ecchi = the anime has the genre named exactly `"Ecchi"`. Both matched real data on the production
    copy (see 0.6.0 below), but not perfectly: selecting the genre `Hentai` alone returns 50 anime (the display
    cap; the true candidate pool is at least that many), and enabling "Exclude Adult Content" on top of that
    still leaves 3, i.e. those 3 are tagged `Hentai` but their `rating` is something other than `"Rx"`. Is
    `rating` reliable enough for this, or should "adult" also fall back to genre (`Hentai`, `Erotica`) the way
    "ecchi" does?

---

## [0.6.0]
<a id="060"></a>

**Released:** 2026-09-22 20:43 CEST (18:43 UTC). Image built from `f6f45ad`. 80 tests pass.

### Advanced options: real Genres/Type filters and Content Filters
The "Advanced Options" panel already had a "Genres" list, a "Type" list and two content-filter checkboxes,
under a "WIP Coming soon" label. They were real `<select>`/`<input>` elements with names, but no `th:field`, so
submitting the form silently dropped whatever was picked.

- **`f6f45ad`** (2026-09-22 20:41, 20 files, +403 / -76) *Advanced options: real Genres/Type filters and Content
  Filters*
  - Added: **"Genres"** and **"Type"** now list the real values from the database (`GenreRepository`, a new
    `AnimeRepository.findDistinctTypes()`) instead of 5 hardcoded genres and a fixed TV/Movie/OVA/ONA list, and
    selecting one or more filters the recommendations the same way the existing "only same genres as input
    anime" checkbox already did (`AnimePreprocessingService`). Multiple selections are OR'd within a list (any
    selected genre matches); genres and type are two separate, ANDed filters.
  - Added: **"Exclude Adult Content"** (checked by default, matching the existing markup) and **"Exclude
    Ecchi"** now do something. Both post into one `excludedContent` list (`"adult"`, `"ecchi"`) rather than two
    independent checkboxes, so a checkbox left unchecked does not need Thymeleaf's `_fieldName` hidden-input
    convention to bind correctly to the `InputDTO` record (the existing `onlyInAnimeGenres` checkbox does need
    it, and every test that posts to `/submit` already has to pass `_onlyInAnimeGenres=on` because of it).
  - Changed: `AnimeOutDTO` carries `type` and `rating` now, fetched in one bulk query per recommendation run
    (`AnimeService.getTypeAndRatingByIds`, alongside the existing bulk genre fetch) before the "Genres"/"Type"/
    "Content Filters" predicates run in `AnimePreprocessingService`.
  - Fixed along the way: some anime rows have `Type = ''` (empty string, not `NULL`) in the data, which showed
    up as one nameless blank option in the "Type" list; `findDistinctTypes()` excludes it.
  - `main.css`: the `.coming-soon-features` dashed-border placeholder style is gone, nothing uses it any more.
  - Tests: `AnimePreprocessingServiceTest` (7, the 4 new filters and their combination with the existing one),
    `AnimeGenreServiceTest`, `InputDTOTest` (the null-safety of the 3 new record components), one more assertion
    in `TemplatesAndAssetsTest`, `AnimeServiceFormTest` (2 more).
  - Not changed: the "Quick Presets" buttons and the min-rating/max-users sliders (untouched by this commit).

**Verified:** on a staging copy of the production database. Unit tests use a synthetic 4-anime map (one match
per filter, one with no genre/type/rating at all, to check nothing throws on missing data). On real data:
searching "Naruto" with genre `Action` selected returned 50 anime, all tagged `ACTION`; adding type `TV`
dropped titles such as "Mononoke Hime" and "Naruto Movie 1" (both Movies) from the list, clearing the type
filter brought them back. Selecting genre `Ecchi` alone returned only `ECCHI`-tagged anime; turning on "Exclude
Ecchi" **while genre `Ecchi` was still selected** correctly returned **zero** anime (a deliberate contradiction:
require the genre and exclude it). Selecting genre `Hentai` returned anime with an `"Rx - Hentai"` rating;
turning on "Exclude Adult Content" dropped that list from 50 (the display cap) to 3 (see question 10 above for
what those 3 are). Checked the live app log after deploying: clean startup, no `ERROR`/`WARN` lines.

## [0.5.0]
<a id="050"></a>

**Released:** 2026-09-21 23:15 CEST (21:15 UTC). Image built from `46150f2`. 60 tests pass.

### Recommendation cards
Three things looked untidy on the result page.

- **`46150f2`** (2026-09-21 23:11, 3 files, +60 / -6) *Recommendation cards: aligned badges, wider meter, equal poster height*
  - Fixed: **AVG and MAL badges were not aligned.** Both boxes had the same size (29.9 px), but the MAL badge
    contains an icon (`fs-6`) that made its content taller and pushed its text 4.6 px lower than the AVG
    text. Both badges are one flex box now (`.stat-badge`, `inline-flex`, centred, fixed min-height, common
    font size and `line-height: 1`), and their row is `align-items-center`. Text offset: 4.6 px -> 0.
  - Fixed: **the "Rated by" meter was too narrow.** It was fixed inline to 50 px (`style="width: 50px"`), a bold
    "72.01%" is 46 px wide, so 2 px were left on each side. The size is in CSS now (`.vertical-progress`, 5rem
    = 80 px, at least 16.9 px around the text) and the bar has a `min-height` of 2.75rem, so its two lines of text
    ("72.01%" and "users") also fit tiny values.
  - Fixed: **posters had different heights.** The link around the poster is a fixed 2:3 frame, but the picture
    inside kept its own proportions (0.64 - 0.75 in the 48 loaded pictures), which gave 19 different picture
    heights (345-403 px) and gaps below the shorter ones. The picture fills the frame now
    (`object-fit: cover`, `object-position: center top`), all posters are equally tall (389 px at 1280 px) and
    are cropped by a few percent at most.
  - Test: `TemplatesAndAssetsTest` (badges share the class, no inline meter width, the css rules exist).
  - Not changed: the DETAILS badge, the watchlist page (it has a MAL badge only, no AVG partner).

**Verified:** measured in a browser on all 50 cards of `/result?id=20`, first as a prototype in the page, then
on the staging container and on the live site: badge text offset 0 px, all badge boxes 32 px, all posters
389 px high with no gap, meter 80 px, no genre pill outside its card at 1280 px and 375 px. At 992-1199 px the
genre pills still stick out of the cards, exactly as before (question 9 above).

## [0.4.0]
<a id="040"></a>

**Released:** 2026-09-21 23:01 CEST (21:01 UTC). Image built from `2779ca4`. 59 tests pass. (The changelog commit `a63bc09` below
came afterwards and is not part of the deployed image.)

### Signing in is optional
The sign-in page was the site root, so every visitor met a login form first although most people do not want an
account.

- **`2779ca4`** (2026-09-21 22:58, 8 files, +124 / -7) *Open the site in the application; sign-in becomes optional*
  - `GET /` redirects to `/main`. The sign-in page moved to `GET /login` (`AuthenticationController`), and Spring
    Security's `loginPage` is `/login` (`AuthConfig`); failed sign-ins (`/login?error`) and sign-outs
    (`/login?logout`) follow because they are derived from it. Pages that need an account send anonymous
    visitors to `/login`.
  - Guest menu (`fragments/menuContent.html`): **Sign In** (`/login`, was `/`) and the new **Sign Up**
    (`/register`).
  - The account-created message and the password-changed message were shown on the sign-in page after a redirect to
    `/`. They redirect to `/login` now, so they are not lost (`AuthenticationController`,
    `SettingPageController`). "Back to Sign In" on the registration page links to `/login`.
  - `readme.md`: endpoint list.
  - Tests: `AuthenticationControllerTest` (6), a menu-link test in `TemplatesAndAssetsTest`.
  - Not changed: the sign-in page itself (it still offers "Continue as Guest" and "Sign Up"); after signing out
    the user lands on the sign-in page as before.
- **`a63bc09`** (23:03, 1 file, +261 / -188) `CHANGELOG.md` rewritten as a release history (versions, dates, every
  commit, impact on existing code, questions for the author).

**Verified:** built from the commit and run as a staging container against a copy of the production database:
`/` -> 302 `/main`; `/login` 200; `/watchlist` and `/settings` -> 302 `/login`; wrong password -> `/login?error`
with the message; `POST /logout` -> `/login?logout`; the menu shows both items (also in a browser at 375 px);
registration validation still works. A successful sign-in was not tried (that needs an account, and the
staging copy may not write to the database); the success handler is unchanged. On the live site the same
checks pass.

## [0.3.1]
<a id="031"></a>

**Released:** 2026-09-21 22:47 CEST (20:47 UTC). Image `50f3747`. 52 tests.

- **`50f3747`** (22:45, 2 files, +12 / -1) *Search box: dark text instead of white on the light field*
  - Fixed: the search field has a light grey background but used `text-white`, so the typed text was hard to
    read. It is `text-dark` now (`rgb(38,38,38)` on `rgb(229,229,229)`, contrast 12:1). It was the only input with
    a white text class. Test in `TemplatesAndAssetsTest`.
- **`b4b6359`** (22:47, 1 file, +4 / -1) `CHANGELOG.md` entry.

## [0.3.0]
<a id="030"></a>

**Released:** 2026-09-21 22:03 CEST (20:03 UTC). Image `fadf25d`. 51 tests.

- **`b68ab78`** (21:58, 4 files, +125 / -1) *Custom error page; unknown anime detail page is a 404*
  - Fixed: every error (403, 400, 404, 500) showed Spring Boot's "Whitelabel Error Page". A common way to see it:
    keep a page open until the session is gone, then reload it; the form is re-sent, the CSRF token does not
    match, the answer is 403. New `templates/error.html` in the look of the other pages: "Your session has
    expired" (403), "We could not find that page" (404), "That link does not look right" (400), "Something went
    wrong on our side" (everything else), and a button back to the search. It needs no session, CSRF token or
    search form, does not echo the requested path, and the HTTP status is unchanged. JSON clients still get JSON.
  - Fixed: `/anime/{id}` with an unknown id answered **500** and wrote an `ERROR` with a stack trace to the log
    (`IllegalArgumentException`). It is a 404 (`ResponseStatusException`) now.
  - Tests: unknown anime -> 404 (`ViewControllerSearchTest`), error page structure (`TemplatesAndAssetsTest`).
- **`aa776d8`** (22:00, 1 file, +1 / -1) The error number is hidden when the status is not a real HTTP error (a
  direct call of `/error` printed "Error 999").
- **`fadf25d`** (22:02, 2 files, +17 / -2) Neutral page title; changelog.

**Verified:** each error triggered as a browser would (`Accept: text/html`) on the staging container; pages
viewed in a browser at desktop and 375 px; the log for an unknown anime has no `ERROR` and no stack trace.

## [0.2.0]
<a id="020"></a>

**Released:** 2026-09-21 17:55 CEST (15:55 UTC). Image `2eccb88`. 49 tests.

- **`3cdb275`** (10:01, 7 files, +98 / -2) *Fix Thymeleaf deprecation warning, add favicon*
  - Fixed: `watchlist.html` used the unwrapped fragment expression `fragments/htmxFragment :: htmxFragment`,
    which made Thymeleaf log a `WARN` on every render of the watchlist and is to be removed in a future
    version. It is `~{...}` now; it was the only occurrence.
  - Added: favicon. Browsers ask for `/favicon.ico` on every visit and got a 404 (a redirect to the login page
    when not signed in). Placeholder icon (yellow rating star on a dark rounded square): `static/favicon.ico`
    (16, 32, 48 px) and `static/assets/image/favicon.svg`, linked in the shared head (`fragments/core.html`);
    `/favicon.ico` is permitted anonymously in `AuthConfig`.
  - Tests: `TemplatesAndAssetsTest` (no template may use the deprecated syntax; favicon files are valid and
    linked).
- **`148d161`** (10:02, 1 file, +7 / -1) Changelog: verification notes.
- **`2eccb88`** (17:51, 5 files, +89 / -5) *Enable browser caching of static files, fix the mobile menu layout*
  - Fixed: **static files were never cached.** Spring Security sends `Cache-Control: no-store` for every
    response, so browsers downloaded the CSS (440 KB), the scripts and the 2.3 MB header photo again on every
    page view and Cloudflare could not cache them. In the `prod` profile static files get
    `Cache-Control: max-age=2592000, public` (30 days), and everything below `/assets/**` is served under a
    name with a content hash (`main-<hash>.css`; `@{...}` links and the `url(...)` in the CSS are rewritten
    automatically), so a changed file gets a new URL and is never stale. HTML pages keep `no-store`, old
    unhashed URLs still work.
  - Fixed: **mobile menu opened off-screen.** Below the `lg` breakpoint (992 px) the hamburger button and the
    opened menu sat in a narrow flex column beside the title; the menu card was wider than the space left, so
    the page became wider than the screen (375 -> 538 px), the title was cut off and the menu pushed out on the
    right. `header.html` got hook classes (`header-row`, `header-spacer`, `header-title`, `header-actions`) and
    `main.css` a `@media (max-width: 991.98px)` block: two-column grid (title | hamburger), the opened menu
    takes the full width of the second row, title 1.6rem. Screens of 992 px and more are unchanged.
  - Tests: cache settings in `ProductionConfigTest`, header hooks in `TemplatesAndAssetsTest`.
- **`5a80716`** (17:55, 1 file, +24 / -1) Changelog.

**Verified:** on staging, HTML `no-store`, every asset hashed with `max-age=2592000, public` and no cookies,
the header photo referenced by its hashed name, old URLs answer 200, CSS revalidation answers 304. (Files that
carry an ETag, the small scripts and the photo, answer a conditional request with the full file instead of 304:
correct, but not optimal; it only happens after 30 days or on a hard reload.) Mobile menu measured in a browser
at 320, 375, 414 and 991 px: no overflow with the menu and dropdown open; at 992 and 1280 px positions and
sizes are identical to the previous version. On the live site Cloudflare serves the files from its cache and
a repeat visit downloads 0 bytes.

## [0.1.0]
<a id="010"></a>

**Released:** 2026-09-21 00:07 CEST (2026-09-20 22:07 UTC). Image `1d08c8d`. 43 tests.

### Why
Searching for an anime required the **complete title, typed exactly**: the app ran `where name = ?` and then
`where englishName = ?`. Anything else ("Cowboy", "Your Name" for the stored "Your Name.", "steins gate" for
"Steins;Gate") ended with "This anime was not found", although the anime is in the database, and a failed
search gave no hint what to try. The clean-up came from putting the application on a new server behind nginx
and Cloudflare.

- **`4c1d0fa`** (2026-09-20 23:42, 24 files, +1161 / -26) *Improve anime search: type-ahead, tolerant matching, pick-list*
  - Added: type-ahead. After two characters the search box shows up to 8 matching titles
    (`GET /search/suggest`, htmx, 250 ms debounce, `fragments/suggestions.html`, `searchSuggest.js`). Picking one
    searches for exactly that anime by its id, so the slider settings are kept and same-named anime cannot be
    confused.
  - Added: tolerant matching (`recommendation/search/SearchText`). Case, Latin accents and punctuation are
    ignored, punctuation counts as a word break ("Your Name" finds "Your Name.", "steins gate" finds
    "Steins;Gate", "pokemon" finds "Pokémon"); Japanese text is left intact. Japanese and English names are
    searched, and a part of a title is enough ("chihiro").
  - Added: ranking (`SearchRanker`): exact name, exact English name, name starts with, English name starts
    with, word starts with, contains, words apart; ties go to the more popular anime.
  - Added: a pick-list for several matches ("cowboy"), "did you mean" for a typo in one word ("Cowboy
    Bepop"); the exact title still wins when it is unique ("Naruto" goes to *Naruto*, not to *Naruto (2023)*).
  - Added: `/result?id=<unknown>` returns to the search with "The anime with id N was not found." (resolves the
    `TODO` in `ViewController.getResultPage`).
  - Added classes: `AnimeSearchRepository` (JPA Criteria, `LIKE '%word%'`, bound parameters, letters and digits
    only), `AnimeSearchService`, `AnimeSearchException`, `AnimeSuggestionDto`.
  - Changed: `InputDTO` has an optional `animeId` (trusted only while the text field still holds that title);
    `AnimeService` delegates to the search; `ViewController` (new dependency, the error paths of `POST /submit`
    and `POST /result/submit` now always put the form `action` into the model, the result page shows the real
    title, not the typed spelling); `/search/suggest` permitted anonymously (`AuthConfig`); htmx is also loaded
    on `main.html` and `detail.html`; `RecommendationConfig` adjusted to the new `InputDTO`.
  - Removed: `AnimeRepository.getAnimeIdByName` and `getAnimeIdByEnglishName` (could fail when two anime share a
    title).
  - Limits: input cut to 100 characters, at most 6 words, 100 candidates loaded, 8 suggestions, 10 in the
    pick-list; Thymeleaf escapes every title. The `anime` table has about 25 000 rows, suggestions take about
    20 ms; no schema change or new index is needed.
  - A view name returned from a controller may not carry Thymeleaf fragment parameters, hence the
    parameter-less entry fragment `suggestions` that delegates to `list(items, notice)`.
  - Tests: `SearchTextTest`, `SearchRankerTest`, `AnimeSearchServiceTest`, `AnimeServiceFormTest`,
    `ViewControllerSearchTest` (39 tests, no database needed). First version of `CHANGELOG.md`.
- **`1d08c8d`** (2026-09-20 23:58, 8 files, +380 / -23) *Production clean-up: proxy headers, secure cookie, quieter log, docs*
  - Fixed: **redirect to `http://` after a search.** The application ignored `X-Forwarded-Proto: https`, so the
    redirect after `POST /submit` pointed to `http://...` and browsers showed "This site doesn't support a
    secure connection". `server.forward-headers-strategy: framework` in `application-prod.yml`.
  - Fixed: the session cookie is `Secure` and `SameSite=Lax` in the production profile (`HttpOnly` was already
    the default); `SESSION_COOKIE_SECURE=false` allows trying the prod profile over plain http locally.
  - Fixed: noisy log. `logging.level.com.zaxxer.hikari: DEBUG` printed the pool housekeeping every 30 s and the
    timing of every recommendation step was logged as `WARN`. Hikari is back to the default level, the step
    timings (`UserAnimeScoreService`, `RecommendationService`) are `DEBUG`
    (`LOGGING_LEVEL_CZ_KOCABEK_ANIMERECOMEDATIONSYSTEM=DEBUG` shows them).
  - Added: `compose.prod.yaml` (MariaDB 11.8 + the application, ports not public, secrets from files; the
    database saves its whole buffer pool on shutdown and gets 120 s to stop, so a restart keeps its cache),
    `docs/deployment.md` (configuration, data load, nginx with TLS, rate limits, restricted actuator,
    Cloudflare, update and rollback, backup), README versions/endpoints/deployment (the referenced
    `docker-compose.prod.yml` never existed), `ProductionConfigTest` (4 tests).
- **`a03cdda`** (2026-09-21 00:06, 2 files, +17 / -2) README corrected to Spring Boot 3.5.13 (the version in his
  `pom.xml`); verification notes in the changelog.

**Verified:** the commits were built with `-Pprod,container-build-base` and run as a staging container against
a copy of the production database with a read-only user: type-ahead, hostile input (`%`, `_`, quotes,
`<script>`, SQL fragments), all submit flows with CSRF and session, the unknown-id message; the type-ahead,
click-to-search and the pick-list in a real browser (the Enter key was not covered by the browser
automation, only the Search button). The data was compared with the original database (row counts and
`CHECKSUM TABLE` identical). The deployment moved the running application from Spring Boot 3.5.7 to the
3.5.13 that `last-version` already uses.

---

## Known issues that were not touched

These tests fail on his unmodified `last-version` as well and are unrelated to this branch:

- `AnimeScoreTest.perfectScore` expects a score of `1.0` but gets `1.4`. `ConfigConstant.OCCURRENCE_WEIGHT` (0.7)
  and `SCORE_WEIGHT` (0.7) add up to 1.4; the README documents these weights, so most likely the test is
  outdated. Either the test or the weights need a decision by the author.
- `RecommendationEngineTest.buildAnimeOccurrencesMap` (order of the result) and
  `RecommendationEngineTest.weightAnime` (`NullPointerException`, `getAverageRating()` is `null`).
- `AnimeRecommendationAppTests.contextLoads` starts the whole application and was not run for this branch (the
  application itself was verified on the staging containers described above).
