package com.wwfish.cms.service;

import com.wwfish.cms.model.ChannelDto;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Tom
 * Date: 2010-8-2
 * Time: 16:45:17
 * To change this template use File | Settings | File Templates.
 */
public class ChannelManagerImpl implements ChannelManager {

    public ChannelDto saveAndUpdate(ChannelDto dto) {
        return null;
    }

    public void sequenceChannel(List channels) {
    }

    public void delete(ChannelDto dto) {
    }

    public List<ChannelDto> getChannelTree(ChannelDto parent) {
        List result = new ArrayList();
        if (parent == null) {
        }
        ChannelDto d1 = new ChannelDto();
        d1.setName("首页");
        d1.setId(1l);
        ChannelDto d2 = new ChannelDto();
        d2.setName("新闻");
        d2.setId(2l);
        d2.setChildren(getChildren());
        result.add(d1);
        result.add(d2);
        return result;
    }

    private List getChildren() {
        ChannelDto d1 = new ChannelDto();
        d1.setName("国内");
        d1.setId(3l);
        ChannelDto d2 = new ChannelDto();
        d2.setName("国际");
        d2.setId(4l);
        List a = new ArrayList();
        a.add(d1);
        a.add(d2);
        return a;
    }
}
