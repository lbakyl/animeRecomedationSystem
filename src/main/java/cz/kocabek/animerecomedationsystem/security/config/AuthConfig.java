package cz.kocabek.animerecomedationsystem.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class AuthConfig {

    AppAuthenticationSuccessHandler successHandler;

    public AuthConfig(AppAuthenticationSuccessHandler successHandler) {
        this.successHandler = successHandler;
    }


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(requests -> requests
                        .requestMatchers("/", "/main", "/submit", "/result", "/result/submit", "/search/suggest", "/register", "/assets/**", "/favicon.ico", "/error","/anime/*","/actuator/**")
                        .permitAll()
                        .anyRequest()
                        .authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/").loginProcessingUrl("/login")
                        .defaultSuccessUrl("/main", true)
                        .permitAll()
                        .successHandler(successHandler))
                .logout(LogoutConfigurer::permitAll).build();
    }

   
}
