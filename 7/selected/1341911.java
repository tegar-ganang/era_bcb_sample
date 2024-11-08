package com.memoire.bu;

import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import javax.swing.Icon;
import javax.swing.JInternalFrame;
import com.memoire.fu.FuLog;

public class BuMenuInternalFrames extends BuDynamicMenu implements ContainerListener {

    private int number_;

    private boolean valid_;

    private BuDesktop desktop_;

    public BuMenuInternalFrames() {
        super(__("Liste"), "LISTE_FENETRES");
    }

    public BuDesktop getDesktop() {
        return desktop_;
    }

    public void setDesktop(BuDesktop _desktop) {
        if (desktop_ != null) desktop_.removeContainerListener(this);
        desktop_ = _desktop;
        if (desktop_ != null) desktop_.addContainerListener(this);
        valid_ = false;
        number_ = (desktop_ == null) ? 0 : desktop_.getAllFrames().length;
    }

    public void componentAdded(ContainerEvent _evt) {
        valid_ = false;
        number_ = (desktop_ == null) ? 0 : desktop_.getAllFrames().length;
    }

    public void componentRemoved(ContainerEvent _evt) {
        valid_ = false;
        number_ = (desktop_ == null) ? 0 : desktop_.getAllFrames().length;
    }

    protected void build() {
        if (isPopupMenuVisible() || valid_) return;
        FuLog.debug("BMT: build menu internal frames");
        valid_ = true;
        removeAll();
        if (desktop_ == null) {
            number_ = 0;
            return;
        }
        JInternalFrame[] frames = desktop_.getAllFrames();
        number_ = frames.length;
        for (int i = 0; i < frames.length - 1; i++) {
            String t0 = frames[i].getTitle();
            String t1 = frames[i + 1].getTitle();
            if (t0 == null) System.err.println("No title for " + frames[i].getName());
            if ((t0 != null) && (t1 != null) && (t1.compareTo(t0) < 0)) {
                JInternalFrame tmp = frames[i];
                frames[i] = frames[i + 1];
                frames[i + 1] = tmp;
                i -= 2;
                if (i < 0) i = -1;
            }
        }
        for (int i = 0; i < frames.length; i++) {
            JInternalFrame f = frames[i];
            String n = f.getName();
            if ((n != null) && (f.getClientProperty("JInternalFrame.isPalette") != Boolean.TRUE)) {
                if (n.startsWith("if")) n = n.substring(2);
                BuMenuItem mi = addMenuItem(f.getTitle(), "FILLE_ACTIVER(" + n + ")", true, BuInternalFrame.getShortcut(f));
                Icon icon = f.getFrameIcon();
                if (icon instanceof BuIcon) mi.setIcon(BuResource.BU.reduceMenuIcon((BuIcon) icon)); else mi.setIcon(icon);
            }
        }
        computeMnemonics();
    }

    public boolean isActive() {
        return (desktop_ != null) && (number_ > 0);
    }
}
