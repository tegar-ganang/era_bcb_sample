package com.wwfish.cmsui.modules.content.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.*;
import com.nexustar.gwt.dashboard.client.AbstractPage;
import com.nexustar.gwt.dashboard.client.PageClient;
import com.nexustar.gwt.widgets.client.asyn.FishAsyncCallback;
import com.nexustar.gwt.widgets.client.asyn.IAsyncModelCallback;
import com.nexustar.gwt.widgets.client.asyn.LoadingAsyncCallback;
import com.nexustar.gwt.widgets.client.model.PaginationModelDto;
import com.nexustar.gwt.widgets.client.ui.button.MyButton;
import com.nexustar.gwt.widgets.client.ui.form.FormContainer;
import com.nexustar.gwt.widgets.client.ui.form.FormElement;
import com.nexustar.gwt.widgets.client.ui.form.FormElementProviderAdpter;
import com.nexustar.gwt.widgets.client.ui.iprovider.IPaginationModelProvider;
import com.nexustar.gwt.widgets.client.ui.panel.ContentPanel;
import com.nexustar.gwt.widgets.client.ui.panel.InfoPanel;
import com.nexustar.gwt.widgets.client.ui.table.*;
import com.nexustar.gwt.widgets.client.ui.textbox.FishLabelDateBox;
import com.nexustar.gwt.widgets.client.ui.textbox.FishLabelListBox;
import com.nexustar.gwt.widgets.client.ui.textbox.FishLabelTextBox;
import com.nexustar.gwt.widgets.client.ui.toolbar.ToolBar;
import com.nexustar.gwt.widgets.client.ui.toolbar.ToolItem;
import com.nexustar.gwt.widgets.client.ui.tree.TreeItemUI;
import com.nexustar.gwt.widgets.client.ui.window.IMessageConfirmCall;
import com.nexustar.gwt.widgets.client.ui.window.Message;
import com.wwfish.cms.model.CMSConstants;
import com.wwfish.cms.model.ChannelDto;
import com.wwfish.cms.model.NewsDto;
import com.wwfish.cms.service.AuditContentManager;
import com.wwfish.cms.service.NewsManager;
import com.wwfish.cmsui.dashboard.client.CMSDashboard;
import com.wwfish.cmsui.modules.common.client.BaseCMSPage;
import com.wwfish.cmsui.modules.common.client.util.CodesHelper;
import com.wwfish.cmsui.modules.common.client.util.ServiceFactory;
import com.wwfish.gwt.reflection.client.ModelReflection;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Tom
 * Date: 2010-8-16
 * Time: 15:41:03
 * To change this template use File | Settings | File Templates.
 */
public class ContentListPage extends BaseCMSPage {

    private ChannelTree channel;

    private PaginationTable tableList;

    private FormContainer searchForm;

    @Override
    protected boolean isRunAsync() {
        return true;
    }

    @Override
    public void initWidgets(final DockPanel page) {
        GWT.runAsync(new RunAsyncCallback() {

            public void onFailure(Throwable reason) {
            }

            public void onSuccess() {
                page.setWidth("100%");
                VerticalPanel vp = new VerticalPanel();
                vp.setWidth("100%");
                vp.setHeight("100%");
                initTableList();
                ContentPanel tableListWrapper = new ContentPanel("内容管理列表");
                tableListWrapper.setWidth("100%");
                tableListWrapper.setHeight("100%");
                tableListWrapper.addContentWidget(tableList);
                vp.add(getSearchPanel());
                vp.add(getMenuBar());
                vp.add(tableListWrapper);
                vp.setCellHeight(tableListWrapper, "96%");
                channel = new ChannelTree();
                channel.setIAsyncModelCallback(new IAsyncModelCallback() {

                    public void setModelElments(Object model) {
                        tableList.getModelManger().renderAsyncModel(new Object[] { 0, 12 });
                    }
                });
                page.add(channel, DockPanel.NORTH);
                page.add(vp, DockPanel.CENTER);
                client.success(null);
            }
        });
    }

    private Widget getMenuBar() {
        ToolBar bar = new ToolBar();
        bar.setWidth("100%");
        ToolItem commit = new ToolItem("提交");
        ToolItem delete = new ToolItem("删除");
        bar.addToolItem(commit);
        bar.addToolItem(delete);
        commit.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                List list = ((CheckBoxViewTable) tableList.getTable()).getSelectModelDate();
                if (list.size() > 0) ServiceFactory.invoke(AuditContentManager.class.getName(), "commitContentList", new Object[] { list }, new LoadingAsyncCallback() {

                    public void success(Object o) {
                        InfoPanel.show("提交成功！");
                        ddOutPageModel(null);
                    }
                });
            }
        });
        delete.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                List list = ((CheckBoxViewTable) tableList.getTable()).getSelectModelDate();
                if (list.size() > 0) Message.confirm("确认要删除?", "温馨提示", new IMessageConfirmCall() {

                    public void doExcute(boolean flag) {
                        if (flag) {
                            List list = ((CheckBoxViewTable) tableList.getTable()).getSelectModelDate();
                            ServiceFactory.invoke(NewsManager.class.getName(), "deleteList", new Object[] { list }, new LoadingAsyncCallback() {

                                public void success(Object o) {
                                    InfoPanel.show("删除成功！");
                                    ddOutPageModel(null);
                                }
                            });
                        }
                    }
                });
            }
        });
        return bar;
    }

    private void initTableList() {
        CheckBoxViewTable table = new CheckBoxViewTable(getColsWaiter());
        tableList = new PaginationTable(table, 12);
        tableList.getTable().setHeight("320px");
        tableList.getModelManger().setProvider(new IPaginationModelProvider() {

            public void setInputData(int begin, int count, final IAsyncModelCallback asyn) {
                Map conditions = (Map) searchForm.getModelManger().getModel();
                if (channel.getTree().getSelectedItem() != null) {
                    ChannelDto dto = (ChannelDto) channel.getTree().getSelectedItem().getUserObject();
                    conditions.put("channel", dto.getId());
                }
                ServiceFactory.invoke(NewsManager.class.getName(), "getPageEntities", new Object[] { begin, count, conditions }, new LoadingAsyncCallback() {

                    public void success(Object o) {
                        asyn.setModelElments(o);
                    }
                });
            }
        });
    }

    private ContentPanel getSearchPanel() {
        ContentPanel searchPanel = new ContentPanel("查询条件");
        searchPanel.setWidth("100%");
        final FishLabelTextBox sName = new FishLabelTextBox("关键字:");
        final FishLabelListBox status = new FishLabelListBox("审核状态:");
        final FishLabelDateBox createTimeBegin = new FishLabelDateBox("创建时间:");
        final FishLabelDateBox createTimeEnd = new FishLabelDateBox("至:");
        final FishLabelDateBox publishTimeBegin = new FishLabelDateBox("发布时间:");
        final FishLabelDateBox publishTimeEnd = new FishLabelDateBox("至:");
        final MyButton go = new MyButton("查询");
        final MyButton reset = new MyButton("重置");
        CodesHelper.initGeneralCodesToListWidget(CodesHelper.getCodesById(CMSConstants._CODE_CONTENT_WORKFLOW_STATUS_ID), (ListBox) status.getUI(), false);
        go.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                tableList.getModelManger().renderAsyncModel(new Object[] { 0, 12 });
            }
        });
        reset.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                searchForm.getModelManger().renderModel(new HashMap());
                channel.getTree().getModelManger().renderAsyncModel(null);
            }
        });
        searchForm = new FormContainer(2);
        searchForm.getModelManger().renderModel(new HashMap());
        searchForm.addElement(new FormElement(sName, new FormElementProviderAdpter() {

            public void getValue(Object object) {
                ((Map) object).put("name", sName.getValue());
            }

            public void setValue(Object object) {
                sName.setValue(((Map) object).get("name"));
            }
        }));
        searchForm.addElement(new FormElement(status, new FormElementProviderAdpter() {

            public void getValue(Object object) {
                ((Map) object).put("status", status.getValue());
            }

            public void setValue(Object object) {
                status.setValue(((Map) object).get("status"));
            }
        }));
        searchForm.addElement(new FormElement(createTimeBegin, new FormElementProviderAdpter() {

            public void getValue(Object object) {
                ((Map) object).put("createTimeBegin", createTimeBegin.getValue() == null ? null : ((Date) createTimeBegin.getValue()).getTime() + "");
            }

            public void setValue(Object object) {
                createTimeBegin.setValue(((Map) object).get("createTimeBegin"));
            }
        }));
        searchForm.addElement(new FormElement(createTimeEnd, new FormElementProviderAdpter() {

            public void getValue(Object object) {
                ((Map) object).put("createTimeEnd", createTimeEnd.getValue() == null ? null : ((Date) createTimeEnd.getValue()).getTime() + "");
            }

            public void setValue(Object object) {
                createTimeEnd.setValue(((Map) object).get("createTimeEnd"));
            }
        }));
        searchForm.addElement(new FormElement(publishTimeBegin, new FormElementProviderAdpter() {

            public void getValue(Object object) {
                ((Map) object).put("publishTimeBegin", publishTimeBegin.getValue() == null ? null : ((Date) publishTimeBegin.getValue()).getTime() + "");
            }

            public void setValue(Object object) {
                publishTimeBegin.setValue(((Map) object).get("publishTimeBegin"));
            }
        }));
        searchForm.addElement(new FormElement(publishTimeEnd, new FormElementProviderAdpter() {

            public void getValue(Object object) {
                ((Map) object).put("publishTimeEnd", publishTimeEnd.getValue() == null ? null : ((Date) publishTimeEnd.getValue()).getTime() + "");
            }

            public void setValue(Object object) {
                publishTimeEnd.setValue(((Map) object).get("publishTimeEnd"));
            }
        }));
        searchForm.addWidget(new Label(""));
        HorizontalPanel bar = new HorizontalPanel();
        bar.add(go);
        bar.add(new HTML("&nbsp;"));
        bar.add(reset);
        searchForm.addWidget(bar);
        searchPanel.addContentWidget(searchForm);
        return searchPanel;
    }

    private ColumnWaiter getColsWaiter() {
        ColumnExt co1 = new ColumnExt("内容名称", 200, new IColumnRender() {

            public void renderColumn(CellItem cell) {
                NewsDto dto = (NewsDto) cell.getUserObject();
                cell.setText(dto.getName());
            }
        });
        ColumnExt co8 = new ColumnExt("所属栏目", 200, new IColumnRender() {

            public void renderColumn(CellItem cell) {
                NewsDto dto = (NewsDto) cell.getUserObject();
                dto.setChannelNameExt(getChannelName(dto.getChannelId()));
                cell.setText(dto.getChannelNameExt());
            }
        });
        ColumnExt co3 = new ColumnExt("审核状态", 100, new IColumnRender() {

            public void renderColumn(CellItem cell) {
                NewsDto dto = (NewsDto) cell.getUserObject();
                cell.setText(CodesHelper.getNameByCode(CMSConstants._CODE_CONTENT_WORKFLOW_STATUS_ID, dto.getWorkflowStatus()));
            }
        });
        ColumnExt co4 = new ColumnExt("创建日期", 150, new IColumnRender() {

            public void renderColumn(CellItem cell) {
                NewsDto dto = (NewsDto) cell.getUserObject();
                cell.setText(DateTimeFormat.getFormat("yyyy-MM-dd HH:mm:ss").format(dto.getCreateTime()));
            }
        });
        ColumnExt co2 = new ColumnExt("审核日期", 150, new IColumnRender() {

            public void renderColumn(CellItem cell) {
                NewsDto dto = (NewsDto) cell.getUserObject();
                if (dto.getAuditTime() != null) cell.setText(DateTimeFormat.getFormat("yyyy-MM-dd HH:mm:ss").format(dto.getAuditTime()));
            }
        });
        ColumnExt co6 = new ColumnExt("发布日期", 150, new IColumnRender() {

            public void renderColumn(CellItem cell) {
                NewsDto dto = (NewsDto) cell.getUserObject();
                if (dto.getAuditTime() != null) cell.setText(DateTimeFormat.getFormat("yyyy-MM-dd HH:mm:ss").format(dto.getPublishTime()));
            }
        });
        ColumnExt co5 = new ColumnExt("操作", 100, new IColumnRender() {

            public void renderColumn(final CellItem cell) {
                HorizontalPanel bar = new HorizontalPanel();
                MyButton t1 = new MyButton("预览");
                MyButton t2 = new MyButton("编辑");
                bar.add(t1);
                bar.add(new HTML("&nbsp;"));
                bar.add(t2);
                cell.setWidget(bar);
                t1.addClickHandler(new ClickHandler() {

                    public void onClick(ClickEvent clickEvent) {
                    }
                });
                t2.addClickHandler(new ClickHandler() {

                    public void onClick(ClickEvent clickEvent) {
                        CMSDashboard.dispatchPage(EditContentPage.class.getName(), new PageClient() {

                            public void success(AbstractPage page) {
                                page.getModelManger().renderModel(ModelReflection.clone(cell.getUserObject()));
                            }

                            public void failure() {
                            }
                        });
                    }
                });
            }
        });
        ColumnWaiter cw = new ColumnWaiter(new ColumnExt[] { co1, co8, co3, co4, co2, co6, co5 });
        return cw;
    }

    @Override
    protected void ddOutPageModel(Object o) {
        channel.getTree().getModelManger().renderAsyncModel(new Object[] { Boolean.TRUE });
    }

    private String getChannelName(Long id) {
        for (Iterator it = channel.getTree().iterator(); it.hasNext(); ) {
            TreeItemUI item = (TreeItemUI) it.next();
            ChannelDto temp = (ChannelDto) item.getTreeItem().getUserObject();
            if (temp.getId().equals(id)) {
                StringBuffer sb = new StringBuffer();
                channel.getAllChannelName(sb, item.getTreeItem());
                return sb.toString();
            }
        }
        return null;
    }
}
