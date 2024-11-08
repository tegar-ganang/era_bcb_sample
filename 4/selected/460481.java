package com.langerra.client;

import java.util.Date;
import com.langerra.client.channel.ChannelServiceFactory;
import com.langerra.client.channel.rpc.RemoteChannelService;
import com.langerra.client.channel.rpc.RemoteChannelServiceAsync;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Gwt_channel implements EntryPoint {

    /**
	 * This is the entry point method.
	 */
    public void onModuleLoad() {
        final DialogBox dialogBox = new DialogBox();
        dialogBox.setText("Remote Procedure Call");
        dialogBox.setAnimationEnabled(true);
        final Label textToServerLabel = new Label();
        final HTML serverResponseLabel = new HTML();
        VerticalPanel dialogVPanel = new VerticalPanel();
        dialogVPanel.addStyleName("dialogVPanel");
        dialogVPanel.add(new HTML("<b>Sending name to the server:</b>"));
        dialogVPanel.add(textToServerLabel);
        dialogVPanel.add(new HTML("<br><b>Server replies:</b>"));
        dialogVPanel.add(serverResponseLabel);
        dialogVPanel.setHorizontalAlignment(VerticalPanel.ALIGN_RIGHT);
        dialogBox.setWidget(dialogVPanel);
        ChannelServiceFactory.getChannelService(GWT.<RemoteChannelServiceAsync>create(RemoteChannelService.class)).<String>getChannel("gwt-channel", false).async(new AsyncCallback<String>() {

            int totalMessage = 0;

            @Override
            public void onSuccess(String result) {
                dialogBox.setText("Remote Procedure Call");
                serverResponseLabel.removeStyleName("serverResponseLabelError");
                serverResponseLabel.setHTML("Server time was " + result.toString() + "<br/>" + "Current time is " + new Date() + "<br/>" + "Have received " + totalMessage++ + " messaage from the server");
                dialogBox.center();
            }

            @Override
            public void onFailure(Throwable caught) {
            }
        });
    }

    private static native void eval(String code);
}
