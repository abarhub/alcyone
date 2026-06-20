package org.alcyone.alcyone.web;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Sert l'application Angular empaquetée (sous {@code META-INF/resources}) et assure le repli SPA :
 * toute route « front » (sans extension de fichier) renvoie {@code index.html} pour que le routage
 * côté client fonctionne, y compris au rafraîchissement. Les chemins {@code /api/**} et les vrais
 * fichiers statiques ne sont pas affectés.
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    private static final String STATIC_LOCATION = "classpath:/META-INF/resources/";
    private final Resource index = new ClassPathResource("META-INF/resources/index.html");

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations(STATIC_LOCATION)
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (!resourcePath.isEmpty() && requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        // Laisse l'API répondre elle-même (404 inclus) plutôt que de renvoyer le SPA.
                        if (resourcePath.startsWith("api/")) {
                            return null;
                        }
                        return index;
                    }
                });
    }
}
