package com.wwfish.cmsui.modules.content.client;

import com.google.gwt.user.client.ui.*;
import com.nexustar.gwt.widgets.client.asyn.FishAsyncCallback;
import com.nexustar.gwt.widgets.client.asyn.IAsyncModelCallback;
import com.nexustar.gwt.widgets.client.asyn.LoadingAsyncCallback;
import com.nexustar.gwt.widgets.client.ui.iprovider.ITreeProvider;
import com.nexustar.gwt.widgets.client.ui.panel.ContentPanel;
import com.nexustar.gwt.widgets.client.ui.tree.ViewTree;
import com.wwfish.cms.model.ChannelDto;
import com.wwfish.cms.service.ChannelManager;
import com.wwfish.cmsui.modules.common.client.util.ServiceFactory;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Tom
 * Date: 2010-8-11
 * Time: 11:26:17
 * To change this template use File | Settings | File Templates.
 */
public class ChannelTree extends Composite {

    private ViewTree channelTree;

    private IAsyncModelCallback iAsyncModelCallback;

    public void setIAsyncModelCallback(IAsyncModelCallback iAsyncModelCallback) {
        this.iAsyncModelCallback = iAsyncModelCallback;
    }

    public ChannelTree() {
        channelTree = new ViewTree(provider);
        ContentPanel wrapper = new ContentPanel("栏目树", true);
        wrapper.setSize("100%", "200px");
        ScrollPanel sp = new ScrollPanel(channelTree);
        sp.setHeight("200px");
        wrapper.addContentWidget(sp);
        initWidget(wrapper);
    }

    public ViewTree getTree() {
        return channelTree;
    }

    private ITreeProvider provider = new ITreeProvider() {

        public String getTreeItemText(Object o) {
            ChannelDto channel = (ChannelDto) o;
            return channel.getName();
        }

        public Image getTreeItemIcon(Object o) {
            return null;
        }

        public boolean hasChildren(Object o) {
            ChannelDto channel = (ChannelDto) o;
            List children = channel.getChildren();
            if (children != null && children.size() > 0) return true; else return false;
        }

        public void getChildren(Object o, IAsyncModelCallback iAsyncModelCallback) {
            ChannelDto channel = (ChannelDto) o;
            List children = channel.getChildren();
            iAsyncModelCallback.setModelElments(children);
        }

        public void setInputData(final Object[] objects, final IAsyncModelCallback iAsyncModelCallback) {
            ServiceFactory.invoke(ChannelManager.class.getName(), "getChannelTree", new Object[] { null }, new LoadingAsyncCallback() {

                public void success(Object o) {
                    iAsyncModelCallback.setModelElments(o);
                    if (objects != null && ((Boolean) objects[0])) if (ChannelTree.this.iAsyncModelCallback != null) ChannelTree.this.iAsyncModelCallback.setModelElments(o);
                }
            });
        }
    };

    public void getAllChannelName(StringBuffer sb, TreeItem item) {
        ChannelDto channelDto = (ChannelDto) item.getUserObject();
        if (item.getParentItem() != null) {
            getAllChannelName(sb, item.getParentItem());
            sb.append("/");
        }
        sb.append(channelDto.getName());
    }
}
