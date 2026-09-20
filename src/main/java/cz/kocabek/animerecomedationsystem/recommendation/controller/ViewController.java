package cz.kocabek.animerecomedationsystem.recommendation.controller;

import cz.kocabek.animerecomedationsystem.account.service.AccService;
import cz.kocabek.animerecomedationsystem.account.service.WatchListService;
import cz.kocabek.animerecomedationsystem.recommendation.dto.InputDTO;
import cz.kocabek.animerecomedationsystem.recommendation.service.DTOResultBuilder;
import cz.kocabek.animerecomedationsystem.recommendation.search.AnimeSearchException;
import cz.kocabek.animerecomedationsystem.recommendation.service.RecommendationService;
import cz.kocabek.animerecomedationsystem.recommendation.service.db.AnimeSearchService;
import cz.kocabek.animerecomedationsystem.recommendation.service.db.AnimeService;
import cz.kocabek.animerecomedationsystem.recommendation.service.recommendationconfig.RecommendationConfig;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@AllArgsConstructor
@Controller
public class ViewController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ViewController.class);

    private static final String INPUT_ATR_NAME = "anime";
    private static final String ATR_ACTION = "action";
    private static final String RECOMMENDATION_ATR = "recommendations";
    private static final String CANDIDATES_ATR = "candidates";
    private static final String NOTICE_ATR = "searchNotice";
    private static final String SEARCH_ERROR_ATR = "searchError";

    private static final String MAIN_PAGE = "main";
    private static final String RESULT_PAGE = "result";
    private static final String RESULT_ENDPOINT = "/" + RESULT_PAGE;
    private static final String POST_SUBMIT_ENDPOINT = "/submit";
    private static final String POST_RESULT_SUBMIT = RESULT_ENDPOINT + POST_SUBMIT_ENDPOINT;

    private final AccService accService;
    private final RecommendationService recommendationService;
    private final AnimeService animeService;
    private final AnimeSearchService searchService;
    private final DTOResultBuilder resultBuilder;
    private final RecommendationConfig config;
    private final WatchListService watchListService;

    @GetMapping("/" + MAIN_PAGE)
    public String getHomePage(Model model) {
        model.addAttribute(INPUT_ATR_NAME, config.getConfigForm());
        model.addAttribute(ATR_ACTION, POST_SUBMIT_ENDPOINT);
        return MAIN_PAGE;
    }

    @PostMapping(POST_SUBMIT_ENDPOINT)
    public String postHomePage(@Valid @ModelAttribute(INPUT_ATR_NAME) InputDTO form, BindingResult bindingResult,
                               RedirectAttributes redirectAttributes, Model model) {
        model.addAttribute(ATR_ACTION, POST_SUBMIT_ENDPOINT);
        if (bindingResult.hasErrors()) {
            return MAIN_PAGE;
        }
        try {
            Long id = processForm(form);
            redirectAttributes.addAttribute("id", id);
            return "redirect:" + RESULT_ENDPOINT;
        } catch (ValidationException e) {
            reportSearchFailure(e, bindingResult, model);
            return MAIN_PAGE;
        }
    }

    /**
     * Type-ahead: HTML fragment with the best matching titles for the text typed so far (htmx).
     */
    @GetMapping("/search/suggest")
    public String suggest(@RequestParam(name = "animeName", defaultValue = "") String query, Model model) {
        if (query.trim().length() < AnimeSearchService.MIN_SUGGEST_LENGTH) {
            return "fragments/suggestions :: none";
        }
        final var items = searchService.suggest(query);
        model.addAttribute("items", items);
        model.addAttribute("notice", items.isEmpty() ? "No titles match yet." : null);
        return "fragments/suggestions :: suggestions";
    }

    @GetMapping(RESULT_ENDPOINT)
    public String getResultPage(@RequestParam("id") Long animeId, Model model, Authentication auth,
                                RedirectAttributes redirectAttributes) {
        try {
            checkAnimeId(animeId);
        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute(SEARCH_ERROR_ATR, e.getMessage());
            return "redirect:/" + MAIN_PAGE;
        }
        final var recommendations = recommendationService.getAnimeRecommendation();
        if (auth != null) {
            watchListService.setWatchlistButtons(recommendations);
        }
        model.addAttribute(RECOMMENDATION_ATR, recommendations);
        model.addAttribute(INPUT_ATR_NAME, config.getConfigForm());
        model.addAttribute(ATR_ACTION, POST_RESULT_SUBMIT);
        return RESULT_PAGE;
    }

    @PostMapping(POST_RESULT_SUBMIT)
    public String postResultPage(@Valid @ModelAttribute("anime") InputDTO form, BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes, Model model) {
        model.addAttribute(ATR_ACTION, POST_RESULT_SUBMIT);
        if (bindingResult.hasErrors()) {
            model.addAttribute(RECOMMENDATION_ATR, resultBuilder.getResultDto());
            return RESULT_PAGE;
        }
        try {
            Long id = processForm(form);
            redirectAttributes.addAttribute("id", id);
            return "redirect:" + RESULT_ENDPOINT;
        } catch (ValidationException e) {
            reportSearchFailure(e, bindingResult, model);
            model.addAttribute(RECOMMENDATION_ATR, resultBuilder.getResultDto());
            return RESULT_PAGE;
        }
    }

    /* detail page */
    @GetMapping("/anime/{id}")
    public String getAnimePage(@PathVariable Long id, Model model) {
        final var animeDetail = animeService.getAnimeByIdWithGenres(id);
        model.addAttribute("detail", animeDetail);
        model.addAttribute(INPUT_ATR_NAME, config.getConfigForm());
        model.addAttribute(ATR_ACTION, POST_RESULT_SUBMIT);
        return "detail";
    }

    @GetMapping("/watchlist")
    public String getWatchlistPage(Model model) {
        model.addAttribute(INPUT_ATR_NAME, config.getConfigForm());
        try {
            model.addAttribute("watchlist", accService.getWatchlistData());
        } catch (IllegalStateException e) {
            LOGGER.error("some unexpected logout because of {}", e.getMessage(), e);
            return "redirect:/logout";
        }
        model.addAttribute(ATR_ACTION, POST_SUBMIT_ENDPOINT);
        return "watchlist";
    }

    /*HTMX mapping */
    @DeleteMapping("/remove_uiitem")
    @ResponseBody
    public ResponseEntity<String> removeFromWatchListPage(@RequestParam long animeId) {
        try {
            watchListService.removeFromWatchlist(animeId);
        } catch (IllegalStateException e) {
            LOGGER.error("during removing item from UI happened error:%n {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("");
        }
        return ResponseEntity.ok("");
    }

    private Long processForm(InputDTO form) throws ValidationException {
        config.updateConfig(form);
        Long id = animeService.getAnimeIdForForm(config.getAnimeName(), form.animeId());
        config.setAnimeId(id);
        // the result page shows the real title, not whatever spelling was typed
        final var title = animeService.getAnimeNameById(id);
        config.setAnimeName(title);
        config.setConfigForm(new InputDTO(title, form.minRating(), form.maxUsers(), form.onlyInAnimeGenres(), id));
        resultBuilder.init(title);
        return id;
    }

    /**
     * A title that was not found or is ambiguous is shown as a pick-list below the search box,
     * any other problem as an error message of the field.
     */
    private void reportSearchFailure(ValidationException e, BindingResult bindingResult, Model model) {
        if (e instanceof AnimeSearchException searchException) {
            model.addAttribute(NOTICE_ATR, searchException.getMessage());
            model.addAttribute(CANDIDATES_ATR, searchException.getCandidates());
        } else {
            bindingResult.rejectValue("animeName", "error.detail", e.getMessage());
        }
    }

    private void checkAnimeId(Long id) throws ValidationException {
        if (config.getAnimeId() != null && config.getAnimeId().equals(id)) {
            return;
        }
        final var name = animeService.getAnimeNameById(id);
        if (name == null) {
            throw new ValidationException("The anime with id %d was not found.".formatted(id));
        }
        config.setAnimeName(name);
        config.setAnimeId(id);
        resultBuilder.init(name);
    }
}
