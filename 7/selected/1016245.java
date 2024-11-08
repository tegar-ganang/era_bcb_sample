package com.memoire.bu;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ColorUIResource;
import com.memoire.fu.FuLog;

/**
 * A DesktopPane with a few additional services. Auto-resizing, optionnaly tabbed, custom-friendly, icons management,
 * ...
 */
public class BuDesktop extends JDesktopPane implements MouseListener, ActionListener {

    public static final int LEFT_MARGIN = 31;

    public static final int SNAPX = Math.max(4, BuPreferences.BU.getIntegerProperty("desktop.snapx", 37));

    public static final int SNAPY = Math.max(4, BuPreferences.BU.getIntegerProperty("desktop.snapy", 37));

    public static final int BLOCK_LAYER = JLayeredPane.DEFAULT_LAYER.intValue() - 100;

    public BuDesktop() {
        super();
        setName("buDESKTOP");
        setDesktopManager(createDesktopManager());
        setBackgroundPainter(createBackgroundPainter());
        addMouseListener(this);
        if (isTabbed()) {
            setToolTipText("...");
            setLayout(new Layout());
        }
    }

    public final boolean isPalette(JComponent _f) {
        return (DEFAULT_LAYER.intValue() != getLayer(_f)) && Boolean.TRUE.equals(_f.getClientProperty("JInternalFrame.isPalette"));
    }

    protected String _(String _s) {
        return BuResource.BU.getString(_s);
    }

    public void paint(Graphics _g) {
        BuLib.setAntialiasing(this, _g);
        super.paint(_g);
    }

    public final void snapXY(JComponent _f) {
        if (BuPreferences.BU.getBooleanProperty("desktop.snap", false) && !isTabbed() && !isPalette(_f) && (_f instanceof JInternalFrame)) {
            Point p = _f.getLocation();
            boolean b = false;
            if (p.x % SNAPX != 0) {
                p.x = p.x - p.x % SNAPX;
                b = true;
            }
            if (p.y % SNAPY != 0) {
                p.y = p.y - p.y % SNAPY;
                b = true;
            }
            if (b) _f.setLocation(p);
        }
    }

    public final void snapWH(JComponent _f) {
        if (BuPreferences.BU.getBooleanProperty("desktop.snap", false) && !isTabbed() && !isPalette(_f) && (_f instanceof JInternalFrame)) {
            Dimension d = _f.getSize();
            boolean b = false;
            if (((JInternalFrame) _f).isResizable()) {
                if (d.width % SNAPX != 0) {
                    d.width = d.width + SNAPX - d.width % SNAPX;
                    b = true;
                }
                if (d.height % SNAPY != 0) {
                    d.height = d.height + SNAPY - d.height % SNAPY;
                    b = true;
                }
            }
            if (b) _f.setSize(d);
        }
    }

    protected DesktopManager createDesktopManager() {
        return new BuDesktopManager(this);
    }

    protected BuBackgroundPainter createBackgroundPainter() {
        BuBackgroundPainter bp = new BuBackgroundPainter();
        if (isBlocked()) {
            bp.setBar(true);
            bp.setBarHeight(BuLib.isMetal() ? 8 + BuResource.BU.getDefaultFrameSize() : 0);
        }
        return bp;
    }

    public boolean isFocusCycleRoot() {
        return true;
    }

    public boolean isOpaque() {
        return Boolean.TRUE.equals(UIManager.get("Desktop.opaque"));
    }

    protected void addImpl(Component _c, Object _constraints, int _index) {
        if (_c instanceof JInternalFrame.JDesktopIcon) {
            JInternalFrame.JDesktopIcon i = (JInternalFrame.JDesktopIcon) _c;
            i.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            putLayer(i, BLOCK_LAYER);
            if (isBlocked()) {
                Component[] c = i.getComponents();
                if (BuLib.isMetal()) {
                    if (i.getComponentCount() == 2) {
                        i.remove(c[1]);
                        i.setBorder(BuBorders.EMPTY0000);
                        if (c[0] instanceof JButton) {
                            ((JButton) c[0]).setHorizontalAlignment(SwingConstants.LEFT);
                            ((JButton) c[0]).setMargin(BuInsets.INSETS1111);
                            ((JButton) c[0]).getModel().setRollover(false);
                        }
                    }
                } else if (BuLib.isSlaf()) {
                    if (c.length == 2) {
                        i.setBorder(BuBorders.EMPTY1111);
                        i.setLayout(new BorderLayout(2, 0));
                        i.add(c[0], BorderLayout.CENTER);
                        i.add(c[1], BorderLayout.WEST);
                        if (c[0] instanceof JLabel) ((JLabel) c[0]).setHorizontalAlignment(SwingConstants.LEFT);
                    }
                }
                Dimension ps = i.getPreferredSize();
                if (BuLib.isMotif()) ; else if (BuLib.isMetal()) ps.width = SNAPX * 4; else ps.width = SNAPX * 3;
                i.setPreferredSize(ps);
                i.setSize(ps);
                JInternalFrame f = i.getInternalFrame();
                f.addPropertyChangeListener(new PropertyChangeListener() {

                    public void propertyChange(PropertyChangeEvent _evt) {
                        if (Boolean.TRUE.equals(_evt.getNewValue()) && "selected".equals(_evt.getPropertyName())) {
                            final JInternalFrame fintern = (JInternalFrame) _evt.getSource();
                            if (fintern.isIcon()) {
                                try {
                                    fintern.setIcon(false);
                                } catch (PropertyVetoException ex) {
                                }
                            }
                        }
                    }
                });
            }
        }
        if (_c instanceof JComponent) {
            snapXY((JComponent) _c);
            snapWH((JComponent) _c);
        }
        super.addImpl(_c, _constraints, _index);
        boolean b = isBlocked() && (_c instanceof JInternalFrame.JDesktopIcon);
        if (b) arrangeIcons0();
    }

    public void remove(int _index) {
        Component c = getComponent(_index);
        Rectangle r = c.getBounds();
        boolean b = isBlocked() && (c instanceof JInternalFrame.JDesktopIcon);
        super.remove(_index);
        repaint(r);
        if (b) arrangeIcons0();
    }

    public void paintComponent(Graphics _g) {
        JInternalFrame[] frames = getAllFrames();
        if (!isTabbed() || (frames.length == 0)) {
            Rectangle clip = _g.getClipBounds();
            BuBackgroundPainter bp = getBackgroundPainter();
            if (bp != null) bp.paintBackground(this, _g);
            if (logo_ != null) {
                Rectangle ri = new Rectangle(5, 5, logo_.getIconWidth(), logo_.getIconHeight());
                if (ri.intersects(clip)) logo_.paintIcon(this, _g, ri.x, ri.y);
            }
        } else {
            Dimension dd = getSize();
            Color pbg = UIManager.getColor("Panel.background");
            Color dbg = getBackground();
            _g.setColor(pbg);
            _g.fillRect(0, 0, dd.width, dd.height);
            int y, l;
            sortFramesByTitle(frames);
            l = frames.length;
            y = 0;
            for (int i = 0; i < l; i++) {
                if (frames[i].isClosed()) continue;
                if (!frames[i].isVisible()) continue;
                _g.setColor(pbg);
                _g.draw3DRect(0, y, LEFT_MARGIN - 1, LEFT_MARGIN - 1, false);
                if (isPalette(frames[i])) _g.setColor(pbg); else if (frames[i].isSelected()) _g.setColor(dbg.brighter()); else _g.setColor(dbg);
                _g.fill3DRect(1, y + 1, LEFT_MARGIN - 2, LEFT_MARGIN - 2, true);
                Icon icon = frames[i].getFrameIcon();
                if (icon == null) icon = UIManager.getIcon("InternalFrame.icon");
                if (icon != null) {
                    int w = icon.getIconWidth();
                    int h = icon.getIconHeight();
                    icon.paintIcon(this, _g, (LEFT_MARGIN - w) / 2, y + (LEFT_MARGIN - h) / 2);
                }
                y += LEFT_MARGIN;
            }
        }
    }

    public void paintChildren(Graphics _g) {
        super.paintChildren(_g);
    }

    public String getToolTipText() {
        return null;
    }

    public String getToolTipText(MouseEvent _evt) {
        if (isTabbed()) {
            int xe = _evt.getX();
            int ye = _evt.getY();
            JInternalFrame[] frames = getAllFrames();
            int l = frames.length;
            if ((xe < LEFT_MARGIN) && (ye < LEFT_MARGIN * l)) {
                sortFramesByTitle(frames);
                JInternalFrame f = frames[ye / LEFT_MARGIN];
                String r = f.getTitle();
                if ("".equals(r)) r = null;
                if (r == null) r = "" + (1 + ye / LEFT_MARGIN);
                return r;
            }
        }
        return super.getToolTipText(_evt);
    }

    public Point getToolTipLocation(MouseEvent _evt) {
        if (isTabbed()) {
            int xe = _evt.getX();
            int ye = _evt.getY();
            JInternalFrame[] frames = getAllFrames();
            int l = frames.length;
            if ((xe < LEFT_MARGIN) && (ye < LEFT_MARGIN * l)) {
                sortFramesByTitle(frames);
                return new Point(LEFT_MARGIN + 2, (ye / LEFT_MARGIN) * LEFT_MARGIN + 1);
            }
        }
        return super.getToolTipLocation(_evt);
    }

    private String title_;

    public String getTitle() {
        return title_;
    }

    public void setTitle(String _title) {
        title_ = _title;
    }

    private boolean outline_ = BuPreferences.BU.getBooleanProperty("desktop.outline", true);

    public boolean isOutline() {
        return outline_;
    }

    private boolean tabbed_ = BuPreferences.BU.getBooleanProperty("desktop.tabbed", false);

    public boolean isTabbed() {
        return tabbed_;
    }

    private boolean blocked_ = BuPreferences.BU.getBooleanProperty("desktop.blocked", true);

    public boolean isBlocked() {
        return blocked_;
    }

    private Icon logo_;

    public Icon getLogo() {
        return logo_;
    }

    public void setLogo(Icon _logo) {
        logo_ = _logo;
        repaint();
    }

    private BuBackgroundPainter bp_;

    public BuBackgroundPainter getBackgroundPainter() {
        return bp_;
    }

    public void setBackgroundPainter(BuBackgroundPainter _bp) {
        bp_ = _bp;
        invalidate();
        repaint();
    }

    private Insets margins_ = createHardMargins();

    protected Insets createHardMargins() {
        return new Insets(0, 0, 0, 0);
    }

    public Insets getHardMargins() {
        Insets r = new Insets(margins_.top, margins_.left, margins_.bottom, margins_.right);
        BuBackgroundPainter bp = getBackgroundPainter();
        if (bp != null) r.bottom = Math.max(r.bottom, bp.getBarHeight());
        return r;
    }

    public void setHardMargins(Insets _margins) {
        margins_ = _margins;
        invalidate();
        repaint();
    }

    /**
   * Ajoute une fenetre interne. Lors de l'ajout, la fenetre est rendue visible, positionn�e devant les autres, et devient active.
   * @param _f La fenetre interne a ajouter.
   */
    public void addInternalFrame(JInternalFrame _f) {
        if (!SwingUtilities.isEventDispatchThread()) throw new RuntimeException("Not in swing thread. " + "Use Implementation.addInternalFrame() instead");
        if (_f.getDesktopPane() == null) {
            if (_f.isSelected()) {
                try {
                    _f.setSelected(false);
                } catch (PropertyVetoException ex) {
                }
            }
            if (_f.isIcon()) {
                try {
                    _f.setIcon(false);
                } catch (PropertyVetoException ex) {
                }
            }
            Point pf = _f.getLocation();
            if ((pf.x == 0) && (pf.y == 0)) {
                Container cp = getParent();
                if (cp instanceof JViewport) {
                    Rectangle vr = ((JViewport) cp).getViewRect();
                    Dimension df = _f.getSize();
                    pf.x = vr.x + (vr.width - df.width) / 2;
                    pf.y = vr.y + (vr.height - df.height) / 2;
                    _f.setLocation(pf);
                }
            }
            add(_f);
            checkInternalFrame(_f);
            snapXY(_f);
            snapWH(_f);
        }
        activateInternalFrame(_f);
    }

    public void removeInternalFrame(JInternalFrame _f) {
        if (!SwingUtilities.isEventDispatchThread()) throw new RuntimeException("Not in swing thread. " + "Use Implementation.removeInternalFrame() instead");
        if (_f != null && _f.getDesktopPane() == this) {
            deactivateInternalFrame(_f);
            remove(_f);
        }
        adjustSize();
    }

    /**
   * Indique au desktop qu'il y a eu un changement de position/taille de l'internal frame. Ceci lui permet de r�ajuster sa taille.
   * @param _f L'internal frame concern�e par le changement de taille/position.
   */
    public void checkInternalFrame(JInternalFrame _f) {
        if (!SwingUtilities.isEventDispatchThread()) throw new RuntimeException("Not in swing thread.");
        Dimension dd = getSize();
        Point pf = _f.getLocation();
        Dimension df = _f.getSize();
        if (_f.isResizable()) {
            if (df.width > dd.width) df.width = dd.width;
            if (df.height > dd.height) df.height = dd.height;
            if (!df.equals(getSize())) _f.setSize(df);
        }
        if (pf.x + df.width > dd.width) pf.x = dd.width - df.width;
        if (pf.y + df.height > dd.height) pf.y = dd.height - df.height;
        if (pf.x < 0) pf.x = 0;
        if (pf.y < 0) pf.y = 0;
        if (isTabbed() && isPalette(_f) && (pf.x < LEFT_MARGIN + 4)) pf.x = LEFT_MARGIN + 4;
        if (!pf.equals(getLocation())) _f.setLocation(pf);
        adjustSize();
    }

    /**
   * Active une fenetre interne. Lors de l'activation, la fenetre est rendue visible, positionn�e devant les autres, et
   * d�siconifi�e.
   * @param _f La fenetre interne a activer.
   */
    public void activateInternalFrame(JInternalFrame _f) {
        if (!SwingUtilities.isEventDispatchThread()) throw new RuntimeException("Not in swing thread. " + "Use Implementation.activateInternalFrame() instead");
        if (!_f.isVisible()) {
            _f.setVisible(true);
        }
        if (_f.isClosed()) {
            try {
                _f.setClosed(false);
            } catch (PropertyVetoException ex) {
            }
        }
        checkInternalFrame(_f);
        if (_f.isIcon()) {
            try {
                _f.setIcon(false);
            } catch (PropertyVetoException ex) {
            }
        }
        {
            moveToFront(_f);
            if (!_f.isSelected() && !isPalette(_f)) {
                try {
                    _f.setSelected(true);
                } catch (PropertyVetoException ex) {
                }
            }
        }
    }

    public void deactivateInternalFrame(JInternalFrame _f) {
        if (!SwingUtilities.isEventDispatchThread()) throw new RuntimeException("Not in swing thread.");
        checkInternalFrame(_f);
        if (_f.isSelected()) {
            try {
                _f.setSelected(false);
            } catch (PropertyVetoException ex) {
            }
        }
    }

    public JInternalFrame getCurrentInternalFrame() {
        JInternalFrame[] frames = getAllFrames();
        JInternalFrame r = null;
        for (int i = 0; i < frames.length; i++) if (frames[i].isSelected()) {
            r = frames[i];
            break;
        }
        return r;
    }

    public JInternalFrame[] getNormalFrames() {
        JInternalFrame[] frames = getAllFrames();
        int l, n, i, j;
        l = frames.length;
        n = 0;
        for (i = 0; i < l; i++) if (!frames[i].isIcon() && !isPalette(frames[i])) n++;
        JInternalFrame[] r = new JInternalFrame[n];
        j = 0;
        for (i = 0; i < l; i++) if (!frames[i].isIcon() && !isPalette(frames[i])) {
            r[j] = frames[i];
            j++;
        }
        return r;
    }

    public JInternalFrame[] getNotIconifiedFrames() {
        JInternalFrame[] frames = getAllFrames();
        int l, n, i, j;
        l = frames.length;
        n = 0;
        for (i = 0; i < l; i++) if (!frames[i].isIcon()) n++;
        JInternalFrame[] r = new JInternalFrame[n];
        j = 0;
        for (i = 0; i < l; i++) if (!frames[i].isIcon()) {
            r[j] = frames[i];
            j++;
        }
        return r;
    }

    public JInternalFrame[] getIconifiedFrames() {
        JInternalFrame[] frames = getAllFrames();
        int l, n, i, j;
        l = frames.length;
        n = 0;
        for (i = 0; i < l; i++) if (frames[i].isIcon()) n++;
        JInternalFrame[] r = new JInternalFrame[n];
        j = 0;
        for (i = 0; i < l; i++) if (frames[i].isIcon()) {
            r[j] = frames[i];
            j++;
        }
        return r;
    }

    public JInternalFrame[] getPalettes() {
        JInternalFrame[] frames = getAllFrames();
        int l, n, i, j;
        l = frames.length;
        n = 0;
        for (i = 0; i < l; i++) if (isPalette(frames[i])) n++;
        JInternalFrame[] r = new JInternalFrame[n];
        j = 0;
        for (i = 0; i < l; i++) if (isPalette(frames[i])) {
            r[j] = frames[i];
            j++;
        }
        return r;
    }

    public int getNormalFramesCount() {
        return getNormalFrames().length;
    }

    public int getNotIconifiedFramesCount() {
        return getNotIconifiedFrames().length;
    }

    public int getIconifiedFramesCount() {
        return getIconifiedFrames().length;
    }

    public int getPalettesCount() {
        return getPalettes().length;
    }

    public JInternalFrame getSelectedFrame() {
        JInternalFrame r = null;
        if (BuLib.swing() >= 1.2) {
            r = super.getSelectedFrame();
        } else {
            JInternalFrame[] f = getNormalFrames();
            for (int i = 0; i < f.length; i++) if (f[i].isSelected()) {
                r = f[i];
                break;
            }
        }
        return r;
    }

    protected void sortFramesByTitle(JInternalFrame[] _frames) {
        JInternalFrame tmp;
        int i, l;
        l = _frames.length;
        for (i = 0; i + 1 < l; i++) {
            String t1 = _frames[i].getTitle();
            String t2 = _frames[i + 1].getTitle();
            if ((t1 != null) && (t2 != null) && (t1.compareTo(t2) > 0)) {
                tmp = _frames[i];
                _frames[i] = _frames[i + 1];
                _frames[i + 1] = tmp;
                i--;
                if (i >= 0) i--;
            }
        }
    }

    protected void sortFramesByHeight(JInternalFrame[] _frames) {
        sortFramesByTitle(_frames);
        JInternalFrame tmp;
        int i, l;
        l = _frames.length;
        for (i = 0; i + 1 < l; i++) if (_frames[i].getHeight() > _frames[i + 1].getHeight()) {
            tmp = _frames[i];
            _frames[i] = _frames[i + 1];
            _frames[i + 1] = tmp;
            i--;
            if (i >= 0) i--;
        }
    }

    protected void sortFramesByWidth(JInternalFrame[] _frames) {
        sortFramesByTitle(_frames);
        JInternalFrame tmp;
        int i, l;
        l = _frames.length;
        for (i = 0; i + 1 < l; i++) if (_frames[i].getWidth() < _frames[i + 1].getWidth()) {
            tmp = _frames[i];
            _frames[i] = _frames[i + 1];
            _frames[i + 1] = tmp;
            i--;
            if (i >= 0) i--;
        }
    }

    public void waterfall() {
        if (isTabbed()) return;
        JInternalFrame[] frames = getNormalFrames();
        int i, l, x, y;
        l = frames.length;
        if (l > 0) {
            sortFramesByTitle(frames);
            x = 74;
            y = 0;
            for (i = l - 1; i >= 0; i--) {
                frames[i].setLocation(x, y);
                moveToFront(frames[i]);
                checkInternalFrame(frames[i]);
                x += SNAPX;
                y += SNAPY;
            }
        }
    }

    public void tile() {
        if (isTabbed()) return;
        JInternalFrame[] frames = getNormalFrames();
        int i, l, x, y, h, wd;
        l = frames.length;
        if (l > 0) {
            sortFramesByHeight(frames);
            wd = getWidth();
            wd = (wd < 200 ? 200 : wd);
            x = 0;
            y = 0;
            h = 0;
            for (i = 0; i < l; i++) {
                int wf = frames[i].getWidth();
                int hf = frames[i].getHeight();
                if (x + wf > wd) {
                    x = 0;
                    y += h;
                    h = 0;
                }
                frames[i].setLocation(x, y);
                x += wf;
                h = Math.max(h, hf);
                moveToFront(frames[i]);
                checkInternalFrame(frames[i]);
            }
        }
    }

    public void arrangeIcons() {
        arrangeIcons0();
        adjustSize();
    }

    protected final void arrangeIcons0() {
        if (isTabbed()) return;
        JInternalFrame[] frames;
        int i, l, x, y, wd, hd, bh;
        wd = getWidth();
        hd = getHeight();
        bh = 0;
        frames = getIconifiedFrames();
        l = frames.length;
        if (l > 0) {
            sortFramesByTitle(frames);
            x = 0;
            y = hd;
            Border b = UIManager.getBorder("StatusBar.border");
            if ((l > 0) && (b != null)) y -= b.getBorderInsets(this).bottom;
            for (i = 0; i < l; i++) {
                JInternalFrame.JDesktopIcon dti = frames[i].getDesktopIcon();
                dti.setSize(dti.getPreferredSize());
                Dimension cs = dti.getSize();
                if (x + cs.width >= wd) {
                    x = 0;
                    y -= cs.height;
                }
                dti.setLocation(x, y - cs.height);
                x += cs.width;
                bh = Math.max(bh, hd - y + cs.height);
            }
            if ((l > 0) && (b != null)) bh += b.getBorderInsets(this).top;
        }
        if (isBlocked()) {
            BuBackgroundPainter bp = getBackgroundPainter();
            if ((bp != null) && (bh != bp.getBarHeight())) {
                if (bh > 0) {
                    bp.setBarHeight(bh);
                    repaint(0, hd - bh, wd, bh);
                }
            }
        }
    }

    public void arrangePalettes() {
        JInternalFrame[] frames;
        int i, l, x, y, hd, wmax;
        hd = getHeight();
        frames = getAllFrames();
        l = frames.length;
        if (l > 0) {
            int yInit = isTabbed() ? 17 : 0;
            int xInit = isTabbed() ? LEFT_MARGIN + 1 : 0;
            if (!isTabbed() && getCurrentInternalFrame() != null) {
                xInit = getCurrentInternalFrame().getX();
                yInit = getCurrentInternalFrame().getY();
            }
            sortFramesByHeight(frames);
            wmax = 0;
            x = xInit;
            y = yInit;
            for (i = 0; i < l; i++) {
                JInternalFrame dti = frames[i];
                if (!isPalette(dti)) continue;
                Dimension cs = dti.getSize();
                if (y + cs.height >= hd) {
                    x += wmax;
                    y = yInit;
                    wmax = 0;
                }
                dti.setLocation(x, y);
                y += cs.height;
                wmax = Math.max(wmax, cs.width);
            }
        }
        adjustSize();
    }

    public void reshape(int _x, int _y, int _w, int _h) {
        int ow = getWidth();
        int oh = getHeight();
        super.reshape(_x, _y, _w, _h);
        if (isBlocked() && ((ow != _w) || (oh != _h))) arrangeIcons0();
    }

    public final Dimension getDefaultPreferredSize() {
        return super.getPreferredSize();
    }

    public Dimension getPreferredSize() {
        Dimension r = getDefaultPreferredSize();
        Point qmin = new Point(0, 0);
        JInternalFrame[] frames = getAllFrames();
        int i, l;
        l = frames.length;
        for (i = l - 1; i >= 0; i--) {
            if (isTabbed() && !isPalette(frames[i])) continue;
            if (!isTabbed() && !frames[i].isIcon() && frames[i].isMaximum()) continue;
            Point q = null;
            if (frames[i].isIcon()) q = frames[i].getDesktopIcon().getLocation(); else q = frames[i].getLocation();
            if (qmin.x > q.x) qmin.x = q.x;
            if (qmin.y > q.y) qmin.y = q.y;
        }
        for (i = l - 1; i >= 0; i--) {
            if (isTabbed() && !isPalette(frames[i])) continue;
            if (!isTabbed() && !frames[i].isIcon() && frames[i].isMaximum()) continue;
            if (frames[i].isIcon()) {
                if (!isBlocked()) {
                    JComponent di = frames[i].getDesktopIcon();
                    if (di.isVisible()) {
                        Point p = di.getLocation();
                        p.x -= qmin.x;
                        p.y -= qmin.y;
                        di.setLocation(p);
                        Dimension d = di.getSize();
                        if (p.x + d.width > r.width) r.width = p.x + d.width;
                        if (p.y + d.height > r.height) r.height = p.y + d.height;
                    }
                }
            } else if (frames[i].isVisible()) {
                Point p = frames[i].getLocation();
                p.x -= qmin.x;
                p.y -= qmin.y;
                frames[i].setLocation(p);
                Dimension d = frames[i].getSize();
                if (p.x + d.width > r.width) r.width = p.x + d.width;
                if (p.y + d.height > r.height) r.height = p.y + d.height;
            }
        }
        return r;
    }

    public void adjustSize() {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                Container pt = getParent();
                Dimension ns = getPreferredSize();
                if (pt != null) {
                    Dimension vs = pt.getSize();
                    if (ns.width < vs.width) ns.width = vs.width;
                    if (ns.height < vs.height) ns.height = vs.height;
                }
                setSize(ns);
            }
        });
    }

    public void showFrame(final JInternalFrame _f) {
        Runnable runnable = new Runnable() {

            public void run() {
                Container cp = getParent();
                if ((cp instanceof JViewport) && (_f != null) && _f.isShowing()) {
                    JViewport vp = (JViewport) cp;
                    Point vo = vp.getViewPosition();
                    Dimension es = vp.getExtentSize();
                    Dimension vs = vp.getViewSize();
                    Point fo = _f.getLocation();
                    Dimension fs = _f.getSize();
                    if (fo.x < vo.x) vo.x = fo.x;
                    if (fo.y < vo.y) vo.y = fo.y;
                    if (fo.x + fs.width > vo.x + es.width) vo.x = Math.min(fo.x, vs.width - es.width);
                    if (fo.y + fs.height > vo.y + es.height) vo.y = Math.min(fo.y, vs.height - es.height);
                    vp.setViewPosition(vo);
                }
            }
        };
        if (SwingUtilities.isEventDispatchThread()) runnable.run(); else SwingUtilities.invokeLater(runnable);
    }

    final class Layout implements LayoutManager2 {

        public void addLayoutComponent(String _s, Component _c) {
            addLayoutComponent(_c, _s);
        }

        public void addLayoutComponent(Component _c, Object _o) {
            if ((_c instanceof JComponent) && isPalette((JComponent) _c)) return;
            if (_c instanceof BuInternalFrame) ((BuInternalFrame) _c).simplifyTop();
            if (_c instanceof JInternalFrame) {
                JInternalFrame f = (JInternalFrame) _c;
                f.setIconifiable(false);
                f.setMaximizable(false);
            }
        }

        public void removeLayoutComponent(Component _c) {
        }

        public float getLayoutAlignmentX(Container _p) {
            return 0.5f;
        }

        public float getLayoutAlignmentY(Container _p) {
            return 0.5f;
        }

        public Dimension minimumLayoutSize(Container _p) {
            return _p.getSize();
        }

        public Dimension preferredLayoutSize(Container _p) {
            return _p.getSize();
        }

        public Dimension maximumLayoutSize(Container _p) {
            return _p.getSize();
        }

        public void invalidateLayout(Container _p) {
            Dimension ds = _p.getSize();
            repaint(0, 0, LEFT_MARGIN, ds.height);
        }

        public void layoutContainer(Container _p) {
            Dimension ds = _p.getSize();
            int l = _p.getComponentCount();
            for (int i = 0; i < l; i++) {
                Component c = getComponent(i);
                if ((c instanceof JInternalFrame) && !isPalette((JComponent) c)) {
                    c.setBounds(LEFT_MARGIN, 0, ds.width - LEFT_MARGIN, ds.height);
                }
            }
            repaint(0, 0, LEFT_MARGIN, ds.height);
        }
    }

    public void mouseDown(MouseEvent _evt) {
    }

    public void mouseEntered(MouseEvent _evt) {
    }

    public void mouseExited(MouseEvent _evt) {
    }

    public void mousePressed(MouseEvent _evt) {
    }

    public void mouseReleased(MouseEvent _evt) {
    }

    public void mouseUp(MouseEvent _evt) {
    }

    public void mouseClicked(MouseEvent _evt) {
        if (BuLib.isRight(_evt)) {
            popupMenu(_evt);
        } else if (BuLib.isLeft(_evt) && isTabbed()) {
            int xe = _evt.getX();
            int ye = _evt.getY();
            JInternalFrame[] frames = getAllFrames();
            int l = frames.length;
            if ((xe < LEFT_MARGIN) && (ye < LEFT_MARGIN * l)) {
                sortFramesByTitle(frames);
                activateInternalFrame(frames[ye / LEFT_MARGIN]);
            }
        }
    }

    protected BuPopupMenu menu_ = null;

    protected BuPopupMenu createPopupMenu() {
        if (menu_ != null) return menu_;
        menu_ = new BuPopupMenu(_("Bureau"));
        menu_.addCheckBox(_("Grille"), "GRILLE", BuResource.BU.getMenuIcon("aucun"), true, true);
        menu_.addCheckBox(_("Points"), "POINTS", BuResource.BU.getMenuIcon("aucun"), true, true);
        menu_.addCheckBox(_("Magn�tisme"), "MAGNETISME", BuResource.BU.getMenuIcon("aucun"), true, true);
        menu_.addSeparator();
        menu_.addMenuItem(_("Uniforme"), "DESKTOP_UNIFORME", BuResource.BU.getMenuIcon("uniforme"), true);
        menu_.addMenuItem(_("D�grad�"), "DESKTOP_DEGRADE", BuResource.BU.getMenuIcon("degrade"), true);
        menu_.addSeparator();
        BuMenu mnTextures = new BuMenu(_("Textures"), "MENU_TEXTURES", BuResource.BU.getMenuIcon("texture"));
        for (int i = 1; i <= 9; i++) {
            final int indx = i;
            final BuTextureIcon icon = new BuTextureIcon();
            final BuMenuItem item = mnTextures.addMenuItem(_("Texture") + " " + i, "DESKTOP_TEXTURE" + i, icon, true);
            item.addChangeListener(new ChangeListener() {

                public void stateChanged(ChangeEvent _evt) {
                    final ChangeListener THIS = this;
                    Thread t = new Thread(new Runnable() {

                        public void run() {
                            item.removeChangeListener(THIS);
                            FuLog.debug("Loading texture #" + indx);
                            Image image = BuPreferences.BU.getTexture(indx);
                            icon.setImage(image);
                            item.repaint();
                        }
                    }, "Bu load texture " + indx);
                    t.setPriority(Thread.MIN_PRIORITY);
                    t.start();
                }
            });
        }
        BuMenu mnCouleurs = new BuMenu(_("Couleurs"), "MENU_COULEURS", new BuColorIcon(new Color(128, 160, 255)));
        mnCouleurs.addMenuItem(_("Rouge"), "DESKTOP_ROUGE", new BuColorIcon(BuLib.getColor(new Color(128, 64, 64))), true);
        mnCouleurs.addMenuItem(_("Vert"), "DESKTOP_VERT", new BuColorIcon(BuLib.getColor(new Color(64, 129, 64))), true);
        mnCouleurs.addMenuItem(_("Bleu"), "DESKTOP_BLEU", new BuColorIcon(BuLib.getColor(new Color(64, 64, 128))), true);
        mnCouleurs.addMenuItem(_("Orange"), "DESKTOP_ORANGE", new BuColorIcon(BuLib.getColor(new Color(192, 128, 96))), true);
        mnCouleurs.addMenuItem(_("Similaire"), "DESKTOP_SIMILAIRE", new BuColorIcon(UIManager.getColor("Panel.background")), true);
        mnCouleurs.addMenuItem(_("D�faut"), "DESKTOP_DEFAUT", new BuColorIcon(UIManager.getColor("Desktop.background")), true);
        menu_.addSubMenu(mnTextures, true);
        menu_.addSubMenu(mnCouleurs, true);
        return menu_;
    }

    public void popupMenu(MouseEvent _evt) {
        BuBackgroundPainter bp = getBackgroundPainter();
        BuPopupMenu pm = createPopupMenu();
        BuActionChecker.setCheckedForAction(pm, "GRILLE", bp.isGrid());
        BuActionChecker.setCheckedForAction(pm, "POINTS", bp.isDots());
        BuActionChecker.setCheckedForAction(pm, "MAGNETISME", BuPreferences.BU.getBooleanProperty("desktop.snap", false));
        int x = _evt.getX() - pm.getPreferredSize().width / 2;
        int y = _evt.getY() + 12;
        pm.show((JComponent) _evt.getSource(), x, y);
    }

    public void actionPerformed(ActionEvent _evt) {
        String action = _evt.getActionCommand();
        BuBackgroundPainter bp = getBackgroundPainter();
        if (action.equals("GRILLE")) {
            bp.setGrid(!bp.isGrid());
        } else if (action.equals("POINTS")) {
            bp.setDots(!bp.isDots());
        } else if (action.equals("MAGNETISME")) {
            BuPreferences.BU.putBooleanProperty("desktop.snap", !BuPreferences.BU.getBooleanProperty("desktop.snap", false));
        } else if (action.equals("DESKTOP_UNIFORME")) {
            bp.setGradient(false);
        } else if (action.equals("DESKTOP_DEGRADE")) {
            bp.setGradient(true);
        } else if (action.startsWith("DESKTOP_TEXTURE")) {
            if (BuLib.getUIBoolean("Desktop.textureAlwaysUsed")) bp.setIcon(null); else bp.setIcon(BuLib.filter(new BuIcon(BuPreferences.BU.getTexture(Integer.parseInt(action.substring(15))))));
        } else if (action.equals("DESKTOP_ROUGE")) {
            setBackground(new ColorUIResource(BuLib.getColor(new Color(128, 64, 64))));
            bp.setIcon(null);
        } else if (action.equals("DESKTOP_VERT")) {
            setBackground(new ColorUIResource(BuLib.getColor(new Color(64, 128, 64))));
            bp.setIcon(null);
        } else if (action.equals("DESKTOP_BLEU")) {
            setBackground(new ColorUIResource(BuLib.getColor(new Color(64, 64, 128))));
            bp.setIcon(null);
        } else if (action.equals("DESKTOP_ORANGE")) {
            setBackground(new ColorUIResource(BuLib.getColor(new Color(192, 128, 96))));
            bp.setIcon(null);
        } else if (action.equals("DESKTOP_SIMILAIRE")) {
            setBackground(UIManager.getColor("Panel.background"));
            bp.setIcon(null);
        } else if (action.equals("DESKTOP_DEFAUT")) {
            setBackground(UIManager.getColor("Desktop.background"));
            bp.setIcon(null);
        }
        setBackgroundPainter(bp);
    }

    public void updateUI() {
        if (isShowing()) setBackground(null);
        super.updateUI();
    }
}
