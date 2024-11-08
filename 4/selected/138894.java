package com.google.code.guidatv.client.ui;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.restlet.client.data.MediaType;
import org.restlet.client.data.Preference;
import org.restlet.client.resource.ClientResource;
import org.restlet.client.resource.Result;
import com.google.code.guidatv.client.ScheduleRemoteService;
import com.google.code.guidatv.client.ScheduleRemoteServiceAsync;
import com.google.code.guidatv.model.Channel;
import com.google.code.guidatv.model.LoginInfo;
import com.google.code.guidatv.model.Schedule;
import com.google.code.guidatv.model.Transmission;
import com.google.code.guidatv.client.pics.Pics;
import com.google.code.guidatv.client.service.ChannelService;
import com.google.code.guidatv.client.service.impl.ChannelServiceImpl;
import com.google.code.guidatv.client.service.rest.ChannelScheduleResourceProxy;
import com.google.code.guidatv.client.service.rest.LoginInfoResourceProxy;
import com.google.code.guidatv.client.ui.widget.ChannelTree;
import com.google.code.guidatv.client.ui.widget.DoubleEntryTable;
import com.google.code.guidatv.client.ui.widget.ResizableVerticalPanel;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.datepicker.client.DateBox;

public class ScheduleWidget extends Composite {

    private final class ScheduleResult implements Result<Schedule> {

        private Date day;

        private int selectedRow;

        public ScheduleResult(Date day, int selectedRow) {
            this.day = day;
            this.selectedRow = selectedRow;
        }

        @Override
        public void onSuccess(Schedule schedule) {
            Date start = new Date(day.getTime());
            Date end = new Date(day.getTime() + 24 * 60 * 60 * 1000);
            int module = 30 * 60 * 1000;
            int column = channel2column.get(schedule.getChannel().getCode());
            List<Transmission> transmissions = schedule.getTransmissions();
            Iterator<Transmission> transmissionIt = null;
            Transmission transmission = null;
            if (!transmissions.isEmpty()) {
                transmissionIt = transmissions.iterator();
                transmission = transmissionIt.next();
                Date currentDate = new Date(start.getTime());
                int row = 0;
                do {
                    currentDate = new Date(currentDate.getTime() + module);
                    ResizableVerticalPanel transmissionPanel = new ResizableVerticalPanel();
                    while (transmission != null && transmission.getStart().compareTo(currentDate) < 0) {
                        transmissionPanel.add(new TransmissionWidget(transmission));
                        if (transmissionIt.hasNext()) {
                            transmission = transmissionIt.next();
                        } else {
                            transmission = null;
                        }
                    }
                    scheduleTable.setWidget(row, column, transmissionPanel);
                    row++;
                } while (currentDate.compareTo(end) < 0);
            } else {
                Label label = new Label("Nessun dato");
                scheduleTable.setWidget(0, column, label);
                Date currentDate = new Date(start.getTime() + module);
                int row = 1;
                do {
                    currentDate = new Date(currentDate.getTime() + module);
                    scheduleTable.setWidget(row, column, new Label());
                    row++;
                } while (currentDate.compareTo(end) < 0);
            }
            scheduleTable.setHeaderColumnWidth("50px");
            scheduleTable.layoutByColumn(column);
            if (selectedRow >= 0) {
                scheduleTable.ensureRowVisible(selectedRow, column);
            }
        }

        @Override
        public void onFailure(Throwable caught) {
            GWT.log("Error getting schedule", caught);
        }
    }

    interface ScheduleWidgetStyle extends CssResource {

        String oddrow();

        String evenrow();

        String nowrow();
    }

    private static final Binder binder = GWT.create(Binder.class);

    private ScheduleRemoteServiceAsync scheduleService = GWT.create(ScheduleRemoteService.class);

    private ChannelService channelService;

    private Map<String, Integer> channel2column;

    @UiField
    DateBox dateBox;

    @UiField
    ScheduleWidgetStyle style;

    @UiField
    SimplePanel containerPanel;

    @UiField
    ChannelTree channelTree;

    @UiField
    Button updateButton;

    @UiField
    Label usernameLabel;

    @UiField
    Anchor logLink;

    @UiField
    Button saveButton;

    private DoubleEntryTable scheduleTable;

    private Image loading;

    interface Binder extends UiBinder<Widget, ScheduleWidget> {
    }

    public ScheduleWidget() {
        initWidget(binder.createAndBindUi(this));
        channelService = new ChannelServiceImpl();
        channelTree.init(channelService);
        scheduleTable = new DoubleEntryTable();
        scheduleTable.setMinimumRowSize(30);
        Pics pics = GWT.create(Pics.class);
        loading = new Image(pics.loading());
        dateBox.setFormat(new DateBox.DefaultFormat(DateTimeFormat.getFormat("dd/MM/yyyy")));
        dateBox.setValue(new Date());
        updateButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                loadSchedule();
            }
        });
        containerPanel.clear();
        containerPanel.add(loading);
        LoginInfoResourceProxy loginInfoResource = GWT.create(LoginInfoResourceProxy.class);
        ClientResource clientResource = loginInfoResource.getClientResource();
        clientResource.setReference("/rest/login-info?requestUri=" + URL.encode(GWT.getHostPageBaseURL()));
        clientResource.getClientInfo().getAcceptedMediaTypes().add(new Preference<MediaType>(MediaType.APPLICATION_JAVA_OBJECT_GWT));
        loginInfoResource.retrieve(new Result<LoginInfo>() {

            @Override
            public void onFailure(Throwable caught) {
                GWT.log("Non riesco a ottenere i dati di login", caught);
            }

            @Override
            public void onSuccess(LoginInfo result) {
                String nickname = result.getNickname();
                if (nickname != null) {
                    usernameLabel.setText("Benvenuto " + nickname + "!");
                    saveButton.setEnabled(true);
                } else {
                    usernameLabel.setText("Benvenuto!");
                }
                channelTree.setSelectedChannels(result.getPreferredChannels());
                logLink.setHref(result.getUrl());
                logLink.setText(result.getLinkLabel());
                loadSchedule();
            }
        });
    }

    private void loadSchedule() {
        Date date = dateBox.getValue();
        int i = 0;
        channel2column = new LinkedHashMap<String, Integer>();
        int selectedRow = prepareTable(date);
        DateTimeFormat format = DateTimeFormat.getFormat("yyyyMMddZ");
        for (String channel : channelTree.getSelectedChannels()) {
            ChannelScheduleResourceProxy channelProxy = GWT.create(ChannelScheduleResourceProxy.class);
            ClientResource clientResource = channelProxy.getClientResource();
            clientResource.setReference("/rest/channels/" + channel + "/schedules/" + format.format(date));
            clientResource.getClientInfo().getAcceptedMediaTypes().add(new Preference<MediaType>(MediaType.APPLICATION_JAVA_OBJECT_GWT));
            channelProxy.retrieve(new ScheduleResult(date, selectedRow));
            channel2column.put(channel, i);
            i++;
        }
    }

    @UiHandler("saveButton")
    void onSaveButtonClick(ClickEvent event) {
        scheduleService.savePreferredChannels(channelTree.getSelectedChannels(), new AsyncCallback<Void>() {

            @Override
            public void onFailure(Throwable caught) {
                GWT.log("Cannot save preferred channels", caught);
            }

            @Override
            public void onSuccess(Void result) {
                Window.alert("Lista canali salvata!");
            }
        });
    }

    public int prepareTable(Date day) {
        Date start = new Date(day.getTime());
        Date end = new Date(day.getTime() + 24 * 60 * 60 * 1000);
        Date now = new Date();
        boolean isToday = now.getYear() == day.getYear() && now.getMonth() == day.getMonth() && now.getDate() == day.getDate();
        containerPanel.clear();
        containerPanel.add(scheduleTable);
        DateTimeFormat format = DateTimeFormat.getFormat(PredefinedFormat.HOUR24_MINUTE);
        scheduleTable.removeAllRows();
        scheduleTable.setCornerWidget(new Label("Ora"));
        int i = 0;
        for (String channelCode : channelTree.getSelectedChannels()) {
            Channel channel = channelService.getChannelByCode(channelCode);
            scheduleTable.setRowHeaderWidget(i, new Label(channel.getName()));
            i++;
        }
        int j = 0;
        boolean selectionDone = false;
        int selectedRow = -1;
        Date currentDate = new Date(start.getTime());
        int module = 30 * 60 * 1000;
        while (currentDate.compareTo(end) < 0) {
            scheduleTable.setColumnHeaderWidget(j, new Label(format.format(currentDate)));
            String styleName;
            if (isToday && !selectionDone && (((now.getHours() - currentDate.getHours()) * 60) + (now.getMinutes() - currentDate.getMinutes())) <= 30) {
                selectionDone = true;
                styleName = style.nowrow();
                selectedRow = j;
            } else {
                styleName = j % 2 == 0 ? style.evenrow() : style.oddrow();
            }
            scheduleTable.getContentRowFormatter().addStyleName(j, styleName);
            scheduleTable.getHeaderColumnRowFormatter().addStyleName(j, styleName);
            currentDate = new Date(currentDate.getTime() + module);
            j++;
        }
        scheduleTable.setHeaderColumnWidth("50px");
        scheduleTable.layout();
        return selectedRow;
    }
}
