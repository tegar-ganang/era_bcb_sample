package net.sf.jeckit.impl;

import java.util.ArrayList;
import java.util.logging.Level;
import net.sf.jeckit.common.PhoneticCode;
import net.sf.jeckit.common.ProjectProperties;
import net.sf.jeckit.common.Sentence;
import net.sf.jeckit.common.Suggestion;
import net.sf.jeckit.common.Token;
import net.sf.jeckit.interfaces.Suggester;
import net.sf.jeckit.resources.Dictionary;
import net.sf.jeckit.resources.DictionaryWord;
import net.sf.jeckit.resources.ResourceManager;

/**
 * Class to make suggestions (similar, but not that good ;-) ) to the ASpell algorithm
 * 
 * @author Martin Schierle
 *
 */
public class ASpellSimilarSuggester implements Suggester {

    /**
	 * Will create suggestions for every word in the sentence which seems to be misspellt
	 * @param sentence The sentence containing misspellt words
	 */
    public void createSuggestions(Sentence sentence) {
        for (Token tok : sentence) {
            if (tok.getContent().trim().length() < 3) continue;
            if (!tok.getContent().matches(".*\\w{3}.*")) continue;
            if (tok.getErrorProbability() < 0.9) continue;
            String word = tok.getContent();
            Dictionary dict = ResourceManager.getInstance().getDictionary(tok.getLocale());
            if (word.length() > 6) {
                for (int i = 2; i < word.length() - 2; i++) {
                    String s1 = word.substring(0, i);
                    String s2 = word.substring(i);
                    if (dict.contains(s1) && dict.contains(s2)) {
                        String sugg = s1 + " " + s2;
                        tok.addSuggestion(new Suggestion(sugg, 0.7));
                        ProjectProperties.getInstance().getLogger().log(Level.INFO, "Added suggestion " + sugg + " for word " + word + " with probability " + 0.7);
                    }
                }
            }
            ArrayList<String> codes = new ArrayList<String>();
            codes.add(PhoneticCode.getPhoneticCode(word, tok.getLocale()));
            for (String near : this.getNearWords(word)) {
                codes.add(PhoneticCode.getPhoneticCode(near, tok.getLocale()));
            }
            ArrayList<DictionaryWord> suggestions = new ArrayList<DictionaryWord>();
            for (String code : codes) {
                suggestions.addAll(dict.getWords(code));
            }
            for (DictionaryWord w : suggestions) {
                tok.addSuggestion(new Suggestion(w.getWord()));
                ProjectProperties.getInstance().getLogger().log(Level.INFO, "Added suggestion " + w.getWord() + " for word " + word);
            }
        }
    }

    /**
	 * Will create some words with edit-distance 1 --> Attention: Brute Force, very bad!!!
	 * @param word The word to create some variants
	 * @return A list with near words
	 */
    private ArrayList<String> getNearWords(String word) {
        ArrayList<String> near_words = new ArrayList<String>();
        char[] letters = word.toCharArray();
        char[] copy;
        copy = new char[letters.length - 1];
        for (int i = 1; i < letters.length; i++) {
            copy[i - 1] = letters[i];
        }
        for (int i = 0; i < letters.length - 1; i++) {
            near_words.add(new String(copy));
            copy[i] = letters[i];
        }
        copy = new char[letters.length];
        for (int i = 0; i < letters.length; i++) {
            copy[i] = letters[i];
        }
        for (int i = 0; i < letters.length - 1; i++) {
            char tmp = copy[i + 1];
            copy[i + 1] = copy[i];
            copy[i] = tmp;
            near_words.add(new String(copy));
            copy[i] = copy[i + 1];
            copy[i + 1] = tmp;
        }
        return near_words;
    }
}
