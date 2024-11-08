import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ResourceBundle;
import javax.imageio.ImageIO;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A Servlet to help view jpg images
 *
 * @author James Chy
 */
public class SearchPrintResults extends HttpServlet {

    String initpath;

    String title, imgStrPath, thbStrPath;

    int i;

    ResourceBundle rb;

    double maxSize;

    boolean showQuery;

    String sessionid;

    boolean FindNew;

    PrintWriter out;

    int columns;

    int rows;

    String connection;

    String imgthmb;

    ResultSet rs;

    static final long serialVersionUID = 0;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        rb = ResourceBundle.getBundle("LocalStrings");
        imgStrPath = rb.getString("servletAlbum.start");
        thbStrPath = rb.getString("servletAlbum.printdir");
        maxSize = Double.parseDouble(rb.getString("servletAlbum.printsize"));
        showQuery = rb.getString("servletAlbum.showQuery").equals("yes");
        connection = rb.getString("servletAlbum.connection");
        imgthmb = rb.getString("servletAlbum.printurl");
        columns = Integer.parseInt(rb.getString("servletAlbum.printwide"));
        rows = Integer.parseInt(rb.getString("servletAlbum.printrows"));
    }

    public void detroy() {
        String query;
        query = "DELETE FROM session_results;";
        rs = executeQuery(query);
        query = "DELETE FROM session;";
        rs = executeQuery(query);
    }

    public void createSession() {
        sessionid = "0";
        String query;
        query = "insert into session set created=now();";
        if (showQuery) out.println("Create Session: " + query + "<BR>");
        rs = executeQuery(query);
        query = "select max(session.uid) as sessionid from session;";
        if (showQuery) out.println("Get Session ID: " + query + "<BR>");
        rs = executeQuery(query);
        if (showQuery) out.println("Completed execute query <BR>");
        try {
            rs.next();
            sessionid = rs.getString("sessionid");
        } catch (SQLException sqle) {
            out.println("SQL Exception Error " + sqle.toString());
        }
    }

    public int getCount(String query) {
        rs = executeQuery(query);
        int count = 0;
        try {
            rs.next();
            count = rs.getInt("COUNT(*)");
        } catch (SQLException sqle) {
            out.println("SQL Exception Error " + sqle.toString());
        }
        return count;
    }

    public ResultSet executeQuery(String query) {
        rs = null;
        try {
            Class.forName("org.gjt.mm.mysql.Driver").newInstance();
            java.sql.Connection conn;
            conn = DriverManager.getConnection(connection);
            Statement stmt;
            stmt = conn.createStatement();
            rs = stmt.executeQuery(query);
        } catch (ClassNotFoundException cnfe) {
            out.println("Class Not Found Exception " + cnfe.toString());
        } catch (InstantiationException ie) {
            out.println("Instantiation Exception " + ie.toString());
        } catch (IllegalAccessException iae) {
            out.println("Illegal Access Exception " + iae.toString());
        } catch (SQLException sqle) {
            out.println("SQL Exception " + sqle.toString());
        }
        return rs;
    }

    public void appendToSession(String image) {
        String query = "SELECT COUNT(*) FROM images WHERE images.image_file=\"" + image + "\";";
        if (getCount(query) == 0) {
            query = "INSERT INTO images SET images.image_file=\"" + image + "\";";
            if (showQuery) out.println("AppendToSession: " + query + "<BR>");
            rs = executeQuery(query);
        }
        query = "SELECT uid FROM images WHERE images.image_file=\"" + image + "\";";
        String image_uid = "";
        if (showQuery) out.println("AppendToSession: " + query + "<BR>");
        rs = executeQuery(query);
        try {
            rs.next();
            image_uid = rs.getString("uid");
        } catch (SQLException sqle) {
            out.println("SQL Exception Error " + sqle.toString());
        }
        String setSession = "INSERT INTO session_results " + "SET session_uid = \"" + sessionid + "\", " + "date=\"\", image_uid=" + image_uid + ", image_file=\"" + image + "\" ";
        if (showQuery) out.println("AppendToSession: " + setSession + "<BR>");
        rs = executeQuery(setSession);
    }

    public void createThumbnail(String image) {
        String imgStr = imgStrPath + "\\" + image;
        String thbStr = thbStrPath + "\\" + image;
        File imgFile = new File(imgStr);
        File thbFile = new File(thbStr);
        try {
            double scale;
            int thumbWidth, thumbHeight;
            BufferedImage bufi = ImageIO.read(imgFile);
            BufferedImage thumbnailImage = null;
            if (bufi.getWidth() > bufi.getHeight()) {
                scale = maxSize / bufi.getWidth();
                thumbWidth = (int) maxSize;
                thumbHeight = (int) (maxSize * bufi.getHeight() / bufi.getWidth());
            } else {
                scale = maxSize / bufi.getHeight();
                thumbWidth = (int) (maxSize * bufi.getWidth() / bufi.getHeight());
                thumbHeight = (int) maxSize;
            }
            AffineTransform xform = AffineTransform.getScaleInstance(scale, scale);
            AffineTransformOp op = new AffineTransformOp(xform, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
            thumbnailImage = new BufferedImage(thumbWidth, thumbHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = thumbnailImage.createGraphics();
            g.drawImage(bufi, op, 0, 0);
            ImageIO.write(thumbnailImage, "jpeg", thbFile);
        } catch (IOException ioe) {
            out.println("I/O Exception");
        }
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        doPost(request, response);
    }

    public void bubblesort(String filenames[]) {
        for (int i = filenames.length - 1; i > 0; i--) {
            for (int j = 0; j < i; j++) {
                String temp;
                if (filenames[j].compareTo(filenames[j + 1]) > 0) {
                    temp = filenames[j];
                    filenames[j] = filenames[j + 1];
                    filenames[j + 1] = temp;
                }
            }
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.setContentType("text/html");
        out = response.getWriter();
        String query;
        out.println("<html>");
        out.println("<body bgcolor=\"white\">");
        out.println("<head>");
        title = "Search Results";
        sessionid = request.getParameter("sessionid");
        FindNew = (request.getParameter("FindNew") != null && request.getParameter("FindNew").equals("on"));
        int page;
        if (request.getParameter("Page") != null) page = Integer.parseInt(request.getParameter("Page")); else page = 1;
        int start;
        int end;
        start = (page - 1) * rows * columns + 1;
        end = page * rows * columns;
        out.println("<title>" + title + "</title>");
        out.println("</head>");
        out.println("<body>");
        if (showQuery) out.println(connection);
        if (FindNew) {
            createSession();
            if (showQuery) out.println("imgStrPath: " + imgStrPath + "<BR>");
            File main = new File(imgStrPath);
            if (showQuery) out.println("Directory: " + main.isDirectory() + " Exists: " + main.exists() + "<BR>");
            if (main.isDirectory() && main.exists()) {
                String[] contents = main.list();
                bubblesort(contents);
                if (showQuery) out.println("contents.length = " + contents.length + "<BR>");
                query = "SELECT images.image_file, dates.date " + "FROM images, image_date, dates " + "WHERE image_date.image_uid = images.uid " + "AND dates.uid = image_date.date_uid " + "ORDER BY images.image_file";
                if (showQuery) out.println("Query: " + query + "<BR>");
                rs = executeQuery(query);
                int i = 0;
                boolean k;
                String image_file;
                int l;
                try {
                    rs.first();
                    k = rs.isAfterLast();
                    while (i < contents.length && !k) {
                        String nextpath = imgStrPath + "\\" + contents[i];
                        File aFile = new File(nextpath);
                        image_file = rs.getString("image_file");
                        if (showQuery) {
                            out.println("contents[" + i + "]=" + contents[i] + " image_file=" + image_file + "<BR>");
                        }
                        if ((contents[i].endsWith(".jpg") || contents[i].endsWith(".JPG")) && !aFile.isDirectory()) {
                            l = image_file.compareToIgnoreCase(contents[i]);
                            if (l > 0) {
                                appendToSession(contents[i]);
                                i++;
                            } else if (l == 0) {
                                i++;
                                rs.next();
                            } else {
                                rs.next();
                            }
                        } else {
                            i++;
                        }
                        k = rs.isAfterLast();
                    }
                } catch (SQLException sqle) {
                    out.println("SQL Exception error " + sqle.toString());
                }
            }
        } else if (sessionid.equals("0")) {
            String dateValues[] = request.getParameterValues("dates");
            String peopleValues[] = request.getParameterValues("people");
            String thingValues[] = request.getParameterValues("things");
            String placeValues[] = request.getParameterValues("places");
            boolean first = true;
            createSession();
            query = "SELECT DISTINCT " + sessionid + " as sessionid, " + "dates.date, images.uid, images.image_file " + "FROM ";
            if ((peopleValues.length != 1) || (!peopleValues[0].equals("0"))) query = query + "image_person, ";
            if ((thingValues.length != 1) || (!thingValues[0].equals("0"))) query = query + "image_thing, ";
            if ((placeValues.length != 1) || (!placeValues[0].equals("0"))) query = query + "image_place, ";
            query = query + "images, image_date, dates ";
            if ((dateValues.length != 1) || (!dateValues[0].equals("0"))) {
                if (first) {
                    query = query + "WHERE ";
                    first = false;
                } else query = query + "AND ";
                for (i = 0; i < dateValues.length; i++) {
                    if (i == 0) query = query + "( "; else query = query + "OR ";
                    query = query + "image_date.date_uid = " + dateValues[i] + " ";
                    if (i == (dateValues.length - 1)) query = query + ") ";
                }
            }
            if ((peopleValues.length != 1) || (!peopleValues[0].equals("0"))) {
                if (first) {
                    query = query + "WHERE ";
                    first = false;
                } else query = query + "AND ";
                for (i = 0; i < peopleValues.length; i++) {
                    if (i == 0) query = query + "( "; else query = query + "OR ";
                    query = query + "image_person.person_uid = " + peopleValues[i] + " ";
                    if (i == (peopleValues.length - 1)) query = query + ") ";
                }
            }
            if ((thingValues.length != 1) || (!thingValues[0].equals("0"))) {
                if (first) {
                    query = query + "WHERE ";
                    first = false;
                } else query = query + "AND ";
                for (i = 0; i < thingValues.length; i++) {
                    if (i == 0) query = query + "( "; else query = query + "OR ";
                    query = query + "image_thing.thing_uid = " + thingValues[i] + " ";
                    if (i == (thingValues.length - 1)) query = query + ") ";
                }
            }
            if ((placeValues.length != 1) || (!placeValues[0].equals("0"))) {
                if (first) {
                    query = query + "WHERE ";
                    first = false;
                } else query = query + "AND ";
                for (i = 0; i < placeValues.length; i++) {
                    if (i == 0) query = query + "( "; else query = query + "OR ";
                    query = query + "image_place.place_uid = " + placeValues[i] + " ";
                    if (i == (placeValues.length - 1)) query = query + ") ";
                }
            }
            if ((peopleValues.length != 1) || (!peopleValues[0].equals("0"))) query = query + "AND images.uid = image_person.image_uid ";
            if ((thingValues.length != 1) || (!thingValues[0].equals("0"))) query = query + "AND images.uid = image_thing.image_uid ";
            if ((placeValues.length != 1) || (!placeValues[0].equals("0"))) query = query + "AND images.uid = image_place.image_uid ";
            query = query + "AND images.uid = image_date.image_uid ";
            query = query + "AND dates.uid = image_date.date_uid ";
            query = query + "ORDER BY (dates.date), (images.image_file);";
            String setSession = "INSERT INTO session_results (session_uid, date, image_uid, image_file) " + query;
            if (showQuery) out.println("SetSession: " + setSession + "<BR>");
            rs = executeQuery(setSession);
            if (showQuery) out.println("Query: " + query + "<BR>");
        }
        if (showQuery) out.println("Session ID = " + sessionid);
        String getSession = "SELECT image_file, date " + "FROM session_results " + "WHERE session_uid = " + sessionid + " " + "ORDER BY (date) DESC, (image_file) DESC;";
        if (showQuery) out.println("Get Session: " + getSession + "<BR>");
        rs = executeQuery(getSession);
        try {
            out.println("<TABLE>");
            i = 1;
            while (rs.next()) {
                if (i >= start && i <= end) {
                    String image_file = rs.getString("image_file");
                    if (i % columns == 1) out.println("<TR>");
                    out.println("<TD>");
                    out.println("<A HREF=\"OneImage?image=" + image_file + "&sessionid=" + sessionid + "&Page=" + String.valueOf(page) + "\">");
                    out.print("<IMAGE SRC=\"" + imgthmb + "/" + image_file + "\" ");
                    out.print("VSPACE=\"0\" HSPACE=\"0\" BORDER=\"0\"");
                    out.println(">");
                    out.println("</A>");
                    out.println("</TD>");
                    if (i % columns == 0) out.println("</TR>");
                }
                i++;
            }
            out.println("</TABLE><BR>");
            out.println("<TABLE BORDER=0><TR>");
            out.println("<TD>");
            out.println("<FORM ACTION=SearchPrint METHOD=GET>");
            out.println("<INPUT TYPE=SUBMIT VALUE=\"New Search\">");
            out.println("</FORM>");
            out.println("</TD>");
            if (page > 1) {
                out.println("<TD>");
                out.println("<FORM ACTION=SearchPrintResults METHOD=POST>");
                out.println("<INPUT TYPE=HIDDEN NAME=Page VALUE=" + String.valueOf(page - 1) + ">");
                out.println("<INPUT TYPE=HIDDEN NAME=sessionid VALUE=" + sessionid + ">");
                out.println("<INPUT TYPE=SUBMIT NAME=Prev Value=Prev>");
                out.println("</FORM>");
                out.println("</TD>");
            }
            if (end < (i - 1)) {
                out.println("<TD>");
                out.println("<FORM ACTION=SearchPrintResults METHOD=POST>");
                out.println("<INPUT TYPE=HIDDEN NAME=Page VALUE=" + String.valueOf(page + 1) + ">");
                out.println("<INPUT TYPE=HIDDEN NAME=sessionid VALUE=" + sessionid + ">");
                out.println("<INPUT TYPE=SUBMIT NAME=Next Value=Next>");
                out.println("</FORM>");
                out.println("</TD>");
            }
        } catch (SQLException sqle) {
            out.println("SQL Exception error " + sqle.toString());
        }
        out.println("<TD>");
        out.println("<FORM ACTION=AddItems METHOD=GET>");
        out.println("<INPUT TYPE=HIDDEN NAME=sessionid VALUE=\"" + sessionid + "\">");
        out.println("<INPUT TYPE=SUBMIT VALUE=\"Add Items\">");
        out.println("</FORM>");
        out.println("</TD>");
        out.println("</TR></TABLE>");
        out.println("</body>");
        out.println("</html>");
    }
}
