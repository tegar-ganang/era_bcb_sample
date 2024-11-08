package com.wwfish.cmsui.modules.content.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.RichTextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.nexustar.gwt.dashboard.client.AbstractPage;
import com.nexustar.gwt.dashboard.client.PageClient;
import com.nexustar.gwt.widgets.client.asyn.FishAsyncCallback;
import com.nexustar.gwt.widgets.client.asyn.LoadingAsyncCallback;
import com.nexustar.gwt.widgets.client.ui.HasModel;
import com.nexustar.gwt.widgets.client.ui.ModelManager;
import com.nexustar.gwt.widgets.client.ui.panel.ContentPanel;
import com.nexustar.gwt.widgets.client.ui.panel.InfoPanel;
import com.nexustar.gwt.widgets.client.ui.tab.FishTabItem;
import com.nexustar.gwt.widgets.client.ui.tab.FishTabPanel;
import com.nexustar.gwt.widgets.client.ui.toolbar.ToolBar;
import com.nexustar.gwt.widgets.client.ui.toolbar.ToolItem;
import com.nexustar.gwt.widgets.client.ui.window.Message;
import com.wwfish.cms.model.BaseContentDto;
import com.wwfish.cms.model.NewsDto;
import com.wwfish.cms.service.ChannelManager;
import com.wwfish.cms.service.NewsManager;
import com.wwfish.cmsui.dashboard.client.CMSDashboard;
import com.wwfish.cmsui.dashboard.client.DashMenuBuilder;
import com.wwfish.cmsui.modules.common.client.util.ServiceFactory;
import com.wwfish.cmsui.modules.common.client.widget.richtext.RichTextToolbar;
import com.wwfish.cmsui.modules.common.client.widget.xmlform.XMLFormContainer;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Tom
 * Date: 2010-8-10
 * Time: 14:26:52
 * To change this template use File | Settings | File Templates.
 */
public class ContentWidget extends Composite implements HasModel {

    private ContentPanel main;

    private XMLFormContainer baseForm;

    private FishTabPanel contentTab;

    private FishTabItem baseInfoItem;

    private FishTabItem baseBodyItem;

    private ToolBar bar;

    private static final String _PAGINATION_FLAG = "<pagination/>";

    private ModelManager mm;

    public ContentWidget() {
        initBar();
        contentTab = new FishTabPanel();
        contentTab.setWidth("100%");
        initBaseFormTab();
        initMainBody();
        contentTab.setSelectItem(contentTab.getItems().get(0));
        VerticalPanel vp = new VerticalPanel();
        vp.setWidth("100%");
        vp.add(bar);
        vp.add(contentTab);
        main = new ContentPanel("内容详情");
        main.addContentWidget(vp);
        initWidget(main);
    }

    public void setContentTitle(String title) {
        main.setTitle(title);
    }

    private void initBar() {
        bar = new ToolBar();
        bar.setWidth("100%");
        ToolItem save = new ToolItem("确认保存");
        ToolItem pagination = new ToolItem("添加分页");
        ToolItem cancel = new ToolItem("取消");
        bar.addToolItem(save);
        bar.addToolItem(pagination);
        bar.addToolItem(cancel);
        cancel.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                final NewsDto dto = (NewsDto) mm.getModel();
                if (dto.getId() == null) {
                    NewsDto ins = (NewsDto) getNewContent();
                    ins.setChannelId(dto.getChannelId());
                    mm.renderModel(ins);
                    contentTab.setSelectItem(baseInfoItem);
                } else {
                    CMSDashboard.dispatchPage(ContentListPage.class.getName(), new PageClient() {

                        public void success(AbstractPage page) {
                        }

                        public void failure() {
                        }
                    });
                }
            }
        });
        save.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent clickEvent) {
                final NewsDto dto = (NewsDto) mm.getModel();
                if (dto.getChannelId() == null) {
                    Message.info("请选择栏目！");
                    return;
                }
                if (!baseForm.isValid()) return;
                getBodyModel(dto);
                baseForm.getModelManger().getModel();
                ServiceFactory.invoke(NewsManager.class.getName(), "saveAndUpdate", new Object[] { dto }, new LoadingAsyncCallback() {

                    public void success(Object result) {
                        InfoPanel.show("保存成功！");
                        NewsDto ins = (NewsDto) getNewContent();
                        ins.setChannelId(dto.getChannelId());
                        mm.renderModel(ins);
                        contentTab.setSelectItem(baseInfoItem);
                    }
                });
            }
        });
        pagination.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                addItem(null);
            }
        });
    }

    private void addItem(String value) {
        FishTabItem item = new FishTabItem(contentTab, getBodyWidget(value), new FishTabItem.FishTableItemEvent() {

            public void onSelect() {
            }

            public void onRefresh() {
            }

            public void onClose() {
                contentTab.setSelectItem(baseBodyItem);
            }
        });
        item.setText("内容信息(分)");
        item.setRefresh(false);
        contentTab.addTabItem(item);
        contentTab.setSelectItem(item);
    }

    private void getBodyModel(NewsDto dto) {
        StringBuffer sb = new StringBuffer();
        for (int i = 1; i < contentTab.getItems().size(); i++) {
            FishTabItem item = contentTab.getItems().get(i);
            RichTextArea area = (RichTextArea) ((Grid) item.getShowWidget()).getWidget(1, 0);
            sb.append(area.getHTML());
            sb.append(_PAGINATION_FLAG);
        }
        dto.setBody(sb.toString());
    }

    private void renderBodyModel(NewsDto dto) {
        resetBodyModel();
        FishTabItem item = contentTab.getItems().get(1);
        RichTextArea area = (RichTextArea) ((Grid) item.getShowWidget()).getWidget(1, 0);
        area.setText(null);
        if (dto.getBody() == null) return;
        String[] body = dto.getBody().split(_PAGINATION_FLAG);
        area.setText(body[0]);
        for (int j = 1; j < body.length; j++) {
            addItem(body[j]);
        }
    }

    private void resetBodyModel() {
        for (int i = contentTab.getItems().size() - 1; i > 1; i--) {
            FishTabItem item = contentTab.getItems().get(i);
            contentTab.closeTabItem(item);
        }
    }

    private void initBaseFormTab() {
        baseForm = new XMLFormContainer("news-form.xml", 2);
        baseInfoItem = new FishTabItem(contentTab, baseForm);
        baseInfoItem.setText("资讯基本信息");
        baseInfoItem.setClose(false);
        baseInfoItem.setRefresh(false);
        contentTab.addTabItem(baseInfoItem);
    }

    private void initMainBody() {
        baseBodyItem = new FishTabItem(contentTab, getBodyWidget(null));
        baseBodyItem.setText("内容信息");
        baseBodyItem.setClose(false);
        baseBodyItem.setRefresh(false);
        contentTab.addTabItem(baseBodyItem);
    }

    private Grid getBodyWidget(String value) {
        RichTextArea area = new RichTextArea();
        area.setSize("100%", "100%");
        RichTextToolbar toolbar = new RichTextToolbar(area);
        if (value != null) area.setText(value);
        Grid grid = new Grid(2, 1);
        grid.setStyleName("cw-RichText");
        grid.setWidget(0, 0, toolbar);
        grid.getCellFormatter().setHeight(0, 0, "40px");
        grid.setWidget(1, 0, area);
        grid.setWidth("100%");
        grid.setHeight("400px");
        return grid;
    }

    public BaseContentDto getNewContent() {
        NewsDto news = new NewsDto();
        return news;
    }

    public void ddOutModel(Object[] parameters, Object model) {
        if (model == null) {
            model = getNewContent();
            mm.cacheModel(model);
        }
        baseForm.getModelManger().renderModel(model);
        if (((NewsDto) model).getChannelNameExt() != null) main.setTitle(((NewsDto) model).getChannelNameExt() + "[内容详情]");
        renderBodyModel((NewsDto) model);
    }

    public void ddIntModel(Object model) {
    }

    public ModelManager getModelManger() {
        if (mm == null) mm = new ModelManager(this);
        return mm;
    }
}
