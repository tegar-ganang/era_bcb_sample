package org.mhpbox.rcontrol;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.tv.graphics.TVContainer;
import org.havi.ui.event.HRcEvent;
import org.mhpbox.application.DVBJProxyImpl;
import org.mhpbox.appman.XletContextImpl;
import org.mhpbox.infra.AbstractSTB;
import org.mhpbox.infra.TVControls;
import org.mhpbox.ui.CommandListener;

public class DVBJAppState extends RemoteControlHandlerState {

    private DVBJProxyImpl proxy;

    protected TVControls tvControls;

    protected Window window;

    public DVBJAppState(DVBJProxyImpl prx) {
        this.proxy = prx;
        this.tvControls = AbstractSTB.getInstance().getTVControls();
        Container parent = AbstractSTB.getInstance().getVideoStack().getRootComponent().getParent();
        while (parent != null && !(parent instanceof Window)) {
            parent = parent.getParent();
        }
        this.window = (Window) parent;
    }

    private int toHaviCode(int code) {
        if (code >= ButtonPressedEvent.NUM0 && code <= ButtonPressedEvent.NUM9) {
            return HRcEvent.VK_0 + (code - ButtonPressedEvent.NUM0);
        }
        switch(code) {
            case ButtonPressedEvent.SK_RED:
                return HRcEvent.VK_COLORED_KEY_0;
            case ButtonPressedEvent.SK_GREEN:
                return HRcEvent.VK_COLORED_KEY_1;
            case ButtonPressedEvent.SK_YELLOW:
                return HRcEvent.VK_COLORED_KEY_2;
            case ButtonPressedEvent.SK_BLUE:
                return HRcEvent.VK_COLORED_KEY_3;
            case ButtonPressedEvent.UP:
                return HRcEvent.VK_UP;
            case ButtonPressedEvent.DOWN:
                return HRcEvent.VK_DOWN;
            case ButtonPressedEvent.LEFT:
                return HRcEvent.VK_LEFT;
            case ButtonPressedEvent.RIGHT:
                return HRcEvent.VK_RIGHT;
            case ButtonPressedEvent.OK:
                return HRcEvent.VK_ACCEPT;
        }
        return 0;
    }

    private char toCharCode(int code) {
        if (code >= ButtonPressedEvent.NUM0 && code <= ButtonPressedEvent.NUM9) {
            return (char) ('0' + (code - ButtonPressedEvent.NUM0));
        }
        return 0;
    }

    public void buttonPressed(int code) {
        if (code >= ButtonPressedEvent.NUM0 && code <= ButtonPressedEvent.NUM9) {
            int dig = code - ButtonPressedEvent.NUM0;
            tvControls.getChannelSelector().appendDigit(dig);
            return;
        }
        XletContextImpl ctx = (XletContextImpl) this.proxy.getXletContext();
        long stamp = System.currentTimeMillis();
        KeyEvent event = new KeyEvent(ctx.getContainer(), 0, stamp, 0, toHaviCode(code), toCharCode(code));
        Container cont = TVContainer.getRootContainer(ctx);
        Component[] child = cont.getComponents();
        for (int i = 0; i < child.length; i++) {
            KeyListener[] lists = child[i].getKeyListeners();
            for (int j = 0; j < lists.length; j++) {
                if (lists[j] instanceof CommandListener) {
                    lists[j].keyPressed(event);
                }
            }
        }
    }
}
