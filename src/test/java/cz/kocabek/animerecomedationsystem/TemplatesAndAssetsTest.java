package cz.kocabek.animerecomedationsystem;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Static checks of the templates and the favicon; no application context is started.
 */
class TemplatesAndAssetsTest {

    /**
     * th:insert / th:replace / th:include whose value is a bare "template :: fragment" instead of "~{...}".
     * Thymeleaf logs a deprecation WARN for it on every render and will drop the syntax in a future version.
     * Variable expressions such as ${title} are fine.
     */
    private static final Pattern UNWRAPPED_FRAGMENT =
            Pattern.compile("th:(?:insert|replace|include)=\"(?!~\\{|\\$\\{)[^\"]*\"");

    @Test
    void noTemplateUsesTheDeprecatedUnwrappedFragmentSyntax() throws IOException {
        final var templates = new PathMatchingResourcePatternResolver().getResources("classpath:templates/**/*.html");
        assertThat(templates).isNotEmpty();

        final List<String> offenders = new ArrayList<>();
        for (final var template : templates) {
            final var matcher = UNWRAPPED_FRAGMENT.matcher(template.getContentAsString(StandardCharsets.UTF_8));
            while (matcher.find()) {
                offenders.add(template.getFilename() + ": " + matcher.group());
            }
        }
        assertThat(offenders).isEmpty();
    }

    @Test
    void faviconIcoIsAValidIconFileWithSeveralSizes() throws IOException {
        final var bytes = new ClassPathResource("static/favicon.ico").getContentAsByteArray();
        final var icon = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

        assertThat(icon.getShort(0)).as("reserved").isZero();
        assertThat(icon.getShort(2)).as("type 1 = icon").isEqualTo((short) 1);
        final int count = icon.getShort(4);
        assertThat(count).isGreaterThanOrEqualTo(2);

        for (int i = 0; i < count; i++) {
            final int entry = 6 + 16 * i;
            final int length = icon.getInt(entry + 8);
            final int offset = icon.getInt(entry + 12);
            assertThat(offset + length).as("image %d lies inside the file", i).isLessThanOrEqualTo(bytes.length);
            // the images are PNG compressed: 0x89 'P' 'N' 'G'
            assertThat(bytes[offset] & 0xFF).isEqualTo(0x89);
            assertThat(new String(bytes, offset + 1, 3, StandardCharsets.US_ASCII)).isEqualTo("PNG");
        }
    }

    @Test
    void faviconSvgExists() throws IOException {
        final var svg = new ClassPathResource("static/assets/image/favicon.svg").getContentAsString(StandardCharsets.UTF_8);
        assertThat(svg).contains("<svg").contains("</svg>");
    }

    @Test
    void theSharedHeadOfEveryPageLinksTheFavicon() throws IOException {
        final var head = new ClassPathResource("templates/fragments/core.html").getContentAsString(StandardCharsets.UTF_8);
        assertThat(head).contains("@{/favicon.ico}").contains("@{/assets/image/favicon.svg}");
    }
}
