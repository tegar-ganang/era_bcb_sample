package com.dcivision.framework.taglib.channel;

import java.util.Iterator;
import java.util.List;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyTagSupport;
import org.apache.struts.util.ResponseUtils;
import com.dcivision.framework.SystemParameterConstant;
import com.dcivision.framework.SystemParameterFactory;
import com.dcivision.framework.Utility;

public class ChannelTag extends BodyTagSupport {

    private String icon = null;

    private String label = null;

    private String link = null;

    private String filterName = null;

    private String defaultFilterItem = null;

    private String isShowButton = "false";

    private int pageSize = 5;

    private long updateReq = Long.parseLong(SystemParameterFactory.getSystemParameter(SystemParameterConstant.CHANNEL_INTERVAL));

    private boolean isAjax = true;

    private int screenSize = 1024;

    private String enabledChannelStr = "";

    private AbstractChannelTagFormatter tagFormatter = null;

    /**
   *  Method doStartTag() - the default method called for <layout:pagerInfo>; It retrieves the PagerTag
   *                        and format the corresponging items range for display.
   *
   *  @param      No pass in parameter
   *  @return     No return value
   *  @throws     JspException
   */
    public int doStartTag() throws JspException {
        this.init();
        String channelStr = tagFormatter.getStartContent();
        ResponseUtils.write(pageContext, channelStr);
        return EVAL_BODY_INCLUDE;
    }

    public int doEndTag() throws JspException {
        ResponseUtils.write(pageContext, tagFormatter.getEndContent());
        return EVAL_PAGE;
    }

    public String getSelectFilter(Filter filter) {
        StringBuffer bstr = new StringBuffer();
        bstr.append("              <div class=\"typeSelectorContainer\">\n");
        if (filter != null) {
            List options = filter.getItems();
            if (!Utility.isEmpty(options)) {
                FilterItem item = (FilterItem) options.get(0);
                this.setDefaultFilterItem(item.getValue());
            }
            bstr.append("                  <select name=\"filterSel\" class=\"typeSelector\" onChange=\"channel" + this.filterName + ".refreshChannelWithParameters(1,this.options[this.selectedIndex].value)\">\n");
            for (Iterator items = options.iterator(); items.hasNext(); ) {
                FilterItem item = (FilterItem) items.next();
                bstr.append("                    <option value='" + item.getValue() + "' " + item.getOption() + ">" + item.getName() + "</option>\n");
            }
            bstr.append("                  </select>\n");
        }
        bstr.append("              </div>\n");
        return bstr.toString();
    }

    private void init() {
        ChannelTagAdapter channelAdapter = new ChannelTagAdapter(this);
        this.setTagFormatter(channelAdapter.getChannelTagFormatter());
        if (!Utility.isEmpty(this.getEnabledChannelStr())) {
            String channelStr = this.getEnabledChannelStr();
            String[] channels = channelStr.split("\\|");
            for (int i = 0; i < channels.length; i++) {
                if (channels[i].indexOf(this.getFilterName()) >= 0) {
                    String[] channelStyle = channels[i].split("_");
                    this.setPageSize(Integer.parseInt(channelStyle[2]));
                }
            }
        }
    }

    public PageContext getPageContext() {
        return this.pageContext;
    }

    public void release() {
        icon = null;
        label = null;
        link = null;
        filterName = null;
        enabledChannelStr = null;
        super.release();
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getFilterName() {
        return filterName;
    }

    public void setFilterName(String filterName) {
        this.filterName = filterName;
    }

    public boolean isAjax() {
        return isAjax;
    }

    public void setIsAjax(boolean isAjax) {
        this.isAjax = isAjax;
    }

    public long getUpdateReq() {
        return updateReq;
    }

    public void setUpdateReq(long updateReq) {
        this.updateReq = updateReq;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public String getEnabledChannelStr() {
        return enabledChannelStr;
    }

    public void setEnabledChannelStr(String enabledChannelStr) {
        this.enabledChannelStr = enabledChannelStr;
    }

    public AbstractChannelTagFormatter getTagFormatter() {
        return tagFormatter;
    }

    public void setTagFormatter(AbstractChannelTagFormatter tagFormatter) {
        this.tagFormatter = tagFormatter;
    }

    public String getDefaultFilterItem() {
        return defaultFilterItem;
    }

    public void setDefaultFilterItem(String defaultFilterItem) {
        this.defaultFilterItem = defaultFilterItem;
    }

    public String getIsShowButton() {
        return isShowButton;
    }

    public void setIsShowButton(String isShowButton) {
        this.isShowButton = isShowButton;
    }
}
