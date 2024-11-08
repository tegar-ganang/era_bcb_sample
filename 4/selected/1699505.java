package verjinxer.sequenceanalysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import verjinxer.util.ArrayFile;
import verjinxer.util.HugeByteArray;

/**
 * 
 * @author Sven Rahmann
 */
public class Alphabet {

    static final int INVALID = 0;

    static final int NORMAL = 1;

    static final int WHITESPACE = 2;

    static final int WILDCARD = 4;

    static final int SEPARATOR = 8;

    static final int ENDOFLINE = 16;

    private String[] initstrings;

    private final byte[] myimage;

    private final byte[] mypreimage;

    private final byte[] modepreimage;

    /** maps a code to a mode */
    private final byte[] modeimage;

    private static int mywhitespace = 128 + WHITESPACE;

    private static int mywildcard = 128 + WILDCARD;

    private static int myseparator = 128 + SEPARATOR;

    private static int myendofline = 128 + ENDOFLINE;

    private Alphabet() {
        myimage = new byte[256];
        mypreimage = new byte[256];
        modepreimage = new byte[256];
        modeimage = new byte[256];
    }

    /**
    * creates an alphabet map from the given text lines. It is best to see the example alphabet maps
    * for how to do this, e.g., which strings create the DNA() map, etc.
    * 
    * If a line starts with '##', it is a control command ('##symbols', '##wildcards',
    * '##separators', '##whitespaces', '##endofline') and determines, what kind of characters are
    * given followed.<br>
    * If '##symbols' ends with an extra number (e.g. '##symbols:0'), than the characters in the next
    * line are mapped to that number. For each new line, the number mapped to will be incremented.<br>
    * If any other command ends with an extra number, the number will be the special code for the
    * corresponding kind ('##separators:-1' determines '-1' as special separator that is returned by
    * {@link #codeSeparator()}).
    * 
    * @param lines
    *           the text lines from which to create the alphabet map
    * @return the created alphabet map
    */
    public Alphabet(final String[] lines) {
        this();
        initstrings = lines;
        int i = 0;
        byte mode = NORMAL;
        for (String l : lines) {
            if (l.startsWith("##")) {
                String ll = l.substring(2);
                String istring;
                int icolon = ll.indexOf(':');
                if (icolon >= 0) {
                    istring = ll.substring(icolon + 1);
                    if (istring.length() > 0) i = Integer.decode(istring);
                    ll = ll.substring(0, icolon);
                }
                int ii = (i < 0) ? i + 256 : i;
                ll = ll.toLowerCase();
                if (ll.startsWith("symbol")) mode = NORMAL; else if (ll.startsWith("wildcard")) {
                    mode = WILDCARD;
                    modeimage[ii] = mode;
                    mywildcard = i;
                } else if (ll.startsWith("separator")) {
                    mode = SEPARATOR;
                    modeimage[ii] = mode;
                    myseparator = i;
                } else if (ll.startsWith("endofline")) {
                    mode = ENDOFLINE;
                    modeimage[ii] = mode;
                    myendofline = i;
                } else if (ll.startsWith("whitespace")) {
                    mode = WHITESPACE;
                    modeimage[ii] = mode;
                    mywhitespace = i;
                    mypreimage[ii] = ' ';
                    for (char cc = 0; cc <= 32; cc++) if (Character.isWhitespace(cc)) {
                        modepreimage[cc] = mode;
                        myimage[cc] = (byte) i;
                    }
                } else throw new RuntimeException("Invalid annotation in alphabet map file: " + ll);
            } else {
                int ii = (i < 0) ? i + 256 : i;
                byte[] characters = l.getBytes();
                if (characters.length == 0) {
                    modeimage[ii] = mode;
                    i++;
                    continue;
                }
                mypreimage[ii] = characters[0];
                modeimage[ii] = mode;
                for (int j = 0; j < characters.length; j++) {
                    int ch = characters[j];
                    if (ch < 0) ch += 256;
                    myimage[ch] = (byte) i;
                    modepreimage[ch] = mode;
                }
                i++;
            }
        }
    }

    /**
    * Another method to initialize an alphabet map. This method reads all lines from a given text
    * file into a String[] and creates the alphabet map from these.
    * 
    * @param file
    *           the text file
    * @return the created alphabet map
    * @throws FileNotFoundException
    *            if the file does not exist, is a directory rather than a regular file, or for some
    *            other reason cannot be opened for reading.
    * @throws IOException
    *            if an I/O error occurs.
    */
    public static Alphabet fromFile(final File file) throws FileNotFoundException, IOException {
        ArrayList<String> lines = new ArrayList<String>();
        BufferedReader inf = new BufferedReader(new FileReader(file));
        String s;
        while ((s = inf.readLine()) != null) lines.add(s);
        inf.close();
        return new Alphabet(lines.toArray(new String[0]));
    }

    /**
    * checks whether a given character can be translated by this alphabet map
    * 
    * @param p
    *           the character
    * @return true if p can be translated
    */
    public final boolean isPreValid(final int p) {
        return (modepreimage[(p < 0) ? (p + 256) : p] != 0);
    }

    public final boolean isValid(final int i) {
        return (modeimage[(i < 0) ? (i + 256) : i] != 0);
    }

    public final boolean isSymbol(final int i) {
        byte m = modeimage[(i < 0) ? (i + 256) : i];
        return m == NORMAL || m == WHITESPACE;
    }

    public final boolean isWildcard(final int i) {
        byte m = modeimage[(i < 0) ? (i + 256) : i];
        return m == WILDCARD;
    }

    public final boolean isSeparator(final int i) {
        byte m = modeimage[(i < 0) ? (i + 256) : i];
        return m == SEPARATOR;
    }

    public final boolean isEndOfLine(final int i) {
        byte m = modeimage[(i < 0) ? (i + 256) : i];
        return m == ENDOFLINE;
    }

    public final boolean isSpecial(final int i) {
        byte m = modeimage[(i < 0) ? (i + 256) : i];
        return m == WILDCARD || m == SEPARATOR || m == ENDOFLINE;
    }

    public final int smallestSymbol() {
        for (int i = -128; i < 128; i++) if (isSymbol(i)) return i;
        return -1;
    }

    public final int largestSymbol() {
        for (int i = 127; i >= -128; i--) if (isSymbol(i)) return i;
        return -1;
    }

    /**
    * @return The smallest Character belonging to this alphabet or 'Integer.MIN_VALUE' if no valid
    *         character exists.
    * @author Markus Kemmerling
    */
    public final int smallestCharacter() {
        for (int i = -128; i < 128; i++) if (isValid(i)) return i;
        return Integer.MIN_VALUE;
    }

    /**
    * @return The largest Character belonging to this alphabet or 'Integer.MIN_VALUE' if no valid
    *         character exists.
    * @author Markus Kemmerling
    */
    public final int largestCharacter() {
        for (int i = 127; i >= -128; i--) if (isValid(i)) return i;
        return Integer.MIN_VALUE;
    }

    /**
    * @return Number of Symbols (that means no special characters) belonging to this alphabet.
    * @author Markus Kemmerling
    */
    public final int size() {
        final int smallest = smallestSymbol();
        final int largest = largestSymbol();
        if (smallest == Integer.MIN_VALUE || largest == Integer.MIN_VALUE) {
            return 0;
        } else {
            return largest - smallest + 1;
        }
    }

    /**
    * print the image of the alphabet map to a PrintWriter
    * 
    * @param out
    *           the PrintWriter
    */
    public void showImage(PrintWriter out) {
        for (int i = 0; i < 256; i++) {
            if (modeimage[i] == 0) continue;
            out.printf("%d  %d  %d", i, modeimage[i], mypreimage[i]);
            out.println();
        }
        out.flush();
    }

    public void showImage() {
        this.showImage(new PrintWriter(System.out));
    }

    public void showPreimage(PrintWriter out) {
        for (int i = 0; i < 256; i++) {
            if (modepreimage[i] == 0) continue;
            out.printf("%d   %d  %d", i, modepreimage[i], myimage[i]);
            out.println();
        }
        out.flush();
    }

    public void showPreimage() {
        this.showPreimage(new PrintWriter(System.out));
    }

    /**
    * write source strings to a given PrintWriter
    * 
    * @param out
    *           the PrintWriter to write to
    */
    public void showSourceStrings(PrintWriter out) {
        for (String s : initstrings) {
            out.println(s);
        }
        out.flush();
    }

    /** write source strings to System.out */
    public void showSourceStrings() {
        this.showSourceStrings(new PrintWriter(System.out));
    }

    /** @return array of strings that define the alphabet map */
    public String[] asStrings() {
        return initstrings;
    }

    /**
    * @param b
    *           the character to translate
    * @return the code corresponding to character b
    * @throws verjinxer.sequenceanalysis.InvalidSymbolException
    */
    public final byte code(byte b) throws InvalidSymbolException {
        int bb = (b < 0) ? b + 256 : b;
        if (modepreimage[bb] == 0) throw new InvalidSymbolException("Symbol " + bb + " (" + (char) bb + ") not in alphabet");
        return myimage[bb];
    }

    public final byte codeSeparator() throws InvalidSymbolException {
        if (myseparator < -128 || myseparator > 127) throw new InvalidSymbolException();
        return (byte) myseparator;
    }

    public final byte codeWhitespace() throws InvalidSymbolException {
        if (mywhitespace < -128 || mywhitespace > 127) throw new InvalidSymbolException();
        return (byte) mywhitespace;
    }

    public final byte codeWildcard() throws InvalidSymbolException {
        if (mywildcard < -128 || mywildcard > 127) throw new InvalidSymbolException();
        return (byte) mywildcard;
    }

    public final byte codeEndOfLine() throws InvalidSymbolException {
        if (myendofline < -128 || myendofline > 127) throw new InvalidSymbolException();
        return (byte) myendofline;
    }

    /**
    * Computes the pre-image of a given code.
    * 
    * @param c
    *           the code
    * @return the charecter that corresponds to the pre-image of the given code under this alphabet
    *         map.
    * @throws verjinxer.sequenceanalysis.InvalidSymbolException
    *            if the code is not valid.
    */
    public final char preimage(byte c) throws InvalidSymbolException {
        int cc = (c < 0) ? c + 256 : c;
        if (modeimage[cc] == 0) throw new InvalidSymbolException("Code " + cc + " not in alphabet");
        return (char) (mypreimage[cc] < 0 ? mypreimage[cc] + 256 : mypreimage[cc]);
    }

    /**
    * Compute the pre-image of an array of given codes
    * 
    * @param a
    *           the array of code values
    * @param offset
    *           where to start computing pre-images in a
    * @param len
    *           how many pre-images to compute
    * @return the string of concatenated pre-images of a[offset .. offset+len-1]
    * @throws verjinxer.sequenceanalysis.InvalidSymbolException
    *            if there is a problem
    */
    public final String preimage(byte[] a, int offset, int len) throws InvalidSymbolException {
        StringBuilder s = new StringBuilder(len);
        for (int i = 0; i < len; i++) s.append(preimage(a[offset + i]));
        return s.toString();
    }

    /**
    * Computes the pre-image of an array of given codes. Equivalent to preimage(a, 0, a.length)
    * 
    * @param a
    *           the array of code values
    * @param offset
    *           where to start computing pre-images in a
    * @param len
    *           how many pre-images to compute
    * @return the string of concatenated pre-images of a[offset .. offset+len-1]
    * @throws verjinxer.sequenceanalysis.InvalidSymbolException
    *            if there is a problem
    */
    public String preimage(byte[] a) throws InvalidSymbolException {
        return preimage(a, 0, a.length);
    }

    /**
    * Compute the pre-image of an array of given codes
    * 
    * @param a
    *           the array of code values
    * @param offset
    *           where to start computing pre-images in a
    * @param len
    *           how many pre-images to compute
    * @return the string of concatenated pre-images of a[offset .. offset+len-1]
    * @throws verjinxer.sequenceanalysis.InvalidSymbolException
    *            if there is a problem
    */
    public final String preimage(HugeByteArray a, long offset, int len) throws InvalidSymbolException {
        StringBuilder s = new StringBuilder(len);
        for (int i = 0; i < len; i++) s.append(preimage(a.get(offset + i)));
        return s.toString();
    }

    /**
    * translate a string, and possibly append a separator at the end
    * 
    * @param s
    *           the string
    * @param appendSeparator
    *           set to true if you want to append a separator at the end
    * @return the translated byte array
    * @throws verjinxer.sequenceanalysis.InvalidSymbolException
    */
    public byte[] applyTo(final String s, final boolean appendSeparator) throws InvalidSymbolException {
        return applyTo(s, appendSeparator, false);
    }

    public byte[] applyTo(final String s, final boolean appendSeparator, final boolean separateByWildcard) throws InvalidSymbolException {
        int l = s.length();
        byte[] ba = new byte[(appendSeparator ? l + 1 : l)];
        for (int i = 0; i < l; i++) ba[i] = code((byte) s.charAt(i));
        if (appendSeparator) ba[l] = (separateByWildcard ? codeWildcard() : codeSeparator());
        return ba;
    }

    /**
    * translate byte array in place
    * 
    * @param s
    *           the original byte array (modified during the process!)
    * @param setSeparator
    *           set to true if you want to set the last byte in s to the separator
    * @throws verjinxer.sequenceanalysis.InvalidSymbolException
    */
    public void applyTo(final byte[] s, final boolean setSeparator) throws InvalidSymbolException {
        applyTo(s, setSeparator, false);
    }

    /**
    * translate byte array in place
    * 
    * @param s
    *           the original byte array (modified during the process!)
    * @param setSeparator
    *           set to true if you want to set the last byte in s to the separator
    * @param separateByWildcard
    *           set to true if you want to use the wildcard code instead of the separator code at
    *           the end (only has and effect if setSeparator is true)
    * @throws verjinxer.sequenceanalysis.InvalidSymbolException
    */
    public void applyTo(final byte[] s, final boolean setSeparator, final boolean separateByWildcard) throws InvalidSymbolException {
        int l = s.length;
        int ll = l - (setSeparator ? 1 : 0);
        for (int i = 0; i < ll; i++) s[i] = code(s[i]);
        if (setSeparator) s[ll] = (separateByWildcard ? codeWildcard() : codeSeparator());
    }

    /**
    * translate string into ByteBuffer (may be null -> reallocate)
    * 
    * @param sequence
    *           the string to translate
    * @param buf
    *           the ByteBuffer to translate s into. This can be null, in which case a new byte
    *           buffer is allocated. A new buffer is also allocated if the given buffer is too small
    *           to fit the translated string.
    * @param append
    *           set to true to append a character (eg, a separator) at the end
    * @param appendwhat
    *           specify the character to append
    * @return either the target ByteBuffer buf, or a newly allocated buffer
    * @throws verjinxer.sequenceanalysis.InvalidSymbolException
    */
    public ByteBuffer applyTo(final String sequence, ByteBuffer buf, final boolean append, final byte appendwhat) throws InvalidSymbolException {
        int ll = sequence.length();
        int req = ll + (append ? 1 : 0);
        if (buf == null || buf.capacity() < req) buf = ByteBuffer.allocateDirect(req + 1024);
        buf.limit(buf.capacity());
        buf.position(0);
        for (int i = 0; i < ll; i++) buf.put(code((byte) sequence.charAt(i)));
        if (append) buf.put(appendwhat);
        assert (buf.position() == req);
        buf.flip();
        return buf;
    }

    /**
    * @return the color space alphabet
    * @author Markus Kemmerling
    */
    public static final Alphabet CS() {
        return new Alphabet(new String[] { "##symbols:0", "0", "1", "2", "3", "##wildcards", ".ACGT4NUacgtnu", "##wildcards", "#", "##separators:-1", "##endofline:-2" });
    }

    /**
    * @return the standard DNA alphabet
    */
    public static final Alphabet DNA() {
        return new Alphabet(new String[] { "##symbols:0", "Aa", "Cc", "Gg", "TtUu", "##wildcards", "XxNnWwRrKkYySsMmBbHhDdVv", "##wildcards", "#", "##separators:-1", "##endofline:-2" });
    }

    /**
    * @return the standard DNA alphabet, but repeat-masked. This means that lowercase nucleotides
    *         are wildcards.
    */
    public static final Alphabet maskedDNA() {
        return new Alphabet(new String[] { "##symbols:0", "A", "C", "G", "TU", "##wildcards", "XxNnWwRrKkYySsMmBbHhDdVvacgtu", "##wildcards", "#", "##separators:-1", "##endofline:-2" });
    }

    /**
    * @return the standard complementary DNA alphabet
    */
    public static final Alphabet cDNA() {
        return new Alphabet(new String[] { "##symbols:0", "TtUu", "Gg", "Cc", "Aa", "##wildcards", "XxNnWwRrKkYySsMmBbHhDdVv", "##wildcards", "#", "##separators:-1", "##endofline:-2" });
    }

    /**
    * @return the standard complementary DNA alphabet
    */
    public static final Alphabet maskedcDNA() {
        return new Alphabet(new String[] { "##symbols:0", "TU", "G", "C", "A", "##wildcards", "XxNnWwRrKkYySsMmBbHhDdVvtugca", "##wildcards", "#", "##separators:-1", "##endofline:-2" });
    }

    /**
    * @return a representation of the bisulfite-treated nonmethylated DNA alphabet
    */
    public static final Alphabet biDNA() {
        return new Alphabet(new String[] { "##symbols:0", "Aa", "Zz", "Gg", "CcTtUu", "##wildcards", "XxNnWwRrKkYySsMmBbHhDdVv", "##wildcards", "#", "##separators:-1", "##endofline:-2" });
    }

    /**
    * @return a representation of the complementary bisulfite-treated nonmethylated DNA alphabet
    */
    public static final Alphabet cbiDNA() {
        return new Alphabet(new String[] { "##symbols:0", "AaGg", "Cc", "Zz", "TtUu", "##wildcards", "XxNnWwRrKkYySsMmBbHhDdVv", "##wildcards", "#", "##separators:-1", "##endofline:-2" });
    }

    /**
    * @return the numeric alphabet 0..9
    */
    public static final Alphabet NUMERIC() {
        return new Alphabet(new String[] { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "##separators:-1", "##endofline:-2" });
    }

    /**
    * @return the standard protein alphebet
    */
    public static final Alphabet Protein() {
        return new Alphabet(new String[] { "##symbols:0", "Aa", "Cc", "Dd", "Ee", "Ff", "Gg", "Hh", "Ii", "Kk", "Ll", "Mm", "Nn", "Pp", "Qq", "Rr", "Ss", "Tt", "Vv", "Ww", "Yy", "##wildcards", "BbXxZz", "##wildcards", "#", "##separators:-1", "##endofline:-2" });
    }

    /**
    * indicates whether a given file can be translated by this AlphabetMap
    * 
    * @param file
    *           the file
    * @return true if the ArrayFile can be translated, false otherwise
    * @throws java.io.IOException
    *            if an IO error occurs
    */
    public boolean isApplicableToFile(final File file) throws IOException {
        final ArrayFile arf = new ArrayFile(file, 0);
        final long[] counts = arf.byteCounts();
        for (int i = 0; i < counts.length; i++) if (counts[i] > 0 && !this.isPreValid(i)) return false;
        return true;
    }

    /**
    * translate one byte file to another, applying this alphabet map
    * 
    * @param infile
    *           input file
    * @param outfile
    *           output file
    * @param appendSeparator
    *           set true if you want to append a separator at the end
    * @throws java.io.IOException
    *            if an IO exception occurs
    * @throws verjinxer.sequenceanalysis.InvalidSymbolException
    *            if a symbol in the input file cannot be translated
    */
    public void translateFileToFile(final File infile, final File outfile, final boolean appendSeparator) throws IOException, InvalidSymbolException {
        final ArrayFile afin = new ArrayFile(infile, 0);
        final ByteBuffer in = afin.mapR();
        final long length = afin.length();
        final long ll = appendSeparator ? length + 1 : length;
        final ArrayFile afout = new ArrayFile(outfile, 0);
        final ByteBuffer out = afout.mapRW(0, ll);
        for (long i = 0; i < length; i++) out.put(this.code(in.get()));
        if (appendSeparator) out.put(this.codeSeparator());
    }

    /**
    * translate a byte file to a byte array, applying this alphabet map
    * 
    * @param infile
    *           input file
    * @param translation
    *           the array where to store the translated string (must have large enough size). If
    *           null or too small, a new array with sufficient size is allocated.
    * @param appendSeparator
    *           set true if you want to append a separator at the end
    * @return the newly allocated translated byte array, or 'translation'
    * @throws java.io.IOException
    * @throws verjinxer.sequenceanalysis.InvalidSymbolException
    */
    public byte[] translateFileToByteArray(final File infile, byte[] translation, final boolean appendSeparator) throws IOException, InvalidSymbolException {
        final ArrayFile afin = new ArrayFile(infile, 0);
        final ByteBuffer buf = afin.mapR();
        final long length = afin.length();
        final int ll = (int) length + (appendSeparator ? 1 : 0);
        if (translation == null || translation.length < ll) translation = new byte[ll];
        for (int i = 0; i < length; i++) translation[i] = this.code(buf.get());
        if (appendSeparator) translation[ll - 1] = this.codeSeparator();
        return translation;
    }

    /**
    * translate a string to a given file, possibly appending to the end of the file.
    * 
    * @param s
    *           the string to be translated
    * @param file
    *           the translated file
    * @param append
    *           whether to append to an existing file
    * @param writeSeparator
    *           whether to append the separator to the end of the translated string. If both append
    *           and writeSeparator are true, and the existing file does not end with a separator, a
    *           separator is appended prior to appending the translated string and the final
    *           separator.
    * @throws java.io.IOException
    * @throws verjinxer.sequenceanalysis.InvalidSymbolException
    */
    public void translateStringToFile(final String s, final File file, final boolean append, final boolean writeSeparator) throws IOException, InvalidSymbolException {
        final int slen = s.length();
        RandomAccessFile f = new RandomAccessFile(file, "rw");
        FileChannel fcout = f.getChannel();
        long flen = fcout.size();
        long start = append ? flen : 0;
        if (flen > 0 && append && writeSeparator) {
            f.seek(flen - 1);
            if (f.readByte() != this.codeSeparator()) {
                f.writeByte(this.codeSeparator());
                flen++;
            }
        }
        long newlen = append ? (flen + slen) : slen;
        if (writeSeparator) newlen++;
        f.seek(newlen - 1);
        f.writeByte(0);
        f.seek(start);
        MappedByteBuffer buf = fcout.map(MapMode.READ_WRITE, start, newlen - start);
        for (int i = 0; i < slen; i++) buf.put(this.code((byte) s.charAt(i)));
        if (writeSeparator) buf.put(this.codeSeparator());
        fcout.close();
        f.close();
    }
}
