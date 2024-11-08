package net.sf.openknowledge;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import net.sf.openknowledge.LoginPanel;
import wicket.PageParameters;
import wicket.RestartResponseException;
import wicket.extensions.behavior.SimpleAttributeModifier;
import wicket.markup.html.basic.Label;
import wicket.markup.html.panel.FeedbackPanel;
import wicket.model.Model;
import wicket.util.string.StringValueConversionException;

/**
 *
 * @author nato
 */
public class DeletePage extends TemplatePage {

    private int entry;

    private int authorId;

    private String title;

    private String username;

    /**
     * Creates a new instance of DeletePage
     */
    public DeletePage() {
        this(null);
    }

    /**
     * Creates a new instance of DeletePage with PageParameters
     */
    public DeletePage(PageParameters pp) {
        super(pp);
        if (pp != null) {
            try {
                entry = pp.getInt("entry");
            } catch (StringValueConversionException ex) {
                ex.printStackTrace();
            }
            initialize();
        }
        if (!isLoggedIn()) {
            redirectToInterceptPage(new LoginPage());
        } else if (!canDelete()) {
            throw new RestartResponseException(new OkErrorPage(OkErrorEnum.NODELETE));
        }
        build();
    }

    private void build() {
        new Label(this, "title", title);
        LoginPanel loginPanel = new LoginPanel(this, "loginPanel");
        loginPanel.getUsernameField().setModelObject(username);
        loginPanel.getUsernameField().setEnabled(false);
        loginPanel.getLoginForm().setNewLoginResultBehavior(new LoginForm.LoginResultBehavior() {

            public void onSuccessfulLogin(int userId, boolean superUser) {
                if (delete()) {
                    info("successfully deleted the " + "entry entitled \"" + title + "\"");
                } else {
                    throw new RestartResponseException(new OkErrorPage(OkErrorEnum.DELETEFAILURE));
                }
            }

            public void onFailedLogin() {
                error("Authentication failed. Please try again.");
            }
        });
        new FeedbackPanel(this, "feedback");
        loginPanel.getFeedbackPanel().setVisible(false);
    }

    private boolean canDelete() {
        return ((authorId == OkSession.get().getUserId()) || OkSession.get().isSuperUser());
    }

    private void initialize() {
        try {
            Connection conn = ((JdbcRequestCycle) getRequestCycle()).getConnection();
            if (conn == null) {
                throw new RestartResponseException(new OkErrorPage(OkErrorEnum.DATABASE));
            }
            String query = "select uid, title, username from scan " + "where eid=? limit 1";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, entry);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            authorId = rs.getInt("uid");
            title = rs.getString("title");
            username = rs.getString("username");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private boolean delete() {
        boolean ret = false;
        try {
            Connection conn = ((JdbcRequestCycle) getRequestCycle()).getConnection();
            if (conn == null) {
                throw new RestartResponseException(new OkErrorPage(OkErrorEnum.DATABASE));
            }
            String query = "delete from revisions where entry=?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, entry);
            int revisionsRowsAffected = pstmt.executeUpdate();
            query = "delete from entry where id=?";
            pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, entry);
            int entryRowAffected = pstmt.executeUpdate();
            if (entryRowAffected > 0) {
                ret = true;
            } else {
                conn.rollback();
            }
            info(entryRowAffected + " entry with " + revisionsRowsAffected + " revisions was deleted.");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return ret;
    }
}
