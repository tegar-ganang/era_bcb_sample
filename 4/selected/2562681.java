package net.innig.imre.web.feed;

import net.innig.framework.web.tapestry.BareAbsoluteLinkRenderer;
import net.innig.framework.web.tapestry.InnigBasePage;
import net.innig.imre.domain.Article;
import net.innig.imre.domain.Category;
import net.innig.imre.domain.Version;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.tapestry.IExternalPage;
import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.link.ILinkRenderer;
import org.apache.tapestry.util.ContentType;

public abstract class FullArticlesRSS extends InnigBasePage implements IExternalPage {

    private static final ContentType CONTENT_TYPE = new ContentType("application/rss+xml");

    private static final DateFormat RSS_DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");

    private static final ILinkRenderer BARE_LINK_RENDERER = new BareAbsoluteLinkRenderer();

    @Override
    public ContentType getResponseContentType() {
        return CONTENT_TYPE;
    }

    public DateFormat getRssDateFormat() {
        return RSS_DATE_FORMAT;
    }

    public ILinkRenderer getBareLinkRenderer() {
        return BARE_LINK_RENDERER;
    }

    public void activateExternalPage(Object[] parameters, IRequestCycle cycle) {
        setChannelTitle("Channel");
        setChannelUrl("URL");
        setChannelDescription("Many fascinating things");
        setChannelCopyright("Test data knows no copyright.");
        setArticles(Article.findRecent(0, 10));
        setChannelPublicationDate(getArticles().isEmpty() ? null : getArticles().get(0).getCurrentVersion().getModificationTime());
    }

    public abstract String getChannelTitle();

    public abstract void setChannelTitle(String value);

    public abstract String getChannelUrl();

    public abstract void setChannelUrl(String value);

    public abstract String getChannelDescription();

    public abstract void setChannelDescription(String value);

    public String getChannelLanguage() {
        return "en-us";
    }

    public abstract String getChannelCopyright();

    public abstract void setChannelCopyright(String value);

    public abstract Date getChannelPublicationDate();

    public abstract void setChannelPublicationDate(Date value);

    public abstract List<Article> getArticles();

    public abstract void setArticles(List<Article> value);

    public void setCurrentArticle(Article article) {
        setArticle(article);
        Version currentVersion = article.getCurrentVersion();
        setArticleTitle(currentVersion.getTitle());
        setArticleUrl("foo");
        setArticleCommentsUrl("foo");
        setArticleCommentsRssUrl("foo");
        setArticlePublicationDate(currentVersion.getModificationTime());
        setArticleAuthor(currentVersion.getModifier().getDisplayName());
        List<String> categories = new ArrayList<String>();
        for (Category cat : currentVersion.getCategories()) categories.add(cat.getName());
        setArticleCategoryNames(categories);
        setArticleDescription(currentVersion.getBody());
        setArticleText(currentVersion.getBody());
    }

    public abstract Article getArticle();

    public abstract void setArticle(Article value);

    public abstract String getArticleTitle();

    public abstract void setArticleTitle(String value);

    public abstract String getArticleUrl();

    public abstract void setArticleUrl(String value);

    public abstract String getArticleCommentsUrl();

    public abstract void setArticleCommentsUrl(String value);

    public abstract String getArticleCommentsRssUrl();

    public abstract void setArticleCommentsRssUrl(String value);

    public abstract Date getArticlePublicationDate();

    public abstract void setArticlePublicationDate(Date value);

    public abstract String getArticleAuthor();

    public abstract void setArticleAuthor(String value);

    public abstract List<String> getArticleCategoryNames();

    public abstract void setArticleCategoryNames(List<String> value);

    public abstract String getCategoryName();

    public abstract void setCategoryName(String value);

    public abstract String getArticleDescription();

    public abstract void setArticleDescription(String value);

    public abstract String getArticleText();

    public abstract void setArticleText(String value);
}
