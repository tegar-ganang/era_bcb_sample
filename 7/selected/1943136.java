package edu.uah.eduardomoriana.qaopmvc.model.screen;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import edu.uah.eduardomoriana.oldcomputer.model.cpu.Cpu;
import edu.uah.eduardomoriana.oldcomputer.model.cpu.NeedCpu;
import edu.uah.eduardomoriana.oldcomputer.model.memory.Memory;
import edu.uah.eduardomoriana.oldcomputer.model.memory.NeedMemory;
import edu.uah.eduardomoriana.oldcomputer.model.screen.NeedScreenTransformer;
import edu.uah.eduardomoriana.oldcomputer.model.screen.Screen;
import edu.uah.eduardomoriana.oldcomputer.model.screen.ScreenTransformer;

public class ScreenImage implements Screen, NeedCpu, NeedMemory, NeedScreenTransformer {

    private Cpu cpu = null;

    private static int SCREENROWS = 24;

    private static int SCREENCOLS = 32;

    private int pixelsX = 256;

    private int pixelsY = 192;

    public final int W = pixelsX + (8 * Mh * 2);

    public final int H = pixelsY + (8 * Mv * 2);

    private int width = W;

    private int height = H;

    public int scale = 1;

    final int[] screen = new int[W / 8 * H];

    final int[] scrchg = new int[SCREENROWS];

    int flash_count = 16;

    int flash = 0x8000;

    private int refresh_t;

    private int refresh_a;

    private int refresh_b;

    private int refresh_s;

    private int refrb_p;

    private int refrb_t;

    private int refrb_x;

    private int refrb_y;

    private int refrb_r;

    private int brdchg_ud;

    private int brdchg_l;

    private int brdchg_r;

    byte border = (byte) 7;

    byte border_solid = -1;

    private static final int Mh = 6;

    private static final int Mv = 6;

    public static final int BORDER_START = (-224 * 8 * Mv) - (4 * Mh) + 4;

    private static final int[] canonic = new int[32768];

    private Memory mem = null;

    private ScreenTransformer screenTransformer;

    static {
        for (int a = 0; a < 0x8000; a += 0x100) {
            int b = (a >> 3) & 0x0800;
            int p = (a >> 3) & 0x0700;
            int i = a & 0x0700;
            if (p != 0) {
                p |= b;
            }
            if (i != 0) {
                i |= b;
            }
            canonic[a] = (p << 4) | 0xFF;
            canonic[a | 0xFF] = (i << 4) | 0xFF;
            for (int m = 1; m < 255; m += 2) {
                if (i != p) {
                    int xm = m >>> 4 | (m << 4);
                    xm = (xm >>> 2 & 0x33) | ((xm << 2) & 0xCC);
                    xm = (xm >>> 1 & 0x55) | ((xm << 1) & 0xAA);
                    canonic[a | m] = (i << 4) | p | xm;
                    canonic[a | (m ^ 0xFF)] = (p << 4) | i | xm;
                } else {
                    canonic[a | m] = canonic[a | (m ^ 0xFF)] = (p << 4) | 0xFF;
                }
            }
        }
    }

    public void refresh_new() {
        refresh_t = refresh_b = 0;
        refresh_s = (Mv * W) + Mh;
        refresh_a = 0x1800;
        refrb_p = 0;
        refrb_t = BORDER_START;
        refrb_x = -Mh;
        refrb_y = -8 * Mv;
        refrb_r = 1;
    }

    /**
	 * Creamos la imagen con el tamaño actual y refrescamos la pantalla completa
	 */
    private void newImage(int ft) {
        screenTransformer.newScreen(width, height);
        force_redraw();
        refresh_screen(ft);
    }

    /**
	 * Vamos a solicitar todos los observadores para añadirles la imagen
	 */
    public boolean update_screens(int ft) {
        if (screenTransformer.getScreen() == null) newImage(ft);
        if (update_screen()) {
            return true;
        } else {
            return false;
        }
    }

    /**
	 * Refresca la pantalla obteniendo la información de la interfaz
	 * de memoria
	 */
    public final void refresh_screen(int ft) {
        if (ft < refresh_t) {
            return;
        }
        final int flash = this.flash;
        int a = refresh_a;
        int b = refresh_b;
        int t = refresh_t;
        int s = refresh_s;
        do {
            int sch = 0;
            int v = (mem.getRam()[a] << 8) | mem.getRam()[b++];
            if (v >= 0x8000) {
                v ^= flash;
            }
            v = canonic[v];
            if (v != screen[s]) {
                screen[s] = v;
                sch = 1;
            }
            v = (mem.getRam()[a + 1] << 8) | mem.getRam()[b++];
            if (v >= 0x8000) {
                v ^= flash;
            }
            v = canonic[v];
            if (v != screen[++s]) {
                screen[s] = v;
                sch += 2;
            }
            if (sch != 0) {
                int index = (a - 0x1800) >> 5;
                if (index != -192) scrchg[index] |= (sch << (a & 31));
            }
            a += 2;
            t += 8;
            s++;
            if ((a & 31) != 0) {
                continue;
            }
            t += 96;
            s += (2 * Mh);
            a -= SCREENCOLS;
            b += (pixelsX - SCREENCOLS);
            if ((b & 0x700) != 0) {
                continue;
            }
            a += SCREENCOLS;
            b += (SCREENCOLS - 0x800);
            if ((b & 0xE0) != 0) {
                continue;
            }
            b += (0x800 - pixelsX);
            if (b >= 6144) {
                t = 99999;
                break;
            }
        } while (ft >= t);
        refresh_a = a;
        refresh_b = b;
        refresh_t = t;
        refresh_s = s;
    }

    /**
	 * Método para refrescar el borde de la pantalla
	 */
    public void refresh_border(int ft) {
        if (ft < refrb_t) {
            return;
        }
        border_solid = -1;
        int t = refrb_t;
        int b = canonic[border << 11];
        int p = refrb_p;
        int x = refrb_x;
        int r = refrb_r;
        loop: do {
            if ((refrb_y < 0) || (refrb_y >= pixelsY)) {
                do {
                    if (screen[p] != b) {
                        screen[p] = b;
                        brdchg_ud |= r;
                    }
                    p++;
                    t += 4;
                    if (++x < (SCREENCOLS + Mh)) {
                        continue;
                    }
                    x = -Mh;
                    t += (224 - (4 * (Mh + SCREENCOLS + Mh)));
                    if ((++refrb_y & 7) != 0) {
                        continue;
                    }
                    if (refrb_y == 0) {
                        r = 1;
                        continue loop;
                    } else if (refrb_y == (pixelsY + (8 * Mv))) {
                        t = 99999;
                        break loop;
                    }
                    r <<= 1;
                } while (ft >= t);
                break;
            }
            for (; ; ) {
                if (x < 0) {
                    for (; ; ) {
                        if (screen[p] != b) {
                            screen[p] = b;
                            brdchg_l |= r;
                        }
                        p++;
                        t += 4;
                        if (++x == 0) {
                            break;
                        }
                        if (ft < t) {
                            break loop;
                        }
                    }
                    x = SCREENCOLS;
                    p += SCREENCOLS;
                    t += (4 * SCREENCOLS);
                    if (ft < t) {
                        break loop;
                    }
                }
                for (; ; ) {
                    if (screen[p] != b) {
                        screen[p] = b;
                        brdchg_r |= r;
                    }
                    p++;
                    t += 4;
                    if (++x == (SCREENCOLS + Mh)) {
                        break;
                    }
                    if (ft < t) {
                        break loop;
                    }
                }
                x = -Mh;
                t += (224 - (4 * (Mh + SCREENCOLS + Mh)));
                if ((++refrb_y & 7) == 0) {
                    if (refrb_y == pixelsY) {
                        r = 1 << Mv;
                        continue loop;
                    }
                    r <<= 1;
                }
                if (ft < t) {
                    break loop;
                }
            }
        } while (ft >= t);
        refrb_r = r;
        refrb_x = x;
        refrb_p = p;
        refrb_t = t;
    }

    /**
	 * Se hace una actualización de la pantalla si se modificó
	 * @return
	 */
    public boolean update_screen() {
        int brdchg_ud_aux = brdchg_ud;
        brdchg_ud = 0;
        int bl = brdchg_l;
        brdchg_l = 0;
        int br = brdchg_r;
        brdchg_r = 0;
        boolean chg = (brdchg_ud_aux | bl | br) != 0;
        int[] buf = new int[8 * W * scale * scale];
        if (brdchg_ud_aux != 0) {
            for (int r = 0; r < Mv; r++, brdchg_ud_aux >>>= 1) if ((brdchg_ud_aux & 1) != 0) {
                update_box(r, 0, Mh + SCREENCOLS + Mh, buf);
            }
        }
        for (int r = 0; r < SCREENROWS; r++) {
            int d = scrchg[r];
            scrchg[r] = 0;
            int x = Mh;
            int n = 0;
            if ((bl & 1) != 0) {
                n = x;
                x = 0;
            }
            for (; ; ) {
                while ((d & 1) != 0) {
                    n++;
                    d >>>= 1;
                }
                if (n != 0) {
                    if (((x + n) == (Mh + SCREENCOLS)) && ((br & 1) != 0)) {
                        n += Mh;
                    }
                    chg = true;
                    update_box(Mv + r, x, n, buf);
                    x += n;
                    if (x >= (Mh + SCREENCOLS)) {
                        break;
                    }
                }
                if (d == 0) {
                    if ((br & 1) == 0) {
                        break;
                    }
                    x = Mh + SCREENCOLS;
                    n = Mh;
                    continue;
                }
                do {
                    x++;
                    d >>>= 1;
                } while ((d & 1) == 0);
                n = 1;
                d >>>= 1;
            }
            bl >>>= 1;
            br >>>= 1;
        }
        if (brdchg_ud_aux != 0) {
            for (int r = Mv + SCREENROWS; r < (Mv + SCREENROWS + Mv); r++, brdchg_ud_aux >>>= 1) if ((brdchg_ud_aux & 1) != 0) {
                update_box(r, 0, Mh + SCREENCOLS + Mh, buf);
            }
        }
        return chg;
    }

    /**
	 * Se obliga a repintar la pantalla asignando valores null
	 */
    public void force_redraw() {
        for (int i = 0; i < screen.length; i++) screen[i] = 0x1FF;
        border_solid = -1;
    }

    /**
	 * Método para refrescar parcialmente la parte de la pantalla que se modificó
	 * @param y
	 * @param x
	 * @param w
	 * @param buf
	 */
    private final void update_box(int y, int x, int w, int[] buf) {
        int si = (y * W) + x;
        int p = 0;
        x <<= 3;
        y <<= 3;
        int h;
        int s;
        if (scale == 1) {
            s = w * 8;
            for (int n = 0; n < 8; n++) {
                for (int k = 0; k < w; k++) {
                    int m = screen[si++];
                    byte c0 = (byte) (m >>> 8 & 0xF);
                    byte c1 = (byte) (m >>> 12);
                    m &= 0xFF;
                    do buf[p++] = ((m & 1) == 0) ? c0 : c1; while ((m >>>= 1) != 0);
                }
                si += ((W / 8) - w);
            }
            h = 8;
        } else {
            h = scale << 3;
            s = w * h;
            for (int n = 0; n < 8; n++) {
                for (int k = 0; k < w; k++) {
                    int m = screen[si++];
                    byte c0 = (byte) (m >>> 8 & 0xF);
                    byte c1 = (byte) (m >>> 12);
                    m &= 0xFF;
                    do {
                        buf[p] = buf[p + 1] = buf[p + s] = buf[p + s + 1] = ((m & 1) == 0) ? c0 : c1;
                        p += 2;
                    } while ((m >>>= 1) != 0);
                }
                p += s;
                si += ((W / 8) - w);
            }
            x *= scale;
            y *= scale;
        }
        try {
            screenTransformer.doit(buf, x, y, h, s);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * Método para modificar el escalado
	 * @param scale
	 */
    public void scale(int scale, int ft) {
        this.scale = scale;
        width = scale * W;
        height = scale * H;
        newImage(ft);
    }

    /**
	 * Seter para la implementación de la Memoria
	 * @param mem
	 */
    public void setMemory(Memory mem) {
        this.mem = mem;
    }

    public byte getBorder_solid() {
        return border_solid;
    }

    public void setBorder_solid(byte border_solid) {
        this.border_solid = border_solid;
    }

    public byte getBorder() {
        return border;
    }

    public void setBorder(byte border) {
        this.border = border;
    }

    public int getRefrb_t() {
        return refrb_t;
    }

    public void setRefrb_t(int refrb_t) {
        this.refrb_t = refrb_t;
    }

    public Object getImage() {
        return screenTransformer.getScreen();
    }

    public void setCpu(Cpu cpu) {
        this.cpu = cpu;
    }

    public int getScale() {
        return scale;
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    public int getPixelsX() {
        return pixelsX;
    }

    public void setPixelsX(int pixelsX) {
        this.pixelsX = pixelsX;
    }

    public int getPixelsY() {
        return pixelsY;
    }

    public void setPixelsY(int pixelsY) {
        this.pixelsY = pixelsY;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setScreenTransformer(ScreenTransformer transformer) {
        this.screenTransformer = transformer;
    }
}
