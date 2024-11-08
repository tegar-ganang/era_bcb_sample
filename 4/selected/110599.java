package com.adpython.controllers;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import com.adpython.domain.Article;
import com.adpython.domain.Channel;
import com.adpython.domain.Users;
import com.adpython.service.ArticleService;
import com.adpython.service.ChannelService;
import com.adpython.service.UsersService;
import com.adpython.utils.StrUtil;
import com.google.appengine.api.datastore.Text;

public class ManageController extends BaseMultiController {

    private ChannelService channelService;

    private UsersService usersService;

    private ArticleService articleService;

    public void manageProxy(HttpServletRequest req, HttpServletResponse res, Map rsmap) {
        if (!rsmap.containsKey("user")) {
            Cookie cookie = StrUtil.getCookie(req, "nibiru.adpython.auth");
            if (cookie != null) {
                String username = cookie.getValue();
                Users user = usersService.queryByMail(username);
                rsmap.put("user", user);
            }
        }
    }

    public ModelAndView center(HttpServletRequest req, HttpServletResponse res) {
        Map rsmap = new HashMap();
        this.manageProxy(req, res, rsmap);
        return new ModelAndView("manage/center", rsmap);
    }

    public ModelAndView postarticle(HttpServletRequest req, HttpServletResponse res) {
        Map rsmap = new HashMap();
        this.manageProxy(req, res, rsmap);
        List<Channel> list = channelService.queryAllChannel();
        rsmap.put("channelList", list);
        String articleid = req.getParameter("articleid");
        if (!StrUtil.empty(articleid)) {
            Long id = (long) StrUtil.parseInt(articleid, -1);
            Article article = articleService.getArticleById(id);
            rsmap.put("article", article);
        }
        return new ModelAndView("manage/postarticle", rsmap);
    }

    public ModelAndView submitArticle(HttpServletRequest req, HttpServletResponse res) {
        Map rsmap = new HashMap();
        this.manageProxy(req, res, rsmap);
        String title = req.getParameter("title");
        String channelId = req.getParameter("channelId");
        String content = req.getParameter("content");
        if (!StrUtil.empty(title) && !StrUtil.empty(channelId) && !StrUtil.empty(content)) {
            Long chid = (long) StrUtil.parseInt(channelId);
            Users user = (Users) rsmap.get("user");
            Text text = new Text(content);
            Article article = null;
            String articleid = req.getParameter("articleId");
            if (!StrUtil.empty(articleid)) {
                Long id = (long) StrUtil.parseInt(articleid, -1);
                article = articleService.getArticleById(id);
                if (article != null) {
                    article.setTitle(title);
                    article.setContent(text);
                    article.setChannelId(chid);
                    article.setUpdateTime(new Date());
                    articleService.updateArticle(article);
                }
            }
            if (article == null) {
                article = new Article(title, text, chid);
                article.setCreateTime(new Date());
                article.setCreateUserId(user.getId());
                article.setUpdateTime(new Date());
                article.setIfShow(true);
                article.setIsDeleted(false);
                article.setRank(100);
                articleService.saveArticle(article);
            }
            Channel channel = channelService.getChannelById(chid);
            rsmap.put("channel", channel);
            rsmap.put("result", "isPosted");
        } else {
            rsmap.put("result", "notPosted");
        }
        return new ModelAndView("manage/result", rsmap);
    }

    public void setUsersService(UsersService usersService) {
        this.usersService = usersService;
    }

    public UsersService getUsersService() {
        return this.usersService;
    }

    public void setChannelService(ChannelService channelService) {
        this.channelService = channelService;
    }

    public ChannelService getChannelService() {
        return this.channelService;
    }

    public void setArticleService(ArticleService articleService) {
        this.articleService = articleService;
    }

    public ArticleService getArticleService() {
        return this.articleService;
    }
}
