package prest.json.parser;

import java.io.IOException;
import java.io.Reader;

/**
 * JSON format parser. Inspired by <a href="http://svn.apache.org/repos/asf/labs/noggit/src/main/java/org/apache/noggit/JSONParser.java"
 * >org.apache.noggit.JSONParser</a>.
 * 
 * <pre>
 * String json = "{\"name\": {\"first\": \"Joe\", \"last\": \"Sixpack\"}}";
 * 
 * JsonParser js = new JsonParser(json);
 * 
 * for (Event event = js.nextEvent(); event != Event.EOF; event = js.nextEvent()) {
 * 	switch (event) {
 * 		case OBJECT_START:
 * 			;
 * 		case OBJECT_END:
 * 			;
 * 		...
 * }
 * </pre>
 * 
 * @author Daniel Buchta
 * @author "Peter Rybar <peter.rybar@centaur.sk>"
 *
 */
public class JsonParser {

    private static final CharArray devNull = new NullCharArray();

    /** nstate flag, '.' already read */
    private static final int HAS_FRACTION = 0x01;

    /** nstate flag, '[eE][+-]?[0-9]' already read */
    private static final int HAS_EXPONENT = 0x02;

    /** input buffer with JSON text in it */
    private final char[] buffer;

    /** current position in the buffer */
    private int start;

    /** end position in the buffer (one past last valid index) */
    private int end;

    /** optional reader to obtain data from */
    private final Reader reader;

    /** true if the end of the stream was reached. */
    private boolean eof = false;

    /** global position = gpos + start */
    private long gpos;

    /** temporary output buffer */
    private final CharArray outputBuffer = new CharArray(64);

    private ParserState[] stack = new ParserState[16];

    /** pointer into the stack of parser states */
    private int pointer = 0;

    /** current parser state */
    private ParserState parserState;

    /** info about value that was just read (or is in the middle of being read) */
    private Event valstate;

    /** boolean value read */
    private boolean booleanValue;

    /** long value read */
    private long longValue;

    /** current state while reading a number */
    private int stateOfNumber;

    /** a dummy buffer we can use to point at other buffers */
    private final CharArray dummyBuffer = new CharArray(null, 0, 0);

    /**
	 * Constructor
	 *
	 * @param in
	 *            reader to read data from
	 */
    public JsonParser(Reader in) {
        this(in, new char[8192]);
    }

    /**
	 * Constructor
	 *
	 * @param data
	 */
    public JsonParser(String data) {
        this(data, 0, data.length());
    }

    /**
	 * Constructor
	 *
	 * @param data
	 * @param start
	 * @param end
	 */
    public JsonParser(String data, int start, int end) {
        this.reader = null;
        this.start = start;
        this.end = end;
        this.buffer = new char[end - start];
        data.getChars(start, end, this.buffer, 0);
    }

    /**
	 * Constructor
	 *
	 * @param in
	 * @param buffer
	 */
    private JsonParser(Reader in, char[] buffer) {
        this.reader = in;
        this.buffer = buffer;
    }

    /** Reads a boolean value */
    public boolean getBoolean() throws IOException {
        goTo(Event.BOOLEAN);
        return this.booleanValue;
    }

    /** Reads a number from the input stream and parses it as a double */
    public double getDouble() throws IOException {
        return Double.parseDouble(getNumberChars().toString());
    }

    /**
	 * Returns the current nesting level, the number of parent objects or
	 * arrays.
	 *
	 * @return the current nesting level, the number of parent objects or
	 *         arrays.
	 */
    public int getLevel() {
        return this.pointer;
    }

    /**
	 * Reads a number from the input stream and parses it as a long, only if the
	 * value will in fact fit into a signed 64 bit integer.
	 */
    public long getLong() throws IOException {
        goTo(Event.LONG);
        return this.longValue;
    }

    /** Reads a null value */
    public void getNull() throws IOException {
        goTo(Event.NULL);
    }

    /**
	 * Returns the characters of a JSON numeric value.
	 * <p/>
	 * The underlying buffer of the returned <code>CharArray</code> should *not*
	 * be modified as it may be shared with the input buffer.
	 * <p/>
	 * The returned <code>CharArray</code> will only be valid up until the next
	 * JSONParser method is called. Any required data should be read before that
	 * point.
	 *
	 * @return the characters of a JSON numeric value.
	 */
    public CharArray getNumberChars() throws IOException {
        Event event = this.valstate == null ? nextEvent() : null;
        if (this.valstate == Event.LONG || this.valstate == Event.NUMBER) {
        } else if (this.valstate == Event.BIGNUMBER) {
            continueNumber(this.outputBuffer);
        } else {
            throw runtimeException("Unexpected " + event);
        }
        this.valstate = null;
        return this.outputBuffer;
    }

    /** Reads a JSON numeric value into the output. */
    public void getNumberChars(CharArray output) throws IOException {
        Event event = this.valstate == null ? nextEvent() : null;
        if (this.valstate == Event.LONG || this.valstate == Event.NUMBER) {
            output.write(this.outputBuffer);
        } else if (this.valstate == Event.BIGNUMBER) {
            continueNumber(output);
        } else {
            throw runtimeException("Unexpected " + event);
        }
        this.valstate = null;
    }

    /** Returns the JSON string value, decoding any escaped characters. */
    public String getString() throws IOException {
        return getStringChars().toString();
    }

    /** Reads a JSON string into the output, decoding any escaped characters. */
    public void getString(CharArray output) throws IOException {
        goTo(Event.STRING);
        readStringChars2(output, this.start);
    }

    /**
	 * Returns the characters of a JSON string value, decoding any escaped
	 * characters.
	 * <p/>
	 * The underlying buffer of the returned <code>CharArray</code> should *not*
	 * be modified as it may be shared with the input buffer.
	 * <p/>
	 * The returned <code>CharArray</code> will only be valid up until the next
	 * JSONParser method is called. Any required data should be read before that
	 * point.
	 *
	 * @return the characters of a JSON string value, decoding any escaped
	 *         characters.
	 */
    public CharArray getStringChars() throws IOException {
        goTo(Event.STRING);
        return readStringChars();
    }

    /**
	 * Returns the next event encountered in the JSON stream, a value of
	 * {@link Enum}
	 */
    public Event nextEvent() throws IOException {
        if (this.valstate == Event.STRING) {
            readStringChars2(devNull, this.start);
        } else if (this.valstate == Event.BIGNUMBER) {
            continueNumber(devNull);
        }
        this.valstate = null;
        if (this.parserState == null) {
            return next(getCharNWS());
        }
        switch(this.parserState) {
            case DID_OBJSTART:
                {
                    int ch = getCharNWS();
                    if (ch == '}') {
                        pop();
                        return Event.OBJECT_END;
                    }
                    if (ch != '"') {
                        throw runtimeException("Expected string");
                    }
                    this.parserState = ParserState.DID_MEMNAME;
                    this.valstate = Event.STRING;
                    return Event.STRING;
                }
            case DID_MEMNAME:
                {
                    int ch = getCharNWS();
                    if (ch != ':') {
                        throw runtimeException("Expected key,value separator ':'");
                    }
                    this.parserState = ParserState.DID_MEMVAL;
                    return next(getChar());
                }
            case DID_MEMVAL:
                {
                    int ch = getCharNWS();
                    if (ch == '}') {
                        pop();
                        return Event.OBJECT_END;
                    } else if (ch != ',') {
                        throw runtimeException("Expected ',' or '}'");
                    }
                    ch = getCharNWS();
                    if (ch != '"') {
                        throw runtimeException("Expected string");
                    }
                    this.parserState = ParserState.DID_MEMNAME;
                    this.valstate = Event.STRING;
                    return Event.STRING;
                }
            case DID_ARRSTART:
                {
                    int ch = getCharNWS();
                    if (ch == ']') {
                        pop();
                        return Event.ARRAY_END;
                    }
                    this.parserState = ParserState.DID_ARRELEM;
                    return next(ch);
                }
            case DID_ARRELEM:
                {
                    int ch = getCharNWS();
                    if (ch == ']') {
                        pop();
                        return Event.ARRAY_END;
                    } else if (ch != ',') {
                        throw runtimeException("Expected ',' or ']'");
                    }
                    return next(getChar());
                }
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("start=").append(this.start);
        result.append(", end=").append(this.end);
        result.append(", state=").append(this.parserState);
        result.append(", valstate=").append(this.valstate);
        return result.toString();
    }

    /**
	 * @param array
	 * @throws IOException
	 *             if an I/O error occurs
	 */
    private void continueNumber(CharArray array) throws IOException {
        if (array != this.outputBuffer) {
            array.write(this.outputBuffer);
        }
        if ((this.stateOfNumber & HAS_EXPONENT) != 0) {
            readExpDigits(array, Integer.MAX_VALUE);
            return;
        }
        if (this.stateOfNumber != 0) {
            readFrac(array, Integer.MAX_VALUE);
            return;
        }
        while (true) {
            int ch = getChar();
            if (ch >= '0' && ch <= '9') {
                array.write(ch);
            } else if (ch == '.') {
                array.write(ch);
                readFrac(array, Integer.MAX_VALUE);
                return;
            } else if (ch == 'e' || ch == 'E') {
                array.write(ch);
                readExponent(array, Integer.MAX_VALUE);
                return;
            } else {
                if (ch != -1) {
                    this.start--;
                }
                return;
            }
        }
    }

    /**
	 * Processes an error
	 *
	 * @param msg
	 *            error msg
	 * @return a {@link RuntimeException}
	 */
    private RuntimeException runtimeException(String msg) {
        if (!this.eof && this.start > 0) {
            this.start--;
        }
        String chs = "char=" + ((this.start >= this.end) ? "(EOF)" : "" + this.buffer[this.start]);
        String pos = "position=" + (this.gpos + this.start);
        String tot = chs + ',' + pos;
        if (msg == null) {
            msg = (this.start >= this.end) ? "Unexpected EOF" : "JSON Parse Error";
        }
        return new RuntimeException(msg + ": " + tot);
    }

    /**
	 * @param arr
	 * @throws IOException
	 *             if an I/O error occurs
	 */
    private void expect(char[] arr) throws IOException {
        for (int i = 1; i < arr.length; i++) {
            int ch = getChar();
            if (ch != arr[i]) {
                if (ch == -1) {
                    throw new RuntimeException("Unexpected EOF");
                } else {
                    throw new RuntimeException("Expected " + new String(arr));
                }
            }
        }
    }

    /**
	 * @throws IOException
	 *             if an I/O error occurs
	 */
    private void fill() throws IOException {
        if (this.reader != null) {
            this.gpos += this.end;
            this.start = 0;
            int num = this.reader.read(this.buffer, 0, this.buffer.length);
            this.end = num >= 0 ? num : 0;
        }
        if (this.start >= this.end) {
            this.eof = true;
        }
    }

    /**
	 * @return
	 * @throws IOException
	 *             if an I/O error occurs
	 */
    private int getChar() throws IOException {
        if (this.start >= this.end) {
            fill();
            if (this.start >= this.end) {
                return -1;
            }
        }
        return this.buffer[this.start++];
    }

    /**
	 * @return
	 * @throws IOException
	 *             if an I/O error occurs
	 */
    private int getCharNWS() throws IOException {
        for (; ; ) {
            int ch = getChar();
            if (!(ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r')) {
                return ch;
            }
        }
    }

    /**
	 * @throws IOException
	 *             if an I/O error occurs
	 */
    private void getMore() throws IOException {
        fill();
        if (this.start >= this.end) {
            throw runtimeException(null);
        }
    }

    /**
	 * @param what
	 * @throws IOException
	 *             if an I/O error occurs
	 */
    private void goTo(Event what) throws IOException {
        if (this.valstate == what) {
            this.valstate = null;
        } else if (this.valstate == null) {
            nextEvent();
            if (this.valstate == what) {
                this.valstate = null;
            } else {
                throw runtimeException("type mismatch");
            }
        } else {
            throw runtimeException("type mismatch");
        }
    }

    /**
	 * @param hexdig
	 * @return
	 */
    private int hexval(int hexdig) {
        if (hexdig >= '0' && hexdig <= '9') {
            return hexdig - '0';
        } else if (hexdig >= 'A' && hexdig <= 'F') {
            return hexdig + (10 - 'A');
        } else if (hexdig >= 'a' && hexdig <= 'f') {
            return hexdig + (10 - 'a');
        }
        throw runtimeException("invalid hex digit");
    }

    /**
	 * Return the next event when parser is in a neutral state (no map
	 * separators or array element separators to read
	 *
	 * @return the next event when parser is in a neutral state
	 * @throws IOException
	 *             if an I/O error occurs
	 */
    private Event next(int ch) throws IOException {
        for (; ; ) {
            switch(ch) {
                case ' ':
                case '\t':
                    break;
                case '\r':
                case '\n':
                    break;
                case '"':
                    this.valstate = Event.STRING;
                    return Event.STRING;
                case '{':
                    push();
                    this.parserState = ParserState.DID_OBJSTART;
                    return Event.OBJECT_START;
                case '[':
                    push();
                    this.parserState = ParserState.DID_ARRSTART;
                    return Event.ARRAY_START;
                case '0':
                    this.outputBuffer.reset();
                    ch = getChar();
                    if (ch == '.') {
                        this.start--;
                        ch = '0';
                        readNumber('0', false);
                        return this.valstate;
                    } else if (ch > '9' || ch < '0') {
                        this.outputBuffer.unsafeWrite('0');
                        this.start--;
                        this.longValue = 0;
                        this.valstate = Event.LONG;
                        return Event.LONG;
                    } else {
                        throw runtimeException("Leading zeros not allowed");
                    }
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    this.outputBuffer.reset();
                    this.longValue = readNumber(ch, false);
                    return this.valstate;
                case '-':
                    this.outputBuffer.reset();
                    this.outputBuffer.unsafeWrite('-');
                    ch = getChar();
                    if (ch < '0' || ch > '9') {
                        throw runtimeException("expected digit after '-'");
                    }
                    this.longValue = readNumber(ch, true);
                    return this.valstate;
                case 't':
                    this.valstate = Event.BOOLEAN;
                    expect(Constant.TRUE.getChars());
                    this.booleanValue = true;
                    return Event.BOOLEAN;
                case 'f':
                    this.valstate = Event.BOOLEAN;
                    expect(Constant.FALSE.getChars());
                    this.booleanValue = false;
                    return Event.BOOLEAN;
                case 'n':
                    this.valstate = Event.NULL;
                    expect(Constant.NULL_VALUE.getChars());
                    return Event.NULL;
                case -1:
                    if (getLevel() > 0) {
                        throw new RuntimeException("Premature EOF");
                    }
                    return Event.EOF;
                default:
                    throw runtimeException(null);
            }
            ch = getChar();
        }
    }

    /** pop parser state (use at end of container) */
    private void pop() {
        if (--this.pointer < 0) {
            throw runtimeException("Unbalanced container");
        } else {
            this.parserState = this.stack[this.pointer];
        }
    }

    /** push current parser state (use at start of new container) */
    private void push() {
        if (this.pointer >= this.stack.length) {
            expendStack();
        }
        this.stack[this.pointer++] = this.parserState;
    }

    /**
	 * Expands the stack
	 */
    private void expendStack() {
        ParserState[] newstack = new ParserState[this.stack.length << 1];
        System.arraycopy(this.stack, 0, newstack, 0, this.stack.length);
        this.stack = newstack;
    }

    /**
	 * Backslash has already been read when this is called
	 *
	 * @return escaped char
	 */
    private char readEscapedChar() throws IOException {
        switch(getChar()) {
            case '"':
                return '"';
            case '\\':
                return '\\';
            case '/':
                return '/';
            case 'n':
                return '\n';
            case 'r':
                return '\r';
            case 't':
                return '\t';
            case 'f':
                return '\f';
            case 'b':
                return '\b';
            case 'u':
                return (char) ((hexval(getChar()) << 12) | (hexval(getChar()) << 8) | (hexval(getChar()) << 4) | (hexval(getChar())));
        }
        throw runtimeException("Invalid character escape in string");
    }

    /**
	 * Called after 'e' or 'E' has been seen to read the rest of the exponent
	 *
	 * @return
	 */
    private Event readExponent(CharArray array, int lim) throws IOException {
        this.stateOfNumber |= HAS_EXPONENT;
        int ch = getChar();
        lim--;
        if (ch == '+' || ch == '-') {
            array.write(ch);
            ch = getChar();
            lim--;
        }
        if (ch < '0' || ch > '9') {
            throw runtimeException("missing exponent number");
        }
        array.write(ch);
        return readExpDigits(array, lim);
    }

    private Event readExpDigits(CharArray arr, int lim) throws IOException {
        while (--lim >= 0) {
            int ch = getChar();
            if (ch >= '0' && ch <= '9') {
                arr.write(ch);
            } else {
                if (ch != -1) {
                    this.start--;
                }
                return Event.NUMBER;
            }
        }
        return Event.BIGNUMBER;
    }

    /** read digits right of decimal point */
    private Event readFrac(CharArray arr, int lim) throws IOException {
        this.stateOfNumber = HAS_FRACTION;
        while (--lim >= 0) {
            int ch = getChar();
            if (ch >= '0' && ch <= '9') {
                arr.write(ch);
            } else if (ch == 'e' || ch == 'E') {
                arr.write(ch);
                return readExponent(arr, lim);
            } else {
                if (ch != -1) {
                    this.start--;
                }
                return Event.NUMBER;
            }
        }
        return Event.BIGNUMBER;
    }

    /**
	 * Returns the long read... only significant if valstate==LONG after this
	 * call. firstChar should be the first numeric digit read.
	 */
    private long readNumber(int firstChar, boolean isNegative) throws IOException {
        this.outputBuffer.unsafeWrite(firstChar);
        long v = '0' - firstChar;
        for (int i = 0; i < 22; i++) {
            int ch = getChar();
            switch(ch) {
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    v = v * 10 - (ch - '0');
                    this.outputBuffer.unsafeWrite(ch);
                    continue;
                case '.':
                    this.outputBuffer.unsafeWrite('.');
                    this.valstate = readFrac(this.outputBuffer, 22 - i);
                    return 0;
                case 'e':
                case 'E':
                    this.outputBuffer.unsafeWrite(ch);
                    this.stateOfNumber = 0;
                    this.valstate = readExponent(this.outputBuffer, 22 - i);
                    return 0;
                default:
                    if (ch != -1) {
                        --this.start;
                    }
                    if (isNegative) {
                        this.valstate = v <= 0 ? Event.LONG : Event.BIGNUMBER;
                    } else {
                        v = -v;
                        this.valstate = v >= 0 ? Event.LONG : Event.BIGNUMBER;
                    }
                    return v;
            }
        }
        this.stateOfNumber = 0;
        this.valstate = Event.BIGNUMBER;
        return 0;
    }

    private CharArray readStringChars() throws IOException {
        char c = 0;
        int i;
        for (i = this.start; i < this.end; i++) {
            c = this.buffer[i];
            if (c == '"') {
                this.dummyBuffer.set(this.buffer, this.start, i);
                this.start = i + 1;
                return this.dummyBuffer;
            } else if (c == '\\') {
                break;
            }
        }
        this.outputBuffer.reset();
        readStringChars2(this.outputBuffer, i);
        return this.outputBuffer;
    }

    private void readStringChars2(CharArray array, int middle) throws IOException {
        while (true) {
            if (middle >= this.end) {
                array.write(this.buffer, this.start, middle - this.start);
                getMore();
                middle = this.start;
            }
            int ch = this.buffer[middle++];
            if (ch == '"') {
                int len = middle - this.start - 1;
                if (len > 0) {
                    array.write(this.buffer, this.start, len);
                }
                this.start = middle;
                return;
            } else if (ch == '\\') {
                int len = middle - this.start - 1;
                if (len > 0) {
                    array.write(this.buffer, this.start, len);
                }
                this.start = middle;
                array.write(readEscapedChar());
                middle = this.start;
            }
        }
    }

    /** Events produced by this parser */
    public static enum Event {

        /**
		 * Event indicating a JSON string value, including member names of
		 * objects
		 */
        STRING, /**
		 * Event indicating a JSON number value which fits into a signed 64 bit
		 * integer
		 */
        LONG, /**
		 * Event indicating a JSON number value which has a fractional part or
		 * an exponent and with string length <= 23 chars not including sign.
		 * This covers all representations of normal values for
		 * Double.toString().
		 */
        NUMBER, /**
		 * Event indicating a JSON number value that was not produced by
		 * toString of any Java primitive numerics such as Double or Long. It is
		 * either an integer outside the range of a 64 bit signed integer, or a
		 * floating point value with a string representation of more than 23
		 * chars.
		 */
        BIGNUMBER, /** Event indicating a JSON boolean */
        BOOLEAN, /** Event indicating a JSON null */
        NULL, /** Event indicating the start of a JSON object */
        OBJECT_START, /** Event indicating the end of a JSON object */
        OBJECT_END, /** Event indicating the start of a JSON array */
        ARRAY_START, /** Event indicating the end of a JSON array */
        ARRAY_END, /** Event indicating the end of input has been reached */
        EOF
    }

    /** Parser states stored in the stack */
    private static enum ParserState {

        /** '{' just read */
        DID_OBJSTART, /** '[' just read */
        DID_ARRSTART, /** array element just read */
        DID_ARRELEM, /** object member name (map key) just read */
        DID_MEMNAME, /** object member value (map val) just read */
        DID_MEMVAL
    }

    /** JSON constant values */
    private static enum Constant {

        TRUE('t', 'r', 'u', 'e'), FALSE('f', 'a', 'l', 's', 'e'), NULL_VALUE('n', 'u', 'l', 'l');

        private final char[] chars;

        /**
		 * Constructor
		 *
		 * @param chars
		 */
        private Constant(char... chars) {
            this.chars = chars;
        }

        /**
		 * The chars getter
		 *
		 * @return the chars
		 */
        public char[] getChars() {
            return this.chars;
        }
    }
}
