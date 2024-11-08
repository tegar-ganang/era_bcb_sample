package jgloss.dictionary;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.UnsupportedCharsetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;
import java.util.Set;
import jgloss.dictionary.attribute.Attribute;
import jgloss.util.CharacterEncodingDetector;
import jgloss.util.StringTools;

/**
 * Base class for dictionaries stored in a local file with separate index file.
 * <p>
 * The class tries to provide a framework to implement dictionaries which are stored in a file.
 * It is assumed that the dictionary file is a sequence of dictionary entries which are separated by
 * byte-sized entry sepator markers. Each entry is again subdivided into several fields like word,
 * reading or translation. Field division markers can be more complex. Common tasks like index
 * management, searching and search type management are implemented in this class. Derived classes
 * which implement specific dictionary formats must implement the abstract methods which deal
 * with the differences in file formats, especially the parsing of dictionary entries to
 * {@link DictionaryEntry DictionaryEntry} instances.
 * </p>
 *
 * @author Michael Koch
 */
public abstract class FileBasedDictionary implements IndexedDictionary, Indexable, BaseEntry.MarkerDictionary {

    /**
     * Localized messages and strings for the dictionary implementations. Stored in
     * <code>resources/messages-dictionary</code>
     */
    protected static final ResourceBundle NAMES = ResourceBundle.getBundle("resources/messages-dictionary");

    /**
     * Generic implementation for file-based dictionaries. The {@link #isInstance(String) isInstance}
     * test reads some bytes from the file to test, converts them to a string using the
     * superclass-supplied encoding and tests it against a regular expression.
     */
    public static class Implementation implements DictionaryFactory.Implementation {

        protected String name;

        protected String encoding;

        private boolean doEncodingTest;

        private java.util.regex.Pattern pattern;

        private float maxConfidence;

        private int lookAtLength;

        private Constructor dictionaryConstructor;

        /**
         * Creates a new implementation instance for some file based dictionary format.
         *
         * @param name Name of the dictionary format.
         * @param encoding Character encoding used by dictionary files.
         * @param doEncodingTest If <code>true</code>, Use the 
         *        {@link CharacterEncodingDetector CharacterEncodingDetector} to guess the encoding
         *        of the tested file and return <code>ZERO_CONFIDENCE</code> if it does not match
         *        the encoding.
         * @param pattern Regular expression to match against the start of a tested file.
         * @param maxConfidence The confidence which is returned when the <code>linePattern</code>
         *                      matches.
         * @param lookAtLength Number of bytes read from the tested file. If the file is shorter
         *                     than the given length, the file will be read completely.
         * @param dictionaryConstructor Constructor used to create a new dictionary instance for 
         *        a matching file. The constructor must take a single <code>File</code> as parameter.
         */
        public Implementation(String name, String encoding, boolean doEncodingTest, java.util.regex.Pattern pattern, float maxConfidence, int lookAtLength, Constructor dictionaryConstructor) {
            this.name = name;
            this.encoding = encoding;
            this.doEncodingTest = doEncodingTest;
            this.pattern = pattern;
            this.maxConfidence = maxConfidence;
            this.lookAtLength = lookAtLength;
            this.dictionaryConstructor = dictionaryConstructor;
        }

        /**
         * Test if the descriptor points to a dictionary file supported by this implementation.
         * The first {@link #lookAtLength lookAtLength} bytes are read from the file pointed to
         * by the descriptor. The byte array is converted to a string using {@link #encoding encoding}
         * and the {@link #pattern pattern} is tested against it. If the pattern matches, the file
         * is accepted and {@link #maxConfidence maxConfidence} is returned.
         */
        public DictionaryFactory.TestResult isInstance(String descriptor) {
            float confidence = ZERO_CONFIDENCE;
            String reason = "";
            try {
                File dic = new File(descriptor);
                int headlen = (int) Math.min(lookAtLength, dic.length());
                byte[] buffer = new byte[headlen];
                DataInputStream in = new DataInputStream(new FileInputStream(dic));
                try {
                    in.readFully(buffer);
                } finally {
                    in.close();
                }
                boolean encodingMatches = false;
                if (doEncodingTest) {
                    String bufferEncoding = CharacterEncodingDetector.guessEncodingName(buffer);
                    encodingMatches = encoding.equals(bufferEncoding);
                    if (!encodingMatches) {
                        reason = MessageFormat.format(NAMES.getString("dictionary.reason.encoding"), new String[] { bufferEncoding, encoding });
                    }
                }
                if (!doEncodingTest || encodingMatches) {
                    if (pattern.matcher(new String(buffer, encoding)).find()) {
                        confidence = maxConfidence;
                        reason = NAMES.getString("dictionary.reason.ok");
                    } else {
                        reason = NAMES.getString("dictionary.reason.pattern");
                    }
                }
            } catch (IOException ex) {
                confidence = ZERO_CONFIDENCE;
                reason = NAMES.getString("dictionary.reason.read");
                ex.printStackTrace();
            }
            return new DictionaryFactory.TestResult(confidence, reason);
        }

        /**
         * Returns the confidence passed to the constructor.
         */
        public float getMaxConfidence() {
            return maxConfidence;
        }

        /**
         * Returns the dictionary format name passed to the constructor.
         */
        public String getName() {
            return name;
        }

        /**
         * Creates a new dictionary instance using 
         * {@link FileBasedDictionary.Instance#dictionaryConstructor dictionaryConstructor}.
         * The constructor is passed a <code>File</code> wrapping the <code>descriptor</code> as only
         * argument.
         */
        public Dictionary createInstance(String descriptor) throws DictionaryFactory.InstantiationException {
            try {
                return (Dictionary) dictionaryConstructor.newInstance(getConstructorParameters(descriptor));
            } catch (InvocationTargetException ex) {
                throw new DictionaryFactory.InstantiationException((Exception) ((InvocationTargetException) ex).getTargetException());
            } catch (Exception ex) {
                throw new DictionaryFactory.InstantiationException(ex);
            }
        }

        /**
         * Constructs the Object array passed to the Dictionary constructor.
         * Subclasses can override this method if the constructor takes more arguments.
         * Default is a single File object constructed using the descriptor.
         * 
         * @param descriptor Dictionary descriptor passed to createInstance
         * @return The Object array used to pass the arguments to the dictionary constructor.
         */
        protected Object[] getConstructorParameters(String descriptor) {
            return new Object[] { new File(descriptor) };
        }

        public Class getDictionaryClass(String descriptor) {
            return dictionaryConstructor.getDeclaringClass();
        }
    }

    /**
     * File which holds the dictionary.
     */
    protected File dicfile;

    /**
     * Channel used to access the dictionary.
     */
    protected FileChannel dicchannel;

    /**
     * Size of the dictionary in bytes. This equals dicchannel.size(), which is slow.
     */
    protected int dictionarySize;

    /**
     * Dictionary file mapped into a byte buffer.
     */
    protected MappedByteBuffer dictionary;

    /**
     * Duplicate of the  {@link #dictionary dictionary} byte buffer. Created using
     * <code>dictionary.duplicate()</code> and needed for comparison.
     */
    protected ByteBuffer dictionaryDuplicate;

    /**
     * Decoder initialized to convert a byte array to a char array using the charset of the
     * dictionary.
     */
    protected CharsetDecoder decoder;

    /**
     * Stores the character handler created by a call to 
     * {@link #createCharacterHandler(String) createCharacterHandler} and used thorough this class.
     */
    protected EncodedCharacterHandler characterHandler;

    /**
     * Name of the dictionary. This will be the filename of the dictionary file.
     */
    protected String name;

    protected File indexFile;

    /**
     * Container which stores the index data for this dictionary.
     */
    protected IndexContainer indexContainer;

    /**
     * Binary search index which is used for expression searches.
     */
    protected Index binarySearchIndex;

    /**
     * Stores the supported search modes of this dictionary. Initialized in 
     * {@link #initSearchModes() initSearchModes}.
     */
    protected Map supportedSearchModes;

    /**
     * Set of attributes supported by this dictionary implementation. Initialized in
     * {@link #initSupportedAttributes() initSupportedAttributes}.
     */
    protected Map supportedAttributes;

    /**
     * Initializes the dictionary. The dictionary file is opened and mapped into memory.
     * The index file is not loaded from the constructor. Before the dictionary can be used,
     * {@link #loadIndex() loadIndex} must be successfully called.
     *
     * @param _dicfile File which holds the dictionary.
     * @param _encoding Character encoding of the dictionary file.
     * @exception IOException if the dictionary or the index file cannot be read.
     */
    protected FileBasedDictionary(File _dicfile, String _encoding) throws IOException {
        this.dicfile = _dicfile;
        this.name = _dicfile.getName();
        indexFile = new File(dicfile.getCanonicalPath() + FileIndexContainer.EXTENSION);
        characterHandler = createCharacterHandler(_encoding);
        try {
            decoder = Charset.forName(_encoding).newDecoder();
        } catch (UnsupportedCharsetException ex) {
        }
        dicchannel = new FileInputStream(dicfile).getChannel();
        dictionarySize = (int) dicchannel.size();
        dictionary = dicchannel.map(FileChannel.MapMode.READ_ONLY, 0, dictionarySize);
        dictionaryDuplicate = dictionary.duplicate();
        binarySearchIndex = new BinarySearchIndex(BinarySearchIndex.TYPE);
        initSearchModes();
        initSupportedAttributes();
    }

    /**
     * Initialize the map of search modes supported by this dictionary implementation.
     */
    protected void initSearchModes() {
        supportedSearchModes = new HashMap(11);
        SearchFieldSelection fields = new SearchFieldSelection(true, true, true, true, true);
        supportedSearchModes.put(ExpressionSearchModes.EXACT, fields);
        supportedSearchModes.put(ExpressionSearchModes.PREFIX, fields);
        supportedSearchModes.put(ExpressionSearchModes.SUFFIX, fields);
        supportedSearchModes.put(ExpressionSearchModes.ANY, fields);
    }

    /**
     * Initialize the set of supported attributes. Since entry parsing, and thus attribute
     * usage is entirely the responsibility of derived classes, this method only creates
     * an empty set. Derived classes should add their supported attributes.
     */
    protected void initSupportedAttributes() {
        supportedAttributes = new HashMap(11);
    }

    public boolean supports(SearchMode mode, boolean fully) {
        return supportedSearchModes.containsKey(mode);
    }

    public Set getSupportedAttributes() {
        return supportedAttributes.keySet();
    }

    public Set getAttributeValues(Attribute att) {
        if (!supportedAttributes.containsKey(att)) return null;
        Set out = (Set) supportedAttributes.get(att);
        if (out == null) return Collections.EMPTY_SET; else return out;
    }

    public SearchFieldSelection getSupportedFields(SearchMode mode) {
        SearchFieldSelection fields = (SearchFieldSelection) supportedSearchModes.get(mode);
        if (fields != null) return fields; else throw new IllegalArgumentException();
    }

    public boolean loadIndex() throws IndexException {
        if (indexFile.lastModified() < dicfile.lastModified()) {
            indexFile.delete();
            return false;
        }
        try {
            indexContainer = new FileIndexContainer(indexFile, false);
            if (!indexContainer.hasIndex(binarySearchIndex.getType())) return false;
            initIndexes();
            return true;
        } catch (FileNotFoundException ex) {
        } catch (IndexException ex) {
            indexFile.delete();
        } catch (IOException ex) {
            throw new IndexException(ex);
        }
        return false;
    }

    public void buildIndex() throws IndexException {
        try {
            indexContainer = new FileIndexContainer(indexFile, true);
            if (!indexContainer.hasIndex(binarySearchIndex.getType())) {
                IndexBuilder builder = new BinarySearchIndexBuilder(binarySearchIndex.getType());
                builder.startBuildIndex(indexContainer, this);
                boolean commit = false;
                try {
                    addIndexTerms(builder);
                    commit = true;
                } finally {
                    builder.endBuildIndex(commit);
                }
            }
        } catch (IOException ex) {
            throw new IndexException(ex);
        } finally {
            if (indexContainer != null) indexContainer.endEditing();
        }
        initIndexes();
    }

    protected void initIndexes() throws IndexException {
        binarySearchIndex.setContainer(indexContainer);
    }

    /**
     * Create a character handler for the given character encoding.
     * Creates a {@link EUCJPCharacterHandler EUCJPCharacterHandler} for encoding
     * EUC-JP, or a {@link UTF8CharacterHandler UTF8CharacterHandler} for encoding "UTF-8".
     * Throws an <code>IllegalArgumentException</code> otherwise.
     * Subclasses may override this method to create custom character handlers.
     */
    protected EncodedCharacterHandler createCharacterHandler(String encoding) {
        if ("EUC-JP".equals(encoding)) return new EUCJPCharacterHandler(); else if ("UTF-8".equals(encoding)) return new UTF8CharacterHandler(); else throw new IllegalArgumentException(encoding);
    }

    /**
     * Return a character handler which understands the character encoding format used by this
     * dictionary.
     */
    public EncodedCharacterHandler getEncodedCharacterHandler() {
        return characterHandler;
    }

    public ResultIterator search(SearchMode searchmode, Object[] parameters) throws SearchException {
        if (searchmode == ExpressionSearchModes.EXACT || searchmode == ExpressionSearchModes.PREFIX || searchmode == ExpressionSearchModes.SUFFIX || searchmode == ExpressionSearchModes.ANY) {
            return searchExpression(searchmode, (String) parameters[0], (SearchFieldSelection) parameters[1]);
        }
        throw new UnsupportedSearchModeException(searchmode);
    }

    /**
     * Implements search for expression search modes.
     */
    protected ResultIterator searchExpression(SearchMode searchmode, String expression, SearchFieldSelection searchFields) throws SearchException {
        expression = escape(expression);
        try {
            ByteBuffer exprbuf = ByteBuffer.wrap(expression.getBytes(characterHandler.getEncodingName()));
            return new ExpressionSearchIterator(searchmode, searchFields, exprbuf.limit(), binarySearchIndex.getEntryPositions(this, exprbuf, null));
        } catch (UnsupportedEncodingException ex) {
            throw new SearchException(ex);
        }
    }

    /**
     * Copy the data of a single dictionary entry from the dictionary buffer to a newly created
     * buffer.
     *
     * @param matchstart Offset into the entry which is to be copied. This can point anywhere in the
     *        entry, the method will search the start and end of the entry.
     * @param entrybuf Buffer into which the entry data is copied. The returned byte buffer will
     *        wrap this array. If <code>null</code>, a new byte array will be allocated.
     * @param seenEntries Set of integers with start positions of entries which have already been copied.
     *        If this is not <code>null</code>, and the set contains the start offset of the entry
     *        pointed to by <code>matchstart</code>, <code>null</code> is returned instead of the
     *        entry data. This can be used
     *        by the caller of <code>copyEntry</code> to filter out duplicate entries.
     * @param outOffsets Start and end offset of the entry in the dictionary buffer. If this is
     *        an int array of size 2, the start offset (inclusive) will be written to index 0,
     *        the end offset (exclusive) to index 1. May be <code>null</code>.
     * @return A byte buffer containing the dictionary entry data. The buffer will wrap the
     *         <code>entrybuf</code> buffer, its limit will be the length of the entry data and its
     *         position will be the byte pointed to by <code>matchstart</code>.
     */
    protected ByteBuffer copyEntry(int matchstart, byte[] entrybuf, EntrySet seenEntries, int[] outOffsets) {
        if (entrybuf == null) entrybuf = new byte[8192];
        int start = matchstart;
        int end = matchstart + 1;
        byte b;
        try {
            int curr = entrybuf.length - 1;
            while (!isEntrySeparator(b = dictionary.get(start - 1))) {
                try {
                    entrybuf[curr--] = b;
                    start--;
                } catch (ArrayIndexOutOfBoundsException ex) {
                    byte[] entrybuf2 = new byte[entrybuf.length * 2];
                    System.arraycopy(entrybuf, 0, entrybuf2, entrybuf.length, entrybuf.length);
                    entrybuf = entrybuf2;
                    curr = entrybuf.length - 1;
                }
            }
        } catch (IndexOutOfBoundsException ex) {
        }
        if (seenEntries != null && seenEntries.contains(start)) {
            return null;
        }
        try {
            System.arraycopy(entrybuf, entrybuf.length - (matchstart - start), entrybuf, 0, matchstart - start);
        } catch (IndexOutOfBoundsException ex) {
        }
        entrybuf[matchstart - start] = dictionary.get(matchstart);
        int entrylength = matchstart - start + 1;
        try {
            while (!isEntrySeparator(b = dictionary.get(end))) try {
                entrybuf[entrylength++] = b;
                end++;
            } catch (ArrayIndexOutOfBoundsException ex) {
                byte[] entrybuf2 = new byte[entrybuf.length * 2];
                System.arraycopy(entrybuf, 0, entrybuf2, 0, entrylength);
                entrybuf = entrybuf2;
            }
        } catch (BufferUnderflowException ex) {
        }
        ByteBuffer out = ByteBuffer.wrap(entrybuf, 0, entrylength);
        out.position(matchstart - start);
        if (outOffsets != null) {
            outOffsets[0] = start;
            outOffsets[1] = end;
        }
        return out;
    }

    /**
     * Create a dictionary entry from a marker, which is the start offset of the entry.
     * Used from {@link BaseEntry.BaseEntryRef BaseEntryRef} to recreate a dictionary entry.
     */
    public DictionaryEntry createEntryFromMarker(int marker) throws SearchException {
        dictionary.position(marker);
        ByteBuffer entry = dictionary.slice();
        while (!isEntrySeparator(entry.get())) ;
        entry.limit(entry.position() - 1);
        return createEntryFrom(entry, marker);
    }

    /**
     * Create a {@link DictionaryEntry DictionaryEntry} object from the data stored in the byte
     * buffer. The method converts the byte buffer data to a string and invokes
     * {@link #parseEntry(String,int) parseEntry}.
     */
    protected DictionaryEntry createEntryFrom(ByteBuffer entry, int startOffset) throws SearchException {
        String entrystring;
        if (decoder != null) {
            try {
                entrystring = unescape(decoder.decode((ByteBuffer) entry.rewind()).toString());
            } catch (CharacterCodingException ex) {
                throw new SearchException(ex);
            }
        } else {
            try {
                if (entry.hasArray()) {
                    entrystring = unescape(new String(entry.array(), entry.arrayOffset(), entry.limit(), characterHandler.getEncodingName()));
                } else {
                    entry.rewind();
                    byte[] bytes = new byte[entry.limit()];
                    entry.get(bytes);
                    entrystring = unescape(new String(bytes, 0, bytes.length, characterHandler.getEncodingName()));
                }
            } catch (UnsupportedEncodingException ex) {
                throw new SearchException(ex);
            }
        }
        return parseEntry(entrystring, startOffset);
    }

    /**
     * Test if the character at the given location is the first in an entry field. If the dictionary
     * supports several words, readings or translations in one entry, each counts as its own field.
     * 
     * @param entry Buffer which holds the dictionary entry.
     * @param location Location of the first byte of the character.
     * @param field Field which the location is in.
     */
    protected abstract boolean isFieldStart(ByteBuffer entry, int location, DictionaryEntryField field);

    /**
     * Test if the character at the given location is the last in an entry field. If the dictionary
     * supports several words, readings or translations in one entry, each counts as its own field.
     * 
     * @param entry Buffer which holds the dictionary entry.
     * @param location Location of the first byte of the character.
     * @param field Field which the location is in.
     */
    protected abstract boolean isFieldEnd(ByteBuffer entry, int location, DictionaryEntryField field);

    /**
     * Test if the character at the given location is the first in a word. The method first tests
     * if the location is at the start of a field by calling 
     * {@link #isFieldStart(ByteBuffer,int,DictionaryEntryField) isFieldStart}. 
     * If it is not at the start of a field,
     * it calls {@link #isWordBoundary(ByteBuffer,int,DictionaryEntryField) isWordBoundary}.
     * 
     * @param entry Buffer which holds the dictionary entry.
     * @param location Location of the first byte of the character.
     * @param field Field which the location is in.
     */
    protected boolean isWordStart(ByteBuffer entry, int location, DictionaryEntryField field) {
        if (isFieldStart(entry, location, field)) return true;
        return isWordBoundary(entry, location, field);
    }

    /**
     * Test if the character at the given location is the last in a word. The method first tests
     * if the location is at the start of a field by calling 
     * {@link #isFieldEnd(ByteBuffer,int,DictionaryEntryField) isFieldEnd}. 
     * If it is not at the start of a field,
     * it calls {@link #isWordBoundary(ByteBuffer,int,DictionaryEntryField) isWordBoundary}.
     * 
     * @param entry Buffer which holds the dictionary entry.
     * @param location Location of the first byte of the character.
     * @param field Field which the location is in.
     */
    protected boolean isWordEnd(ByteBuffer entry, int location, DictionaryEntryField field) {
        if (isFieldEnd(entry, location, field)) return true;
        return isWordBoundary(entry, location, field);
    }

    /**
     * Test if the characters before and at the given location form a word boundary.
     */
    protected boolean isWordBoundary(ByteBuffer entry, int location, DictionaryEntryField field) {
        try {
            entry.position(location);
            int c1 = characterHandler.readPreviousCharacter(entry);
            entry.position(location);
            int c2 = characterHandler.readCharacter(entry);
            CharacterClass cc1 = characterHandler.getCharacterClass(c1, false);
            boolean inWord = (cc1 == CharacterClass.ROMAN_WORD);
            return (cc1 != characterHandler.getCharacterClass(c2, inWord));
        } catch (BufferOverflowException ex) {
            return true;
        } catch (CharacterCodingException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Test if the byte is the separator mark for two entries. This implementation uses
     * ASCII lf (10) and cr (13) bytes as entry separator.
     *
     * @param <CODE>true</CODE> if the byte separates two entries.
     */
    protected boolean isEntrySeparator(byte c) {
        return (c == 10 || c == 13);
    }

    /**
     * Escape all characters in the string not representable in the dictionary byte array.
     * This can be either chars not representable through the character encoding used by the
     * dictionary, or special characters used as field and entry separators.
     * <p>
     * The method is called to escape search expressions before the search in the dictionary
     * array is begun. The case where different escaping rules are needed for different
     * fields of a dictionary entry is not covered by this escaping scheme.
     * </p><p>
     * This implementation calls {@link #escapeChar(char) escapeChar} for every character
     * in the string and uses a {@link StringTools#unicodeEscape(char) unicode escape sequence}
     * if the method returns <code>true</code>.
     * </p>
     */
    protected String escape(String str) {
        StringBuffer buf = null;
        for (int i = str.length() - 1; i >= 0; i--) {
            if (escapeChar(str.charAt(i))) {
                if (buf == null) buf = new StringBuffer(str);
                buf.replace(i, i + 1, StringTools.unicodeEscape(str.charAt(i)));
            }
        }
        if (buf == null) return str; else return buf.toString();
    }

    /**
     * Test if a character must be escaped if it is to be used in a dictionary entry.
     *
     * @param c The character to test.
     * @return <code>true</code>, if the character must be escaped.
     */
    protected abstract boolean escapeChar(char c);

    /**
     * Replace any escape sequences in the string by the character represented.
     * This is the inverse method to {@link #escape(String) escape}. This implementation
     * calls {@link StringTools#unicodeUnescape(String) StringTools.unicodeUnescape}.
     */
    protected String unescape(String str) {
        return StringTools.unicodeUnescape(str);
    }

    /**
     * Create a {@link DictionaryEntry DictionaryEntry} object from the entry string from the dictionary.
     *
     * @param entry Entry string as found in the dictionary.
     * @param startOffset Start offset of the entry in the dictionary file.
     * @exception SearchException if the dictionary entry is malformed.
     */
    protected abstract DictionaryEntry parseEntry(String entry, int startOffset) throws SearchException;

    public void dispose() {
        try {
            dicchannel.close();
            if (indexContainer != null) indexContainer.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Returns the name of this dictionary. This implemenation uses the filename of the dictionary file.
     *
     * @return The name of this dictionary.
     */
    public String getName() {
        return name;
    }

    /**
     * Adds all indexable terms in the dictionary to the index builder.
     *
     * @return The number of index entries created.
     */
    protected int addIndexTerms(IndexBuilder builder) throws IOException, IndexException {
        int indexsize = 0;
        dictionary.position(0);
        ArrayList termStarts = new ArrayList(25);
        int previousTerm = -1;
        DictionaryEntryField field = moveToNextField(dictionary, 0, null);
        while (dictionary.position() < dictionarySize) try {
            boolean inWord = false;
            int c;
            CharacterClass clazz;
            int termStart;
            DictionaryEntryField termField;
            do {
                termStart = dictionary.position();
                c = characterHandler.readCharacter(dictionary);
                clazz = characterHandler.getCharacterClass(c, inWord);
                field = moveToNextField(dictionary, c, field);
            } while (clazz == CharacterClass.OTHER);
            termStarts.clear();
            termStarts.add(new Integer(termStart));
            termField = field;
            if (clazz == CharacterClass.ROMAN_WORD) inWord = true;
            int termLength = 1;
            int termEnd;
            CharacterClass clazz2;
            do {
                termLength++;
                termEnd = dictionary.position();
                c = characterHandler.readCharacter(dictionary);
                clazz2 = characterHandler.getCharacterClass(c, inWord);
                if (clazz == CharacterClass.KANJI && clazz2 == clazz) termStarts.add(new Integer(termEnd));
            } while (clazz2 == clazz);
            if (clazz == CharacterClass.KANJI || clazz == CharacterClass.HIRAGANA || clazz == CharacterClass.KATAKANA || clazz == CharacterClass.ROMAN_WORD && termLength >= 3) {
                for (int i = 0; i < termStarts.size(); i++) {
                    termStart = ((Integer) termStarts.get(i)).intValue();
                    if (termStart <= previousTerm) System.err.println("Warning: possible duplicate index entry");
                    previousTerm = termStart;
                    if (builder.addEntry(termStart, termEnd - termStart, termField)) indexsize++;
                }
            }
            if (clazz2 != CharacterClass.OTHER) {
                dictionary.position(termEnd);
            } else {
                field = moveToNextField(dictionary, c, field);
            }
        } catch (BufferUnderflowException ex) {
        } catch (IndexOutOfBoundsException ex) {
        }
        return indexsize;
    }

    /**
     * Skip to the next indexable field. This method is called at index
     * creation time before any character is read from the dictionary and after each read character.
     * It can be used to skip entry fields which should not be indexed.
     *
     * @param buf Skip entries in this buffer by moving the current position of the buffer.
     * @param character The last character read from the buffer, or 0 at the first invocation. The
     *                  character format is dependent on 
     *                  {@link EncodedCharacterHandler#readCharacter(ByteBuffer) readCharacter}
     *                  and not neccessaryly unicode.
     * @param field The current field.
     * @return The type of the field the method moved to.
     */
    protected abstract DictionaryEntryField moveToNextField(ByteBuffer buf, int character, DictionaryEntryField field);

    /**
     * Return the type of the entry field at the given location.
     */
    protected abstract DictionaryEntryField getFieldType(ByteBuffer buf, int entryStart, int entryEnd, int location);

    public int compare(int pos1, int pos2) throws IndexException {
        try {
            return compare(dictionary, pos1, Integer.MAX_VALUE, dictionaryDuplicate, pos2);
        } catch (java.nio.charset.CharacterCodingException ex) {
            throw new IndexException(ex);
        }
    }

    public int compare(ByteBuffer data, int position) throws IndexException {
        try {
            return compare(data, 0, data.limit(), dictionary, position);
        } catch (CharacterCodingException ex) {
            throw new IndexException(ex);
        }
    }

    /**
     * Lexicographic comparison of two byte buffers.
     *
     * @param buf1 First buffer to compare.
     * @param i1 Offset in the first buffer to the first string.
     * @param length maximum number of bytes to compare.
     * @param buf2 Second buffer to compare. Must be different from <code>buf1</code> for the comparison
     *             to succeed. If two positions in the same buffer should be compared, use
     *             <code>buf1.duplicate()</code> to create a second independent instance.
     * @param i2 Offset in the second buffer to the second string.
     * @return -1 if i1&lt;i2, 1 if i1&gt;i2, 0 for equality.
     */
    protected int compare(ByteBuffer buf1, int i1, int length, ByteBuffer buf2, int i2) throws CharacterCodingException {
        if (i1 == i2) {
            return 0;
        }
        buf1.position(i1);
        buf2.position(i2);
        int end = (int) Math.min(Integer.MAX_VALUE, (long) i1 + (long) length);
        try {
            while (buf1.position() < end) {
                int b1 = characterHandler.convertCharacter(characterHandler.readCharacter(buf1));
                int b2 = characterHandler.convertCharacter(characterHandler.readCharacter(buf2));
                if (b1 < b2) return -1; else if (b1 > b2) return 1;
            }
        } catch (BufferUnderflowException ex) {
            if (buf1.hasRemaining()) return 1; else if (buf2.hasRemaining()) return -1;
        }
        return 0;
    }

    public Indexable.CharData getChar(int position, CharData result) throws IndexException {
        if (result == null) result = new Indexable.CharData();
        dictionary.position(position);
        try {
            result.character = characterHandler.readCharacter(dictionary);
        } catch (java.nio.charset.CharacterCodingException ex) {
            throw new IndexException(ex);
        }
        result.position = dictionary.position();
        return result;
    }

    /**
     * Iterator returning results from an expression search.
     */
    protected class ExpressionSearchIterator implements ResultIterator {

        protected SearchMode searchmode;

        protected SearchFieldSelection fields;

        protected int expressionLength;

        protected Index.Iterator matchingIndexEntries;

        protected byte[] entrybuf = new byte[8192];

        protected EntrySet seenEntries = new EntrySet();

        protected int[] entryOffsets = new int[2];

        protected DictionaryEntry nextEntry = null;

        protected SearchException deferredException = null;

        public ExpressionSearchIterator(SearchMode _searchmode, SearchFieldSelection _fields, int _expressionLength, Index.Iterator _matchingIndexEntries) throws SearchException {
            this.searchmode = _searchmode;
            this.fields = _fields;
            this.expressionLength = _expressionLength;
            this.matchingIndexEntries = _matchingIndexEntries;
            generateNextEntry();
        }

        public boolean hasNext() {
            return nextEntry != null || deferredException != null;
        }

        public DictionaryEntry next() throws SearchException, NoSuchElementException {
            if (!hasNext()) throw new NoSuchElementException();
            if (deferredException == null) {
                DictionaryEntry current = nextEntry;
                generateNextEntry();
                return current;
            } else {
                SearchException out = new SearchException(deferredException);
                deferredException = null;
                if (out instanceof MalformedEntryException) {
                    generateNextEntry();
                }
                throw out;
            }
        }

        public void remove() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        /**
         * Set the {@link #nextEntry nextEntry} variable to the next dictionary entry matching
         * the search. If there are no more entries, it will be set to <code>null</code>.
         */
        protected void generateNextEntry() {
            nextEntry = null;
            try {
                while (nextEntry == null && matchingIndexEntries.hasNext()) {
                    int match = matchingIndexEntries.next();
                    ByteBuffer entry = copyEntry(match, entrybuf, seenEntries, entryOffsets);
                    if (entry == null) continue;
                    match = entry.position();
                    DictionaryEntryField field = getFieldType(entry, 0, entry.limit(), match);
                    try {
                        if (!fields.isSelected(field)) continue;
                    } catch (IllegalArgumentException ex) {
                        continue;
                    }
                    if (searchmode == ExpressionSearchModes.EXACT || searchmode == ExpressionSearchModes.PREFIX) {
                        if (match > 0 && !(fields.isSelected(MatchMode.WORD) ? isWordStart(entry, match, field) : isFieldStart(entry, match, field))) continue;
                    }
                    if (searchmode == ExpressionSearchModes.EXACT || searchmode == ExpressionSearchModes.SUFFIX) {
                        int matchend = match + expressionLength;
                        if (matchend < entry.limit() && !(fields.isSelected(MatchMode.WORD) ? isWordEnd(entry, matchend, field) : isFieldEnd(entry, matchend, field))) continue;
                    }
                    nextEntry = createEntryFrom(entry, entryOffsets[0]);
                    seenEntries.add(entryOffsets[0]);
                }
            } catch (SearchException ex) {
                deferredException = ex;
            }
        }
    }

    /**
     * Set of integers which represent entry start indexes in the dictionary.
     */
    private static class EntrySet {

        private HashSet entries;

        public EntrySet() {
            entries = new HashSet(51);
        }

        public boolean contains(int entry) {
            return (entries.contains(new Integer(entry)));
        }

        public void add(int entry) {
            entries.add(new Integer(entry));
        }
    }
}
