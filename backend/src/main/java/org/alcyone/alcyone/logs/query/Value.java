package org.alcyone.alcyone.logs.query;

/**
 * Valeur littérale du membre droit d'une comparaison. Conserve le texte d'origine et, le cas
 * échéant, sa valeur numérique (la comparaison est numérique si les deux côtés le sont).
 *
 * @param text   représentation textuelle
 * @param number valeur numérique si {@code text} en est une, sinon {@code null}
 */
public record Value(String text, Double number) {

    public static Value of(String text) {
        Double number = null;
        try {
            number = Double.parseDouble(text);
        } catch (NumberFormatException ignored) {
            // valeur non numérique : comparaison de chaînes
        }
        return new Value(text, number);
    }

    public boolean isNumeric() {
        return number != null;
    }
}
