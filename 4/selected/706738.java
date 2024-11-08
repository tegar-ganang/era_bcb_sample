package de.d3web.we.jspwiki;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import com.ecyrd.jspwiki.PageManager;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.event.WikiEvent;
import com.ecyrd.jspwiki.event.WikiEventListener;
import com.ecyrd.jspwiki.event.WikiEventUtils;
import com.ecyrd.jspwiki.event.WikiPageEvent;
import com.ecyrd.jspwiki.event.WikiPageRenameEvent;
import com.ecyrd.jspwiki.filters.BasicPageFilter;
import com.ecyrd.jspwiki.filters.FilterException;
import com.ecyrd.jspwiki.plugin.PluginException;
import com.ecyrd.jspwiki.plugin.WikiPlugin;
import com.ecyrd.jspwiki.providers.ProviderException;
import com.ecyrd.jspwiki.ui.TemplateManager;
import de.d3web.we.core.KnowWEArticleManager;
import de.d3web.we.core.KnowWEEnvironment;
import de.d3web.we.core.KnowWERessourceLoader;
import de.d3web.we.kdom.KnowWEArticle;
import de.d3web.we.kdom.rendering.PageAppendHandler;
import de.d3web.we.user.UserContext;
import de.d3web.we.user.UserContextUtil;
import de.d3web.we.utils.KnowWEUtils;

public class KnowWEPlugin extends BasicPageFilter implements WikiPlugin, WikiEventListener {

    private static final String LEFT_MENU_FOOTER = "LeftMenuFooter";

    private static final String LEFT_MENU = "LeftMenu";

    private static final String MORE_MENU = "MoreMenu";

    /**
	 * To initialize KnowWE.
	 * 
	 * @see KnowWE_config.properties
	 */
    @Override
    public void initialize(WikiEngine engine, Properties properties) throws FilterException {
        super.initialize(engine, properties);
        m_engine = engine;
        initKnowWEEnvironmentIfNeeded(engine);
        ResourceBundle knowweconfig = ResourceBundle.getBundle("KnowWE_config");
        if (knowweconfig.getString("knowweplugin.jspwikiconnector.copycorepages").equals("false")) {
            WikiEventUtils.addWikiEventListener(engine.getPageManager(), WikiPageEvent.PAGE_DELETE_REQUEST, this);
            return;
        }
        File f = new File(KnowWEEnvironment.getInstance().getKnowWEExtensionPath());
        f = f.getParentFile();
        try {
            for (File s : f.listFiles()) {
                if (s.getName().equals("WEB-INF")) {
                    BufferedReader in = new BufferedReader(new FileReader(s.getPath() + "/jspwiki.properties"));
                    String zeile = null;
                    File pagedir = null;
                    while ((zeile = in.readLine()) != null) {
                        if (!zeile.contains("#") && zeile.contains("jspwiki.fileSystemProvider.pageDir")) {
                            zeile = zeile.trim();
                            zeile = zeile.substring(zeile.lastIndexOf(" ") + 1);
                            pagedir = new File(zeile);
                            in.close();
                            break;
                        }
                    }
                    if (pagedir.exists()) {
                        File[] files = pagedir.listFiles();
                        File coreDir = new File(s.getPath() + "/resources/core-pages");
                        File[] cores = coreDir.listFiles();
                        for (File cP : coreDir.listFiles()) {
                            if (!cP.getName().endsWith(".txt")) continue;
                            File newFile = new File(pagedir.getPath() + "/" + cP.getName());
                            if (!newFile.exists()) FileUtils.copyFile(cP, newFile);
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
        WikiEventUtils.addWikiEventListener(engine.getPageManager(), WikiPageEvent.PAGE_DELETE_REQUEST, this);
        WikiEventUtils.addWikiEventListener(engine.getPageManager(), WikiPageRenameEvent.PAGE_RENAMED, this);
    }

    private void initKnowWEEnvironmentIfNeeded(WikiEngine wEngine) {
        if (!KnowWEEnvironment.isInitialized()) {
            KnowWEEnvironment.initKnowWE(new JSPWikiKnowWEConnector(wEngine));
        }
    }

    @Override
    public String execute(WikiContext context, Map params) throws PluginException {
        try {
            if (context.getCommand().getRequestContext().equals(WikiContext.VIEW)) {
                initKnowWEEnvironmentIfNeeded(context.getEngine());
            }
        } catch (Exception t) {
            System.out.println("****Exception EXECUTE***");
            t.printStackTrace();
        }
        return "";
    }

    @Override
    public void postSave(WikiContext wikiContext, String content) throws FilterException {
        String topic = wikiContext.getPage().getName();
        String user = wikiContext.getWikiSession().getUserPrincipal().getName();
        initKnowWEEnvironmentIfNeeded(wikiContext.getEngine());
        KnowWEArticle article = KnowWEEnvironment.getInstance().buildAndRegisterArticle(content, topic, KnowWEEnvironment.DEFAULT_WEB);
        if (article != null) {
            String logEntry = topic + ", " + wikiContext.getRealPage().getVersion() + ", " + user + ", " + new Date().toString() + (article.isFullParse() ? ", fullparse " + article.getClassesCausingFullParse() : "") + "\n";
            KnowWEUtils.appendToFile(KnowWEUtils.getPageChangeLogPath(), logEntry);
        }
    }

    @Override
    public String postTranslate(WikiContext wikiContext, String htmlContent) throws FilterException {
        try {
            htmlContent = KnowWEUtils.unmaskNewline(htmlContent);
            htmlContent = KnowWEUtils.unmaskHTML(htmlContent);
            return htmlContent;
        } catch (Exception e) {
            System.out.println("****Exception in POST TRANSLATE***");
            System.out.println("****Exception in POST TRANSLATE***");
            System.out.println("****Exception in POST TRANSLATE***");
            e.printStackTrace();
            return "";
        }
    }

    @Override
    public String preTranslate(WikiContext wikiContext, String content) throws FilterException {
        initKnowWEEnvironmentIfNeeded(wikiContext.getEngine());
        initializeAllArticlesIfNeeded(wikiContext.getEngine());
        if (!wikiContext.getCommand().getRequestContext().equals(WikiContext.VIEW)) {
            return content;
        }
        JSPWikiUserContext userContext = new JSPWikiUserContext(wikiContext, UserContextUtil.getParameters(wikiContext.getHttpRequest()));
        String title = wikiContext.getRealPage().getName();
        List<String> supportArticleNames = Arrays.asList(MORE_MENU, LEFT_MENU, LEFT_MENU_FOOTER);
        if (supportArticleNames.contains(title)) {
            KnowWEArticle supportArticle = KnowWEEnvironment.getInstance().getArticle(KnowWEEnvironment.DEFAULT_WEB, title);
            if (supportArticle != null && supportArticle.getSection().getOriginalText().equals(content)) {
                return renderKDOM(content, userContext, supportArticle);
            }
        }
        WikiEngine engine = wikiContext.getEngine();
        String pureText = "";
        if (engine != null) {
            pureText = engine.getPureText(wikiContext.getPage().getName(), wikiContext.getPage().getVersion());
            if (!content.equals(pureText)) return content;
        }
        try {
            KnowWEArticle article = KnowWEEnvironment.getInstance().getArticle(KnowWEEnvironment.DEFAULT_WEB, title);
            String originalText = "";
            if (article != null) {
                originalText = article.getSection().getOriginalText();
            }
            String parse = userContext.getParameter("parse");
            boolean fullParse = parse != null && (parse.equals("full") || parse.equals("true"));
            if (fullParse || !originalText.equals(content)) {
                KnowWEEnvironment.getInstance().buildAndRegisterArticle(content, title, KnowWEEnvironment.DEFAULT_WEB, fullParse);
            }
            StringBuilder articleHTML = new StringBuilder();
            List<PageAppendHandler> appendhandlers = KnowWEEnvironment.getInstance().getAppendHandlers();
            for (PageAppendHandler pageAppendHandler : appendhandlers) {
                if (pageAppendHandler.isPre()) {
                    articleHTML.append(pageAppendHandler.getDataToAppend(title, KnowWEEnvironment.DEFAULT_WEB, userContext));
                }
            }
            article.getRenderer().render(article, article.getSection(), userContext, articleHTML);
            for (PageAppendHandler pageAppendHandler : appendhandlers) {
                if (!pageAppendHandler.isPre()) {
                    articleHTML.append(pageAppendHandler.getDataToAppend(title, KnowWEEnvironment.DEFAULT_WEB, userContext));
                }
            }
            includeDOMResources(wikiContext);
            return articleHTML.toString();
        } catch (Exception e) {
            System.out.println("*****EXCEPTION IN preTranslate !!! *********");
            System.out.println("*****EXCEPTION IN preTranslate !!! *********");
            System.out.println("*****EXCEPTION IN preTranslate !!! *********");
            e.printStackTrace();
            return null;
        }
    }

    /**
	 * Loads ALL articles stored in the pageDir (which is specified in
	 * jspwiki.properties).
	 * 
	 * @created 07.06.2010
	 * @param engine
	 */
    private void initializeAllArticlesIfNeeded(WikiEngine engine) {
        if (KnowWEEnvironment.getInstance().getArticleManager(KnowWEEnvironment.DEFAULT_WEB).areArticlesInitialized()) {
            return;
        }
        PageManager mgr = engine.getPageManager();
        Collection wikipages = null;
        try {
            wikipages = mgr.getAllPages();
        } catch (ProviderException e1) {
            Logger.getLogger(this.getClass()).warn("Unable to load all articles, maybe some articles won't be initialized!");
        }
        for (Object o : wikipages) {
            WikiPage wp = (WikiPage) o;
            KnowWEArticle article = KnowWEEnvironment.getInstance().getArticle(KnowWEEnvironment.DEFAULT_WEB, wp.getName());
            if (article == null) {
                String content = engine.getPureText(wp.getName(), wp.getVersion());
                KnowWEEnvironment.getInstance().buildAndRegisterArticle(content, wp.getName(), KnowWEEnvironment.DEFAULT_WEB);
            }
        }
        KnowWEEnvironment.getInstance().getArticleManager(KnowWEEnvironment.DEFAULT_WEB).setArticlesInitialized(true);
        KnowWEEnvironment.getInstance().getArticleManager(KnowWEEnvironment.DEFAULT_WEB).updateQueuedArticles();
    }

    private String renderKDOM(String content, UserContext userContext, KnowWEArticle article) {
        if (article != null) {
            StringBuilder articleString = new StringBuilder();
            article.getRenderer().render(article, article.getSection(), userContext, articleString);
            return articleString.toString();
        }
        return content + "\n(no KDOM)";
    }

    /**
	 * Handles events passed from JSPWiki
	 */
    @Override
    public void actionPerformed(WikiEvent event) {
        if ((event instanceof WikiPageEvent) && (event.getType() == WikiPageEvent.PAGE_DELETE_REQUEST)) {
            WikiPageEvent e = (WikiPageEvent) event;
            KnowWEArticleManager amgr = KnowWEEnvironment.getInstance().getArticleManager(KnowWEEnvironment.DEFAULT_WEB);
            amgr.deleteArticle(amgr.getArticle(e.getPageName()));
        } else if (event instanceof WikiPageRenameEvent) {
            WikiPageRenameEvent e = (WikiPageRenameEvent) event;
            KnowWEArticleManager amgr = KnowWEEnvironment.getInstance().getArticleManager(KnowWEEnvironment.DEFAULT_WEB);
            amgr.deleteArticle(amgr.getArticle(e.getOldPageName()));
        }
    }

    /**
	 * Adds the CSS and JS files to the current page.
	 * 
	 * @param wikiContext
	 */
    private void includeDOMResources(WikiContext wikiContext) {
        Object ctx = wikiContext.getVariable(TemplateManager.RESOURCE_INCLUDES);
        KnowWERessourceLoader loader = KnowWERessourceLoader.getInstance();
        List<String> script = loader.getScriptIncludes();
        for (String resource : script) {
            if (ctx != null && !ctx.toString().contains(resource)) {
                if (!resource.contains("://")) {
                    TemplateManager.addResourceRequest(wikiContext, KnowWERessourceLoader.RESOURCE_SCRIPT, KnowWERessourceLoader.defaultScript + resource);
                } else {
                    TemplateManager.addResourceRequest(wikiContext, KnowWERessourceLoader.RESOURCE_SCRIPT, resource);
                }
            } else if (ctx == null) {
                if (!resource.contains("://")) {
                    TemplateManager.addResourceRequest(wikiContext, KnowWERessourceLoader.RESOURCE_SCRIPT, KnowWERessourceLoader.defaultScript + resource);
                } else {
                    TemplateManager.addResourceRequest(wikiContext, KnowWERessourceLoader.RESOURCE_SCRIPT, resource);
                }
            }
        }
        List<String> css = loader.getStylesheetIncludes();
        for (String resource : css) {
            if (ctx != null && !ctx.toString().contains(resource)) {
                TemplateManager.addResourceRequest(wikiContext, KnowWERessourceLoader.RESOURCE_STYLESHEET, KnowWERessourceLoader.defaultStylesheet + resource);
            } else if (ctx == null) {
                TemplateManager.addResourceRequest(wikiContext, KnowWERessourceLoader.RESOURCE_STYLESHEET, KnowWERessourceLoader.defaultStylesheet + resource);
            }
        }
    }
}
