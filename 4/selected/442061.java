package com.google.code.guidatv.client.ui.widget;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import com.google.code.guidatv.model.Channel;
import com.google.code.guidatv.model.Region;
import com.google.code.guidatv.client.service.ChannelService;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.Widget;

public class ChannelTree extends Composite {

    private static final Binder binder = GWT.create(Binder.class);

    @UiField
    Tree tree;

    private ChannelService service;

    private Map<String, CheckBox> channel2checkbox;

    interface Binder extends UiBinder<Widget, ChannelTree> {
    }

    @UiConstructor
    public ChannelTree() {
        initWidget(binder.createAndBindUi(this));
        channel2checkbox = new LinkedHashMap<String, CheckBox>();
    }

    public void init(ChannelService service) {
        this.service = service;
        for (Region region : service.getRegions()) {
            TreeItem regionItem = new TreeItem(new Label(region.getName()));
            tree.addItem(regionItem);
            for (String network : service.getNetworks(region.getCode())) {
                TreeItem networkItem = new TreeItem(new Label(network));
                regionItem.addItem(networkItem);
                for (Channel channel : service.getChannels(network)) {
                    CheckBox checkbox = new CheckBox(channel.getName());
                    channel2checkbox.put(channel.getCode(), checkbox);
                    TreeItem channelItem = new TreeItem(checkbox);
                    networkItem.addItem(channelItem);
                }
            }
        }
    }

    public Set<String> getSelectedChannels() {
        Set<String> channels = new LinkedHashSet<String>();
        for (Map.Entry<String, CheckBox> entry : channel2checkbox.entrySet()) {
            if (entry.getValue().getValue()) {
                channels.add(entry.getKey());
            }
        }
        return channels;
    }

    public void setSelectedChannels(Set<String> channels) {
        for (Map.Entry<String, CheckBox> entry : channel2checkbox.entrySet()) {
            entry.getValue().setValue(channels.contains(entry.getKey()));
        }
    }
}
