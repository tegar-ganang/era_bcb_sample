package com.tysanclan.site.projectewok.pages;

import org.apache.wicket.Page;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import com.jeroensteenbeeke.hyperion.data.ModelMaker;
import com.tysanclan.site.projectewok.TysanPage;
import com.tysanclan.site.projectewok.beans.BugTrackerService;
import com.tysanclan.site.projectewok.entities.Bug;

/**
 * @author Jeroen Steenbeeke
 */
public class TysanErrorPage extends TysanPage {

    @SpringBean
    private BugTrackerService bugTrackerService;

    /**
	 * @param originalPage
	 *            The page we were trying to view
	 * @param exception
	 *            The exception thrown
	 */
    public TysanErrorPage(Page originalPage, final Exception exception) {
        super("An error has occurred");
        boolean known = (bugTrackerService.isKnownIssue(exception));
        Bug report = bugTrackerService.reportCrash(getUser(), originalPage != null ? originalPage.getClass().getSimpleName() : null, exception);
        if (known) {
            if (report.getComments().isEmpty()) {
                add(new Label("issuedescriptor", "This issue has already been reported, but the person who last encountered it did not write a report of what he or she was doing when it happened. Please help us by doing so"));
            } else {
                add(new Label("issuedescriptor", "This issue has already been reported and described, so we probably already know enough to solve it. Still, if you feel you may have vital info to help us solve it, please fill out what you were doing when the error occurred."));
            }
        } else {
            add(new Label("issuedescriptor", "We have not encountered this issue before. It would be of great help if you could tell us what you were trying to do when the error occurred."));
        }
        final TextArea<String> reportArea = new TextArea<String>("report", new Model<String>(""));
        Form<Bug> reportForm = new Form<Bug>("bugreport", ModelMaker.wrap(report)) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit() {
                bugTrackerService.addCommentToBug(getModelObject(), getUser(), reportArea.getModelObject());
                setResponsePage(NewsPage.class);
            }
        };
        reportForm.add(reportArea);
        add(reportForm);
    }
}
