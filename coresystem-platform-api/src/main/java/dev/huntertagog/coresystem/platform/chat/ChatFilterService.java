package dev.huntertagog.coresystem.platform.chat;


import dev.huntertagog.coresystem.platform.provider.Service;

import java.util.Set;

public interface ChatFilterService extends Service {

    /**
     * Filtert einen reinen Chat-String.
     * Gibt die gefilterte Variante zurück (Badwords → "***").
     */
    String filter(String raw);

    /**
     * Fügt ein neues Badword hinzu (case-insensitive).
     */
    void addBadWord(String word);

    /**
     * Entfernt ein Badword (case-insensitive).
     */
    void removeBadWord(String word);

    /**
     * Aktueller Satz an Badwords (normiert, z. B. lowercase).
     */
    Set<String> getBadWords();
}
