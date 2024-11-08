package cz.cacek.ebook;

import java.io.EOFException;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/**
 * Implementation of ebook content screen.
 *
 * @author Tom� Darmovzal [tomas.darmovzal (at) seznam.cz]
 * @author Josef Cacek [josef.cacek (at) atlas.cz]
 * @author $Author: kwart $
 * @version $Revision: 1.12 $
 * @created $Date: 2007/08/30 11:25:22 $
 */
public class ContentView {

    private int width;

    private int height;

    private int widthOrig;

    private int heightOrig;

    private int background;

    private int foreground;

    private Font font;

    private StringBuffer buffer;

    private int borderSpace;

    private int lineSpace;

    private boolean wrapSpaces;

    private boolean rotated;

    private Book book;

    private int position;

    private int scrollWidth;

    private int scrollHeight;

    private int[] charWidths;

    private Image offscreen;

    private Image origscreen;

    private int colorScheme = 0;

    private Screen screen;

    /**
	 * Class Screen holds lines which are currently displayed on the screen.
	 * Class is synchronized.<p/> Instances of this class are created during
	 * LCD-screen font change.
	 *
	 * @author Josef Cacek [josef.cacek (at) atlas.cz]
	 * @author $Author: kwart $
	 * @version $Revision: 1.12 $
	 * @created $Date: 2007/08/30 11:25:22 $
	 */
    static class Screen {

        private int positions[];

        private String content[];

        /**
		 * Creates new screen object
		 *
		 * @param aLines
		 */
        public Screen(int aLines) {
            positions = new int[aLines + 1];
            content = new String[aLines];
        }

        /**
		 * Returns count of rows displayed on screen.
		 *
		 * @return count of rows displayed on screen
		 */
        public synchronized int size() {
            return content.length;
        }

        /**
		 * Returns position (in book) of first character displayed on given
		 * line.
		 *
		 * @param aLine
		 *            line for which is position returned
		 * @return position of line in book
		 */
        public synchronized int getPosition(int aLine) {
            return positions[aLine];
        }

        /**
		 * Sets position of line
		 *
		 * @param aLine
		 * @param aPos
		 */
        public synchronized void setPosition(int aLine, int aPos) {
            positions[aLine] = aPos;
        }

        /**
		 * Returns string displayed in given row.
		 *
		 * @param aLine
		 *            index of row
		 * @return string displayed in given row
		 */
        public synchronized String getContent(int aLine) {
            return content[aLine];
        }

        /**
		 * Sets string to display in given line
		 *
		 * @param aLine
		 *            index of row
		 * @param aStr
		 */
        public synchronized void setContent(int aLine, String aStr) {
            content[aLine] = aStr;
        }

        /**
		 * Rolls positions and content forward and adds new line. First
		 * displayed line is deleted (rolled out). to the end.
		 *
		 * @param aLine
		 *            new line
		 * @param aNewPosition
		 *            position of new line (position in book)
		 */
        synchronized void rollFw(String aLine, int aNewPosition) {
            for (int i = 0; i < content.length - 1; i++) {
                content[i] = content[i + 1];
                positions[i] = positions[i + 1];
            }
            positions[content.length - 1] = positions[content.length];
            content[content.length - 1] = aLine;
            positions[positions.length - 1] = aNewPosition;
        }

        /**
		 * Rolls backward, new line (given as parameter) is added to the
		 * beginning.
		 *
		 * @param aLine
		 *            new line
		 * @param aNewPosition
		 * @see #rollFw(String, int)
		 */
        synchronized void rollBw(String aLine, int aNewPosition) {
            positions[content.length] = positions[content.length - 1];
            for (int i = content.length - 1; i > 0; i--) {
                content[i] = content[i - 1];
                positions[i] = positions[i - 1];
            }
            content[0] = aLine;
            positions[0] = aNewPosition;
        }
    }

    /**
	 * Constructor
	 *
	 * @param aWidth
	 * @param aHeight
	 * @throws Exception
	 */
    public ContentView(SettingBean settings, int aWidth, int aHeight) throws Exception {
        setDimension(aWidth, aHeight);
        buffer = new StringBuffer(256);
        borderSpace = 2;
        lineSpace = 0;
        wrapSpaces = true;
        scrollWidth = 5;
        scrollHeight = 5;
        charWidths = new int[256];
        setColors(0xFFFFFF, 0x000000);
        setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        initSetting(settings);
    }

    public void initSetting(SettingBean settings) throws Exception {
        this.setColorScheme(settings.getColorScheme());
        this.setWrapSpaces(settings.isWrapSpaces());
        this.setRotated(settings.isRotated());
        this.setFont(settings.getFont());
        int tmpLines = (height - (2 * borderSpace)) / (font.getHeight() + lineSpace);
        screen = new Screen(tmpLines);
        for (int i = 0; i < charWidths.length; i++) {
            charWidths[i] = font.charWidth((char) i);
        }
        if (book != null) {
            fillPage();
        }
        scaleScreen();
    }

    protected void setDimension(int aWidth, int aHeight) {
        Utils.debug("Setting screen w/h: " + aWidth + "/" + aHeight);
        widthOrig = aWidth;
        heightOrig = aHeight;
        scaleScreen();
    }

    protected void scaleScreen() {
        Utils.debug("Scale - isRotated: " + rotated);
        if (this.isRotated()) {
            width = heightOrig;
            height = widthOrig;
            origscreen = Image.createImage(widthOrig, heightOrig);
        } else {
            width = widthOrig;
            height = heightOrig;
        }
        offscreen = Image.createImage(width, height);
    }

    /**
	 * Sets active book for view.
	 *
	 * @param aBook
	 */
    public void setBook(Book aBook) {
        book = aBook;
        setPosition(book.getPosition());
    }

    /**
	 * Sets font for view
	 *
	 * @param aFont
	 * @throws Exception
	 */
    private void setFont(Font aFont) throws Exception {
        font = aFont;
    }

    /**
	 * Returns font
	 *
	 * @return font
	 */
    public Font getFont() {
        return font;
    }

    /**
	 * Sets color pair as in given scheme
	 *
	 * @param aSchemeNr
	 */
    private void setColorScheme(int aSchemeNr) {
        colorScheme = aSchemeNr;
        switch(colorScheme) {
            case 1:
                setColors(0xFFFFFF, 0x0000FF);
                break;
            case 2:
                setColors(0x000000, 0x00FF00);
                break;
            case 3:
                setColors(0x0000A0, 0xFFFFFF);
                break;
            default:
                setColors(0xFFFFFF, 0x000000);
                break;
        }
    }

    /**
	 * Sets FG/BG colors for view
	 *
	 * @param aBG
	 * @param aFG
	 */
    private void setColors(int aBG, int aFG) {
        background = aBG;
        foreground = aFG;
    }

    /**
	 * Returns foreground color.
	 *
	 * @return foreground color
	 */
    public int getForegroundColor() {
        return foreground;
    }

    /**
	 * Returns background color.
	 *
	 * @return background color
	 */
    public int getBackgroundColor() {
        return background;
    }

    /**
	 * Sets position in active book in characters
	 *
	 * @param aPos
	 */
    public void setPosition(int aPos) {
        Utils.debug("View.setPosition(aPos) started");
        if (aPos > book.size) {
            aPos = book.size - 1;
        }
        if (aPos < 0) {
            aPos = 0;
        }
        position = aPos;
        book.setPosition(aPos);
        fillPage();
    }

    /**
	 * Sets position in active book in percents
	 *
	 * @param aPerc
	 */
    public void setPercPosition(int aPerc) {
        setPosition((book.size - 1) * aPerc / 100);
    }

    /**
	 * Returns current position as a percentige of book.
	 *
	 * @return current position as a percentige of book
	 */
    public int getPercPosition() {
        if (book.size < 2) {
            return 0;
        }
        return position * 100 / (book.size - 1);
    }

    /**
	 * Moves view one page ahead
	 *
	 * @throws Exception
	 */
    public void fwdPage() throws Exception {
        position = screen.getPosition(screen.size());
        fillPage();
    }

    /**
	 * Moves view one page back
	 *
	 * @throws Exception
	 */
    public void bckPage() throws Exception {
        for (int i = 0, n = screen.size(); i < n; i++) {
            bckLine();
        }
    }

    /**
	 * Moves view one line ahead
	 *
	 * @throws Exception
	 * @return true if scrolling is succesfull
	 */
    public synchronized boolean fwdLine() throws Exception {
        Utils.debug("fwdLine() started");
        book.setPosition(screen.getPosition(screen.size()));
        final String tmpLine = nextLine();
        final boolean tmpResult = tmpLine != null;
        if (tmpResult) {
            screen.rollFw(tmpLine, book.getPosition());
            position = screen.getPosition(0);
        }
        Utils.debug("fwdLine() finished (" + tmpResult + ")");
        return tmpResult;
    }

    /**
	 * Moves view one line back
	 *
	 * @throws Exception
	 */
    public synchronized void bckLine() throws Exception {
        if (Utils.DEBUG) {
            Utils.debug("bckLine() started");
        }
        book.setPosition(screen.getPosition(0) - 1);
        String line = prevLine();
        if (line != null) {
            screen.rollBw(line, book.getPosition() + 1);
            position = screen.getPosition(0);
        }
    }

    /**
	 * fills page from current position
	 */
    public void fillPage() {
        if (Utils.DEBUG) {
            Utils.debug("fillPage() started");
        }
        book.setPosition(position);
        screen.setPosition(0, position);
        try {
            for (int i = 0, n = screen.size(); i < n; i++) {
                screen.setContent(i, nextLine());
                screen.setPosition(i + 1, book.getPosition());
            }
        } catch (Exception e) {
            if (Utils.ERROR) {
                Utils.error(" fillPage() failed:" + e.getMessage(), e);
            }
            throw new RuntimeException(e.getMessage());
        }
        Utils.debug("fillPage() finished");
    }

    /**
	 * Reads and returns next line for view.
	 *
	 * @return next line
	 * @throws Exception
	 */
    protected String nextLine() throws Exception {
        if (book.getPosition() >= book.size - 1) {
            return null;
        }
        int len = 0;
        int ws = -1;
        int index = 0;
        boolean eof = false;
        buffer.setLength(0);
        for (; ; ) {
            char c;
            try {
                c = book.readNext();
            } catch (EOFException e) {
                eof = true;
                break;
            }
            if (c == '\t') c = ' ';
            if (c == '\r' || (c == ' ' && index == 0)) continue;
            if (c == '\n') break;
            if (c == ' ') ws = index;
            len += charWidth(c);
            if (len > width - (2 * borderSpace) - scrollWidth) {
                book.readPrev();
                if ((ws != -1) && this.isWrapSpaces()) {
                    int discard = index - ws - 1;
                    for (int i = 0; i < discard; i++) {
                        book.readPrev();
                    }
                    index = ws;
                    buffer.setLength(index);
                }
                break;
            }
            buffer.append(c);
            index++;
        }
        if (eof && index == 0) {
            return null;
        }
        if (book.isRightToLeft()) {
            buffer.reverse();
        }
        return (buffer.toString()).trim();
    }

    /**
	 * Reads and returns previous line. (backward reading)
	 *
	 * @return previous line
	 * @throws Exception
	 */
    protected String prevLine() throws Exception {
        if (book.getPosition() <= 0) return null;
        int len = 0;
        int ws = -1;
        int index = 0;
        boolean eof = false;
        buffer.setLength(0);
        for (; ; ) {
            char c;
            try {
                c = book.readPrev();
            } catch (EOFException e) {
                eof = true;
                break;
            }
            if (c == '\t') c = ' ';
            if (c == '\r' || (c == ' ' && index == 0)) continue;
            if (c == '\n') break;
            if (c == ' ') ws = index;
            len += charWidth(c);
            if (len > width - (2 * borderSpace) - scrollWidth) {
                book.readNext();
                if ((ws != -1) && this.isWrapSpaces()) {
                    int discard = index - ws - 1;
                    for (int i = 0; i < discard; i++) {
                        book.readNext();
                    }
                    index = ws;
                    buffer.setLength(index);
                }
                break;
            }
            buffer.append(c);
            index++;
        }
        if (eof && index == 0) {
            return null;
        }
        if (!book.isRightToLeft()) {
            buffer.reverse();
        }
        return (buffer.toString()).trim();
    }

    /**
	 * returns width of given character for current font
	 *
	 * @param aChr
	 *            character
	 * @return width of given character
	 */
    protected int charWidth(char aChr) {
        return (aChr < 256) ? charWidths[aChr] : font.charWidth(aChr);
    }

    /**
	 * Draw current view to display.
	 *
	 * @param aGraphic
	 * @param aX
	 * @param aY
	 */
    public void draw(Graphics aGraphic, int aX, int aY) {
        Graphics g = offscreen.getGraphics();
        g.setColor(background);
        g.fillRect(0, 0, width, height);
        g.setColor(foreground);
        g.setFont(font);
        int pos = 0;
        int anchor, xPos;
        if (book.isRightToLeft()) {
            anchor = Graphics.RIGHT | Graphics.TOP;
            xPos = width - scrollWidth - borderSpace;
        } else {
            anchor = Graphics.LEFT | Graphics.TOP;
            xPos = borderSpace;
        }
        for (int i = 0, n = screen.size(); i < n; i++) {
            String line = screen.getContent(i);
            if (line != null) {
                g.drawString(line, xPos, borderSpace + pos, anchor);
            }
            pos += font.getHeight() + lineSpace;
        }
        g.setColor(foreground);
        g.drawRect(0, 0, width - 1, height - 1);
        g.setColor(background);
        g.fillRect(width - scrollWidth, 0, scrollWidth - 1, height - 1);
        g.setColor(foreground);
        g.drawRect(width - scrollWidth, 0, scrollWidth - 1, height - 1);
        int scroll = (height - scrollHeight) * book.getPosition() / book.size;
        g.fillRect(width - scrollWidth, scroll, scrollWidth - 1, scrollHeight - 1);
        if (this.isRotated()) {
            final Graphics g2 = origscreen.getGraphics();
            for (int i = 0; i < widthOrig; i++) {
                for (int j = 0; j < heightOrig; j++) {
                    g2.setClip(i, j, 1, 1);
                    g2.drawImage(offscreen, i - j, j + i - widthOrig, Graphics.TOP | Graphics.LEFT);
                }
            }
        } else {
            origscreen = offscreen;
        }
        aGraphic.drawImage(origscreen, aX, aY, Graphics.LEFT | Graphics.TOP);
    }

    /**
	 * Returns position of first character on screen of current view.
	 *
	 * @return current position of view
	 */
    public int getPosition() {
        return screen == null ? 0 : screen.getPosition(0);
    }

    public boolean isWrapSpaces() {
        return wrapSpaces;
    }

    private void setWrapSpaces(boolean wrapSpaces) {
        this.wrapSpaces = wrapSpaces;
    }

    public boolean isRotated() {
        return rotated;
    }

    private void setRotated(boolean rotated) {
        this.rotated = rotated;
    }

    public int getColorScheme() {
        return colorScheme;
    }

    public Book getBook() {
        return book;
    }
}
