package org.jdna.bmt.web.client.ui.prefs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import org.jdna.bmt.web.client.Application;
import org.jdna.bmt.web.client.ui.input.InputBuilder;
import org.jdna.bmt.web.client.ui.util.Dialogs;
import org.jdna.bmt.web.client.ui.util.TableUtils;
import org.jdna.bmt.web.client.ui.util.UpdatablePanel;
import org.jdna.bmt.web.client.util.StringUtils;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class ChannelsEditorPanel extends Composite implements UpdatablePanel {

    private final PreferencesServiceAsync preferencesService = GWT.create(PreferencesService.class);

    private VerticalPanel panel = new VerticalPanel();

    private ArrayList<Channel> channels = null;

    private FlexTable table = null;

    private ListBox sortSelection;

    private TextBox range;

    public ChannelsEditorPanel() {
        panel.setWidth("100%");
        initWidget(panel);
    }

    public void onLoad() {
        final PopupPanel waiting = Dialogs.showWaitingPopup("Loading Channels...");
        waiting.show();
        preferencesService.getChannels(new AsyncCallback<ArrayList<Channel>>() {

            public void onFailure(Throwable caught) {
                waiting.hide();
                Application.fireErrorEvent("Failed to load channels", caught);
            }

            public void onSuccess(ArrayList<Channel> result) {
                waiting.hide();
                updatePanel(result);
            }
        });
    }

    private void updatePanel(ArrayList<Channel> sources) {
        this.channels = sources;
        panel.clear();
        HorizontalPanel hp = new HorizontalPanel();
        hp.setSpacing(10);
        sortSelection = new ListBox();
        sortSelection.addItem("Number", "number");
        sortSelection.addItem("Name", "name");
        sortSelection.addItem("Enabled", "enabled");
        sortSelection.addChangeHandler(new ChangeHandler() {

            @Override
            public void onChange(ChangeEvent event) {
                updateSort();
            }
        });
        HorizontalPanel lab = new HorizontalPanel();
        lab.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        lab.add(new Label("Sort By:"));
        lab.add(sortSelection);
        hp.add(lab);
        range = new TextBox();
        range.setTitle("You can specify a range using a starting channel # - ending channel #, ie, '600-699'.  You can specify multiple ranges using a comma, ie, '600-699, 850-899, 1000-1099'");
        Button enable = new Button("Enable");
        enable.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                updateRange(true);
            }
        });
        Button disable = new Button("Disable");
        disable.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                updateRange(false);
            }
        });
        HorizontalPanel rangePanel = new HorizontalPanel();
        rangePanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        rangePanel.add(new Label("Update Range:"));
        rangePanel.add(range);
        rangePanel.add(enable);
        rangePanel.add(disable);
        hp.add(rangePanel);
        panel.add(hp);
        table = new FlexTable();
        table.setWidth("100%");
        table.setCellSpacing(4);
        buildChannelTable(sources);
        panel.add(table);
    }

    protected void updateRange(boolean b) {
        try {
            String text = range.getText();
            if (StringUtils.isEmpty(text)) {
                Application.fireErrorEvent("Range must include a value");
                return;
            }
            String parts[] = text.split(",");
            for (String p : parts) {
                String r[] = p.split("-");
                if (r.length != 2) {
                    Application.fireErrorEvent("Invalid Range: " + p + "; Must be 'start'-'end'");
                    return;
                }
                int r1 = Integer.parseInt(r[0].trim());
                int r2 = Integer.parseInt(r[1].trim());
                int s = channels.size();
                for (int i = 0; i < s; i++) {
                    Channel c = channels.get(i);
                    int ch = Integer.parseInt(c.getNumber().trim());
                    if (ch >= r1 && ch <= r2) {
                        c.enabled().set(b);
                        CheckBox cb = (CheckBox) table.getWidget(i, 0);
                        cb.setValue(c.enabled().get());
                    }
                }
            }
            Application.fireNotification("Updated Range: " + text);
        } catch (Exception e) {
            Application.fireErrorEvent("Failed to update range!", e);
        }
    }

    private void buildChannelTable(ArrayList<Channel> sources) {
        table.clear();
        for (int i = 0; i < sources.size(); i++) {
            Channel vs = sources.get(i);
            addChannel(i, vs);
        }
        TableUtils.stripe(table);
    }

    protected void updateSort() {
        Application.fireNotification("Updating Channel Sort... Could take a few seconds...");
        String sort = sortSelection.getValue(sortSelection.getSelectedIndex());
        Comparator<Channel> comp = null;
        if ("number".equals(sort)) {
            comp = new ChannelNumberComparator();
        } else if ("name".equals(sort)) {
            comp = new ChannelNameComparator();
        } else if ("enabled".equals(sort)) {
            comp = new ChannelEnabledComparator();
        } else {
            Application.fireErrorEvent("Invalid Sort: " + sort);
            return;
        }
        final Comparator<Channel> comparator = comp;
        Scheduler.get().scheduleDeferred(new ScheduledCommand() {

            @Override
            public void execute() {
                Collections.sort(channels, comparator);
                buildChannelTable(channels);
            }
        });
    }

    private void addChannel(final int row, final Channel vs) {
        table.setWidget(row, 0, InputBuilder.checkbox().bind(vs.enabled()).widget());
        table.getCellFormatter().setHorizontalAlignment(row, 0, HasHorizontalAlignment.ALIGN_CENTER);
        Label l = new Label(vs.getNumber());
        l.addStyleName("form-label");
        table.setWidget(row, 1, l);
        table.getCellFormatter().setHorizontalAlignment(row, 1, HasHorizontalAlignment.ALIGN_RIGHT);
        table.setWidget(row, 2, new Label(vs.getName()));
        table.setWidget(row, 3, new Label(vs.getDescription()));
        table.setWidget(row, 4, new Label(vs.getNetwork()));
    }

    public String getHeader() {
        return "Enable Channels";
    }

    public String getHelp() {
        return "Enable/Disable Channels";
    }

    public boolean isReadonly() {
        return false;
    }

    public void save(final AsyncCallback<UpdatablePanel> callback) {
        final PopupPanel waiting = Dialogs.showWaitingPopup("Saving Channels...");
        waiting.show();
        preferencesService.saveChannels(channels, new AsyncCallback<ArrayList<Channel>>() {

            public void onFailure(Throwable caught) {
                waiting.hide();
                callback.onFailure(caught);
            }

            public void onSuccess(ArrayList<Channel> result) {
                waiting.hide();
                Dialogs.showMessageDialog("Channels", "Updated " + result.size() + " channels");
                callback.onSuccess(ChannelsEditorPanel.this);
            }
        });
    }
}
