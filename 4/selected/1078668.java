package kr.pe.javarss.web;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import kr.pe.javarss.ChannelRepository;
import kr.pe.javarss.dao.FeedDao;
import kr.pe.javarss.model.Category;
import kr.pe.javarss.model.Feed;
import kr.pe.javarss.util.CommonUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import de.nava.informa.core.ChannelIF;

/**
 * 카테고리별로 JAVARSS 컨텐츠를 제공하기 위한 컨트롤러.
 *
 * TODO javarss.jsp에서 렌더링 로직을 어떻게 빼낼 것인가.
 *
 */
public class JavarssController implements Controller {

    protected FeedDao feedDao;

    protected ChannelRepository repository;

    public void setRepository(ChannelRepository service) {
        this.repository = service;
    }

    public void setFeedDao(FeedDao feedManager) {
        this.feedDao = feedManager;
    }

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String categoryId = request.getParameter("category");
        Category category = null;
        if (StringUtils.isNotEmpty(categoryId)) {
            category = feedDao.getCategory(categoryId);
        }
        if (category == null) {
            category = repository.getDefaultCategory();
        }
        SortedMap<Feed, ChannelIF> channelMap = repository.getChannelMap(category);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("channelMap", channelMap);
        map.put("categoryId", category.getId());
        map.put("categories", repository.getCategories());
        map.put("mobile", CommonUtils.isMobile(request));
        return new ModelAndView("javarss", map);
    }
}
