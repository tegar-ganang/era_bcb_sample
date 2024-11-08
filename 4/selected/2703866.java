package org.jdna.bmt.web.client.ui.browser;

import java.util.Map;
import org.jdna.bmt.web.client.Application;
import org.jdna.bmt.web.client.media.GWTCastMember;
import org.jdna.bmt.web.client.media.GWTMediaFile;
import org.jdna.bmt.web.client.media.GWTMediaMetadata;
import org.jdna.bmt.web.client.ui.util.DataDialog;
import org.jdna.bmt.web.client.util.DateFormatUtil;
import org.jdna.bmt.web.client.util.MessageHandler;
import org.jdna.bmt.web.client.util.NotificationCallback;
import org.jdna.bmt.web.client.util.NumberUtil;
import org.jdna.bmt.web.client.util.StringUtils;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ErrorEvent;
import com.google.gwt.event.dom.client.ErrorHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class ViewAiringItemDetails extends Composite implements MessageHandler, HasMediaFile {

    private static ViewAiringItemDetailsUiBinder uiBinder = GWT.create(ViewAiringItemDetailsUiBinder.class);

    @UiField
    Label showTitle;

    @UiField
    Label episodeName;

    @UiField
    Label description;

    @UiField
    Image poster;

    @UiField
    Label seasonEp;

    @UiField
    Label seasonEpLabel;

    @UiField
    Label aired;

    @UiField
    Label duration;

    @UiField
    Label channel;

    @UiField
    Image firstRun;

    @UiField
    Image watchedMarker;

    @UiField
    Image manualRecord;

    @UiField
    Label category;

    @UiField
    Label airingid;

    @UiField
    HTMLPanel actors;

    private GWTMediaFile file = null;

    private BrowsePanel controller = null;

    interface ViewAiringItemDetailsUiBinder extends UiBinder<Widget, ViewAiringItemDetails> {
    }

    public ViewAiringItemDetails(GWTMediaFile gfile, BrowsePanel controller) {
        initWidget(uiBinder.createAndBindUi(this));
        History.newItem("viewairing", false);
        this.file = gfile;
        this.controller = controller;
        poster.addErrorHandler(new ErrorHandler() {

            @Override
            public void onError(ErrorEvent event) {
                GWT.log("Invalid URL: " + poster.getUrl());
                poster.setUrl("images/128x128/video2.png");
            }
        });
        poster.setUrl("fanart?mediafile=" + gfile.getAiringId() + "&scalex=200&artifact=poster");
        showTitle.setText(file.getTitle());
        if (file.getMinorTitle() != null && !file.getMinorTitle().equals(file.getTitle())) {
            episodeName.setText("\"" + file.getMinorTitle() + "\"");
        }
        description.setText("");
        controller.getServices().loadMetadata(file, new AsyncCallback<GWTMediaMetadata>() {

            @Override
            public void onSuccess(GWTMediaMetadata result) {
                updateDisplay(result);
            }

            @Override
            public void onFailure(Throwable caught) {
                Application.fireErrorEvent("Unable to load metadata for " + file.getTitle(), caught);
            }
        });
        if (file.getAiringDetails() != null) {
            aired.setText(DateFormatUtil.formatAiredDate(file.getAiringDetails().getStartTime()));
            duration.setText(DateFormatUtil.formatDurationFancy(file.getAiringDetails().getDuration()));
            channel.setText(file.getAiringDetails().getChannel() + " (" + file.getAiringDetails().getNetwork() + ")");
            firstRun.setVisible(file.getAiringDetails().isFirtRun());
            manualRecord.setVisible(file.getAiringDetails().isManualRecord());
            if (file.getAiringDetails().getYear() > 0) {
                showTitle.setText(file.getTitle() + " (" + file.getAiringDetails().getYear() + ")");
            }
        }
        watchedMarker.setVisible(file.getIsWatched().get());
        airingid.setText(file.getAiringId());
    }

    protected void updateDisplay(GWTMediaMetadata result) {
        file.attachMetadata(result);
        int ep = NumberUtil.toInt(result.getEpisodeNumber().get(), 0);
        if (ep > 0) {
            seasonEp.setText(result.getSeasonNumber().get() + " x " + result.getEpisodeNumber().get());
        } else {
            seasonEp.setVisible(false);
            seasonEpLabel.setVisible(false);
        }
        description.setText(result.getDescription().get());
        category.setText(result.getGenres().get());
        StringBuilder sb = new StringBuilder();
        for (GWTCastMember c : result.getActors()) {
            sb.append(c.getName());
            if (!StringUtils.isEmpty(c.getRole())) {
                sb.append(" -- ").append(c.getRole());
            }
            sb.append("<br/>");
        }
        actors.add(new HTML(sb.toString()));
    }

    @UiHandler("back")
    public void back(ClickEvent evt) {
        controller.back();
    }

    @UiHandler("updateFanart")
    public void udpateFanart(ClickEvent evt) {
        SearchQueryOptions options = new SearchQueryOptions(file);
        DataDialog.showDialog(new SearchQueryDialog(controller, file, options));
    }

    @Override
    public void onMessageReceived(String msg, Map<String, ?> args) {
        if (BrowsePanel.MSG_METADATA_CHANGED.equals(msg)) {
            GWTMediaFile mf = (GWTMediaFile) args.get("mediafile");
            if (mf.getMetadata() != null) {
                if (mf.getMetadata().getFanart().size() > 0) {
                    Application.fireNotification("Saving fanart...");
                    controller.saveMetadata(mf, null, this);
                } else {
                    Application.fireNotification("No fanart to save");
                }
            }
        } else if (BrowsePanel.MSG_FILE_WATCHED.equals(msg)) {
            GWTMediaFile f = (GWTMediaFile) args.get("file");
            if (f.getAiringId() != null && f.getAiringId().equals(file.getAiringId())) {
                boolean watched = (Boolean) args.get("watched");
                file.getIsWatched().set(watched);
                watchedMarker.setVisible(watched);
            }
        }
    }

    @Override
    protected void onAttach() {
        super.onAttach();
        controller.getMessageBus().addHandler(BrowsePanel.MSG_METADATA_CHANGED, this);
        controller.getMessageBus().addHandler(BrowsePanel.MSG_FILE_WATCHED, this);
    }

    @Override
    protected void onDetach() {
        super.onDetach();
        controller.getMessageBus().removeHandler(BrowsePanel.MSG_METADATA_CHANGED, this);
        controller.getMessageBus().removeHandler(BrowsePanel.MSG_FILE_WATCHED, this);
    }

    @Override
    public void setMediaFile(GWTMediaFile file) {
        Application.fireNotification("Fanart is saving, but it may take a few minutes to download all the items.");
    }

    @UiHandler("record")
    public void record(ClickEvent evt) {
        controller.record(file);
    }

    @UiHandler("watched")
    public void watched(ClickEvent evt) {
        controller.setWatched(file, !file.getIsWatched().get());
    }

    @UiHandler("cancelRecord")
    public void cancelRecord(ClickEvent evt) {
        controller.getServices().cancelRecord(file, new NotificationCallback<Void>("Failed to cancel recording", "Recording Cancelled"));
    }
}
