package de.kopis.jusenet.nntp;

import gnu.inet.nntp.ArticleNumberIterator;
import gnu.inet.nntp.ArticleResponse;
import gnu.inet.nntp.GroupIterator;
import gnu.inet.nntp.LineIterator;
import gnu.inet.nntp.NNTPConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.EventListener;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Stack;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;
import de.kopis.jusenet.Application;
import de.kopis.jusenet.nntp.exceptions.NntpNotConnectedException;
import de.kopis.jusenet.nntp.listeners.ArticleListener;
import de.kopis.jusenet.nntp.listeners.GroupListener;
import de.kopis.jusenet.nntp.listeners.GroupManager;
import de.kopis.jusenet.nntp.listeners.ServerListener;
import de.kopis.jusenet.utils.HibernateUtils;
import de.kopis.utils.FileUtils;

/**
 * NNTPUtils is a utility class for handling a NNTP connection.<br>
 * <br>
 * This class should implement all actions in Events/Listeners. This is 
 * neccessary to use multi-threading and multiple connections.<br>
 * 
 * @author cringe
 * @version 0.1
 * @created 08.03.2007 11:26:38
 */
public class NNTPUtils implements EventListener, GroupManager {

    /**
     * Current instance of NNTPUtils to be used through all parts of 
     * application.
     */
    private static NNTPUtils instance;

    /**
     * Pool of available NNTPConnections
     */
    private Stack<NNTPConnection> availableConnections;

    /**
     * Pool of used NNTPConnections
     */
    private Stack<NNTPConnection> usedConnections;

    /**
     * EventListeners for Group, Server, Article
     */
    private EventListenerList listeners;

    public NNTPUtils() {
        listeners = new EventListenerList();
        availableConnections = new Stack<NNTPConnection>();
        usedConnections = new Stack<NNTPConnection>();
    }

    /**
     * Returns the available instance of NNTPUtils.
     * 
     * @return Instance of NNTPUtils to be used
     */
    public static NNTPUtils getInstance() {
        if (instance == null) {
            instance = new NNTPUtils();
        }
        return instance;
    }

    /**
     * Adds a <code>gnu.inet.nntp.NNTPConnection</code> to the pool.
     * 
     * @param connection
     */
    public void addNntpConnection(NNTPConnection connection) {
        availableConnections.push(connection);
    }

    public void addGroupListener(GroupListener listener) {
        listeners.add(GroupListener.class, listener);
    }

    public void removeGroupListener(GroupListener listener) {
        listeners.remove(GroupListener.class, listener);
    }

    public void addArticleListener(ArticleListener listener) {
        listeners.add(ArticleListener.class, listener);
    }

    public void removeArticleListener(ArticleListener listener) {
        listeners.remove(ArticleListener.class, listener);
    }

    public void addServerListener(ServerListener listener) {
        listeners.add(ServerListener.class, listener);
    }

    public void removeServerListener(ServerListener listener) {
        listeners.remove(ServerListener.class, listener);
    }

    /**
     * Fires an event when article HEAD is loaded from server.
     * 
     * @param a     Newly loaded article
     */
    private void fireArticleHeaderLoaded(Article a) {
    }

    /**
     * Fires an event when article BODY is loaded from server.
     * 
     * @param a     Newly loaded article with BODY
     */
    private void fireArticleBodyLoaded(Article a) {
    }

    /**
     * Fires an event when a complete article (HEAD + BODY) is loaded from server.
     * 
     * @param a     Newly loaded article with full data
     */
    private void fireArticleLoaded(Article a) {
        ArticleListener[] l = listeners.getListeners(ArticleListener.class);
        for (int i = 0; i < l.length; i++) {
            l[i].articleLoaded(a);
        }
    }

    /**
     * Fires an event when an article is sent to the server.
     * 
     */
    private void fireArticleSent() {
    }

    /**
     * Fires an event when connected to server.
     * 
     */
    private void fireConnected() {
        ServerListener[] l = listeners.getListeners(ServerListener.class);
        for (int i = 0; i < l.length; i++) {
            l[i].connected();
        }
    }

    /**
     * Fires an event when disconnected from server.
     *
     */
    private void fireDisconnected() {
        ServerListener[] l = listeners.getListeners(ServerListener.class);
        for (int i = 0; i < l.length; i++) {
            l[i].disconnected();
        }
    }

    /**
     * Fires an event when a group is loaded.<br>
     * <br>
     * Loads only the group, not the articles or other detailed data.
     * 
     * @param g
     */
    private void fireGroupLoaded(Group g) {
        GroupListener[] l = listeners.getListeners(GroupListener.class);
        for (int i = 0; i < l.length; i++) {
            l[i].groupLoaded(g);
        }
    }

    /**
     * Establishes one connection to the server.
     * @throws NntpNotConnectedException 
     *
     */
    public void connect() throws NntpNotConnectedException {
        connect(1);
    }

    /**
     * Connects to the server.
     * 
     * @param connections   Number of simultaneous connections
     * @throws NntpNotConnectedException 
     */
    private void connect(int numberOfConnections) throws NntpNotConnectedException {
        if (availableConnections.size() < 1) {
            try {
                NNTPConnection con = new NNTPConnection(Application.getInstance().getProperty("nntp.hostname", "localhost"), Integer.parseInt(Application.getInstance().getProperty("nntp.port", "119")));
                String username = Application.getInstance().getProperty("nntp.username");
                if (!"".equals(username)) {
                    if (con.authinfo(username, Application.getInstance().getProperty("nntp.password"))) {
                        System.out.println(new Date() + ": connected");
                        availableConnections.push(con);
                    } else {
                        System.err.println(new Date() + ": not connected. check username/password");
                        throw new NntpNotConnectedException();
                    }
                }
            } catch (NumberFormatException e) {
                throw new NntpNotConnectedException();
            } catch (UnknownHostException e) {
                throw new NntpNotConnectedException();
            } catch (IOException e) {
                throw new NntpNotConnectedException();
            }
        }
        for (int i = 0; i < numberOfConnections; i++) {
            if (i >= availableConnections.size()) break;
            NNTPConnection con = availableConnections.pop();
            fireConnected();
            usedConnections.push(con);
        }
    }

    /**
     * Disconnects all opened connections from the server.
     * @throws IOException 
     * 
     */
    public void disconnect() throws IOException {
        Iterator it = usedConnections.iterator();
        while (it.hasNext()) {
            NNTPConnection con = (NNTPConnection) it.next();
            con.quit();
            availableConnections.push(con);
        }
        fireDisconnected();
    }

    /**
     * Returns the number of available connections.
     * 
     * @return
     */
    public int getAvailableConnectionCount() {
        return availableConnections.size();
    }

    /**
     * Returns the number of used connections.
     * 
     * @return
     */
    public int getOpenedConnectionCount() {
        return usedConnections.size();
    }

    /**
     * Returns number of all available and used connections.
     * 
     * @return
     */
    public int getConnectionCount() {
        return availableConnections.size() + usedConnections.size();
    }

    /**
     * Fetches a complete list of groups from server.
     * 
     * @throws NntpNotConnectedException 
     * 
     */
    public void getGroups() throws NntpNotConnectedException {
        if (usedConnections.size() == 0) {
            throw new NntpNotConnectedException();
        }
        Thread t = new Thread() {

            NNTPConnection con = usedConnections.get(0);

            public void run() {
                GroupIterator git;
                try {
                    git = con.list();
                    while (git.hasNext()) {
                        gnu.inet.nntp.Group g = (gnu.inet.nntp.Group) git.next();
                        Group group = new Group();
                        group.setName(g.getName());
                        group.setFirst(g.getFirst());
                        group.setLast(g.getLast());
                        group.setLastUpdate(new Date());
                        group.setSubscribed(false);
                        HibernateUtils.addGroup(group);
                        fireGroupLoaded(group);
                        Thread.sleep(5);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        t.start();
    }

    /**
     * Gets all articles from a group.
     * 
     * @param g     Group to update
     * @throws NntpNotConnectedException 
     */
    public void getArticles(final Group g) throws NntpNotConnectedException {
        if (usedConnections.size() == 0) {
            throw new NntpNotConnectedException();
        }
        Thread t = new Thread() {

            private NNTPConnection con = usedConnections.get(0);

            private Stack<Integer> articlesIds = new Stack<Integer>();

            public void run() {
                ArticleNumberIterator ait;
                try {
                    ait = con.listGroup(g.getName());
                    while (ait.hasNext()) {
                        Integer id = (Integer) ait.next();
                        System.out.println("Got article ID '" + id + "'");
                        articlesIds.push(id);
                        Thread.sleep(2);
                    }
                    for (Integer id : articlesIds) {
                        ArticleResponse a = (ArticleResponse) con.article(id);
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        BufferedReader br = new BufferedReader(new InputStreamReader(a.in));
                        String line = null;
                        while ((line = br.readLine()) != null) {
                            pw.println(line);
                        }
                        Article article = extract(sw.toString());
                        HibernateUtils.addArticle(g, article);
                        fireArticleLoaded(article);
                        Thread.sleep(5);
                    }
                    g.setLastUpdate(getCurrentDate());
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        t.start();
    }

    /**
     * Returns a <code>Date</code> with the current time.
     * 
     * @return
     */
    private Date getCurrentDate() {
        return GregorianCalendar.getInstance().getTime();
    }

    /**
     * Gets all articles from a group.
     * 
     * @param g     Group to update
     * @throws NntpNotConnectedException 
     */
    public void getArticleHeaders(final Group g) throws NntpNotConnectedException {
        if (usedConnections.size() == 0) {
            throw new NntpNotConnectedException();
        }
        Thread t = new Thread() {

            NNTPConnection con = usedConnections.get(0);

            public void run() {
                ArticleNumberIterator ait;
                try {
                    ait = con.listGroup(g.getName());
                    while (ait.hasNext()) {
                        int aid = (Integer) ait.next();
                        ArticleResponse a = con.head(aid);
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        BufferedReader br = new BufferedReader(new InputStreamReader(a.in));
                        String line = null;
                        while ((line = br.readLine()) != null) {
                            pw.println(line);
                        }
                        Article article = extract(sw.toString());
                        HibernateUtils.addArticle(g, article);
                        fireArticleLoaded(article);
                        Thread.sleep(5);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        t.start();
    }

    /**
     * Gets all articles from a group.
     * 
     * @param g     Group to update
     * @throws NntpNotConnectedException 
     */
    public void getArticleBodies(final Group g) throws NntpNotConnectedException {
        if (usedConnections.size() == 0) {
            throw new NntpNotConnectedException();
        }
        Thread t = new Thread() {

            private NNTPConnection con = usedConnections.get(0);

            public void run() {
                ArticleNumberIterator ait;
                try {
                    ait = con.listGroup(g.getName());
                    while (ait.hasNext()) {
                        int aid = (Integer) ait.next();
                        ArticleResponse a = con.body(aid);
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        BufferedReader br = new BufferedReader(new InputStreamReader(a.in));
                        String line = null;
                        while ((line = br.readLine()) != null) {
                            pw.println(line);
                        }
                        Article article = extract(sw.toString());
                        fireArticleLoaded(article);
                        Thread.sleep(5);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        t.start();
    }

    /**
     * Gets an article via message ID.
     * 
     * @param messageId     Unique message ID to get
     * @throws NntpNotConnectedException 
     */
    public void getArticle(final String messageId) throws NntpNotConnectedException {
        if (usedConnections.size() == 0) {
            throw new NntpNotConnectedException();
        }
        Thread t = new Thread() {

            private NNTPConnection con = usedConnections.get(0);

            public void run() {
                try {
                    ArticleResponse ait = con.article(messageId);
                    Article article = new Article();
                    fireArticleLoaded(article);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        t.start();
    }

    /**
     * Gets an article.
     * 
     * @param articleId     Unique Article ID to get
     * @throws NntpNotConnectedException 
     */
    private void getArticle(final int articleId) throws NntpNotConnectedException {
        if (usedConnections.size() == 0) {
            throw new NntpNotConnectedException();
        }
        Thread t = new Thread() {

            private NNTPConnection con = usedConnections.get(0);

            public void run() {
                try {
                    ArticleResponse ait = con.article(articleId);
                    Article article = new Article();
                    fireArticleLoaded(article);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        t.start();
    }

    public void send(final String content) throws NntpNotConnectedException {
        if (usedConnections.size() == 0) {
            throw new NntpNotConnectedException();
        }
        Thread t = new Thread() {

            private NNTPConnection con = usedConnections.get(0);

            public void run() {
                try {
                    PrintWriter pw = new PrintWriter(con.post());
                    pw.write(content);
                    fireArticleSent();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        t.start();
    }

    public void read(Group group) {
        HibernateUtils.markRead(group, true);
    }

    public void unread(Group group) {
        HibernateUtils.markRead(group, false);
    }

    public void subscribe(Group group) {
        HibernateUtils.subscribe(group, true);
    }

    public void unsubscribe(Group group) {
        HibernateUtils.subscribe(group, false);
    }

    public void update(final Group group) throws NntpNotConnectedException {
        if (usedConnections.size() == 0) {
            throw new NntpNotConnectedException();
        }
        Thread t = new Thread() {

            private NNTPConnection con = usedConnections.get(0);

            public void run() {
                LineIterator ait;
                try {
                    ait = con.newNews(group.getName(), group.getLastUpdate(), null);
                    System.out.println("NEW NEWS: ");
                    while (ait.hasNext()) {
                        System.out.println(ait.nextLine());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        t.start();
        group.setLastUpdate(new Date());
    }

    public void newGroups(final Date date) throws NntpNotConnectedException {
        if (usedConnections.size() == 0) {
            throw new NntpNotConnectedException();
        }
        Thread t = new Thread() {

            private NNTPConnection con = usedConnections.get(0);

            public void run() {
                LineIterator lit;
                try {
                    lit = con.newGroups(date, null);
                    System.out.println("NEW NEWS: ");
                    while (lit.hasNext()) {
                        System.out.println(lit.nextLine());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        t.start();
    }

    public void getArticles(String groupname) {
        Group group;
        group = HibernateUtils.getGroup(groupname);
        try {
            getArticles(group);
        } catch (NntpNotConnectedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Extract informations from plain text article.<br/>
     * <br/>
     * Splits on \n\n for Header/Body separation and uses the start words
     * for header recognition. This is hardcoded from RFC and must not be 
     * changed.<br/>
     * <br/>
     * TODO make sure extract() works even for HEAD/BODY only
     * 
     * @param content
     * @return
     */
    private static Article extract(String content) {
        Article a = new Article();
        content = content.replaceAll("\\r", "");
        int pos = content.indexOf("\n\n");
        String body;
        pos = content.indexOf("\n\n");
        if (pos != -1) {
            String[] headers = content.substring(0, pos).split("\\n");
            a.setHeaders(headers);
            body = content.substring(pos);
        } else {
            body = content;
        }
        pos = body.indexOf("-- ");
        if (pos != -1) {
            String tmp = body.substring(pos + 3).trim();
            a.setSignature(tmp);
            tmp = body.substring(0, pos);
            body = tmp;
        }
        a.setText(body.trim());
        return a;
    }

    public void getArticles(final Group g, final Date lastUpdate) throws NntpNotConnectedException {
        if (usedConnections.size() == 0) {
            throw new NntpNotConnectedException();
        }
        NNTPConnection con = usedConnections.get(0);
        Stack<Integer> articlesIds = new Stack<Integer>();
        try {
            ArticleNumberIterator ait = con.listGroup(g.getName());
            while (ait.hasNext()) {
                Integer id = (Integer) ait.next();
                System.out.println("Got article ID '" + id + "'");
                articlesIds.push(id);
            }
            for (Integer id : articlesIds) {
                ArticleResponse a = (ArticleResponse) con.article(id);
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                BufferedReader br = new BufferedReader(new InputStreamReader(a.in));
                String line = null;
                while ((line = br.readLine()) != null) {
                    pw.println(line);
                }
                final Article article = extract(sw.toString());
                HibernateUtils.addArticle(g, article);
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        fireArticleLoaded(article);
                    }
                });
                Thread.sleep(5);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            Article a = extract(FileUtils.getFileContents(args[0]));
            System.out.println("This is what I got:");
            System.out.println("Subject:\n'" + a.getSubject() + "'");
            System.out.println("Author:\n'" + a.getAuthor() + "'");
            System.out.println("Sent:\n'" + a.getSent() + "'");
            System.out.println("Message-ID:\n'" + a.getMessageId() + "'");
            System.out.println("TEXT:\n" + a.getText());
            System.out.println("SIGNATURE:\n" + a.getSignature());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
