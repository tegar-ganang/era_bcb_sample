package org.jdna.bmt.web.client.ui.browser;

import java.util.Map;
import org.jdna.bmt.web.client.Application;
import org.jdna.bmt.web.client.media.GWTAiringDetails;
import org.jdna.bmt.web.client.media.GWTMediaArt;
import org.jdna.bmt.web.client.media.GWTMediaFile;
import org.jdna.bmt.web.client.media.GWTMediaFolder;
import org.jdna.bmt.web.client.media.GWTMediaResource;
import org.jdna.bmt.web.client.ui.debug.DebugDialog;
import org.jdna.bmt.web.client.ui.util.Dialogs;
import org.jdna.bmt.web.client.ui.widgets.AbstractMouseAdapter;
import org.jdna.bmt.web.client.ui.widgets.IconAction;
import org.jdna.bmt.web.client.ui.widgets.PopupMenu;
import org.jdna.bmt.web.client.ui.widgets.PopupMenu.SimpleMenuAdapter;
import org.jdna.bmt.web.client.util.DateFormatUtil;
import org.jdna.bmt.web.client.util.MessageHandler;
import org.jdna.bmt.web.client.util.StringUtils;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ErrorEvent;
import com.google.gwt.event.dom.client.ErrorHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

public class MediaItem extends AbstractMouseAdapter implements MessageHandler {

    private static final int MENU_VIDEO_THUMB = 1;

    private static final int MENU_DELETE_FILE = 2;

    private static final int MENU_DELETE_WRONG_RECORDING = 3;

    private static final int MENU_DEBUG = 4;

    protected static final int MENU_RECORD_FAVORITE = 5;

    protected static final int MENU_RECORD_ONCE = 6;

    protected static final int MENU_SET_WATCHED = 7;

    protected static final int MENU_VIEW = 8;

    protected static final int MENU_EDIT = 9;

    protected static final int MENU_EDIT_RAW = 10;

    private GWTMediaResource res;

    private VerticalPanel vpanel = new VerticalPanel();

    private VerticalPanel titles = new VerticalPanel();

    private HorizontalPanel actions = new HorizontalPanel();

    private BrowsePanel controller;

    private Image watchedIcon;

    Image posterImage = new Image();

    public MediaItem(final GWTMediaResource res, BrowsePanel controller) {
        super();
        this.controller = controller;
        this.res = res;
        titles.setWidth("100%");
        vpanel.add(titles);
        vpanel.setWidth("150px");
        vpanel.setCellVerticalAlignment(titles, HasVerticalAlignment.ALIGN_TOP);
        vpanel.setCellHeight(titles, "10px");
        vpanel.setStyleName("MediaItem");
        setTitles(res);
        posterImage.addStyleName("MediaItem-Image");
        posterImage.addErrorHandler(new ErrorHandler() {

            public void onError(ErrorEvent event) {
                String url = posterImage.getUrl();
                GWT.log("Could not load image: " + url);
                if (res instanceof GWTMediaFolder) {
                    posterImage.setUrl("images/128x128/folder_video2.png");
                } else {
                    posterImage.setUrl("images/128x128/video2.png");
                }
                posterImage.setTitle(url);
            }
        });
        if (res instanceof GWTMediaFolder) {
            if (res.getThumbnailUrl() != null) {
                String url = GWT.getModuleBaseURL() + res.getThumbnailUrl();
                posterImage.setUrl(url);
            } else {
                posterImage.addStyleName("MediaItemPoster-Folder");
                posterImage.setUrl("images/128x128/folder_video2.png");
            }
        } else {
            posterImage.setUrl(res.getThumbnailUrl());
            actions.setSpacing(10);
            if (!(res instanceof GWTMediaFolder)) {
                watchedIcon = new Image("images/marker_watched.png");
                watchedIcon.setSize("16px", "16px");
                watchedIcon.setVisible(((GWTMediaFile) res).getIsWatched().get());
                actions.insert(watchedIcon, 0);
                if (!res.isAiring()) {
                    actions.insert(new IconAction("images/16x16/media-playback-start.png") {

                        @Override
                        protected void onClick() {
                            playFile((GWTMediaFile) res);
                        }
                    }, 0);
                }
                if (res.isAiring() && ((GWTMediaFile) res).getAiringDetails() != null && ((GWTMediaFile) res).getAiringDetails().isManualRecord()) {
                    Image img2 = new Image("images/marker_manual.png");
                    img2.setSize("16px", "16px");
                    actions.insert(img2, 0);
                }
            }
        }
        vpanel.add(posterImage);
        vpanel.setCellHorizontalAlignment(posterImage, HasHorizontalAlignment.ALIGN_CENTER);
        vpanel.setCellVerticalAlignment(posterImage, HasVerticalAlignment.ALIGN_BOTTOM);
        vpanel.add(actions);
        vpanel.setCellHorizontalAlignment(actions, HasHorizontalAlignment.ALIGN_RIGHT);
        vpanel.setCellVerticalAlignment(actions, HasVerticalAlignment.ALIGN_BOTTOM);
        vpanel.setCellHeight(actions, "20px");
        if (res instanceof GWTMediaFolder) {
            vpanel.addStyleName("MediaFolder");
        }
        if (res.getMessage() != null) {
            vpanel.setTitle(res.getMessage());
        }
        controller.getMessageBus().addHandler(BrowsePanel.MSG_POSTER_UPDATED, this);
        initWidget(vpanel);
    }

    protected void playFile(GWTMediaFile res2) {
        Dialogs.showAsDialog("Play Video", new PlayOnClientDialogPanel(res2));
    }

    private void setTitles(GWTMediaResource res2) {
        GWTAiringDetails airing = (res instanceof GWTMediaFile) ? ((GWTMediaFile) res2).getAiringDetails() : null;
        titles.clear();
        Label title1 = new Label();
        if (airing != null) {
            if (airing.getYear() > 0) {
                title1.setText(res2.getTitle() + " (" + airing.getYear() + ")");
            } else {
                title1.setText(res2.getTitle());
            }
        } else {
            title1.setText(res2.getTitle());
        }
        titles.add(title1);
        titles.setCellHorizontalAlignment(title1, HasHorizontalAlignment.ALIGN_CENTER);
        title1.setStyleName("MediaItem-Title1");
        String epTitle = res2.getMinorTitle();
        if (!StringUtils.isEmpty(epTitle) && !epTitle.equals(res.getTitle())) {
            Label title2 = new Label(epTitle);
            titles.add(title2);
            titles.setCellHorizontalAlignment(title2, HasHorizontalAlignment.ALIGN_CENTER);
            title2.setStyleName("MediaItem-Title2");
        }
        if (res2 instanceof GWTMediaFile && ((GWTMediaFile) res2).getAiringDetails() != null) {
            GWTAiringDetails det = ((GWTMediaFile) res2).getAiringDetails();
            Label title = new Label(DateFormatUtil.formatAiredDate(det.getStartTime()));
            titles.add(title);
            titles.setCellHorizontalAlignment(title, HasHorizontalAlignment.ALIGN_CENTER);
            title.setStyleName("MediaItem-Title2");
            String s = "on channel " + det.getChannel() + " (" + det.getNetwork() + ")";
            title = new Label(s);
            titles.add(title);
            titles.setCellHorizontalAlignment(title, HasHorizontalAlignment.ALIGN_CENTER);
            title.setStyleName("MediaItem-Title2");
        }
    }

    public void onClick() {
        if (res instanceof GWTMediaFolder) {
            controller.browseFolder((GWTMediaFolder) res, 0, ((GWTMediaFolder) res).getPageSize());
        } else {
            if (res.isAiring()) {
                controller.view(res);
            } else {
                controller.edit(res);
            }
        }
    }

    public void onLongPress() {
        if (res instanceof GWTMediaFile) {
            if (res.isAiring()) {
                PopupMenu pm = new PopupMenu("Select Airing Action", new PopupMenu.MenuSelectionHandler() {

                    @Override
                    public void onMenuItemSelected(Object data) {
                        int id = (Integer) ((SimpleMenuAdapter.SimpleItem) data).getId();
                        switch(id) {
                            case MENU_RECORD_FAVORITE:
                                Application.fireNotification("Favorites Not Implemented");
                                break;
                            case MENU_RECORD_ONCE:
                                controller.record((GWTMediaFile) res);
                                break;
                            case MENU_DEBUG:
                                DebugDialog.show((GWTMediaFile) res);
                                break;
                            case MENU_SET_WATCHED:
                                controller.setWatched((GWTMediaFile) res, !((GWTMediaFile) res).getIsWatched().get());
                                break;
                            case MENU_VIEW:
                                controller.view(res);
                                break;
                        }
                    }
                });
                SimpleMenuAdapter sma = (SimpleMenuAdapter) pm.getListAdapter();
                sma.addItem(MENU_RECORD_FAVORITE, "Add as Favorite");
                sma.addItem(MENU_RECORD_ONCE, "Record Once");
                sma.addItem(MENU_SET_WATCHED, "Toggle Watched");
                sma.addItem(MENU_VIEW, "View Details");
                sma.addItem(MENU_DEBUG, "View Debug Metadata Details");
                sma.fireDataChanged();
                pm.center();
                pm.show();
            } else {
                PopupMenu pm = new PopupMenu("Select MediaFile Action", new PopupMenu.MenuSelectionHandler() {

                    @Override
                    public void onMenuItemSelected(Object data) {
                        int id = (Integer) ((SimpleMenuAdapter.SimpleItem) data).getId();
                        switch(id) {
                            case MENU_DELETE_FILE:
                                if (Window.confirm("Press OK to delete this file, permanently.  It will be removed from disk.")) {
                                    removeFromParent();
                                }
                                break;
                            case MENU_DELETE_WRONG_RECORDING:
                                if (Window.confirm("Press OK to delete this file, permanently, since it was recording that was recorded incorrectly.  It will be removed from disk.")) {
                                    removeFromParent();
                                }
                                break;
                            case MENU_DEBUG:
                                DebugDialog.show((GWTMediaFile) res);
                                break;
                            case MENU_SET_WATCHED:
                                controller.setWatched((GWTMediaFile) res, !((GWTMediaFile) res).getIsWatched().get());
                                break;
                            case MENU_VIDEO_THUMB:
                                controller.showViewThumbnails((GWTMediaFile) res);
                                break;
                            case MENU_VIEW:
                                controller.view(res);
                                break;
                            case MENU_EDIT_RAW:
                                controller.editraw(res);
                                break;
                            case MENU_EDIT:
                                controller.edit(res);
                                break;
                        }
                    }
                });
                GWTMediaFile f = (GWTMediaFile) res;
                SimpleMenuAdapter sma = (SimpleMenuAdapter) pm.getListAdapter();
                sma.addItem(MENU_VIDEO_THUMB, "View Video Thumbnails");
                sma.addItem(MENU_DELETE_FILE, "Delete Media File");
                if (f.getSageRecording().get()) {
                    sma.addItem(MENU_DELETE_WRONG_RECORDING, "Delete, Wrong Recording");
                }
                sma.addItem(MENU_SET_WATCHED, "Toggle Watched");
                sma.addItem(MENU_EDIT, "Edit Details");
                sma.addItem(MENU_EDIT_RAW, "Edit Raw Metadata Details");
                sma.addItem(MENU_DEBUG, "View Debug Metadata Details");
                sma.fireDataChanged();
                pm.center();
                pm.show();
            }
        }
    }

    @Override
    protected void onAttach() {
        super.onAttach();
        controller.getMessageBus().addHandler(BrowsePanel.MSG_FILE_WATCHED, this);
    }

    @Override
    protected void onDetach() {
        super.onDetach();
        controller.getMessageBus().removeHandler(BrowsePanel.MSG_FILE_WATCHED, this);
    }

    @Override
    public void onMessageReceived(String msg, Map<String, ?> args) {
        if (watchedIcon != null && BrowsePanel.MSG_FILE_WATCHED.equals(msg)) {
            GWTMediaFile f = (GWTMediaFile) args.get("file");
            boolean watched = (Boolean) args.get("watched");
            if (f.getAiringId() != null && f.getAiringId().equals(((GWTMediaFile) res).getAiringId())) {
                ((GWTMediaFile) res).getIsWatched().set(watched);
                watchedIcon.setVisible(watched);
            }
        } else if (BrowsePanel.MSG_POSTER_UPDATED.equals(msg)) {
            GWT.log("updating poster...");
            GWTMediaFile file = (GWTMediaFile) args.get("file");
            if (file != null && file.getPath().equals(res.getPath())) {
                GWT.log("updating poster for " + file.getPath());
                GWTMediaArt art = (GWTMediaArt) args.get("poster");
                if (art != null) {
                    GWT.log("Poster updated...");
                    posterImage.setUrl(art.getDisplayUrl());
                }
            }
        }
    }
}
