package org.xtoto.ui.chat;

import org.xtoto.dao.UserDao;
import org.xtoto.model.Comment;
import org.xtoto.service.CommentService;
import org.xtoto.start.XtotoApplication;
import org.xtoto.ui.utils.core.NavigablePage;
import org.xtoto.utils.JavaScriptUtils;
import org.xtoto.utils.SessionUtils;
import com.ocpsoft.pretty.time.PrettyTime;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.validator.StringValidator;
import org.wicketstuff.push.ChannelEvent;
import org.wicketstuff.push.IChannelListener;
import org.wicketstuff.push.IChannelService;
import org.wicketstuff.push.IChannelTarget;
import org.wicketstuff.push.cometd.CometdService;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class Chats extends NavigablePage {

    public static final int MAX_COMMENTS = 6;

    @SpringBean
    CommentService commentService;

    @SpringBean
    UserDao userDao;

    public Chats() {
        super();
        final CometdService service = (CometdService) getChannelService();
        final Label chat = new Label("chat");
        chat.setOutputMarkupId(true);
        service.addChannelListener(this, "chat", new IChannelListener() {

            public void onEvent(final String channel, final Map<String, String> map, final IChannelTarget target) {
                target.appendJavascript(JavaScriptUtils.prepend(chat, createChatEntry(map)));
            }
        });
        add(chat);
        final FeedbackPanel feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        add(feedback);
        add(new CommentsForm("commentForm", feedback));
    }

    private void loadComments() {
        List<Comment> comments = commentService.findComments(MAX_COMMENTS);
        for (int i = 0; i < MAX_COMMENTS && i < comments.size(); ++i) {
            Comment comment = comments.get(i);
            publish(comment);
        }
    }

    private String createChatEntry(Map<String, String> map) {
        String result = "<strong>" + map.get("author") + ":  </strong>" + map.get("message") + "<br/>" + "<span class=\"time\">" + map.get("time") + "</span>" + "<div class=\"thin_bottom_line\"></div>";
        return result;
    }

    private void publish(Comment comment) {
        final ChannelEvent event = new ChannelEvent("chat");
        event.addData("author", comment.getAuthor().getForename() + " " + comment.getAuthor().getSurname());
        event.addData("message", comment.getMessage());
        PrettyTime prettyTime = new PrettyTime();
        event.addData("time", prettyTime.format(comment.getCommentDate()));
        getChannelService().publish(event);
    }

    protected IChannelService getChannelService() {
        return XtotoApplication.get().getCometdService();
    }

    class CommentsForm extends Form {

        CommentsForm(String id, final FeedbackPanel feedback) {
            super(id);
            final TextArea<String> editor = new TextArea<String>("editor", new Model<String>(""));
            editor.setOutputMarkupId(true);
            editor.add(StringValidator.maximumLength(140));
            add(editor);
            add(new AjaxSubmitLink("save") {

                @Override
                protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                    Object object = editor.getModelObject();
                    if (object == null) return;
                    Comment comment = new Comment(object.toString(), SessionUtils.getUser(userDao), new Date());
                    commentService.post(comment);
                    editor.setModel(new Model<String>(""));
                    publish(comment);
                    target.addComponent(editor);
                    target.addComponent(feedback);
                    target.focusComponent(editor);
                }

                @Override
                protected void onError(AjaxRequestTarget target, Form<?> form) {
                    target.addComponent(feedback);
                }
            });
        }
    }
}
