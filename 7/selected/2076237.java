package org.fudaa.ebli.calque;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.util.Locale;
import javax.swing.JComponent;
import com.memoire.bu.BuLib;
import org.fudaa.ebli.geometrie.GrBoite;
import org.fudaa.ebli.geometrie.GrPoint;
import org.fudaa.ebli.geometrie.GrPolygone;
import org.fudaa.ebli.geometrie.GrVecteur;
import org.fudaa.ebli.palette.BPaletteCouleur;
import org.fudaa.ebli.palette.BPaletteCouleurSimple;
import org.fudaa.ebli.trace.BParametresGouraud;
import org.fudaa.ebli.trace.Gouraud;
import org.fudaa.ebli.trace.TraceGeometrie;
import org.fudaa.ebli.trace.TraceIsoLignes;
import org.fudaa.ebli.trace.TraceIsoSurfaces;

/**
 * Un calque d'affichage d'une grille.
 *
 * @version $Revision: 1.18 $ $Date: 2006-11-14 09:06:23 $ by $Author: deniger $
 * @author Axel von Arnim , Guillaume Desnoix (clip)
 */
public class BCalqueGrilleReguliere extends BCalqueAffichage {

    private double[][] vvz_;

    private Color[][] vvc_;

    private double[][] vvv_;

    double zmin_, zmax_;

    public BCalqueGrilleReguliere() {
        super();
        boite_ = null;
        palette_ = new BPaletteCouleurSimple();
        paramGouraud_ = new BParametresGouraud();
        vvz_ = null;
        vvc_ = null;
        zmin_ = 1.;
        zmax_ = -1.;
        surface_ = true;
        contour_ = false;
        isosurfaces_ = false;
        isolignes_ = false;
        ecart_ = 0.05;
    }

    public void reinitialise() {
        boite_ = null;
        vvz_ = null;
        vvc_ = null;
        vvv_ = null;
    }

    public void setValeurs(final double[][] _vvz) {
        if ((_vvz == null) || (_vvz.length == 0) || (_vvz[0].length == 0)) {
            return;
        }
        vvz_ = _vvz;
        if (palette_ != null) {
            calculeCouleurs();
            construitLegende();
        }
    }

    protected void construitLegende() {
        final BCalqueLegende cqLg = getLegende();
        if (cqLg == null) {
            return;
        }
        cqLg.enleve(this);
        final BPaletteCouleurSimple paletteLeg = new BPaletteCouleurSimple();
        final BPaletteCouleurSimple privPal = (BPaletteCouleurSimple) getPaletteCouleur();
        paletteLeg.setEspace(privPal.getEspace());
        paletteLeg.setCouleurMin(privPal.getCouleurMin());
        paletteLeg.setCouleurMax(privPal.getCouleurMax());
        paletteLeg.setPaliers(privPal.getPaliers());
        paletteLeg.setCycles(privPal.getCycles());
        paletteLeg.setOrientation(BPaletteCouleurSimple.VERTICAL);
        final JComponent p = new JComponent() {

            public void paint(Graphics _g) {
                super.paint(_g);
                _g.setColor(BCalqueGrilleReguliere.this.getForeground());
                _g.setFont(BCalqueGrilleReguliere.this.getFont());
                NumberFormat nf = NumberFormat.getInstance(Locale.FRENCH);
                nf.setMaximumFractionDigits(2);
                double iv = zmin_;
                double av = zmax_;
                if (!Double.isNaN(minVal_)) {
                    iv = minVal_;
                }
                if (!Double.isNaN(maxVal_)) {
                    av = maxVal_;
                }
                _g.drawString(nf.format(av) + getM(), 0, _g.getFontMetrics().getHeight());
                _g.drawString(nf.format((iv + av) / 2) + getM(), 0, (this.getHeight() + _g.getFontMetrics().getHeight()) / 2);
                _g.drawString(nf.format(iv) + getM(), 0, this.getHeight());
            }

            public Dimension getPreferredSize() {
                if (this.isPreferredSizeSet()) {
                    return super.getPreferredSize();
                }
                NumberFormat nf = NumberFormat.getInstance(Locale.getDefault());
                nf.setMaximumFractionDigits(2);
                Graphics gcq = BCalqueGrilleReguliere.this.getGraphics();
                FontMetrics fm = BuLib.getFontMetrics(BCalqueGrilleReguliere.this, BCalqueGrilleReguliere.this.getFont());
                double iv = zmin_;
                double av = zmax_;
                if (!Double.isNaN(minVal_)) {
                    iv = minVal_;
                }
                if (!Double.isNaN(maxVal_)) {
                    av = maxVal_;
                }
                Rectangle2D r1 = fm.getStringBounds(nf.format(iv) + getM(), gcq);
                Rectangle2D r2 = fm.getStringBounds(nf.format(av) + getM(), gcq);
                Rectangle2D r3 = fm.getStringBounds(nf.format((iv + av) / 2) + getM(), gcq);
                double w = Math.max(Math.max(r1.getWidth(), r2.getWidth()), r3.getWidth());
                double h = r1.getHeight() + r2.getHeight() + r3.getHeight();
                return new Dimension((int) w, (int) h);
            }
        };
        p.setOpaque(false);
        cqLg.ajoute(this, paletteLeg, p);
    }

    public void paintIcon(final Component _c, final Graphics _g, int _x, int _y) {
        super.paintIcon(_c, _g, _x, _y);
        _g.translate(_x, _y);
        final boolean attenue = isAttenue();
        final int w = getIconWidth();
        final int h = getIconHeight();
        Color fg = getForeground();
        Color bg = getBackground();
        if (attenue) {
            fg = attenueCouleur(fg);
        }
        if (attenue) {
            bg = attenueCouleur(bg);
        }
        if (isosurfaces_) {
            Color c;
            c = palette_.couleur(0.0);
            if (attenue) {
                c = attenueCouleur(c);
            }
            _g.setColor(c);
            _g.fillRect(1, 1, w - 1, h - 1);
            c = palette_.couleur(0.5);
            if (attenue) {
                c = attenueCouleur(c);
            }
            _g.setColor(c);
            _g.fillOval(3, 3, w - 5, h - 5);
            c = palette_.couleur(1.0);
            if (attenue) {
                c = attenueCouleur(c);
            }
            _g.setColor(c);
            _g.fillOval(7, 7, w - 14, h - 14);
        }
        for (int i = 2; i < w - 5; i += 4) {
            for (int j = 2; j < h - 5; j += 4) {
                final int[] vx = new int[] { i, i + 4, i + 4, i };
                final int[] vy = new int[] { j, j, j + 4, j + 4 };
                final double v = (double) j / (double) h;
                Color c = palette_.couleur(v);
                if (attenue) {
                    c = attenueCouleur(c);
                }
                if (surface_ && !isosurfaces_) {
                    _g.setColor(c);
                    _g.fillPolygon(vx, vy, 4);
                }
                if (contour_) {
                    if (surface_ || isolignes_ || isosurfaces_) {
                        _g.setColor(bg);
                    } else {
                        _g.setColor(c);
                    }
                    _g.drawPolygon(vx, vy, 4);
                }
            }
        }
        if (isolignes_) {
            Color c;
            if (surface_ || isosurfaces_) {
                _g.setColor(fg);
            } else {
                c = palette_.couleur(0.5);
                if (attenue) {
                    c = attenueCouleur(c);
                }
                _g.setColor(c);
            }
            _g.drawOval(3, 3, w - 5, h - 5);
            if (surface_ || isosurfaces_) {
                _g.setColor(fg);
            } else {
                c = palette_.couleur(1.0);
                if (attenue) {
                    c = attenueCouleur(c);
                }
                _g.setColor(c);
            }
            _g.drawOval(7, 7, w - 14, h - 14);
        }
        _g.translate(-_x, -_y);
    }

    public void paintComponent(final Graphics _g) {
        super.paintComponent(_g);
        if (boite_ == null) {
            return;
        }
        if (vvz_ == null) {
            return;
        }
        if (isRapide()) {
            final TraceGeometrie tg = new TraceGeometrie(getVersEcran());
            tg.setForeground(getForeground());
            tg.dessinePolygone((Graphics2D) _g, boite_, false, true);
            return;
        }
        final GrBoite clip = getClipEcran(_g);
        final GrPolygone r = boite_.applique(getVersEcran());
        final GrBoite b = r.boite();
        if (b.intersectionXY(clip) == null) {
            return;
        }
        BPaletteCouleur ligPalette = palette_;
        if ((surface_ && contour_) || (surface_ && isolignes_) || (isosurfaces_ && contour_) || (isosurfaces_ && isolignes_)) {
            ligPalette = new BPaletteCouleurSimple();
            ((BPaletteCouleurSimple) ligPalette).setCouleurMin(getForeground());
            ((BPaletteCouleurSimple) ligPalette).setCouleurMax(getForeground());
        }
        final TraceIsoLignes isol = new TraceIsoLignes(ecart_, ligPalette);
        final TraceIsoSurfaces isos = new TraceIsoSurfaces(ecart_, palette_);
        Gouraud tg = null;
        if (paramGouraud_ != null) {
            tg = new Gouraud(_g, paramGouraud_.getNiveau(), paramGouraud_.getTaille());
        }
        final GrPoint o = r.sommet(0);
        final GrVecteur pasX = r.sommet(1).soustraction(o).division(vvz_.length - 1);
        final GrVecteur pasY = r.sommet(3).soustraction(o).division(vvz_[0].length - 1);
        final int imin = 0;
        final int imax = vvz_.length - 1;
        final int jmin = 0;
        final int jmax = vvz_[0].length - 1;
        final int[] x = new int[4];
        final int[] y = new int[4];
        final Color[] c = new Color[4];
        final double[] v = new double[4];
        for (int i = imin; i < imax; i++) {
            final GrPoint pi = o.addition(pasX.multiplication(i));
            for (int j = jmin; j < jmax; j++) {
                final GrPoint p0 = pi.addition(pasY.multiplication(j));
                final GrPoint p1 = p0.addition(pasX);
                final GrPoint p2 = p1.addition(pasY);
                final GrPoint p3 = p0.addition(pasY);
                boolean dedans = false;
                if (clip.contientXY(p0)) {
                    dedans = true;
                }
                if (clip.contientXY(p1)) {
                    dedans = true;
                }
                if (clip.contientXY(p2)) {
                    dedans = true;
                }
                if (clip.contientXY(p3)) {
                    dedans = true;
                }
                if (dedans) {
                    x[0] = (int) p0.x_;
                    y[0] = (int) p0.y_;
                    c[0] = vvc_[i][j];
                    v[0] = vvv_[i][j];
                    x[1] = (int) p1.x_;
                    y[1] = (int) p1.y_;
                    c[1] = vvc_[i + 1][j];
                    v[1] = vvv_[i + 1][j];
                    x[2] = (int) p2.x_;
                    y[2] = (int) p2.y_;
                    c[2] = vvc_[i + 1][j + 1];
                    v[2] = vvv_[i + 1][j + 1];
                    x[3] = (int) p3.x_;
                    y[3] = (int) p3.y_;
                    c[3] = vvc_[i][j + 1];
                    v[3] = vvv_[i][j + 1];
                    if (surface_ && (!isosurfaces_)) {
                        if (tg == null) {
                            _g.setColor(c[0]);
                            _g.fillPolygon(x, y, x.length);
                        } else {
                            tg.fillRectangle(x, y, c);
                        }
                    }
                    if (isosurfaces_) {
                        isos.draw(_g, new Polygon(x, y, x.length), v);
                    }
                    if (contour_) {
                        _g.setColor(getBackground());
                        _g.drawPolygon(x, y, x.length);
                    }
                    if (isolignes_) {
                        isol.draw(_g, new Polygon(x, y, x.length), v);
                    }
                }
            }
        }
    }

    public GrBoite getDomaine() {
        GrBoite r = null;
        if (boite_ != null) {
            r = boite_.boite();
        }
        final GrBoite d = super.getDomaine();
        if (d != null) {
            if (r == null) {
                r = d;
            } else {
                r = r.union(d);
            }
        }
        return r;
    }

    private GrPolygone boite_;

    /**
   * Accesseur de la propriete <I>rectangle</I>. Elle donne la position, la taille, le domaine de la grille.
   */
    public GrPolygone getRectangle() {
        return boite_;
    }

    /**
   * Affectation de la propriete <I>rectangle</I>.
   */
    public void setRectangle(final GrPolygone _v) {
        if (boite_ != _v) {
            if (_v.nombre() != 4) {
                throw new IllegalArgumentException("l'argument n'est pas un rectangle: il a " + _v.nombre() + " sommets");
            }
            if (((int) (_v.arete(0).norme()) != (int) (_v.arete(2).norme())) || ((int) (_v.arete(1).norme()) != (int) (_v.arete(3).norme()))) {
                throw new IllegalArgumentException("l'argument n'est pas un rectangle: ses cot�s ne sont pas egaux 2 � 2");
            }
            final GrPolygone vp = boite_;
            boite_ = _v;
            firePropertyChange("rectangle", vp, _v);
        }
    }

    public void setRectangle(final GrBoite _v) {
        boite_ = _v.enPolygoneXY();
    }

    private BPaletteCouleur palette_;

    public BPaletteCouleur getPaletteCouleur() {
        return palette_;
    }

    public void setPaletteCouleur(final BPaletteCouleur _palette) {
        if (palette_ != _palette) {
            final BPaletteCouleur vp = palette_;
            palette_ = _palette;
            construitLegende();
            firePropertyChange("paletteCouleur", vp, palette_);
            if (vvz_ != null) {
                calculeCouleurs();
            }
            repaint();
        }
    }

    public Color getCouleur() {
        return getForeground();
    }

    private BParametresGouraud paramGouraud_;

    public BParametresGouraud getParametresGouraud() {
        return paramGouraud_;
    }

    public void setParametresGouraud(final BParametresGouraud _paramGouraud) {
        paramGouraud_ = _paramGouraud;
        repaint();
    }

    private boolean contour_;

    public boolean getContour() {
        return contour_;
    }

    public void setContour(final boolean _v) {
        if (_v != contour_) {
            final boolean vp = contour_;
            contour_ = _v;
            firePropertyChange("contour", vp, contour_);
            quickRepaint();
        }
    }

    private boolean surface_;

    public boolean getSurface() {
        return surface_;
    }

    public void setSurface(final boolean _v) {
        if (_v != surface_) {
            final boolean vp = surface_;
            surface_ = _v;
            firePropertyChange("surface", vp, surface_);
            quickRepaint();
        }
    }

    private boolean isolignes_;

    public boolean getIsolignes() {
        return isolignes_;
    }

    public void setIsolignes(final boolean _v) {
        if (_v != isolignes_) {
            final boolean vp = isolignes_;
            isolignes_ = _v;
            firePropertyChange("isolignes", vp, isolignes_);
            quickRepaint();
        }
    }

    private boolean isosurfaces_;

    public boolean getIsosurfaces() {
        return isosurfaces_;
    }

    public void setIsosurfaces(final boolean _v) {
        if (_v != isosurfaces_) {
            final boolean vp = isosurfaces_;
            isosurfaces_ = _v;
            firePropertyChange("isosurfaces", vp, isosurfaces_);
            quickRepaint();
        }
    }

    private double ecart_;

    public double getEcart() {
        return ecart_;
    }

    public void setEcart(final double _v) {
        double v = _v;
        double iv = zmin_;
        double av = zmax_;
        if (!Double.isNaN(minVal_)) {
            iv = minVal_;
        }
        if (!Double.isNaN(maxVal_)) {
            av = maxVal_;
        }
        v = Math.round((100. * v / (av - iv))) / 100.;
        if (v < 0.01) {
            v = 0.01;
        }
        if (v > 0.50) {
            v = 0.50;
        }
        if (v != ecart_) {
            final double vp = ecart_;
            ecart_ = v;
            firePropertyChange("ecart", vp, ecart_);
            quickRepaint();
        }
    }

    double minVal_ = Double.NaN;

    public double getMinValeur() {
        return minVal_;
    }

    public void setMinValeur(final double _v) {
        if (_v != minVal_) {
            final double vp = minVal_;
            minVal_ = _v;
            firePropertyChange("minValeur", vp, minVal_);
            calculeCouleurs();
            quickRepaint();
        }
    }

    double maxVal_ = Double.NaN;

    public double getMaxValeur() {
        return maxVal_;
    }

    public void setMaxValeur(final double _v) {
        if (_v != maxVal_) {
            final double vp = maxVal_;
            maxVal_ = _v;
            firePropertyChange("maxValeur", vp, maxVal_);
            calculeCouleurs();
            quickRepaint();
        }
    }

    private boolean paletteLocale_;

    public boolean getPaletteLocale() {
        return paletteLocale_;
    }

    public void setPaletteLocale(final boolean _v) {
        if (_v != paletteLocale_) {
            final boolean vp = paletteLocale_;
            paletteLocale_ = _v;
            firePropertyChange("paletteLocale", vp, paletteLocale_);
            quickRepaint();
        }
    }

    private void calculeCouleurs() {
        zmin_ = vvz_[0][0];
        zmax_ = vvz_[0][0];
        for (int i = 0; i < vvz_.length; i++) {
            for (int j = 0; j < vvz_[0].length; j++) {
                if (vvz_[i][j] < zmin_) {
                    zmin_ = vvz_[i][j];
                }
                if (vvz_[i][j] > zmax_) {
                    zmax_ = vvz_[i][j];
                }
            }
        }
        double iv = zmin_;
        double av = zmax_;
        if (!Double.isNaN(minVal_)) {
            iv = minVal_;
        }
        if (!Double.isNaN(maxVal_)) {
            av = maxVal_;
        }
        vvc_ = new Color[vvz_.length][vvz_[0].length];
        vvv_ = new double[vvz_.length][vvz_[0].length];
        for (int i = 0; i < vvz_.length; i++) {
            for (int j = 0; j < vvz_[0].length; j++) {
                vvv_[i][j] = (vvz_[i][j] - iv) / (av - iv);
                vvc_[i][j] = palette_.couleur(vvv_[i][j]);
            }
        }
    }

    public static String getM() {
        return "m";
    }
}
