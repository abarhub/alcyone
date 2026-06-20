package org.alcyone.alcyone.logs.web;

import org.alcyone.alcyone.logs.config.LogProperties;
import org.alcyone.alcyone.logs.domain.LogFormat;

/**
 * Vue exposée d'une source de logs (on n'expose pas le chemin disque au frontend).
 */
public record SourceDto(String name, LogFormat format) {

    public static SourceDto from(LogProperties.Source source) {
        return new SourceDto(source.getName(), source.getFormat());
    }
}
