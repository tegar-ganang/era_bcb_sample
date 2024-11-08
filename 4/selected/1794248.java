package com.business.web.cms;

import java.util.List;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.springframework.beans.factory.annotation.Autowired;
import org.springside.modules.orm.Page;
import org.springside.modules.orm.PropertyFilter;
import org.springside.modules.utils.web.struts2.Struts2Utils;
import com.business.entity.cms.Channel;
import com.business.service.ServiceException;
import com.business.service.cms.ChannelService;
import com.business.web.CrudActionSupport;

@Namespace("/channel")
@Results({ @Result(name = CrudActionSupport.RELOAD, location = "channel.action", type = "redirect") })
public class ChannelAction extends CrudActionSupport<Channel> {

    private static final long serialVersionUID = 1L;

    private ChannelService channelService;

    private Long id;

    private Channel entity;

    private Page<Channel> page = new Page<Channel>(10);

    @Override
    public String list() throws Exception {
        List<PropertyFilter> filters = PropertyFilter.buildFromHttpRequest(Struts2Utils.getRequest());
        if (!page.isOrderBySetted()) {
            page.setOrderBy("id");
            page.setOrder(Page.DESC);
        }
        page = channelService.searchChannel(page, filters);
        return SUCCESS;
    }

    @Override
    public String input() throws Exception {
        return INPUT;
    }

    @Override
    public String save() throws Exception {
        channelService.saveChannel(entity);
        return RELOAD;
    }

    @Override
    public String delete() throws Exception {
        try {
            channelService.deleteChannel(id);
            addActionMessage("delete success");
        } catch (ServiceException e) {
            logger.error(e.getMessage(), e);
            addActionMessage("delete failure");
        }
        return RELOAD;
    }

    @Override
    public void prepare() throws Exception {
    }

    /**
	 * 定义在input()前执行二次绑定.
	 */
    public void prepareInput() throws Exception {
        prepareModel();
    }

    /**
	 * 定义在save()前执行二次绑定.
	 */
    public void prepareSave() throws Exception {
        prepareModel();
    }

    /**
	 * 等同于prepare()的内部函数,供prepardMethodName()函数调用. 
	 */
    protected void prepareModel() throws Exception {
        if (id != null) {
            entity = channelService.getChannel(id);
        } else {
            entity = new Channel();
        }
    }

    @Override
    public Channel getModel() {
        return entity;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Page<Channel> getPage() {
        return page;
    }

    public void setPage(Page<Channel> page) {
        this.page = page;
    }

    @Autowired
    public void setChannelService(ChannelService channelService) {
        this.channelService = channelService;
    }
}
