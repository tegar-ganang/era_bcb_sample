package xbrowser.bookmark.io;

import java.io.*;
import java.util.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.text.html.parser.*;
import xbrowser.XProjectConstants;
import xbrowser.bookmark.*;

public class XBookmarkNetscapeSerializer implements XBookmarkSerializer {

    public XBookmarkNetscapeSerializer() {
    }

    public void importFrom(String file_name, XBookmarkFolder root_folder) throws Exception {
        FileReader file_reader = new FileReader(file_name);
        parentFolder = root_folder;
        parser.parse(file_reader, new XCallback(), true);
        try {
            file_reader.close();
        } catch (Exception e) {
        }
    }

    private Date buildDate(String str) {
        Date date = null;
        try {
            date = new Date(Long.parseLong(str + "000"));
        } catch (Exception e) {
            date = new Date();
        }
        return date;
    }

    private class XCallback extends HTMLEditorKit.ParserCallback {

        public void flush() {
        }

        public void handleComment(char[] data, int pos) {
        }

        public void handleEndOfLineString(String eol) {
        }

        public void handleEndTag(HTML.Tag t, int pos) {
            if (t == HTML.Tag.DL) parentFolder = parentFolder.getParent();
        }

        public void handleError(String errorMsg, int pos) {
        }

        public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {
        }

        public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
            if (t == HTML.Tag.A) {
                String href = (String) a.getAttribute(HTML.Attribute.HREF);
                href = href.replace('|', ':');
                currentBookmark = new XBookmark(href);
                currentBookmark.setCreationDate(buildDate((String) a.getAttribute("add_date")));
                currentBookmark.setModificationDate(buildDate((String) a.getAttribute("last_modified")));
                parentFolder.addBookmark(currentBookmark);
            } else if (t == HTML.Tag.H3) {
                currentBookmark = new XBookmarkFolder();
                currentBookmark.setCreationDate(buildDate((String) a.getAttribute("add_date")));
                currentBookmark.setModificationDate(buildDate((String) a.getAttribute("add_date")));
                parentFolder.addBookmark(currentBookmark);
                parentFolder = (XBookmarkFolder) currentBookmark;
            }
        }

        public void handleText(char[] data, int pos) {
            if (currentBookmark != null) currentBookmark.setTitle(new String(data));
        }
    }

    public void exportTo(String file_name, XBookmarkFolder root_folder) throws Exception {
        FileWriter file_writer = new FileWriter(file_name);
        String title = "Bookmarks generated by " + XProjectConstants.PRODUCT_NAME;
        file_writer.write("<!DOCTYPE NETSCAPE-Bookmark-file-1>\n");
        file_writer.write("<!-- This is an automatically generated file.\n");
        file_writer.write("It will be read and overwritten.\n");
        file_writer.write("Do Not Edit! -->\n");
        file_writer.write("<TITLE>" + title + "</TITLE>\n");
        file_writer.write("<H1>" + title + "</H1>\n\n\n");
        file_writer.write("<DL><p>\n\n");
        Iterator bookmarks = root_folder.getBookmarks();
        while (bookmarks.hasNext()) saveBookmarksImpl((XAbstractBookmark) bookmarks.next(), file_writer, "\t");
        file_writer.write("</DL><p>\n\n");
        try {
            file_writer.close();
        } catch (Exception e) {
        }
    }

    private String prepareDateForSaving(Date date) {
        String str = "" + date.getTime();
        str = str.substring(0, str.length() - 3);
        return str;
    }

    private void saveBookmarksImpl(XAbstractBookmark abs_bm, FileWriter file_writer, String header) {
        try {
            if (abs_bm instanceof XBookmark) saveBookmark((XBookmark) abs_bm, file_writer, header); else if (abs_bm instanceof XBookmarkFolder) saveBookmarkFolder((XBookmarkFolder) abs_bm, file_writer, header);
        } catch (Exception e) {
        }
    }

    private void saveBookmark(XBookmark bookmark, FileWriter file_writer, String header) throws Exception {
        file_writer.write(header + "<DT><A HREF=\"" + bookmark.getHRef() + "\"");
        file_writer.write(" ADD_DATE=\"" + prepareDateForSaving(bookmark.getCreationDate()) + "\"");
        file_writer.write(" LAST_VISIT=\"" + prepareDateForSaving(bookmark.getModificationDate()) + "\"");
        file_writer.write(" LAST_MODIFIED=\"" + prepareDateForSaving(bookmark.getModificationDate()) + "\">");
        file_writer.write(bookmark.getTitle() + "</A>\n\n");
    }

    private void saveBookmarkFolder(XBookmarkFolder bm_folder, FileWriter file_writer, String header) throws Exception {
        file_writer.write(header + "<DT><H3 ADD_DATE=\"" + prepareDateForSaving(bm_folder.getCreationDate()));
        file_writer.write("\">" + bm_folder.getTitle() + "</H3>\n\n");
        if (bm_folder.getBookmarkCount() == 0) file_writer.write(header + "<DD>\n\n"); else {
            file_writer.write(header + "<DL><p>\n\n");
            Iterator bookmarks = bm_folder.getBookmarks();
            while (bookmarks.hasNext()) saveBookmarksImpl((XAbstractBookmark) bookmarks.next(), file_writer, header + "\t");
            file_writer.write(header + "</DL><p>\n\n");
        }
    }

    private ParserDelegator parser = new ParserDelegator();

    private XBookmarkFolder parentFolder = null;

    private XAbstractBookmark currentBookmark = null;
}
