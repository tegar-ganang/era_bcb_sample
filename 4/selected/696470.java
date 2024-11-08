package org.basex.build;

import static org.basex.data.DataText.*;
import java.io.IOException;
import org.basex.core.Main;
import org.basex.core.Prop;
import org.basex.core.proc.DropDB;
import org.basex.data.Data;
import org.basex.data.DiskData;
import org.basex.data.MetaData;
import org.basex.io.DataInput;
import org.basex.io.DataOutput;
import org.basex.io.IO;
import org.basex.io.TableAccess;
import org.basex.io.TableDiskAccess;
import org.basex.io.TableOutput;
import org.basex.util.Token;

/**
 * This class creates a disk based database instance. The storage layout is
 * described in the {@link Data} class.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-10, ISC License
 * @author Christian Gruen
 */
public final class DiskBuilder extends Builder {

    /** Database table. */
    private DataOutput tout;

    /** Database texts. */
    private DataOutput xout;

    /** Database values. */
    private DataOutput vout;

    /** Text pointer. */
    private long txtlen;

    /** Attribute value pointer. */
    private long vallen;

    /** Database size stream (temporary). */
    private DataOutput sout;

    /**
   * Constructor.
   * @param p parser
   */
    public DiskBuilder(final Parser p) {
        super(p);
    }

    @Override
    public DiskData build(final String db) throws IOException {
        final Prop pr = parser.prop;
        DropDB.drop(db, pr);
        pr.dbpath(db).mkdirs();
        meta = new MetaData(db, pr);
        meta.file = parser.io;
        meta.filesize = meta.file.length();
        meta.time = meta.file.date();
        int bs = IO.BLOCKSIZE;
        while (bs < meta.filesize && bs < 1 << 22) bs <<= 1;
        tout = new DataOutput(new TableOutput(meta, DATATBL));
        xout = new DataOutput(meta.file(DATATXT), bs);
        vout = new DataOutput(meta.file(DATAATV), bs);
        sout = new DataOutput(meta.file(DATATMP), bs);
        parse(db);
        close();
        final TableAccess ta = new TableDiskAccess(meta, DATATBL);
        final DataInput in = new DataInput(meta.file(DATATMP));
        for (; spos < ssize; spos++) ta.write4(in.readNum(), 8, in.readNum());
        ta.close();
        in.close();
        meta.file(DATATMP).delete();
        return new DiskData(meta, tags, atts, path, ns);
    }

    @Override
    public void abort() {
        try {
            close();
        } catch (final IOException ex) {
            Main.debug(ex);
        }
        DropDB.drop(meta.name, meta.prop);
    }

    @Override
    public void close() throws IOException {
        if (tout == null) return;
        tout.close();
        tout = null;
        xout.close();
        xout = null;
        vout.close();
        vout = null;
        sout.close();
        sout = null;
    }

    @Override
    protected void addDoc(final byte[] txt) throws IOException {
        tout.write(Data.DOC);
        tout.write2(0);
        tout.write5(inline(txt, true));
        tout.writeInt(0);
        tout.writeInt(meta.size++);
    }

    @Override
    protected void addElem(final int dis, final int n, final int as, final int u, final boolean ne) throws IOException {
        tout.write(as << 3 | Data.ELEM);
        tout.write2((ne ? 1 << 15 : 0) | n);
        tout.write(u);
        tout.writeInt(dis);
        tout.writeInt(as);
        tout.writeInt(meta.size++);
    }

    @Override
    protected void addAttr(final int n, final byte[] v, final int dis, final int u) throws IOException {
        tout.write(dis << 3 | Data.ATTR);
        tout.write2(n);
        tout.write5(inline(v, false));
        tout.writeInt(u);
        tout.writeInt(meta.size++);
    }

    @Override
    protected void addText(final byte[] txt, final int dis, final byte kind) throws IOException {
        tout.write(kind);
        tout.write2(0);
        tout.write5(inline(txt, true));
        tout.writeInt(dis);
        tout.writeInt(meta.size++);
    }

    @Override
    protected void setSize(final int pre, final int val) throws IOException {
        sout.writeNum(pre);
        sout.writeNum(val);
        ssize++;
    }

    /**
   * Calculates the value to be inlined or returns a text position.
   * @param val value to be inlined
   * @param txt text/attribute flag
   * @return inline value or text position
   * @throws IOException I/O exception
   */
    private long inline(final byte[] val, final boolean txt) throws IOException {
        long v = Token.toSimpleInt(val);
        if (v != Integer.MIN_VALUE) {
            v |= IO.NUMOFF;
        } else if (txt) {
            v = txtlen;
            txtlen += xout.writeBytes(val);
        } else {
            v = vallen;
            vallen += vout.writeBytes(val);
        }
        return v;
    }
}
