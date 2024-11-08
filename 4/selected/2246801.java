package com.wwfish.cmsui.modules.common.client;

import com.google.gwt.user.client.ui.*;
import com.nexustar.gwt.dashboard.client.MenusFactory;
import com.nexustar.gwt.widgets.client.asyn.FishAsyncCallback;
import com.nexustar.gwt.widgets.client.asyn.IAsyncModelCallback;
import com.nexustar.gwt.widgets.client.ui.iprovider.ITreeProvider;
import com.wwfish.cms.service.ChannelManager;
import com.wwfish.cmsui.dashboard.client.DashMenuBuilder;
import com.wwfish.cmsui.modules.common.client.util.ServiceFactory;
import com.wwfish.cmsui.modules.common.client.widget.richtext.RichTextToolbar;
import com.wwfish.cmsui.modules.content.client.ChannelTree;
import com.wwfish.cmsui.modules.sysuser.client.widget.AccessChannelTreeWidget;
import com.wwfish.cmsui.modules.sysuser.client.widget.AccessFunctionTreeWidget;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Tom
 * Date: 2010-8-11
 * Time: 9:59:52
 * To change this template use File | Settings | File Templates.
 */
public class Demo1 extends BaseCMSPage {

    @Override
    public void initWidgets(DockPanel dockPanel) {
        RichTextArea area = new RichTextArea();
        RichTextToolbar toolbar = new RichTextToolbar(area);
        Grid grid = new Grid(2, 1);
        grid.setStyleName("cw-RichText");
        grid.setWidget(0, 0, toolbar);
        grid.setWidget(1, 0, area);
        HorizontalPanel bar = new HorizontalPanel();
        PushButton t1 = new PushButton("预览");
        PushButton t2 = new PushButton("编辑");
        bar.add(t1);
        bar.add(t2);
        VerticalPanel vp = new VerticalPanel();
        vp.add(getAccessFunction());
        vp.add(getAccessChannel());
        dockPanel.add(bar, DockPanel.NORTH);
        dockPanel.add(vp, DockPanel.CENTER);
        dockPanel.add(grid, DockPanel.SOUTH);
    }

    @Override
    protected void ddOutPageModel(Object o) {
    }

    private Widget getAccessFunction() {
        AccessFunctionTreeWidget w = new AccessFunctionTreeWidget();
        List r = MenusFactory.getInstance().getMenu();
        w.getTree().getModelManger().renderModel(DashMenuBuilder.sequenceMenus(DashMenuBuilder.mergeMenus(r)));
        return w;
    }

    private Widget getAccessChannel() {
        final AccessChannelTreeWidget t = new AccessChannelTreeWidget();
        ServiceFactory.invoke(ChannelManager.class.getName(), "getChannelTree", new Object[] { null }, new FishAsyncCallback() {

            public void onSuccess(Object o) {
                t.getTree().getModelManger().renderModel(o);
            }
        });
        return t;
    }
}
