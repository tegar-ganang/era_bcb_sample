package trojuhelnik;

import java.util.Vector;

public class Spolecne {

    public final double PR = 0.00001;

    public final double PI = Math.PI;

    public ZadaniInt zadani;

    public KompletInt komplet;

    public CompLibInt compLib;

    public Vypocty1 vypocty1;

    public Vypocty2 vypocty2;

    public Vypocty3 vypocty3;

    public Vypocty4 vypocty4;

    public Vypocty5 vypocty5;

    public Vypocty6 vypocty6;

    public Funkce1 funkce1;

    public Funkce2 funkce2;

    public Funkce3 funkce3;

    private Vector reseni = null;

    /** Creates a new instance of Spolecne */
    public Spolecne(ZadaniInt zadani, KompletInt komplet, CompLibInt compLib) {
        this.zadani = zadani;
        this.komplet = komplet;
        this.compLib = compLib;
        vypocty1 = new Vypocty1(this);
        vypocty2 = new Vypocty2(this);
        vypocty3 = new Vypocty3(this);
        vypocty4 = new Vypocty4(this);
        vypocty5 = new Vypocty5(this);
        vypocty6 = new Vypocty6(this);
        funkce1 = new Funkce1(this);
        funkce2 = new Funkce2(this);
        funkce3 = new Funkce3(this);
    }

    public class Double2 {

        public double b, c;

        public Double2(double a1, double b1) {
            b = a1;
            c = b1;
        }
    }

    public class Double2M {

        public double h, u;

        public Double2M(double a1, double b1) {
            h = a1;
            u = b1;
        }
    }

    public class Double3 {

        public double a, b, c;

        public Double3(double a1, double b1, double c1) {
            a = a1;
            b = b1;
            c = c1;
        }
    }

    public interface D3 {

        public double f(double x1, double x2, double x3);
    }

    public interface D3B {

        public double f(double x1, double x2, double x3, boolean tp);
    }

    public double cs(double a, double b, double ga) {
        return (Math.sqrt(a * a + b * b - 2 * a * b * Math.cos(ga)));
    }

    public double uhel1(double a, double b, double c) {
        return (compLib.acos((a * a + b * b - c * c) / 2 / a / b));
    }

    public double tez(double a, double b, double c) {
        return (0.5 * Math.sqrt(2 * (a * a + b * b) - c * c));
    }

    public double roabc(double a, double b, double c) {
        double s = (a + b + c) / 2;
        return (Math.sqrt((s - a) * (s - b) * (s - c) / s));
    }

    public double osauhlu(double a, double b, double c) {
        return (Math.sqrt(a * b * ((a + b) * (a + b) - c * c)) / (a + b));
    }

    public double cabal(double a, double b, double al) {
        return (b * Math.cos(al) + Math.sqrt(a * a - (b * b * Math.sin(al) * Math.sin(al))));
    }

    public double cabal1(double a, double b, double al) {
        return (b * Math.cos(al) - Math.sqrt(a * a - (b * b * Math.sin(al) * Math.sin(al))));
    }

    public double strtx(double a, double b, double ta) {
        return (Math.sqrt(a * a / 2 - b * b + 2 * ta * ta));
    }

    public double strux(double ua, double al, double be) {
        return (ua * Math.sin(al / 2) * (1 / Math.sin(be) + 1 / Math.sin(al + be)));
    }

    public double strtux(double ub, double al, double be) {
        if (Math.abs(al + be - PI / 2) < PR) return (ub * Math.cos(be / 2)); else return (ub * (Math.cos(be / 2) + Math.sin(be / 2) / Math.tan(PI - al - be)));
    }

    public double dr(double r, double va, double al) {
        if (Math.abs(r * (1 + Math.cos(al)) - va) < PR) return (0.0); else return (Math.sqrt(r * r - ((r * Math.cos(al) - va) * (r * Math.cos(al) - va))));
    }

    public double avabe(double a, double va, double be) {
        double ga;
        if (Math.abs(a - va / Math.tan(be)) < PR) ga = PI / 2; else ga = compLib.atan(va / (a - va / Math.tan(be)));
        if (ga < 0) ga = PI + ga;
        return ga;
    }

    public double gata(double a, double ta, double e) {
        double ga;
        if (Math.abs(a / 2 - ta * Math.cos(e)) < PR) ga = PI / 2; else ga = compLib.atan(ta * Math.sin(e) / (a / 2 - ta * Math.cos(e)));
        if (ga < 0) ga = PI + ga;
        return ga;
    }

    public double gatb(double a, double tb, double e) {
        double ga;
        if (Math.abs(a - tb * Math.cos(e)) < PR) ga = PI / 2; else ga = compLib.atan(tb * Math.sin(e) / (a - tb * Math.cos(e)));
        if (ga < 0) ga = PI + ga;
        return ga;
    }

    public double garo(double a, double ro, double be) {
        double ga;
        if (Math.abs(a - ro / Math.tan(be / 2)) < PR) ga = PI / 2; else ga = 2 * compLib.atan(ro / (a - ro / Math.tan(be / 2)));
        if (ga < 0) ga = PI + ga;
        return ga;
    }

    public double gaub(double a, double ub, double be) {
        double ga;
        if (Math.abs(a - ub * Math.cos(be / 2)) < PR) ga = PI / 2; else ga = compLib.atan(ub * Math.sin(be / 2) / (a - ub * Math.cos(be / 2)));
        if (ga < 0) ga = PI + ga;
        return ga;
    }

    public double gavaub(double va, double ub, double be) {
        double ga;
        if (Math.abs(ub - va / Math.tan(be) / Math.cos(be / 2)) < PR) ga = PI / 2; else ga = compLib.atan((va - ub * Math.sin(be / 2)) / (ub * Math.cos(be / 2) - va / Math.tan(be)));
        if (ga < 0) ga = PI + ga;
        return ga;
    }

    public double eta(double a, double ua, double al) {
        double m = ((2 * a * Math.sin(al / 2)) * (2 * a * Math.sin(al / 2)));
        double n = ua * Math.sin(al);
        return compLib.acos(0.5 * (n + Math.sqrt(n * n + m)) / a);
    }

    public double altaua(double ta, double ua, double e) {
        double k = Math.sqrt(ta * ta - ((ua * Math.cos(e)) * (ua * Math.cos(e)))) - ua * Math.abs(Math.sin(e));
        return (2 * compLib.asin(Math.cos(e) * Math.sqrt(k / (ua * Math.abs(Math.sin(e)) + k))));
    }

    public double alpua(double ua, double p, double e) {
        double a, k, va, al;
        va = ua * Math.cos(e);
        a = 2 * p / va;
        k = Math.tan(e) * Math.tan(e);
        if (k == 0) al = 2 * compLib.atan(p / ua / ua); else al = 2 * compLib.atan(va / k / a * (-1 - k + Math.sqrt((1 + k) * (1 + k) + k * a * a / va / va)));
        if (al < 0) al = PI + al;
        return al;
    }

    public double roral(double r, double ro, double al) {
        double k, be;
        k = 2 * r * Math.sin(al) / ro;
        be = 2 * compLib.atan((k - Math.sqrt(k * k - 4 * (1 + k * Math.tan(al / 2)))) / 2 / (1 + k * Math.tan(al / 2)));
        if (be < 0) be = PI + be;
        return be;
    }

    public double hodnota(Spolecne.D3 F, double x, double y, double hodn, double m1, double m2) {
        double uhel, fm;
        int iter = 0;
        do {
            uhel = (m1 + m2) / 2;
            fm = F.f(x, y, uhel);
            if (fm < hodn) m1 = uhel; else m2 = uhel;
            iter = iter + 1;
            if (iter > 100) break;
        } while (Math.abs(hodn - fm) > 1.0E-10);
        return uhel;
    }

    public double hodnota1(Spolecne.D3B F, double x, double y, double hodn, double m1, double m2, boolean tp) {
        double uhel, fm;
        int iter = 0;
        do {
            uhel = (m1 + m2) / 2;
            fm = F.f(x, y, uhel, tp);
            if (fm < hodn) m1 = uhel; else m2 = uhel;
            iter = iter + 1;
            if (iter > 100) break;
        } while (Math.abs(hodn - fm) > 1.0E-10);
        return uhel;
    }

    public double mez(double r, double ro, double uhel, double krok) {
        double va;
        for (int k = 1; k <= 6; k++) {
            do {
                va = ro * ro / r / (1 - Math.cos(uhel)) + 2 * ro;
                if (va > r * (1 + Math.cos(uhel))) uhel = uhel + krok;
            } while (va > r * (1 + Math.cos(uhel)));
            uhel = uhel - krok;
            krok = krok / 10;
        }
        return uhel + 10 * krok;
    }

    public Double2M mez1(double p, double r, double uhel, double krok) {
        double va = 0.0;
        for (int k = 1; k <= 6; k++) {
            do {
                va = p / r / Math.sin(uhel);
                if (va > r * (1 + Math.cos(uhel))) uhel = uhel + krok;
            } while (va > r * (1 + Math.cos(uhel)));
            uhel = uhel - krok;
            krok = krok / 10;
        }
        return new Double2M(va, uhel + 10 * krok);
    }

    public Double2M mez2(double p, double s, double abb, double krok) {
        double c, m;
        double vc = 0.0;
        for (int k = 1; k <= 6; k++) {
            do {
                c = 2 * s - abb;
                vc = 2 * p / c;
                m = vc * vc / (abb * abb - c * c);
                abb = abb + krok;
            } while (m >= 0.25);
            abb = abb - 2 * krok;
            krok = krok / 10;
        }
        return new Double2M(vc, abb + 10 * krok);
    }

    public Double2M max(Spolecne.D3 F, double x, double y, double uhel, double krok, double phodn) {
        double hodn, t;
        for (int k = 1; k <= 6; k++) {
            do {
                hodn = F.f(x, y, uhel);
                t = phodn;
                if (hodn > phodn) {
                    phodn = hodn;
                    uhel = uhel + krok;
                }
            } while (hodn >= t);
            phodn = hodn;
            krok = -krok / 10;
            uhel = uhel + krok;
        }
        return new Double2M(phodn, uhel - krok);
    }

    public Double2M max1(Spolecne.D3B F, double x, double y, double uhel, double krok, double phodn, boolean tp) {
        double hodn, t;
        for (int k = 1; k <= 6; k++) {
            do {
                hodn = F.f(x, y, uhel, tp);
                t = phodn;
                if (hodn > phodn) {
                    phodn = hodn;
                    uhel = uhel + krok;
                }
            } while (hodn >= t);
            phodn = hodn;
            krok = -krok / 10;
            uhel = uhel + krok;
        }
        return new Double2M(phodn, uhel - krok);
    }

    public Double2M max2(Spolecne.D3 F, double x, double y, double uhel, double krok, double phodn, double mez) {
        double hodn, t;
        for (int k = 1; k <= 6; k++) {
            do {
                if (Math.abs(mez - uhel) < krok) {
                    uhel = mez;
                    break;
                }
                hodn = F.f(x, y, uhel);
                t = phodn;
                if (hodn > phodn) {
                    phodn = hodn;
                    uhel = uhel + krok;
                }
            } while (hodn >= t);
            phodn = F.f(x, y, uhel);
            krok = -krok / 10;
            uhel = uhel + krok;
        }
        return new Double2M(phodn, uhel - krok);
    }

    public Double2M max3(Spolecne.D3B F, double x, double y, double uhel, double krok, double phodn, double mez, boolean tp) {
        double hodn, t;
        for (int k = 1; k <= 6; k++) {
            do {
                if (Math.abs(mez - uhel) < krok) {
                    uhel = mez;
                    break;
                }
                hodn = F.f(x, y, uhel, tp);
                t = phodn;
                if (hodn > phodn) {
                    phodn = hodn;
                    uhel = uhel + krok;
                }
            } while (hodn >= t);
            phodn = F.f(x, y, uhel, tp);
            krok = -krok / 10;
            uhel = uhel + krok;
        }
        return new Double2M(phodn, uhel - krok);
    }

    public Double2M min(Spolecne.D3 F, double x, double y, double uhel, double krok, double phodn) {
        double hodn, t;
        for (int k = 1; k <= 6; k++) {
            do {
                hodn = F.f(x, y, uhel);
                t = phodn;
                if (hodn < phodn) {
                    phodn = hodn;
                    uhel = uhel + krok;
                }
            } while (hodn <= t);
            phodn = hodn;
            krok = -krok / 10;
            uhel = uhel + krok;
        }
        return new Double2M(phodn, uhel - krok);
    }

    public Double2M min1(Spolecne.D3B F, double x, double y, double uhel, double krok, double phodn, boolean tp) {
        double hodn, t;
        for (int k = 1; k <= 6; k++) {
            do {
                hodn = F.f(x, y, uhel, tp);
                t = phodn;
                if (hodn < phodn) {
                    phodn = hodn;
                    uhel = uhel + krok;
                }
            } while (hodn <= t);
            phodn = hodn;
            krok = -krok / 10;
            uhel = uhel + krok;
        }
        return new Double2M(phodn, uhel - krok);
    }

    public Double2M min2(Spolecne.D3 F, double x, double y, double uhel, double krok, double phodn, double mez) {
        double hodn, t;
        for (int k = 1; k <= 6; k++) {
            do {
                if (Math.abs(mez - uhel) < krok) {
                    uhel = mez;
                    break;
                }
                hodn = F.f(x, y, uhel);
                t = phodn;
                if (hodn < phodn) {
                    phodn = hodn;
                    uhel = uhel + krok;
                }
            } while (hodn <= t);
            phodn = F.f(x, y, uhel);
            krok = -krok / 10;
            uhel = uhel + krok;
        }
        return new Double2M(phodn, uhel - krok);
    }

    public Double3 abcrval(double r, double va, double al) {
        double d, a, b, c;
        d = dr(r, va, al);
        a = 2 * r * Math.sin(al);
        b = Math.sqrt((d - a / 2) * (d - a / 2) + va * va);
        c = Math.sqrt((d + a / 2) * (d + a / 2) + va * va);
        return new Double3(a, b, c);
    }

    public Double3 abcro(double ro, double al, double be, double ga) {
        double a, b, c;
        a = ro * (1 / Math.tan(be / 2) + 1 / Math.tan(ga / 2));
        b = ro * (1 / Math.tan(al / 2) + 1 / Math.tan(ga / 2));
        c = ro * (1 / Math.tan(al / 2) + 1 / Math.tan(be / 2));
        return new Double3(a, b, c);
    }

    public Double3 abcrova(double va, double ro, double e) {
        double al, a, b, c;
        al = 2 * compLib.asin(ro * Math.cos(e) / (va - ro));
        b = va / Math.cos(e - al / 2);
        c = va / Math.cos(e + al / 2);
        a = cs(b, c, al);
        return new Double3(a, b, c);
    }

    public Double3 abcva(double va, double al, double e) {
        double a, b, c;
        a = va * (Math.tan(al / 2 + e) + Math.tan(al / 2 - e));
        b = va / Math.cos(al / 2 - e);
        c = va / Math.cos(al / 2 + e);
        return new Double3(a, b, c);
    }

    public Double3 abcubva(double va, double ub, double be) {
        double c = va / Math.sin(be);
        double al = gaub(c, ub, be);
        Double2 z = bca(c, al + be, al, be);
        return new Double3(z.b, z.c, c);
    }

    public Double3 altae(double al, double ta, double e) {
        double k, a, b, c;
        k = Math.sin(e) / Math.tan(al);
        a = 2 * ta * (-k + Math.sqrt(k * k + 1));
        b = Math.sqrt((ta * Math.cos(e) - a / 2) * (ta * Math.cos(e) - a / 2) + (ta * Math.sin(e)) * (ta * Math.sin(e)));
        c = Math.sqrt((ta * Math.cos(e) + a / 2) * (ta * Math.cos(e) + a / 2) + (ta * Math.sin(e)) * (ta * Math.sin(e)));
        return new Double3(a, b, c);
    }

    public Double3 abcua(double ua, double al, double e) {
        double a, b, c;
        a = ua * Math.sin(al / 2) * (1 / Math.cos(al / 2 + e) + 1 / Math.cos(al / 2 - e));
        b = ua * Math.cos(e) / Math.cos(al / 2 - e);
        c = ua * Math.cos(e) / Math.cos(al / 2 + e);
        return new Double3(a, b, c);
    }

    public Double3 abcual(double ua, double al, double be) {
        double a = strux(ua, al, be);
        Double2 z = bca(a, al, be, al + be);
        return new Double3(a, z.b, z.c);
    }

    public Double3 abcub(double ub, double al, double be) {
        double a, b, c;
        a = ub * Math.sin(al + be / 2) / Math.sin(al + be);
        b = a * Math.sin(be) / Math.sin(al);
        c = a * Math.sin(al + be) / Math.sin(al);
        return new Double3(a, b, c);
    }

    public Double3 vabega(double va, double be, double ga) {
        double a, b, c;
        a = va * (1 / Math.tan(be) + 1 / Math.tan(ga));
        b = va / Math.sin(ga);
        c = va / Math.sin(be);
        return new Double3(a, b, c);
    }

    public Double3 vaega(double va, double e, double ga) {
        double a, b, c;
        a = va / 2 * (1 / Math.tan(e) + 1 / Math.tan(ga));
        b = va / Math.sin(ga);
        c = cs(a, b, ga);
        return new Double3(a, b, c);
    }

    public Double3 vavbga(double va, double vb, double ga) {
        double a, b, c;
        a = vb / Math.sin(ga);
        b = va / Math.sin(ga);
        c = cs(a, b, ga);
        return new Double3(a, b, c);
    }

    public Double3 vataga(double va, double e, double ga) {
        double a, b, c;
        a = 2 * va * (1 / Math.tan(e) + 1 / Math.tan(ga));
        b = va / Math.sin(ga);
        c = cs(a, b, ga);
        return new Double3(a, b, c);
    }

    public Double3 vapbe(double va, double p, double be) {
        double a, b, c;
        a = 2 * p / va;
        c = va / Math.sin(be);
        b = cs(a, c, be);
        return new Double3(a, b, c);
    }

    public Double3 vaobbe(double va, double ob, double be) {
        double a, b, c, ab;
        c = va / Math.sin(be);
        ab = ob - c;
        a = (ab * ab - c * c) / 2 / (ab - c * Math.cos(be));
        b = cs(a, c, be);
        return new Double3(a, b, c);
    }

    public Double3 abctatb(double ta, double tb, double e) {
        double a, b, c;
        a = 2 * cs(ta, 2 * tb, e) / 3;
        b = 2 * cs(2 * ta, tb, e) / 3;
        c = 2 * cs(ta, tb, PI - e) / 3;
        return new Double3(a, b, c);
    }

    public Double3 abctaub(double ta, double e, double be) {
        double a, b, c;
        a = 2 * ta * (Math.sin(e) / Math.tan(be) - Math.cos(e));
        c = ta * Math.sin(e) / Math.sin(be);
        b = cs(a, c, be);
        return new Double3(a, b, c);
    }

    public Double3 abctar(double ta, double r, double al, double e) {
        double a = 2 * r * Math.sin(al);
        Double2 z = bcta(a, ta, e);
        return new Double3(a, z.b, z.c);
    }

    public Double3 abctap(double ta, double p, double e) {
        double a, b, c;
        a = 2 * p / ta / Math.sin(e);
        b = cs(a / 2, ta, e);
        c = cs(a / 2, ta, PI - e);
        return new Double3(a, b, c);
    }

    public Double2 bca(double a, double al, double be, double ga) {
        double b = a * Math.sin(be) / Math.sin(al);
        double c = a * Math.sin(ga) / Math.sin(al);
        return new Double2(b, c);
    }

    public Double2 bcta(double a, double ta, double e) {
        double b = cs(a / 2, ta, e);
        double c = cs(a / 2, ta, PI - e);
        return new Double2(b, c);
    }

    public Double2 bctb(double a, double tb, double e) {
        double b = 2 * cs(a, tb, e);
        double c = strtx(b, a, tb);
        return new Double2(b, c);
    }

    public Double3 taroo(double ta, double ro, double o) {
        double va, al, e;
        va = ta * Math.cos(o);
        e = compLib.acos((va - ro) * (va - ro) / Math.sqrt((ta * ta - va * va) * (va - 2 * ro) * (va - 2 * ro) + (va - ro) * (va - ro) * (va - ro) * (va - ro)));
        if (o < 0) e = -e;
        al = 2 * compLib.asin(ro * Math.cos(e) / (va - ro));
        return new Double3(va, al, e);
    }

    public double taral(double ta, double r, double al) {
        double va = r * (Math.cos(al) - ((Math.cos(al) * Math.cos(al)) + 1 - ta * ta / r / r) / 2 / Math.cos(al));
        return va;
    }

    public void setrideniPolozek(String[] pol) {
        int i[] = new int[11];
        int j[] = new int[3];
        int n;
        for (n = 0; n <= 2; n++) {
            if (pol[n].equals("a")) {
                i[n] = 1;
                j[n] = 1;
            } else if (pol[n].equals("b")) {
                i[n] = 1;
                j[n] = 2;
            } else if (pol[n].equals("c")) {
                i[n] = 1;
                j[n] = 3;
            } else if (pol[n].equals("alfa")) {
                i[n] = 2;
                j[n] = 1;
            } else if (pol[n].equals("beta")) {
                i[n] = 2;
                j[n] = 2;
            } else if (pol[n].equals("gama")) {
                i[n] = 2;
                j[n] = 3;
            } else if (pol[n].equals("va")) {
                i[n] = 3;
                j[n] = 1;
            } else if (pol[n].equals("vb")) {
                i[n] = 3;
                j[n] = 2;
            } else if (pol[n].equals("vc")) {
                i[n] = 3;
                j[n] = 3;
            } else if (pol[n].equals("ta")) {
                i[n] = 4;
                j[n] = 1;
            } else if (pol[n].equals("tb")) {
                i[n] = 4;
                j[n] = 2;
            } else if (pol[n].equals("tc")) {
                i[n] = 4;
                j[n] = 3;
            } else if (pol[n].equals("ua")) {
                i[n] = 5;
                j[n] = 1;
            } else if (pol[n].equals("ub")) {
                i[n] = 5;
                j[n] = 2;
            } else if (pol[n].equals("uc")) {
                i[n] = 5;
                j[n] = 3;
            } else if (pol[n].equals("p")) i[n] = 6; else if (pol[n].equals("r")) i[n] = 7; else if (pol[n].equals("ro")) i[n] = 8; else if (pol[n].equals("ab")) i[n] = 9; else if (pol[n].equals("ob")) i[n] = 10; else if (pol[n].equals("z")) i[n] = 11;
        }
        int d = 0;
        if (i[2] == 9 || i[1] == 9 || i[0] > 5 || i[2] == 11) d = 0; else {
            if (i[0] == i[1]) {
                if (j[0] == 1) {
                    if (j[1] == 2) d = 0; else if (j[1] == 3) d = 2;
                } else d = 1;
            } else d = j[0] - 1;
        }
        if (i[0] < 6 && d > 0) {
            zadaniHlaseni("CHANGE");
        }
        for (n = 0; n <= 2; n++) {
            if (i[n] > 5) continue;
            j[n] = j[n] - d;
            if (j[n] < 1) j[n] = j[n] + 3;
        }
        for (n = 0; n <= 1; n++) {
            if (i[n] == i[n + 1]) {
                if (j[n] > j[n + 1]) {
                    int m = j[n];
                    j[n] = j[n + 1];
                    j[n + 1] = m;
                }
            }
        }
        for (n = 0; n <= 2; n++) {
            if (i[n] == 1 && j[n] == 1) pol[n] = "a"; else if (i[n] == 1 && j[n] == 2) pol[n] = "b"; else if (i[n] == 1 && j[n] == 3) pol[n] = "c"; else if (i[n] == 2 && j[n] == 1) pol[n] = "alfa"; else if (i[n] == 2 && j[n] == 2) pol[n] = "beta"; else if (i[n] == 2 && j[n] == 3) pol[n] = "gama"; else if (i[n] == 3 && j[n] == 1) pol[n] = "va"; else if (i[n] == 3 && j[n] == 2) pol[n] = "vb"; else if (i[n] == 3 && j[n] == 3) pol[n] = "vc"; else if (i[n] == 4 && j[n] == 1) pol[n] = "ta"; else if (i[n] == 4 && j[n] == 2) pol[n] = "tb"; else if (i[n] == 4 && j[n] == 3) pol[n] = "tc"; else if (i[n] == 5 && j[n] == 1) pol[n] = "ua"; else if (i[n] == 5 && j[n] == 2) pol[n] = "ub"; else if (i[n] == 5 && j[n] == 3) pol[n] = "uc"; else if (i[n] == 6) pol[n] = "p"; else if (i[n] == 7) pol[n] = "r"; else if (i[n] == 8) pol[n] = "ro"; else if (i[n] == 9) pol[n] = "ab"; else if (i[n] == 10) pol[n] = "ob"; else if (i[n] == 11) pol[n] = "z";
        }
    }

    public double[] kontrolaVypocet(String l1, String l2, String l3, String t1, String t2, String t3) {
        double[] result = { Double.NaN, Double.NaN, Double.NaN };
        if ((t1.equals("")) || (t2.equals("")) || (t3.equals(""))) {
            hlaseni("VALUES");
            return result;
        }
        double h1, h2, h3;
        try {
            h1 = compLib.parseDouble(t1);
            h2 = compLib.parseDouble(t2);
            h3 = compLib.parseDouble(t3);
        } catch (Exception e) {
            hlaseni("NUMBER_FORMAT");
            return result;
        }
        if (l1.equals("alfa") && h1 >= 180.0) {
            chyba("alfa", ">=", 180.0);
            return result;
        } else if (l1.equals("beta") && h1 >= 180.0) {
            chyba("beta", ">=", 180.0);
            return result;
        } else if (l1.equals("gama") && h1 >= 180.0) {
            chyba("gama", ">=", 180.0);
            return result;
        } else if (l2.equals("alfa") && h2 >= 180.0) {
            chyba("alfa", ">=", 180.0);
            return result;
        } else if (l2.equals("beta") && h2 >= 180.0) {
            chyba("beta", ">=", 180.0);
            return result;
        } else if (l2.equals("gama") && h2 >= 180.0) {
            chyba("gama", ">=", 180.0);
            return result;
        } else if (l3.equals("alfa") && h3 >= 180.0) {
            chyba("alfa", ">=", 180.0);
            return result;
        } else if (l3.equals("beta") && h3 >= 180.0) {
            chyba("beta", ">=", 180.0);
            return result;
        } else if (l3.equals("gama") && h3 >= 180.0) {
            chyba("gama", ">=", 180.0);
            return result;
        }
        result[0] = h1;
        result[1] = h2;
        result[2] = h3;
        return result;
    }

    public Vector spustVypocet(String typ, double h1, double h2, double h3) {
        reseni = null;
        if (typ.equals("a,b,c")) vypocty1.P001(h1, h2, h3); else if (typ.equals("a,b,alfa")) vypocty1.P002(h1, h2, Math.toRadians(h3)); else if (typ.equals("a,b,beta")) vypocty1.P003(h1, h2, Math.toRadians(h3)); else if (typ.equals("a,b,gama")) vypocty1.P004(h1, h2, Math.toRadians(h3)); else if (typ.equals("a,b,va")) vypocty1.P005(h1, h2, h3); else if (typ.equals("a,b,vb")) vypocty1.P006(h1, h2, h3); else if (typ.equals("a,b,vc")) vypocty1.P007(h1, h2, h3); else if (typ.equals("a,b,p")) vypocty1.P008(h1, h2, h3); else if (typ.equals("a,b,r")) vypocty1.P009(h1, h2, h3); else if (typ.equals("a,b,ro")) vypocty1.P010(h1, h2, h3); else if (typ.equals("a,alfa,beta")) vypocty1.P011(h1, Math.toRadians(h2), Math.toRadians(h3), true, "beta"); else if (typ.equals("a,alfa,gama")) vypocty1.P011(h1, Math.toRadians(h2), Math.toRadians(h3), false, "gama"); else if (typ.equals("a,alfa,va")) vypocty1.P013(h1, Math.toRadians(h2), h3); else if (typ.equals("a,alfa,vb")) vypocty1.P014(h1, Math.toRadians(h2), h3, true); else if (typ.equals("a,alfa,vc")) vypocty1.P014(h1, Math.toRadians(h2), h3, false); else if (typ.equals("a,alfa,p")) vypocty1.P016(h1, Math.toRadians(h2), h3); else if (typ.equals("a,alfa,r")) {
            chyba1("   a = 2*r*sin(al)");
        } else if (typ.equals("a,alfa,ro")) vypocty1.P017(h1, Math.toRadians(h2), h3); else if (typ.equals("a,beta,gama")) vypocty1.P018(h1, Math.toRadians(h2), Math.toRadians(h3)); else if (typ.equals("a,beta,va")) vypocty1.P019(h1, Math.toRadians(h2), h3, true); else if (typ.equals("a,beta,vb")) vypocty1.P021(h1, Math.toRadians(h2), h3, true, "beta"); else if (typ.equals("a,beta,vc")) {
            chyba1("   vc = a*sin(be)");
        } else if (typ.equals("a,beta,p")) vypocty1.P023(h1, Math.toRadians(h2), h3, true); else if (typ.equals("a,beta,r")) vypocty1.P024(h1, Math.toRadians(h2), h3, true, "beta"); else if (typ.equals("a,beta,ro")) vypocty1.P025(h1, Math.toRadians(h2), h3, true); else if (typ.equals("a,gama,va")) vypocty1.P019(h1, Math.toRadians(h2), h3, false); else if (typ.equals("a,gama,vb")) {
            chyba1("   vb = a*sin(ga)");
        } else if (typ.equals("a,gama,vc")) vypocty1.P021(h1, Math.toRadians(h2), h3, false, "gama"); else if (typ.equals("a,gama,p")) vypocty1.P023(h1, Math.toRadians(h2), h3, false); else if (typ.equals("a,gama,r")) vypocty1.P024(h1, Math.toRadians(h2), h3, false, "gama"); else if (typ.equals("a,gama,ro")) vypocty1.P025(h1, Math.toRadians(h2), h3, false); else if (typ.equals("a,va,vb")) vypocty1.P026(h1, h2, h3, true); else if (typ.equals("a,va,vc")) vypocty1.P026(h1, h2, h3, false); else if (typ.equals("a,va,p")) {
            chyba1("   p = a*va/2");
        } else if (typ.equals("a,va,r")) vypocty1.P027(h1, h2, h3); else if (typ.equals("a,va,ro")) vypocty1.P028(h1, h2, h3); else if (typ.equals("a,vb,vc")) vypocty1.P029(h1, h2, h3); else if (typ.equals("a,vb,p")) vypocty1.P030(h1, h2, h3, true); else if (typ.equals("a,vb,r")) vypocty1.P031(h1, h2, h3, true, "vb"); else if (typ.equals("a,vb,ro")) vypocty1.P032(h1, h2, h3, true); else if (typ.equals("a,vc,p")) vypocty1.P030(h1, h2, h3, false); else if (typ.equals("a,vc,r")) vypocty1.P031(h1, h2, h3, false, "vc"); else if (typ.equals("a,vc,ro")) vypocty1.P032(h1, h2, h3, false); else if (typ.equals("a,p,r")) vypocty1.P033(h1, h2, h3); else if (typ.equals("a,p,ro")) vypocty1.P034(h1, h2, h3); else if (typ.equals("a,r,ro")) vypocty1.P035(h1, h2, h3); else if (typ.equals("alfa,beta,gama")) {
            chyba1("   gama = 180-alfa-beta");
        } else if (typ.equals("alfa,beta,va")) vypocty1.P036(Math.toRadians(h1), Math.toRadians(h2), h3, "va"); else if (typ.equals("alfa,beta,vb")) vypocty1.P036(Math.toRadians(h1), Math.toRadians(h2), h3, "vb"); else if (typ.equals("alfa,beta,vc")) vypocty1.P036(Math.toRadians(h1), Math.toRadians(h2), h3, "vc"); else if (typ.equals("alfa,beta,p")) vypocty1.P037(Math.toRadians(h1), Math.toRadians(h2), h3); else if (typ.equals("alfa,beta,r")) vypocty1.P038(Math.toRadians(h1), Math.toRadians(h2), h3); else if (typ.equals("alfa,beta,ro")) vypocty1.P039(Math.toRadians(h1), Math.toRadians(h2), h3); else if (typ.equals("alfa,va,vb")) vypocty1.P040(Math.toRadians(h1), h2, h3, true); else if (typ.equals("alfa,va,vc")) vypocty1.P040(Math.toRadians(h1), h2, h3, false); else if (typ.equals("alfa,va,p")) vypocty1.P041(Math.toRadians(h1), h2, h3); else if (typ.equals("alfa,va,r")) vypocty1.P042(Math.toRadians(h1), h2, h3); else if (typ.equals("alfa,va,ro")) vypocty1.P043(Math.toRadians(h1), h2, h3); else if (typ.equals("alfa,vb,vc")) vypocty1.P044(Math.toRadians(h1), h2, h3); else if (typ.equals("alfa,vb,p")) vypocty1.P045(Math.toRadians(h1), h2, h3, true); else if (typ.equals("alfa,vb,r")) vypocty1.P046(Math.toRadians(h1), h2, h3, true, "vb"); else if (typ.equals("alfa,vb,ro")) vypocty1.P047(Math.toRadians(h1), h2, h3, true); else if (typ.equals("alfa,vc,p")) vypocty1.P045(Math.toRadians(h1), h2, h3, false); else if (typ.equals("alfa,vc,r")) vypocty1.P046(Math.toRadians(h1), h2, h3, false, "vc"); else if (typ.equals("alfa,vc,ro")) vypocty1.P047(Math.toRadians(h1), h2, h3, false); else if (typ.equals("alfa,p,r")) vypocty1.P048(Math.toRadians(h1), h2, h3); else if (typ.equals("alfa,p,ro")) vypocty1.P049(Math.toRadians(h1), h2, h3); else if (typ.equals("alfa,r,ro")) vypocty1.P050(Math.toRadians(h1), h2, h3); else if (typ.equals("va,vb,vc")) vypocty1.P051(h1, h2, h3); else if (typ.equals("va,vb,p")) vypocty1.P052(h1, h2, h3); else if (typ.equals("va,vb,r")) vypocty1.P053(h1, h2, h3); else if (typ.equals("va,vb,ro")) vypocty1.P054(h1, h2, h3); else if (typ.equals("va,p,r")) vypocty1.P055(h1, h2, h3); else if (typ.equals("va,p,ro")) vypocty1.P056(h1, h2, h3); else if (typ.equals("va,r,ro")) vypocty1.P057(h1, h2, h3); else if (typ.equals("p,r,ro")) vypocty1.P058(h1, h2, h3); else if (typ.equals("a,b,ta")) vypocty2.P060(h1, h2, h3, true, "ta"); else if (typ.equals("a,b,tb")) vypocty2.P060(h2, h1, h3, false, "tb"); else if (typ.equals("a,b,tc")) vypocty2.P061(h1, h2, h3); else if (typ.equals("a,alfa,ta")) vypocty2.P062(h1, Math.toRadians(h2), h3); else if (typ.equals("a,alfa,tb")) vypocty2.P063(h1, Math.toRadians(h2), h3, true, "tb"); else if (typ.equals("a,alfa,tc")) vypocty2.P063(h1, Math.toRadians(h2), h3, false, "tc"); else if (typ.equals("a,beta,ta")) vypocty2.P064(h1, Math.toRadians(h2), h3, true); else if (typ.equals("a,beta,tb")) vypocty2.P065(h1, Math.toRadians(h2), h3, true, "tb"); else if (typ.equals("a,beta,tc")) vypocty2.P066(h1, Math.toRadians(h2), h3, true, "tc"); else if (typ.equals("a,gama,ta")) vypocty2.P064(h1, Math.toRadians(h2), h3, false); else if (typ.equals("a,gama,tb")) vypocty2.P066(h1, Math.toRadians(h2), h3, false, "tb"); else if (typ.equals("a,gama,tc")) vypocty2.P065(h1, Math.toRadians(h2), h3, false, "tc"); else if (typ.equals("a,va,ta")) vypocty2.P067(h1, h2, h3); else if (typ.equals("a,va,tb")) vypocty2.P068(h1, h2, h3, true); else if (typ.equals("a,va,tc")) vypocty2.P068(h1, h2, h3, false); else if (typ.equals("a,vb,ta")) vypocty2.P069(h1, h2, h3, true); else if (typ.equals("a,vb,tb")) vypocty2.P070(h1, h2, h3, true, "vb"); else if (typ.equals("a,vb,tc")) vypocty2.P071(h1, h2, h3, true, "vb"); else if (typ.equals("a,vc,ta")) vypocty2.P069(h1, h2, h3, false); else if (typ.equals("a,vc,tb")) vypocty2.P071(h1, h2, h3, false, "vc"); else if (typ.equals("a,vc,tc")) vypocty2.P070(h1, h2, h3, false, "vc"); else if (typ.equals("a,ta,tb")) vypocty2.P078(h1, h2, h3, true, "tb"); else if (typ.equals("a,ta,tc")) vypocty2.P078(h1, h2, h3, false, "tc"); else if (typ.equals("a,ta,p")) vypocty2.P072(h1, h2, h3); else if (typ.equals("a,ta,r")) vypocty2.P074(h1, h2, h3); else if (typ.equals("a,ta,ro")) vypocty2.P076(h1, h2, h3); else if (typ.equals("a,tb,tc")) vypocty2.P079(h1, h2, h3); else if (typ.equals("a,tb,p")) vypocty2.P073(h1, h2, h3, true); else if (typ.equals("a,tb,r")) vypocty2.P075(h1, h2, h3, true, "tb"); else if (typ.equals("a,tb,ro")) vypocty2.P077(h1, h2, h3, true); else if (typ.equals("a,tc,p")) vypocty2.P073(h1, h2, h3, false); else if (typ.equals("a,tc,r")) vypocty2.P075(h1, h2, h3, false, "tc"); else if (typ.equals("a,tc,ro")) vypocty2.P077(h1, h2, h3, false); else if (typ.equals("alfa,beta,ta")) vypocty2.P080(Math.toRadians(h1), Math.toRadians(h2), h3, "ta"); else if (typ.equals("alfa,beta,tb")) vypocty2.P080(Math.toRadians(h1), Math.toRadians(h2), h3, "tb"); else if (typ.equals("alfa,beta,tc")) vypocty2.P080(Math.toRadians(h1), Math.toRadians(h2), h3, "tc"); else if (typ.equals("alfa,va,ta")) vypocty2.P082(Math.toRadians(h1), h2, h3); else if (typ.equals("alfa,va,tb")) vypocty2.P083(Math.toRadians(h1), h2, h3, true); else if (typ.equals("alfa,va,tc")) vypocty2.P083(Math.toRadians(h1), h2, h3, false); else if (typ.equals("alfa,vb,ta")) vypocty2.P084(Math.toRadians(h1), h2, h3, true); else if (typ.equals("alfa,vb,tb")) vypocty2.P085(Math.toRadians(h1), h2, h3, true, "tb"); else if (typ.equals("alfa,vb,tc")) vypocty2.P086(Math.toRadians(h1), h2, h3, true, "tc"); else if (typ.equals("alfa,vc,ta")) vypocty2.P084(Math.toRadians(h1), h2, h3, false); else if (typ.equals("alfa,vc,tb")) vypocty2.P086(Math.toRadians(h1), h2, h3, false, "tb"); else if (typ.equals("alfa,vc,tc")) vypocty2.P085(Math.toRadians(h1), h2, h3, false, "tc"); else if (typ.equals("alfa,ta,tb")) vypocty2.P093(Math.toRadians(h1), h2, h3, true, "tb"); else if (typ.equals("alfa,ta,tc")) vypocty2.P093(Math.toRadians(h1), h2, h3, false, "tc"); else if (typ.equals("alfa,ta,p")) vypocty2.P087(Math.toRadians(h1), h2, h3); else if (typ.equals("alfa,ta,r")) vypocty2.P089(Math.toRadians(h1), h2, h3); else if (typ.equals("alfa,ta,ro")) vypocty2.P091(Math.toRadians(h1), h2, h3); else if (typ.equals("alfa,tb,tc")) vypocty2.P094(Math.toRadians(h1), h2, h3); else if (typ.equals("alfa,tb,p")) vypocty2.P088(Math.toRadians(h1), h2, h3, true); else if (typ.equals("alfa,tb,r")) vypocty2.P090(Math.toRadians(h1), h2, h3, true, "tb"); else if (typ.equals("alfa,tb,ro")) vypocty2.P092(Math.toRadians(h1), h2, h3, true, "tb"); else if (typ.equals("alfa,tc,p")) vypocty2.P088(Math.toRadians(h1), h2, h3, false); else if (typ.equals("alfa,tc,r")) vypocty2.P090(Math.toRadians(h1), h2, h3, false, "tc"); else if (typ.equals("alfa,tc,ro")) vypocty2.P092(Math.toRadians(h1), h2, h3, false, "tc"); else if (typ.equals("va,vb,ta")) vypocty2.P095(h1, h2, h3); else if (typ.equals("va,vb,tb")) vypocty2.P096(h1, h2, h3); else if (typ.equals("va,vb,tc")) vypocty2.P097(h1, h2, h3); else if (typ.equals("va,ta,tb")) vypocty2.P104(h1, h2, h3, true); else if (typ.equals("va,ta,tc")) vypocty2.P104(h1, h2, h3, false); else if (typ.equals("va,ta,p")) vypocty2.P098(h1, h2, h3); else if (typ.equals("va,ta,r")) vypocty2.P100(h1, h2, h3); else if (typ.equals("va,ta,ro")) vypocty2.P102(h1, h2, h3); else if (typ.equals("va,tb,tc")) vypocty2.P105(h1, h2, h3); else if (typ.equals("va,tb,p")) vypocty2.P099(h1, h2, h3, true); else if (typ.equals("va,tb,r")) vypocty2.P101(h1, h2, h3, true, "tb"); else if (typ.equals("va,tb,ro")) vypocty2.P103(h1, h2, h3, true, "tb"); else if (typ.equals("va,tc,p")) vypocty2.P099(h1, h2, h3, false); else if (typ.equals("va,tc,r")) vypocty2.P101(h1, h2, h3, false, "tc"); else if (typ.equals("va,tc,ro")) vypocty2.P103(h1, h2, h3, false, "tc"); else if (typ.equals("ta,tb,tc")) vypocty2.P112(h1, h2, h3); else if (typ.equals("ta,tb,p")) vypocty2.P108(h1, h2, h3); else if (typ.equals("ta,tb,r")) vypocty2.P110(h1, h2, h3); else if (typ.equals("ta,tb,ro")) vypocty2.P111(h1, h2, h3); else if (typ.equals("ta,p,r")) vypocty2.P106(h1, h2, h3); else if (typ.equals("ta,p,ro")) vypocty2.P107(h1, h2, h3); else if (typ.equals("ta,r,ro")) vypocty2.P109(h1, h2, h3); else if (typ.equals("a,b,ua")) vypocty3.P120(h1, h2, h3, true, "ua"); else if (typ.equals("a,b,ub")) vypocty3.P120(h2, h1, h3, false, "ub"); else if (typ.equals("a,b,uc")) vypocty3.P121(h1, h2, h3); else if (typ.equals("a,alfa,ua")) vypocty3.P122(h1, Math.toRadians(h2), h3); else if (typ.equals("a,alfa,ub")) vypocty3.P123(h1, Math.toRadians(h2), h3, true, "ub"); else if (typ.equals("a,alfa,uc")) vypocty3.P123(h1, Math.toRadians(h2), h3, false, "uc"); else if (typ.equals("a,beta,ua")) vypocty3.P124(h1, Math.toRadians(h2), h3, true); else if (typ.equals("a,beta,ub")) vypocty3.P125(h1, Math.toRadians(h2), h3, true, "ub"); else if (typ.equals("a,beta,uc")) vypocty3.P126(h1, Math.toRadians(h2), h3, true, "uc"); else if (typ.equals("a,gama,ua")) vypocty3.P124(h1, Math.toRadians(h2), h3, false); else if (typ.equals("a,gama,ub")) vypocty3.P126(h1, Math.toRadians(h2), h3, false, "ub"); else if (typ.equals("a,gama,uc")) vypocty3.P125(h1, Math.toRadians(h2), h3, false, "uc"); else if (typ.equals("a,va,ua")) vypocty3.P127(h1, h2, h3); else if (typ.equals("a,va,ub")) vypocty3.P128(h1, h2, h3, true); else if (typ.equals("a,va,uc")) vypocty3.P128(h1, h2, h3, false); else if (typ.equals("a,vb,ua")) vypocty3.P129(h1, h2, h3, true); else if (typ.equals("a,vb,ub")) vypocty3.P130(h1, h2, h3, true, "vb", "ub"); else if (typ.equals("a,vb,uc")) vypocty3.P131(h1, h2, h3, true, "uc"); else if (typ.equals("a,vc,ua")) vypocty3.P129(h1, h2, h3, false); else if (typ.equals("a,vc,ub")) vypocty3.P131(h1, h2, h3, false, "ub"); else if (typ.equals("a,vc,uc")) vypocty3.P130(h1, h2, h3, false, "vc", "uc"); else if (typ.equals("a,ta,ua")) vypocty3.P132(h1, h2, h3); else if (typ.equals("a,ta,ub")) vypocty3.P133(h1, h2, h3, true, "ub"); else if (typ.equals("a,ta,uc")) vypocty3.P133(h1, h2, h3, false, "uc"); else if (typ.equals("a,tb,ua")) vypocty3.P134(h1, h2, h3, true); else if (typ.equals("a,tb,ub")) vypocty3.P135(h1, h2, h3, true, "ub"); else if (typ.equals("a,tb,uc")) vypocty3.P136(h1, h2, h3, true, "uc"); else if (typ.equals("a,tc,ua")) vypocty3.P134(h1, h2, h3, false); else if (typ.equals("a,tc,ub")) vypocty3.P136(h1, h2, h3, false, "ub"); else if (typ.equals("a,tc,uc")) vypocty3.P135(h1, h2, h3, false, "uc"); else if (typ.equals("a,ua,ub")) vypocty3.P137(h1, h2, h3, true); else if (typ.equals("a,ua,uc")) vypocty3.P137(h1, h2, h3, false); else if (typ.equals("a,ua,p")) vypocty3.P139(h1, h2, h3); else if (typ.equals("a,ua,r")) vypocty3.P141(h1, h2, h3); else if (typ.equals("a,ua,ro")) vypocty3.P143(h1, h2, h3); else if (typ.equals("a,ub,uc")) vypocty3.P138(h1, h2, h3); else if (typ.equals("a,ub,p")) vypocty3.P140(h1, h2, h3, true); else if (typ.equals("a,ub,r")) vypocty3.P142(h1, h2, h3, true, "ub"); else if (typ.equals("a,ub,ro")) vypocty3.P144(h1, h2, h3, true, "ub"); else if (typ.equals("a,uc,p")) vypocty3.P140(h1, h2, h3, false); else if (typ.equals("a,uc,r")) vypocty3.P142(h1, h2, h3, false, "uc"); else if (typ.equals("a,uc,ro")) vypocty3.P144(h1, h2, h3, false, "uc"); else if (typ.equals("alfa,beta,ua")) vypocty3.P145(Math.toRadians(h1), Math.toRadians(h2), h3, "ua"); else if (typ.equals("alfa,beta,ub")) vypocty3.P145(Math.toRadians(h1), Math.toRadians(h2), h3, "ub"); else if (typ.equals("alfa,beta,uc")) vypocty3.P145(Math.toRadians(h1), Math.toRadians(h2), h3, "uc"); else if (typ.equals("alfa,va,ua")) vypocty3.P146(Math.toRadians(h1), h2, h3); else if (typ.equals("alfa,va,ub")) vypocty3.P147(Math.toRadians(h1), h2, h3, true, "ub"); else if (typ.equals("alfa,va,uc")) vypocty3.P147(Math.toRadians(h1), h2, h3, false, "uc"); else if (typ.equals("alfa,vb,ua")) vypocty3.P148(Math.toRadians(h1), h2, h3, true); else if (typ.equals("alfa,vb,ub")) vypocty3.P149(Math.toRadians(h1), h2, h3, true, "ub"); else if (typ.equals("alfa,vb,uc")) vypocty3.P150(Math.toRadians(h1), h2, h3, true); else if (typ.equals("alfa,vc,ua")) vypocty3.P148(Math.toRadians(h1), h2, h3, false); else if (typ.equals("alfa,vc,ub")) vypocty3.P150(Math.toRadians(h1), h2, h3, false); else if (typ.equals("alfa,vc,uc")) vypocty3.P149(Math.toRadians(h1), h2, h3, false, "uc"); else if (typ.equals("alfa,ta,ua")) vypocty3.P151(Math.toRadians(h1), h2, h3); else if (typ.equals("alfa,ta,ub")) vypocty3.P152(Math.toRadians(h1), h2, h3, true); else if (typ.equals("alfa,ta,uc")) vypocty3.P152(Math.toRadians(h1), h2, h3, false); else if (typ.equals("alfa,tb,ua")) vypocty3.P153(Math.toRadians(h1), h2, h3, true, "tb"); else if (typ.equals("alfa,tb,ub")) vypocty3.P154(Math.toRadians(h1), h2, h3, true, "tb"); else if (typ.equals("alfa,tb,uc")) vypocty3.P155(Math.toRadians(h1), h2, h3, true, "tb"); else if (typ.equals("alfa,tc,ua")) vypocty3.P153(Math.toRadians(h1), h2, h3, false, "tc"); else if (typ.equals("alfa,tc,ub")) vypocty3.P155(Math.toRadians(h1), h2, h3, false, "tc"); else if (typ.equals("alfa,tc,uc")) vypocty3.P154(Math.toRadians(h1), h2, h3, false, "tc"); else if (typ.equals("alfa,ua,ub")) vypocty3.P156(Math.toRadians(h1), h2, h3, true, "ub"); else if (typ.equals("alfa,ua,uc")) vypocty3.P156(Math.toRadians(h1), h2, h3, false, "uc"); else if (typ.equals("alfa,ua,p")) vypocty3.P158(Math.toRadians(h1), h2, h3); else if (typ.equals("alfa,ua,r")) vypocty3.P160(Math.toRadians(h1), h2, h3); else if (typ.equals("alfa,ua,ro")) vypocty3.P162(Math.toRadians(h1), h2, h3); else if (typ.equals("alfa,ub,uc")) vypocty3.P157(Math.toRadians(h1), h2, h3); else if (typ.equals("alfa,ub,p")) vypocty3.P159(Math.toRadians(h1), h2, h3, true); else if (typ.equals("alfa,ub,r")) vypocty3.P161(Math.toRadians(h1), h2, h3, true, "ub"); else if (typ.equals("alfa,ub,ro")) vypocty3.P163(Math.toRadians(h1), h2, h3, true); else if (typ.equals("alfa,uc,p")) vypocty3.P159(Math.toRadians(h1), h2, h3, false); else if (typ.equals("alfa,uc,r")) vypocty3.P161(Math.toRadians(h1), h2, h3, false, "uc"); else if (typ.equals("alfa,uc,ro")) vypocty3.P163(Math.toRadians(h1), h2, h3, false); else if (typ.equals("va,vb,ua")) vypocty3.P164(h1, h2, h3, true); else if (typ.equals("va,vb,ub")) vypocty3.P164(h2, h1, h3, false); else if (typ.equals("va,vb,uc")) vypocty3.P166(h1, h2, h3); else if (typ.equals("va,ta,ua")) vypocty3.P167(h1, h2, h3); else if (typ.equals("va,ta,ub")) vypocty3.P168(h1, h2, h3, true); else if (typ.equals("va,ta,uc")) vypocty3.P168(h1, h2, h3, false); else if (typ.equals("va,tb,ua")) vypocty3.P169(h1, h2, h3, true); else if (typ.equals("va,tb,ub")) vypocty3.P170(h1, h2, h3, true, "tb", "ub"); else if (typ.equals("va,tb,uc")) vypocty3.P171(h1, h2, h3, true); else if (typ.equals("va,tc,ua")) vypocty3.P169(h1, h2, h3, false); else if (typ.equals("va,tc,ub")) vypocty3.P171(h1, h2, h3, false); else if (typ.equals("va,tc,uc")) vypocty3.P170(h1, h2, h3, false, "tc", "uc"); else if (typ.equals("va,ua,ub")) vypocty3.P172(h1, h2, h3, true); else if (typ.equals("va,ua,uc")) vypocty3.P172(h1, h2, h3, false); else if (typ.equals("va,ua,p")) vypocty3.P174(h1, h2, h3); else if (typ.equals("va,ua,r")) vypocty3.P176(h1, h2, h3); else if (typ.equals("va,ua,ro")) vypocty3.P178(h1, h2, h3); else if (typ.equals("va,ub,uc")) vypocty3.P173(h1, h2, h3); else if (typ.equals("va,ub,p")) vypocty3.P175(h1, h2, h3, true, "ub"); else if (typ.equals("va,ub,r")) vypocty3.P177(h1, h2, h3, true, "ub"); else if (typ.equals("va,ub,ro")) vypocty3.P179(h1, h2, h3, true); else if (typ.equals("va,uc,p")) vypocty3.P175(h1, h2, h3, false, "uc"); else if (typ.equals("va,uc,r")) vypocty3.P177(h1, h2, h3, false, "uc"); else if (typ.equals("va,uc,ro")) vypocty3.P179(h1, h2, h3, false); else if (typ.equals("ta,tb,ua")) vypocty3.P180(h1, h2, h3); else if (typ.equals("ta,tb,ub")) vypocty3.P181(h1, h2, h3); else if (typ.equals("ta,tb,uc")) vypocty3.P182(h1, h2, h3); else if (typ.equals("ta,ua,ub")) vypocty3.P183(h1, h2, h3, true); else if (typ.equals("ta,ua,uc")) vypocty3.P183(h1, h2, h3, false); else if (typ.equals("ta,ua,p")) vypocty3.P185(h1, h2, h3); else if (typ.equals("ta,ua,r")) vypocty3.P187(h1, h2, h3); else if (typ.equals("ta,ua,ro")) vypocty3.P189(h1, h2, h3); else if (typ.equals("ta,ub,uc")) vypocty3.P184(h1, h2, h3); else if (typ.equals("ta,ub,p")) vypocty3.P186(h1, h2, h3, true, "ub"); else if (typ.equals("ta,ub,r")) vypocty3.P188(h1, h2, h3, true, "ub"); else if (typ.equals("ta,ub,ro")) vypocty3.P190(h1, h2, h3, true); else if (typ.equals("ta,uc,p")) vypocty3.P186(h1, h2, h3, false, "uc"); else if (typ.equals("ta,uc,r")) vypocty3.P188(h1, h2, h3, false, "uc"); else if (typ.equals("ta,uc,ro")) vypocty3.P190(h1, h2, h3, false); else if (typ.equals("ua,ub,uc")) vypocty3.P194(h1, h2, h3); else if (typ.equals("ua,ub,p")) vypocty3.P191(h1, h2, h3); else if (typ.equals("ua,ub,r")) vypocty3.P192(h1, h2, h3); else if (typ.equals("ua,ub,ro")) vypocty3.P193(h1, h2, h3); else if (typ.equals("ua,p,r")) vypocty3.P195(h1, h2, h3); else if (typ.equals("ua,p,ro")) vypocty3.P196(h1, h2, h3); else if (typ.equals("ua,r,ro")) vypocty3.P197(h1, h2, h3); else if (typ.equals("a,b,ab")) {
            chyba1("   ab = a + b");
        } else if (typ.equals("a,c,ab")) {
            if (h3 - h1 <= 0) {
                chyba("ab", "<=", h1);
                return reseni;
            }
            vypocty1.P001(h1, h3 - h1, h2);
        } else if (typ.equals("a,alfa,ab")) {
            if (h3 - h1 <= 0) {
                chyba("ab", "<=", h1);
                return reseni;
            }
            vypocty1.P002(h1, h3 - h1, Math.toRadians(h2));
        } else if (typ.equals("a,beta,ab")) {
            if (h3 - h1 <= 0) {
                chyba("ab", "<=", h1);
                return reseni;
            }
            vypocty1.P003(h1, h3 - h1, Math.toRadians(h2));
        } else if (typ.equals("a,gama,ab")) {
            if (h3 - h1 <= 0) {
                chyba("ab", "<=", h1);
                return reseni;
            }
            vypocty1.P004(h1, h3 - h1, Math.toRadians(h2));
        } else if (typ.equals("a,va,ab")) {
            if (h3 - h1 <= 0) {
                chyba("ab", "<=", h1);
                return reseni;
            }
            vypocty1.P005(h1, h3 - h1, h2);
        } else if (typ.equals("a,vb,ab")) {
            if (h3 - h1 <= 0) {
                chyba("ab", "<=", h1);
                return reseni;
            }
            vypocty1.P006(h3 - h1, h1, h2);
        } else if (typ.equals("a,vc,ab")) {
            if (h3 - h1 <= 0) {
                chyba("ab", "<=", h1);
                return reseni;
            }
            vypocty1.P007(h1, h3 - h1, h2);
        } else if (typ.equals("a,p,ab")) {
            if (h3 - h1 <= 0) {
                chyba("ab", "<=", h1);
                return reseni;
            }
            vypocty1.P008(h1, h3 - h1, h2);
        } else if (typ.equals("a,r,ab")) {
            if (h3 - h1 <= 0) {
                chyba("a", "<=", h1);
                return reseni;
            }
            vypocty1.P009(h1, h3 - h1, h2);
        } else if (typ.equals("a,ro,ab")) {
            if (h3 - h1 <= 0) {
                chyba("ab", "<=", h1);
                return reseni;
            }
            vypocty1.P010(h1, h3 - h1, h2);
        } else if (typ.equals("a,ta,ab")) {
            if (h3 - h1 <= 0) {
                chyba("ab", "<=", h1);
                return reseni;
            }
            vypocty2.P060(h1, h3 - h1, h2, true, "ta");
        } else if (typ.equals("a,tb,ab")) {
            if (h3 - h1 <= 0) {
                chyba("ab", "<=", h1);
                return reseni;
            }
            vypocty2.P060(h3 - h1, h1, h2, false, "tb");
        } else if (typ.equals("a,tc,ab")) {
            if (h3 - h1 <= 0) {
                chyba("ab", "<=", h1);
                return reseni;
            }
            vypocty2.P061(h1, h3 - h1, h2);
        } else if (typ.equals("a,ua,ab")) {
            if (h3 - h1 <= 0) {
                chyba("ab", "<=", h1);
                return reseni;
            }
            vypocty3.P120(h1, h3 - h1, h2, true, "ua");
        } else if (typ.equals("a,ub,ab")) {
            if (h3 - h1 <= 0) {
                chyba("ab", "<=", h1);
                return reseni;
            }
            vypocty3.P120(h3 - h1, h1, h2, false, "ub");
        } else if (typ.equals("a,uc,ab")) {
            if (h3 - h1 <= 0) {
                chyba("ab", "<=", h1);
                return reseni;
            }
            vypocty3.P121(h1, h3 - h1, h2);
        } else if (typ.equals("b,c,ab")) {
            if (h3 - h1 <= 0) {
                chyba("ab", "<=", h1);
                return reseni;
            }
            vypocty1.P001(h3 - h1, h1, h2);
        } else if (typ.equals("b,alfa,ab")) {
            if (h3 - h1 <= 0) {
                chyba("ab", "<=", h1);
                return reseni;
            }
            vypocty1.P002(h3 - h1, h1, Math.toRadians(h2));
        } else if (typ.equals("b,beta,ab")) {
            if (h3 - h1 <= 0) {
                chyba("ab", "<=", h1);
                return reseni;
            }
            vypocty1.P003(h3 - h1, h1, Math.toRadians(h2));
        } else if (typ.equals("b,gama,ab")) {
            if (h3 - h1 <= 0) {
                chyba("ab", "<=", h1);
                return reseni;
            }
            vypocty1.P004(h3 - h1, h1, Math.toRadians(h2));
        } else if (typ.equals("b,va,ab")) {
            if (h3 <= h1) {
                chyba("ab", "<=", h1);
                return reseni;
            }
            vypocty1.P005(h3 - h1, h1, h2);
        } else if (typ.equals("b,vb,ab")) {
            if (h3 - h1 <= 0) {
                chyba("ab", "<=", h1);
                return reseni;
            }
            vypocty1.P006(h1, h3 - h1, h2);
        } else if (typ.equals("b,vc,ab")) {
            if (h3 - h1 <= 0) {
                chyba("ab", "<=", h1);
                return reseni;
            }
            vypocty1.P007(h3 - h1, h1, h2);
        } else if (typ.equals("b,p,ab")) {
            if (h3 - h1 <= 0) {
                chyba("ab", "<=", h1);
                return reseni;
            }
            vypocty1.P008(h3 - h1, h1, h2);
        } else if (typ.equals("b,r,ab")) {
            if (h3 - h1 <= 0) {
                chyba("ab", "<=", h1);
                return reseni;
            }
            vypocty1.P009(h3 - h1, h1, h2);
        } else if (typ.equals("b,ro,ab")) {
            if (h3 - h1 <= 0) {
                chyba("ab", "<=", h1);
                return reseni;
            }
            vypocty1.P010(h3 - h1, h1, h2);
        } else if (typ.equals("b,ta,ab")) {
            if (h3 - h1 <= 0) {
                chyba("ab", "<=", h1);
                return reseni;
            }
            vypocty2.P060(h3 - h1, h1, h2, true, "ta");
        } else if (typ.equals("b,tb,ab")) {
            if (h3 - h1 <= 0) {
                chyba("ab", "<=", h1);
                return reseni;
            }
            vypocty2.P060(h1, h3 - h1, h2, false, "tb");
        } else if (typ.equals("b,tc,ab")) {
            if (h3 - h1 <= 0) {
                chyba("ab", "<=", h1);
                return reseni;
            }
            vypocty2.P061(h3 - h1, h1, h2);
        } else if (typ.equals("b,ua,ab")) {
            if (h3 - h1 <= 0) {
                chyba("ab", "<=", h1);
                return reseni;
            }
            vypocty3.P120(h3 - h1, h1, h2, true, "ua");
        } else if (typ.equals("b,ub,ab")) {
            if (h3 - h1 <= 0) {
                chyba("ab", "<=", h1);
                return reseni;
            }
            vypocty3.P120(h1, h3 - h1, h2, false, "ub");
        } else if (typ.equals("b,uc,ab")) {
            if (h3 - h1 <= 0) {
                chyba("ab", "<=", h1);
                return reseni;
            }
            vypocty3.P121(h3 - h1, h1, h2);
        } else if (typ.equals("c,alfa,ab")) vypocty4.P200(h1, Math.toRadians(h2), h3, true); else if (typ.equals("c,beta,ab")) vypocty4.P200(h1, Math.toRadians(h2), h3, false); else if (typ.equals("c,gama,ab")) vypocty4.P201(h1, Math.toRadians(h2), h3); else if (typ.equals("c,va,ab")) vypocty4.P202(h1, h2, h3, true); else if (typ.equals("c,vb,ab")) vypocty4.P202(h1, h2, h3, false); else if (typ.equals("c,vc,ab")) vypocty4.P203(h1, h2, h3); else if (typ.equals("c,ta,ab")) vypocty4.P204(h1, h2, h3, true); else if (typ.equals("c,tb,ab")) vypocty4.P204(h1, h2, h3, false); else if (typ.equals("c,tc,ab")) vypocty4.P205(h1, h2, h3); else if (typ.equals("c,p,ab")) vypocty4.P206(h1, h2, h3); else if (typ.equals("c,r,ab")) vypocty4.P207(h1, h2, h3); else if (typ.equals("c,ro,ab")) vypocty4.P208(h1, h2, h3); else if (typ.equals("alfa,beta,ab")) vypocty4.P209(Math.toRadians(h1), Math.toRadians(h2), h3); else if (typ.equals("alfa,gama,ab")) vypocty4.P210(Math.toRadians(h1), Math.toRadians(h2), h3, true, "alfa"); else if (typ.equals("alfa,va,ab")) vypocty4.P211(Math.toRadians(h1), h2, h3, true); else if (typ.equals("alfa,vb,ab")) vypocty4.P212(Math.toRadians(h1), h2, h3, true); else if (typ.equals("alfa,vc,ab")) vypocty4.P213(Math.toRadians(h1), h2, h3, true); else if (typ.equals("alfa,ta,ab")) vypocty4.P214(Math.toRadians(h1), h2, h3, true); else if (typ.equals("alfa,tb,ab")) vypocty4.P215(Math.toRadians(h1), h2, h3, true); else if (typ.equals("alfa,tc,ab")) vypocty4.P216(Math.toRadians(h1), h2, h3, true); else if (typ.equals("alfa,p,ab")) vypocty4.P217(Math.toRadians(h1), h2, h3, true); else if (typ.equals("alfa,r,ab")) vypocty4.P218(Math.toRadians(h1), h2, h3, true); else if (typ.equals("alfa,ro,ab")) vypocty4.P219(Math.toRadians(h1), h2, h3, true); else if (typ.equals("beta,gama,ab")) vypocty4.P210(Math.toRadians(h1), Math.toRadians(h2), h3, false, "beta"); else if (typ.equals("beta,va,ab")) vypocty4.P212(Math.toRadians(h1), h2, h3, false); else if (typ.equals("beta,vb,ab")) vypocty4.P211(Math.toRadians(h1), h2, h3, false); else if (typ.equals("beta,vc,ab")) vypocty4.P213(Math.toRadians(h1), h2, h3, false); else if (typ.equals("beta,ta,ab")) vypocty4.P215(Math.toRadians(h1), h2, h3, false); else if (typ.equals("beta,tb,ab")) vypocty4.P214(Math.toRadians(h1), h2, h3, false); else if (typ.equals("beta,tc,ab")) vypocty4.P216(Math.toRadians(h1), h2, h3, false); else if (typ.equals("beta,p,ab")) vypocty4.P217(Math.toRadians(h1), h2, h3, false); else if (typ.equals("beta,r,ab")) vypocty4.P218(Math.toRadians(h1), h2, h3, false); else if (typ.equals("beta,ro,ab")) vypocty4.P219(Math.toRadians(h1), h2, h3, false); else if (typ.equals("gama,va,ab")) vypocty4.P220(Math.toRadians(h1), h2, h3, true); else if (typ.equals("gama,vb,ab")) vypocty4.P220(Math.toRadians(h1), h2, h3, false); else if (typ.equals("gama,vc,ab")) vypocty4.P221(Math.toRadians(h1), h2, h3); else if (typ.equals("gama,ta,ab")) vypocty4.P227(Math.toRadians(h1), h2, h3, true); else if (typ.equals("gama,tb,ab")) vypocty4.P227(Math.toRadians(h1), h2, h3, false); else if (typ.equals("gama,tc,ab")) vypocty4.P228(Math.toRadians(h1), h2, h3); else if (typ.equals("gama,p,ab")) vypocty4.P229(Math.toRadians(h1), h2, h3); else if (typ.equals("gama,r,ab")) vypocty4.P230(Math.toRadians(h1), h2, h3); else if (typ.equals("gama,ro,ab")) vypocty4.P231(Math.toRadians(h1), h2, h3); else if (typ.equals("va,vb,ab")) vypocty4.P222(h1, h2, h3); else if (typ.equals("va,vc,ab")) vypocty4.P223(h1, h2, h3, true); else if (typ.equals("va,ta,ab")) vypocty4.P232(h1, h2, h3, true, "va"); else if (typ.equals("va,tb,ab")) vypocty4.P233(h1, h2, h3, true, "va"); else if (typ.equals("va,tc,ab")) vypocty4.P234(h1, h2, h3, true); else if (typ.equals("va,p,ab")) vypocty4.P224(h1, h2, h3, true); else if (typ.equals("va,r,ab")) vypocty4.P225(h1, h2, h3, true); else if (typ.equals("va,ro,ab")) vypocty4.P226(h1, h2, h3, true); else if (typ.equals("vb,vc,ab")) vypocty4.P223(h1, h2, h3, false); else if (typ.equals("vb,ta,ab")) vypocty4.P233(h1, h2, h3, false, "vb"); else if (typ.equals("vb,tb,ab")) vypocty4.P232(h1, h2, h3, false, "vb"); else if (typ.equals("vb,tc,ab")) vypocty4.P234(h1, h2, h3, false); else if (typ.equals("vb,p,ab")) vypocty4.P224(h1, h2, h3, false); else if (typ.equals("vb,r,ab")) vypocty4.P225(h1, h2, h3, false); else if (typ.equals("vb,ro,ab")) vypocty4.P226(h1, h2, h3, false); else if (typ.equals("vc,ta,ab")) vypocty4.P235(h1, h2, h3, true); else if (typ.equals("vc,tb,ab")) vypocty4.P235(h1, h2, h3, false); else if (typ.equals("vc,tc,ab")) vypocty4.P236(h1, h2, h3); else if (typ.equals("vc,p,ab")) vypocty4.P237(h1, h2, h3); else if (typ.equals("vc,r,ab")) vypocty4.P238(h1, h2, h3); else if (typ.equals("vc,ro,ab")) vypocty4.P239(h1, h2, h3); else if (typ.equals("ta,tb,ab")) vypocty4.P240(h1, h2, h3); else if (typ.equals("ta,tc,ab")) vypocty4.P241(h1, h2, h3, true); else if (typ.equals("ta,p,ab")) vypocty4.P242(h1, h2, h3, true); else if (typ.equals("ta,r,ab")) vypocty4.P243(h1, h2, h3, true); else if (typ.equals("ta,ro,ab")) vypocty4.P244(h1, h2, h3, true); else if (typ.equals("tb,tc,ab")) vypocty4.P241(h1, h2, h3, false); else if (typ.equals("tb,p,ab")) vypocty4.P242(h1, h2, h3, false); else if (typ.equals("tb,r,ab")) vypocty4.P243(h1, h2, h3, false); else if (typ.equals("tb,ro,ab")) vypocty4.P244(h1, h2, h3, false); else if (typ.equals("tc,p,ab")) vypocty4.P245(h1, h2, h3); else if (typ.equals("tc,r,ab")) vypocty4.P246(h1, h2, h3); else if (typ.equals("tc,ro,ab")) vypocty4.P247(h1, h2, h3); else if (typ.equals("p,r,ab")) vypocty4.P248(h1, h2, h3); else if (typ.equals("p,ro,ab")) vypocty4.P249(h1, h2, h3); else if (typ.equals("r,ro,ab")) vypocty4.P250(h1, h2, h3); else if (typ.equals("c,ua,ab")) vypocty4.P255(h1, h2, h3, true); else if (typ.equals("c,ub,ab")) vypocty4.P255(h1, h2, h3, false); else if (typ.equals("c,uc,ab")) vypocty4.P256(h1, h2, h3); else if (typ.equals("alfa,ua,ab")) vypocty4.P257(Math.toRadians(h1), h2, h3, true); else if (typ.equals("alfa,ub,ab")) vypocty4.P258(Math.toRadians(h1), h2, h3, true); else if (typ.equals("alfa,uc,ab")) vypocty4.P259(Math.toRadians(h1), h2, h3, true); else if (typ.equals("beta,ua,ab")) vypocty4.P258(Math.toRadians(h1), h2, h3, false); else if (typ.equals("beta,ub,ab")) vypocty4.P257(Math.toRadians(h1), h2, h3, false); else if (typ.equals("beta,uc,ab")) vypocty4.P259(Math.toRadians(h1), h2, h3, false); else if (typ.equals("gama,ua,ab")) vypocty4.P262(Math.toRadians(h1), h2, h3, true); else if (typ.equals("gama,ub,ab")) vypocty4.P262(Math.toRadians(h1), h2, h3, false); else if (typ.equals("gama,uc,ab")) vypocty4.P264(Math.toRadians(h1), h2, h3); else if (typ.equals("va,ua,ab")) vypocty4.P265(h1, h2, h3, true, "va"); else if (typ.equals("va,ub,ab")) vypocty4.P266(h1, h2, h3, true); else if (typ.equals("va,uc,ab")) vypocty4.P267(h1, h2, h3, true); else if (typ.equals("vb,ua,ab")) vypocty4.P266(h1, h2, h3, false); else if (typ.equals("vb,ub,ab")) vypocty4.P265(h1, h2, h3, false, "vb"); else if (typ.equals("vb,uc,ab")) vypocty4.P267(h1, h2, h3, false); else if (typ.equals("vc,ua,ab")) vypocty4.P271(h1, h2, h3, true); else if (typ.equals("vc,ub,ab")) vypocty4.P271(h1, h2, h3, false); else if (typ.equals("vc,uc,ab")) vypocty4.P273(h1, h2, h3); else if (typ.equals("ta,ua,ab")) vypocty4.P274(h1, h2, h3, true, "ta"); else if (typ.equals("ta,ub,ab")) vypocty4.P275(h1, h2, h3, true); else if (typ.equals("ta,uc,ab")) vypocty4.P276(h1, h2, h3, true); else if (typ.equals("tb,ua,ab")) vypocty4.P275(h1, h2, h3, false); else if (typ.equals("tb,ub,ab")) vypocty4.P274(h1, h2, h3, false, "tb"); else if (typ.equals("tb,uc,ab")) vypocty4.P276(h1, h2, h3, false); else if (typ.equals("tc,ua,ab")) vypocty4.P280(h1, h2, h3, true); else if (typ.equals("tc,ub,ab")) vypocty4.P280(h1, h2, h3, false); else if (typ.equals("tc,uc,ab")) vypocty4.P281(h1, h2, h3); else if (typ.equals("ua,ub,ab")) vypocty4.P282(h1, h2, h3); else if (typ.equals("ua,uc,ab")) vypocty4.P283(h1, h2, h3, true); else if (typ.equals("ua,p,ab")) vypocty4.P284(h1, h2, h3, true); else if (typ.equals("ua,r,ab")) vypocty4.P285(h1, h2, h3, true); else if (typ.equals("ua,ro,ab")) vypocty4.P286(h1, h2, h3, true); else if (typ.equals("ub,uc,ab")) vypocty4.P283(h1, h2, h3, false); else if (typ.equals("ub,p,ab")) vypocty4.P284(h1, h2, h3, false); else if (typ.equals("ub,r,ab")) vypocty4.P285(h1, h2, h3, false); else if (typ.equals("ub,ro,ab")) vypocty4.P286(h1, h2, h3, false); else if (typ.equals("uc,p,ab")) vypocty4.P287(h1, h2, h3); else if (typ.equals("uc,r,ab")) vypocty4.P288(h1, h2, h3); else if (typ.equals("uc,ro,ab")) vypocty4.P289(h1, h2, h3); else if (typ.equals("a,b,ob")) vypocty1.P001(h1, h2, h3 - h1 - h2); else if (typ.equals("a,alfa,ob")) vypocty5.P300(h1, Math.toRadians(h2), h3); else if (typ.equals("a,beta,ob")) vypocty5.P301(h1, Math.toRadians(h2), h3, true); else if (typ.equals("a,gama,ob")) vypocty5.P301(h1, Math.toRadians(h2), h3, false); else if (typ.equals("a,va,ob")) vypocty5.P302(h1, h2, h3); else if (typ.equals("a,vb,ob")) vypocty5.P303(h1, h2, h3, true); else if (typ.equals("a,vc,ob")) vypocty5.P303(h1, h2, h3, false); else if (typ.equals("a,ta,ob")) vypocty5.P304(h1, h2, h3); else if (typ.equals("a,tb,ob")) vypocty5.P305(h1, h2, h3, true); else if (typ.equals("a,tc,ob")) vypocty5.P305(h1, h2, h3, false); else if (typ.equals("a,p,ob")) vypocty5.P306(h1, h2, h3); else if (typ.equals("a,r,ob")) vypocty5.P307(h1, h2, h3); else if (typ.equals("a,ro,ob")) vypocty5.P308(h1, h2, h3); else if (typ.equals("alfa,beta,ob")) vypocty5.P309(Math.toRadians(h1), Math.toRadians(h2), h3); else if (typ.equals("alfa,va,ob")) vypocty5.P310(Math.toRadians(h1), h2, h3); else if (typ.equals("alfa,vb,ob")) vypocty5.P311(Math.toRadians(h1), h2, h3, true); else if (typ.equals("alfa,vc,ob")) vypocty5.P311(Math.toRadians(h1), h2, h3, false); else if (typ.equals("alfa,ta,ob")) vypocty5.P312(Math.toRadians(h1), h2, h3); else if (typ.equals("alfa,tb,ob")) vypocty5.P313(Math.toRadians(h1), h2, h3, true); else if (typ.equals("alfa,tc,ob")) vypocty5.P313(Math.toRadians(h1), h2, h3, false); else if (typ.equals("alfa,p,ob")) vypocty5.P314(Math.toRadians(h1), h2, h3); else if (typ.equals("alfa,r,ob")) vypocty5.P315(Math.toRadians(h1), h2, h3); else if (typ.equals("alfa,ro,ob")) vypocty5.P316(Math.toRadians(h1), h2, h3); else if (typ.equals("va,vb,ob")) vypocty5.P317(h1, h2, h3); else if (typ.equals("va,ta,ob")) vypocty5.P318(h1, h2, h3); else if (typ.equals("va,tb,ob")) vypocty5.P319(h1, h2, h3, true); else if (typ.equals("va,tc,ob")) vypocty5.P319(h1, h2, h3, false); else if (typ.equals("va,p,ob")) vypocty5.P320(h1, h2, h3); else if (typ.equals("va,r,ob")) vypocty5.P321(h1, h2, h3); else if (typ.equals("va,ro,ob")) vypocty5.P322(h1, h2, h3); else if (typ.equals("ta,tb,ob")) vypocty5.P323(h1, h2, h3); else if (typ.equals("ta,p,ob")) vypocty5.P324(h1, h2, h3); else if (typ.equals("ta,r,ob")) vypocty5.P325(h1, h2, h3); else if (typ.equals("ta,ro,ob")) vypocty5.P326(h1, h2, h3); else if (typ.equals("p,r,ob")) vypocty5.P327(h1, h2, h3); else if (typ.equals("p,ro,ob")) {
            chyba1("   p = ro*s = ro*ob/2");
        } else if (typ.equals("r,ro,ob")) vypocty5.P328(h1, h2, h3); else if (typ.equals("a,ua,ob")) vypocty5.P330(h1, h2, h3); else if (typ.equals("a,ub,ob")) vypocty5.P331(h1, h2, h3, true); else if (typ.equals("a,uc,ob")) vypocty5.P331(h1, h2, h3, false); else if (typ.equals("alfa,ua,ob")) vypocty5.P332(Math.toRadians(h1), h2, h3); else if (typ.equals("alfa,ub,ob")) vypocty5.P333(Math.toRadians(h1), h2, h3, true); else if (typ.equals("alfa,uc,ob")) vypocty5.P333(Math.toRadians(h1), h2, h3, false); else if (typ.equals("va,ua,ob")) vypocty5.P335(h1, h2, h3); else if (typ.equals("va,ub,ob")) vypocty5.P336(h1, h2, h3, true); else if (typ.equals("va,uc,ob")) vypocty5.P336(h1, h2, h3, false); else if (typ.equals("ta,ua,ob")) vypocty5.P338(h1, h2, h3); else if (typ.equals("ta,ub,ob")) vypocty5.P339(h1, h2, h3, true); else if (typ.equals("ta,uc,ob")) vypocty5.P339(h1, h2, h3, false); else if (typ.equals("ua,ub,ob")) vypocty5.P341(h1, h2, h3); else if (typ.equals("ua,p,ob")) vypocty5.P342(h1, h2, h3); else if (typ.equals("ua,r,ob")) vypocty5.P343(h1, h2, h3); else if (typ.equals("ua,ro,ob")) vypocty5.P344(h1, h2, h3); else if (typ.equals("a,b,z")) vypocty6.P400(h1, h2, Math.toRadians(h3)); else if (typ.equals("a,c,z")) vypocty6.P401(h1, h2, Math.toRadians(h3)); else if (typ.equals("a,gama,z")) vypocty6.P402(h1, Math.toRadians(h2), Math.toRadians(h3), "a"); else if (typ.equals("a,va,z")) vypocty6.P403(h1, h2, Math.toRadians(h3)); else if (typ.equals("a,vb,z")) vypocty6.P404(h1, h2, Math.toRadians(h3)); else if (typ.equals("a,vc,z")) vypocty6.P405(h1, h2, Math.toRadians(h3)); else if (typ.equals("a,ta,z")) vypocty6.P406(h1, h2, Math.toRadians(h3)); else if (typ.equals("a,tb,z")) vypocty6.P407(h1, h2, Math.toRadians(h3)); else if (typ.equals("a,tc,z")) vypocty6.P408(h1, h2, Math.toRadians(h3)); else if (typ.equals("a,ua,z")) vypocty6.P409(h1, h2, Math.toRadians(h3)); else if (typ.equals("a,ub,z")) vypocty6.P410(h1, h2, Math.toRadians(h3)); else if (typ.equals("a,uc,z")) vypocty6.P411(h1, h2, Math.toRadians(h3)); else if (typ.equals("a,p,z")) vypocty6.P412(h1, h2, Math.toRadians(h3)); else if (typ.equals("a,r,z")) vypocty6.P413(h1, h2, Math.toRadians(h3)); else if (typ.equals("a,ro,z")) vypocty6.P414(h1, h2, Math.toRadians(h3)); else if (typ.equals("a,ob,z")) vypocty6.P415(h1, h2, Math.toRadians(h3)); else if (typ.equals("b,c,z")) vypocty6.P416(h1, h2, Math.toRadians(h3)); else if (typ.equals("b,gama,z")) vypocty6.P402(h1, Math.toRadians(h2), Math.toRadians(h3), "b"); else if (typ.equals("b,va,z")) vypocty6.P418(h1, h2, Math.toRadians(h3)); else if (typ.equals("b,vb,z")) vypocty6.P419(h1, h2, Math.toRadians(h3)); else if (typ.equals("b,vc,z")) vypocty6.P420(h1, h2, Math.toRadians(h3)); else if (typ.equals("b,ta,z")) vypocty6.P421(h1, h2, Math.toRadians(h3)); else if (typ.equals("b,tb,z")) vypocty6.P422(h1, h2, Math.toRadians(h3)); else if (typ.equals("b,tc,z")) vypocty6.P423(h1, h2, Math.toRadians(h3)); else if (typ.equals("b,ua,z")) vypocty6.P424(h1, h2, Math.toRadians(h3)); else if (typ.equals("b,ub,z")) vypocty6.P425(h1, h2, Math.toRadians(h3)); else if (typ.equals("b,uc,z")) vypocty6.P426(h1, h2, Math.toRadians(h3)); else if (typ.equals("b,p,z")) vypocty6.P427(h1, h2, Math.toRadians(h3)); else if (typ.equals("b,r,z")) vypocty6.P428(h1, h2, Math.toRadians(h3)); else if (typ.equals("b,ro,z")) vypocty6.P429(h1, h2, Math.toRadians(h3)); else if (typ.equals("b,ob,z")) vypocty6.P430(h1, h2, Math.toRadians(h3)); else if (typ.equals("c,gama,z")) vypocty6.P402(h1, Math.toRadians(h2), Math.toRadians(h3), "c"); else if (typ.equals("c,va,z")) vypocty6.P431(h1, h2, Math.toRadians(h3)); else if (typ.equals("c,vb,z")) vypocty6.P432(h1, h2, Math.toRadians(h3)); else if (typ.equals("c,vc,z")) vypocty6.P433(h1, h2, Math.toRadians(h3)); else if (typ.equals("c,ta,z")) vypocty6.P434(h1, h2, Math.toRadians(h3)); else if (typ.equals("c,tb,z")) vypocty6.P435(h1, h2, Math.toRadians(h3)); else if (typ.equals("c,tc,z")) vypocty6.P436(h1, h2, Math.toRadians(h3)); else if (typ.equals("c,ua,z")) vypocty6.P437(h1, h2, Math.toRadians(h3)); else if (typ.equals("c,ub,z")) vypocty6.P438(h1, h2, Math.toRadians(h3)); else if (typ.equals("c,uc,z")) vypocty6.P439(h1, h2, Math.toRadians(h3)); else if (typ.equals("c,p,z")) vypocty6.P440(h1, h2, Math.toRadians(h3)); else if (typ.equals("c,r,z")) vypocty6.P441(h1, h2, Math.toRadians(h3)); else if (typ.equals("c,ro,z")) vypocty6.P442(h1, h2, Math.toRadians(h3)); else if (typ.equals("c,ob,z")) vypocty6.P443(h1, h2, Math.toRadians(h3)); else if (typ.equals("va,vb,z")) vypocty6.P444(h1, h2, Math.toRadians(h3)); else if (typ.equals("va,vc,z")) vypocty6.P445(h1, h2, Math.toRadians(h3)); else if (typ.equals("va,ta,z")) vypocty6.P446(h1, h2, Math.toRadians(h3)); else if (typ.equals("va,tb,z")) vypocty6.P447(h1, h2, Math.toRadians(h3)); else if (typ.equals("va,tc,z")) vypocty6.P448(h1, h2, Math.toRadians(h3)); else if (typ.equals("va,ua,z")) vypocty6.P449(h1, h2, Math.toRadians(h3)); else if (typ.equals("va,ub,z")) vypocty6.P450(h1, h2, Math.toRadians(h3)); else if (typ.equals("va,uc,z")) vypocty6.P451(h1, h2, Math.toRadians(h3)); else if (typ.equals("va,p,z")) vypocty6.P452(h1, h2, Math.toRadians(h3)); else if (typ.equals("va,r,z")) vypocty6.P453(h1, h2, Math.toRadians(h3)); else if (typ.equals("va,ro,z")) vypocty6.P454(h1, h2, Math.toRadians(h3)); else if (typ.equals("va,ob,z")) vypocty6.P455(h1, h2, Math.toRadians(h3)); else if (typ.equals("vb,vc,z")) vypocty6.P456(h1, h2, Math.toRadians(h3)); else if (typ.equals("vb,ta,z")) vypocty6.P457(h1, h2, Math.toRadians(h3)); else if (typ.equals("vb,tb,z")) vypocty6.P458(h1, h2, Math.toRadians(h3)); else if (typ.equals("vb,tc,z")) vypocty6.P459(h1, h2, Math.toRadians(h3)); else if (typ.equals("vb,ua,z")) vypocty6.P460(h1, h2, Math.toRadians(h3)); else if (typ.equals("vb,ub,z")) vypocty6.P461(h1, h2, Math.toRadians(h3)); else if (typ.equals("vb,uc,z")) vypocty6.P462(h1, h2, Math.toRadians(h3)); else if (typ.equals("vb,p,z")) vypocty6.P463(h1, h2, Math.toRadians(h3)); else if (typ.equals("vb,r,z")) vypocty6.P464(h1, h2, Math.toRadians(h3)); else if (typ.equals("vb,ro,z")) vypocty6.P465(h1, h2, Math.toRadians(h3)); else if (typ.equals("vb,ob,z")) vypocty6.P466(h1, h2, Math.toRadians(h3)); else if (typ.equals("vc,ta,z")) vypocty6.P467(h1, h2, Math.toRadians(h3)); else if (typ.equals("vc,tb,z")) vypocty6.P468(h1, h2, Math.toRadians(h3)); else if (typ.equals("vc,tc,z")) vypocty6.P469(h1, h2, Math.toRadians(h3)); else if (typ.equals("vc,ua,z")) vypocty6.P470(h1, h2, Math.toRadians(h3)); else if (typ.equals("vc,ub,z")) vypocty6.P471(h1, h2, Math.toRadians(h3)); else if (typ.equals("vc,uc,z")) {
            chyba1("   alfa - beta = const");
        } else if (typ.equals("vc,p,z")) vypocty6.P472(h1, h2, Math.toRadians(h3)); else if (typ.equals("vc,r,z")) vypocty6.P473(h1, h2, Math.toRadians(h3)); else if (typ.equals("vc,ro,z")) vypocty6.P474(h1, h2, Math.toRadians(h3)); else if (typ.equals("vc,ob,z")) vypocty6.P475(h1, h2, Math.toRadians(h3)); else if (typ.equals("ta,tb,z")) vypocty6.P476(h1, h2, Math.toRadians(h3)); else if (typ.equals("ta,tc,z")) vypocty6.P477(h1, h2, Math.toRadians(h3)); else if (typ.equals("ta,ua,z")) vypocty6.P478(h1, h2, Math.toRadians(h3)); else if (typ.equals("ta,ub,z")) vypocty6.P479(h1, h2, Math.toRadians(h3)); else if (typ.equals("ta,uc,z")) vypocty6.P480(h1, h2, Math.toRadians(h3)); else if (typ.equals("ta,p,z")) vypocty6.P481(h1, h2, Math.toRadians(h3)); else if (typ.equals("ta,r,z")) vypocty6.P482(h1, h2, Math.toRadians(h3)); else if (typ.equals("ta,ro,z")) vypocty6.P483(h1, h2, Math.toRadians(h3)); else if (typ.equals("ta,ob,z")) vypocty6.P484(h1, h2, Math.toRadians(h3)); else if (typ.equals("tb,tc,z")) vypocty6.P485(h1, h2, Math.toRadians(h3)); else if (typ.equals("tb,ua,z")) vypocty6.P486(h1, h2, Math.toRadians(h3)); else if (typ.equals("tb,ub,z")) vypocty6.P487(h1, h2, Math.toRadians(h3)); else if (typ.equals("tb,uc,z")) vypocty6.P488(h1, h2, Math.toRadians(h3)); else if (typ.equals("tb,p,z")) vypocty6.P489(h1, h2, Math.toRadians(h3)); else if (typ.equals("tb,r,z")) vypocty6.P490(h1, h2, Math.toRadians(h3)); else if (typ.equals("tb,ro,z")) vypocty6.P491(h1, h2, Math.toRadians(h3)); else if (typ.equals("tb,ob,z")) vypocty6.P492(h1, h2, Math.toRadians(h3)); else if (typ.equals("tc,ua,z")) vypocty6.P493(h1, h2, Math.toRadians(h3)); else if (typ.equals("tc,ub,z")) vypocty6.P494(h1, h2, Math.toRadians(h3)); else if (typ.equals("tc,uc,z")) vypocty6.P495(h1, h2, Math.toRadians(h3)); else if (typ.equals("tc,p,z")) vypocty6.P496(h1, h2, Math.toRadians(h3)); else if (typ.equals("tc,r,z")) vypocty6.P497(h1, h2, Math.toRadians(h3)); else if (typ.equals("tc,ro,z")) vypocty6.P498(h1, h2, Math.toRadians(h3)); else if (typ.equals("tc,ob,z")) vypocty6.P499(h1, h2, Math.toRadians(h3)); else if (typ.equals("ua,ub,z")) vypocty6.P500(h1, h2, Math.toRadians(h3)); else if (typ.equals("ua,uc,z")) vypocty6.P501(h1, h2, Math.toRadians(h3)); else if (typ.equals("ua,p,z")) vypocty6.P502(h1, h2, Math.toRadians(h3)); else if (typ.equals("ua,r,z")) vypocty6.P503(h1, h2, Math.toRadians(h3)); else if (typ.equals("ua,ro,z")) vypocty6.P504(h1, h2, Math.toRadians(h3)); else if (typ.equals("ua,ob,z")) vypocty6.P505(h1, h2, Math.toRadians(h3)); else if (typ.equals("ub,uc,z")) vypocty6.P506(h1, h2, Math.toRadians(h3)); else if (typ.equals("ub,p,z")) vypocty6.P507(h1, h2, Math.toRadians(h3)); else if (typ.equals("ub,r,z")) vypocty6.P508(h1, h2, Math.toRadians(h3)); else if (typ.equals("ub,ro,z")) vypocty6.P509(h1, h2, Math.toRadians(h3)); else if (typ.equals("ub,ob,z")) vypocty6.P510(h1, h2, Math.toRadians(h3)); else if (typ.equals("uc,p,z")) vypocty6.P511(h1, h2, Math.toRadians(h3)); else if (typ.equals("uc,r,z")) vypocty6.P512(h1, h2, Math.toRadians(h3)); else if (typ.equals("uc,ro,z")) vypocty6.P513(h1, h2, Math.toRadians(h3)); else if (typ.equals("uc,ob,z")) vypocty6.P514(h1, h2, Math.toRadians(h3)); else if (typ.equals("p,r,z")) vypocty6.P515(h1, h2, Math.toRadians(h3)); else if (typ.equals("p,ro,z")) vypocty6.P516(h1, h2, Math.toRadians(h3)); else if (typ.equals("p,ob,z")) vypocty6.P517(h1, h2, Math.toRadians(h3)); else if (typ.equals("r,ro,z")) vypocty6.P518(h1, h2, Math.toRadians(h3)); else if (typ.equals("r,ob,z")) vypocty6.P519(h1, h2, Math.toRadians(h3)); else if (typ.equals("ro,ob,z")) vypocty6.P520(h1, h2, Math.toRadians(h3)); else {
            hlaseni("NOPROGRAM");
        }
        return reseni;
    }

    public String grafTyp(String typ) {
        String gr = "";
        if (typ.equals("a,b,c")) gr = "G001"; else if (typ.equals("a,b,alfa")) gr = "G002"; else if (typ.equals("a,b,beta")) gr = "G002"; else if (typ.equals("a,b,gama")) gr = "G004"; else if (typ.equals("a,b,va")) gr = "G005"; else if (typ.equals("a,b,vb")) gr = "G005"; else if (typ.equals("a,b,vc")) gr = "G007"; else if (typ.equals("a,b,p")) gr = "G008"; else if (typ.equals("a,b,r")) gr = "G009"; else if (typ.equals("a,b,ro")) gr = "G010"; else if (typ.equals("a,alfa,va")) gr = "G013"; else if (typ.equals("a,alfa,vb")) gr = "G014"; else if (typ.equals("a,alfa,vc")) gr = "G014"; else if (typ.equals("a,alfa,p")) gr = "G016"; else if (typ.equals("a,alfa,ro")) gr = "G017"; else if (typ.equals("a,beta,va")) gr = "G019"; else if (typ.equals("a,gama,va")) gr = "G019"; else if (typ.equals("a,beta,vb")) gr = "G021"; else if (typ.equals("a,gama,vc")) gr = "G021"; else if (typ.equals("a,beta,p")) gr = "G023"; else if (typ.equals("a,gama,p")) gr = "G023"; else if (typ.equals("a,beta,r")) gr = "G024"; else if (typ.equals("a,gama,r")) gr = "G024"; else if (typ.equals("a,beta,ro")) gr = "G025"; else if (typ.equals("a,gama,ro")) gr = "G025"; else if (typ.equals("a,va,r")) gr = "G027"; else if (typ.equals("a,va,ro")) gr = "G028"; else if (typ.equals("a,vb,vc")) gr = "G029"; else if (typ.equals("a,vb,p")) gr = "G030"; else if (typ.equals("a,vc,p")) gr = "G030"; else if (typ.equals("a,vb,r")) gr = "G031"; else if (typ.equals("a,vc,r")) gr = "G031"; else if (typ.equals("a,vb,ro")) gr = "G032"; else if (typ.equals("a,vc,ro")) gr = "G032"; else if (typ.equals("a,p,r")) gr = "G033"; else if (typ.equals("a,p,ro")) gr = "G034"; else if (typ.equals("a,r,ro")) gr = "G035"; else if (typ.equals("alfa,va,vb")) gr = "G040"; else if (typ.equals("alfa,va,vc")) gr = "G040"; else if (typ.equals("alfa,va,p")) gr = "G041"; else if (typ.equals("alfa,va,r")) gr = "G042"; else if (typ.equals("alfa,va,ro")) gr = "G043"; else if (typ.equals("alfa,vb,vc")) gr = "G044"; else if (typ.equals("alfa,vb,p")) gr = "G045"; else if (typ.equals("alfa,vc,p")) gr = "G045"; else if (typ.equals("alfa,vb,r")) gr = "G046"; else if (typ.equals("alfa,vc,r")) gr = "G046"; else if (typ.equals("alfa,vb,ro")) gr = "G047"; else if (typ.equals("alfa,vc,ro")) gr = "G047"; else if (typ.equals("alfa,p,r")) gr = "G048"; else if (typ.equals("alfa,p,ro")) gr = "G049"; else if (typ.equals("alfa,r,ro")) gr = "G050"; else if (typ.equals("va,vb,vc")) gr = "G051"; else if (typ.equals("va,vb,p")) gr = "G052"; else if (typ.equals("va,vb,r")) gr = "G053"; else if (typ.equals("va,vb,ro")) gr = "G054"; else if (typ.equals("va,p,r")) gr = "G055"; else if (typ.equals("va,p,ro")) gr = "G056"; else if (typ.equals("va,r,ro")) gr = "G057"; else if (typ.equals("p,r,ro")) gr = "G058"; else if (typ.equals("a,b,ta")) gr = "G060"; else if (typ.equals("a,b,tb")) gr = "G060"; else if (typ.equals("a,b,tc")) gr = "G061"; else if (typ.equals("a,alfa,ta")) gr = "G062"; else if (typ.equals("a,alfa,tb")) gr = "G063"; else if (typ.equals("a,alfa,tc")) gr = "G063"; else if (typ.equals("a,beta,ta")) gr = "G064"; else if (typ.equals("a,gama,ta")) gr = "G064"; else if (typ.equals("a,beta,tb")) gr = "G065"; else if (typ.equals("a,gama,tc")) gr = "G065"; else if (typ.equals("a,beta,tc")) gr = "G066"; else if (typ.equals("a,gama,tb")) gr = "G066"; else if (typ.equals("a,va,ta")) gr = "G067"; else if (typ.equals("a,va,tb")) gr = "G068"; else if (typ.equals("a,va,tc")) gr = "G068"; else if (typ.equals("a,vb,ta")) gr = "G069"; else if (typ.equals("a,vc,ta")) gr = "G069"; else if (typ.equals("a,vb,tb")) gr = "G070"; else if (typ.equals("a,vc,tc")) gr = "G070"; else if (typ.equals("a,vb,tc")) gr = "G071"; else if (typ.equals("a,vc,tb")) gr = "G071"; else if (typ.equals("a,ta,p")) gr = "G072"; else if (typ.equals("a,tb,p")) gr = "G073"; else if (typ.equals("a,tc,p")) gr = "G073"; else if (typ.equals("a,ta,r")) gr = "G074"; else if (typ.equals("a,tb,r")) gr = "G075"; else if (typ.equals("a,tc,r")) gr = "G075"; else if (typ.equals("a,ta,ro")) gr = "G076"; else if (typ.equals("a,tb,ro")) gr = "G077"; else if (typ.equals("a,tc,ro")) gr = "G077"; else if (typ.equals("a,ta,tb")) gr = "G078"; else if (typ.equals("a,ta,tc")) gr = "G078"; else if (typ.equals("a,tb,tc")) gr = "G079"; else if (typ.equals("alfa,va,ta")) gr = "G082"; else if (typ.equals("alfa,va,tb")) gr = "G083"; else if (typ.equals("alfa,va,tc")) gr = "G083"; else if (typ.equals("alfa,vb,ta")) gr = "G084"; else if (typ.equals("alfa,vc,ta")) gr = "G084"; else if (typ.equals("alfa,vb,tb")) gr = "G085"; else if (typ.equals("alfa,vc,tc")) gr = "G085"; else if (typ.equals("alfa,vb,tc")) gr = "G086"; else if (typ.equals("alfa,vc,tb")) gr = "G086"; else if (typ.equals("alfa,ta,p")) gr = "G087"; else if (typ.equals("alfa,tb,p")) gr = "G088"; else if (typ.equals("alfa,tc,p")) gr = "G088"; else if (typ.equals("alfa,ta,r")) gr = "G089"; else if (typ.equals("alfa,tb,r")) gr = "G090"; else if (typ.equals("alfa,tc,r")) gr = "G090"; else if (typ.equals("alfa,ta,ro")) gr = "G091"; else if (typ.equals("alfa,tb,ro")) gr = "G092"; else if (typ.equals("alfa,tc,ro")) gr = "G092"; else if (typ.equals("alfa,ta,tb")) gr = "G093"; else if (typ.equals("alfa,ta,tc")) gr = "G093"; else if (typ.equals("alfa,tb,tc")) gr = "G094"; else if (typ.equals("va,vb,ta")) gr = "G095"; else if (typ.equals("va,vb,tb")) gr = "G096"; else if (typ.equals("va,vb,tc")) gr = "G097"; else if (typ.equals("va,ta,p")) gr = "G098"; else if (typ.equals("va,tb,p")) gr = "G099"; else if (typ.equals("va,tc,p")) gr = "G099"; else if (typ.equals("va,ta,r")) gr = "G100"; else if (typ.equals("va,tb,r")) gr = "G101"; else if (typ.equals("va,tc,r")) gr = "G101"; else if (typ.equals("va,ta,ro")) gr = "G102"; else if (typ.equals("va,tb,ro")) gr = "G103"; else if (typ.equals("va,tc,ro")) gr = "G103"; else if (typ.equals("va,ta,tb")) gr = "G104"; else if (typ.equals("va,ta,tc")) gr = "G104"; else if (typ.equals("va,tb,tc")) gr = "G105"; else if (typ.equals("ta,p,r")) gr = "G106"; else if (typ.equals("ta,p,ro")) gr = "G107"; else if (typ.equals("ta,tb,p")) gr = "G108"; else if (typ.equals("ta,r,ro")) gr = "G109"; else if (typ.equals("ta,tb,r")) gr = "G110"; else if (typ.equals("ta,tb,ro")) gr = "G111"; else if (typ.equals("ta,tb,tc")) gr = "G112"; else if (typ.equals("a,b,ua")) gr = "G120"; else if (typ.equals("a,b,ub")) gr = "G120"; else if (typ.equals("a,b,uc")) gr = "G121"; else if (typ.equals("a,alfa,ua")) gr = "G122"; else if (typ.equals("a,alfa,ub")) gr = "G123"; else if (typ.equals("a,alfa,uc")) gr = "G123"; else if (typ.equals("a,beta,ua")) gr = "G124"; else if (typ.equals("a,gama,ua")) gr = "G124"; else if (typ.equals("a,beta,ub")) gr = "G125"; else if (typ.equals("a,gama,uc")) gr = "G125"; else if (typ.equals("a,beta,uc")) gr = "G126"; else if (typ.equals("a,gama,ub")) gr = "G126"; else if (typ.equals("a,va,ua")) gr = "G127"; else if (typ.equals("a,va,ub")) gr = "G128"; else if (typ.equals("a,va,uc")) gr = "G128"; else if (typ.equals("a,vb,ua")) gr = "G129"; else if (typ.equals("a,vc,ua")) gr = "G129"; else if (typ.equals("a,vb,ub")) gr = "G130"; else if (typ.equals("a,vc,uc")) gr = "G130"; else if (typ.equals("a,vb,uc")) gr = "G131"; else if (typ.equals("a,vc,ub")) gr = "G131"; else if (typ.equals("a,ta,ua")) gr = "G132"; else if (typ.equals("a,ta,ub")) gr = "G133"; else if (typ.equals("a,ta,uc")) gr = "G133"; else if (typ.equals("a,tb,ua")) gr = "G134"; else if (typ.equals("a,tc,ua")) gr = "G134"; else if (typ.equals("a,tb,ub")) gr = "G135"; else if (typ.equals("a,tc,uc")) gr = "G135"; else if (typ.equals("a,tb,uc")) gr = "G136"; else if (typ.equals("a,tc,ub")) gr = "G136"; else if (typ.equals("a,ua,ub")) gr = "G137"; else if (typ.equals("a,ua,uc")) gr = "G137"; else if (typ.equals("a,ub,uc")) gr = "G138"; else if (typ.equals("a,ua,p")) gr = "G139"; else if (typ.equals("a,ub,p")) gr = "G140"; else if (typ.equals("a,uc,p")) gr = "G140"; else if (typ.equals("a,ua,r")) gr = "G141"; else if (typ.equals("a,ub,r")) gr = "G142"; else if (typ.equals("a,uc,r")) gr = "G142"; else if (typ.equals("a,ua,ro")) gr = "G143"; else if (typ.equals("a,ub,ro")) gr = "G144"; else if (typ.equals("a,uc,ro")) gr = "G144"; else if (typ.equals("alfa,va,ua")) gr = "G146"; else if (typ.equals("alfa,va,ub")) gr = "G147"; else if (typ.equals("alfa,va,uc")) gr = "G147"; else if (typ.equals("alfa,vb,ua")) gr = "G148"; else if (typ.equals("alfa,vc,ua")) gr = "G148"; else if (typ.equals("alfa,vb,ub")) gr = "G149"; else if (typ.equals("alfa,vc,uc")) gr = "G149"; else if (typ.equals("alfa,vb,uc")) gr = "G150"; else if (typ.equals("alfa,vc,ub")) gr = "G150"; else if (typ.equals("alfa,ta,ua")) gr = "G151"; else if (typ.equals("alfa,ta,ub")) gr = "G152"; else if (typ.equals("alfa,ta,uc")) gr = "G152"; else if (typ.equals("alfa,tb,ua")) gr = "G153"; else if (typ.equals("alfa,tc,ua")) gr = "G153"; else if (typ.equals("alfa,tb,ub")) gr = "G154"; else if (typ.equals("alfa,tc,uc")) gr = "G154"; else if (typ.equals("alfa,tb,uc")) gr = "G155"; else if (typ.equals("alfa,tc,ub")) gr = "G155"; else if (typ.equals("alfa,ua,ub")) gr = "G156"; else if (typ.equals("alfa,ua,uc")) gr = "G156"; else if (typ.equals("alfa,ub,uc")) gr = "G157"; else if (typ.equals("alfa,ua,p")) gr = "G158"; else if (typ.equals("alfa,ub,p")) gr = "G159"; else if (typ.equals("alfa,uc,p")) gr = "G159"; else if (typ.equals("alfa,ua,r")) gr = "G160"; else if (typ.equals("alfa,ub,r")) gr = "G161"; else if (typ.equals("alfa,uc,r")) gr = "G161"; else if (typ.equals("alfa,ua,ro")) gr = "G162"; else if (typ.equals("alfa,ub,ro")) gr = "G163"; else if (typ.equals("alfa,uc,ro")) gr = "G163"; else if (typ.equals("va,vb,ua")) gr = "G164"; else if (typ.equals("va,vb,ub")) gr = "G164"; else if (typ.equals("va,vb,uc")) gr = "G166"; else if (typ.equals("va,ta,ua")) gr = "G167"; else if (typ.equals("va,ta,ub")) gr = "G168"; else if (typ.equals("va,ta,uc")) gr = "G168"; else if (typ.equals("va,tb,ua")) gr = "G169"; else if (typ.equals("va,tc,ua")) gr = "G169"; else if (typ.equals("va,tb,ub")) gr = "G170"; else if (typ.equals("va,tc,uc")) gr = "G170"; else if (typ.equals("va,tb,uc")) gr = "G171"; else if (typ.equals("va,tc,ub")) gr = "G171"; else if (typ.equals("va,ua,ub")) gr = "G172"; else if (typ.equals("va,ua,uc")) gr = "G172"; else if (typ.equals("va,ub,uc")) gr = "G173"; else if (typ.equals("va,ua,p")) gr = "G174"; else if (typ.equals("va,ub,p")) gr = "G175"; else if (typ.equals("va,uc,p")) gr = "G175"; else if (typ.equals("va,ua,r")) gr = "G176"; else if (typ.equals("va,ub,r")) gr = "G177"; else if (typ.equals("va,uc,r")) gr = "G177"; else if (typ.equals("va,ua,ro")) gr = "G178"; else if (typ.equals("va,ub,ro")) gr = "G179"; else if (typ.equals("va,uc,ro")) gr = "G179"; else if (typ.equals("ta,tb,ua")) gr = "G180"; else if (typ.equals("ta,tb,ub")) gr = "G181"; else if (typ.equals("ta,tb,uc")) gr = "G182"; else if (typ.equals("ta,ua,ub")) gr = "G183"; else if (typ.equals("ta,ua,uc")) gr = "G183"; else if (typ.equals("ta,ub,uc")) gr = "G184"; else if (typ.equals("ta,ua,p")) gr = "G185"; else if (typ.equals("ta,ub,p")) gr = "G186"; else if (typ.equals("ta,uc,p")) gr = "G186"; else if (typ.equals("ta,ua,r")) gr = "G187"; else if (typ.equals("ta,ub,r")) gr = "G188"; else if (typ.equals("ta,uc,r")) gr = "G188"; else if (typ.equals("ta,ua,ro")) gr = "G189"; else if (typ.equals("ta,ub,ro")) gr = "G190"; else if (typ.equals("ta,uc,ro")) gr = "G190"; else if (typ.equals("ua,ub,p")) gr = "G191"; else if (typ.equals("ua,ub,r")) gr = "G192"; else if (typ.equals("ua,ub,ro")) gr = "G193"; else if (typ.equals("ua,ub,uc")) gr = "G194"; else if (typ.equals("ua,p,r")) gr = "G195"; else if (typ.equals("ua,p,ro")) gr = "G196"; else if (typ.equals("ua,r,ro")) gr = "G197"; else if (typ.equals("c,alfa,ab")) gr = "G200"; else if (typ.equals("c,beta,ab")) gr = "G200"; else if (typ.equals("c,gama,ab")) gr = "G201"; else if (typ.equals("c,va,ab")) gr = "G202"; else if (typ.equals("c,vb,ab")) gr = "G202"; else if (typ.equals("c,vc,ab")) gr = "G203"; else if (typ.equals("c,ta,ab")) gr = "G204"; else if (typ.equals("c,tb,ab")) gr = "G204"; else if (typ.equals("c,tc,ab")) gr = "G205"; else if (typ.equals("c,p,ab")) gr = "G206"; else if (typ.equals("c,r,ab")) gr = "G207"; else if (typ.equals("c,ro,ab")) gr = "G208"; else if (typ.equals("alfa,va,ab")) gr = "G211"; else if (typ.equals("beta,vb,ab")) gr = "G211"; else if (typ.equals("alfa,vb,ab")) gr = "G212"; else if (typ.equals("beta,va,ab")) gr = "G212"; else if (typ.equals("alfa,vc,ab")) gr = "G213"; else if (typ.equals("beta,vc,ab")) gr = "G213"; else if (typ.equals("alfa,ta,ab")) gr = "G214"; else if (typ.equals("beta,tb,ab")) gr = "G214"; else if (typ.equals("alfa,tb,ab")) gr = "G215"; else if (typ.equals("beta,ta,ab")) gr = "G215"; else if (typ.equals("alfa,tc,ab")) gr = "G216"; else if (typ.equals("beta,tc,ab")) gr = "G216"; else if (typ.equals("alfa,p,ab")) gr = "G217"; else if (typ.equals("beta,p,ab")) gr = "G217"; else if (typ.equals("alfa,r,ab")) gr = "G218"; else if (typ.equals("beta,r,ab")) gr = "G218"; else if (typ.equals("alfa,ro,ab")) gr = "G219"; else if (typ.equals("beta,ro,ab")) gr = "G219"; else if (typ.equals("gama,va,ab")) gr = "G220"; else if (typ.equals("gama,vb,ab")) gr = "G220"; else if (typ.equals("gama,vc,ab")) gr = "G221"; else if (typ.equals("va,vb,ab")) gr = "G222"; else if (typ.equals("va,vc,ab")) gr = "G223"; else if (typ.equals("vb,vc,ab")) gr = "G223"; else if (typ.equals("va,p,ab")) gr = "G224"; else if (typ.equals("vb,p,ab")) gr = "G224"; else if (typ.equals("va,r,ab")) gr = "G225"; else if (typ.equals("vb,r,ab")) gr = "G225"; else if (typ.equals("va,ro,ab")) gr = "G226"; else if (typ.equals("vb,ro,ab")) gr = "G226"; else if (typ.equals("gama,ta,ab")) gr = "G227"; else if (typ.equals("gama,tb,ab")) gr = "G227"; else if (typ.equals("gama,tc,ab")) gr = "G228"; else if (typ.equals("gama,p,ab")) gr = "G229"; else if (typ.equals("gama,r,ab")) gr = "G230"; else if (typ.equals("gama,ro,ab")) gr = "G231"; else if (typ.equals("va,ta,ab")) gr = "G232"; else if (typ.equals("vb,tb,ab")) gr = "G232"; else if (typ.equals("va,tb,ab")) gr = "G233"; else if (typ.equals("vb,ta,ab")) gr = "G233"; else if (typ.equals("va,tc,ab")) gr = "G234"; else if (typ.equals("vb,tc,ab")) gr = "G234"; else if (typ.equals("vc,ta,ab")) gr = "G235"; else if (typ.equals("vc,tb,ab")) gr = "G235"; else if (typ.equals("vc,tc,ab")) gr = "G236"; else if (typ.equals("vc,p,ab")) gr = "G237"; else if (typ.equals("vc,r,ab")) gr = "G238"; else if (typ.equals("vc,ro,ab")) gr = "G239"; else if (typ.equals("ta,tb,ab")) gr = "G240"; else if (typ.equals("ta,tc,ab")) gr = "G241"; else if (typ.equals("tb,tc,ab")) gr = "G241"; else if (typ.equals("ta,p,ab")) gr = "G242"; else if (typ.equals("tb,p,ab")) gr = "G242"; else if (typ.equals("ta,r,ab")) gr = "G243"; else if (typ.equals("tb,r,ab")) gr = "G243"; else if (typ.equals("ta,ro,ab")) gr = "G244"; else if (typ.equals("tb,ro,ab")) gr = "G244"; else if (typ.equals("tc,p,ab")) gr = "G245"; else if (typ.equals("tc,r,ab")) gr = "G246"; else if (typ.equals("tc,ro,ab")) gr = "G247"; else if (typ.equals("p,r,ab")) gr = "G248"; else if (typ.equals("p,ro,ab")) gr = "G249"; else if (typ.equals("r,ro,ab")) gr = "G250"; else if (typ.equals("c,ua,ab")) gr = "G255"; else if (typ.equals("c,ub,ab")) gr = "G255"; else if (typ.equals("c,uc,ab")) gr = "G256"; else if (typ.equals("alfa,ua,ab")) gr = "G257"; else if (typ.equals("beta,ub,ab")) gr = "G257"; else if (typ.equals("alfa,ub,ab")) gr = "G258"; else if (typ.equals("beta,ua,ab")) gr = "G258"; else if (typ.equals("alfa,uc,ab")) gr = "G259"; else if (typ.equals("beta,uc,ab")) gr = "G259"; else if (typ.equals("gama,ua,ab")) gr = "G262"; else if (typ.equals("gama,ub,ab")) gr = "G262"; else if (typ.equals("gama,uc,ab")) gr = "G264"; else if (typ.equals("va,ua,ab")) gr = "G265"; else if (typ.equals("vb,ub,ab")) gr = "G265"; else if (typ.equals("va,ub,ab")) gr = "G266"; else if (typ.equals("vb,ua,ab")) gr = "G266"; else if (typ.equals("va,uc,ab")) gr = "G267"; else if (typ.equals("vb,uc,ab")) gr = "G267"; else if (typ.equals("vc,ua,ab")) gr = "G271"; else if (typ.equals("vc,ub,ab")) gr = "G271"; else if (typ.equals("vc,uc,ab")) gr = "G273"; else if (typ.equals("ta,ua,ab")) gr = "G274"; else if (typ.equals("tb,ub,ab")) gr = "G274"; else if (typ.equals("ta,ub,ab")) gr = "G275"; else if (typ.equals("tb,ua,ab")) gr = "G275"; else if (typ.equals("ta,uc,ab")) gr = "G276"; else if (typ.equals("tb,uc,ab")) gr = "G276"; else if (typ.equals("tc,ua,ab")) gr = "G280"; else if (typ.equals("tc,ub,ab")) gr = "G280"; else if (typ.equals("tc,uc,ab")) gr = "G281"; else if (typ.equals("ua,ub,ab")) gr = "G282"; else if (typ.equals("ua,uc,ab")) gr = "G283"; else if (typ.equals("ub,uc,ab")) gr = "G283"; else if (typ.equals("ua,p,ab")) gr = "G284"; else if (typ.equals("ub,p,ab")) gr = "G284"; else if (typ.equals("ua,r,ab")) gr = "G285"; else if (typ.equals("ub,r,ab")) gr = "G285"; else if (typ.equals("ua,ro,ab")) gr = "G286"; else if (typ.equals("ub,ro,ab")) gr = "G286"; else if (typ.equals("uc,p,ab")) gr = "G287"; else if (typ.equals("uc,r,ab")) gr = "G288"; else if (typ.equals("uc,ro,ab")) gr = "G289"; else if (typ.equals("a,alfa,ob")) gr = "G300"; else if (typ.equals("a,beta,ob")) gr = "G301"; else if (typ.equals("a,gama,ob")) gr = "G301"; else if (typ.equals("a,va,ob")) gr = "G302"; else if (typ.equals("a,vb,ob")) gr = "G303"; else if (typ.equals("a,vc,ob")) gr = "G303"; else if (typ.equals("a,ta,ob")) gr = "G304"; else if (typ.equals("a,tb,ob")) gr = "G305"; else if (typ.equals("a,tc,ob")) gr = "G305"; else if (typ.equals("a,p,ob")) gr = "G306"; else if (typ.equals("a,r,ob")) gr = "G307"; else if (typ.equals("a,ro,ob")) gr = "G308"; else if (typ.equals("alfa,beta,ob")) gr = "G309"; else if (typ.equals("alfa,va,ob")) gr = "G310"; else if (typ.equals("alfa,vb,ob")) gr = "G311"; else if (typ.equals("alfa,vc,ob")) gr = "G311"; else if (typ.equals("alfa,ta,ob")) gr = "G312"; else if (typ.equals("alfa,tb,ob")) gr = "G313"; else if (typ.equals("alfa,tc,ob")) gr = "G313"; else if (typ.equals("alfa,p,ob")) gr = "G314"; else if (typ.equals("alfa,r,ob")) gr = "G315"; else if (typ.equals("alfa,ro,ob")) gr = "G316"; else if (typ.equals("va,vb,ob")) gr = "G317"; else if (typ.equals("va,ta,ob")) gr = "G318"; else if (typ.equals("va,tb,ob")) gr = "G319"; else if (typ.equals("va,tc,ob")) gr = "G319"; else if (typ.equals("va,p,ob")) gr = "G320"; else if (typ.equals("va,r,ob")) gr = "G321"; else if (typ.equals("va,ro,ob")) gr = "G322"; else if (typ.equals("ta,tb,ob")) gr = "G323"; else if (typ.equals("ta,p,ob")) gr = "G324"; else if (typ.equals("ta,r,ob")) gr = "G325"; else if (typ.equals("ta,ro,ob")) gr = "G326"; else if (typ.equals("p,r,ob")) gr = "G327"; else if (typ.equals("r,ro,ob")) gr = "G328"; else if (typ.equals("a,ua,ob")) gr = "G330"; else if (typ.equals("a,ub,ob")) gr = "G331"; else if (typ.equals("a,uc,ob")) gr = "G331"; else if (typ.equals("alfa,ua,ob")) gr = "G332"; else if (typ.equals("alfa,ub,ob")) gr = "G333"; else if (typ.equals("alfa,uc,ob")) gr = "G333"; else if (typ.equals("va,ua,ob")) gr = "G335"; else if (typ.equals("va,ub,ob")) gr = "G336"; else if (typ.equals("va,uc,ob")) gr = "G336"; else if (typ.equals("ta,ua,ob")) gr = "G338"; else if (typ.equals("ta,ub,ob")) gr = "G339"; else if (typ.equals("ta,uc,ob")) gr = "G339"; else if (typ.equals("ua,ub,ob")) gr = "G341"; else if (typ.equals("ua,p,ob")) gr = "G342"; else if (typ.equals("ua,r,ob")) gr = "G343"; else if (typ.equals("ua,ro,ob")) gr = "G344"; else if (typ.equals("a,b,z")) gr = "G400"; else if (typ.equals("a,c,z")) gr = "G401"; else if (typ.equals("a,gama,z")) gr = "G402"; else if (typ.equals("a,va,z")) gr = "G403"; else if (typ.equals("a,vb,z")) gr = "G404"; else if (typ.equals("a,vc,z")) gr = "G405"; else if (typ.equals("a,ta,z")) gr = "G406"; else if (typ.equals("a,tb,z")) gr = "G407"; else if (typ.equals("a,tc,z")) gr = "G408"; else if (typ.equals("a,ua,z")) gr = "G409"; else if (typ.equals("a,ub,z")) gr = "G410"; else if (typ.equals("a,uc,z")) gr = "G411"; else if (typ.equals("a,p,z")) gr = "G412"; else if (typ.equals("a,r,z")) gr = "G413"; else if (typ.equals("a,ro,z")) gr = "G414"; else if (typ.equals("a,ob,z")) gr = "G415"; else if (typ.equals("b,c,z")) gr = "G416"; else if (typ.equals("b,gama,z")) gr = "G417"; else if (typ.equals("b,va,z")) gr = "G418"; else if (typ.equals("b,vb,z")) gr = "G419"; else if (typ.equals("b,vc,z")) gr = "G420"; else if (typ.equals("b,ta,z")) gr = "G421"; else if (typ.equals("b,tb,z")) gr = "G422"; else if (typ.equals("b,tc,z")) gr = "G423"; else if (typ.equals("b,ua,z")) gr = "G424"; else if (typ.equals("b,ub,z")) gr = "G425"; else if (typ.equals("b,uc,z")) gr = "G426"; else if (typ.equals("b,p,z")) gr = "G427"; else if (typ.equals("b,r,z")) gr = "G428"; else if (typ.equals("b,ro,z")) gr = "G429"; else if (typ.equals("b,ob,z")) gr = "G430"; else if (typ.equals("c,gama,z")) gr = "G417"; else if (typ.equals("c,va,z")) gr = "G431"; else if (typ.equals("c,vb,z")) gr = "G432"; else if (typ.equals("c,vc,z")) gr = "G433"; else if (typ.equals("c,ta,z")) gr = "G434"; else if (typ.equals("c,tb,z")) gr = "G435"; else if (typ.equals("c,tc,z")) gr = "G436"; else if (typ.equals("c,ua,z")) gr = "G437"; else if (typ.equals("c,ub,z")) gr = "G438"; else if (typ.equals("c,uc,z")) gr = "G439"; else if (typ.equals("c,p,z")) gr = "G440"; else if (typ.equals("c,r,z")) gr = "G441"; else if (typ.equals("c,ro,z")) gr = "G442"; else if (typ.equals("c,ob,z")) gr = "G443"; else if (typ.equals("va,vb,z")) gr = "G444"; else if (typ.equals("va,vc,z")) gr = "G445"; else if (typ.equals("va,ta,z")) gr = "G446"; else if (typ.equals("va,tb,z")) gr = "G447"; else if (typ.equals("va,tc,z")) gr = "G448"; else if (typ.equals("va,ua,z")) gr = "G449"; else if (typ.equals("va,ub,z")) gr = "G450"; else if (typ.equals("va,uc,z")) gr = "G451"; else if (typ.equals("va,p,z")) gr = "G452"; else if (typ.equals("va,r,z")) gr = "G453"; else if (typ.equals("va,ro,z")) gr = "G454"; else if (typ.equals("va,ob,z")) gr = "G455"; else if (typ.equals("vb,vc,z")) gr = "G456"; else if (typ.equals("vb,ta,z")) gr = "G457"; else if (typ.equals("vb,tb,z")) gr = "G458"; else if (typ.equals("vb,tc,z")) gr = "G459"; else if (typ.equals("vb,ua,z")) gr = "G460"; else if (typ.equals("vb,ub,z")) gr = "G461"; else if (typ.equals("vb,uc,z")) gr = "G462"; else if (typ.equals("vb,p,z")) gr = "G463"; else if (typ.equals("vb,r,z")) gr = "G464"; else if (typ.equals("vb,ro,z")) gr = "G465"; else if (typ.equals("vb,ob,z")) gr = "G466"; else if (typ.equals("vc,ta,z")) gr = "G467"; else if (typ.equals("vc,tb,z")) gr = "G468"; else if (typ.equals("vc,tc,z")) gr = "G469"; else if (typ.equals("vc,ua,z")) gr = "G470"; else if (typ.equals("vc,ub,z")) gr = "G471"; else if (typ.equals("vc,p,z")) gr = "G472"; else if (typ.equals("vc,r,z")) gr = "G473"; else if (typ.equals("vc,ro,z")) gr = "G474"; else if (typ.equals("vc,ob,z")) gr = "G475"; else if (typ.equals("ta,tb,z")) gr = "G476"; else if (typ.equals("ta,tc,z")) gr = "G477"; else if (typ.equals("ta,ua,z")) gr = "G478"; else if (typ.equals("ta,ub,z")) gr = "G479"; else if (typ.equals("ta,uc,z")) gr = "G480"; else if (typ.equals("ta,p,z")) gr = "G481"; else if (typ.equals("ta,r,z")) gr = "G482"; else if (typ.equals("ta,ro,z")) gr = "G483"; else if (typ.equals("ta,ob,z")) gr = "G484"; else if (typ.equals("tb,tc,z")) gr = "G485"; else if (typ.equals("tb,ua,z")) gr = "G486"; else if (typ.equals("tb,ub,z")) gr = "G487"; else if (typ.equals("tb,uc,z")) gr = "G488"; else if (typ.equals("tb,p,z")) gr = "G489"; else if (typ.equals("tb,r,z")) gr = "G490"; else if (typ.equals("tb,ro,z")) gr = "G491"; else if (typ.equals("tb,ob,z")) gr = "G492"; else if (typ.equals("tc,ua,z")) gr = "G493"; else if (typ.equals("tc,ub,z")) gr = "G494"; else if (typ.equals("tc,uc,z")) gr = "G495"; else if (typ.equals("tc,p,z")) gr = "G496"; else if (typ.equals("tc,r,z")) gr = "G497"; else if (typ.equals("tc,ro,z")) gr = "G498"; else if (typ.equals("tc,ob,z")) gr = "G499"; else if (typ.equals("ua,ub,z")) gr = "G500"; else if (typ.equals("ua,uc,z")) gr = "G501"; else if (typ.equals("ua,p,z")) gr = "G502"; else if (typ.equals("ua,r,z")) gr = "G503"; else if (typ.equals("ua,ro,z")) gr = "G504"; else if (typ.equals("ua,ob,z")) gr = "G505"; else if (typ.equals("ub,uc,z")) gr = "G506"; else if (typ.equals("ub,p,z")) gr = "G507"; else if (typ.equals("ub,r,z")) gr = "G508"; else if (typ.equals("ub,ro,z")) gr = "G509"; else if (typ.equals("ub,ob,z")) gr = "G510"; else if (typ.equals("uc,p,z")) gr = "G511"; else if (typ.equals("uc,r,z")) gr = "G512"; else if (typ.equals("uc,ro,z")) gr = "G513"; else if (typ.equals("uc,ob,z")) gr = "G514"; else if (typ.equals("p,r,z")) gr = "G515"; else if (typ.equals("p,ro,z")) gr = "G516"; else if (typ.equals("p,ob,z")) gr = "G517"; else if (typ.equals("r,ro,z")) gr = "G518"; else if (typ.equals("r,ob,z")) gr = "G519"; else if (typ.equals("ro,ob,z")) gr = "G520"; else hlaseni("NOGRAPH");
        return gr;
    }

    public void vysledek(double a, double b, double c) {
        double[] r = new double[3];
        r[0] = a;
        r[1] = b;
        r[2] = c;
        if (reseni == null) reseni = new Vector();
        reseni.addElement(r);
    }

    public void vysacb(double a, double b, double c) {
        vysledek(a, b, c);
        vysledek(a, c, b);
    }

    public void vysbac(double a, double b, double c) {
        vysledek(a, b, c);
        vysledek(b, a, c);
    }

    public void vyslbac(boolean znak, double a, double b, double c) {
        if (znak == true) vysledek(a, b, c); else vysledek(b, a, c);
    }

    public void vyslacb(boolean znak, double a, double b, double c) {
        if (znak == true) vysledek(a, b, c); else vysledek(a, c, b);
    }

    public void mala(double a, double va) {
        vysledek(a, Math.sqrt(a * a / 4 + va * va), Math.sqrt(a * a / 4 + va * va));
    }

    public void malux(double al, double va) {
        vysledek(2 * va * Math.tan(al / 2), va / Math.cos(al / 2), va / Math.cos(al / 2));
    }

    public void malcux(double ga, double vc) {
        vysledek(vc / Math.cos(ga / 2), vc / Math.cos(ga / 2), 2 * vc * Math.tan(ga / 2));
    }

    public void macavc(double a, double vc) {
        vysledek(a, a, 2 * Math.sqrt(a * a - vc * vc));
    }

    public void zobrazKomplet(double a, double b, double c) {
        double al, be, ga, alfa, beta, gama, va, vb, vc, ta, tb, tc, ua, ub, uc, p, r, ro, ab, ob, z;
        al = compLib.acos((b * b + c * c - a * a) / 2 / b / c);
        be = compLib.acos((a * a + c * c - b * b) / 2 / a / c);
        ga = Math.PI - al - be;
        alfa = Math.toDegrees(al);
        beta = Math.toDegrees(be);
        gama = Math.toDegrees(ga);
        va = c * Math.sin(be);
        vb = c * Math.sin(al);
        vc = a * Math.sin(be);
        ta = 0.5 * Math.sqrt(2 * (b * b + c * c) - a * a);
        tb = 0.5 * Math.sqrt(2 * (a * a + c * c) - b * b);
        tc = 0.5 * Math.sqrt(2 * (a * a + b * b) - c * c);
        ua = Math.sqrt(b * c * ((b + c) * (b + c) - a * a)) / (b + c);
        ub = Math.sqrt(a * c * ((a + c) * (a + c) - b * b)) / (a + c);
        uc = Math.sqrt(a * b * ((a + b) * (a + b) - c * c)) / (a + b);
        p = a * va / 2;
        r = a / 2 / Math.sin(al);
        ro = 0.5 * (b + c - a) * Math.tan(al / 2);
        ab = a + b;
        ob = a + b + c;
        z = alfa - beta;
        komplet.setKompletValue(a, 0, 1);
        komplet.setKompletValue(b, 0, 2);
        komplet.setKompletValue(c, 0, 3);
        komplet.setKompletValue(alfa, 1, 1);
        komplet.setKompletValue(beta, 1, 2);
        komplet.setKompletValue(gama, 1, 3);
        komplet.setKompletValue(va, 2, 1);
        komplet.setKompletValue(vb, 2, 2);
        komplet.setKompletValue(vc, 2, 3);
        komplet.setKompletValue(ta, 3, 1);
        komplet.setKompletValue(tb, 3, 2);
        komplet.setKompletValue(tc, 3, 3);
        komplet.setKompletValue(ua, 4, 1);
        komplet.setKompletValue(ub, 4, 2);
        komplet.setKompletValue(uc, 4, 3);
        komplet.setKompletValue(p, 5, 1);
        komplet.setKompletValue(r, 6, 1);
        komplet.setKompletValue(ro, 7, 1);
        komplet.setKompletValue(ab, 8, 1);
        komplet.setKompletValue(ob, 9, 1);
        komplet.setKompletValue(z, 10, 1);
        komplet.setKompletLabel("Strana", 0);
        komplet.setKompletLabel("Uhel", 1);
        komplet.setKompletLabel("Vyska", 2);
        komplet.setKompletLabel("Teznice", 3);
        komplet.setKompletLabel("Osa_uhlu", 4);
        komplet.setKompletLabel("Plocha", 5);
        komplet.setKompletLabel("Pol.kr.ops.", 6);
        komplet.setKompletLabel("Pol.kr.veps.", 7);
        komplet.setKompletLabel("Soucet_a_b", 8);
        komplet.setKompletLabel("Obvod", 9);
        komplet.setKompletLabel("Alfa-beta", 10);
    }

    public void zadaniHlaseni(String zprava) {
        zadani.zadaniHlaseni(compLib.localize(zprava));
    }

    public void vypisHlaseni(String zprava) {
        zadani.vypisHlaseni(zprava);
    }

    private String setHlasValue(double p2) {
        return compLib.doubleToString(p2, 6);
    }

    public void hlaseni(String zprava) {
        vypisHlaseni("  " + compLib.localize(zprava));
    }

    public void chyba(String pstr, String op, double p2) {
        vypisHlaseni("  " + pstr + " " + op + " " + setHlasValue(p2));
    }

    public void chyba1(String vztah) {
        vypisHlaseni("  " + compLib.localize("AMBIGOUS") + vztah);
    }

    public void chyba2(String angle, String op, double p2) {
        vypisHlaseni("  " + compLib.localize(angle) + " " + op + " " + setHlasValue(p2));
    }

    public void chyba3(String pstr, String op, double p2, String op1, double p3) {
        vypisHlaseni("  " + pstr + " " + op + " " + setHlasValue(p2) + "   &&   " + pstr + " " + op1 + " " + setHlasValue(p3));
    }
}
