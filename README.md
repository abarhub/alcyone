# Alcyone

Visualiseur de logs : un backend qui lit des fichiers de logs (texte ou JSON) et un frontend web
pour les consulter façon Splunk — une barre de recherche et une liste paginée
(date / niveau / message).

## Fonctionnalités

- Lecture de fichiers de logs **texte** ou **JSON Lines** (NDJSON), définis en configuration.
- **Pagination et recherche côté backend** (lecture en streaming, adaptée aux gros fichiers).
- **Filtre par période** : sélecteurs début/fin, appliqués côté backend sur l'horodatage parsé.
- Regroupement des lignes de continuation (ex. stack traces) en une seule entrée.
- **Langage de requête** type Splunk : recherche booléenne (`AND`/`OR`/`NOT`, parenthèses) et
  pipeline `| filter` / `| select` sur les champs JSON. Voir
  **[la documentation du langage](docs/query-language.md)**.
- Empaquetage en **un seul jar exécutable** : le frontend Angular est servi par le backend.

## Architecture

- **Backend** : Spring Boot 4 / Java 25 (module `backend`, Spring Modulith).
- **Frontend** : Angular 22 (module `frontend`, servi depuis le jar du backend).

## Construire

```bash
mvn clean package
```

Produit le jar exécutable `backend/target/alcyone-backend-*.jar` (frontend Angular inclus).

## Lancer

```bash
java -jar backend/target/alcyone-backend-*.jar
```

Puis ouvrir http://localhost:8080.

### Développement

Pour itérer sur le frontend avec rechargement à chaud (proxy vers le backend sur le port 8080) :

```bash
# backend
cd backend && mvn spring-boot:run
# frontend (port 4200)
cd frontend && npm start
```

## Configurer les sources de logs

Les fichiers exposés sont déclarés sous `alcyone.logs.sources` dans
[`backend/src/main/resources/application.yaml`](backend/src/main/resources/application.yaml) :

```yaml
alcyone:
  logs:
    sources:
      - name: app-text
        path: chemin/vers/app.log
        format: TEXT
      - name: api-json
        path: chemin/vers/api.jsonl
        format: JSON
        timestamp-field: "@timestamp"
        message-field: message
        level-field: level
        # Optionnel : parsing de l'horodatage (sinon parsing souple ISO/local, zone UTC)
        # timestamp-format: "yyyy-MM-dd HH:mm:ss.SSS"
        # timestamp-zone: "Europe/Paris"
```

> Les chemins sont relatifs au répertoire de lancement ; en production, préférez des chemins
> absolus. Les horodatages sans fuseau sont interprétés dans `timestamp-zone` (UTC par défaut).

## Documentation

- [Langage de requête](docs/query-language.md)
