package cz.kocabek.animerecomedationsystem.security.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import cz.kocabek.animerecomedationsystem.security.service.RegistrationService;

/**
 * The site opens in the application; the sign-in page is optional and lives at /login.
 */
class AuthenticationControllerTest {

    private RegistrationService registration;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        registration = mock(RegistrationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthenticationController(registration)).build();
    }

    @Test
    void rootUrlOpensTheApplicationInsteadOfTheSignInPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/main"));
    }

    @Test
    void signInPageLivesAtLogin() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/index"));
    }

    @Test
    void registerPageShowsTheForm() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/registration"))
                .andExpect(model().attributeExists("account"));
    }

    @Test
    void newAccountLeadsToTheSignInPageWithASuccessMessage() throws Exception {
        when(registration.registerNewUser(any())).thenReturn(true);

        mockMvc.perform(post("/register").param("username", "newuser1").param("password", "Abcdef1!xyz"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void takenUsernameStaysOnTheRegistrationFormWithAnError() throws Exception {
        when(registration.registerNewUser(any())).thenReturn(false);

        mockMvc.perform(post("/register").param("username", "newuser1").param("password", "Abcdef1!xyz"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/registration"))
                .andExpect(model().attributeHasFieldErrors("account", "username"));
    }

    @Test
    void invalidInputIsRejectedBeforeAnAccountIsCreated() throws Exception {
        mockMvc.perform(post("/register").param("username", "ab").param("password", "short"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/registration"))
                .andExpect(model().attributeHasFieldErrors("account", "username", "password"));
        org.mockito.Mockito.verifyNoInteractions(registration);
    }
}
