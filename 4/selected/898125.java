package ppa1_cviceni;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import javax.swing.JOptionPane;
import javax.xml.bind.JAXBElement;
import shodne.RedukceZdrojovychTextu;
import zdrojeJAXB.CviceniType;
import zdrojeJAXB.DomaciUlohyType;
import zdrojeJAXB.NalezenaShodnostType;
import zdrojeJAXB.ObjectFactory;
import zdrojeJAXB.OdevzdanoType;
import zdrojeJAXB.Ppa1VysledkyCviceniType;
import zdrojeJAXB.ShodneSType;
import zdrojeJAXB.StudentType;
import zdrojeJAXB.UlohaType;

/**
 * Filtrov�n� p��pon
 */
class ExtensionFilter implements FilenameFilter {

    private String extension;

    public ExtensionFilter(final String extension) {
        this.extension = extension;
    }

    public boolean accept(final File dir, final String name) {
        return (name.endsWith(extension));
    }
}

/**
 * T��da s funkcemi volan�mi z GUI. V�echny metody t��dy jsou statick�, proto�e
 * nebudeme vytv��et dynamickou instanci t��dy
 * 
 * @author Tom� Kohlsch�tter
 * 
 */
public class Utils {

    private static final String SLOVNIK = Ppa1Cviceni.POMOCNE_PROGRAMY + File.separator + "shodnost" + File.separator + "slovnik.txt";

    private static List<StudentType> students;

    private static HashMap<String, String[]> buffer;

    /**
	 * Ov��� shodnost samostatn�ch prac�
	 * 
	 * @param uroven
	 *            ud�v� �rove� testu shodnosti
	 */
    public static void overShodnost(final int uroven) {
        LogManager.getInstance().log("Prob�h� ov��ov�n� shodnosti...");
        RedukceZdrojovychTextu.nacteniSlovniku(SLOVNIK);
        final File dir = new File(Ppa1Cviceni.VALIDOVANE + File.separator);
        final String[] list = dir.list();
        final JAXBElement<?> element = ElementJAXB.getJAXBElement();
        final Ppa1VysledkyCviceniType pvct = (Ppa1VysledkyCviceniType) element.getValue();
        students = pvct.getStudent();
        for (final String directory : list) {
            shodnostCviceni(Ppa1Cviceni.VALIDOVANE + File.separator + directory + File.separator, uroven);
        }
        ElementJAXB.setJAXBElement(element);
        LogManager.getInstance().log("Ov��ov�n� shodnosti dokon�eno.");
    }

    public static void overStudenta(final String osobniCislo, int uroven) {
        LogManager.getInstance().log("Ov��uji opisov�n� u studenta s ��slem: " + osobniCislo);
        RedukceZdrojovychTextu.nacteniSlovniku(SLOVNIK);
        final File dir = new File(Ppa1Cviceni.VALIDOVANE + File.separator);
        final String[] slozky = dir.list();
        final GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeInMillis(System.currentTimeMillis());
        String nazevSouboru = String.format("%s_u%d_%tF.txt", osobniCislo, uroven, cal);
        try {
            PrintStream ps = new PrintStream(new File(Ppa1Cviceni.SHODNE + File.separator + nazevSouboru));
            for (String slozka : slozky) {
                String adresar = Ppa1Cviceni.VALIDOVANE + File.separator + slozka + File.separator;
                File file = new File(adresar);
                if (file.isHidden()) continue;
                final String[] list = file.list();
                for (final String soubor1 : list) {
                    if (soubor1.split("_")[2].equals("0")) {
                        continue;
                    }
                    final String osobni_cislo = soubor1.split("[._]")[3];
                    if (osobni_cislo.equals(osobniCislo)) {
                        final RedukceZdrojovychTextu rzt1 = new RedukceZdrojovychTextu(adresar + soubor1);
                        String s1 = rzt1.uprava(uroven);
                        for (final String soubor2 : list) {
                            if (soubor2.equals(soubor1) || !soubor1.split("_")[2].equals(soubor2.split("_")[2])) {
                                continue;
                            }
                            if (soubor1.split("_")[2].equals("0")) {
                                continue;
                            }
                            final RedukceZdrojovychTextu rzt2 = new RedukceZdrojovychTextu(adresar + soubor2);
                            String s2 = rzt2.uprava(uroven);
                            if (s1.equals(s2)) {
                                ps.printf("%s = %s%s", soubor1, soubor2, System.getProperty("line.separator"));
                            }
                        }
                    }
                }
            }
            ps.close();
            LogManager.getInstance().log("Ov��ov�n� dokon�eno. V�sledky ulo�eny do shodne-java" + File.separator + nazevSouboru);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    /**
	 *	Na�te redukovan� zdrojov� texty do pam�ti. 
	 */
    private static HashMap<String, String[]> prefetchData(final String[] list, final String directory, final int uroven) {
        LogManager.getInstance().log("Na��t�m do pam�ti adres�� " + directory.substring(Ppa1Cviceni.USER_DIR.length()) + "...");
        final HashMap<String, String[]> buffer = new HashMap<String, String[]>();
        for (final String soubor : list) {
            if (!soubor.split("_")[2].equals("0")) {
                final String[] poleUprav = new String[uroven == -1 ? 3 : 1];
                final RedukceZdrojovychTextu rzt = new RedukceZdrojovychTextu(directory + soubor);
                if (uroven == -1) {
                    poleUprav[0] = rzt.uprava(1);
                    poleUprav[1] = rzt.uprava(2);
                    poleUprav[2] = rzt.uprava(3);
                } else {
                    poleUprav[0] = rzt.uprava(uroven);
                }
                buffer.put(soubor, poleUprav);
            }
        }
        return buffer;
    }

    /**
	 * Ov��� shodnost prac� ze cvi�en� ve slo�ce d
	 * 
	 * @param directory
	 *            slo�ka kter� se bude prov��ovat
	 * @param uroven
	 *            jakou �rovn� se bude test shodnosti prov�d�t
	 */
    private static void shodnostCviceni(final String directory, final int uroven) {
        final File dir = new File(directory);
        final String[] list = dir.list();
        if (dir.isHidden() || (list.length == 0)) {
            return;
        }
        buffer = prefetchData(list, directory, uroven);
        LogManager.getInstance().log("Ov��uji shodnost v adres��i  " + directory.substring(Ppa1Cviceni.USER_DIR.length()) + "...");
        LogManager.getInstance().log("");
        for (final String soubor : list) {
            testStudenta(soubor, directory, uroven, list);
        }
    }

    private static void testStudenta(final String soubor1, final String directory, final int uroven, final String[] list) {
        final String[] s1 = buffer.get(soubor1);
        if (s1 != null) {
            LogManager.getInstance().changeLog("Ov��uji soubor: " + soubor1);
            buffer.remove(soubor1);
            for (final String soubor2 : list) {
                if (!soubor1.split("_")[2].equals(soubor2.split("_")[2])) {
                    continue;
                }
                final String[] s2 = buffer.get(soubor2);
                if (s2 != null) {
                    if (s1[0].equals(s2[0])) {
                        nalezenaShodnost(soubor1, soubor2, directory);
                        nalezenaShodnost(soubor2, soubor1, directory);
                    } else if (uroven == -1) {
                        testShodnostiVMinulosti(soubor1, soubor2, directory, s1, s2);
                        testShodnostiVMinulosti(soubor2, soubor1, directory, s1, s2);
                    }
                }
            }
        }
    }

    /**
	 * Pokud student u� v minulosti opisoval, tak ho otestuje �rovn� 2, pop�. 3.
	 * @param fileName	n�zev opsan�ho souboru
	 * @param ShodneS	student, se kter�m je pr�ce shodn�
	 * @param directory	prohled�van� adres��
	 * @param s1		redukovan� zdrojov� text opsan�ho souboru
	 * @param s2		redukovan� zdrojov� text shodn�ho souboru
	 */
    private static void testShodnostiVMinulosti(final String fileName, final String ShodneS, final String directory, String[] s1, String[] s2) {
        final StudentType student = findStudent(fileName);
        if (student != null) {
            final DomaciUlohyType dut = student.getDomaciUlohy();
            final NalezenaShodnostType nst = dut.getNalezenaShodnost();
            if (nst.isVysledek() == true) {
                if (s1[1].equals(s2[1]) || s1[2].equals(s2[2])) {
                    nalezenaShodnost(fileName, ShodneS, directory);
                }
            }
        }
    }

    /**
	 * Nastav� p��slu�n� �daje o tom, �e student opisoval.
	 * 
	 * @param fileName	n�zev opsan�ho souboru
	 * @param directory	adres��, ve kter�m se prov�d� kontrola
	 * @param ShodneS	soubor je shodn� se studentem, kter� m� toto osobn� ��slo
	 */
    private static void nalezenaShodnost(final String fileName, final String ShodneS, final String directory) {
        try {
            final GregorianCalendar cal = new GregorianCalendar();
            cal.setTimeInMillis(System.currentTimeMillis());
            final String path = String.format("%s%tF%s%s", Ppa1Cviceni.SHODNE + File.separator, cal, directory.substring(Ppa1Cviceni.VALIDOVANE.length()), fileName);
            FileUtils.copyFile(new File(directory + fileName), new File(path));
        } catch (final IOException e) {
            JOptionPane.showMessageDialog(null, e.getLocalizedMessage());
        }
        final StudentType student = findStudent(fileName);
        if (student != null) {
            final DomaciUlohyType dut = student.getDomaciUlohy();
            final NalezenaShodnostType nst = dut.getNalezenaShodnost();
            nst.setVysledek(true);
            final String[] nazevSouboru = fileName.split("_");
            CviceniType cviceni = null;
            for (final CviceniType cv : dut.getCviceni()) {
                if (cv.getCislo().intValue() == Integer.parseInt(nazevSouboru[1].substring(2))) {
                    cviceni = cv;
                    break;
                }
            }
            for (final UlohaType ut : cviceni.getUloha()) {
                if (ut.getCislo().intValue() == Integer.parseInt(nazevSouboru[2])) {
                    final List<OdevzdanoType> odevzdane = ut.getOdevzdano();
                    int last = 0;
                    OdevzdanoType ot = null;
                    do {
                        ot = odevzdane.get(last);
                        last++;
                    } while ((!odevzdane.get(last - 1).getValidator().isVysledek()) && last < odevzdane.size());
                    if (!ot.getValidator().isVysledek()) {
                        continue;
                    }
                    HashSet<String> mnozinaOsobnichCiselOpisujicich = new HashSet<String>();
                    List<ShodneSType> shodneSTypeList = ot.getShodneS();
                    for (ShodneSType sst : shodneSTypeList) {
                        if (!mnozinaOsobnichCiselOpisujicich.contains(sst.getOsCislo())) {
                            mnozinaOsobnichCiselOpisujicich.add(sst.getOsCislo());
                        }
                    }
                    ObjectFactory of = new ObjectFactory();
                    final ShodneSType shodny = of.createShodneSType();
                    shodny.setOsCislo(ShodneS.split("[._]")[3]);
                    if (!mnozinaOsobnichCiselOpisujicich.contains(shodny.getOsCislo())) {
                        ot.getShodneS().add(shodny);
                    }
                    break;
                }
            }
        }
    }

    private static StudentType findStudent(final String fileName) {
        final String osobni_cislo = fileName.split("[._]")[3];
        for (final StudentType st : students) {
            if (st.getOsobniCislo().equals(osobni_cislo)) {
                return st;
            }
        }
        return null;
    }

    public static void XSLTTransformation(final String xsl_soubor, final String vystupniAdresar, final String datum) {
        try {
            final History h = new History();
            final File xmlFile = new File(Ppa1Cviceni.VYSLEDKY + File.separator + "vysledky_" + h.lastEntery() + ".xml");
            final File xsltFile = new File(Ppa1Cviceni.POMOCNE_PROGRAMY + File.separator + "html" + File.separator + xsl_soubor);
            LogManager.getInstance().log("Prob�h� vytv��en� HTML soubor� pomoc� transforma�n�ho XSL ...");
            net.sf.saxon.Transform.main(new String[] { "-o", (new File(Ppa1Cviceni.HTML + File.separator + datum + File.separator + "vysledky_vse.html")).toString(), xmlFile.toString(), xsltFile.toString(), "vystupniAdresar=file:///" + vystupniAdresar, "zobrazitJmena=" + Konfigurace.getInstance().getZobrazitJmena(), "zobrazitJmenaShodnych=" + Konfigurace.getInstance().getZobrazitJmenaShodnych() });
            LogManager.getInstance().changeLog("Prob�h� vytv��en� HTML soubor� pomoc� transforma�n�ho XSL ... OK");
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
