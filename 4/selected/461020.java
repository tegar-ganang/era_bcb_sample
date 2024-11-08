package com.spring.rssReader.web;

import com.spring.rssReader.Channel;
import com.spring.rssReader.jdbc.ChannelController;
import com.spring.workflow.WorkflowConstants;
import com.spring.workflow.mvc.WorkflowSimpleFormController;
import com.spring.workflow.parser.PageDefinition;
import org.springframework.validation.BindException;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ronald Haring
 *         Date: 3-jan-2004
 *         Time: 14:11:06
 *         To change this template use Options | File Templates.
 */
public class EditChannel extends WorkflowSimpleFormController {

    private ChannelController channelController;

    public static final String CHANNEL_FOR_EDIT = "com.spring.rssReader.web.EditChannel.id";

    public EditChannel() {
        setSessionForm(false);
        setBindOnNewForm(false);
        setCommandName("channel");
    }

    protected Object formBackingObject(HttpServletRequest request) throws ServletException {
        long id = ServletRequestUtils.getLongParameter(request, WorkflowConstants.ID_NAME, -1);
        Channel channel = null;
        if (id != -1) {
            channel = channelController.getChannel(new Long(id));
            request.getSession().setAttribute(CHANNEL_FOR_EDIT, channel.getId());
            if (channel == null || channel.getId() == null) {
                channel = (Channel) request.getSession().getAttribute(ChannelWebController.ACTIVE_CHANNEL);
                if (channel == null) {
                    String page = getCurrentPage(request);
                    Map model = new HashMap();
                    model.put("message", "backbutton");
                    return new ModelAndView("forward:" + page, model);
                }
            }
        } else {
            if (request.getSession().getAttribute(CHANNEL_FOR_EDIT) == null) {
                channel = new Channel();
            } else {
                channel = channelController.getChannel((Long) request.getSession().getAttribute(CHANNEL_FOR_EDIT));
            }
        }
        return channel;
    }

    public String getCurrentPage(HttpServletRequest request) {
        return ((PageDefinition) request.getSession().getAttribute(WorkflowConstants.RESPONSE_PAGE)).getFullName();
    }

    public ModelAndView getCurrent(HttpServletRequest request) throws ServletException {
        return new ModelAndView("forward:" + getCurrentPage(request) + "/" + (String) request.getSession().getAttribute("current"));
    }

    protected ModelAndView processFormSubmission(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors) throws Exception {
        if (errors.hasErrors()) {
            return setErrors(request, response, command, errors);
        }
        return super.processFormSubmission(request, response, command, errors);
    }

    protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors) throws Exception {
        Channel channel = (Channel) command;
        channelController.update(channel);
        Map model = new HashMap();
        model.putAll(errors.getModel());
        model.put("message", "channel.saved");
        request.getSession().setAttribute(getFormSessionAttributeName(), null);
        request.getSession().setAttribute(CHANNEL_FOR_EDIT, null);
        request.getSession().setAttribute(ChannelWebController.ACTIVE_CHANNEL, channel);
        return new ModelAndView("", model);
    }

    protected ModelAndView handleInvalidSubmit(HttpServletRequest request, HttpServletResponse response) throws Exception {
        return showNewForm(request, response);
    }

    public ChannelController getChannelController() {
        return channelController;
    }

    public void setChannelController(ChannelController channelController) {
        this.channelController = channelController;
    }
}
