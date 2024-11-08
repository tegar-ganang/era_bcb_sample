package org.tapestrycomponents.tassel.components;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.tapestry.IForm;
import org.apache.tapestry.IMarkupWriter;
import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.IResourceLocation;
import org.apache.tapestry.IScript;
import org.apache.tapestry.engine.IScriptSource;
import org.apache.tapestry.form.AbstractFormComponent;
import org.apache.tapestry.html.Body;

/**
 * @author robertz
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public abstract class ThreadedDiscussionHandler extends AbstractFormComponent {

    public abstract void setTopmessages(List msgs);

    public abstract List getTopmessages();

    public abstract void setNewmessage(IThreadedMessage msg);

    public abstract IThreadedMessage getNewmessage();

    public abstract Object getUserobj();

    private static Logger log = Logger.getLogger(ThreadedDiscussionHandler.class);

    private IScript script = null;

    private SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

    protected void renderComponent(IMarkupWriter writer, IRequestCycle cycle) {
        log.setLevel(Level.DEBUG);
        IForm form = getForm(cycle);
        String name = form.getElementId(this);
        if (form.isRewinding()) {
            log.debug("We're rewinding");
            if (getNewmessage() != null) {
                log.debug("we have a valid newmessage");
                log.debug("userobj return: " + getUserobj());
                if (!isNull(getUserobj())) {
                    log.debug("we have a valid userobj");
                    String msgText = cycle.getRequestContext().getParameter(name + "text");
                    log.debug("msgText: " + msgText);
                    if (!isNull(msgText)) {
                        String msgSubject = cycle.getRequestContext().getParameter(name + "subject");
                        if (isNull(msgSubject)) msgSubject = "no subj";
                        log.debug("subj: " + msgSubject);
                        String msgID = cycle.getRequestContext().getParameter(name + "messageid");
                        log.debug("msgID: " + msgID);
                        if (isNull(msgID)) {
                            log.debug("newmessage");
                            getNewmessage().setNewMsg(true);
                        } else {
                            log.debug("reply to a msg");
                            getNewmessage().setNewMsg(false);
                            getNewmessage().setParent(msgID);
                        }
                        getNewmessage().setMessage(msgText);
                        getNewmessage().setSubject(msgSubject);
                        getNewmessage().setWrittenDate(new Date());
                        getNewmessage().setMessageAuthor(getUserobj().toString());
                        getNewmessage().save();
                    }
                }
            }
            return;
        }
        log.debug("not rewinding...");
        log.debug("writing a form element...");
        writer.begin("input");
        writer.attribute("type", "hidden");
        writer.attribute("name", name + "messageid");
        writer.end();
        writer.begin("input");
        writer.attribute("type", "hidden");
        writer.attribute("name", name + "subject");
        writer.end();
        writer.begin("input");
        writer.attribute("type", "hidden");
        writer.attribute("name", name + "text");
        writer.end();
        if (!isNull(this.getTopmessages())) {
            writer.begin("ul");
            for (int i = 0; i < getTopmessages().size(); i++) {
                log.debug("rendering msg #: " + i);
                IThreadedMessage msg = (IThreadedMessage) getTopmessages().get(i);
                renderMessage(writer, cycle, msg, name);
                log.debug("finished rendering msg #: " + i);
            }
            writer.end();
        }
        log.debug("finished rendering list of messages... checking for usr object");
        if (!isNull(getUserobj())) {
            log.debug("user object exists; rendering new msg box...");
            writer.begin("div");
            writer.attribute("id", name + "newmsgdiv");
            writer.print(getMessage("subject"));
            writer.begin("input");
            writer.attribute("type", "text");
            writer.attribute("name", name + "newmsgsubject");
            writer.end();
            writer.beginEmpty("br");
            writer.begin("span");
            writer.attribute("class", name + "MessageTextHeader");
            writer.print(getMessage("msg-text"));
            writer.end();
            writer.begin("textarea");
            writer.attribute("cols", 75);
            writer.attribute("rows", 10);
            writer.attribute("name", name + "newmsgtext");
            writer.end();
            writer.beginEmpty("br");
            writer.begin("input");
            writer.attribute("type", "button");
            writer.attribute("value", getMessage("newmsg"));
            writer.attributeRaw("onclick", "this.form." + name + "text.value=this.form." + name + "newmsgtext.value;" + "this.form." + name + "subject.value=this.form." + name + "newmsgsubject.value;this.form.submit();");
            writer.end();
            writer.end();
        }
        log.debug("rendering the script...");
        if (script == null) {
            IScriptSource source = cycle.getEngine().getScriptSource();
            IResourceLocation specLocation = getSpecification().getLocation().getResourceLocation();
            IResourceLocation scriptLocation = specLocation.getRelativeLocation("ThreadedDiscussionHandler.script");
            script = source.getScript(scriptLocation);
        }
        Body body = Body.get(cycle);
        Map symbols = new HashMap();
        script.execute(cycle, body, symbols);
        log.debug("finished rendering");
    }

    private void renderMessage(IMarkupWriter writer, IRequestCycle cycle, IThreadedMessage msg, String name) {
        log.debug("rendering a single msg...");
        String cName = name + "message" + msg.getMsgID();
        writer.begin("li");
        writer.begin("a");
        writer.attribute("href", "#");
        writer.attribute("id", cName + "header");
        writer.attributeRaw("onclick", "return toggleView('" + cName + "');");
        writer.print(msg.getSubject() + "(" + msg.getMessageAuthor() + " - " + df.format(msg.getWrittenDate()) + ")");
        writer.end();
        writer.begin("span");
        writer.attribute("id", cName);
        writer.attribute("style", "display:none;");
        writer.begin("span");
        writer.attribute("id", cName + "body");
        writer.attribute("style", "display:block; width: 450px;");
        writer.begin("span");
        writer.attribute("id", cName + "bodylabel");
        writer.attribute("style", "display:block");
        writer.print(getMessage("msg-text"));
        writer.end();
        writer.begin("span");
        writer.attribute("id", cName + "text");
        writer.attribute("style", "display:block; margin-left: 10px");
        writer.print(msg.getMessage());
        writer.end();
        writer.end();
        log.debug("before the 'has user' check...");
        if (!isNull(this.getUserobj())) {
            writer.begin("span");
            writer.attribute("id", cName + "controls");
            writer.attribute("style", "display:block");
            writer.begin("a");
            writer.attribute("href", "#");
            writer.attributeRaw("onclick", "return toggleView('" + cName + "replycontrol');");
            writer.print(getMessage("reply"));
            writer.end();
            writer.end();
            writer.begin("span");
            writer.attribute("id", cName + "replycontrol");
            writer.attribute("style", "display:none");
            writer.print(getMessage("subject"));
            writer.begin("input");
            writer.attribute("type", "text");
            writer.attribute("name", cName + "msgsubject");
            writer.attribute("value", "re: " + msg.getSubject());
            writer.end();
            writer.beginEmpty("br");
            writer.begin("span");
            writer.attribute("class", name + "MessageTextHeader");
            writer.print(getMessage("msg-text"));
            writer.end();
            writer.begin("textarea");
            writer.attribute("cols", "75");
            writer.attribute("rows", "10");
            writer.attribute("name", cName + "msgtext");
            writer.end();
            writer.beginEmpty("br");
            writer.begin("input");
            writer.attribute("type", "button");
            writer.attribute("value", getMessage("reply"));
            writer.attributeRaw("onclick", "this.form." + name + "messageid.value=" + msg.getMsgID() + ";this.form." + name + "text.value=this.form." + cName + "msgtext.value;this.form." + name + "subject.value=this.form." + cName + "msgsubject.value;this.form.submit();");
            writer.end();
            writer.end();
        }
        writer.end();
        log.debug("attempting to get msg responses...");
        List l = msg.getResponses();
        if (l.size() > 0) {
            writer.begin("ul");
            for (int i = 0; i < l.size(); i++) {
                log.debug("rendering response #:" + i);
                IThreadedMessage response = (IThreadedMessage) l.get(i);
                if (!response.getMsgID().equals(msg.getMsgID())) {
                    log.debug("response's id didn't equal the msg id... responseid: " + response.getMsgID() + "; msgid: " + msg.getMsgID());
                    this.renderMessage(writer, cycle, response, name);
                }
            }
            writer.end();
        }
        log.debug("finished rendering responses.");
        writer.end();
    }

    private boolean isNull(Object obj) {
        if (obj == null) return true;
        if (obj instanceof String && "".equals((String) obj)) return true;
        if (obj instanceof List && ((List) obj).size() < 1) return true;
        return false;
    }

    public boolean isDisabled() {
        return false;
    }
}
