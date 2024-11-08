package com.wwfish.cmsui.modules.sysuser.client.widget;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;
import com.nexustar.gwt.dashboard.client.MenusFactory;
import com.nexustar.gwt.widgets.client.asyn.FishAsyncCallback;
import com.nexustar.gwt.widgets.client.asyn.LoadingAsyncCallback;
import com.nexustar.gwt.widgets.client.ui.HasModel;
import com.nexustar.gwt.widgets.client.ui.ModelManager;
import com.nexustar.gwt.widgets.client.ui.form.FormContainer;
import com.nexustar.gwt.widgets.client.ui.textbox.FishLabelListBox;
import com.wwfish.cms.model.CMSConstants;
import com.wwfish.cms.model.sysuser.SysRoleDto;
import com.wwfish.cms.model.sysuser.SysUserDto;
import com.wwfish.cms.service.ChannelManager;
import com.wwfish.cms.service.sysuser.SysRoleManager;
import com.wwfish.cmsui.dashboard.client.DashMenuBuilder;
import com.wwfish.cmsui.modules.common.client.util.ServiceFactory;
import com.wwfish.cmsui.modules.sysuser.client.RolePage;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Tom
 * Date: 2010-8-22
 * Time: 14:25:18
 * To change this template use File | Settings | File Templates.
 */
public class RoleElementWidget extends Composite implements HasModel {

    public SimplePanel panel;

    private RadioButton role;

    private RadioButton manual;

    private FishLabelListBox roleList;

    private AccessChannelTreeWidget channelTree;

    private AccessFunctionTreeWidget functionTree;

    private FormContainer form;

    private ModelManager mm;

    private List<SysRoleDto> roles;

    private SysRoleDto cache;

    public RoleElementWidget() {
        panel = new SimplePanel();
        panel.setWidth("450px");
        panel.setWidget(init());
        initEvent();
        initWidget(panel);
    }

    private Widget init() {
        VerticalPanel vp = new VerticalPanel();
        HorizontalPanel wrap = new HorizontalPanel();
        role = new RadioButton("");
        manual = new RadioButton("");
        roleList = new FishLabelListBox("");
        wrap.add(role);
        wrap.add(new Label("角色选择"));
        wrap.add(roleList);
        vp.add(wrap);
        wrap = new HorizontalPanel();
        wrap.add(manual);
        wrap.add(new Label("权限选择"));
        vp.add(wrap);
        vp.add(getDetailPanel());
        return vp;
    }

    private Widget getDetailPanel() {
        channelTree = new AccessChannelTreeWidget();
        functionTree = new AccessFunctionTreeWidget();
        form = new FormContainer(2);
        form.addElement(RolePage.getFunctionAccess(functionTree));
        form.addElement(RolePage.getChannelAccess(channelTree));
        form.getContainer().getCellFormatter().setVerticalAlignment(0, 0, VerticalPanel.ALIGN_TOP);
        form.getContainer().getCellFormatter().setVerticalAlignment(0, 1, VerticalPanel.ALIGN_TOP);
        return form;
    }

    public void ddOutModel(Object[] parameters, final Object model) {
        ServiceFactory.invoke(ChannelManager.class.getName(), "getChannelTree", new Object[] { null }, new FishAsyncCallback() {

            public void onSuccess(Object o) {
                channelTree.getTree().getModelManger().renderModel(o);
                functionTree.getTree().getModelManger().renderModel(DashMenuBuilder.sequenceMenus(DashMenuBuilder.mergeMenus(MenusFactory.getInstance().getMenu())));
                channelTree.getTree().expandAll(true);
                functionTree.getTree().expandAll(true);
                initModel(model);
            }
        });
    }

    private void initModel(Object model) {
        SysRoleDto dto = (SysRoleDto) model;
        if (dto == null) {
            dto = getVirtualRoleDto();
        }
        cache = dto;
        mm.cacheModel(dto);
        Map con = new HashMap(1);
        con.put("type", CMSConstants._ROLE_TYPE_REALITY);
        final SysRoleDto finalDto = dto;
        ServiceFactory.invoke(SysRoleManager.class.getName(), "getAllRoles", new Object[] { con }, new LoadingAsyncCallback() {

            @Override
            protected void success(Object result) {
                ListBox box = (ListBox) roleList.getUI();
                box.clear();
                roles = (List<SysRoleDto>) result;
                if (roles != null) for (SysRoleDto temp : roles) {
                    box.addItem(temp.getName(), temp.getId().toString());
                }
                renderSelf(finalDto);
            }
        });
    }

    private SysRoleDto getVirtualRoleDto() {
        SysRoleDto dto = new SysRoleDto();
        dto.setType(CMSConstants._ROLE_TYPE_VIRTUAL);
        return dto;
    }

    private void renderSelf(SysRoleDto dto) {
        reset();
        if (dto.getType().equals(CMSConstants._ROLE_TYPE_REALITY)) {
            role.setValue(true);
            roleList.setVisible(true);
            roleList.setValue(dto.getId());
        } else {
            manual.setValue(true);
            channelTree.getTree().setCheckBoxEnabled(true);
            functionTree.getTree().setCheckBoxEnabled(true);
        }
        form.getModelManger().renderModel(dto);
    }

    private void reset() {
        role.setValue(false);
        roleList.setVisible(false);
        manual.setValue(false);
        roleList.setValue(null);
        form.reset();
        channelTree.getTree().setCheckBoxEnabled(false);
        functionTree.getTree().setCheckBoxEnabled(false);
    }

    private void initEvent() {
        role.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                manual.setValue(false);
                roleList.setVisible(true);
                roleList.setValue(null);
                form.reset();
                channelTree.getTree().setCheckBoxEnabled(false);
                functionTree.getTree().setCheckBoxEnabled(false);
            }
        });
        manual.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                role.setValue(false);
                roleList.setVisible(false);
                if (cache.getType().equals(CMSConstants._ROLE_TYPE_VIRTUAL)) form.getModelManger().renderModel(cache); else {
                    form.getModelManger().renderModel(getVirtualRoleDto());
                }
                channelTree.getTree().setCheckBoxEnabled(true);
                functionTree.getTree().setCheckBoxEnabled(true);
            }
        });
        ((ListBox) roleList.getUI()).addChangeHandler(new ChangeHandler() {

            public void onChange(ChangeEvent event) {
                String roleId = (String) roleList.getValue();
                if (roleId != null) {
                    for (Iterator it = roles.iterator(); it.hasNext(); ) {
                        SysRoleDto temp = (SysRoleDto) it.next();
                        if (temp.getId().toString().equals(roleId)) {
                            form.getModelManger().renderModel(temp);
                        }
                    }
                }
            }
        });
    }

    public void ddIntModel(Object model) {
        if (role.getValue()) {
            String roleId = (String) roleList.getValue();
            if (roleId != null) {
                for (Iterator it = roles.iterator(); it.hasNext(); ) {
                    SysRoleDto temp = (SysRoleDto) it.next();
                    if (temp.getId().toString().equals(roleId)) {
                        mm.cacheModel(temp);
                    }
                }
            } else mm.cacheModel(null);
        } else if (manual.getValue()) {
            mm.cacheModel(form.getModelManger().getModel());
        }
    }

    public ModelManager getModelManger() {
        if (mm == null) mm = new ModelManager(this);
        return mm;
    }
}
