package org.xmatthew.spy2servers.component.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationContext;
import org.xmatthew.spy2servers.adapter.spring.ContextServiceLocator;
import org.xmatthew.spy2servers.core.AlertComponent;
import org.xmatthew.spy2servers.core.Component;
import org.xmatthew.spy2servers.core.CoreComponent;
import org.xmatthew.spy2servers.core.Message;
import org.xmatthew.spy2servers.core.MessageAlertChannel;
import org.xmatthew.spy2servers.core.MessageAlertChannelActiveAwareComponent;
import org.xmatthew.spy2servers.core.SpyComponent;
import org.xmatthew.spy2servers.core.context.ComponentContext;
import org.xmatthew.spy2servers.jmx.AlertComponentView;
import org.xmatthew.spy2servers.jmx.ChannelAwareComponentView;
import org.xmatthew.spy2servers.jmx.ComponentViewMBean;
import org.xmatthew.spy2servers.jmx.SpyComponentView;
import org.xmatthew.spy2servers.rule.AlertRule;
import org.xmatthew.spy2servers.rule.SimpleAlertRule;
import org.xmatthew.spy2servers.util.CollectionUtils;

/**
 * @author XieMaLin
 *
 */
public class ComponentsViewServlet extends HttpServlet {

    private ComponentContext componentContext;

    /**
     * serial Version UID
     */
    private static final long serialVersionUID = 3294271328182389316L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String path = request.getServletPath();
        String parameter = request.getParameter("type");
        if (StringUtils.isBlank(parameter)) {
            parameter = "spy";
        }
        StringBuffer sbuff = new StringBuffer(1000);
        sbuff.append("<a href='").append(path).append("?type=spy'>").append("SpyComponents").append("</a>&nbsp;");
        sbuff.append("<a href='").append(path).append("?type=alert'>").append("AlertComponents").append("</a>&nbsp;");
        sbuff.append("<a href='").append(path).append("?type=channel'>").append("ChannelAwareComponents").append("</a>&nbsp;");
        sbuff.append("<a href='").append(path).append("?type=rule'>").append("AlertRule").append("</a><br>");
        if (componentContext == null) {
            componentContext = getComponentContext();
        }
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        List<Component> components = componentContext.getComponents();
        int size = components.size();
        List<SpyComponent> spyComponents = new ArrayList<SpyComponent>(size);
        List<AlertComponent> alertComponents = new ArrayList<AlertComponent>(size);
        List<MessageAlertChannelActiveAwareComponent> channelAwareComponents;
        channelAwareComponents = new ArrayList<MessageAlertChannelActiveAwareComponent>(size);
        CoreComponent coreComponent = null;
        for (Component component : components) {
            if (component instanceof SpyComponent) {
                spyComponents.add((SpyComponent) component);
            } else if (component instanceof AlertComponent) {
                alertComponents.add((AlertComponent) component);
            } else if (component instanceof MessageAlertChannelActiveAwareComponent) {
                channelAwareComponents.add((MessageAlertChannelActiveAwareComponent) component);
            } else if (component instanceof CoreComponent) {
                coreComponent = (CoreComponent) component;
            }
        }
        sbuff.append("<b>Components list:</b><br><br><br>");
        if ("spy".equals(parameter)) {
            getSpyHtmlView(spyComponents, sbuff);
        } else if ("alert".equals(parameter)) {
            getAlertHtmlView(alertComponents, sbuff);
        } else if ("channel".equals(parameter)) {
            getChannelHtmlView(channelAwareComponents, sbuff);
        } else if ("rule".equals(parameter)) {
            if (coreComponent != null) {
                getAlertRuleHtmlView(coreComponent.getAlertRule(), sbuff);
            }
        }
        response.getWriter().println(sbuff);
    }

    private void getAlertRuleHtmlView(AlertRule alertRule, StringBuffer sbuff) {
        if (alertRule instanceof SimpleAlertRule) {
            SimpleAlertRule simpleAlertRule = (SimpleAlertRule) alertRule;
            sbuff.append("<b>Components alert rule:<b><br>");
            Map<String, Set<String>> channelRules = simpleAlertRule.getChannelRules();
            if (CollectionUtils.isBlankMap(channelRules)) {
                return;
            }
            sbuff.append("<table border='1' >");
            sbuff.append("<tr bgcolor='#CCFF66'><td>").append("from");
            sbuff.append("</td>").append("<td>to</td></tr>");
            for (Map.Entry<String, Set<String>> entry : channelRules.entrySet()) {
                sbuff.append("<tr><td><b><font color='FF0000'>").append(entry.getKey());
                sbuff.append("</td></font>").append("<td><font color='FF00FF'>");
                for (String to : entry.getValue()) {
                    sbuff.append(to).append("<br>");
                }
                sbuff.append("</font></td></tr>");
            }
            sbuff.append("</table>");
        }
    }

    private void getChannelHtmlView(List<MessageAlertChannelActiveAwareComponent> channelAwareComponents, StringBuffer sbuff) {
        sbuff.append("<b>MessageAlertChannelActiveAwareComponent</b>: count=").append(channelAwareComponents.size()).append("<br><br>");
        for (MessageAlertChannelActiveAwareComponent channelComponent : channelAwareComponents) {
            genChannelComponentssRef(channelAwareComponents, sbuff);
            sbuff.append("<b><font color='FF0000'>").append(genCompentArch(channelComponent.getName())).append("</font></b><br>");
            ChannelAwareComponentView channelAwareComponentView = new ChannelAwareComponentView(channelComponent);
            viewComponent(channelAwareComponentView, sbuff);
            viewChannelssAsTable(channelComponent.getChannels(), sbuff);
        }
    }

    private String genCompentArch(String name) {
        return "<a name='" + name + "'></a>" + name;
    }

    private void genChannelComponentssRef(List<MessageAlertChannelActiveAwareComponent> channelAwareComponents, StringBuffer sbuff) {
        sbuff.append("<table border='1' ><tr>");
        for (MessageAlertChannelActiveAwareComponent component : channelAwareComponents) {
            genComponentRef(component, sbuff);
        }
        sbuff.append("</tr></table>");
    }

    private void genComponentRef(Component component, StringBuffer sbuff) {
        sbuff.append("<td>");
        sbuff.append("<a href=#").append(component.getName()).append(">");
        sbuff.append(component.getName()).append("</a>");
        sbuff.append("</td>");
    }

    private void viewChannelssAsTable(List<MessageAlertChannel> channels, StringBuffer sbuff) {
        if (CollectionUtils.isBlankCollection(channels)) {
            return;
        }
        int order = 1;
        for (MessageAlertChannel channel : channels) {
            viewChannelAsTable(channel, sbuff, order++);
        }
    }

    private void viewChannelAsTable(MessageAlertChannel channel, StringBuffer sbuff, int order) {
        Message message = channel.getMessage();
        sbuff.append("<table border='1' >");
        viewMessageAsTr(message, sbuff, order);
        sbuff.append("<tr><td>").append("from");
        sbuff.append("</td>").append("<td>").append(channel.getSpyComponent().getName()).append("</td></tr>");
        sbuff.append("<tr><td>").append("to");
        sbuff.append("</td>").append("<td>").append(channel.getAlertComponent().getName()).append("</td></tr>");
        sbuff.append("</table>");
    }

    private void getAlertHtmlView(List<AlertComponent> alertComponents, StringBuffer sbuff) {
        sbuff.append("<b>AlertComonent</b>: count=").append(alertComponents.size()).append("</font><br><br>");
        for (AlertComponent alertComponent : alertComponents) {
            genALertComponentssRef(alertComponents, sbuff);
            sbuff.append("<b><font color='FF0000'>").append(genCompentArch(alertComponent.getName())).append("</font></b><br>");
            AlertComponentView alertComponentView = new AlertComponentView(alertComponent);
            viewComponent(alertComponentView, sbuff);
            viewMessagesAsTable(alertComponent.getMessages(), sbuff);
        }
    }

    private void genALertComponentssRef(List<AlertComponent> alertComponents, StringBuffer sbuff) {
        sbuff.append("<table border='1' ><tr>");
        for (AlertComponent component : alertComponents) {
            genComponentRef(component, sbuff);
        }
        sbuff.append("</tr></table>");
    }

    private void genSpyComponentssRef(List<SpyComponent> spyComponents, StringBuffer sbuff) {
        sbuff.append("<table border='1' ><tr>");
        for (SpyComponent component : spyComponents) {
            genComponentRef(component, sbuff);
        }
        sbuff.append("</tr></table>");
    }

    private void getSpyHtmlView(List<SpyComponent> spyComponents, StringBuffer sbuff) {
        sbuff.append("<b>SpyComonent</b>: count=").append(spyComponents.size()).append("<br><br>");
        for (SpyComponent spyComponent : spyComponents) {
            genSpyComponentssRef(spyComponents, sbuff);
            sbuff.append("<b><font color='FF0000'>").append(genCompentArch(spyComponent.getName())).append("</font></b><br>");
            SpyComponentView spyComponentView = new SpyComponentView(spyComponent);
            viewComponent(spyComponentView, sbuff);
            viewMessagesAsTable(spyComponent.getMessages(), sbuff);
        }
    }

    private void viewMessagesAsTable(List<Message> messages, StringBuffer sbuff) {
        if (CollectionUtils.isBlankCollection(messages)) {
            return;
        }
        sbuff.append("<table>");
        int i = 1;
        for (Message message : messages) {
            sbuff.append("<tr><td>");
            viewMessageAsTable(message, sbuff, i++);
            sbuff.append("</td></tr>");
        }
        sbuff.append("</table>");
    }

    private void viewMessageAsTable(Message message, StringBuffer sbuff, int order) {
        sbuff.append("<table border='1' >");
        viewMessageAsTr(message, sbuff, order);
        sbuff.append("</table>");
    }

    private void viewMessageAsTr(Message message, StringBuffer sbuff, int order) {
        sbuff.append("<tr bgcolor='#CCFF66'><td>").append("order");
        sbuff.append("</td>").append("<td>").append(order).append("</td></tr>");
        sbuff.append("<tr><td>").append("id");
        sbuff.append("</td>").append("<td>").append(message.getId()).append("</td></tr>");
        sbuff.append("<tr><td>").append("level");
        sbuff.append("</td>").append("<td>").append(message.getLevel()).append("</td></tr>");
        sbuff.append("<tr><td>").append("body");
        sbuff.append("</td>").append("<td>").append(message.getBody()).append("</td></tr>");
        sbuff.append("<tr><td>").append("description");
        sbuff.append("</td>").append("<td>").append(message.getDescription()).append("</td></tr>");
        sbuff.append("<tr><td>").append("type");
        sbuff.append("</td>").append("<td>").append(message.getType()).append("</td></tr>");
        sbuff.append("<tr><td>").append("properties");
        sbuff.append("</td>").append("<td>").append(message.getProperties()).append("</td></tr>");
    }

    private void viewComponent(ComponentViewMBean componentView, StringBuffer sbuff) {
        sbuff.append("message count=").append(componentView.getMessageCount()).append("<br>");
        sbuff.append("component status=").append(componentView.getStatusName()).append("<br>");
        sbuff.append("startup date=").append(componentView.getStartupDate()).append("<br>");
    }

    private ComponentContext getComponentContext() {
        ApplicationContext context = ContextServiceLocator.getContext();
        Map beansMap = context.getBeansOfType(CoreComponent.class);
        if (beansMap != null && beansMap.size() > 0) {
            return ((CoreComponent) beansMap.values().iterator().next()).getContext();
        }
        return null;
    }
}
