package cz.kocabek.animerecomedationsystem;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

/**
 * Guards the settings of {@code application-prod.yml} that matter behind a TLS-terminating reverse proxy
 * and for a readable production log.
 */
class ProductionConfigTest {

    private static StandardEnvironment prodEnvironment() {
        final var yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application-prod.yml"));
        final var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new PropertiesPropertySource("prod", yaml.getObject()));
        return environment;
    }

    @Test
    void hikariIsNotLoggedAtDebugLevel() {
        assertThat(prodEnvironment().getProperty("logging.level.com.zaxxer.hikari")).isNull();
        assertThat(prodEnvironment().getProperty("logging.level.root")).isEqualTo("WARN");
    }

    @Test
    void forwardedHeadersOfTheReverseProxyAreHonoured() {
        assertThat(prodEnvironment().getProperty("server.forward-headers-strategy")).isEqualTo("framework");
    }

    @Test
    void sessionCookieIsSecureAndSameSiteByDefault() {
        final var environment = prodEnvironment();
        assertThat(environment.getProperty("server.servlet.session.cookie.secure", Boolean.class)).isTrue();
        assertThat(environment.getProperty("server.servlet.session.cookie.same-site")).isEqualTo("lax");
    }

    @Test
    void secureCookieCanBeSwitchedOffExplicitlyForLocalTesting() {
        final var environment = prodEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("env", Map.of("SESSION_COOKIE_SECURE", "false")));
        assertThat(environment.getProperty("server.servlet.session.cookie.secure", Boolean.class)).isFalse();
    }
}
