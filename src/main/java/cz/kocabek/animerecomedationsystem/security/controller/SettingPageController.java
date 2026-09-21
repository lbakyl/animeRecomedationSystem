package cz.kocabek.animerecomedationsystem.security.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import cz.kocabek.animerecomedationsystem.security.dto.DeletingCheckDTO;
import cz.kocabek.animerecomedationsystem.security.dto.SettingDTO;
import cz.kocabek.animerecomedationsystem.security.service.PasswordService;
import cz.kocabek.animerecomedationsystem.security.service.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@Controller
@RequestMapping("/settings")
public class SettingPageController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SettingPageController.class);
    private static final String SETTING_FORM_ATTR = "passwordForm";
    private static final String DELETING_CHECK_ATTR = "deletingCheck";
    private static final String ERROR_DIV = "fragments/errorDiv";

    private static final String SETTING_PAGE = "settings";
    private static final String SETTING_ENDPOINT = "/settings";
    private static final String LOGIN_ENDPOINT = "/login"; // the sign-in page (the root URL opens the app now)
    private static final String CHANGEPASS_ENDPOINT = "/changePassword";
    private static final String DELETEACC_ENDPOINT = "/delete";
    private static final String MAIN_ENDPOINT = "/main";

    private final PasswordService passwordService;
    private final RegistrationService registration;

    private void initializeSettingModel(Model model) {
        if (!model.containsAttribute(SETTING_FORM_ATTR)) {
            model.addAttribute(SETTING_FORM_ATTR, new SettingDTO());
        }
        if (!model.containsAttribute(DELETING_CHECK_ATTR)) {
            model.addAttribute(DELETING_CHECK_ATTR, new DeletingCheckDTO());
        }
        model.addAttribute("changePasswordPoint", SETTING_ENDPOINT + CHANGEPASS_ENDPOINT);
        model.addAttribute("deleteAccountPoint", SETTING_ENDPOINT + DELETEACC_ENDPOINT);
        model.addAttribute("mainPoint", MAIN_ENDPOINT);
    }

    @GetMapping
    public String getSettingPage(Model model) {
        initializeSettingModel(model);
        return SETTING_PAGE;
    }

    @PostMapping(CHANGEPASS_ENDPOINT)
    public String changePassword(@Valid @ModelAttribute(SETTING_FORM_ATTR) SettingDTO settingForm, BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute(DELETING_CHECK_ATTR, new DeletingCheckDTO());
            return SETTING_PAGE;
        }
        if (!passwordService.checkPassword(settingForm.oldPass())) {
            result.rejectValue("oldPass", "error.nonMatchingPassword", "your Current Password don't match. Try again");
            model.addAttribute(DELETING_CHECK_ATTR, new DeletingCheckDTO());
            return SETTING_PAGE;
        }
        passwordService.changePassword(settingForm);
        redirectAttributes.addFlashAttribute("successMessage", "Your password was changed successfully.");
        return "redirect:" + LOGIN_ENDPOINT;
    }

    /*HTMX method for returning just the error div into page or deleting account and HTMX redirecting to the login page */
    @PostMapping(DELETEACC_ENDPOINT)
    public String deleteAccount(@Valid @ModelAttribute(DELETING_CHECK_ATTR) DeletingCheckDTO deletingCheckDTO, BindingResult result, Model model, HttpServletRequest request, HttpServletResponse response) {
        if (result.hasErrors()) {
            return ERROR_DIV;
        }
        if (!passwordService.checkPassword(deletingCheckDTO.password())) {
            result.rejectValue("password", "error.nonMatchingPassword", "you put the wrong password.");
            return ERROR_DIV;
        }
        try {
            registration.deleteCurrentUser();
            request.getSession().invalidate();
            response.setHeader("HX-Redirect", "/logout");
            response.setStatus(HttpServletResponse.SC_NO_CONTENT); // 204 No Content
            return null;
        } catch (IllegalStateException e) {
            LOGGER.error("Error deleting user account: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Account deletion failed. Please try again.");
            initializeSettingModel(model);
            return SETTING_PAGE;
        }
    }

}
