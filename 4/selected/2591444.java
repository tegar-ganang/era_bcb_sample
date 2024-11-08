package org.pannotas;

import static org.junit.Assert.*;
import java.sql.SQLException;
import java.util.regex.Pattern;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestRepositorySqlite implements RepositoryListener, ParagraphListener {

    private static RepositorySqlite rep;

    private boolean page_changed, page_deleted, page_added;

    private boolean phrase_inserted, phrase_deleted, phrase_changed, paragraph_moved;

    private String page_callback, text_callback;

    private void resetCallback() {
        page_changed = page_deleted = page_added = false;
        phrase_inserted = phrase_deleted = phrase_changed = paragraph_moved = false;
        page_callback = text_callback = null;
    }

    @BeforeClass
    public static void setUpBeforeClass() {
        rep = new RepositorySqlite();
        rep.open("test_unit.db");
        rep.clearRepository();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        rep.close();
        rep = null;
    }

    @Test
    public void testAppendPhrase() {
        rep.clearRepository();
        rep.appendPhrase("test append", "append 1");
        assertEquals(rep.readPage("test append"), "append 1");
        rep.appendPhrase("test append", "; append 2");
        assertEquals(rep.readPage("test append"), "append 1; append 2");
    }

    @Test
    public void testAddPageListener() {
        rep.clearRepository();
        rep.addPageListener("test21", this);
        rep.writePage("test21", "hello world");
        resetCallback();
        rep.insertPhrase("test21", 0, 0, "no ");
        assertTrue(phrase_inserted);
        assertEquals(page_callback, "test21");
        assertEquals(text_callback, "no ");
        resetCallback();
        rep.deletePhrase("test21", 0, 3, 3);
        assertTrue(phrase_deleted);
        assertEquals(page_callback, "test21");
        resetCallback();
        rep.changePhrase("test21", 0, 0, 3, "yes ");
        assertTrue(phrase_changed);
        assertEquals(page_callback, "test21");
        assertEquals(text_callback, "yes ");
        rep.removePageListener("test21", this);
    }

    @Test
    public void testAddParagrahListener() {
        rep.clearRepository();
        rep.addParagrahListener("test22", 1, this);
        resetCallback();
        rep.writePage("test22", "what\n\nis\ngoing\non?");
        fail("not implemented");
    }

    @Test
    public void testAddRepositoryListener() {
        rep.clearRepository();
        resetCallback();
        rep.addRepositoryListener(this);
        rep.writePage("test1", "some text");
        assertTrue(page_added);
        assertEquals(page_callback, "test1");
        resetCallback();
        rep.deletePage("test2");
        assertTrue(page_deleted);
        assertEquals(page_callback, "test2");
        resetCallback();
        rep.insertPhrase("test1", 0, 0, "more text ");
        assertTrue(page_changed);
        assertEquals(page_callback, "test1");
        rep.removeRepositoryListener(this);
        rep.writePage("test3", "dah dum");
        assertFalse(page_added);
        assertFalse(page_deleted);
        assertFalse(page_changed);
    }

    @Test
    public void testGetAllPageTitles() {
        String[] p;
        rep.clearRepository();
        p = rep.getAllPageTitles();
        assertTrue(p.length == 0);
        rep.writePage("test1", "text blah");
        p = rep.getAllPageTitles();
        assertTrue(p.length == 1);
        assertEquals(p[0], "test1");
        rep.writePage("test2", "text blah");
        rep.writePage("test3", "text blah");
        p = rep.getAllPageTitles();
        assertTrue(p.length == 3);
        rep.clearRepository();
    }

    @Test
    public void testGetPageInfo() {
        rep.writePage("test11", "1234567");
        PageInfo info = rep.getPageInfo("test11");
        assertEquals(info.text, "1234567");
        assertEquals(info.size, 7);
    }

    @Test
    public void testInsertPhrase() {
        rep.writePage("test10", "Hello World!\nWhat is happening?");
        rep.insertPhrase("test10", 1, 5, "the hell ");
        assertEquals(rep.readPage("test10"), "Hello World!\nWhat the hell is happening?");
        rep.writePage("test11", "");
        rep.insertPhrase("test11", 0, 0, "what");
        assertEquals(rep.readPage("test11"), "what");
        rep.writePage("test12", "\n\n\n");
        rep.insertPhrase("test12", 0, 0, "1");
        assertEquals(rep.readPage("test12"), "1\n\n\n");
        rep.insertPhrase("test12", 1, 0, "2");
        assertEquals(rep.readPage("test12"), "1\n2\n\n");
    }

    @Test
    public void testChangePhrase() {
        rep.writePage("test11", "Hello World!\nWhat is happening?");
        rep.changePhrase("test11", 1, 0, 7, "Not sure");
        assertEquals(rep.readPage("test11"), "Hello World!\nNot sure happening?");
    }

    @Test
    public void testDeletePhrase() {
        rep.writePage("test2", "This is some text.");
        rep.deletePhrase("test2", 0, 4, 8);
        assertEquals(rep.readPage("test2"), "This text.");
    }

    @Test
    public void testCopyPhrase() {
        rep.writePage("test6", "hello world today!");
        rep.writePage("test7", "1\n2\n3");
        rep.copyPhrase("test6", 0, 6, 5, "test7", 1, 1);
        assertEquals(rep.readPage("test7"), "1\n2world\n3");
    }

    @Test
    public void testDeletePage() {
        rep.writePage("test delete", "some text");
        assertNotNull(rep.readPage("test delete"));
        rep.deletePage("test delete");
        assertNull(rep.readPage("test delete"));
    }

    @Test
    public void testWriteReadPage() {
        rep.writePage("test write read", "what are you doing?");
        assertEquals(rep.readPage("test write read"), "what are you doing?");
    }

    @Test
    public void testOpenAndClose() {
        RepositorySqlite r = new RepositorySqlite();
        r.open("test_open.db");
        r.close();
    }

    @Test
    public void testFindAllInPage() {
        rep.writePage("s", "first line\nsecond line\ntodo:buy milk\ntodo:buy bananas");
        Pattern pat = Pattern.compile("line");
        PhraseLocation loc[] = rep.findAll(pat, "s");
        assertEquals(loc.length, 2);
        assertEquals(loc[0], new PhraseLocation("s", 0, 6, 4));
        assertEquals(loc[1], new PhraseLocation("s", 1, 7, 4));
        pat = Pattern.compile("first");
        loc = rep.findAll(pat, "s");
        assertEquals(loc.length, 1);
        assertEquals(loc[0], new PhraseLocation("s", 0, 0, 5));
        pat = Pattern.compile("^todo:.*$", Pattern.MULTILINE);
        loc = rep.findAll(pat, "s");
        assertEquals(loc.length, 2);
        assertEquals(loc[0], new PhraseLocation("s", 2, 0, 13));
    }

    @Test
    public void testSqlExists() {
        assertFalse(rep.sqlPageExists("some completely random non-existent page.."));
        java.util.Date d = new java.util.Date();
        rep.sqlSetPage("test1", "This is a test text. The brown fox jumps !!!", d.getTime());
        assertTrue(rep.sqlPageExists("test1"));
    }

    @Test
    public void testSqlSetGet() {
        java.util.Date d = new java.util.Date();
        rep.sqlSetPage("test1", "blah text bloom", d.getTime());
        assertEquals(rep.sqlGetPage("test1"), "blah text bloom");
    }

    @Test
    public void testGetParagraphStart() {
        assertEquals(rep.getParagraphStart("hello", 0), 0);
        assertEquals(rep.getParagraphStart("hello", 1), -1);
        assertEquals(rep.getParagraphStart("hello\n\nYou\n", 1), 6);
        assertEquals(rep.getParagraphStart("hello\n\nYou\n", 2), 7);
        assertEquals(rep.getParagraphStart("\n\n\n", 1), 1);
        assertEquals(rep.getParagraphStart("\n\n\n", 2), 2);
    }

    @Test
    public void testGetParagraphCount() {
        assertEquals(rep.getParagraphCount("\n\n", 0), 0);
        assertEquals(rep.getParagraphCount("     ", 10), 0);
        assertEquals(rep.getParagraphCount("\n\nAA\n", 1), 1);
        assertEquals(rep.getParagraphCount("\n\nAA\n", 2), 2);
        assertEquals(rep.getParagraphCount("\n\nAA\n", 3), 2);
    }

    @Override
    public void pageAdded(String page) {
        page_added = true;
        page_callback = page;
    }

    @Override
    public void pageChanged(String page) {
        page_changed = true;
        page_callback = page;
    }

    @Override
    public void pageDeleted(String page) {
        page_deleted = true;
        page_callback = page;
    }

    @Override
    public void phraseChanged(String page, int paragraph, int start, int end, String text) {
        phrase_changed = true;
        page_callback = page;
        text_callback = text;
    }

    @Override
    public void phraseDeleted(String page, int paragraph, int start, int end) {
        phrase_deleted = true;
        page_callback = page;
    }

    @Override
    public void phraseInserted(String page, int paragraph, int start, String text) {
        phrase_inserted = true;
        page_callback = page;
        text_callback = text;
    }

    @Override
    public void paragraphMoved(String page, int paragrah, int newParagrah) {
        paragraph_moved = true;
        page_callback = page;
    }
}
