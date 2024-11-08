package net.narusas.cafelibrary.serial;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Hashtable;
import java.util.Vector;
import java.util.logging.Logger;
import net.narusas.cafelibrary.Book;
import net.narusas.cafelibrary.BookList;
import net.narusas.cafelibrary.Library;
import org.apache.commons.net.ftp.FTPClient;
import HTML.Template;

public class HTMLPublisher {

    static SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd");

    static Logger logger = Logger.getLogger("log");

    public static String toHTML(Library lib) throws FileNotFoundException, IllegalStateException, IOException {
        Template t = new Template("web/index.tmpl");
        Vector<Hashtable<String, Object>> books = new Vector<Hashtable<String, Object>>();
        for (int i = 0; i < lib.getBookSize(); i++) {
            Book book = lib.get(i);
            Hashtable<String, Object> param = new Hashtable<String, Object>();
            put(param, "id", String.valueOf(book.getId()));
            put(param, "title", book.getTitle());
            put(param, "author", book.getAuthor());
            put(param, "favorite", String.valueOf(book.getFavorite()));
            put(param, "cover", book.getCoverLargeUrl());
            put(param, "publisher", book.getPublisher());
            put(param, "publishDate", book.getPublishDate() == null ? "" : formatter.format(book.getPublishDate()));
            put(param, "category", book.getCategory());
            put(param, "translator", book.getTranslator());
            put(param, "purchaseDate", book.getPurchaseDate() == null ? "" : formatter.format(book.getPurchaseDate()));
            put(param, "borrower", book.getBorrower() == null ? "" : book.getBorrower().getName());
            put(param, "description", book.getDescription().replace('"', '\''));
            put(param, "notes", book.getNotes().replace('"', '\''));
            put(param, "isbn", book.getIsbn());
            books.add(param);
        }
        Vector<Hashtable<String, Object>> booklists = new Vector<Hashtable<String, Object>>();
        addBookList(booklists, lib);
        for (int i = 0; i < lib.sizeOfBookLists(); i++) {
            BookList bList = lib.getBookList(i);
            addBookList(booklists, bList);
        }
        Vector<Hashtable<String, Object>> borrowers = new Vector<Hashtable<String, Object>>();
        for (int i = 0; i < lib.sizeOfBorrowers(); i++) {
            BookList bList = lib.getBorrower(i);
            addBookList(borrowers, bList);
        }
        t.setParam("books", books);
        t.setParam("booklists", booklists);
        t.setParam("borrowers", borrowers);
        return t.output();
    }

    private static void addBookList(Vector<Hashtable<String, Object>> booklists, BookList bList) {
        Hashtable<String, Object> param = new Hashtable<String, Object>();
        put(param, "id", String.valueOf(bList.getId()));
        put(param, "name", bList.getName());
        Vector<Hashtable<String, Object>> ownbooks = new Vector<Hashtable<String, Object>>();
        for (int k = 0; k < bList.getBookSize(); k++) {
            Book ownbook = bList.get(k);
            Hashtable<String, Object> ownBookParam = new Hashtable<String, Object>();
            put(ownBookParam, "bookid", String.valueOf(ownbook.getId()));
            ownbooks.add(ownBookParam);
        }
        put(param, "ownbooks", ownbooks);
        booklists.add(param);
    }

    private static void put(Hashtable<String, Object> param, String key, Object value) {
        param.put(key, value == null ? "" : value);
    }

    public static void publish(String server, String id, String passwd, String path, String html) throws SocketException, IOException, LoginFailException {
        logger.info("Connect to FTP Server " + server);
        FTPClient f = new FTPClient();
        f.connect(server);
        if (f.login(id, passwd) == false) {
            logger.info("Fail to login with id=" + id);
            throw new LoginFailException(id, passwd);
        }
        f.changeWorkingDirectory(path);
        logger.info("Start to upload");
        f.storeFile("index.html", new ByteArrayInputStream(html.getBytes("utf-8")));
        logger.info("Upload index.html");
        f.storeFile("main.css", new FileInputStream("web/main.css"));
        logger.info("Upload main.css");
        f.storeFile("cafelibrary.js", new FileInputStream("web/cafelibrary.js"));
        logger.info("Upload cafelibrary.js");
        f.makeDirectory("img");
        f.changeWorkingDirectory("img");
        for (int i = 0; i <= 5; i++) {
            String fileName = "favorite_star_" + i + ".png";
            f.storeFile(fileName, new FileInputStream("web/img/" + fileName));
            logger.info("Upload " + fileName);
        }
        f.logout();
        f.disconnect();
    }
}
