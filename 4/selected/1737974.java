package de.inovox.pipeline.plugin;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;
import com.documentum.fc.client.DfClient;
import com.documentum.fc.client.DfQuery;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfTypedObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfLoginInfo;
import com.documentum.fc.common.IDfId;
import com.documentum.fc.common.IDfLoginInfo;

/**
 * <p>
 * InputStream reads a named Documentum Object.
 * </p>
 * @author: 
 */
public class DctmInputStream extends java.io.InputStream {

    private com.documentum.fc.client.IDfSession s;

    private java.lang.String DocName;

    private java.lang.String TempFileName;

    private java.io.InputStream hIn;

    /**
 * DctmInputStream constructor comment.
 */
    protected DctmInputStream() {
        super();
    }

    /**
 * DctmInputStream constructor comment.
 */
    public DctmInputStream(IDfSession s, String DocumentName) throws IOException {
        this(s, DocumentName, null);
    }

    /**
 * DctmInputStream constructor comment.
 */
    public DctmInputStream(IDfSession s, String DocumentName, String Format) throws IOException {
        super();
        try {
            this.s = s;
            DocName = new String(DocumentName);
            String IdStr = null;
            if (!DctmStreamUtils.isId(DocName)) {
                if (DocumentName.indexOf("/") == -1 || !DocumentName.startsWith("/")) {
                    throw new IllegalArgumentException(getClass().getName() + ".DctmInputStream( s, " + DocumentName + "): Document name must be absolute!");
                }
                String FolderName = DocumentName.substring(0, DocumentName.lastIndexOf("/"));
                String FileName = DocumentName.substring(DocumentName.lastIndexOf("/") + 1);
                DfQuery NewQuery = new DfQuery();
                NewQuery.setDQL("select r_object_id from dm_sysobject where object_name='" + FileName + "' and folder('" + FolderName + "')");
                IDfCollection Docs = NewQuery.execute(s, 0);
                Vector Ret = new Vector();
                while (Docs.next()) {
                    IDfTypedObject Next = Docs.getTypedObject();
                    IDfId ObjectId = Next.getId("r_object_id");
                    Ret.addElement(ObjectId.toString());
                    break;
                }
                IdStr = (String) Ret.elementAt(0);
            } else IdStr = DocName;
            getContent(IdStr, Format);
            hIn = new BufferedInputStream(new FileInputStream(TempFileName));
        } catch (Exception exc) {
            exc.printStackTrace(System.err);
            throw new IOException(getClass().getName() + ".DctmInputStream(" + DocumentName + ")");
        }
    }

    /**
 * <p>
 * Closes the InputStream!
 * </p>
 */
    public void close() throws IOException {
        synchronized (this) {
            if (hIn != null) {
                hIn.close();
                hIn = null;
            }
        }
    }

    /**
 * <p>
 * Closes the input stream (if still open) and deletes the
 * Temporary file!
 * </p>
 */
    public void finalize() {
        if (hIn != null) {
            try {
                hIn.close();
            } catch (IOException exc) {
            }
        }
        new File(TempFileName).delete();
    }

    /**
 * <p>
 * Gets the content from the server.
 * </p>
 */
    private void getContent(String ObjectId, String Format) throws DfException {
        StringBuffer Command = new StringBuffer();
        Command.append(ObjectId);
        if (Format != null) {
            Command.append(",");
            Command.append(Format);
        }
        TempFileName = s.apiGet("getfile", Command.toString());
    }

    /**
 * <p>
 * Test method
 * </p>
 * @param args java.lang.String[]
 */
    public static void main(String[] args) {
        try {
            IDfLoginInfo l = new DfLoginInfo();
            l.setUser(args[1]);
            l.setPassword(args[2]);
            IDfSession d = DfClient.getLocalClient().newSession(args[0], l);
            InputStream hIn = new DctmInputStream(d, "/Temp/TargetSetup.Result");
            ByteArrayOutputStream bTemp = new ByteArrayOutputStream();
            byte[] Bytes = new byte[4096];
            int nBytesRead = -1;
            while ((nBytesRead = hIn.read(Bytes)) != -1) bTemp.write(Bytes, 0, nBytesRead);
            System.out.println(new String(bTemp.toByteArray()));
            hIn.close();
        } catch (Exception exc) {
            exc.printStackTrace(System.err);
        }
    }

    /**
	 * Reads the next byte of data from the input stream. The value byte is
	 * returned as an <code>int</code> in the range <code>0</code> to
	 * <code>255</code>. If no byte is available because the end of the stream
	 * has been reached, the value <code>-1</code> is returned. This method
	 * blocks until input data is available, the end of the stream is detected,
	 * or an exception is thrown.
	 *
	 * <p> A subclass must provide an implementation of this method.
	 *
	 * @return     the next byte of data, or <code>-1</code> if the end of the
	 *             stream is reached.
	 * @exception  IOException  if an I/O error occurs.
	 */
    public int read() throws java.io.IOException {
        return hIn.read();
    }
}
