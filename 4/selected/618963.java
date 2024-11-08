package org.zkoss.eclipse.setting.zklib.archive.browse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.zkoss.eclipse.setting.zklib.archive.IArchiveEntry;

/**
 * @author Ian Tsai
 *
 */
public class InnerZipStreamDataNode extends ZipEntryDataNode {

    protected String[] pathz;

    protected ZipInputStream zin;

    /**
	 * 
	 * @param path
	 * @param zin
	 * @param parent
	 * @throws IOException
	 */
    protected InnerZipStreamDataNode(String path, ZipInputStream zin, IArchiveEntry parent, NodeFactory fac) throws IOException {
        super(fac);
        pathz = path.split("/");
        this.zin = zin;
        List<IArchiveEntry> zeList = new LinkedList<IArchiveEntry>();
        this.setName(pathz[pathz.length - 1]);
        this.setParent(parent);
        AdjustHelper adjustor = new AdjustHelper(this);
        ZipEntry en = null;
        byte[] buff = new byte[1024 * 256];
        while ((en = zin.getNextEntry()) != null) {
            ByteArrayOutputStream dataBank = new ByteArrayOutputStream();
            int offset = 0;
            while ((offset = zin.read(buff)) != -1) dataBank.write(buff, 0, offset);
            final InputStream in = new ByteArrayInputStream(dataBank.toByteArray());
            dataBank.close();
            InnerZipEntryDataNode node = new InnerZipEntryDataNode(en.getName(), en.isDirectory() ? null : in, parent, fac);
            adjustor.add(node);
        }
        adjustor.adjustNodeLocation();
    }

    public void setParent(IArchiveEntry node) {
        super.setParent(node);
    }

    public InputStream getContent() throws IOException {
        return zin;
    }

    public boolean isLeaf() {
        return false;
    }

    public String[] getPathz() {
        return pathz;
    }
}
