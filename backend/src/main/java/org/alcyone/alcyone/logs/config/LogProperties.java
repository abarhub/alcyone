package org.alcyone.alcyone.logs.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.alcyone.alcyone.logs.domain.LogFormat;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration des sources de logs, alimentée par {@code alcyone.logs.*} dans application.yaml.
 */
@Data
@Validated
@ConfigurationProperties(prefix = "alcyone.logs")
public class LogProperties {

    /** Taille de page par défaut si le frontend n'en fournit pas. */
    @Min(1)
    private int defaultPageSize = 100;

    /** Borne maximale de la taille de page acceptée (garde-fou). */
    @Min(1)
    private int maxPageSize = 1000;

    /** Liste des fichiers de logs exposés. */
    private List<Source> sources = new ArrayList<>();

    /**
     * Une source de logs = un fichier sur le disque.
     */
    @Data
    public static class Source {

        /** Nom logique, unique, utilisé par le frontend. */
        @NotBlank
        private String name;

        /** Chemin du fichier de logs sur le disque. */
        @NotBlank
        private String path;

        /** Format du fichier. */
        private LogFormat format = LogFormat.TEXT;

        /**
         * Motif de parsing de l'horodatage (syntaxe {@link java.time.format.DateTimeFormatter}).
         * Optionnel : si absent, un parsing souple est tenté (ISO, décalage, ou date-heure locale).
         */
        private String timestampFormat;

        /** Fuseau appliqué aux horodatages sans fuseau (défaut UTC). */
        private String timestampZone = "UTC";

        // --- Spécifique TEXT ---------------------------------------------------

        /**
         * Expression régulière (avec groupes nommés {@code timestamp}, {@code level}, {@code message})
         * servant à découper une ligne de log texte. Une ligne qui ne correspond pas est considérée
         * comme la continuation de l'entrée précédente (ex. stack trace).
         */
        private String lineRegex =
                "^(?<timestamp>\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}([.,]\\d{1,9})?)"
                        + "\\s+(?<level>TRACE|DEBUG|INFO|WARN|WARNING|ERROR|FATAL)?\\s*(?<message>.*)$";

        // --- Spécifique JSON ---------------------------------------------------

        /** Nom du champ JSON contenant la date. */
        private String timestampField = "timestamp";

        /** Nom du champ JSON contenant le message. */
        private String messageField = "message";

        /** Nom du champ JSON contenant le niveau. */
        private String levelField = "level";
    }
}
