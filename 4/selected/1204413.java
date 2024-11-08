package symbiosis.ui;

import symbiosis.*;
import symbiosis.ui.controller.*;
import symbiosis.message.*;
import symbiosis.util.*;
import symbiosis.security.*;
import javax.swing.JComponent;

public class UIManager extends BaseManager {

    private static final int NO_SCREEN = 0;

    private static final int SPLASH_SCREEN = 1;

    private static final int LOGIN_SCREEN = 2;

    private static final int MAIN_SCREEN = 3;

    private SplashController splash = null;

    private LoginController login = null;

    private MainController main = null;

    private int currentScreen = NO_SCREEN;

    private MessageChannel consoleChannel = null;

    private Identity userIdentity = null;

    public UIManager() {
        try {
            javax.swing.UIManager.setLookAndFeel(new com.incors.plaf.kunststoff.KunststoffLookAndFeel());
        } catch (Exception e) {
        }
    }

    public void init() {
        super.init();
        consoleChannel = Symbiosis.getChannelManager().getChannel(MessageConstants.CONSOLE_CHANNEL);
        consoleChannel.addMessageListener(this);
        splash = new SplashController();
        splash.start();
        currentScreen = SPLASH_SCREEN;
    }

    public void start() {
        super.start();
        splash.hide();
        login = new LoginController(this);
        login.start();
        currentScreen = LOGIN_SCREEN;
    }

    public void loginApproved(Identity userIdentity) {
        this.userIdentity = userIdentity;
        login.hide();
        main = new MainController(this);
        main.start();
        currentScreen = MAIN_SCREEN;
    }

    public void removeTab(JComponent component) {
        if (currentScreen == MAIN_SCREEN) {
            main.removeTab(component);
        }
    }

    public void handleSymMessage(SymMessage message) {
        if (message.getType().startsWith(MessageConstants.CONSOLE_MESSAGE)) {
            switch(currentScreen) {
                case NO_SCREEN:
                    break;
                case SPLASH_SCREEN:
                    splash.setStatus((String) message.getPayload());
                    break;
                case LOGIN_SCREEN:
                    login.setStatus((String) message.getPayload());
                    break;
                case MAIN_SCREEN:
                    main.setStatus((String) message.getPayload());
                    break;
            }
        }
    }

    public Identity getUserIdentity() {
        return userIdentity;
    }
}
