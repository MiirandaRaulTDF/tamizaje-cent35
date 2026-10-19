package cent35;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TamizajeApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:vue-web-tests;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class WebUiIntegrationTests {

    @Autowired
    private MockMvc mvc;

    @Test
    void rootForwardsToTheVueWelcomePage() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));
    }

    @Test
    void welcomePageUsesTheVueRootAndLocalBootstrapAssets() throws Exception {
        String html = mvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("id=\"app\"", "v-cloak",
                "href=\"/vendor/bootstrap/bootstrap.min.css\"",
                "src=\"/vendor/bootstrap/bootstrap.bundle.min.js\"",
                "type=\"module\" src=\"/js/app.js\"")
                .doesNotContainPattern("(?:src|href)\\s*=\\s*[\"'](?:https?:)?//");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/js/app.js", "/js/api.js",
            "/vendor/vue/vue.esm-browser.prod.js",
            "/vendor/bootstrap/bootstrap.bundle.min.js"
    })
    void javascriptModulesAndLibrariesAreServedBySpringBoot(String path) throws Exception {
        String javascript = mvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/javascript"))
                .andReturn().getResponse().getContentAsString();

        assertThat(javascript).isNotBlank();
        if (path.equals("/js/app.js")) {
            assertThat(javascript).contains("../vendor/vue/vue.esm-browser.prod.js",
                    "./api.js", "createApp(", "#app");
        }
    }

    @Test
    void bootstrapStylesAreServedLocally() throws Exception {
        String css = mvc.perform(get("/vendor/bootstrap/bootstrap.min.css"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/css"))
                .andReturn().getResponse().getContentAsString();

        assertThat(css).contains("Bootstrap", ".btn", ".modal");
    }

    @Test
    void frontendCanReadPatientsThroughTheSameOriginApi() throws Exception {
        mvc.perform(get("/api/pacientes").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void questionnaireForAMissingScreeningReturnsNotFound() throws Exception {
        mvc.perform(get("/api/tamizajes/9223372036854775807/cuestionario"))
                .andExpect(status().isNotFound());
    }
}
