package org.yawni.wordnet;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.AbstractIterator;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.base.Preconditions.*;
import java.io.BufferedInputStream;
import org.yawni.util.cache.Cache;
import static org.yawni.util.MergedIterable.merge;
import static org.yawni.util.Utils.uniq;
import org.yawni.util.CharSequences;
import org.yawni.util.LightImmutableList;
import org.yawni.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.yawni.util.StringTokenizer;
import org.yawni.util.cache.BloomFilter;
import org.yawni.util.cache.Caches;
import org.yawni.wordnet.WordSense.AdjPosition;

/** 
 * An implementation of {@code WordNetInterface} that retrieves objects from the text files in the WordNet distribution
 * directory (typically <tt><em>$WNHOME</em>/dict/</tt>), or from a properly organized jar file containing it;
 * typical users will use {@link WordNet#getInstance()} to get the canonical instance of this
 * class.
 *
 * <p> A {@code WordNet} has an <em>entity cache</em>.  The entity cache is used to resolve multiple
 * temporally contiguous lookups of the same entity to the same object -- for example, successive
 * calls to {@link #lookupWord(CharSequence, POS)} with the same parameters would return the same value
 * ({@code ==} as well as {@code equals}), as would traversal of two {@link Relation}s
 * that shared the same target.  Under memory pressure, it is possible for
 * two different ({@code !=}, but still {@code equals}) objects to represent the same entity, 
 * if their retrieval is separated by other database operations.
 *
 * @see WordNetInterface
 * @see Cache
 */
public final class WordNet implements WordNetInterface {

    private static final Logger log = LoggerFactory.getLogger(WordNet.class.getName());

    private final FileManagerInterface fileManager;

    final Morphy morphy;

    /**
   * Construct a {@link WordNetInterface} that retrieves file data from {@code fileManager}.
   */
    private WordNet(final FileManagerInterface fileManager) {
        this.fileManager = fileManager;
        this.morphy = new Morphy(this);
    }

    /**
   * Construct a dictionary backed by a set of files contained in the default
   * WordNet search directory.
   * See {@link FileManager} for a description of the location of the default
   * WordNet search directory ({@code $WNSEARCHDIR}).
   */
    WordNet() {
        this(new FileManager());
    }

    private static final class InstanceHolder {

        /** singleton reference */
        static final WordNet instance = new WordNet();
    }

    /**
   * Factory method to get <em>the</em> dictionary backed by a set of files contained
   * in the default WordNet search directory.
   * See {@link FileManager} for a description of the location of the default
   * WordNet search directory ({@code $WNSEARCHDIR}).
   */
    public static WordNet getInstance() {
        return InstanceHolder.instance;
    }

    static final int DEFAULT_CACHE_CAPACITY = 10000;

    private final Cache<DatabaseKey, Object> synsetCache = Caches.withCapacity(DEFAULT_CACHE_CAPACITY);

    private final Cache<DatabaseKey, Object> indexWordCache = Caches.withCapacity(DEFAULT_CACHE_CAPACITY);

    static interface DatabaseKey {

        @Override
        public int hashCode();

        @Override
        public boolean equals(Object that);
    }

    static class POSOffsetDatabaseKey implements DatabaseKey {

        private final int offset;

        private final byte posOrdinal;

        POSOffsetDatabaseKey(final POS pos, final int offset) {
            this.offset = offset;
            this.posOrdinal = Utils.checkedCast(pos.ordinal());
        }

        @Override
        public boolean equals(final Object object) {
            if (object instanceof POSOffsetDatabaseKey) {
                final POSOffsetDatabaseKey that = (POSOffsetDatabaseKey) object;
                return that.posOrdinal == this.posOrdinal && that.offset == this.offset;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return (offset * 10) + posOrdinal;
        }

        @Override
        public String toString() {
            return "[POSOffsetDatabaseKey offset: " + offset + " " + POS.fromOrdinal(posOrdinal) + "]";
        }
    }

    static class StringPOSDatabaseKey implements DatabaseKey {

        private final CharSequence key;

        private final byte posOrdinal;

        StringPOSDatabaseKey(final CharSequence key, final POS pos) {
            this.key = key;
            this.posOrdinal = Utils.checkedCast(pos.ordinal());
        }

        @Override
        public boolean equals(final Object object) {
            if (object instanceof StringPOSDatabaseKey) {
                final StringPOSDatabaseKey that = (StringPOSDatabaseKey) object;
                return that.posOrdinal == this.posOrdinal && Utils.equals(that.key, this.key);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return posOrdinal ^ CharSequences.hashCode(key);
        }

        @Override
        public String toString() {
            return "[StringPOSDatabaseKey key: " + key + " " + POS.fromOrdinal(posOrdinal) + "]";
        }
    }

    private static final Map<POS, String> POS_TO_FILENAME_ROOT;

    static {
        POS_TO_FILENAME_ROOT = new EnumMap<POS, String>(POS.class) {

            {
                put(POS.NOUN, "noun");
                put(POS.VERB, "verb");
                put(POS.ADJ, "adj");
                put(POS.SAT_ADJ, "adj");
                put(POS.ADV, "adv");
            }
        };
    }

    private static String getDatabaseSuffixName(final POS pos) {
        final String toReturn = POS_TO_FILENAME_ROOT.get(pos);
        if (toReturn == null) {
            throw new IllegalArgumentException("no filename for pos " + pos);
        }
        return toReturn;
    }

    private static final Map<POS, String> DATA_FILE_NAMES;

    static {
        DATA_FILE_NAMES = new EnumMap<POS, String>(POS.class);
        for (final POS pos : POS.CATS) {
            DATA_FILE_NAMES.put(pos, "data." + getDatabaseSuffixName(pos));
        }
    }

    private static String getDataFilename(final POS pos) {
        final String toReturn = DATA_FILE_NAMES.get(pos);
        if (toReturn == null) {
            throw new IllegalArgumentException("no filename for pos " + pos);
        }
        return toReturn;
    }

    private static final Map<POS, String> INDEX_FILE_NAMES;

    static {
        INDEX_FILE_NAMES = new EnumMap<POS, String>(POS.class);
        for (final POS pos : POS.CATS) {
            INDEX_FILE_NAMES.put(pos, "index." + getDatabaseSuffixName(pos));
        }
    }

    private static String getIndexFileName(final POS pos) {
        final String toReturn = INDEX_FILE_NAMES.get(pos);
        if (toReturn == null) {
            throw new IllegalArgumentException("no filename for pos " + pos);
        }
        return toReturn;
    }

    private static final Map<POS, String> EXCEPTION_FILE_NAMES;

    static {
        EXCEPTION_FILE_NAMES = new EnumMap<POS, String>(POS.class);
        for (final POS pos : POS.CATS) {
            EXCEPTION_FILE_NAMES.put(pos, getDatabaseSuffixName(pos) + ".exc");
        }
    }

    private static String getExceptionsFilename(final POS pos) {
        final String toReturn = EXCEPTION_FILE_NAMES.get(pos);
        if (toReturn == null) {
            throw new IllegalArgumentException("no filename for pos " + pos);
        }
        return toReturn;
    }

    private static String getCntlistDotRevFilename() {
        return "cntlist.rev";
    }

    private static String getCoreRankFilename() {
        return "core-wordnet.ranked";
    }

    @VisibleForTesting
    static String getMorphosemanticRelationsFilename() {
        return "morphosemantic-links.xls.tsv.offsets.synsetIndexes.bidi";
    }

    private static String getVerbGroupRelationsFilename() {
        return "verb_groups.non_pairs.offsets";
    }

    private static String getVerbSentencesIndexFilename() {
        return "sentidx.vrb";
    }

    private static String getVerbSentencesFileName() {
        return "sents.vrb";
    }

    private static String getGenericVerbFramesFilename() {
        return "frames.vrb";
    }

    private static void cacheDebug(final Cache<DatabaseKey, Object> cache) {
    }

    static int getIndexWordAtCacheMiss = 0;

    static int getIndexWordAtCacheHit = 0;

    static int weirdGetIndexWordAtCacheMiss = 0;

    Word getIndexWordAt(final POS pos, final int offset) {
        final DatabaseKey cacheKey = new POSOffsetDatabaseKey(pos, offset);
        Word word = (Word) indexWordCache.get(cacheKey);
        if (word != null) {
            getIndexWordAtCacheHit++;
            cacheDebug(indexWordCache);
        } else {
            getIndexWordAtCacheMiss++;
            cacheDebug(indexWordCache);
            final String fileName = getIndexFileName(pos);
            final CharSequence line;
            try {
                line = fileManager.readLineAt(offset, fileName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (line == null) {
                throw new RuntimeException("line null for offset " + offset + " " + pos);
            }
            word = new Word(line, offset, this);
            indexWordCache.put(cacheKey, word);
        }
        return word;
    }

    static int getSynsetAtCacheMiss = 0;

    static int getSynsetAtCacheHit = 0;

    static int weirdGetSynsetAtCacheMiss = 0;

    String getSynsetLineAt(final POS pos, final int offset) {
        final String line;
        final String fileName = getDataFilename(pos);
        try {
            line = fileManager.readLineAt(offset, fileName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return line;
    }

    Synset getSynsetAt(final POS pos, final int offset, String line) {
        final DatabaseKey cacheKey = new POSOffsetDatabaseKey(pos, offset);
        Synset synset = (Synset) synsetCache.get(cacheKey);
        if (synset != null) {
            getSynsetAtCacheHit++;
            cacheDebug(synsetCache);
        } else {
            getSynsetAtCacheMiss++;
            cacheDebug(synsetCache);
            line = getSynsetLineAt(pos, offset);
            synset = new Synset(line, this);
            synsetCache.put(cacheKey, synset);
        }
        return synset;
    }

    Synset getSynsetAt(final POS pos, final int offset) {
        return getSynsetAt(pos, offset, null);
    }

    static int lookupIndexWordCacheMiss = 0;

    static int lookupIndexWordCacheHit = 0;

    static int weirdLookupIndexWordCacheMiss = 0;

    private static final Map<POS, BloomFilter<CharSequence>> INDEX_DATA_FILTERS;

    private static final Map<POS, BloomFilter<CharSequence>> EXCEPTIONS_FILTERS;

    static {
        INDEX_DATA_FILTERS = new EnumMap<POS, BloomFilter<CharSequence>>(POS.class);
        EXCEPTIONS_FILTERS = new EnumMap<POS, BloomFilter<CharSequence>>(POS.class);
        for (final POS pos : POS.CATS) {
            final String indexDataResourceName = "dict/" + pos.name() + ".bloom";
            final BloomFilter<CharSequence> indexDataFilter = getResource(indexDataResourceName);
            if (indexDataFilter != null) {
                INDEX_DATA_FILTERS.put(pos, indexDataFilter);
            }
            final String exceptionsResourceName = "dict/" + pos.name() + ".exc.bloom";
            final BloomFilter<CharSequence> exceptionsFilter = getResource(exceptionsResourceName);
            if (exceptionsFilter != null) {
                EXCEPTIONS_FILTERS.put(pos, exceptionsFilter);
            }
        }
    }

    private static BloomFilter<CharSequence> getResource(final String resourceName) {
        try {
            final URL url = WordNet.class.getClassLoader().getResource(resourceName);
            if (url == null) {
                log.info("resourceName: {} not found!", resourceName);
                return null;
            }
            final URLConnection conn = url.openConnection();
            final InputStream input = conn.getInputStream();
            final ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(input));
            @SuppressWarnings("unchecked") final BloomFilter<CharSequence> filter = (BloomFilter<CharSequence>) ois.readObject();
            return filter;
        } catch (Exception e) {
            log.info("caught {}", e);
            System.err.println("caught!" + e);
            return null;
        }
    }

    private boolean maybeDefined(final CharSequence lemma, final POS pos) {
        if (INDEX_DATA_FILTERS.isEmpty()) {
            return true;
        }
        return INDEX_DATA_FILTERS.get(pos).contains(lemma);
    }

    private boolean maybeException(final CharSequence lemma, final POS pos) {
        if (EXCEPTIONS_FILTERS.isEmpty()) {
            return true;
        }
        return EXCEPTIONS_FILTERS.get(pos).contains(lemma);
    }

    private static final Object NULL_INDEX_WORD = new Object();

    /** {@inheritDoc} */
    public Word lookupWord(final CharSequence lemma, final POS pos) {
        checkValidPOS(pos);
        final DatabaseKey cacheKey = new StringPOSDatabaseKey(lemma, pos);
        Object indexWord = indexWordCache.get(cacheKey);
        if (indexWord != null && indexWord != NULL_INDEX_WORD) {
            lookupIndexWordCacheHit++;
            cacheDebug(indexWordCache);
        } else {
            indexWord = NULL_INDEX_WORD;
            if (maybeDefined(lemma, pos)) {
                lookupIndexWordCacheMiss++;
                cacheDebug(indexWordCache);
                final String fileName = getIndexFileName(pos);
                final int offset;
                try {
                    offset = fileManager.getIndexedLinePointer(lemma, fileName);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                if (offset >= 0) {
                    indexWord = getIndexWordAt(pos, offset);
                }
            }
            if (indexWord != NULL_INDEX_WORD) {
                indexWordCache.put(cacheKey, indexWord);
            }
        }
        return indexWord != NULL_INDEX_WORD ? (Word) indexWord : null;
    }

    /** {@inheritDoc} */
    public List<String> lookupBaseForms(final String someString, final POS pos) {
        if (pos == POS.ALL) {
            return LightImmutableList.copyOf(uniq(merge(lookupBaseForms(someString, POS.NOUN), lookupBaseForms(someString, POS.VERB), lookupBaseForms(someString, POS.ADJ), lookupBaseForms(someString, POS.ADV))));
        } else {
            return morphy.morphstr(someString, pos);
        }
    }

    /** {@inheritDoc} */
    public List<Synset> lookupSynsets(final String someString, final POS pos) {
        if (pos == POS.ALL) {
            return LightImmutableList.copyOf(uniq(merge(lookupSynsets(someString, POS.NOUN), lookupSynsets(someString, POS.VERB), lookupSynsets(someString, POS.ADJ), lookupSynsets(someString, POS.ADV))));
        } else {
            return doLookupSynsets(someString, pos);
        }
    }

    private List<Synset> doLookupSynsets(final String someString, final POS pos) {
        checkValidPOS(pos);
        final LightImmutableList<String> morphs = morphy.morphstr(someString, pos);
        if (morphs.isEmpty()) {
            return LightImmutableList.of();
        }
        final ArrayList<Synset> syns = new ArrayList<Synset>();
        int morphNum = -1;
        for (final String lemma : morphs) {
            morphNum++;
            final Word word = this.lookupWord(lemma, pos);
            if (word == null) {
                continue;
            }
            syns.ensureCapacity(syns.size() + word.getSynsets().size());
            for (final Synset syn : word.getSynsets()) {
                syns.add(syn);
            }
        }
        if (!morphs.isEmpty() && syns.isEmpty()) {
            return LightImmutableList.of();
        }
        return LightImmutableList.copyOf(syns);
    }

    /** {@inheritDoc} */
    public List<WordSense> lookupWordSenses(final String someString, final POS pos) {
        if (pos == POS.ALL) {
            return LightImmutableList.copyOf(uniq(merge(lookupWordSenses(someString, POS.NOUN), lookupWordSenses(someString, POS.VERB), lookupWordSenses(someString, POS.ADJ), lookupWordSenses(someString, POS.ADV))));
        } else {
            return doLookupWordSenses(someString, pos);
        }
    }

    private List<WordSense> doLookupWordSenses(final String someString, final POS pos) {
        checkValidPOS(pos);
        final LightImmutableList<String> morphs = morphy.morphstr(someString, pos);
        if (morphs.isEmpty()) {
            return LightImmutableList.of();
        }
        final ArrayList<WordSense> wordSenses = new ArrayList<WordSense>();
        int morphNum = -1;
        for (final String lemma : morphs) {
            morphNum++;
            final Word word = this.lookupWord(lemma, pos);
            if (word == null) {
                continue;
            }
            wordSenses.ensureCapacity(wordSenses.size() + word.getSynsets().size());
            for (final WordSense wordSense : word.getWordSenses()) {
                wordSenses.add(wordSense);
            }
        }
        if (!morphs.isEmpty() && wordSenses.isEmpty()) {
            return LightImmutableList.of();
        }
        return LightImmutableList.copyOf(wordSenses);
    }

    /** {@inheritDoc} */
    public Iterable<Synset> synsets(final String query) {
        final EnumMap<Command, String> cmdToValue = Command.getCmdToValue(query);
        if (cmdToValue.containsKey(Command.OFFSET)) {
            Command.OFFSET.act(cmdToValue, this);
        }
        if (cmdToValue.size() == 1) {
            if (cmdToValue.containsKey(Command.POS)) {
                final POS pos = POS.valueOf(cmdToValue.get(Command.POS));
                return synsets(pos);
            } else if (cmdToValue.containsKey(Command.LEXNAME)) {
                final Lexname lexname = Lexname.lookupLexname(cmdToValue.get(Command.LEXNAME));
                return synsets(lexname);
            } else if (cmdToValue.containsKey(Command.WORD)) {
                final String someString = cmdToValue.get(Command.WORD);
                final POS pos = POS.ALL;
                return lookupSynsets(someString, pos);
            }
        } else if (cmdToValue.size() == 2) {
            if (cmdToValue.containsKey(Command.POS)) {
                final POS pos = POS.valueOf(cmdToValue.get(Command.POS));
                if (cmdToValue.containsKey(Command.OFFSET)) {
                    final int offset = Integer.parseInt(cmdToValue.get(Command.OFFSET));
                    final Synset synset = getSynsetAt(pos, offset);
                    if (synset != null) {
                        return LightImmutableList.of(synset);
                    } else {
                        return LightImmutableList.of();
                    }
                } else if (cmdToValue.containsKey(Command.WORD)) {
                    final String someString = cmdToValue.get(Command.WORD);
                    return lookupSynsets(someString, pos);
                }
            }
        }
        throw new IllegalArgumentException("unsatisfiable query " + query);
    }

    /** {@inheritDoc} */
    public Iterable<WordSense> wordSenses(final String query) {
        final EnumMap<Command, String> cmdToValue = Command.getCmdToValue(query);
        if (cmdToValue.size() == 1) {
            if (cmdToValue.containsKey(Command.POS)) {
                final POS pos = POS.valueOf(cmdToValue.get(Command.POS));
                return wordSenses(pos);
            } else if (cmdToValue.containsKey(Command.ADJ_POSITION)) {
                final AdjPosition adjPosition = AdjPosition.fromValue(cmdToValue.get(Command.ADJ_POSITION));
                return wordSenses(adjPosition);
            } else if (cmdToValue.containsKey(Command.WORD)) {
                final String someString = cmdToValue.get(Command.WORD);
                final POS pos = POS.ALL;
                return lookupWordSenses(someString, pos);
            }
        } else if (cmdToValue.size() == 2) {
            if (cmdToValue.containsKey(Command.POS)) {
                final POS pos = POS.valueOf(cmdToValue.get(Command.POS));
                if (cmdToValue.containsKey(Command.OFFSET)) {
                    final int offset = Integer.parseInt(cmdToValue.get(Command.OFFSET));
                    final Iterable<WordSense> synset = getSynsetAt(pos, offset);
                    if (synset != null) {
                        return synset;
                    } else {
                        return LightImmutableList.of();
                    }
                } else if (cmdToValue.containsKey(Command.WORD)) {
                    final String someString = cmdToValue.get(Command.WORD);
                    return lookupWordSenses(someString, pos);
                }
            }
        }
        throw new IllegalArgumentException("unsatisfiable query " + query);
    }

    private final Cache<DatabaseKey, LightImmutableList<String>> exceptionsCache = Caches.withCapacity(DEFAULT_CACHE_CAPACITY);

    /**
   * <em>looks up</em> word in the appropriate <em>exc</em>eptions file for the given {@code pos}.
   * The exception list files, <tt>pos</tt>.<em>exc</em> , are used to help the morphological
   * processor find base forms from irregular inflections.  
   * <strong>NOTE: The first entry is the exceptional word itself (e.g., for "geese", it's "geese")</strong>.
   * Port of {@code morph.c exc_lookup()}
   * @see <a href="http://wordnet.princeton.edu/man/morphy.7WN.html#sect3">
   *   http://wordnet.princeton.edu/man/morphy.7WN.html#sect3</a>
   */
    LightImmutableList<String> getExceptions(final CharSequence someString, final POS pos) {
        checkValidPOS(pos);
        if (!maybeException(someString, pos)) {
            return LightImmutableList.of();
        }
        final DatabaseKey cacheKey = new StringPOSDatabaseKey(someString, pos);
        final LightImmutableList<String> cached = exceptionsCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        assert someString != null;
        assert someString.length() > 0 : "someString: \"" + someString + "\" " + pos;
        assert pos != null;
        final String fileName = getExceptionsFilename(pos);
        try {
            final int offset = fileManager.getIndexedLinePointer(someString, fileName);
            if (offset >= 0) {
                final String line = fileManager.readLineAt(offset, fileName);
                final LightImmutableList<String> toReturn = LightImmutableList.copyOf(new StringTokenizer(line, " "));
                assert toReturn.size() >= 2;
                exceptionsCache.put(cacheKey, toReturn);
                return toReturn;
            } else {
                exceptionsCache.put(cacheKey, LightImmutableList.<String>of());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return LightImmutableList.of();
    }

    /**
   * <em>looks up</em> <a href="http://wordnet.princeton.edu/man/senseidx.5WN.html#sect3">senskey</a>
   * in the {@code cntlist.rev} file and returns the matching line (or
   * {@code null}).  Informationally equivalent to searching
   * {@code index.sense} (or {@code sense.idx} on older Windows
   * releases).  Differences are that {@code cntlist.rev} includes defunct
   * sense information (does no harm though because it isn't referenced in its
   * WordNet), doesn't include entries for items with zero counts, doesn't
   * include synset offset, and formats adjective sense keys correctly (including
   * {@link WordSense.AdjPosition} information).
   * 
   * @see Word#getTaggedSenseCount()
   */
    String lookupCntlistDotRevLine(final CharSequence senseKey) {
        final int offset;
        final String line;
        try {
            offset = fileManager.getIndexedLinePointer(senseKey, getCntlistDotRevFilename());
            if (offset < 0) {
                line = null;
            } else {
                line = fileManager.readLineAt(offset, getCntlistDotRevFilename());
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        return line;
    }

    String lookupCoreRankLine(final CharSequence senseKey) {
        final int offset;
        final String line;
        try {
            offset = fileManager.getIndexedLinePointer(senseKey, getCoreRankFilename());
            if (offset < 0) {
                line = null;
            } else {
                line = fileManager.readLineAt(offset, getCoreRankFilename());
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        return line;
    }

    Iterable<CharSequence> lookupMorphoSemanticRelationLines(final CharSequence senseKey) {
        try {
            return fileManager.getMatchingLines(senseKey, getMorphosemanticRelationsFilename());
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    Iterable<CharSequence> lookupVerbGroupLines(final CharSequence senseKey) {
        try {
            return fileManager.getMatchingLines(senseKey, getVerbGroupRelationsFilename());
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    /** XXX DOCUMENT ME */
    String lookupGenericFrame(final int framenum) {
        assert framenum >= 1;
        String line = null;
        try {
            line = fileManager.readLineNumber(framenum - 1, getGenericVerbFramesFilename());
            assert line != null : "framenum: " + framenum;
            int idx = line.indexOf(' ');
            assert idx >= 0;
            for (int i = idx + 1, n = line.length(); i < n && line.charAt(i) == ' '; i++) {
                idx++;
            }
            assert line.charAt(idx) == ' ';
            assert line.charAt(idx + 1) != ' ';
            idx++;
            line = line.substring(idx);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        return line;
    }

    /**
   * @return verb sentence numbers as a comma separated list with no spaces
   *         (e.g., "15,16")
   */
    String lookupVerbSentencesNumbers(final CharSequence verbSenseKey) {
        String line = null;
        try {
            final int offset = fileManager.getIndexedLinePointer(verbSenseKey, getVerbSentencesIndexFilename());
            if (offset >= 0) {
                line = fileManager.readLineAt(offset, getVerbSentencesIndexFilename());
                assert line != null;
                int idx = line.indexOf(' ');
                assert idx >= 0;
                for (int i = idx + 1, n = line.length(); i < n && line.charAt(i) == ' '; i++) {
                    idx++;
                }
                assert line.charAt(idx) == ' ';
                if (idx + 1 < line.length()) {
                    assert line.charAt(idx + 1) != ' ' : "verbSenseKey: " + verbSenseKey;
                    idx++;
                    line = line.substring(idx);
                } else {
                    line = null;
                }
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        return line;
    }

    /**
   * @return illustrative verb sentence for given number. Always contains "%s".
   */
    String lookupVerbSentence(final String verbSentenceNumber) {
        String line = null;
        try {
            final int offset = fileManager.getIndexedLinePointer(verbSentenceNumber, getVerbSentencesFileName());
            if (offset >= 0) {
                line = fileManager.readLineAt(offset, getVerbSentencesFileName());
                assert line != null;
                int idx = line.indexOf(' ');
                assert idx >= 0;
                for (int i = idx + 1, n = line.length(); i < n && line.charAt(i) == ' '; i++) {
                    idx++;
                }
                assert line.charAt(idx) == ' ';
                assert line.charAt(idx + 1) != ' ';
                idx++;
                line = line.substring(idx);
                assert line.indexOf("%s") >= 0;
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        return line;
    }

    private static void checkValidPOS(final POS pos) {
        if (POS.ALL == pos) {
            throw new IllegalArgumentException("POS.ALL is not supported");
        }
    }

    private abstract class AbstractWordIterator extends AbstractIterator<Word> {

        private static final String TWO_SPACES = "  ";

        protected final POS pos;

        protected final String fileName;

        protected int nextOffset = 0;

        protected AbstractWordIterator(final POS pos) {
            this.pos = pos;
            this.fileName = getIndexFileName(pos);
        }

        protected void skipLicenseLines() throws IOException {
            if (nextOffset != 0) {
                return;
            }
            String line = null;
            int offset = -1;
            do {
                if (nextOffset < 0) {
                    throw new NoSuchElementException();
                }
                line = fileManager.readLineAt(nextOffset, fileName);
                if (line == null) {
                    break;
                }
                offset = nextOffset;
                nextOffset = fileManager.getNextLinePointer(nextOffset, fileName);
            } while (line.startsWith(TWO_SPACES));
            assert nextOffset != -1;
            nextOffset = offset;
        }
    }

    /**
   * @see WordNetInterface#words
   */
    private class WordIterator extends AbstractWordIterator {

        WordIterator(final POS pos) {
            super(pos);
        }

        @Override
        protected Word computeNext() {
            try {
                skipLicenseLines();
                final int offset = nextOffset;
                final String line = fileManager.readLineAt(nextOffset, fileName);
                nextOffset = fileManager.getNextLinePointer(nextOffset, fileName);
                if (line == null) {
                    return endOfData();
                }
                return new Word(line, offset, WordNet.this);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /** {@inheritDoc} */
    public Iterable<Word> words(final POS pos) {
        if (pos == POS.ALL) {
            return merge(words(POS.NOUN), words(POS.VERB), words(POS.ADJ), words(POS.ADV));
        } else {
            return new Iterable<Word>() {

                public Iterator<Word> iterator() {
                    return new WordIterator(pos);
                }
            };
        }
    }

    /**
   * @see WordNetInterface#searchBySubstring
   */
    private class SearchBySubstringIterator extends AbstractWordIterator {

        private final Matcher matcher;

        SearchBySubstringIterator(final POS pos, final CharSequence pattern) {
            super(pos);
            this.matcher = Pattern.compile(Morphy.searchNormalize(pattern.toString())).matcher("");
        }

        @Override
        protected Word computeNext() {
            try {
                skipLicenseLines();
                final int offset = fileManager.getMatchingLinePointer(nextOffset, matcher, fileName);
                if (offset >= 0) {
                    final Word value = getIndexWordAt(pos, offset);
                    nextOffset = fileManager.getNextLinePointer(offset, fileName);
                    return value;
                } else {
                    return endOfData();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /** {@inheritDoc} */
    public Iterable<Word> searchBySubstring(final CharSequence substring, final POS pos) {
        if (pos == POS.ALL) {
            return merge(searchBySubstring(substring, POS.NOUN), searchBySubstring(substring, POS.VERB), searchBySubstring(substring, POS.ADJ), searchBySubstring(substring, POS.ADV));
        } else {
            return new Iterable<Word>() {

                public Iterator<Word> iterator() {
                    return new SearchBySubstringIterator(pos, substring);
                }
            };
        }
    }

    /**
   * @see WordNetInterface#searchByPrefix
   */
    private class SearchByPrefixIterator extends AbstractIterator<Word> {

        private final POS pos;

        private final CharSequence prefix;

        private final String fileName;

        private int nextOffset;

        SearchByPrefixIterator(final POS pos, final CharSequence prefix) {
            this.pos = pos;
            this.prefix = Morphy.searchNormalize(prefix.toString());
            this.fileName = getIndexFileName(pos);
        }

        @Override
        protected Word computeNext() {
            try {
                final int offset = fileManager.getPrefixMatchLinePointer(nextOffset, prefix, fileName);
                if (offset >= 0) {
                    final Word value = getIndexWordAt(pos, offset);
                    nextOffset = fileManager.getNextLinePointer(offset, fileName);
                    return value;
                } else {
                    return endOfData();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /** {@inheritDoc} */
    public Iterable<Word> searchByPrefix(final CharSequence prefix, final POS pos) {
        if (pos == POS.ALL) {
            return merge(searchByPrefix(prefix, POS.NOUN), searchByPrefix(prefix, POS.VERB), searchByPrefix(prefix, POS.ADJ), searchByPrefix(prefix, POS.ADV));
        } else {
            return new Iterable<Word>() {

                public Iterator<Word> iterator() {
                    return new SearchByPrefixIterator(pos, prefix);
                }
            };
        }
    }

    /**
   * @see WordNetInterface#searchGlossBySubstring
   */
    private class SearchGlossBySubstringIterator extends AbstractIterator<Synset> {

        private final Iterator<Synset> syns;

        private final Matcher matcher;

        SearchGlossBySubstringIterator(final POS pos, final CharSequence pattern) {
            this.syns = synsets(pos).iterator();
            this.matcher = Pattern.compile(pattern.toString()).matcher("");
        }

        @Override
        protected Synset computeNext() {
            while (syns.hasNext()) {
                final Synset syn = syns.next();
                if (matcher.reset(syn.getGloss()).find()) {
                    return syn;
                }
            }
            return endOfData();
        }
    }

    /** {@inheritDoc} */
    public Iterable<Synset> searchGlossBySubstring(final CharSequence substring, final POS pos) {
        if (pos == POS.ALL) {
            return merge(searchGlossBySubstring(substring, POS.NOUN), searchGlossBySubstring(substring, POS.VERB), searchGlossBySubstring(substring, POS.ADJ), searchGlossBySubstring(substring, POS.ADV));
        } else {
            return new Iterable<Synset>() {

                public Iterator<Synset> iterator() {
                    return new SearchGlossBySubstringIterator(pos, substring);
                }
            };
        }
    }

    Iterable<Synset> synsets(final Lexname lexname) {
        return new Iterable<Synset>() {

            public Iterator<Synset> iterator() {
                return new LexnameIterator(lexname);
            }
        };
    }

    private class LexnameIterator extends AbstractIterator<Synset> {

        private final Iterator<Synset> syns;

        private final Lexname lexname;

        LexnameIterator(final Lexname lexname) {
            this.syns = synsets(lexname.getPOS()).iterator();
            this.lexname = lexname;
        }

        @Override
        protected Synset computeNext() {
            while (syns.hasNext()) {
                final Synset syn = syns.next();
                if (lexname == syn.getLexname()) {
                    return syn;
                }
            }
            return endOfData();
        }
    }

    Iterable<WordSense> wordSenses(final AdjPosition adjPosition) {
        return new Iterable<WordSense>() {

            public Iterator<WordSense> iterator() {
                return new AdjPositionIterator(adjPosition);
            }
        };
    }

    private class AdjPositionIterator extends AbstractIterator<WordSense> {

        private final Iterator<WordSense> wordSenses;

        private final AdjPosition adjPosition;

        AdjPositionIterator(final AdjPosition adjPosition) {
            this.wordSenses = wordSenses(POS.ADJ).iterator();
            this.adjPosition = adjPosition;
        }

        @Override
        protected WordSense computeNext() {
            while (wordSenses.hasNext()) {
                final WordSense wordSense = wordSenses.next();
                if (adjPosition == wordSense.getAdjPosition()) {
                    return wordSense;
                }
            }
            return endOfData();
        }
    }

    /**
   * @see WordNetInterface#synsets
   */
    private class POSSynsetsIterator extends AbstractIterator<Synset> {

        private final POS pos;

        private final String fileName;

        private int nextOffset;

        POSSynsetsIterator(final POS pos) {
            this.pos = pos;
            this.fileName = getDataFilename(pos);
        }

        @Override
        protected Synset computeNext() {
            try {
                String line;
                int offset;
                do {
                    if (nextOffset < 0) {
                        throw new NoSuchElementException();
                    }
                    line = fileManager.readLineAt(nextOffset, fileName);
                    offset = nextOffset;
                    if (line == null) {
                        return endOfData();
                    }
                    nextOffset = fileManager.getNextLinePointer(nextOffset, fileName);
                } while (line.startsWith("  "));
                return getSynsetAt(pos, offset, line);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /** {@inheritDoc} */
    public Iterable<Synset> synsets(final POS pos) {
        if (pos == POS.ALL) {
            return merge(synsets(POS.NOUN), synsets(POS.VERB), synsets(POS.ADJ), synsets(POS.ADV));
        } else {
            return new Iterable<Synset>() {

                public Iterator<Synset> iterator() {
                    return new POSSynsetsIterator(pos);
                }
            };
        }
    }

    /**
   * @see WordNetInterface#wordSenses
   */
    private class POSWordSensesIterator extends AbstractIterator<WordSense> {

        private final Iterator<WordSense> wordSenses;

        POSWordSensesIterator(final POS pos) {
            this.wordSenses = concat(words(pos)).iterator();
        }

        @Override
        protected WordSense computeNext() {
            if (wordSenses.hasNext()) {
                return wordSenses.next();
            }
            return endOfData();
        }
    }

    /** {@inheritDoc} */
    public Iterable<WordSense> wordSenses(final POS pos) {
        if (pos == POS.ALL) {
            return merge(wordSenses(POS.NOUN), wordSenses(POS.VERB), wordSenses(POS.ADJ), wordSenses(POS.ADV));
        } else {
            return new Iterable<WordSense>() {

                public Iterator<WordSense> iterator() {
                    return new POSWordSensesIterator(pos);
                }
            };
        }
    }

    /**
   * @see WordNetInterface#relations
   */
    private class POSRelationsIterator extends AbstractIterator<Relation> {

        private final Iterator<Relation> relations;

        POSRelationsIterator(final POS pos, final RelationType relationType) {
            this.relations = concat(transform(synsets(pos), new SynsetToRelations(relationType))).iterator();
        }

        @Override
        protected Relation computeNext() {
            if (relations.hasNext()) {
                return relations.next();
            }
            return endOfData();
        }
    }

    private static class SynsetToRelations implements Function<Synset, List<Relation>> {

        private final RelationType relationType;

        SynsetToRelations(final RelationType relationType) {
            this.relationType = relationType;
        }

        @Override
        public List<Relation> apply(final Synset synset) {
            if (relationType == null) {
                return synset.getRelations();
            } else {
                return synset.getRelations(relationType);
            }
        }
    }

    /** {@inheritDoc} */
    public Iterable<Relation> relations(final POS pos) {
        return relations(null, pos);
    }

    /** {@inheritDoc} */
    public Iterable<Relation> relations(final RelationType relationType, final POS pos) {
        if (pos == POS.ALL) {
            return merge(relations(relationType, POS.NOUN), relations(relationType, POS.VERB), relations(relationType, POS.ADJ), relations(relationType, POS.ADV));
        } else {
            return new Iterable<Relation>() {

                public Iterator<Relation> iterator() {
                    return new POSRelationsIterator(pos, relationType);
                }
            };
        }
    }

    /**
   * @see WordNetInterface#exceptions
   */
    private class POSExceptionsIterator extends AbstractIterator<List<String>> {

        private final POS pos;

        private final String fileName;

        private int nextOffset;

        POSExceptionsIterator(final POS pos) {
            this.pos = pos;
            this.fileName = getExceptionsFilename(pos);
        }

        @Override
        protected List<String> computeNext() {
            try {
                final String line = fileManager.readLineAt(nextOffset, fileName);
                if (line == null) {
                    return endOfData();
                }
                nextOffset = fileManager.getNextLinePointer(nextOffset, fileName);
                final LightImmutableList<String> toReturn = LightImmutableList.of(line.split(" "));
                assert toReturn.size() >= 2;
                return toReturn;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /** {@inheritDoc} */
    public Iterable<List<String>> exceptions(final POS pos) {
        if (pos == POS.ALL) {
            return concat(exceptions(POS.NOUN), exceptions(POS.VERB), exceptions(POS.ADJ), exceptions(POS.ADV));
        } else {
            return new Iterable<List<String>>() {

                public Iterator<List<String>> iterator() {
                    return new POSExceptionsIterator(pos);
                }
            };
        }
    }
}
