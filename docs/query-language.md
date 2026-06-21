# Langage de requête

La zone de recherche d'Alcyone accepte un petit langage inspiré de Splunk : une **expression
de recherche** optionnelle, suivie d'une suite d'**étapes** séparées par `|`.

```
expression_de_recherche  |  commande  |  commande  ...
```

Tout est évalué **côté backend** : seule la page demandée est renvoyée, avec le nombre total de
résultats. Les sauts de ligne de la zone de saisie sont traités comme des espaces, ce qui permet
d'écrire une requête sur plusieurs lignes.

---

## 1. Expression de recherche

### Mots (recherche texte)

Un mot correspond si la ligne brute du log **contient** ce mot (insensible à la casse).

```
timeout
```

Pour rechercher une expression contenant des espaces, utilisez des guillemets :

```
"connection timeout"
```

### Opérateurs booléens

`AND`, `OR` et `NOT` (obligatoirement **en majuscules**) combinent les critères. La
**juxtaposition** de deux termes équivaut à un `AND` implicite.

```
erreur timeout            # erreur AND timeout
erreur AND timeout        # identique
erreur OR warning
NOT timeout
erreur AND NOT timeout
```

Précédence (du plus prioritaire au moins prioritaire) : `NOT` > `AND` > `OR`.
Les **parenthèses** permettent de forcer le regroupement :

```
(titi OR tutu) AND NOT erreur
NOT (a OR b)
```

### Comparaisons de champs (JSON)

Pour les sources JSON, on peut comparer un champ à une valeur. Un champ commence par un point ;
les champs imbriqués s'écrivent avec des points (`.b.c`).

```
.status >= 500
.level == ERROR
.b.c != 0
```

Opérateurs disponibles :

| Opérateur | Signification        |
|-----------|----------------------|
| `==`      | égal (`=` accepté)   |
| `!=`      | différent            |
| `>`       | strictement supérieur|
| `<`       | strictement inférieur|
| `>=`      | supérieur ou égal    |
| `<=`      | inférieur ou égal    |

La comparaison est **numérique** si le champ et la valeur sont numériques, sinon c'est une
comparaison de **chaînes**. Un champ absent rend la comparaison fausse.

---

## 2. Commandes de pipeline

### `filter`

Ne conserve que les entrées satisfaisant le prédicat. Le prédicat accepte la même syntaxe que
l'expression de recherche (mots, booléens, parenthèses, comparaisons).

```
| filter .status >= 500
| filter .level == ERROR AND NOT .retry == true
| filter (paiement OR commande)
```

### `select`

Restreint les champs affichés. La colonne **Message** affiche alors un **JSON compact** à clés
plates ; la ligne brute complète reste accessible en dépliant l'entrée.

```
| select .a, .b.c
```

Exemple de rendu pour `| select .level, .message` :

```json
{"level":"INFO","message":"Demarrage de la passerelle API"}
```

Un champ sélectionné absent apparaît avec la valeur `null`. Plusieurs `select` cumulent leurs
champs.

---

## 3. Exemple complet

```
erreur (titi OR tutu) | filter .nb > 15 | select .a, .b.c
```

Se lit : garder les lignes qui contiennent « erreur » **et** (« titi » **ou** « tutu »), puis ne
garder que celles dont le champ `.nb` est supérieur à 15, puis n'afficher que les champs `.a` et
`.b.c`.

---

## 4. Filtrer par date

Le filtrage par **période** se fait via les sélecteurs début/fin de l'interface (et non dans le
langage de requête). Ils s'appliquent côté backend sur l'horodatage parsé de chaque entrée, en
plus de la requête. Une entrée sans horodatage parsable est exclue dès qu'une borne est posée.

---

## 5. Sources au format texte

Les logs texte n'ont pas de champs structurés. Sur ce type de source :

- la recherche texte (mots, `AND`/`OR`/`NOT`, parenthèses) fonctionne normalement ;
- une comparaison `.champ op valeur` est toujours **fausse** (donc `NOT .champ op valeur` est
  toujours vraie) ;
- `select` est **sans effet** (le message d'origine est conservé).

---

## 6. Erreurs

Une requête syntaxiquement invalide renvoie une erreur **HTTP 400** dont le message (avec la
position fautive) s'affiche sous la barre de recherche, par exemple :

```
Valeur attendue après l'opérateur (position 14)
```

---

## 7. Grammaire (référence)

```
pipeline   := boolExpr? ( '|' command )*
command    := 'select' fieldRef (',' fieldRef)*
            | 'filter' boolExpr
boolExpr   := orExpr
orExpr     := andExpr ( 'OR' andExpr )*
andExpr    := notExpr ( 'AND'? notExpr )*        // juxtaposition = AND
notExpr    := 'NOT' notExpr | unary
unary      := '(' orExpr ')' | comparison | word
comparison := fieldRef op value
fieldRef   := '.' ident ('.' ident)*
op         := '==' | '!=' | '>' | '<' | '>=' | '<=' | '='
word       := mot | "chaîne entre guillemets"
value      := nombre | mot | "chaîne entre guillemets"
```

### Limites actuelles (non gérées)

- indexation de tableaux (`.a[0]`) ;
- fonctions d'agrégation / statistiques (`stats`, `count`, …).
