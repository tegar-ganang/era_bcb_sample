package rcm.awt;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Panel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TabPanel extends Panel {

    static final int MAX_TABS = 25;

    static final int selUpper = 2;

    static final int lineWidth = 2;

    static final int horRound = 2;

    static final int verRound = 2;

    static final int internalMargin = 4;

    static final int xTabOffset = 2;

    static final int XTitle = 4;

    static final int YTitle = 2;

    Font plainFont;

    Font boldFont;

    FontMetrics fmBoldFont;

    final Color light = new Color(223, 223, 223);

    final Color shadow = new Color(127, 127, 127);

    CardLayout tabLayout = new CardLayout();

    Panel tabPanel = new Panel();

    boolean mustRecalculate = true;

    int Z;

    int W;

    int H;

    Component cards[] = new Component[MAX_TABS];

    String arrName[] = new String[MAX_TABS];

    int arrEnd[] = new int[MAX_TABS];

    int arrBeg[] = new int[MAX_TABS];

    int nbTab = 0;

    int selected = -1;

    static final int BUTTON_GUTTER = 2;

    public TabPanel() {
        setLayout(null);
        tabPanel.setLayout(tabLayout);
        add(tabPanel);
        addMouseListener(new MouseHandler());
    }

    void recalculate() {
        plainFont = getFont();
        if (plainFont == null) plainFont = new Font("Helvetica", Font.PLAIN, 12);
        boldFont = new Font(plainFont.getFamily(), Font.BOLD, plainFont.getSize());
        fmBoldFont = getFontMetrics(boldFont);
        Z = fmBoldFont.getHeight() + 2 * lineWidth + 2 * YTitle + 1;
        for (int i = 0; i < nbTab; ++i) {
            arrBeg[i] = (i == 0) ? xTabOffset : arrEnd[i - 1];
            arrEnd[i] = arrBeg[i] + 2 * XTitle + fmBoldFont.stringWidth(arrName[i]) + 2 * lineWidth;
        }
        if (selected >= 0) {
            arrBeg[selected] -= lineWidth;
            arrEnd[selected] += lineWidth;
        }
        mustRecalculate = false;
    }

    public void layout() {
        if (mustRecalculate) recalculate();
        Dimension external = getSize();
        W = external.width;
        H = external.height;
        int x = lineWidth + internalMargin;
        int y = Z + 1 + internalMargin;
        int w = W - 2 * (lineWidth + internalMargin);
        int h = H - Z - 2 * (lineWidth + internalMargin);
        tabPanel.setBounds(x, y, w, h);
        tabPanel.validate();
        Component[] c = getComponents();
        int bx = external.width;
        for (int i = c.length - 1; i >= 0; --i) if (c[i] != tabPanel && c[i].isVisible()) {
            Dimension d = c[i].getPreferredSize();
            bx -= d.width;
            c[i].setBounds(bx, 0, d.width, d.height);
            bx -= BUTTON_GUTTER;
        }
    }

    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(d.width + 2 * (lineWidth + internalMargin), d.height + Z + 2 * (lineWidth + internalMargin));
    }

    public Dimension getMinimumSize() {
        Dimension d = super.getMinimumSize();
        return new Dimension(d.width + 2 * (lineWidth + internalMargin), d.height + Z + 2 * (lineWidth + internalMargin));
    }

    public void addTabPanel(String name, boolean enabled, Component c) {
        if (c.getParent() == tabPanel || nbTab >= arrBeg.length || c == this) return;
        cards[nbTab] = c;
        arrName[nbTab] = name;
        tabPanel.add(String.valueOf(nbTab), c);
        nbTab++;
        if (selected < 0) select(0);
        mustRecalculate = true;
        repaint();
    }

    public String[] getPanelLabels() {
        return arrName;
    }

    public void removeAllTabPanels() {
        for (int i = nbTab - 1; i >= 0; --i) removeTabPanel(i);
    }

    public void removeTabPanel(Component c) {
        for (int i = 0; i < cards.length; i++) {
            if (cards[i] == c) {
                removeTabPanel(i);
                break;
            }
        }
    }

    public void removeTabPanel(int i) {
        if (i < 0 || i >= nbTab) return;
        tabPanel.remove(cards[i]);
        for (int j = i; j < nbTab - 1; j++) {
            arrName[j] = arrName[j + 1];
            cards[j] = cards[j + 1];
        }
        nbTab--;
        if (selected == i) select(Math.min(selected + 1, nbTab - 1));
        mustRecalculate = true;
        repaint();
    }

    public int countTabs() {
        return nbTab;
    }

    public void renameTab(String oldName, String newName) {
        for (int i = 0; i < nbTab; i++) {
            if (arrName[i].equals(oldName)) {
                arrName[i] = new String(newName);
                mustRecalculate = true;
                repaint();
                break;
            }
        }
    }

    public void select(int num) {
        if (num < 0 || num > nbTab || num == selected) return;
        selected = num;
        tabLayout.show(tabPanel, Integer.toString(selected));
        mustRecalculate = true;
        repaint();
    }

    public Component getSelectedComponent() {
        if (selected < 0 || selected >= nbTab) return null; else return cards[selected];
    }

    public synchronized void update(Graphics g) {
        paint(g);
    }

    public synchronized void paint(Graphics g) {
        if (mustRecalculate || getFont() != plainFont) recalculate();
        Dimension d = getSize();
        g.setColor(getBackground());
        g.fillRect(0, 0, W, Z + 1);
        for (int curr = 0; curr < nbTab; curr++) {
            int upper = (curr == selected) ? 0 : selUpper;
            g.setFont((curr == selected) ? boldFont : plainFont);
            g.setColor(shadow);
            for (int i = lineWidth; curr != (selected - 1) && i > 0; i--) {
                if (i == 1) g.setColor(Color.black);
                g.drawLine(arrEnd[curr] - i, upper + verRound, arrEnd[curr] - i, Z - lineWidth);
                if (i == 1) g.drawLine(arrEnd[curr] - lineWidth, upper + 1, arrEnd[curr] - lineWidth, upper + 1);
            }
            g.setColor(Color.black);
            g.drawString(arrName[curr], arrBeg[curr] + (curr == selected ? 2 * lineWidth : lineWidth) + XTitle, upper + YTitle + lineWidth + fmBoldFont.getAscent());
            g.setColor(Color.white);
            for (int i = 0; i < lineWidth; i++) {
                if (i == 1) g.setColor(light);
                g.drawLine(arrBeg[curr] + horRound, upper + i, arrEnd[curr] - lineWidth - horRound + 1, upper + i);
                if (curr != (selected + 1)) {
                    g.drawLine(arrBeg[curr] + i, upper + verRound, arrBeg[curr] + i, Z);
                    if (i == 0) g.drawLine(arrBeg[curr] + 1, upper + 1, arrBeg[curr] + 1, upper + 1);
                }
            }
        }
        g.setColor(Color.white);
        for (int i = 0; i < lineWidth; i++) {
            if (i == 1) g.setColor(light);
            g.drawLine(i, Z, i, H - 1);
            if (selected >= 0) {
                if (selected != 0) g.drawLine(0, Z - lineWidth + i + 1, arrBeg[selected], Z - lineWidth + i + 1);
                g.drawLine(arrEnd[selected], Z - lineWidth + i + 1, W - 1, Z - lineWidth + i + 1);
            } else {
                g.drawLine(0, Z - lineWidth + i + 1, W - 1, Z - lineWidth + i + 1);
            }
        }
        g.setColor(shadow);
        for (int i = lineWidth; i > 0; i--) {
            if (i == 1) g.setColor(Color.black);
            g.drawLine(0, H - i, W, H - i);
            g.drawLine(W - i, Z, W - i, H - 1);
        }
    }

    void clickTab(int x, int y) {
        for (int i = 0; i < nbTab; i++) if (x > arrBeg[i] && x < arrEnd[i]) select(i);
    }

    class MouseHandler extends MouseAdapter {

        public void mousePressed(MouseEvent event) {
            if (event.getY() < Z) clickTab(event.getX(), event.getY());
        }
    }
}
