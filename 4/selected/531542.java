package org.fudaa.dodico.mesure;

import gnu.trove.TDoubleArrayList;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.fudaa.ctulu.CsvWriter;
import org.fudaa.dodico.common.TestIO;
import org.fudaa.dodico.commun.DodicoArrayList;
import org.fudaa.dodico.fichiers.CsvDoubleReader;
import org.fudaa.dodico.mesure.DodicoCsvReader;
import org.fudaa.dodico.mesure.EvolutionFileFormat;
import org.fudaa.dodico.mesure.EvolutionReguliere;
import org.fudaa.dodico.mesure.EvolutionReguliereInterface;

/**
 * @author deniger
 * @version $Id: TestJEvolution.java,v 1.6 2007-05-22 13:11:26 deniger Exp $
 */
public class TestJEvolution extends TestIO {

    EvolutionReguliere evol_;

    /**
   *
   */
    public TestJEvolution() {
        super("courbes.txt");
        init();
    }

    private void init() {
        evol_ = new EvolutionReguliere();
        evol_.add(3, 3);
        evol_.add(1, 3);
        evol_.add(13, 23);
    }

    public void testJCsvWriter() {
        File f = null;
        try {
            f = File.createTempFile("fudaaTest", ".csv");
        } catch (IOException e) {
            e.printStackTrace();
        }
        assertNotNull(f);
        double[][] tests = new double[15][20];
        for (int i = tests.length - 1; i >= 0; i--) {
            for (int j = tests[i].length - 1; j >= 0; j--) {
                tests[i][j] = Math.random();
            }
        }
        CsvWriter writer = null;
        try {
            writer = new CsvWriter(f);
            for (int i = 0; i < tests.length; i++) {
                for (int j = 0; j < tests[i].length; j++) {
                    writer.appendDouble(tests[i][j]);
                }
                writer.newLine();
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            }
        }
        try {
            CsvDoubleReader reader = new CsvDoubleReader(new FileReader(f));
            if (writer != null) {
                reader.setSep(String.valueOf(writer.getSepChar()));
            }
            TDoubleArrayList l = new TDoubleArrayList();
            int idx = 0;
            while (reader.readLine(l)) {
                assertTrue(Arrays.equals(l.toNativeArray(), tests[idx++]));
            }
            reader.close();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
        if (f != null) f.delete();
    }

    /**
   * Test DodicoArrayList.
   */
    public void testDodicoArrayList() {
        int nInit = 10;
        DodicoArrayList l = new DodicoArrayList(nInit);
        for (int i = 0; i < nInit; i++) {
            l.add(Integer.toString(i));
        }
        for (int i = 0; i < nInit; i++) {
            assertEquals(Integer.toString(i), l.get(i));
        }
        assertEquals(nInit, l.size());
        DodicoArrayList lInit = new DodicoArrayList(l);
        lInit.setSize(nInit);
        assertEquals(nInit, lInit.size());
        for (int i = 0; i < nInit; i++) {
            assertEquals(Integer.toString(i), lInit.get(i));
        }
        int n = 0;
        DodicoArrayList l0 = new DodicoArrayList(l);
        l0.setSize(n);
        assertEquals(n, l0.size());
        n = 5;
        DodicoArrayList l5 = new DodicoArrayList(l);
        l5.setSize(n);
        assertEquals(n, l5.size());
        for (int i = 0; i < n; i++) {
            assertEquals(Integer.toString(i), l5.get(i));
        }
        n = 15;
        DodicoArrayList l15 = new DodicoArrayList(l);
        l15.setSize(n);
        assertEquals(n, l15.size());
        for (int i = 0; i < nInit; i++) {
            assertEquals(Integer.toString(i), l15.get(i));
        }
        for (int i = nInit; i < n; i++) {
            assertEquals(null, l15.get(i));
        }
    }

    /**
   * Test si les bonnes valeurs sont presentes.
   */
    public void testInit() {
        assertEquals(evol_.getX(0), 1, eps_);
        assertEquals(evol_.getY(0), 3, eps_);
        assertEquals(evol_.getX(1), 3, eps_);
        assertEquals(evol_.getY(1), 3, eps_);
        assertEquals(evol_.getX(2), 13, eps_);
        assertEquals(evol_.getY(2), 23, eps_);
    }

    /**
   * test de l'interpolation lin�aire.
   */
    public void testInterpo() {
        assertEquals(false, evol_.isInclude(0));
        assertEquals(3, evol_.getInterpolateYValueFor(0), eps_);
        assertEquals(false, evol_.isInclude(24));
        assertEquals(evol_.getInterpolateYValueFor(24), 23, eps_);
        assertEquals(evol_.isInclude(13), true);
        assertEquals(evol_.getInterpolateYValueFor(13), 23, eps_);
        assertEquals(evol_.isInclude(1), true);
        assertEquals(evol_.getInterpolateYValueFor(1), 3, eps_);
        assertEquals(evol_.isInclude(2), true);
        assertEquals(evol_.getInterpolateYValueFor(2), 3, eps_);
        assertEquals(evol_.isInclude(8), true);
        assertEquals(evol_.getInterpolateYValueFor(8), 13, eps_);
        assertEquals(evol_.isInclude(10.5), true);
        assertEquals(evol_.getInterpolateYValueFor(10.5), 18, eps_);
    }

    /**
   * Test creation evolution.
   */
    public void testCreateEvolution() {
        double[] t = new double[] { 0, 1, 2, 3, 4, 5, 13, 100 };
        EvolutionReguliereInterface ev = evol_.createEvolutionFromInterpolation(t);
        assertEquals(ev.getNbValues(), t.length);
        for (int i = t.length - 1; i >= 0; i--) {
            assertEquals(t[i], ev.getX(i), eps_);
        }
        assertEquals(ev.getY(0), 3, eps_);
        assertEquals(ev.getY(1), 3, eps_);
        assertEquals(ev.getY(2), 3, eps_);
        assertEquals(ev.getY(3), 3, eps_);
        assertEquals(ev.getY(4), 5, eps_);
        assertEquals(ev.getY(5), 7, eps_);
        assertEquals(ev.getY(6), 23, eps_);
        assertEquals(ev.getY(7), 23, eps_);
    }

    /**
   * Ne fait rien.
   */
    public void testEcriture() {
    }

    /**
   * Lit courbrefortran.txt.
   */
    public void testLecture() {
        DodicoCsvReader e = new DodicoCsvReader();
        e.setFile(fic_);
        Map options = new HashMap();
        options.put("SEP", ";");
        EvolutionReguliereInterface[] evols = (EvolutionReguliereInterface[]) EvolutionFileFormat.getInstance().readEvolutions(fic_, null, options).getSource();
        assertNotNull(evols);
        assertEquals(3, evols.length);
        EvolutionReguliereInterface evol = evols[0];
        assertNotNull(evol);
        assertEquals(4, evol.getNbValues());
        for (int i = 0; i < 3; i++) {
            assertEquals(1d, evols[i].getX(0), eps_);
            assertEquals(2d, evols[i].getX(1), eps_);
            assertEquals(3d, evols[i].getX(2), eps_);
            assertEquals(4d, evols[i].getX(3), eps_);
            assertEquals(3d + i, evols[i].getY(0), eps_);
            assertEquals(4d + i, evols[i].getY(1), eps_);
            assertEquals(5d + i, evols[i].getY(2), eps_);
            assertEquals(10d + i, evols[i].getY(3), eps_);
        }
        DodicoCsvReader evolReader = new DodicoCsvReader();
        evolReader.setNumeric(true);
        evolReader.setFortranFormat(new int[] { 3, 4 });
        String[][] res = (String[][]) evolReader.read(getFile("courbesFortran.txt"), null).getSource();
        assertNotNull(res);
        assertEquals(2, res.length);
        assertEquals(5, res[0].length);
        for (int i = 1; i < 5; i++) {
            double x = Double.parseDouble(res[0][i - 1]);
            assertEquals(i * 100, x, eps_);
            x = Double.parseDouble(res[1][i - 1]);
            assertEquals(230 + (i - 1) * 200, x, eps_);
        }
    }
}
