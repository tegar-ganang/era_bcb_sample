package com.quickwcm.render.impl;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.quickwcm.content.Content;
import com.quickwcm.content.DraftOrPublishedView;
import com.quickwcm.content.Page;
import com.quickwcm.dao.ContentDAO;
import com.quickwcm.dao.PageDAO;
import com.quickwcm.layout.LayoutRegistry;
import com.quickwcm.page.PageState;
import com.quickwcm.page.PageStateService;
import com.quickwcm.render.PageRenderer;
import com.quickwcm.security.SecurityProvider;
import com.quickwcm.url.QuickWCMURL;
import com.quickwcm.url.URLService;
import com.quickwcm.utils.ExceptionUtils;
import com.quickwcm.utils.FileUtils;

/**
 * Page renderer implementation.
 *  
 * @author pavelgj
 */
public class PageRendererImpl implements PageRenderer {

    private DraftOrPublishedView draftOrPublishedView;

    private ContentDAO contentDAO;

    private PageDAO pageDAO;

    private LayoutRegistry layoutregistry;

    private Provider<PageStateService> pageStateServiceProvider;

    private SecurityProvider securityProvider;

    private Provider<URLService> urlServiceProvider;

    private Exception internalException;

    @Inject
    public void setDraftOrPublishedView(DraftOrPublishedView draftOrPublishedView) {
        this.draftOrPublishedView = draftOrPublishedView;
    }

    @Inject
    public void setUrlServiceProvider(Provider<URLService> urlServiceProvider) {
        this.urlServiceProvider = urlServiceProvider;
    }

    @Inject
    public void setContentDAO(ContentDAO contentDAO) {
        this.contentDAO = contentDAO;
    }

    @Inject
    public void setSecurityProvider(SecurityProvider securityProvider) {
        this.securityProvider = securityProvider;
    }

    @Inject
    public void setPageDAO(PageDAO pageDAO) {
        this.pageDAO = pageDAO;
    }

    @Inject
    public void setLayoutregistry(LayoutRegistry layoutregistry) {
        this.layoutregistry = layoutregistry;
    }

    @Inject
    public void setPageStateServiceProvider(Provider<PageStateService> pageStateServiceProvider) {
        this.pageStateServiceProvider = pageStateServiceProvider;
    }

    public void renderPage(Page page, QuickWCMURL url, HttpServletRequest request, HttpServletResponse response) {
        String tmpFileName = FileUtils.getNewTmpFile() + ".html";
        try {
            Page rootPage = pageDAO.getPagesTree(draftOrPublishedView.isDraft());
            pageDAO.populateChildren(page, draftOrPublishedView.isDraft());
            PrintWriter out = new PrintWriter(new FileOutputStream(tmpFileName));
            VelocityContext context = new VelocityContext();
            context.put("qwcmPrintWriter", out);
            context.put("httpRequest", request);
            context.put("httpResponse", response);
            context.put("qwcmPageTitle", page.getTitle());
            context.put("qwcmVelocityPageContext", context);
            context.put("qwcmContextPath", url.getContextPath());
            context.put("qwcmThisPage", page);
            context.put("qwcmThisPageContents", checkContentSecurity(contentDAO.getPageContents(page.getId(), draftOrPublishedView.isDraft())));
            context.put("qwcmURL", url);
            context.put("qwcmRootPage", rootPage);
            PageState pageState = pageStateServiceProvider.get().getPageState(page.getId());
            if (pageState.getLastMaximizedContentId() == null) {
                context.put("qwcmLayoutRenderer", layoutregistry.getLayoutRenderer(page.getLayoutId()));
            } else {
                context.put("qwcmLayoutRenderer", layoutregistry.getMaximizedLayoutRenderer());
            }
            try {
                Velocity.getTemplate("themes/default/main.vm").merge(context, out);
            } finally {
                out.close();
            }
            dumpFile(tmpFileName, response);
        } catch (Exception e) {
            e.printStackTrace();
            internalException = e;
            QuickWCMURL errorUrl = urlServiceProvider.get().getBasicURL(request);
            errorUrl.setInternal(true);
            errorUrl.setPageId("error");
            renderInternalPage(errorUrl, request, response);
        } finally {
            FileUtils.deleteTmpFile(tmpFileName);
        }
    }

    private static Map<String, String> internalPageTitles = new HashMap<String, String>();

    static {
        internalPageTitles.put("404", "Page Not Found");
        internalPageTitles.put("error", "Error Occurred On This Page");
        internalPageTitles.put("login", "Login");
    }

    public void renderInternalPage(QuickWCMURL url, HttpServletRequest request, HttpServletResponse response) {
        String tmpFileName = FileUtils.getNewTmpFile() + ".html";
        try {
            Page rootPage = pageDAO.getPagesTree(draftOrPublishedView.isDraft());
            pageDAO.populateChildren(rootPage, draftOrPublishedView.isDraft());
            PrintWriter out = new PrintWriter(new FileOutputStream(tmpFileName));
            VelocityContext context = new VelocityContext();
            context.put("qwcmPrintWriter", out);
            context.put("httpRequest", request);
            context.put("httpResponse", response);
            context.put("qwcmPageTitle", internalPageTitles.get(url.getPageId()));
            context.put("qwcmVelocityPageContext", context);
            context.put("qwcmContextPath", url.getContextPath());
            context.put("qwcmURL", url);
            context.put("qwcmRootPage", rootPage);
            if (internalException != null) {
                context.put("qwcmInternalException", ExceptionUtils.renderException(internalException));
            }
            try {
                Velocity.getTemplate("themes/default/main.vm").merge(context, out);
            } finally {
                out.close();
            }
            dumpFile(tmpFileName, response);
        } catch (Exception e) {
            e.printStackTrace();
            QuickWCMURL errorUrl = urlServiceProvider.get().getBasicURL(request);
            errorUrl.setInternal(true);
            errorUrl.setPageId("error");
            try {
                response.sendRedirect(urlServiceProvider.get().render(errorUrl));
            } catch (Exception sube) {
                throw new RuntimeException(sube.getMessage(), sube);
            }
        } finally {
            FileUtils.deleteTmpFile(tmpFileName);
        }
    }

    private void dumpFile(String file, HttpServletResponse response) throws Exception {
        FileInputStream fis = new FileInputStream(file);
        PrintWriter responseOut = response.getWriter();
        byte[] buff = new byte[10240];
        int read;
        while ((read = fis.read(buff)) > 0) {
            responseOut.write(new String(buff), 0, read);
        }
        fis.close();
    }

    private List<Content> checkContentSecurity(List<Content> contents) {
        List<Content> res = new ArrayList<Content>();
        for (Content content : contents) {
            if (securityProvider.isAllowed(content.getPermissions(), Content.CONTENT_VIEW)) {
                res.add(content);
            }
        }
        return res;
    }
}
