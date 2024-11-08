package shared;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;
import org.eclipse.swt.widgets.Tree;
import org.pircbotx.Channel;
import org.pircbotx.Colors;
import ui.composites.MainWindow;
import ui.room.Room;
import connection.Connection;
import connection.KEllyBot;
import connection.Settings;

/**
 * The Class RoomManager.
 */
public class RoomManager {

    /**
	 * Gets the main.
	 *
	 * @return the main
	 */
    public static MainWindow getMain() {
        return main;
    }

    /** The main. */
    private static MainWindow main;

    /** The colorset. */
    public static Customs colorset = new Customs();

    /**
	 * En queue.
	 *
	 * @param mes the mes
	 */
    public static void enQueue(Message mes) {
        filterMessage(mes);
    }

    /**
	 * Creates the room.
	 *
	 * @param c the c
	 * @param tree the tree
	 * @param style the style
	 * @param channelstr the channelstr
	 * @param newConnection the new connection
	 * @param layout the layout
	 * @param channel the channel
	 */
    public static void createRoom(final Composite c, final Tree tree, final int style, final String channelstr, final Connection newConnection, final int layout, final Channel channel) {
        if (!main.getDisplay().isDisposed()) {
            if (canAddRoom(newConnection, channelstr)) {
                main.getDisplay().asyncExec(new Runnable() {

                    public void run() {
                        Room r = new Room(c, style, layout, tree, channelstr, newConnection, channel);
                        newConnection.addRoom(r);
                    }
                });
            }
        }
    }

    /**
	 * Sets the main.
	 *
	 * @param w the new main
	 */
    public static void setMain(MainWindow w) {
        main = w;
        initTray();
    }

    /**
	 * Initializes the tray icon.
	 */
    private static void initTray() {
        Tray sysTray = main.getDisplay().getSystemTray();
        if (sysTray != null) {
            TrayItem item = new TrayItem(sysTray, SWT.NONE);
            item.setToolTipText(KEllyBot.VERSION);
            try {
                Image image = new Image(main.getDisplay(), "icon.png");
                item.setImage(image);
            } catch (Exception e) {
                Logger log = Logger.getLogger("logs.init");
                log.log(Level.WARNING, "icon.png not found");
            }
            item.addSelectionListener(new SelectionListener() {

                @Override
                public void widgetDefaultSelected(SelectionEvent arg0) {
                }

                @Override
                public void widgetSelected(SelectionEvent arg0) {
                    if (Settings.getSettings().isMinimizeTray()) {
                        main.getParent().setVisible(!main.getParent().isVisible());
                    }
                }
            });
        }
    }

    /**
	 * Can add room.
	 *
	 * @param newConnection the new connection
	 * @param s the s
	 * @return true, if successful
	 */
    public static boolean canAddRoom(Connection newConnection, String s) {
        return newConnection.canAddRoom(s);
    }

    /**
	 * Filter message.
	 *
	 * @param m the m
	 */
    private static void filterMessage(final Message m) {
        if (!main.getDisplay().isDisposed()) {
            main.getDisplay().asyncExec(new Runnable() {

                public void run() {
                    Room r = m.getBot().getConnection().findRoom(m.getChannel());
                    if (r == null) return;
                    if (Settings.getSettings().getNicksIgnored().contains(m.getSender())) return;
                    String formattedMsg;
                    if (m.getType() == Message.ACTION) formattedMsg = Settings.getSettings().getActionFormat(); else formattedMsg = Settings.getSettings().getMessageFormat();
                    formattedMsg = formattedMsg.replaceAll("%chan%", m.getChannel());
                    formattedMsg = formattedMsg.replaceAll("%msg%", m.getContent());
                    formattedMsg = formattedMsg.replaceAll("%nick%", m.getSender());
                    SimpleDateFormat sdf = new SimpleDateFormat(Settings.getSettings().getTimestampFormatPattern());
                    if (Settings.getSettings().isTimestampsEnabled()) formattedMsg = formattedMsg.replaceAll("%time%", sdf.format(m.getDate())); else formattedMsg = formattedMsg.replaceAll("%time%", "");
                    if (m.getType() != Message.MSG) {
                        String colorStr = Settings.getSettings().getOutputColors().get(m.getType());
                        formattedMsg = colorset.ircColorsStr.get(colorStr) + formattedMsg;
                    }
                    String strippedLine = Colors.removeFormattingAndColors(formattedMsg);
                    if (r.getOutput() != null) {
                        int scrollPos = r.getOutput().getTopPixel();
                        int ySize = r.getOutput().getBounds().height;
                        boolean scrollDown = (scrollPos > (r.getOutput().getVerticalBar().getMaximum() - ySize));
                        switch(m.getType()) {
                            case Message.ACTION:
                            case Message.MSG:
                            case Message.PM:
                            case Message.NOTICE:
                                sendMessageToRoom(m, r, strippedLine);
                                break;
                            case Message.CONSOLE:
                                r.newMessage(strippedLine);
                                r.changeStatus(Room.NEW_IRC_EVENT);
                                break;
                        }
                        if (scrollDown) r.getOutput().setSelection(r.getOutput().getText().length());
                    }
                    List<StyleRange> styleRanges = ControlCodeParser.parseControlCodes(formattedMsg, r.getOutput().getText().length() - strippedLine.length());
                    for (StyleRange styleRange : styleRanges.toArray(new StyleRange[styleRanges.size()])) r.getOutput().setStyleRange(styleRange);
                    for (String s : strippedLine.split(" ")) {
                        if (s.contains("://") || Quicklinks.hasQuicklink(s)) {
                            linkify(r, strippedLine, s);
                        }
                    }
                }

                private void sendMessageToRoom(final Message m, Room r, String strippedLine) {
                    r.newMessage(strippedLine, true);
                    if (strippedLine.toLowerCase().contains(m.getBot().getNick().toLowerCase())) r.changeStatus(Room.NAME_CALLED); else r.changeStatus(Room.NEW_MESSAGE);
                }

                private void linkify(Room r, String strippedLine, String s) {
                    Color blue = new Color(r.getOutput().getDisplay(), 0, 0, 255);
                    StyleRange styleRange = new StyleRange();
                    styleRange.start = r.getOutput().getCharCount() - strippedLine.length() + strippedLine.indexOf(s);
                    styleRange.length = s.length();
                    styleRange.foreground = blue;
                    styleRange.data = s;
                    styleRange.underline = true;
                    styleRange.underlineStyle = SWT.UNDERLINE_LINK;
                    r.getOutput().setStyleRange(styleRange);
                }
            });
        }
    }
}
