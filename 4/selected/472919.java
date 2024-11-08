package com.wwfish.cmsui.modules.channel.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.event.dom.client.ContextMenuEvent;
import com.google.gwt.event.dom.client.ContextMenuHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import com.nexustar.gwt.widgets.client.ServiceRegistryManager;
import com.nexustar.gwt.widgets.client.asyn.FishAsyncCallback;
import com.nexustar.gwt.widgets.client.asyn.IAsyncModelCallback;
import com.nexustar.gwt.widgets.client.ui.iprovider.ITreeProvider;
import com.nexustar.gwt.widgets.client.ui.panel.ContentPanel;
import com.nexustar.gwt.widgets.client.ui.toolbar.MyMenuBar;
import com.nexustar.gwt.widgets.client.ui.toolbar.MyMenuItem;
import com.nexustar.gwt.widgets.client.ui.tree.ViewTree;
import com.nexustar.gwt.widgets.client.ui.window.IMessageConfirmCall;
import com.nexustar.gwt.widgets.client.ui.window.Message;
import com.wwfish.cms.model.ChannelDto;
import com.wwfish.cms.service.ChannelManager;
import com.wwfish.cmsui.modules.common.client.BaseCMSPage;
import com.wwfish.cmsui.modules.common.client.CMSRunAsyncCallBack;
import com.wwfish.cmsui.modules.common.client.util.ServiceFactory;
import com.wwfish.gwt.reflection.client.ModelReflection;
import com.wwfish.gwt.reflection.client.ReflectionHelper;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Tom
 * Date: 2010-7-31
 * Time: 17:06:22
 * To change this template use File | Settings | File Templates.
 */
public class ChannelManagePage extends BaseCMSPage {

    private ViewTree channelTree;

    private MyMenuBar treeContextMenu;

    private PopupPanel contextMenuPanel;

    private SimplePanel mainPanel;

    private ChannelWidget channel;

    private SequenceTreeWidget sequenceTree;

    private IAsyncModelCallback modelFinishCallback = new IAsyncModelCallback() {

        public void setModelElments(Object model) {
            channelTree.getModelManger().renderAsyncModel(null);
        }
    };

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

        public void setInputData(Object[] objects, final IAsyncModelCallback iAsyncModelCallback) {
            ServiceFactory.invoke(ChannelManager.class.getName(), "getChannelTree", new Object[] { null }, new FishAsyncCallback() {

                public void onSuccess(Object o) {
                    iAsyncModelCallback.setModelElments(o);
                    if (mainPanel.getWidget() != null) {
                        mainPanel.getWidget().removeFromParent();
                    }
                    channelTree.expandAll(false);
                }
            });
        }
    };

    @Override
    protected boolean isRunAsync() {
        return true;
    }

    @Override
    public void initWidgets(final DockPanel dockPanel) {
        GWT.runAsync(new CMSRunAsyncCallBack() {

            public void onSuccess() {
                dockPanel.setWidth("100%");
                channel = new ChannelWidget(modelFinishCallback);
                sequenceTree = new SequenceTreeWidget(modelFinishCallback);
                channel.setWidth("100%");
                mainPanel = new SimplePanel();
                dockPanel.add(getChannelTree(), DockPanel.NORTH);
                dockPanel.add(mainPanel, DockPanel.CENTER);
                ChannelManagePage.this.client.success(null);
            }
        });
    }

    public Widget getChannelTree() {
        channelTree = new ViewTree(provider);
        addEvent();
        ContentPanel wrapper = new ContentPanel("栏目树", true);
        wrapper.setSize("100%", "200px");
        ScrollPanel sp = new ScrollPanel(channelTree);
        sp.setHeight("200px");
        wrapper.addContentWidget(sp);
        return wrapper;
    }

    private void initTreeContextMenu() {
        treeContextMenu = new MyMenuBar(true);
        MyMenuItem edit = new MyMenuItem("编辑", new Command() {

            public void execute() {
                channel.getModelManger().renderModel(channelTree.getSelectedItem().getUserObject());
                channel.setTitle("编辑栏目");
                mainPanel.setWidget(channel);
                contextMenuPanel.hide();
            }
        });
        MyMenuItem delete = new MyMenuItem("删除", new Command() {

            public void execute() {
                Message.confirm("当前栏目的子栏目和内容将都被删除，是否继续？", new IMessageConfirmCall() {

                    public void doExcute(boolean flag) {
                        if (flag) {
                            ServiceFactory.invoke(ChannelManager.class.getName(), "delete", new Object[] { channelTree.getSelectedItem().getUserObject() }, new FishAsyncCallback() {

                                public void onSuccess(Object o) {
                                    channelTree.deleteItem();
                                }
                            });
                        }
                    }
                });
                contextMenuPanel.hide();
            }
        });
        MyMenuItem sequence = new MyMenuItem("同级排序", new Command() {

            public void execute() {
                List sequenceChannels;
                if (channelTree.getSelectedItem().getParentItem() != null) {
                    ChannelDto parentChannel = (ChannelDto) channelTree.getSelectedItem().getParentItem().getUserObject();
                    sequenceChannels = parentChannel.getChildren();
                } else {
                    sequenceChannels = (List) channelTree.getModelManger().getModel();
                }
                sequenceTree.getTree().getModelManger().renderModel(ReflectionHelper.cloneProperty(sequenceChannels));
                mainPanel.setWidget(sequenceTree);
                contextMenuPanel.hide();
            }
        });
        MyMenuItem add = new MyMenuItem("添加同级节点", new Command() {

            public void execute() {
                ChannelDto selected = (ChannelDto) channelTree.getSelectedItem().getUserObject();
                channel.getModelManger().renderModel(getChannelModel(selected.getParentExt()));
                channel.setTitle("添加栏目");
                mainPanel.setWidget(channel);
                contextMenuPanel.hide();
                contextMenuPanel.hide();
            }
        });
        MyMenuItem addChild = new MyMenuItem("添加子节点", new Command() {

            public void execute() {
                ChannelDto selected = (ChannelDto) channelTree.getSelectedItem().getUserObject();
                channel.getModelManger().renderModel(getChannelModel(selected));
                channel.setTitle("添加子栏目");
                mainPanel.setWidget(channel);
                contextMenuPanel.hide();
            }
        });
        treeContextMenu.addItem(add);
        treeContextMenu.addItem(addChild);
        treeContextMenu.addItem(edit);
        treeContextMenu.addItem(sequence);
        treeContextMenu.addItem(delete);
        contextMenuPanel = new PopupPanel(true);
        contextMenuPanel.setWidget(treeContextMenu);
    }

    private ChannelDto getChannelModel(ChannelDto parent) {
        ChannelDto dto = new ChannelDto();
        dto.setParentExt(parent);
        if (parent != null) dto.setParentId(parent.getId());
        return dto;
    }

    private void addEvent() {
        initTreeContextMenu();
        channelTree.addContextMenuHandler(new ContextMenuHandler() {

            public void onContextMenu(ContextMenuEvent contextMenuEvent) {
                contextMenuPanel.setPopupPosition(contextMenuEvent.getNativeEvent().getClientX() + 3, contextMenuEvent.getNativeEvent().getClientY());
                contextMenuPanel.show();
            }
        });
    }

    @Override
    protected void ddOutPageModel(Object o) {
        channelTree.getModelManger().renderAsyncModel(null);
    }
}
