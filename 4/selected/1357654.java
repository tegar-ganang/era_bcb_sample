package com.apachetune.httpserver.ui.welcomescreen;

import chrriis.common.WebServer;
import chrriis.dj.nativeswing.swtimpl.components.JWebBrowser;
import chrriis.dj.nativeswing.swtimpl.components.WebBrowserAdapter;
import chrriis.dj.nativeswing.swtimpl.components.WebBrowserCommandEvent;
import com.apachetune.core.AppManager;
import com.apachetune.core.ui.CoreUIWorkItem;
import com.apachetune.core.ui.UIWorkItem;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URI;
import java.util.List;
import static com.apachetune.core.Constants.VELOCITY_LOG4J_APPENDER_NAME;
import static com.apachetune.core.ui.Constants.CORE_UI_WORK_ITEM;
import static com.apachetune.core.ui.Constants.SEND_FEEDBACK_EVENT;
import static com.apachetune.core.utils.Utils.createRuntimeException;
import static com.apachetune.core.utils.Utils.openExternalWebPage;
import static org.apache.commons.lang.StringUtils.removeStart;
import static org.apache.commons.lang.Validate.isTrue;
import static org.apache.commons.lang.Validate.notNull;

/**
 * FIXDOC
 *
 * @author <a href="mailto:progmonster@gmail.com">Aleksey V. Katorgin</a>
 *         Created Date: 18.03.2010
 */
public class WelcomeScreenSmartPart implements VelocityContextProvider, WelcomeScreenView {

    private static final Logger logger = LoggerFactory.getLogger(WelcomeScreenSmartPart.class);

    private static final String START_PAGE_RELATIVE_URL = "index.html.vm";

    private final WelcomeScreenPresenter presenter;

    private final AppManager appManager;

    private final JFrame mainFrame;

    private final CoreUIWorkItem coreUIWorkItem;

    private JPanel mainPanel;

    private JWebBrowser browser;

    private final Object listLock = new Object();

    @Inject
    public WelcomeScreenSmartPart(final WelcomeScreenPresenter presenter, @Named(CORE_UI_WORK_ITEM) final UIWorkItem coreUIWorkItem, final AppManager appManager, final JFrame mainFrame) {
        this.presenter = presenter;
        this.appManager = appManager;
        this.mainFrame = mainFrame;
        this.coreUIWorkItem = (CoreUIWorkItem) coreUIWorkItem;
        $$$setupUI$$$();
        WebServer.getDefaultWebServer().addContentProvider(new WebServer.WebServerContentProvider() {

            @Override
            public WebServer.WebServerContent getWebServerContent(WebServer.HTTPRequest httpRequest) {
                logger.debug("Local web-server requested [requestUrl=" + httpRequest.getURLPath() + ']');
                DefaultWebServerContent content;
                synchronized (listLock) {
                    content = new DefaultWebServerContent(httpRequest, WelcomeScreenSmartPart.this);
                }
                logger.debug("Content handler created [requestUrl=" + httpRequest.getURLPath() + ']');
                return content;
            }
        });
    }

    @Override
    public VelocityContext getVelocityContext() {
        VelocityContext ctx = new VelocityContext();
        return ctx;
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    @Override
    public void setRecentOpenedServerList(List<URI> serverUriList) {
    }

    @Override
    public void initialize(final UIWorkItem workItem) {
        notNull(workItem, "[this=" + this + ']');
        presenter.initialize(workItem, this);
        coreUIWorkItem.switchToWelcomeScreen(getMainPanel());
        browser = new JWebBrowser();
        browser.setDefaultPopupMenuRegistered(false);
        browser.addWebBrowserListener(new WebBrowserAdapter() {

            public void commandReceived(final WebBrowserCommandEvent e) {
                String cmd = e.getCommand();
                if (cmd.equals("openServer")) {
                    presenter.onShowOpenServerDialog();
                } else if (cmd.equals("searchServer")) {
                    presenter.onShowSearchServerDialog();
                } else if (cmd.equals("openProductWebPortal")) {
                    openExternalWebPage(mainFrame, appManager.getProductWebPortalUri());
                } else if (cmd.equals("openProductWebPortal_DonatePage")) {
                    presenter.onOpenWebPortalDonatePage();
                } else if (cmd.equals("sendFeedback")) {
                    coreUIWorkItem.raiseEvent(SEND_FEEDBACK_EVENT);
                } else {
                    throw createRuntimeException("Unknown command [cmd=" + cmd + ']');
                }
            }
        });
        mainPanel.add(browser);
        browser.setBarsVisible(false);
        presenter.onViewReady();
    }

    @Override
    public final void run() {
    }

    @Override
    public void dispose() {
        presenter.onCloseView();
        presenter.dispose();
    }

    private void createUIComponents() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout());
    }

    public void reloadStartPage() {
    }

    public void openStartPage() {
        logger.debug("Open start page... [startPageUrl=" + getStartPageUrl() + ']');
        browser.navigate(getStartPageUrl());
        logger.debug("Start page has been opened [startPageUrl=" + getStartPageUrl() + ']');
    }

    private String getStartPageUrl() {
        return WebServer.getDefaultWebServer().getURLPrefix() + "/" + START_PAGE_RELATIVE_URL;
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }
}

class DefaultWebServerContent extends WebServer.WebServerContent {

    private final WebServer.HTTPRequest request;

    private final VelocityContextProvider velocityContextProvider;

    public DefaultWebServerContent(WebServer.HTTPRequest request, VelocityContextProvider velocityContextProvider) {
        notNull(request);
        notNull(velocityContextProvider);
        this.request = request;
        this.velocityContextProvider = velocityContextProvider;
    }

    @Override
    public String getContentType() {
        String ext = getResourceExtension();
        if (ext.equals("vm")) {
            return "text/html";
        } else {
            return getDefaultMimeType(ext);
        }
    }

    @Override
    public InputStream getInputStream() {
        try {
            InputStream contentIS = WelcomeScreenPresenter.class.getResourceAsStream(removeStart(request.getResourcePath(), "/"));
            if (getResourceExtension().equals("vm")) {
                Reader contentReader = new InputStreamReader(contentIS, "UTF-8");
                return new ByteArrayInputStream(fillVelocityTemplate(contentReader).getBytes("UTF-8"));
            } else {
                return contentIS;
            }
        } catch (Throwable cause) {
            throw createRuntimeException(cause);
        }
    }

    private String fillVelocityTemplate(Reader reader) {
        try {
            StringWriter writer = new StringWriter();
            VelocityContext ctx = velocityContextProvider.getVelocityContext();
            boolean isOk = Velocity.evaluate(ctx, writer, VELOCITY_LOG4J_APPENDER_NAME, reader);
            isTrue(isOk);
            writer.close();
            return writer.toString();
        } catch (Exception e) {
            throw createRuntimeException(e);
        }
    }

    private String getResourceExtension() {
        int dotIdx = request.getResourcePath().lastIndexOf(".");
        if (dotIdx == -1) {
            return "";
        } else {
            return request.getResourcePath().substring(dotIdx + 1);
        }
    }
}

interface VelocityContextProvider {

    VelocityContext getVelocityContext();
}
