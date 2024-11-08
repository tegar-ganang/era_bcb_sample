package fr.fg.client.core.login;

import java.util.ArrayList;
import java.util.HashMap;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.Widget;
import fr.fg.client.ajax.Action;
import fr.fg.client.ajax.ActionCallback;
import fr.fg.client.data.ServerData;
import fr.fg.client.i18n.StaticMessages;
import fr.fg.client.openjwt.core.Config;
import fr.fg.client.openjwt.ui.JSButton;
import fr.fg.client.openjwt.ui.JSComboBox;
import fr.fg.client.openjwt.ui.JSDialog;
import fr.fg.client.openjwt.ui.JSLabel;
import fr.fg.client.openjwt.ui.JSPasswordField;
import fr.fg.client.openjwt.ui.JSRowLayout;
import fr.fg.client.openjwt.ui.JSTextField;
import fr.fg.client.openjwt.ui.SelectionListener;

public class LoginDialog extends JSDialog implements SelectionListener, ClickListener, KeyboardListener {

    private JSComboBox galaxyComboBox;

    private JSTextField loginField;

    private JSPasswordField passwordField;

    private JSButton loginBt, registerBt;

    private JSLabel forgottenPasswordLabel;

    private Action loginAction;

    private ActionCallback callback;

    public LoginDialog(ActionCallback callback) {
        super(((StaticMessages) GWT.create(StaticMessages.class)).gameConnection(), false, true, false);
        this.callback = callback;
        StaticMessages messages = (StaticMessages) GWT.create(StaticMessages.class);
        JSLabel galaxyLabel = new JSLabel("&nbsp;" + messages.galaxy());
        galaxyLabel.setPixelWidth(100);
        galaxyComboBox = new JSComboBox();
        galaxyComboBox.setPixelWidth(200);
        JSLabel loginLabel = new JSLabel("&nbsp;" + messages.login());
        loginLabel.setPixelWidth(100);
        loginField = new JSTextField();
        loginField.setPixelWidth(200);
        if (Cookies.getCookie("login") != null) loginField.setText(Cookies.getCookie("login"));
        JSLabel passwordLabel = new JSLabel("&nbsp;" + messages.password());
        passwordLabel.setPixelWidth(100);
        passwordField = new JSPasswordField();
        passwordField.setPixelWidth(200);
        passwordField.addKeyboardListener(this);
        forgottenPasswordLabel = new JSLabel("<a unselectable=\"on\">" + messages.passwordForgotten() + "</a>");
        forgottenPasswordLabel.setPixelWidth(300);
        forgottenPasswordLabel.setAlignment(JSLabel.ALIGN_CENTER);
        loginBt = new JSButton(messages.connect());
        loginBt.setPixelWidth(100);
        loginBt.addClickListener(this);
        registerBt = new JSButton(messages.register());
        registerBt.setPixelWidth(100);
        registerBt.addClickListener(this);
        JSRowLayout layout = new JSRowLayout();
        layout.addComponent(galaxyLabel);
        layout.addComponent(galaxyComboBox);
        layout.addRow();
        layout.addComponent(loginLabel);
        layout.addComponent(loginField);
        layout.addRow();
        layout.addComponent(passwordLabel);
        layout.addComponent(passwordField);
        layout.addRow();
        layout.addComponent(forgottenPasswordLabel);
        layout.addRowSeparator(5);
        layout.addComponent(loginBt);
        layout.addComponent(registerBt);
        layout.setRowAlignment(JSRowLayout.ALIGN_CENTER);
        setComponent(layout);
        centerOnScreen();
        sinkEvents(Event.ONCLICK);
    }

    public void setServers(ArrayList<ServerData> servers, int currentServerIndex) {
        galaxyComboBox.removeSelectionListener(this);
        ArrayList<ServerUI> serversUI = new ArrayList<ServerUI>();
        for (ServerData server : servers) serversUI.add(new ServerUI(server));
        galaxyComboBox.setItems(serversUI);
        galaxyComboBox.setSelectedIndex(currentServerIndex);
        galaxyComboBox.addSelectionListener(this);
    }

    public void selectionChanged(Widget sender, int newValue, int oldValue) {
        if (sender == galaxyComboBox) {
            Window.Location.assign(((ServerUI) galaxyComboBox.getSelectedItem()).getData().getUrl());
        }
    }

    public void onClick(Widget sender) {
        if (sender == loginBt) {
            login();
        } else if (sender == registerBt) {
            setVisible(false);
            RegisterDialog dialog = new RegisterDialog(this);
            dialog.setVisible(true);
        }
    }

    @Override
    public void onBrowserEvent(Event event) {
        super.onBrowserEvent(event);
        switch(event.getTypeInt()) {
            case Event.ONCLICK:
                if (forgottenPasswordLabel.getElement().isOrHasChild(event.getTarget())) {
                    setVisible(false);
                    RecoverPasswordDialog dialog = new RecoverPasswordDialog(this);
                    dialog.setVisible(true);
                }
                break;
        }
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            if (Cookies.getCookie("login") != null) passwordField.setFocus(true); else loginField.setFocus(true);
        } else {
            passwordField.setText("");
            loginField.setFocus(false);
            passwordField.setFocus(false);
        }
    }

    public void onKeyDown(Widget sender, char keyCode, int modifiers) {
    }

    public void onKeyPress(Widget sender, char keyCode, int modifiers) {
    }

    public void onKeyUp(Widget sender, char keyCode, int modifiers) {
        switch(keyCode) {
            case 3:
            case 13:
                login();
                break;
        }
    }

    private void login() {
        if (loginAction != null && loginAction.isPending()) return;
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("login", loginField.getText());
        params.put("password", passwordField.getText());
        loginAction = new Action("login", params, callback);
    }

    private class ServerUI {

        private ServerData data;

        public ServerUI(ServerData data) {
            this.data = data;
        }

        public ServerData getData() {
            return data;
        }

        public String toString() {
            return data.getName() + "  <img src=\"" + Config.getMediaUrl() + "images/flags/" + data.getLanguage() + ".png\" alt=\"" + data.getLanguage() + "\" style=\"margin-bottom: -1px;\">";
        }
    }
}
