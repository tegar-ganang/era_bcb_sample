package org.renjin.primitives.io.serialization;

import static org.renjin.primitives.io.serialization.SerializationFormat.BASEENV_SXP;
import static org.renjin.primitives.io.serialization.SerializationFormat.BASENAMESPACE_SXP;
import static org.renjin.primitives.io.serialization.SerializationFormat.BCODESXP;
import static org.renjin.primitives.io.serialization.SerializationFormat.BUILTINSXP;
import static org.renjin.primitives.io.serialization.SerializationFormat.CHARSXP;
import static org.renjin.primitives.io.serialization.SerializationFormat.CLASSREFSXP;
import static org.renjin.primitives.io.serialization.SerializationFormat.CLOSXP;
import static org.renjin.primitives.io.serialization.SerializationFormat.CPLXSXP;
import static org.renjin.primitives.io.serialization.SerializationFormat.DOTSXP;
import static org.renjin.primitives.io.serialization.SerializationFormat.EMPTYENV_SXP;
import static org.renjin.primitives.io.serialization.SerializationFormat.ENVSXP;
import static org.renjin.primitives.io.serialization.SerializationFormat.EXPRSXP;
import static org.renjin.primitives.io.serialization.SerializationFormat.EXTPTRSXP;
import static org.renjin.primitives.io.serialization.SerializationFormat.GENERICREFSXP;
import static org.renjin.primitives.io.serialization.SerializationFormat.GLOBALENV_SXP;
import static org.renjin.primitives.io.serialization.SerializationFormat.INTSXP;
import static org.renjin.primitives.io.serialization.SerializationFormat.LANGSXP;
import static org.renjin.primitives.io.serialization.SerializationFormat.LGLSXP;
import static org.renjin.primitives.io.serialization.SerializationFormat.LISTSXP;
import static org.renjin.primitives.io.serialization.SerializationFormat.MISSINGARG_SXP;
import static org.renjin.primitives.io.serialization.SerializationFormat.NAMESPACESXP;
import static org.renjin.primitives.io.serialization.SerializationFormat.NILVALUE_SXP;
import static org.renjin.primitives.io.serialization.SerializationFormat.PACKAGESXP;
import static org.renjin.primitives.io.serialization.SerializationFormat.PERSISTSXP;
import static org.renjin.primitives.io.serialization.SerializationFormat.PROMSXP;
import static org.renjin.primitives.io.serialization.SerializationFormat.RAWSXP;
import static org.renjin.primitives.io.serialization.SerializationFormat.REALSXP;
import static org.renjin.primitives.io.serialization.SerializationFormat.REFSXP;
import static org.renjin.primitives.io.serialization.SerializationFormat.S4SXP;
import static org.renjin.primitives.io.serialization.SerializationFormat.SPECIALSXP;
import static org.renjin.primitives.io.serialization.SerializationFormat.STRSXP;
import static org.renjin.primitives.io.serialization.SerializationFormat.SYMSXP;
import static org.renjin.primitives.io.serialization.SerializationFormat.UNBOUNDVALUE_SXP;
import static org.renjin.primitives.io.serialization.SerializationFormat.VECSXP;
import static org.renjin.primitives.io.serialization.SerializationFormat.VERSION2;
import static org.renjin.primitives.io.serialization.SerializationFormat.WEAKREFSXP;
import static org.renjin.primitives.io.serialization.SerializationFormat.XDR_NA_BITS;
import java.io.BufferedReader;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math.complex.Complex;
import org.renjin.eval.Context;
import org.renjin.parser.ParseUtil;
import org.renjin.sexp.CHARSEXP;
import org.renjin.sexp.Closure;
import org.renjin.sexp.ComplexVector;
import org.renjin.sexp.DoubleVector;
import org.renjin.sexp.Environment;
import org.renjin.sexp.ExpressionVector;
import org.renjin.sexp.FunctionCall;
import org.renjin.sexp.IntVector;
import org.renjin.sexp.ListVector;
import org.renjin.sexp.LogicalVector;
import org.renjin.sexp.Null;
import org.renjin.sexp.PairList;
import org.renjin.sexp.Promise;
import org.renjin.sexp.SEXP;
import org.renjin.sexp.StringVector;
import org.renjin.sexp.Symbol;

public class RDataReader {

    private final Context context;

    private InputStream conn;

    private StreamReader in;

    private int version;

    private Version writerVersion;

    private Version releaseVersion;

    private List<SEXP> referenceTable;

    private PersistentRestorer restorer;

    public RDataReader(Context context, InputStream conn) {
        this.context = context;
        this.conn = conn;
        this.referenceTable = new ArrayList<SEXP>();
    }

    public RDataReader(Context context, Environment rho, InputStream conn, PersistentRestorer restorer) {
        this(context, conn);
        this.restorer = restorer;
    }

    public SEXP readFile() throws IOException {
        in = readHeader(conn);
        readAndVerifyVersion();
        return readExp();
    }

    protected void readAndVerifyVersion() throws IOException {
        version = in.readInt();
        writerVersion = new Version(in.readInt());
        releaseVersion = new Version(in.readInt());
        if (version != VERSION2) {
            if (releaseVersion.isExperimental()) {
                throw new IOException(String.format("cannot read unreleased workspace version %d written by experimental R %s", version, writerVersion));
            } else {
                throw new IOException(String.format("cannot read workspace version %d written by R %s; need R %s or newer", version, releaseVersion, releaseVersion));
            }
        }
    }

    private static StreamReader readHeader(InputStream conn) throws IOException {
        byte bytes[] = new byte[7];
        bytes[0] = (byte) conn.read();
        bytes[1] = (byte) conn.read();
        if (bytes[1] == '\n') {
            switch(bytes[0]) {
                case 'X':
                    return new XdrReader(conn);
                case 'A':
                    return new AsciiReader(conn);
                case 'B':
                    return new BinaryReader(conn);
                default:
                    throw new IOException("Malformed header: " + Integer.toHexString(bytes[0]) + " " + Integer.toHexString(bytes[1]));
            }
        }
        for (int i = 2; i != 7; ++i) {
            bytes[i] = (byte) conn.read();
        }
        String header = new String(bytes);
        if (header.equals(SerializationFormat.ASCII_FORMAT)) {
            return new AsciiReader(conn);
        } else if (header.equals(SerializationFormat.BINARY_FORMAT)) {
            return new BinaryReader(conn);
        } else if (header.equals(SerializationFormat.XDR_FORMAT)) {
            return new XdrReader(conn);
        }
        throw new IOException("could not read header");
    }

    public SEXP readExp() throws IOException {
        Flags flags = new Flags(in.readInt());
        switch(flags.type) {
            case NILVALUE_SXP:
                return Null.INSTANCE;
            case EMPTYENV_SXP:
                return Environment.EMPTY;
            case BASEENV_SXP:
                return context.getGlobals().baseEnvironment;
            case GLOBALENV_SXP:
                return context.getGlobals().globalEnvironment;
            case UNBOUNDVALUE_SXP:
                return Symbol.UNBOUND_VALUE;
            case MISSINGARG_SXP:
                return Symbol.MISSING_ARG;
            case BASENAMESPACE_SXP:
                return context.getGlobals().baseNamespaceEnv;
            case REFSXP:
                return readReference(flags);
            case PERSISTSXP:
                return readPersistentExp();
            case SYMSXP:
                return readSymbol();
            case PACKAGESXP:
                return readPackage();
            case NAMESPACESXP:
                return readNamespace();
            case ENVSXP:
                return readEnv();
            case LISTSXP:
                return readPairList(flags);
            case LANGSXP:
                return readLangExp(flags);
            case CLOSXP:
                return readClosure(flags);
            case PROMSXP:
                return readPromise(flags);
            case DOTSXP:
                return readDotExp(flags);
            case EXTPTRSXP:
                return readExternalPointer(flags);
            case WEAKREFSXP:
                return readWeakReference(flags);
            case SPECIALSXP:
            case BUILTINSXP:
                return readPrimitive(flags);
            case CHARSXP:
                return readCharExp(flags);
            case LGLSXP:
                return readLogical(flags);
            case INTSXP:
                return readIntVector(flags);
            case REALSXP:
                return readDoubleExp(flags);
            case CPLXSXP:
                return readComplexExp(flags);
            case STRSXP:
                return readStringVector(flags);
            case VECSXP:
                return readListExp(flags);
            case EXPRSXP:
                return readExpExp(flags);
            case BCODESXP:
                throw new IOException("Byte code expressions are not supported.");
            case CLASSREFSXP:
                throw new IOException("this version of R cannot read class references");
            case GENERICREFSXP:
                throw new IOException("this version of R cannot read generic function references");
            case RAWSXP:
                throw new IOException("this version of R cannot read RAWSXP");
            case S4SXP:
                return readS4XP();
            default:
                throw new IOException(String.format("ReadItem: unknown type %d, perhaps written by later version of R", flags.type));
        }
    }

    private SEXP readPromise(Flags flags) throws IOException {
        SEXP attributes = readTag(flags);
        SEXP env = readTag(flags);
        SEXP value = readExp();
        SEXP expr = readExp();
        if (value != Null.INSTANCE) {
            return new Promise(expr, value);
        }
        throw new IOException();
    }

    private SEXP readClosure(Flags flags) throws IOException {
        PairList attributes = readAttributes(flags);
        Environment env = (Environment) readTag(flags);
        PairList formals = (PairList) readExp();
        SEXP body = readExp();
        return new Closure(env, formals, body, attributes);
    }

    private SEXP readLangExp(Flags flags) throws IOException {
        PairList attributes = readAttributes(flags);
        SEXP tag = readTag(flags);
        SEXP function = readExp();
        PairList arguments = (PairList) readExp();
        return new FunctionCall(function, arguments, attributes);
    }

    private SEXP readDotExp(Flags flags) throws IOException {
        throw new IOException("readDotExp not impl");
    }

    private PairList readPairList(Flags flags) throws IOException {
        PairList attributes = (PairList) readAttributes(flags);
        SEXP tag = readTag(flags);
        SEXP value = readExp();
        PairList nextNode = (PairList) readExp();
        return new PairList.Node(tag, value, attributes, nextNode);
    }

    private SEXP readTag(Flags flags) throws IOException {
        return flags.hasTag ? readExp() : Null.INSTANCE;
    }

    private PairList readAttributes(Flags flags) throws IOException {
        return flags.hasAttributes ? (PairList) readExp() : Null.INSTANCE;
    }

    private SEXP readPackage() throws IOException {
        throw new IOException("package");
    }

    private SEXP readReference(Flags flags) throws IOException {
        int i = readReferenceIndex(flags);
        return referenceTable.get(i);
    }

    private int readReferenceIndex(Flags flags) throws IOException {
        int i = flags.unpackRefIndex();
        if (i == 0) return in.readInt() - 1; else return i - 1;
    }

    private SEXP readSymbol() throws IOException {
        CHARSEXP printName = (CHARSEXP) readExp();
        return addReadRef(Symbol.get(printName.getValue()));
    }

    private SEXP addReadRef(SEXP value) {
        referenceTable.add(value);
        return value;
    }

    private SEXP readNamespace() throws IOException {
        StringVector name = readPersistentNamesVector();
        SEXP namespace = context.findNamespace(Symbol.get(name.getElementAsString(0)));
        if (namespace == Null.INSTANCE) {
            throw new IllegalStateException("Cannot find namespace '" + name + "'");
        }
        return addReadRef(namespace);
    }

    private SEXP readEnv() throws IOException {
        int locked = in.readInt();
        Environment env = Environment.createChildEnvironment(Environment.EMPTY);
        addReadRef(env);
        SEXP parent = readExp();
        SEXP frame = readExp();
        SEXP hashtab = readExp();
        SEXP attributes = readExp();
        env.setParent(parent == Null.INSTANCE ? Environment.EMPTY : (Environment) parent);
        env.setVariables((PairList) frame);
        return env;
    }

    private SEXP readS4XP() throws IOException {
        throw new IOException("not yet impl");
    }

    private SEXP readListExp(Flags flags) throws IOException {
        return new ListVector(readExpArray(), readAttributes(flags));
    }

    private SEXP readExpExp(Flags flags) throws IOException {
        return new ExpressionVector(readExpArray(), readAttributes(flags));
    }

    private SEXP[] readExpArray() throws IOException {
        int length = in.readInt();
        SEXP values[] = new SEXP[length];
        for (int i = 0; i != length; ++i) {
            values[i] = readExp();
        }
        return values;
    }

    private SEXP readStringVector(Flags flags) throws IOException {
        int length = in.readInt();
        String[] values = new String[length];
        for (int i = 0; i != length; ++i) {
            values[i] = ((CHARSEXP) readExp()).getValue();
        }
        return new StringVector(values, readAttributes(flags));
    }

    private SEXP readComplexExp(Flags flags) throws IOException {
        int length = in.readInt();
        Complex[] values = new Complex[length];
        for (int i = 0; i != length; ++i) {
            values[i] = new Complex(in.readDouble(), in.readDouble());
        }
        return new ComplexVector(values, readAttributes(flags));
    }

    private SEXP readDoubleExp(Flags flags) throws IOException {
        int length = in.readInt();
        double[] values = new double[length];
        for (int i = 0; i != length; ++i) {
            values[i] = in.readDouble();
        }
        return new DoubleVector(values, readAttributes(flags));
    }

    private SEXP readIntVector(Flags flags) throws IOException {
        int length = in.readInt();
        int[] values = new int[length];
        for (int i = 0; i != length; ++i) {
            values[i] = in.readInt();
        }
        return new IntVector(values, readAttributes(flags));
    }

    private SEXP readLogical(Flags flags) throws IOException {
        int length = in.readInt();
        int values[] = new int[length];
        for (int i = 0; i != length; ++i) {
            values[i] = in.readInt();
        }
        return new LogicalVector(values, readAttributes(flags));
    }

    private SEXP readCharExp(Flags flags) throws IOException {
        int length = in.readInt();
        if (length == -1) {
            return new CHARSEXP(StringVector.NA);
        } else {
            byte buf[] = in.readString(length);
            if (flags.isUTF8Encoded()) {
                return new CHARSEXP(new String(buf, "UTF8"));
            } else if (flags.isLatin1Encoded()) {
                return new CHARSEXP(new String(buf, "Latin1"));
            } else {
                return new CHARSEXP(new String(buf));
            }
        }
    }

    private SEXP readPrimitive(Flags flags) throws IOException {
        throw new IOException("readPrim ");
    }

    private SEXP readWeakReference(Flags flags) throws IOException {
        throw new IOException("weakRef not yet impl");
    }

    private SEXP readExternalPointer(Flags flags) throws IOException {
        throw new IOException("readExternalPointer not yet implemented");
    }

    private SEXP readPersistentExp() throws IOException {
        if (restorer == null) {
            throw new IOException("no restore method available");
        }
        return addReadRef(restorer.restore(readPersistentNamesVector()));
    }

    private StringVector readPersistentNamesVector() throws IOException {
        if (in.readInt() != 0) {
            throw new IOException("names in persistent strings are not supported yet");
        }
        int len = in.readInt();
        String values[] = new String[len];
        for (int i = 0; i != len; ++i) {
            values[i] = ((CHARSEXP) readExp()).getValue();
        }
        return new StringVector(values);
    }

    private interface StreamReader {

        int readInt() throws IOException;

        byte[] readString(int length) throws IOException;

        double readDouble() throws IOException;
    }

    private static class BinaryReader implements StreamReader {

        private final DataInput in;

        private BinaryReader(DataInputStream in) throws IOException {
            this.in = in;
        }

        private BinaryReader(InputStream in) throws IOException {
            this(new DataInputStream(in));
        }

        public int readInt() throws IOException {
            return in.readInt();
        }

        @Override
        public byte[] readString(int length) throws IOException {
            byte buf[] = new byte[length];
            in.readFully(buf);
            return buf;
        }

        @Override
        public double readDouble() throws IOException {
            return in.readDouble();
        }
    }

    private static class AsciiReader implements StreamReader {

        private BufferedReader reader;

        private AsciiReader(BufferedReader reader) {
            this.reader = reader;
        }

        private AsciiReader(InputStream in) {
            this(new BufferedReader(new InputStreamReader(in)));
        }

        public String readWord() throws IOException {
            int codePoint;
            do {
                codePoint = reader.read();
                if (codePoint == -1) {
                    throw new EOFException();
                }
            } while (Character.isWhitespace(codePoint));
            StringBuilder sb = new StringBuilder();
            while (!Character.isWhitespace(codePoint)) {
                sb.appendCodePoint(codePoint);
                codePoint = reader.read();
            }
            return sb.toString();
        }

        @Override
        public int readInt() throws IOException {
            String word = readWord();
            if ("NA".equals(word)) {
                return IntVector.NA;
            } else {
                return Integer.parseInt(word);
            }
        }

        @Override
        public double readDouble() throws IOException {
            String word = readWord();
            if ("NA".equals(word)) {
                return DoubleVector.NA;
            } else if ("Inf".equals(word)) {
                return Double.POSITIVE_INFINITY;
            } else if ("-Inf".equals(word)) {
                return Double.NEGATIVE_INFINITY;
            } else {
                return ParseUtil.parseDouble(word);
            }
        }

        @Override
        public byte[] readString(int length) throws IOException {
            throw new IOException("reading strings from ascii file not yet impl");
        }
    }

    private static class XdrReader implements StreamReader {

        private final DataInputStream in;

        private XdrReader(DataInputStream in) throws IOException {
            this.in = in;
        }

        public XdrReader(InputStream conn) throws IOException {
            this(new DataInputStream(conn));
        }

        @Override
        public int readInt() throws IOException {
            return in.readInt();
        }

        @Override
        public byte[] readString(int length) throws IOException {
            byte buf[] = new byte[length];
            in.readFully(buf);
            return buf;
        }

        @Override
        public double readDouble() throws IOException {
            long bits = in.readLong();
            if (bits == XDR_NA_BITS) {
                return DoubleVector.NA;
            } else {
                return Double.longBitsToDouble(bits);
            }
        }
    }

    public interface PersistentRestorer {

        SEXP restore(SEXP values);
    }
}
