package com.bocoon.app.cms.front;

import static com.bocoon.app.cms.Constants.TPLDIR_INDEX;
import static com.bocoon.common.web.Constants.INDEX;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import com.bocoon.app.cms.utils.CmsUtils;
import com.bocoon.app.cms.utils.FrontUtils;
import com.bocoon.common.page.Paginable;
import com.bocoon.common.page.SimplePage;
import com.bocoon.entity.cms.main.Channel;
import com.bocoon.entity.cms.main.CmsGroup;
import com.bocoon.entity.cms.main.CmsSite;
import com.bocoon.entity.cms.main.CmsUser;
import com.bocoon.entity.cms.main.Content;
import com.jeecms.cms.manager.assist.CmsKeywordMng;
import com.jeecms.cms.manager.main.ChannelMng;
import com.jeecms.cms.manager.main.ContentMng;
import com.jeecms.core.web.front.URLHelper;
import com.jeecms.core.web.front.URLHelper.PageInfo;

@Controller
public class DynamicPageAct {

    private static final Logger log = LoggerFactory.getLogger(DynamicPageAct.class);

    /**
	 * 首页模板名称
	 */
    public static final String TPL_INDEX = "tpl.index";

    public static final String GROUP_FORBIDDEN = "login.groupAccessForbidden";

    /**
	 * TOMCAT的默认路径
	 * 
	 * @param request
	 * @param model
	 * @return
	 */
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String index(HttpServletRequest request, ModelMap model) {
        CmsSite site = CmsUtils.getSite(request);
        FrontUtils.frontData(request, model, site);
        return FrontUtils.getTplPath(request, site.getSolutionPath(), TPLDIR_INDEX, TPL_INDEX);
    }

    /**
	 * WEBLOGIC的默认路径
	 * 
	 * @param request
	 * @param model
	 * @return
	 */
    @RequestMapping(value = "/index.jhtml", method = RequestMethod.GET)
    public String indexForWeblogic(HttpServletRequest request, ModelMap model) {
        return index(request, model);
    }

    /**
	 * 动态页入口
	 */
    @RequestMapping(value = "/**/*.*", method = RequestMethod.GET)
    public String dynamic(HttpServletRequest request, HttpServletResponse response, ModelMap model) {
        int pageNo = URLHelper.getPageNo(request);
        String[] params = URLHelper.getParams(request);
        PageInfo info = URLHelper.getPageInfo(request);
        String[] paths = URLHelper.getPaths(request);
        int len = paths.length;
        if (len == 1) {
            return channel(paths[0], pageNo, params, info, request, response, model);
        } else if (len == 2) {
            if (paths[1].equals(INDEX)) {
                return channel(paths[0], pageNo, params, info, request, response, model);
            } else {
                try {
                    Integer id = Integer.parseInt(paths[1]);
                    return content(id, pageNo, params, info, request, response, model);
                } catch (NumberFormatException e) {
                    log.debug("Content id must String: {}", paths[1]);
                    return FrontUtils.pageNotFound(request, response, model);
                }
            }
        } else {
            log.debug("Illegal path length: {}, paths: {}", len, paths);
            return FrontUtils.pageNotFound(request, response, model);
        }
    }

    public String channel(String path, int pageNo, String[] params, PageInfo info, HttpServletRequest request, HttpServletResponse response, ModelMap model) {
        CmsSite site = CmsUtils.getSite(request);
        Channel channel = channelMng.findByPathForTag(path, site.getId());
        if (channel == null) {
            log.debug("Channel path not found: {}", path);
            return FrontUtils.pageNotFound(request, response, model);
        }
        model.addAttribute("channel", channel);
        FrontUtils.frontData(request, model, site);
        FrontUtils.frontPageData(request, model);
        return channel.getTplChannelOrDef();
    }

    public String content(Integer id, int pageNo, String[] params, PageInfo info, HttpServletRequest request, HttpServletResponse response, ModelMap model) {
        Content content = contentMng.findById(id);
        if (content == null) {
            log.debug("Content id not found: {}", id);
            return FrontUtils.pageNotFound(request, response, model);
        }
        CmsUser user = CmsUtils.getUser(request);
        CmsSite site = content.getSite();
        Set<CmsGroup> groups = content.getViewGroupsExt();
        int len = groups.size();
        if (len != 0) {
            if (user == null) {
                return FrontUtils.showLogin(request, model, site);
            }
            Integer gid = user.getGroup().getId();
            boolean right = false;
            for (CmsGroup group : groups) {
                if (group.getId().equals(gid)) {
                    right = true;
                    break;
                }
            }
            if (!right) {
                String gname = user.getGroup().getName();
                return FrontUtils.showMessage(request, model, GROUP_FORBIDDEN, gname);
            }
        }
        String txt = content.getTxtByNo(pageNo);
        txt = cmsKeywordMng.attachKeyword(site.getId(), txt);
        Paginable pagination = new SimplePage(pageNo, 1, content.getPageCount());
        model.addAttribute("pagination", pagination);
        FrontUtils.frontPageData(request, model);
        model.addAttribute("content", content);
        model.addAttribute("channel", content.getChannel());
        model.addAttribute("title", content.getTitleByNo(pageNo));
        model.addAttribute("txt", txt);
        model.addAttribute("pic", content.getPictureByNo(pageNo));
        FrontUtils.frontData(request, model, site);
        return content.getTplContentOrDef();
    }

    @Autowired
    private ChannelMng channelMng;

    @Autowired
    private ContentMng contentMng;

    @Autowired
    private CmsKeywordMng cmsKeywordMng;
}
