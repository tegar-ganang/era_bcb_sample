package com.konarkdev.elibrary_manager.db;

import com.konarkdev.elibrary_manager.server.dataobjects.*;
import java.io.*;
import java.net.URL;
import java.util.Date;
import java.util.Properties;
import javax.jdo.PersistenceManager;
import javax.jdo.Transaction;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.*;

public class DataObjectsReaderWriter extends HttpServlet {

    public DataObjectsReaderWriter() {
    }

    public void doGet(HttpServletRequest httpservletrequest, HttpServletResponse httpservletresponse) throws ServletException, IOException {
        String s = httpservletrequest.getParameter("action");
        if (s == null) {
            RequestDispatcher requestdispatcher = httpservletrequest.getRequestDispatcher("/pages/install.jsp");
            requestdispatcher.forward(httpservletrequest, httpservletresponse);
            return;
        } else {
            return;
        }
    }

    public void doPost(HttpServletRequest httpservletrequest, HttpServletResponse httpservletresponse) throws ServletException, IOException {
        java.io.InputStream inputstream = DBConnector.class.getResourceAsStream("dbparams.properties");
        Properties properties = new Properties();
        properties.load(inputstream);
        String s = httpservletrequest.getParameter("url");
        System.out.println((new StringBuilder()).append("url :: ").append(s).toString());
        String s1 = httpservletrequest.getParameter("username");
        String s2 = httpservletrequest.getParameter("password");
        properties.put("javax.jdo.option.ConnectionURL", s);
        properties.put("javax.jdo.option.ConnectionUserName", s1);
        properties.put("javax.jdo.option.ConnectionPassword", s2);
        URL url = DBConnector.class.getResource("dbparams.properties");
        File file = new File(url.getPath());
        FileOutputStream fileoutputstream = new FileOutputStream(file);
        properties.store(fileoutputstream, (new Date()).toString());
        initDB();
        RequestDispatcher requestdispatcher = httpservletrequest.getRequestDispatcher("/pages/installed.jsp");
        requestdispatcher.forward(httpservletrequest, httpservletresponse);
    }

    public void initDB() {
        PersistenceManager persistencemanager;
        User user;
        UserRole userrole;
        Transaction transaction;
        DBConnector dbconnector = DBConnector.getInstance();
        persistencemanager = dbconnector.getPM();
        String s = "admin";
        String s1 = "admin@nobody.com";
        String s2 = "admin";
        String s3 = "admin";
        user = new User();
        user.setName(s);
        user.setEmail(s1);
        user.setPassword(s2);
        userrole = new UserRole();
        userrole.setName(s);
        userrole.setRole(s3);
        transaction = persistencemanager.currentTransaction();
        try {
            transaction.begin();
            persistencemanager.makePersistent(user);
            persistencemanager.makePersistent(userrole);
            String as[] = { "Fiction", "Computers", "Electronics", "Biology" };
            for (int i = 0; i < as.length; i++) {
                BookType booktype = new BookType();
                booktype.setType(as[i]);
                persistencemanager.makePersistent(booktype);
            }
            transaction.commit();
        } finally {
            if (transaction.isActive()) {
                transaction.rollback();
            }
        }
    }

    public static void main(String args[]) {
        DataObjectsReaderWriter dataobjectsreaderwriter = new DataObjectsReaderWriter();
    }
}
