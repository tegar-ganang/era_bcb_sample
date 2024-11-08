package org.makagiga.plugins.pdfviewer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import com.sun.pdfview.OutlineNode;
import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;
import com.sun.pdfview.PagePanel;
import com.sun.pdfview.ThumbPanel;
import org.makagiga.commons.Config;
import org.makagiga.commons.MDisposable;
import org.makagiga.commons.MLogger;

/**
 * DOC: https://pdf-renderer.dev.java.net/examples.html
 */
public final class Core extends PagePanel implements Config.IntRange, MDisposable {

    public static final int FIRST_PAGE_INDEX = 1;

    private final Bookmarks bookmarks = new Bookmarks();

    private int currentPage = -1;

    private PDFFile file;

    private ThumbPanel thumbPanel;

    public static PDFFile getPDFFile(final File file) throws FileNotFoundException, IOException {
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        FileChannel channel = raf.getChannel();
        ByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        return new PDFFile(buf);
    }

    @Override
    public Config.IntInfo getIntInfo() {
        return new Config.IntInfo(Core.FIRST_PAGE_INDEX, getPageCount());
    }

    @Override
    public void dispose() {
        thumbPanel.stop();
    }

    Core() {
    }

    Bookmarks getBookmarks() {
        return bookmarks;
    }

    int getCurrentPage() {
        return currentPage;
    }

    PDFFile getFile() {
        return file;
    }

    OutlineNode getOutline() {
        try {
            return file.getOutline();
        } catch (IOException exception) {
            MLogger.exception(exception);
            return null;
        }
    }

    PDFPage getPageAt(final int number) {
        return file.getPage(number);
    }

    int getPageCount() {
        return file.getNumPages();
    }

    ThumbPanel getThumbPanel() {
        return thumbPanel;
    }

    void load(final File file) throws FileNotFoundException, IOException {
        this.file = getPDFFile(file);
        thumbPanel = new ThumbPanel(this.file);
    }

    void showPage(final int number) {
        MLogger.debug("pdf", "Show page: %s", number);
        if ((number < FIRST_PAGE_INDEX) || (number > getPageCount())) return;
        if (number == currentPage) return;
        currentPage = number;
        PDFPage page = getPageAt(number);
        if ((getWidth() <= 0) || (getHeight() <= 0)) setSize(getPreferredSize());
        thumbPanel.pageShown(number - 1);
        showPage(page);
    }
}
