package com.dcivision.framework.taglib.channel;

import java.util.List;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.dcivision.framework.web.AbstractSearchForm;

public class AjaxChannelProcessor extends AbstractAjaxProcessor {

    private static final Log log = LogFactory.getLog(AjaxChannelProcessor.class);

    protected void initPageContext(ServletContext servletContext, ServletConfig servletConfig, HttpServletRequest request, HttpServletResponse response, HttpSession session, String filterName, String filterItem) throws Exception {
        super.initPageContext(servletContext, servletConfig, request, response, session);
        if (threadContext.getRequestAttribute(AjaxConstant.AJAXTAGFACTORY) == null) {
            threadContext.setRequestAttribute(AjaxConstant.AJAXTAGFACTORY, AjaxTagFactory.getInstance());
        }
        threadContext.setRequestAttribute(AjaxConstant.FILTERNAME, filterName);
        threadContext.setRequestAttribute(AjaxConstant.FILTERITEM, filterItem);
    }

    private AbstractSearchForm getCurrentForm(String filterName, int currentPage, int pageSize) {
        AbstractSearchForm form = AjaxFormAdapter.getChannelAdapterForm(filterName);
        form.setCurStartRowNo("" + currentPage);
        form.setPageOffset("" + pageSize);
        return form;
    }

    public String[] getChannelListByFilterName(ServletContext servletContext, ServletConfig servletConfig, HttpServletRequest request, HttpServletResponse response, HttpSession session, int currentPage, int pageSize, String filterName, String filterItem) {
        List channelList = null;
        String[] contents = new String[5];
        try {
            AjaxChannelListProcessor channelListProcessor = AjaxChannelListProcessor.getAjaxListProcessor();
            AbstractSearchForm form = this.getCurrentForm(filterName, currentPage, pageSize);
            initPageContext(servletContext, servletConfig, request, response, session, filterName, filterItem);
            channelList = channelListProcessor.getChannelList(threadContext, form);
            String[] content = AjaxChannelFormatterAdapter.getAjaxChannelFormatter(threadContext.getPageContext(), pageSize, channelList, filterName).getContent();
            contents[0] = filterName;
            contents[1] = content[0];
            contents[2] = channelList.size() + "";
            contents[3] = content[1];
            contents[4] = content[2];
        } catch (Exception ex) {
            log.error(ex, ex);
        } finally {
            try {
                threadContext.closeConnection();
                channelList = null;
            } catch (Exception e) {
                log.error(e, e);
            }
        }
        return contents;
    }
}
