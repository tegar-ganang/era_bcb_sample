package net.bnubot.bot.gui.colors;

import java.awt.Color;

/**
 * @author scotta
 */
public class StarcraftColorScheme extends ColorScheme {

    public static final Color SCWhite = Color.WHITE;

    public static final Color SCRed = Color.RED;

    public static final Color SCGreen = Color.GREEN;

    public static final Color SCBlue = Color.BLUE;

    public static final Color SCLtGray = Color.LIGHT_GRAY;

    public static final Color SCGray = Color.GRAY;

    public static final Color SCBlack = Color.BLACK;

    public static final Color SCMagenta = Color.MAGENTA;

    public static final Color SCCyan = Color.CYAN;

    public static final Color SCYellow = Color.YELLOW;

    @Override
    public Color getBackgroundColor() {
        return SCBlack;
    }

    @Override
    public Color getForegroundColor() {
        return SCLtGray;
    }

    @Override
    public Color getChannelColor() {
        return SCGreen;
    }

    @Override
    public Color getInfoColor() {
        return SCBlue;
    }

    @Override
    public Color getErrorColor() {
        return SCRed;
    }

    @Override
    public Color getDebugColor() {
        return SCYellow;
    }

    @Override
    public Color getUserNameColor(int flags) {
        if ((flags & 0x20) != 0) return SCRed;
        if ((flags & 0x01) != 0) return SCCyan;
        if ((flags & 0x08) != 0) return SCGreen;
        if ((flags & 0x02) != 0) return SCWhite;
        if ((flags & 0x04) != 0) return SCYellow;
        if ((flags & 0x40) != 0) return SCMagenta;
        return SCYellow;
    }

    @Override
    public Color getSelfUserNameColor(int flags) {
        return SCCyan;
    }

    @Override
    public Color getUserNameListColor(int flags, boolean myUser) {
        if (myUser) return SCYellow;
        if ((flags & 0x20) != 0) return SCRed;
        if ((flags & 0x01) != 0) return SCCyan;
        if ((flags & 0x08) != 0) return SCGreen;
        if ((flags & 0x02) != 0) return SCWhite;
        if ((flags & 0x04) != 0) return SCYellow;
        if ((flags & 0x40) != 0) return SCMagenta;
        return SCLtGray;
    }

    @Override
    public Color getChatColor(int flags) {
        if ((flags & 0x20) != 0) return SCGray;
        if ((flags & 0x01) != 0) return SCCyan;
        if ((flags & 0x08) != 0) return SCGreen;
        if ((flags & 0x02) != 0) return SCWhite;
        if ((flags & 0x04) != 0) return SCYellow;
        if ((flags & 0x40) != 0) return SCMagenta;
        return SCWhite;
    }

    @Override
    public Color getEmoteColor(int flags) {
        if ((flags & 0x01) != 0) return SCCyan;
        if ((flags & 0x08) != 0) return SCGreen;
        if ((flags & 0x02) != 0) return SCWhite;
        if ((flags & 0x04) != 0) return SCYellow;
        if ((flags & 0x40) != 0) return SCMagenta;
        return SCYellow;
    }

    @Override
    public Color getWhisperColor(int flags) {
        return SCGray;
    }
}
