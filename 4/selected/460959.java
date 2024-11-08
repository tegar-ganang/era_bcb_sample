package org.yawni.wordnet;

import org.junit.*;
import static org.junit.Assert.*;
import java.util.*;
import static org.fest.assertions.Assertions.assertThat;

/** 
 * By far most complex features involve multi-words, esp those containing
 * prepositions and "-".
 *
 * - Test Plan
 *   - Morphy
 *     - make sure caching strategies are not harming correctness (uses DatabaseKey(pos, someString))
 *   - specific synsets
 *     - using offets will require changes as WN is improved
 *     - could use lemmma, pos, and (sense number OR gloss)
 *   - other relations including derivationally related
 *   - add speed tests
 *     - task-based: count unique WordSense's in all DBs
 *     - get stems of every lemma in all DBs ("wounds" → "wound" → "wind") 
 *     - compare speed with various CharStream impls (add some package private methods)
 *   - sense numbers
 *   - gloss
 *   - compare to parsed output of 'wn' binary (optional - @Ignore and/or boolean flag)
 * 
 * TODO add tests with prepositions
 * TODO consider proper Parameterized tests
 */
public class MorphyTest {

    private WordNetInterface wn;

    @Before
    public void init() {
        wn = WordNet.getInstance();
    }

    @Test
    public void testMorphyUtils() {
        assertEquals(1, Morphy.countWords("dog", ' '));
        assertEquals(2, Morphy.countWords("dog_gone", ' '));
        assertEquals(2, Morphy.countWords("dog _ gone", ' '));
        assertEquals(2, Morphy.countWords("dog__gone", ' '));
        assertEquals(2, Morphy.countWords("dog_ gone", ' '));
        assertEquals(2, Morphy.countWords("_dog_gone_", ' '));
        assertEquals(1, Morphy.countWords("dog-gone", ' '));
        assertEquals(2, Morphy.countWords("dog-gone", '-'));
        assertEquals(3, Morphy.countWords("internal-combustion engine", '-'));
        assertEquals(2, Morphy.countWords("internal-combustion engine", '_'));
        assertEquals(3, Morphy.countWords("a-b-c", '-'));
        assertEquals(3, Morphy.countWords("a-b-c-", '-'));
        assertEquals(3, Morphy.countWords("-a-b-c", '-'));
        assertEquals(0, Morphy.countWords("", ' '));
        assertEquals(0, Morphy.countWords(" ", ' '));
        assertEquals(1, Morphy.countWords("-", ' '));
        assertEquals(1, Morphy.countWords("--", ' '));
        assertEquals(0, Morphy.countWords("__", ' '));
        assertEquals(0, Morphy.countWords("  ", ' '));
        assertEquals(1, Morphy.countWords("- ", ' '));
    }

    @Test
    public void coreTest() {
        String[][] unstemmedStemmedCases = new String[][] { { POS.NOUN.name(), "dogs", "dog" }, { POS.NOUN.name(), "geese", "goose", "geese" }, { POS.NOUN.name(), "handfuls", "handful" }, { POS.NOUN.name(), "villas", "Villa", "villa" }, { POS.NOUN.name(), "Villa", "Villa", "villa" }, { POS.NOUN.name(), "br", "Br", "BR" }, { POS.NOUN.name(), "heiresses", "heiress" }, { POS.NOUN.name(), "heiress", "heiress" }, { POS.NOUN.name(), "George W. \t\tBush", "George W. Bush" }, { POS.NOUN.name(), "george w. bush", "George W. Bush" }, { POS.NOUN.name(), "mice", "mouse", "mice" }, { POS.NOUN.name(), "internal-combustion engine", "internal-combustion engine" }, { POS.NOUN.name(), "internal combustion engine", "internal-combustion engine" }, { POS.NOUN.name(), "internal combustion engines", "internal-combustion engine" }, { POS.NOUN.name(), "hangers-on", "hanger-on", "hangers-on" }, { POS.NOUN.name(), "hangers on", "hanger-on" }, { POS.NOUN.name(), "letter bombs", "letter bomb" }, { POS.NOUN.name(), "letter-bomb", "letter bomb" }, { POS.NOUN.name(), "I ran", null }, { POS.NOUN.name(), "be an", null }, { POS.NOUN.name(), "are a", null }, { POS.NOUN.name(), " Americans", "American" }, { POS.NOUN.name(), "_slovaks_", "Slovak" }, { POS.NOUN.name(), "superheroes", "superhero", "superheroes" }, { POS.NOUN.name(), "businessmen", "businessman", "businessmen" }, { POS.NOUN.name(), "_", null }, { POS.NOUN.name(), "\n", null }, { POS.NOUN.name(), "\ndog", null }, { POS.NOUN.name(), "dog\n", null }, { POS.NOUN.name(), "\n''", null }, { POS.NOUN.name(), "''\n", null }, { POS.NOUN.name(), "-", null }, { POS.NOUN.name(), "--", null }, { POS.NOUN.name(), "__", null }, { POS.NOUN.name(), "  ", null }, { POS.NOUN.name(), " ", null }, { POS.NOUN.name(), "-_", null }, { POS.NOUN.name(), "_-", null }, { POS.NOUN.name(), " -", null }, { POS.NOUN.name(), "- ", null }, { POS.NOUN.name(), "armful", "armful" }, { POS.NOUN.name(), "attorneys general", "attorney general", "Attorney General" }, { POS.NOUN.name(), "axes", "axis", "ax", "axes", "Axis" }, { POS.NOUN.name(), "bases", "basis", "base", "bases" }, { POS.NOUN.name(), "boxesful", "boxful" }, { POS.NOUN.name(), "Bachelor of Sciences in Engineering", "Bachelor of Science in Engineering" }, { POS.NOUN.name(), "cd", "Cd", "cd", "CD" }, { POS.NOUN.name(), "lines of business", "line of business" }, { POS.NOUN.name(), "SS", "SS" }, { POS.NOUN.name(), "mamma's boy", "mamma's boy" }, { POS.NOUN.name(), "15_minutes", "15 minutes" }, { POS.NOUN.name(), "talks", "talk", "talks" }, { POS.NOUN.name(), "talk", "talk" }, { POS.NOUN.name(), "values", "value", "values" }, { POS.NOUN.name(), "value", "value" }, { POS.NOUN.name(), "wounded", "wounded" }, { POS.NOUN.name(), "yourselves", "yourself", "yourselves" }, { POS.NOUN.name(), "wounding", "wounding" }, { POS.NOUN.name(), "'s Gravenhage", "'s Gravenhage" }, { POS.NOUN.name(), "parts of speech", "part of speech" }, { POS.NOUN.name(), "read/write memory", "read/write memory" }, { POS.NOUN.name(), "roma", "rom", "roma", "Roma" }, { POS.NOUN.name(), "rom", "ROM" }, { POS.ADJ.name(), "KO'd", "KO'd" }, { POS.VERB.name(), "KO'd", "ko", "ko'd" }, { POS.VERB.name(), "booby-trapped", "booby-trap", "booby-trapped" }, { POS.VERB.name(), "bird-dogged", "bird-dog", "bird-dogged" }, { POS.VERB.name(), "wounded", "wound" }, { POS.VERB.name(), "wound", "wind", "wound" }, { POS.ADJ.name(), "wounded", "wounded" }, { POS.VERB.name(), "dogs", "dog" }, { POS.VERB.name(), "abided by", "abide by" }, { POS.VERB.name(), "gave a damn", "give a damn" }, { POS.VERB.name(), "asking for it", "ask for it" }, { POS.VERB.name(), "asked for it", "ask for it" }, { POS.VERB.name(), "accounting for", "account for" }, { POS.VERB.name(), "was", "be", "was" }, { POS.VERB.name(), "founded", "found" }, { POS.VERB.name(), "founder", "founder" }, { POS.VERB.name(), "found", "find", "found" }, { POS.VERB.name(), "names of", null }, { POS.VERB.name(), "names of association football", null }, { POS.ADJ.name(), "founder", "founder" }, { POS.NOUN.name(), "was", "WA" }, { POS.VERB.name(), "cannonball along", "cannonball along" }, { POS.VERB.name(), "accesses", "access" }, { POS.VERB.name(), "went", "go", "went" }, { POS.VERB.name(), "bloging", "blog" }, { POS.VERB.name(), "shook hands", "shake hands", "shook hands" }, { POS.VERB.name(), "Americanize", "Americanize" }, { POS.VERB.name(), "saw", "see", "saw" }, { POS.VERB.name(), "let the cats out of the bag", "let the cat out of the bag" }, { POS.ADJ.name(), "onliner", "online" }, { POS.ADJ.name(), "redder", "red" }, { POS.ADJ.name(), "Middle Eastern", "Middle Eastern" }, { POS.ADJ.name(), "Latin-American", "Latin-American" }, { POS.ADJ.name(), "low-pitched", "low-pitched" } };
        for (final String[] testElements : unstemmedStemmedCases) {
            final POS pos = POS.valueOf(testElements[0]);
            final String unstemmed = testElements[1];
            final String stemmed = testElements[2];
            final List<String> goldStems = new ArrayList<String>();
            for (int i = 2; i < testElements.length; ++i) {
                goldStems.add(testElements[i]);
            }
            assertTrue("goldStems: " + goldStems, areUnique(goldStems));
            final List<String> baseForms = stem(unstemmed, pos);
            String msg = "unstemmed: \"" + unstemmed + "\" " + pos + " gold: \"" + stemmed + "\" output: " + baseForms;
            assertTrue(msg, baseForms.contains(stemmed) || (stemmed == null && baseForms.isEmpty()));
            assertFalse("baseForms: " + baseForms, baseFormContainsUnderScore(baseForms));
            if (baseForms.size() >= 2 && !goldStems.equals(baseForms)) {
                System.err.println("extra variants for \"" + unstemmed + "\": " + baseForms + " goldStems: " + goldStems);
            }
            assertTrue(areUnique(baseForms));
            final List<String> upperBaseForms = stem(unstemmed.toUpperCase(), pos);
            msg = "UPPER unstemmed: \"" + unstemmed + "\" " + pos + " gold: \"" + stemmed + "\" output: " + upperBaseForms;
            assertTrue(msg, upperBaseForms.contains(stemmed) || (stemmed == null && upperBaseForms.isEmpty()));
        }
    }

    @Test
    public void testLookupWord() {
        String lemma;
        lemma = "";
        assertNull("lemma: " + lemma, wn.lookupWord(lemma, POS.NOUN));
        lemma = "dog";
        assertNotNull("lemma: " + lemma, wn.lookupWord(lemma, POS.NOUN));
        lemma = "DOG";
        assertNotNull("lemma: " + lemma, wn.lookupWord(lemma, POS.NOUN));
        lemma = "ad blitz";
        assertNotNull("lemma: " + lemma, wn.lookupWord(lemma, POS.NOUN));
        lemma = "ad_blitz";
        assertNotNull("lemma: " + lemma, wn.lookupWord(lemma, POS.NOUN));
        lemma = "AD BLITZ";
        assertNotNull("lemma: " + lemma, wn.lookupWord(lemma, POS.NOUN));
        lemma = "wild-goose chase";
        assertNotNull("lemma: " + lemma, wn.lookupWord(lemma, POS.NOUN));
        lemma = "wild-goose_chase";
        assertNotNull("lemma: " + lemma, wn.lookupWord(lemma, POS.NOUN));
    }

    @Test
    public void testGetExceptions() {
        final WordNet wordNet = (WordNet) wn;
        String lemma;
        lemma = "";
        assertThat(wordNet.getExceptions(lemma, POS.NOUN)).isEmpty();
        lemma = "dog";
        assertThat(wordNet.getExceptions(lemma, POS.NOUN)).isEmpty();
        lemma = "geese";
        assertThat(wordNet.getExceptions(lemma, POS.NOUN)).contains("goose");
        lemma = "geese";
        assertThat(wordNet.getExceptions(lemma, POS.NOUN).get(0)).isEqualTo("geese");
        lemma = "goose";
        assertThat(wordNet.getExceptions(lemma, POS.NOUN)).isEmpty();
    }

    @Test(expected = NullPointerException.class)
    public void testNullLookupWord() {
        assertNull(wn.lookupWord(null, POS.NOUN));
    }

    @Test
    public void testWordSense() {
        assertEquals(42, wn.lookupWord("dog", POS.NOUN).getSense(1).getSensesTaggedFrequency());
        assertEquals(2, wn.lookupWord("dog", POS.VERB).getSense(1).getSensesTaggedFrequency());
        assertEquals(3, wn.lookupWord("cardinal", POS.ADJ).getSense(1).getSensesTaggedFrequency());
        assertEquals(0, wn.lookupWord("cardinal", POS.ADJ).getSense(2).getSensesTaggedFrequency());
        assertEquals(9, wn.lookupWord("concrete", POS.ADJ).getSense(1).getSensesTaggedFrequency());
        assertEquals(1, wn.lookupWord("dogmatic", POS.ADJ).getSense(1).getSensesTaggedFrequency());
    }

    @Test
    public void detectLostVariants() {
        int issues = 0;
        int nonCaseIssues = 0;
        for (final POS pos : POS.CATS) {
            for (final Word word : wn.words(pos)) {
                for (final WordSense wordSense : word.getWordSenses()) {
                    final String lemma = wordSense.getLemma();
                    final List<String> restems = stem(lemma, pos);
                    String msg = "ok";
                    if (!restems.contains(lemma)) {
                        msg = "restems: " + restems + " doesn't contain lemma: " + lemma;
                        issues++;
                        boolean nonCaseIssue = !containsIgnoreCase(lemma, restems);
                        if (nonCaseIssue) {
                            nonCaseIssues++;
                        }
                        System.err.println("issues: " + issues + " nonCases: " + nonCaseIssues + (nonCaseIssue ? "*" : " ") + " " + msg);
                    }
                    if (restems.size() > 1) {
                    }
                    assertTrue(msg, restems.contains(lemma));
                    assertTrue(areUnique(restems));
                }
            }
        }
        IterationTest.printMemoryUsage();
    }

    @Test
    public void findCollocationAmbiguity() {
        int spaceAndDash = 0;
        for (final POS pos : POS.CATS) {
            for (final Word word : wn.words(pos)) {
                final String lemma = word.getLowercasedLemma();
                if (lemma.indexOf("-") > 0 && lemma.indexOf(" ") > 0) {
                    spaceAndDash++;
                }
            }
        }
        int dash = 0;
        int space = 0;
        int dashNoDash = 0;
        int dashSpace = 0;
        int dashNotSpace = 0;
        for (final POS pos : POS.CATS) {
            for (final Word word : wn.words(pos)) {
                final String lemma = word.getLowercasedLemma();
                if (lemma.indexOf('-') > 0) {
                    dash++;
                    final String noDash = lemma.replace("-", "");
                    if (null != wn.lookupWord(noDash, pos)) {
                        dashNoDash++;
                    }
                    final String dashToSpace = lemma.replace('-', ' ');
                    if (null != wn.lookupWord(dashToSpace, pos)) {
                        dashSpace++;
                    } else {
                        dashNotSpace++;
                    }
                }
                if (lemma.indexOf(' ') > 0) {
                    space++;
                }
            }
        }
        System.err.println("dash: " + dash);
        System.err.println("space: " + space);
        System.err.println("dashNotSpace: " + dashNotSpace);
        System.err.println("spaceAndDash: " + spaceAndDash);
        System.err.println("dashNoDash: " + dashNoDash);
        System.err.println("dashSpace: " + dashSpace);
    }

    private List<String> stem(final String someString, final POS pos) {
        return wn.lookupBaseForms(someString, pos);
    }

    private static <T> boolean areUnique(final Collection<T> items) {
        return items.size() == new HashSet<T>(items).size();
    }

    private static boolean containsIgnoreCase(final String needle, final Iterable<String> haystack) {
        for (final String item : haystack) {
            if (item.equalsIgnoreCase(needle)) {
                return true;
            }
        }
        return false;
    }

    private static boolean baseFormContainsUnderScore(final Iterable<String> baseForms) {
        for (final String baseForm : baseForms) {
            if (baseForm.indexOf('_') >= 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean baseFormContainsUpperCase(final Iterable<String> baseForms) {
        for (final String baseForm : baseForms) {
            for (int i = 0, n = baseForm.length(); i < n; i++) {
                final char c = baseForm.charAt(i);
                if (Character.isUpperCase(c)) {
                    return true;
                }
            }
        }
        return false;
    }
}
