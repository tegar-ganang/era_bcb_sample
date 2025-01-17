package fr.itris.glips.svgeditor.io.managers.export.handler;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import javax.swing.*;
import org.w3c.dom.*;
import com.keypoint.*;
import fr.itris.glips.svgeditor.*;
import fr.itris.glips.svgeditor.io.managers.export.*;
import fr.itris.glips.svgeditor.io.managers.export.handler.dialog.*;
import fr.itris.glips.svgeditor.io.managers.export.monitor.*;

/**
 * the class used to export images in a jpg format
 * @author ITRIS, Jordi SUC
 */
public class PNGExport extends Export {

    /**
     * the compression level
     */
    private int compressionLevel = 9;

    /**
     * whether to encode the alpha channel or not
     */
    private boolean encodeAlpha = true;

    /**
	 * the constructor of the class
	 * @param fileExport the object manager the export
	 */
    protected PNGExport(FileExport fileExport) {
        super(fileExport);
        if (Editor.getParent() instanceof Frame) {
            exportDialog = new PNGExportDialog((Frame) Editor.getParent());
        } else {
            exportDialog = new PNGExportDialog((JDialog) Editor.getParent());
        }
    }

    @Override
    public void export(JComponent relativeComponent, Document document, File destFile) {
        monitor = new ExportMonitor(Editor.getParent(), 0, 100, FileExport.prefixLabels[1]);
        monitor.setRelativeComponent(relativeComponent);
        PNGExportDialog pngExportDialog = (PNGExportDialog) exportDialog;
        int res = exportDialog.showExportDialog(document);
        if (res == ExportDialog.OK_ACTION) {
            width = pngExportDialog.getExportSize().getX();
            height = pngExportDialog.getExportSize().getY();
            compressionLevel = pngExportDialog.getCompressionLevel();
            encodeAlpha = pngExportDialog.encodeAlpha();
            createImage(document, destFile, encodeAlpha);
        }
    }

    @Override
    protected void writeImage(final BufferedImage image, final File destFile) {
        Thread thread = new Thread() {

            @Override
            public void run() {
                try {
                    PngEncoder encoder = new PngEncoder(image, encodeAlpha, PngEncoder.FILTER_NONE, compressionLevel);
                    monitor.setProgress(55);
                    byte[] encodedBytes = encoder.pngEncode();
                    monitor.setProgress(90);
                    if (monitor.isCancelled()) {
                        monitor.stop();
                        return;
                    }
                    FileChannel outChannel = new FileOutputStream(destFile).getChannel();
                    ByteBuffer buffer = ByteBuffer.allocate(encodedBytes.length);
                    buffer.put(encodedBytes);
                    buffer.clear();
                    outChannel.write(buffer);
                    outChannel.close();
                    monitor.stop();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    handleExportFailure();
                }
            }
        };
        thread.start();
    }
}
