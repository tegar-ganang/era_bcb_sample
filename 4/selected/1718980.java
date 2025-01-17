package objot.bytecode;

import java.io.PrintStream;
import objot.util.Array2;
import objot.util.Class2;
import objot.util.InvalidValueException;

public final class AnnoParams extends Element {

    public final Constants cons;

    public final boolean hided;

    int paramN;

    /** [parameter index] */
    int[] annoNs;

    /** [parameter index][annotation index] */
    Annotation[][] annos;

    public AnnoParams(Constants c, byte[] bs, int beginBi_, boolean hided_) {
        super(bs, beginBi_);
        cons = c;
        hided = hided_;
        paramN = readU1(bytes, beginBi + 6);
        annoNs = Array2.newInts(paramN);
        int bi = beginBi + 7;
        for (int i = 0; i < paramN; i++) {
            annoNs[i] = readU2(bytes, bi);
            bi += 2;
            for (int n = annoNs[i]; n > 0; n--) bi += Annotation.readByteN(bytes, bi);
        }
        if (bi - beginBi - 6 != readU4(bytes, beginBi + 2)) throw new ClassFormatError("inconsistent attribute length");
        end1Bi = bi;
    }

    public int getParamN() {
        return paramN;
    }

    void checkIndex(int pi) {
        if (pi < 0 || pi >= paramN) throw new InvalidValueException(pi);
    }

    public int getAnnoN(int pi) {
        checkIndex(pi);
        return annoNs[pi];
    }

    void checkIndex(int pi, int ai) {
        checkIndex(pi);
        if (ai < 0 || ai >= annoNs[pi]) throw new InvalidValueException(pi);
    }

    void readAnnos() {
        if (annos != null) return;
        annos = new Annotation[paramN][];
        int bi = beginBi + 7;
        for (int i = 0; i < paramN; i++) {
            annos[i] = new Annotation[allocN(annoNs[i])];
            bi += 2;
            for (int j = 0; j < annoNs[i]; j++) {
                annos[i][j] = new Annotation(cons, bytes, bi);
                bi = annos[i][j].end1Bi;
            }
        }
    }

    public Annotation getAnno(int pi, int ai) {
        checkIndex(pi, ai);
        readAnnos();
        return annos[pi][ai];
    }

    /**
	 * @return the index(<code>param << 32L | anno & 0xFFFFFFFFL</code>) of annotation
	 *         found, negative for not found.
	 */
    public static long searchAnno(AnnoParams as, Class<? extends java.lang.annotation.Annotation> anno) {
        if (as != null) for (int g = as.paramN - 1; g >= 0; g--) for (int a = as.getAnnoN(g) - 1; a >= 0; a--) if (as.cons.equalsUtf(as.getAnno(g, a).getDescCi(), utf(Class2.descript(anno)))) return g << 32L | a & 0xFFFFFFFFL;
        return -1;
    }

    /**
	 * @return the index(<code>param << 32L | anno & 0xFFFFFFFFL</code>) of annotated
	 *         annotation found, negative for not found.
	 */
    public static long searchAnnoAnno(ClassLoader cl, AnnoParams as, Class<? extends java.lang.annotation.Annotation> anno) throws ClassNotFoundException {
        if (as != null) for (int g = as.paramN - 1; g >= 0; g--) for (int a = as.getAnnoN(g) - 1; a >= 0; a--) {
            int desc = as.getAnno(g, a).getDescCi();
            Class<?> ca = cl.loadClass(as.cons.classDesc2NameUcs(desc).replace('/', '.'));
            if (ca.isAnnotationPresent(anno)) return g << 32L | a & 0xFFFFFFFFL;
        }
        return -1;
    }

    @Override
    void printContents(PrintStream out, int indent1st, int indent, int verbose) {
        if (verbose > 0) {
            printIndent(out, indent1st);
            out.print(" paramN ");
            out.print(paramN);
        }
        out.println();
        for (int i = 0; i < paramN; i++) for (int j = 0; j < getAnnoN(i); j++) {
            printIndent(out, indent);
            out.print(i);
            out.print(',');
            out.print(j);
            out.print('.');
            getAnno(i, j).printTo(out, 0, indent, verbose);
        }
    }

    public void ensureAnnoN(int pi, int an) {
        checkIndex(pi);
        readAnnos();
        annos[pi] = Array2.ensureN(annos[pi], an);
    }

    public int addAnno(int pi, Annotation a) {
        if (cons != a.cons) throw new IllegalArgumentException("inconsistent constants");
        checkIndex(pi);
        readAnnos();
        ensureAnnoN(pi, annoNs[pi] + 1);
        annos[pi][annoNs[pi]] = a;
        return annoNs[pi]++;
    }

    public void setAnno(int pi, int ai, Annotation a) {
        if (cons != a.cons) throw new IllegalArgumentException("inconsistent constants");
        checkIndex(pi, ai);
        readAnnos();
        annos[pi][ai] = a;
    }

    @Override
    public int normalizeByteN() {
        if (annos == null) return byteN0();
        int n = 7;
        for (int i = 0; i < paramN; i++) {
            n += 2;
            for (int j = 0; j < annoNs[i]; j++) n += annos[i][j].normalizeByteN();
        }
        return n;
    }

    @Override
    public int normalizeTo(byte[] bs, int begin) {
        if (annos == null) {
            System.arraycopy(bytes, beginBi, bs, begin, byteN0());
            return begin + byteN0();
        }
        writeU2(bs, begin, readU2(bytes, beginBi));
        writeU1(bs, begin + 6, paramN);
        int bi = begin + 7;
        for (int gi = 0; gi < paramN; gi++) {
            bi = writeU2(bs, bi, annoNs[gi]);
            for (int ai = 0; ai < annoNs[gi]; ai++) bi = annos[gi][ai].normalizeTo(bs, bi);
        }
        writeS4(bs, begin + 2, bi - begin - 6);
        return bi;
    }
}
