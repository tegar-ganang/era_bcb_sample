package com.jhyle.sce;

import java.io.File;
import java.io.FileInputStream;
import java.text.ParseException;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.jmimemagic.Magic;
import net.sf.jmimemagic.MagicMatch;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import com.jhyle.sce.dao.Hibernate;
import com.jhyle.sce.entity.Article;

public abstract class Rest extends Controller {

    private static final int HITS_PER_PAGE = 100;

    public Rest(Config config, Hibernate orm) {
        super(config, orm);
    }

    @Override
    public void view(HttpServletRequest request, HttpServletResponse response) throws Exception {
        boolean found = false;
        String name = getArgument(request.getPathInfo());
        if (StringUtils.contains(name, '/')) {
            File file = new File(config.getProperty(Config.MULTIMEDIA_PATH) + Config.FILE_SEPARATOR + name);
            if (file.exists() && file.isFile()) {
                found = true;
                MagicMatch match = Magic.getMagicMatch(file, true);
                response.setContentType(match.getMimeType());
                FileInputStream in = new FileInputStream(file);
                IOUtils.copyLarge(in, response.getOutputStream());
                in.close();
            }
        } else if (!StringUtils.isBlank(name)) {
            int articleId = NumberUtils.toInt(name);
            if (articleId > 0) {
                Article article = articleDao.load(articleId);
                if (article != null) {
                    found = true;
                    sendArticle(request, response, article);
                }
            }
        } else {
            int page = NumberUtils.toInt(request.getParameter("page"), 0);
            Date fromDate = null;
            String from = request.getParameter("from");
            if (StringUtils.isNotBlank(from)) {
                try {
                    fromDate = dayMonthYearEn.parse(from);
                } catch (ParseException e) {
                }
            }
            Date untilDate = null;
            String until = request.getParameter("until");
            if (StringUtils.isNotBlank(until)) {
                try {
                    untilDate = dayMonthYearEn.parse(until);
                } catch (ParseException e) {
                }
            }
            sendArticleList(request, response, articleDao.list(request.getParameter("query"), request.getParameter("author"), request.getParameter("tags"), request.getParameterValues("types"), fromDate, untilDate, page, HITS_PER_PAGE, null));
            found = true;
        }
        if (found != true) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    protected abstract void sendArticle(HttpServletRequest request, HttpServletResponse response, Article article) throws Exception;

    protected abstract void sendArticleList(HttpServletRequest request, HttpServletResponse response, com.jhyle.sce.dao.ArticleList list) throws Exception;
}
