package whiteboard.poll;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;
import org.apache.struts2.util.ServletContextAware;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.sql.DataSource;
import org.apache.commons.lang.StringEscapeUtils;
import whiteboard.HomePage;
import whiteboard.course.Course;

/**
 * The action for adding a poll.
 * 
 * @author John
 * 
 */
public class AddPoll extends ActionSupport implements ServletContextAware {

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
        DataSource dbcp = (DataSource) servletContext.getAttribute("dbpool");
        int pollid = addPollToDB(dbcp);
        addPollToInstance(pollid);
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
	 * Add the new poll to the course instance vector with poll id passedPollId.
	 * Initialize all votes to 0.
	 */
    private void addPollToInstance(int passedPollId) {
        Poll p = new Poll(selectedCourse.getAdmin(), selectedCourse.getCourseId(), getTitle(), passedPollId);
        p.addPollOption(getOption1(), 0);
        p.addPollOption(getOption2(), 0);
        p.addPollOption(getOption3(), 0);
        p.addPollOption(getOption4(), 0);
        selectedCourse.addPoll(p);
    }

    /**
	 * Add a poll to the database. We also add its options. We return
	 * poll id that was created in the database as an int.
	 * 
	 * If there's an exception, roll back changes.
	 * 
	 * @source Template for try catch.
	 *         http://stackoverflow.com/questions/3160756
	 *         /in-jdbc-when-autocommit
	 *         -is-false-and-no-explicit-savepoints-have-been-set-is-it
	 * @param database
	 */
    private int addPollToDB(DataSource database) {
        int pollid = -2;
        Connection con = null;
        try {
            con = database.getConnection();
            con.setAutoCommit(false);
            String add = "insert into polls" + " values( ?, ?, ?, ?)";
            PreparedStatement prepStatement = con.prepareStatement(add);
            prepStatement.setString(1, selectedCourse.getAdmin());
            prepStatement.setString(2, selectedCourse.getCourseId());
            prepStatement.setString(3, getTitle());
            prepStatement.setInt(4, 0);
            prepStatement.executeUpdate();
            String findNewID = "select max(pollid) from polls";
            prepStatement = con.prepareStatement(findNewID);
            ResultSet newID = prepStatement.executeQuery();
            pollid = -2;
            while (newID.next()) {
                pollid = newID.getInt(1);
            }
            if (pollid == -2) {
                this.sqlError = true;
                throw new Exception();
            }
            String[] options = getAllOptions();
            for (int i = 0; i < 4; i++) {
                String insertOption = "insert into polloptions values ( ?, ?, ? )";
                prepStatement = con.prepareStatement(insertOption);
                prepStatement.setString(1, options[i]);
                prepStatement.setInt(2, 0);
                prepStatement.setInt(3, pollid);
                prepStatement.executeUpdate();
            }
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
        return pollid;
    }

    /**
	 * Get all all the options and put it into a String array.
	 */
    private String[] getAllOptions() {
        String[] result = new String[4];
        result[0] = getOption1();
        result[1] = getOption2();
        result[2] = getOption3();
        result[3] = getOption4();
        return result;
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
        if (!getHomePage().isAdmin()) addFieldError("title", getText(" You cannot perform this action "));
    }

    private Map<String, Object> session;

    private String title;

    private String option1;

    private String option2;

    private String option3;

    private String option4;

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

    public void setOption1(String option1) {
        this.option1 = option1;
    }

    public String getOption1() {
        return option1;
    }

    public void setOption2(String option2) {
        this.option2 = option2;
    }

    public String getOption2() {
        return option2;
    }

    public void setOption3(String option3) {
        this.option3 = option3;
    }

    public String getOption3() {
        return option3;
    }

    public void setOption4(String option4) {
        this.option4 = option4;
    }

    public String getOption4() {
        return option4;
    }
}
