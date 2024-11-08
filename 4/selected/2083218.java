package com.adpython.controllers;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
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
import com.adpython.service.SeoService;
import com.adpython.service.UsersService;
import com.adpython.utils.StrUtil;

public class CommonController extends BaseMultiController {

    private ChannelService channelService;

    private SeoService seoService;

    private UsersService usersService;

    private ArticleService articleService;

    private static List<Channel> channelCache;

    public void userProxy(HttpServletRequest req, HttpServletResponse res, Map rsmap) {
        if (!rsmap.containsKey("user")) {
            Cookie cookie = StrUtil.getCookie(req, "nibiru.adpython.auth");
            if (cookie != null) {
                String username = cookie.getValue();
                Users user = usersService.queryByMail(username);
                rsmap.put("user", user);
            }
        }
    }

    public ModelAndView channelHandler(HttpServletRequest req, HttpServletResponse res) throws IOException {
        Map rsmap = new HashMap();
        this.userProxy(req, res, rsmap);
        String serverName = req.getServerName();
        if (serverName.equals("manage.adpython.com")) {
            res.sendRedirect("http://manage.adpython.com/manage.htm?method=center");
            return null;
        }
        String channelName = req.getParameter("channelName");
        if (channelCache == null) {
            List<Channel> list = channelService.queryAllChannel();
            channelCache = list;
        }
        String url = "/" + channelName + "/";
        Map seoMap = seoService.parseSeoRule(url);
        if (seoMap != null) rsmap.put("seoRule", seoMap);
        rsmap.put("author", "Adpython.com, Herr.Nibiru@gmail.com");
        rsmap.put("channelList", channelCache);
        rsmap.put("channelName", channelName);
        Channel channel = channelService.queryChannelByName(channelName);
        if (channel != null) {
            List<Article> articleList = articleService.getArticleByChannel(channel.getId());
            List<Article> retList = new LinkedList<Article>();
            int n = articleList.size();
            for (int i = n - 1; i >= 0; i--) {
                retList.add(articleList.get(i));
            }
            rsmap.put("articleList", retList);
        }
        return new ModelAndView("adpython/list", rsmap);
    }

    public ModelAndView initChannelDatas() {
        channelService.initData();
        return null;
    }

    public void setChannelService(ChannelService channelService) {
        this.channelService = channelService;
    }

    public ChannelService getChannelService() {
        return this.channelService;
    }

    public void setSeoService(SeoService seoService) {
        this.seoService = seoService;
    }

    public SeoService getSeoService() {
        return this.seoService;
    }

    public void setUsersService(UsersService usersService) {
        this.usersService = usersService;
    }

    public UsersService getUsersService() {
        return this.usersService;
    }

    public void setArticleService(ArticleService articleService) {
        this.articleService = articleService;
    }

    public ArticleService getArticleService() {
        return this.articleService;
    }
}
