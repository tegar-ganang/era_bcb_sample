package annone.engine.jsp;

import java.io.InputStream;
import javax.servlet.http.HttpSession;
import annone.engine.Channel;

public class JspSession {

    private static final String CHANNEL_SESSION_ATTRIBUTE = JspSession.class.getName() + ".channel";

    public static String getLanguage(HttpSession session) {
        return "it";
    }

    public static String getTheme(HttpSession session) {
        return "themes/default/theme.css";
    }

    public static Channel getChannel(HttpSession session) {
        synchronized (session) {
            Channel channel = (Channel) session.getAttribute(CHANNEL_SESSION_ATTRIBUTE);
            if (channel == null) {
                channel = JspApplication.getEngine(session.getServletContext()).newChannel();
                session.setAttribute(CHANNEL_SESSION_ATTRIBUTE, channel);
            }
            return channel;
        }
    }

    public static InputStream getResourceAsStream(HttpSession session, String name) {
        return JspSession.class.getResourceAsStream(name);
    }
}
