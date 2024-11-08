package org.armedbear.lisp;

import static org.armedbear.lisp.Nil.NIL;
import static org.armedbear.lisp.Lisp.*;

public final class EchoStream extends Stream {

    private final Stream in;

    private final Stream out;

    private int unreadChar = -1;

    public EchoStream(Stream in, Stream out) {
        this.in = in;
        this.out = out;
    }

    public EchoStream(Stream in, Stream out, boolean interactive) {
        this.in = in;
        this.out = out;
        setInteractive(interactive);
    }

    @Override
    public LispObject getElementType() throws ConditionThrowable {
        LispObject itype = in.getElementType();
        LispObject otype = out.getElementType();
        if (itype.equal(otype)) return itype;
        return SymbolConstants.NULL;
    }

    public Stream getInputStream() {
        return in;
    }

    public Stream getOutputStream() {
        return out;
    }

    @Override
    public LispObject typeOf() {
        return SymbolConstants.ECHO_STREAM;
    }

    @Override
    public LispObject classOf() {
        return BuiltInClass.ECHO_STREAM;
    }

    @Override
    public LispObject typep(LispObject type) throws ConditionThrowable {
        if (type == SymbolConstants.ECHO_STREAM) return T;
        if (type == BuiltInClass.ECHO_STREAM) return T;
        return super.typep(type);
    }

    @Override
    public boolean isInputStream() {
        return true;
    }

    @Override
    public boolean isOutputStream() {
        return true;
    }

    @Override
    public boolean isCharacterInputStream() throws ConditionThrowable {
        return in.isCharacterInputStream();
    }

    @Override
    public boolean isBinaryInputStream() throws ConditionThrowable {
        return in.isBinaryInputStream();
    }

    @Override
    public boolean isCharacterOutputStream() throws ConditionThrowable {
        return out.isCharacterOutputStream();
    }

    @Override
    public boolean isBinaryOutputStream() throws ConditionThrowable {
        return out.isBinaryOutputStream();
    }

    @Override
    protected int _readChar() throws ConditionThrowable, java.io.IOException {
        int n = in._readChar();
        if (n >= 0) {
            if (unreadChar < 0) out._writeChar((char) n); else unreadChar = -1;
        }
        return n;
    }

    @Override
    protected void _unreadChar(int n) throws ConditionThrowable, java.io.IOException {
        in._unreadChar(n);
        unreadChar = n;
    }

    @Override
    protected boolean _charReady() throws ConditionThrowable, java.io.IOException {
        return in._charReady();
    }

    @Override
    public void _writeChar(char c) throws ConditionThrowable {
        out._writeChar(c);
    }

    @Override
    public void _writeChars(char[] chars, int start, int end) throws ConditionThrowable {
        out._writeChars(chars, start, end);
    }

    @Override
    public void _writeString(String s) throws ConditionThrowable {
        out._writeString(s);
    }

    @Override
    public void _writeLine(String s) throws ConditionThrowable {
        out._writeLine(s);
    }

    @Override
    public int _readByte() throws ConditionThrowable {
        int n = in._readByte();
        if (n >= 0) out._writeByte(n);
        return n;
    }

    @Override
    public void _writeByte(int n) throws ConditionThrowable {
        out._writeByte(n);
    }

    @Override
    public void _finishOutput() throws ConditionThrowable {
        out._finishOutput();
    }

    @Override
    public void _clearInput() throws ConditionThrowable {
        in._clearInput();
    }

    @Override
    public LispObject close(LispObject abort) throws ConditionThrowable {
        setOpen(false);
        return T;
    }

    @Override
    public LispObject listen() throws ConditionThrowable {
        return in.listen();
    }

    @Override
    public LispObject freshLine() throws ConditionThrowable {
        return out.freshLine();
    }

    @Override
    public String toString() {
        return unreadableString("ECHO-STREAM");
    }

    private static final Primitive MAKE_ECHO_STREAM = new Primitive("make-echo-stream", "input-stream output-stream") {

        @Override
        public LispObject execute(LispObject first, LispObject second) throws ConditionThrowable {
            if (!(first instanceof Stream)) first = type_error(first, SymbolConstants.STREAM);
            if (!(second instanceof Stream)) second = type_error(second, SymbolConstants.STREAM);
            return new EchoStream((Stream) first, (Stream) second);
        }
    };

    private static final Primitive ECHO_STREAM_INPUT_STREAM = new Primitive("echo-stream-input-stream", "echo-stream") {

        @Override
        public LispObject execute(LispObject arg) throws ConditionThrowable {
            if (arg instanceof EchoStream) return ((EchoStream) arg).getInputStream();
            return type_error(arg, SymbolConstants.ECHO_STREAM);
        }
    };

    private static final Primitive ECHO_STREAM_OUTPUT_STREAM = new Primitive("echo-stream-output-stream", "echo-stream") {

        @Override
        public LispObject execute(LispObject arg) throws ConditionThrowable {
            if (arg instanceof EchoStream) return ((EchoStream) arg).getOutputStream();
            return type_error(arg, SymbolConstants.ECHO_STREAM);
        }
    };
}
