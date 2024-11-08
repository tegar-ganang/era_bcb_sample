package net.sf.openknowledge;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import wicket.MarkupContainer;
import wicket.markup.html.form.ChoiceRenderer;
import wicket.markup.html.form.Form;
import wicket.markup.html.form.RadioChoice;
import wicket.markup.html.form.TextArea;
import wicket.markup.html.form.TextField;
import wicket.markup.html.panel.FeedbackPanel;
import wicket.model.Model;
import wicket.model.PropertyModel;
import wicket.protocol.http.WebRequestCycle;

/**
 * @author nato
 */
public class AddPage extends TemplatePage {

    AddForm form;

    /**
     * Creates a new instance of AddPage
     */
    public AddPage() {
        build();
    }

    private void build() {
        form = new AddForm(this, "addForm");
        new FeedbackPanel(this, "feedback");
    }
}

class AddForm extends Form {

    private int userId;

    private String title = "";

    private String content = "";

    private String tags = "";

    private Accessibility accessibility = new Accessibility(0);

    AddForm(MarkupContainer parent, String id) {
        super(parent, id);
        userId = OkSession.get().getUserId();
        if (userId == 0) setResponsePage(LoginPage.class);
        build();
    }

    private void build() {
        TextField titleW = new TextField(this, "titleW", new PropertyModel(this, "title"));
        TextArea contentW = new TextArea(this, "contentW", new PropertyModel(this, "content"));
        TextField tagsW = new TextField(this, "tagsW", new PropertyModel(this, "tags"));
        List accList = Accessibility.getAllByStyle('A');
        RadioChoice accessibility = new RadioChoice(this, "accessibility", new PropertyModel(this, "accessibility"), accList, new ChoiceRenderer("desc", "id"));
        accessibility.setSuffix("<br/>");
    }

    protected void onSubmit() {
        try {
            Connection conn = ((JdbcRequestCycle) getRequestCycle()).getConnection();
            String sql = "insert into entry (author, accessibility) values(?,?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            pstmt.setInt(2, accessibility.getId());
            pstmt.executeUpdate();
            ResultSet insertedEntryIdRs = pstmt.getGeneratedKeys();
            insertedEntryIdRs.next();
            int insertedEntryId = insertedEntryIdRs.getInt(1);
            sql = "insert into revisions (title, entry, content, tags," + " revision_remark) values(?,?,?,?,?)";
            PreparedStatement pstmt2 = conn.prepareStatement(sql);
            pstmt2.setString(1, getTitle());
            pstmt2.setInt(2, insertedEntryId);
            pstmt2.setString(3, getContent());
            pstmt2.setString(4, getTags());
            pstmt2.setString(5, "newly added");
            int insertCount = pstmt2.executeUpdate();
            if (insertCount > 0) {
                info("Successfully added one new record.");
            } else {
                conn.rollback();
                info("Addition of one new record failed.");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Accessibility getAccessibility() {
        return accessibility;
    }

    public void setAccessibility(Accessibility accessibility) {
        this.accessibility = accessibility;
    }
}
