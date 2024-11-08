package abstrasy.externals;

import abstrasy.Bivalence;
import abstrasy.Buffer;
import abstrasy.Node;
import abstrasy.interpreter.Base64;
import abstrasy.interpreter.ExternalTK;
import abstrasy.interpreter.InterpreterException;
import abstrasy.interpreter.StdErrors;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class External_RandomAccessFile extends External implements ExternalObject {

    private boolean inUse = false;

    private File file = null;

    private String mode = null;

    private RandomAccessFile raf = null;

    public External_RandomAccessFile() {
    }

    public Node external_open(Node startAt) throws Exception {
        startAt.isGoodArgsCnt(3);
        if (inUse) {
            throw new InterpreterException(StdErrors.extend(StdErrors.Already_used, "File already open"));
        }
        file = new File(startAt.getSubNode(1, Node.TYPE_STRING).getString());
        mode = startAt.getSubNode(2, Node.TYPE_STRING).getString().toLowerCase().trim();
        if (mode.equals("r") || mode.equals("rw") || mode.equals("rws") || mode.equals("rwd")) {
            raf = new RandomAccessFile(file, mode);
            inUse = true;
        } else {
            throw new InterpreterException(StdErrors.extend(StdErrors.Invalid_parameter, "Mode not supported"));
        }
        return null;
    }

    public Node external_close(Node startAt) throws Exception {
        startAt.isGoodArgsCnt(1);
        if (raf != null) {
            raf.close();
            raf = null;
        }
        mode = null;
        file = null;
        if (inUse) {
            inUse = false;
        }
        return null;
    }

    public Node external_length(Node startAt) throws Exception {
        startAt.isGoodArgsCnt(1);
        return new Node(raf.length());
    }

    public Node external_set_length(Node startAt) throws Exception {
        startAt.isGoodArgsCnt(2);
        long offset = (long) startAt.getSubNode(1, Node.TYPE_NUMBER).getNumber();
        if (offset < 0) {
            throw new InterpreterException(StdErrors.extend(StdErrors.Out_of_range, "length:" + offset));
        }
        raf.setLength(offset);
        return null;
    }

    public Node external_get_offset(Node startAt) throws Exception {
        startAt.isGoodArgsCnt(1);
        return new Node(raf.getFilePointer());
    }

    public Node external_set_offset(Node startAt) throws Exception {
        startAt.isGoodArgsCnt(2);
        long offset = (long) startAt.getSubNode(1, Node.TYPE_NUMBER).getNumber();
        if (offset < 0) {
            throw new InterpreterException(StdErrors.extend(StdErrors.Out_of_range, "offset:" + offset));
        }
        raf.seek(offset);
        return null;
    }

    public Node external_write_buffer(Node startAt) throws Exception {
        startAt.isGoodArgsCnt(2);
        Buffer buffer = ((External_Buffer) External.getArgExternalInstance(startAt, 1, External_Buffer.class)).getBuffer();
        if (buffer.length() > 0) {
            byte[] buf = buffer.read_bytes();
            raf.write(buf);
        }
        return null;
    }

    public Node external_read_buffer(Node startAt) throws Exception {
        startAt.isGoodArgsCnt(1, 2);
        Buffer buffer = new Buffer();
        if (startAt.size() == 2) {
            int sz = (int) startAt.getSubNode(1, Node.TYPE_NUMBER).getNumber();
            byte[] buf = new byte[sz];
            int cread = raf.read(buf);
            if (cread > 0) {
                buffer.write_bytes(buf, cread);
            }
        } else {
            byte[] buf = new byte[4096];
            int cread;
            while ((cread = raf.read(buf)) != -1) {
                if (cread > 0) {
                    buffer.write_bytes(buf, cread);
                }
            }
        }
        External_Buffer res = new External_Buffer();
        res.setBuffer(buffer);
        return ExternalTK.createVObject_External(null, External_Buffer.class.getName(), res);
    }

    public Node external_read_chunked_buffer(Node startAt) throws Exception {
        startAt.isGoodArgsCnt(1);
        Buffer buffer = new Buffer();
        int sz = raf.readInt();
        byte[] buf = new byte[sz];
        int cread = raf.read(buf);
        if (cread > 0) {
            buffer.write_bytes(buf, cread);
        }
        External_Buffer res = new External_Buffer();
        res.setBuffer(buffer);
        return ExternalTK.createVObject_External(null, External_Buffer.class.getName(), res);
    }

    public Node external_write_chunked_buffer(Node startAt) throws Exception {
        startAt.isGoodArgsCnt(2);
        Buffer buffer = ((External_Buffer) External.getArgExternalInstance(startAt, 1, External_Buffer.class)).getBuffer();
        int len = buffer.length();
        raf.writeInt(len);
        if (buffer.length() > 0) {
            byte[] buf = buffer.read_bytes();
            raf.write(buf);
        }
        return null;
    }

    public Object clone_my_self(Bivalence bival) {
        return null;
    }
}
