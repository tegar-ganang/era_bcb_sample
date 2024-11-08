package net.sourceforge.filebot.ui.transfer;

import static net.sourceforge.filebot.Settings.*;
import static net.sourceforge.tuned.FileUtilities.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.sourceforge.tuned.TemporaryFolder;

public class ByteBufferTransferable implements Transferable {

    protected final Map<String, ByteBuffer> vfs;

    private FileTransferable transferable;

    public ByteBufferTransferable(Map<String, ByteBuffer> vfs) {
        this.vfs = vfs;
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
        if (FileTransferable.isFileListFlavor(flavor)) {
            try {
                if (transferable == null) {
                    transferable = createFileTransferable();
                }
                return transferable.getTransferData(flavor);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        throw new UnsupportedFlavorException(flavor);
    }

    protected FileTransferable createFileTransferable() throws IOException {
        List<File> files = new ArrayList<File>();
        for (Entry<String, ByteBuffer> entry : vfs.entrySet()) {
            String name = entry.getKey();
            ByteBuffer data = entry.getValue().duplicate();
            files.add(createTemporaryFile(name, data));
        }
        return new FileTransferable(files);
    }

    protected File createTemporaryFile(String name, ByteBuffer data) throws IOException {
        String validFileName = validateFileName(name);
        File temporaryFile = TemporaryFolder.getFolder(getApplicationName()).subFolder("dnd").createFile(validFileName);
        FileChannel fileChannel = new FileOutputStream(temporaryFile).getChannel();
        try {
            fileChannel.write(data);
        } finally {
            fileChannel.close();
        }
        return temporaryFile;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[] { DataFlavor.javaFileListFlavor, FileTransferable.uriListFlavor };
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return FileTransferable.isFileListFlavor(flavor);
    }
}
