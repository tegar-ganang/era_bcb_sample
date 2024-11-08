package com.spring.rssReader.web;

import com.spring.rssReader.*;
import com.spring.rssReader.jdbc.IChannelController;
import com.spring.rssReader.lucene.ISearchIndex;
import com.spring.rssReader.lucene.SearchItemVisitor;
import com.spring.rssReader.validator.ChannelValidator;
import com.spring.workflow.WorkflowConstants;
import com.spring.workflow.exception.ResponseUsedException;
import com.spring.workflow.parser.PageDefinition;
import com.spring.workflow.util.StringUtils;
import com.roha.xmlparsing.GeneralPullHandler;
import com.roha.xmlparsing.XMLElement;
import org.apache.lucene.queryParser.ParseException;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.context.ApplicationContextException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.BindUtils;
import org.springframework.web.bind.RequestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.DefaultMultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.*;
import java.lang.reflect.InvocationTargetException;

/**
 * @author Ronald Date: Dec 1, 2003 Time: 12:44:05 PM
 */
public class ChannelWebController extends MultiActionController {

    public IChannelController channelController = null;

    private ISearchIndex searchIndex;

    public static final String ACTIVE_CHANNEL = "selectedChannel";

    protected ICategoryDao categoryDao;

    public ChannelWebController() throws ApplicationContextException {
        super();
    }

    public IChannelController getChannelController() {
        return channelController;
    }

    public void setChannelController(IChannelController channelController) {
        this.channelController = channelController;
    }

    public ISearchIndex getSearchIndex() {
        return searchIndex;
    }

    public void setSearchIndex(ISearchIndex searchIndex) {
        this.searchIndex = searchIndex;
    }

    public void setCategoryDao(ICategoryDao categoryDao) {
        this.categoryDao = categoryDao;
    }

    public ModelAndView getArticles(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        ModelAndView view = new ModelAndView("");
        view.addObject("articles", channelController.getArticles());
        request.getSession().setAttribute("current", "getArticles");
        request.getSession().setAttribute(WorkflowConstants.RESPONSE_PAGE, request.getAttribute(WorkflowConstants.RESPONSE_PAGE));
        return view;
    }

    public ModelAndView getFavourites(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        ModelAndView view = new ModelAndView("");
        view.addObject("articles", channelController.getFavourites());
        request.getSession().setAttribute("current", "getFavourites");
        request.getSession().setAttribute(WorkflowConstants.RESPONSE_PAGE, request.getAttribute(WorkflowConstants.RESPONSE_PAGE));
        return view;
    }

    public ModelAndView getChannelsWithNews(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        ModelAndView view = new ModelAndView("");
        view.addObject("articles", channelController.getChannelsToRead());
        request.getSession().setAttribute("current", "getChannelsWithNews");
        request.getSession().setAttribute(WorkflowConstants.RESPONSE_PAGE, request.getAttribute(WorkflowConstants.RESPONSE_PAGE));
        return view;
    }

    public ModelAndView getChannelsWithNoNews(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        ModelAndView view = new ModelAndView("");
        view.addObject("articles", channelController.getNoNewsChannels());
        request.getSession().setAttribute("current", "getChannelsWithNoNews");
        request.getSession().setAttribute(WorkflowConstants.RESPONSE_PAGE, request.getAttribute(WorkflowConstants.RESPONSE_PAGE));
        return view;
    }

    public ModelAndView getEmptyChannels(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        ModelAndView view = new ModelAndView("");
        view.addObject("articles", channelController.getEmptyChannels());
        request.getSession().setAttribute("current", "getEmptyChannels");
        request.getSession().setAttribute(WorkflowConstants.RESPONSE_PAGE, request.getAttribute(WorkflowConstants.RESPONSE_PAGE));
        return view;
    }

    public ModelAndView getCategorizedChannels(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        long categoryId = RequestUtils.getLongParameter(request, WorkflowConstants.ID_NAME, -1);
        ModelAndView view = new ModelAndView("");
        view.addObject("categories", categoryDao.load());
        if (categoryId != -1) {
            ICategory category = categoryDao.load(new Long(categoryId));
            view.addObject("articles", channelController.getCategorizedChannels(category));
            view.addObject("selectedCategory", category.getName());
        } else {
            view.addObject("articles", channelController.getCategorizedChannels(null));
            view.addObject("selectedCategory", "");
        }
        request.getSession().setAttribute("current", "getCategorizedChannels");
        request.getSession().setAttribute(WorkflowConstants.RESPONSE_PAGE, request.getAttribute(WorkflowConstants.RESPONSE_PAGE));
        return view;
    }

    public ModelAndView markAsRead(HttpServletRequest request, HttpServletResponse response, Object command) throws ServletException {
        channelController.markAsRead(new Long(RequestUtils.getRequiredLongParameter(request, WorkflowConstants.ID_NAME)));
        return getCurrent(request);
    }

    /**
	 * This method is used to retrieve the current view. This specific method (with request and response) will be used if
	 * there is a method getCurrent defined in the workflow.xml. The getCurrent with only a request will return the current
	 * view in a ModelAndView and thus doesnt need a specific actionClass in the workflow.xml. Both are here as examples.
	 *
	 * @param request
	 * @param response
	 * @return
	 * @throws ServletException
	 */
    public ModelAndView getCurrent(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        String page = ((PageDefinition) request.getSession().getAttribute(WorkflowConstants.RESPONSE_PAGE)).getFullName();
        return new ModelAndView("forward:" + page + "/" + request.getSession().getAttribute("current"));
    }

    /**
	 * At the end of a method, this method can be called to return the lastly used view. View in this case is defined as
	 * the last database centric view (e.g. articles or favourites or channels with news)
	 *
	 * @param request
	 * @return
	 * @throws ServletException
	 */
    public ModelAndView getCurrent(HttpServletRequest request) throws ServletException {
        if (request.getSession().getAttribute(WorkflowConstants.RESPONSE_PAGE) == null) {
            return new ModelAndView("");
        }
        String page = ((PageDefinition) request.getSession().getAttribute(WorkflowConstants.RESPONSE_PAGE)).getFullName();
        return new ModelAndView("forward:" + page + "/" + request.getSession().getAttribute("current"));
    }

    public ModelAndView dontPollAnymore(HttpServletRequest request, HttpServletResponse response, Object command) throws ServletException {
        channelController.removeChannel(new Long(RequestUtils.getRequiredLongParameter(request, WorkflowConstants.ID_NAME)));
        return getCurrent(request);
    }

    public ModelAndView setActiveChannel(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        Channel channel = channelController.getChannel(new Long(RequestUtils.getRequiredLongParameter(request, WorkflowConstants.ID_NAME)));
        BindException exception = BindUtils.bind(request, channel, "channel");
        Map model = exception.getModel();
        ModelAndView view = new ModelAndView("", model);
        view.addObject("channel", channel);
        request.getSession().setAttribute(ACTIVE_CHANNEL, channel);
        return view;
    }

    public ModelAndView searchChannelUrl(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, ParseException {
        ModelAndView view = new ModelAndView("articles");
        if (request.getParameter("searchObject") != null) {
            String typeSearch = request.getParameter("searchType");
            if (typeSearch.equals("url")) {
                view.addObject("articles", channelController.getChannelsLikeUrl(request.getParameter("searchObject")));
            } else if (typeSearch.equals("title")) {
                view.addObject("articles", channelController.getChannelsLikeTitle(request.getParameter("searchObject")));
            } else if (typeSearch.equals("item")) {
                return new ModelAndView("forward:rssReader/overview/searchResults");
            }
            return view;
        }
        return getCurrent(request);
    }

    public ModelAndView setActiveNewChannel(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        ModelAndView view = new ModelAndView("");
        Channel channel = new Channel();
        view.addObject("channel", channel);
        request.getSession().setAttribute(ACTIVE_CHANNEL, channel);
        return view;
    }

    public ModelAndView save(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        Channel channel = (Channel) request.getSession().getAttribute(ACTIVE_CHANNEL);
        if (channel == null) {
            ModelAndView current = getCurrent(request);
            current.addObject("message", "Dont use back button please");
            return current;
        }
        BindException exception = BindUtils.bind(request, channel, "channel");
        ChannelValidator validator = new ChannelValidator();
        validator.setChannelController(channelController);
        validator.validate(channel, exception);
        if (exception.hasErrors()) {
            return new ModelAndView("forward:", exception.getModel());
        }
        channelController.update(channel);
        request.getSession().removeAttribute(ACTIVE_CHANNEL);
        return new ModelAndView("");
    }

    public ModelAndView pollChannel(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        long id = RequestUtils.getLongParameter(request, WorkflowConstants.ID_NAME, -1);
        Channel channel = null;
        if (id == -1) {
            channel = (Channel) request.getSession().getAttribute(ACTIVE_CHANNEL);
            if (channel == null) {
                ModelAndView current = getCurrent(request);
                current.addObject("message", "error.backbutton");
                return current;
            }
        } else {
            channel = channelController.getChannel(new Long(id));
        }
        channelController.pollChannel(channel);
        return new ModelAndView("");
    }

    public ModelAndView pollAllChannels(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        List allChannelsToPoll = channelController.getChannelsToPoll();
        for (int i = 0; i < allChannelsToPoll.size(); i++) {
            Channel channel = (Channel) allChannelsToPoll.get(i);
            channelController.pollChannel(channel);
        }
        return getCurrent(request);
    }

    /**
     * This method will remove the channel id from the session after the user presses on cancel or on new.
     * @param request
     * @param response
     * @return
     * @throws ServletException
     */
    public ModelAndView emptyChannelSession(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        request.getSession().removeAttribute(EditChannel.CHANNEL_FOR_EDIT);
        return new ModelAndView("");
    }

    /**
	 * This method will set the active channel's preferance to the specified value.
	 *
	 * @param request
	 * @param response
	 * @return
	 * @throws ServletException
	 */
    public ModelAndView setHotItem(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        Channel channel = this.getActiveChannel(request);
        if (channel == null) {
            Map model = new HashMap();
            model.put("message", "Dont use back button please");
            return new ModelAndView("forward:", model);
        }
        int preferance = RequestUtils.getIntParameter(request, "preferance", -1);
        if (preferance != -1) {
            channel.setPreferance(preferance);
            channelController.update(channel);
            response.setStatus(204);
            throw new ResponseUsedException();
        }
        return getCurrent(request);
    }

    /**
	 * This method will import the opml url or file or xml, based on the given parameter.
	 *
	 * @param request
	 * @param response
	 * @return
	 * @throws ServletException
	 */
    public ModelAndView importOPML(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String xmlDefinitions = null;
        if (request.getParameter("opmlURL") != null && !request.getParameter("opmlURL").trim().equals("")) {
            GeneralItem item = new GeneralItem();
            item.setUrl(request.getParameter("opmlURL"));
            xmlDefinitions = item.load();
        } else if (request.getParameter("opmlTextarea") != null && !request.getParameter("opmlTextarea").trim().equals("")) {
            xmlDefinitions = request.getParameter("opmlTextarea");
        } else if (request instanceof DefaultMultipartHttpServletRequest) {
            DefaultMultipartHttpServletRequest multiRequest = (DefaultMultipartHttpServletRequest) request;
            if (multiRequest.getFile("opmlFILE") != null) {
                MultipartFile file = multiRequest.getFile("opmlFILE");
                xmlDefinitions = new String(file.getBytes());
            }
        }
        int numberOfImports = 0;
        int numberOfNew = 0;
        try {
            GeneralPullHandler handler = new GeneralPullHandler();
            XMLElement root = handler.parse(new StringReader(xmlDefinitions));
            String urlName = "xmlUrl";
            if (request.getParameter("xmlUrlName") != null && !request.getParameter("xmlUrlName").trim().equals("")) {
                urlName = request.getParameter("xmlUrlName");
            }
            if (root != null) {
                PrintWriter out = response.getWriter();
                response.setContentType("text/html");
                Iterator elementIterator = root.getElementsIterator("outline");
                while (elementIterator.hasNext()) {
                    numberOfImports++;
                    XMLElement xmlElement = (XMLElement) elementIterator.next();
                    if (xmlElement.getAttribute(urlName) != null) {
                        Channel channel = new Channel();
                        channel.setUrl((String) xmlElement.getAttribute(urlName));
                        if (channelController.isUniqueUrl(channel)) {
                            if (xmlElement.getAttribute("title") != null) {
                                channel.setTitle((String) xmlElement.getAttribute("title"));
                            } else {
                                channel.setTitle("to be polled");
                            }
                            out.println("Starting to poll channel (" + channel.getTitle() + ")<br>");
                            channelController.update(channel);
                            numberOfNew++;
                            channelController.pollChannel(channel);
                            out.println("&nbsp;&nbsp;&nbsp;Done polling channel (" + channel.getTitle() + ")<br>");
                            out.flush();
                        }
                    }
                }
                out.println("Finished importing " + numberOfImports + " channels. Found " + numberOfNew + " new channels.");
                out.close();
                throw new ResponseUsedException();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ModelAndView("");
    }

    public ModelAndView exportOPML(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        List channels = channelController.getChannelsToPoll();
        StringBuffer sb = new StringBuffer();
        sb.append("&lt;opml version=\"1.1\"&gt;<br>\n" + "&nbsp;&nbsp;&nbsp;&lt;head&gt;<br>\n" + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;title&gt;rssReader Export&lt;/title&gt;<br>\n" + "&nbsp;&nbsp;&nbsp;&lt;/head&gt;<br>\n" + "&lt;body&gt;<br>\n");
        for (int i = 0; i < channels.size(); i++) {
            Channel channel = (Channel) channels.get(i);
            sb.append("&nbsp;&nbsp;&nbsp;&lt;outline");
            sb.append(" text=\"").append(StringUtils.replaceForXML(channel.getTitle())).append("\"");
            sb.append(" desctiption=\"").append(StringUtils.replaceForXML(channel.getDescription())).append("\"");
            sb.append(" language=\"").append(StringUtils.replaceForXML(channel.getLanguage())).append("\"");
            sb.append(" title=\"").append(StringUtils.replaceForXML(channel.getTitle())).append("\"");
            sb.append(" xmlUrl=\"").append(StringUtils.replaceForXML(channel.getUrl())).append("\"");
            sb.append("/&gt;<br>");
        }
        sb.append("&lt;/body&gt;<br>&lt;/opml&gt;");
        return new ModelAndView("", "exportOPML", sb.toString());
    }

    /**
	 * This method will try to get the active channel from the request. If it isnt found then it is assumed that the
	 * browser back button was used. This will result in a exception.
	 *
	 * @param request
	 * @return
	 * @throws ServletException
	 */
    private Channel getActiveChannel(HttpServletRequest request) {
        return (Channel) request.getSession().getAttribute(ACTIVE_CHANNEL);
    }

    /**
	 * This method will create the indexes as needed by the Lucene search engine. Each item that isnt deleted, will be
	 * added.
	 *
	 * @param request
	 * @param response
	 * @return
	 * @throws ServletException
	 */
    public ModelAndView indexItems(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        List channels = this.channelController.getChannelsToPoll();
        channels.addAll(this.channelController.getArticles());
        searchIndex.createIndexWriter();
        for (int i = 0; i < channels.size(); i++) {
            Channel channel = (Channel) channels.get(i);
            if (channel.isRemove()) {
                continue;
            }
            List items = channelController.getAllItems(channel.getId());
            for (int j = 0; j < items.size(); j++) {
                Item item = (Item) items.get(j);
                if (item.isRemove()) {
                    continue;
                }
                item.setPreferance(item.getPreferance() + channel.getPreferance());
                searchIndex.insert(new SearchItemVisitor(item));
            }
        }
        searchIndex.optimize();
        searchIndex.close();
        return new ModelAndView("");
    }

    public ModelAndView searchResults(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, ParseException {
        List items = new ArrayList();
        String searchFor = request.getParameter("searchObject");
        ModelAndView modelAndView = new ModelAndView("");
        if (searchFor != null) {
            List itemIds = searchIndex.search(new SearchItemVisitor(), searchFor);
            for (int i = 0; i < itemIds.size(); i++) {
                Item item = channelController.getItem(new Long((String) itemIds.get(i)));
                if (!item.isRemove()) {
                    items.add(item);
                }
            }
        }
        Collections.sort(items, new Comparator() {

            public int compare(Object o1, Object o2) {
                if (o1 instanceof IItem && o2 instanceof IItem) {
                    return ((Item) o2).getPreferance() - ((Item) o1).getPreferance();
                }
                return 0;
            }
        });
        modelAndView.addObject("items", items);
        if (items.size() == 0) {
            modelAndView.addObject("message", "error.nothingFound");
        }
        modelAndView.addObject("searchObject", request.getParameter("searchObject"));
        modelAndView.addObject("searchType", request.getParameter("searchType"));
        return modelAndView;
    }

    /**
     * This method should be called only once after an update. This will re-poll all channels that have a status code
     * that isnt equal to 204. This is done, since the new version of the rssReader will now follow redirects.
     * @param request
     * @param response
     * @return
     * @throws ServletException
     */
    public ModelAndView pollRedirected(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        List channels = channelController.getChannels("redirectQueries");
        for (int i = 0; i < channels.size(); i++) {
            Channel channel = (Channel) channels.get(i);
            channelController.pollChannel(channel);
        }
        return new ModelAndView("");
    }

    public ModelAndView convert(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        return new ModelAndView("");
    }

    public void destroy() {
    }
}
