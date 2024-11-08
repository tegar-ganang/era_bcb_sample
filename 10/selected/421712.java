package whiteboard;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;
import org.apache.struts2.util.ServletContextAware;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.sql.DataSource;
import org.apache.commons.lang.StringEscapeUtils;
import whiteboard.course.Course;

/**
 * The action for editing or adding a document, like a note or homework.
 * Deletion is handled in another class called RememberDocumentAction.
 * 
 * @author John
 * 
 */
public class DocumentAction extends ActionSupport implements ServletContextAware {

    private ServletContext servletContext;

    private boolean sqlError;

    private HomePage homePage;

    private boolean foundDupDocument;

    private Course selectedCourse;

    public String execute() {
        homePage = getHomePage();
        selectedCourse = homePage.getSelectedCourse();
        sqlError = false;
        foundDupDocument = false;
        String action = selectedCourse.getAction();
        String document = selectedCourse.getWhatDocument();
        DataSource dbcp = (DataSource) servletContext.getAttribute("dbpool");
        String typeOfAction = findTypeOfAction(action);
        String typeOfDoc = findTypeOfDoc(action);
        if (typeOfAction.compareTo("add") == 0) {
            addDocToDB(action, dbcp);
            addDocToInstance(action);
            System.out.println("Added " + typeOfDoc + " to db and instance!...");
        } else if (typeOfAction.compareTo("edit") == 0) {
            if (typeOfDoc.compareTo("homework") == 0) {
                selectedCourse.removeHomework(document);
            } else if (typeOfDoc.compareTo("announcements") == 0) {
                selectedCourse.removeAnnouncement(document);
            } else if (typeOfDoc.compareTo("notes") == 0) {
                selectedCourse.removeNote(document);
            }
            selectedCourse.removeDocFromDB(typeOfDoc, document, dbcp);
            addDocToDB(action, dbcp);
            addDocToInstance(action);
            System.out.println("Edited " + typeOfDoc + " to db and instance!...");
        } else if (typeOfAction.compareTo("delete") == 0) {
        }
        if (!homePage.isAdmin() || !isValidInput()) {
            validate();
            return INPUT;
        }
        if (!sqlError) {
        } else {
            validate();
            return INPUT;
        }
        return SUCCESS;
    }

    /**
	 * We're adding a document to the homepage instance. The String action
	 * contains what kind of doc we're adding.
	 * 
	 * @param action
	 */
    private void addDocToInstance(String action) {
        String typeOfDoc = findTypeOfDoc(action).trim().toLowerCase();
        if (typeOfDoc.compareTo("homework") == 0) {
            Homework hmk = new Homework();
            hmk.setCourseid(selectedCourse.getCourseId());
            hmk.setAdmin(selectedCourse.getAdmin());
            hmk.setTimestamp(this.getTimeStamp());
            hmk.setLink(getLink());
            hmk.setLogin(homePage.getUser());
            hmk.setText(getText());
            hmk.setTitle(getTitle());
            selectedCourse.addHomework(hmk);
        } else if (typeOfDoc.compareTo("announcements") == 0) {
            Announcement a = new Announcement();
            a.setCourseid(selectedCourse.getCourseId());
            a.setAdmin(selectedCourse.getAdmin());
            a.setTimestamp(this.getTimeStamp());
            a.setLink(getLink());
            a.setLogin(homePage.getUser());
            a.setText(getText());
            a.setTitle(getTitle());
            selectedCourse.addAnnouncement(a);
        } else if (typeOfDoc.compareTo("notes") == 0) {
            Note n = new Note();
            n.setCourseid(selectedCourse.getCourseId());
            n.setAdmin(selectedCourse.getAdmin());
            n.setTimestamp(this.getTimeStamp());
            n.setLink(getLink());
            n.setLogin(homePage.getUser());
            n.setText(getText());
            n.setTitle(getTitle());
            selectedCourse.addNote(n);
        }
    }

    /**
	 * Add a document to the database. The String action will let us know what
	 * type of document we're adding, hmk, notes, announcements. It uses the
	 * DataSource database passed in.
	 * 
	 * If there's an exception, roll back changes.
	 * 
	 * @source Template for try catch.
	 *         http://stackoverflow.com/questions/3160756
	 *         /in-jdbc-when-autocommit
	 *         -is-false-and-no-explicit-savepoints-have-been-set-is-it
	 * @param action
	 * @param database
	 */
    private void addDocToDB(String action, DataSource database) {
        String typeOfDoc = findTypeOfDoc(action).trim().toLowerCase();
        Connection con = null;
        try {
            con = database.getConnection();
            con.setAutoCommit(false);
            checkDupDoc(typeOfDoc, con);
            String add = "insert into " + typeOfDoc + " values( ?, ?, ?, ?, ?, ?, ? )";
            PreparedStatement prepStatement = con.prepareStatement(add);
            prepStatement.setString(1, selectedCourse.getCourseId());
            prepStatement.setString(2, selectedCourse.getAdmin());
            prepStatement.setTimestamp(3, getTimeStamp());
            prepStatement.setString(4, getLink());
            prepStatement.setString(5, homePage.getUser());
            prepStatement.setString(6, getText());
            prepStatement.setString(7, getTitle());
            prepStatement.executeUpdate();
            prepStatement.close();
            con.commit();
        } catch (Exception e) {
            sqlError = true;
            e.printStackTrace();
            if (con != null) try {
                con.rollback();
            } catch (Exception logOrIgnore) {
            }
            try {
                throw e;
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        } finally {
            if (con != null) try {
                con.close();
            } catch (Exception logOrIgnore) {
            }
        }
    }

    /**
	 * Get the current time in a Timestamp format and return it.
	 * 
	 * @source 
	 *         http://www.devdaily.com/java/java-timestamp-example-current-time-now
	 * @return currentTimestamp
	 */
    private Timestamp getTimeStamp() {
        Calendar calendar = Calendar.getInstance();
        java.util.Date now = calendar.getTime();
        java.sql.Timestamp currentTimestamp = new java.sql.Timestamp(now.getTime());
        return currentTimestamp;
    }

    /**
	 * Check for duplicates of the same document. We check against the type
	 * using String typeOfDoc.
	 * 
	 * @param typeOfDoc
	 * @param con
	 * @throws SQLException
	 * @throws Exception
	 */
    private void checkDupDoc(String typeOfDoc, Connection con) throws SQLException, Exception {
        String check = "SELECT * from " + typeOfDoc + " doc, courses c " + "WHERE doc.courseid = c.courseid AND " + "doc.admin = c.admin AND doc.title = ?";
        PreparedStatement prepStatement = con.prepareStatement(check);
        prepStatement.setString(1, this.getTitle().trim());
        ResultSet rs = prepStatement.executeQuery();
        int resultsCounter = 0;
        while (rs.next()) {
            resultsCounter++;
        }
        prepStatement.close();
        if (resultsCounter == 0) {
        } else {
            foundDupDocument = true;
            throw new Exception();
        }
    }

    /**
	 * Given the parameter from the browser, found out what type of document
	 * we're dealing with, i.e. return the String that indicates the type of
	 * document.
	 * 
	 * This could be refactored to a String.contains().
	 * 
	 * @param action
	 * @return
	 */
    public static String findTypeOfDoc(String action) {
        if (action == null) System.out.println("ACTION IS NUL!!!!!!!!!!!!");
        if (action.compareTo("AddHomework") == 0) {
            return "homework";
        } else if (action.compareTo("EditHomework") == 0) {
            return "homework";
        } else if (action.compareTo("DeleteHomework") == 0) {
            return "homework";
        } else if (action.compareTo("AddNote") == 0) {
            return "notes";
        } else if (action.compareTo("EditNote") == 0) {
            return "notes";
        } else if (action.compareTo("DeleteNote") == 0) {
            return "notes";
        } else if (action.compareTo("AddAnnouncement") == 0) {
            return "announcements";
        } else if (action.compareTo("EditAnnouncement") == 0) {
            return "announcements";
        } else if (action.compareTo("DeleteAnnouncement") == 0) {
            return "announcements";
        } else if (action.compareTo("ViewAnnouncement") == 0) {
            return "announcements";
        } else if (action.compareTo("ViewNote") == 0) {
            return "notes";
        } else if (action.compareTo("ViewHomework") == 0) {
            return "homework";
        }
        return "error";
    }

    /**
	 * The String action is given from the URL browser parameter. We want to
	 * find the type of action we're doing, be it add, edit or delete, based on
	 * what the user clicked.
	 * 
	 * @param action
	 * @return
	 */
    public static String findTypeOfAction(String action) {
        if (action.compareTo("AddHomework") == 0) {
            return "add";
        } else if (action.compareTo("EditHomework") == 0) {
            return "edit";
        } else if (action.compareTo("DeleteHomework") == 0) {
            return "delete";
        } else if (action.compareTo("AddNote") == 0) {
            return "add";
        } else if (action.compareTo("EditNote") == 0) {
            return "edit";
        } else if (action.compareTo("DeleteNote") == 0) {
            return "delete";
        } else if (action.compareTo("AddAnnouncement") == 0) {
            return "add";
        } else if (action.compareTo("EditAnnouncement") == 0) {
            return "edit";
        } else if (action.compareTo("DeleteAnnouncement") == 0) {
            return "delete";
        } else if (action.startsWith("View")) {
            return "view";
        }
        return "error";
    }

    private boolean isValidInput() {
        return true;
    }

    public void validate() {
        if (getTitle().length() == 0) {
            addFieldError("title", getText("Title required"));
        }
        if (this.sqlError) {
            addFieldError("title", getText("Bad input: SQL error"));
        }
        if (this.foundDupDocument) {
            addFieldError("title", getText("Document of title " + getTitle() + " already exists. Please try with a different title."));
        }
        if (!getHomePage().isAdmin()) addFieldError("title", getText(" You cannot perform this action "));
    }

    private Map<String, Object> session;

    private String title;

    private String text;

    private String link;

    private Map<String, Object> getSession() {
        return this.session;
    }

    private HomePage getHomePage() {
        Map<String, Object> attibutes = ActionContext.getContext().getSession();
        return ((HomePage) attibutes.get("homePage"));
    }

    public void setServletContext(ServletContext arg0) {
        this.servletContext = arg0;
    }

    public void setTitle(String title) {
        this.title = StringEscapeUtils.escapeHtml(title);
    }

    public String getTitle() {
        return title;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getLink() {
        return link;
    }
}
