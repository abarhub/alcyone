package org.alcyone.alcyone.logs.web;

import org.alcyone.alcyone.logs.domain.Histogram;
import org.alcyone.alcyone.logs.domain.LogPage;
import org.alcyone.alcyone.logs.service.LogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API REST de consultation des logs.
 */
@RestController
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    /** Liste des sources de logs configurées. */
    @GetMapping("/api/logs/sources")
    public List<SourceDto> sources() {
        return logService.listSources().stream()
                .map(SourceDto::from)
                .toList();
    }

    /**
     * Page de logs d'une source, avec recherche texte optionnelle.
     *
     * @param source nom de la source
     * @param q      requête (recherche + pipeline filter/select), optionnelle
     * @param from   borne basse de date (incluse), date-heure ISO, optionnelle
     * @param to     borne haute de date (exclue), date-heure ISO, optionnelle
     * @param page   index de page (0-based), défaut 0
     * @param size   taille de page, défaut = configuration backend
     */
    @GetMapping("/api/logs")
    public LogPage logs(
            @RequestParam String source,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return logService.read(source, q, from, to, page, size);
    }

    /**
     * Histogramme temporel (nombre d'entrées par tranche) pour la même requête et plage de dates.
     *
     * @param source nom de la source
     * @param q      requête (recherche + pipeline filter/select), optionnelle
     * @param from   borne basse de date (incluse), date-heure ISO, optionnelle
     * @param to     borne haute de date (exclue), date-heure ISO, optionnelle
     */
    @GetMapping("/api/logs/histogram")
    public Histogram histogram(
            @RequestParam String source,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return logService.histogram(source, q, from, to);
    }
}
