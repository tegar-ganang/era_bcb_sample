package com.flipdf2.render;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;

public class SunPDFRender extends Render {

    private PDFFile pdfFile;

    public void openFile(File file) throws RenderException {
        try {
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            FileChannel channel = raf.getChannel();
            ByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            this.pdfFile = new PDFFile(buf);
        } catch (IOException ioe) {
            throw new RenderException(ioe);
        }
    }

    public RenderedPage getPage(int pageNo) {
        PDFPage page = this.pdfFile.getPage(pageNo);
        return new PDFRenderedPage(page);
    }
}
