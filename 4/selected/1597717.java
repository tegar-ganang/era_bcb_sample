package com.wwfish.cmsui.modules.sysuser.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;
import com.nexustar.gwt.dashboard.client.AbstractPage;
import com.nexustar.gwt.dashboard.client.IFunctionModel;
import com.nexustar.gwt.dashboard.client.MenusFactory;
import com.nexustar.gwt.dashboard.client.PageClient;
import com.nexustar.gwt.widgets.client.asyn.FishAsyncCallback;
import com.nexustar.gwt.widgets.client.asyn.IAsyncModelCallback;
import com.nexustar.gwt.widgets.client.asyn.LoadingAsyncCallback;
import com.nexustar.gwt.widgets.client.ui.form.*;
import com.nexustar.gwt.widgets.client.ui.panel.InfoPanel;
import com.nexustar.gwt.widgets.client.ui.panel.WidgetContainer;
import com.nexustar.gwt.widgets.client.ui.textbox.*;
import com.nexustar.gwt.widgets.client.ui.toolbar.ToolBar;
import com.nexustar.gwt.widgets.client.ui.toolbar.ToolItem;
import com.nexustar.gwt.widgets.client.ui.tree.CheckBoxTreeItemUI;
import com.wwfish.cms.model.CMSConstants;
import com.wwfish.cms.model.ChannelDto;
import com.wwfish.cms.model.sysuser.AccessItemDto;
import com.wwfish.cms.model.sysuser.SysRoleDto;
import com.wwfish.cms.service.ChannelManager;
import com.wwfish.cms.service.sysuser.SysRoleManager;
import com.wwfish.cmsui.dashboard.client.CMSDashboard;
import com.wwfish.cmsui.dashboard.client.DashMenuBuilder;
import com.wwfish.cmsui.modules.common.client.BaseCMSPage;
import com.wwfish.cmsui.modules.common.client.CMSPageClient;
import com.wwfish.cmsui.modules.common.client.CMSRunAsyncCallBack;
import com.wwfish.cmsui.modules.common.client.util.ServiceFactory;
import com.wwfish.cmsui.modules.sysuser.client.widget.AccessChannelTreeWidget;
import com.wwfish.cmsui.modules.sysuser.client.widget.AccessFunctionTreeWidget;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Tom
 * Date: 2010-8-19
 * Time: 16:37:14
 * To change this template use File | Settings | File Templates.
 */
public class RolePage extends BaseCMSPage {

    private ToolBar bar;

    private FormContainer form;

    private AccessChannelTreeWidget channelTree;

    private AccessFunctionTreeWidget functionTree;

    protected ToolItem cancel;

    private ToolItem back;

    @Override
    protected boolean isRunAsync() {
        return true;
    }

    @Override
    public void initWidgets(final DockPanel dockPanel) {
        GWT.runAsync(new CMSRunAsyncCallBack() {

            public void onSuccess() {
                initBar();
                channelTree = new AccessChannelTreeWidget();
                functionTree = new AccessFunctionTreeWidget();
                dockPanel.setWidth("100%");
                dockPanel.add(bar, DockPanel.NORTH);
                dockPanel.setCellHeight(bar, "20px");
                dockPanel.add(getForm(), DockPanel.CENTER);
                client.success(null);
            }
        });
    }

    private void initBar() {
        bar = new ToolBar();
        bar.setWidth("100%");
        ToolItem save = new ToolItem("保存");
        back = new ToolItem("返回");
        cancel = new ToolItem("重置");
        bar.addToolItem(save);
        bar.addToolItem(cancel);
        bar.addToolItem(back);
        save.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                if (!form.isValid()) return;
                final SysRoleDto dto = (SysRoleDto) form.getModelManger().getModel();
                ServiceFactory.invoke(SysRoleManager.class.getName(), "saveAndUpdate", new Object[] { dto }, new LoadingAsyncCallback() {

                    @Override
                    protected void success(final Object result) {
                        CMSDashboard.dispatchPage(RoleManagePage.class.getName(), new CMSPageClient() {

                            @Override
                            protected void process(BaseCMSPage page) {
                                RoleManagePage mp = (RoleManagePage) page;
                                if (dto.getId() == null) {
                                    InfoPanel.show("创建成功！");
                                    mp.addRow(mp.table, result);
                                } else {
                                    InfoPanel.show("修改成功！");
                                    mp.updateRow(mp.table, result);
                                }
                            }
                        });
                    }
                });
            }
        });
        cancel.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                renderForm(null);
            }
        });
        back.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                CMSDashboard.dispatchPage(RoleManagePage.class.getName(), null);
            }
        });
    }

    private Widget getForm() {
        WidgetContainer wrapper = new WidgetContainer();
        wrapper.setSize("100%", "100%");
        form = new FormContainer(2);
        final FishLabelTextBox name = new FishLabelTextBox("角色名称:");
        final FishLabelTextAreaBox description = new FishLabelTextAreaBox("角色描述:");
        form.addElement(new FormElement(name, new FormElementProviderAdpter() {

            public void setValue(Object object) {
                name.setValue(((SysRoleDto) object).getName());
            }

            public void getValue(Object object) {
                ((SysRoleDto) object).setName((String) name.getValue());
            }
        }, new IValidator[] { ValidatorCreator.require() }));
        form.addElement(new FormElement(description, new FormElementProviderAdpter() {

            public void setValue(Object object) {
                description.setValue(((SysRoleDto) object).getDescription());
            }

            public void getValue(Object object) {
                ((SysRoleDto) object).setDescription((String) description.getValue());
            }
        }));
        form.addElement(getFunctionAccess(functionTree));
        form.addElement(getChannelAccess(channelTree));
        form.getContainer().getCellFormatter().setVerticalAlignment(1, 0, VerticalPanel.ALIGN_TOP);
        form.getContainer().getCellFormatter().setVerticalAlignment(1, 1, VerticalPanel.ALIGN_TOP);
        wrapper.add(form);
        return wrapper;
    }

    public static FormElement getFunctionAccess(final AccessFunctionTreeWidget functionTree) {
        CaptionPanel wrapper = new CaptionPanel("功能权限");
        wrapper.add(functionTree);
        FormElement el = new FormElement(wrapper, new IFormElementProvider() {

            public void setValue(Object object) {
                List access = ((SysRoleDto) object).getMenuAccesses();
                if (access == null) return;
                for (Iterator iv = access.iterator(); iv.hasNext(); ) {
                    AccessItemDto dto = (AccessItemDto) iv.next();
                    for (Iterator it = functionTree.getTree().iterator(); it.hasNext(); ) {
                        CheckBoxTreeItemUI item = (CheckBoxTreeItemUI) it.next();
                        IFunctionModel model = (IFunctionModel) item.getTreeItem().getUserObject();
                        if (model.getCode().equals(dto.getCode())) {
                            item.getCheckBox().setValue(true);
                        }
                    }
                }
            }

            public void getValue(Object object) {
                List values = functionTree.getTree().getCheckUserObject();
                List r = new ArrayList();
                for (Iterator it = values.iterator(); it.hasNext(); ) {
                    IFunctionModel dto = (IFunctionModel) it.next();
                    AccessItemDto aDto = new AccessItemDto();
                    aDto.setCode(dto.getCode());
                    r.add(aDto);
                }
                ((SysRoleDto) object).setMenuAccessListAndCheck(r);
            }

            public void reset(IFormElement element) {
                functionTree.getTree().setCheckUserObject(null);
            }
        });
        return el;
    }

    public static FormElement getChannelAccess(final AccessChannelTreeWidget channelTree) {
        CaptionPanel wrapper = new CaptionPanel("栏目权限");
        wrapper.add(channelTree);
        FormElement el = new FormElement(wrapper, new IFormElementProvider() {

            public void setValue(Object object) {
                List access = ((SysRoleDto) object).getChannelAccesses();
                if (access == null) return;
                for (Iterator iv = access.iterator(); iv.hasNext(); ) {
                    AccessItemDto dto = (AccessItemDto) iv.next();
                    for (Iterator it = channelTree.getTree().iterator(); it.hasNext(); ) {
                        CheckBoxTreeItemUI item = (CheckBoxTreeItemUI) it.next();
                        if (item.getTreeItem().getUserObject() instanceof AccessItemDto) {
                            AccessItemDto model = (AccessItemDto) item.getTreeItem().getUserObject();
                            if (model.getCode().equals(dto.getCode())) {
                                item.getCheckBox().setValue(true);
                                CheckBoxTreeItemUI pa = (CheckBoxTreeItemUI) item.getTreeItem().getParentItem().getWidget();
                                pa.getCheckBox().setValue(true);
                                checkParentChannel(pa);
                            }
                        }
                    }
                }
            }

            public void getValue(Object object) {
                List values = channelTree.getTree().getCheckUserObject();
                List r = new ArrayList();
                for (Iterator it = values.iterator(); it.hasNext(); ) {
                    Object ob = it.next();
                    if (ob instanceof AccessItemDto) {
                        AccessItemDto aDto = (AccessItemDto) ob;
                        r.add(aDto);
                    }
                }
                ((SysRoleDto) object).setChannelAccessListAndCheck(r);
            }

            public void reset(IFormElement element) {
                channelTree.getTree().setCheckUserObject(null);
            }
        });
        return el;
    }

    private static void checkParentChannel(CheckBoxTreeItemUI item) {
        if (item.getTreeItem().getParentItem() != null) {
            CheckBoxTreeItemUI pa = (CheckBoxTreeItemUI) item.getTreeItem().getParentItem().getWidget();
            pa.getCheckBox().setValue(true);
            checkParentChannel(pa);
        }
    }

    @Override
    protected void ddOutPageModel(final Object model) {
        ServiceFactory.invoke(ChannelManager.class.getName(), "getChannelTree", new Object[] { null }, new FishAsyncCallback() {

            public void onSuccess(Object o) {
                channelTree.getTree().getModelManger().renderModel(o);
                functionTree.getTree().getModelManger().renderModel(DashMenuBuilder.sequenceMenus(DashMenuBuilder.mergeMenus(MenusFactory.getInstance().getMenu())));
                renderForm(model);
                channelTree.getTree().expandAll(true);
                functionTree.getTree().expandAll(true);
            }
        });
    }

    private void renderForm(Object model) {
        if (model == null) {
            SysRoleDto dto = new SysRoleDto();
            dto.setType(CMSConstants._ROLE_TYPE_REALITY);
            getModelManger().cacheModel(dto);
        }
        form.getModelManger().renderModel(getModelManger().getModel());
    }
}
