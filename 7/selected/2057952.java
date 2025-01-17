package org.jmol.adapter.readers.simple;

import java.util.BitSet;
import org.jmol.adapter.smarter.*;
import org.jmol.util.Logger;
import org.jmol.util.Parser;

/**
 * Reads Mopac 93, 6, 7, 2002, or 2009 output files
 *
 * @author Egon Willighagen <egonw@jmol.org>
 */
public class MopacReader extends AtomSetCollectionReader {

    private int baseAtomIndex;

    private boolean chargesFound = false;

    private boolean haveHeader;

    private int mopacVersion;

    @Override
    protected void initializeReader() throws Exception {
        while (mopacVersion == 0) {
            discardLinesUntilContains("MOPAC");
            if (line.indexOf("2009") >= 0) mopacVersion = 2009; else if (line.indexOf("6.") >= 0) mopacVersion = 6; else if (line.indexOf("7.") >= 0) mopacVersion = 7; else if (line.indexOf("93") >= 0) mopacVersion = 93; else if (line.indexOf("2002") >= 0) mopacVersion = 2002;
        }
        Logger.info("MOPAC version " + mopacVersion);
    }

    @Override
    protected boolean checkLine() throws Exception {
        if (!haveHeader) {
            if (line.trim().equals("CARTESIAN COORDINATES")) {
                processCoordinates();
                atomSetCollection.setAtomSetName("Input Structure");
                return true;
            }
            haveHeader = line.startsWith(" ---");
            return true;
        }
        if (line.indexOf("TOTAL ENERGY") >= 0) {
            processTotalEnergy();
            return true;
        }
        if (line.indexOf("ATOMIC CHARGES") >= 0) {
            processAtomicCharges();
            return true;
        }
        if (line.trim().equals("CARTESIAN COORDINATES")) {
            processCoordinates();
            return true;
        }
        if (line.indexOf("ORIENTATION OF MOLECULE IN FORCE") >= 0) {
            processCoordinates();
            atomSetCollection.setAtomSetName("Orientation in Force Field");
            return true;
        }
        if (line.indexOf("NORMAL COORDINATE ANALYSIS") >= 0) {
            readFrequencies();
            return true;
        }
        return true;
    }

    void processTotalEnergy() {
    }

    /**
   * Reads the section in MOPAC files with atomic charges.
   * These sections look like:
   * <pre>
   *               NET ATOMIC CHARGES AND DIPOLE CONTRIBUTIONS
   * 
   *          ATOM NO.   TYPE          CHARGE        ATOM  ELECTRON DENSITY
   *            1          C          -0.077432        4.0774
   *            2          C          -0.111917        4.1119
   *            3          C           0.092081        3.9079
   * </pre>
   * They are expected to be found in the file <i>before</i> the 
   * cartesian coordinate section.
   * 
   * @throws Exception
   */
    void processAtomicCharges() throws Exception {
        readLines(2);
        atomSetCollection.newAtomSet();
        baseAtomIndex = atomSetCollection.getAtomCount();
        int expectedAtomNumber = 0;
        while (readLine() != null) {
            int atomNumber = parseInt(line);
            if (atomNumber == Integer.MIN_VALUE) break;
            ++expectedAtomNumber;
            if (atomNumber != expectedAtomNumber) throw new Exception("unexpected atom number in atomic charges");
            Atom atom = atomSetCollection.addNewAtom();
            atom.elementSymbol = parseToken();
            atom.partialCharge = parseFloat();
        }
        chargesFound = true;
    }

    /**
   * Reads the section in MOPAC files with cartesian coordinates.
   * These sections look like:
   * <pre>
   *           CARTESIAN COORDINATES
   * 
   *     NO.       ATOM         X         Y         Z
   * 
   *      1         C        0.0000    0.0000    0.0000
   *      2         C        1.3952    0.0000    0.0000
   *      3         C        2.0927    1.2078    0.0000
   * </pre>
   * In a MOPAC2002 file the columns are different:
   * <pre>
   *          CARTESIAN COORDINATES
   *
   * NO.       ATOM           X             Y             Z
   *
   *  1         H        0.00000000    0.00000000    0.00000000
   *  2         O        0.95094500    0.00000000    0.00000000
   *  3         H        1.23995160    0.90598439    0.00000000
   * </pre>
   * 
   * @throws Exception
   */
    void processCoordinates() throws Exception {
        readLines(3);
        int expectedAtomNumber = 0;
        if (!chargesFound) {
            atomSetCollection.newAtomSet();
            baseAtomIndex = atomSetCollection.getAtomCount();
        } else {
            chargesFound = false;
        }
        Atom[] atoms = atomSetCollection.getAtoms();
        while (readLine() != null) {
            int atomNumber = parseInt(line);
            if (atomNumber == Integer.MIN_VALUE) break;
            ++expectedAtomNumber;
            if (atomNumber != expectedAtomNumber) throw new Exception("unexpected atom number in coordinates");
            String elementSymbol = parseToken();
            Atom atom = atoms[baseAtomIndex + atomNumber - 1];
            if (atom == null) {
                atom = atomSetCollection.addNewAtom();
            }
            atom.atomSerial = atomNumber;
            setAtomCoord(atom, parseFloat(), parseFloat(), parseFloat());
            int atno = parseInt(elementSymbol);
            if (atno != Integer.MIN_VALUE) elementSymbol = getElementSymbol(atno);
            atom.elementSymbol = elementSymbol;
        }
    }

    /**
   * Interprets the Harmonic frequencies section.
   * 
   * <pre>
   *     THE LAST 6 VIBRATIONS ARE THE TRANSLATION AND ROTATION MODES
   *    THE FIRST THREE OF THESE BEING TRANSLATIONS IN X, Y, AND Z, RESPECTIVELY
   *              NORMAL COORDINATE ANALYSIS
   *   
   *       ROOT NO.    1           2           3           4           5           6
   *   
   *              370.51248   370.82204   618.03031   647.68700   647.74806   744.32662
   *     
   *            1   0.00002     0.00001    -0.00002    -0.05890     0.07204    -0.00002
   *            2   0.00001    -0.00006    -0.00001     0.01860     0.13517     0.00000
   *            3   0.00421    -0.11112     0.06838    -0.00002    -0.00003    -0.02449
   *   
   *            4   0.00002     0.00001    -0.00002    -0.04779     0.07977    -0.00001
   *            5  -0.00002     0.00002     0.00001     0.13405    -0.02908     0.00004
   *            6  -0.10448     0.05212    -0.06842    -0.00005    -0.00002    -0.02447
   * </pre>
   * 
   * <p>
   * The vectors are added to a clone of the last read AtomSet. Only the
   * Frequencies are set as properties for each of the frequency type AtomSet
   * generated.
   * 
   * @throws Exception
   *             If an I/O error occurs
   */
    private void readFrequencies() throws Exception {
        BitSet bsOK = new BitSet();
        int n0 = atomSetCollection.getCurrentAtomSetIndex() + 1;
        String[] tokens;
        boolean done = false;
        while (!done && readLine() != null && line.indexOf("DESCRIPTION") < 0 && line.indexOf("MASS-WEIGHTED") < 0) if (line.toUpperCase().indexOf("ROOT") >= 0) {
            discardLinesUntilNonBlank();
            tokens = getTokens();
            if (Float.isNaN(Parser.parseFloatStrict(tokens[tokens.length - 1]))) {
                discardLinesUntilNonBlank();
                tokens = getTokens();
            }
            int frequencyCount = tokens.length;
            readLine();
            int iAtom0 = atomSetCollection.getAtomCount();
            int atomCount = atomSetCollection.getLastAtomSetAtomCount();
            boolean[] ignore = new boolean[frequencyCount];
            for (int i = 0; i < frequencyCount; ++i) {
                ignore[i] = done || (done = Parser.parseFloatStrict(tokens[i]) < 1) || !doGetVibration(++vibrationNumber);
                if (ignore[i]) continue;
                bsOK.set(vibrationNumber - 1);
                atomSetCollection.cloneLastAtomSet();
            }
            fillFrequencyData(iAtom0, atomCount, atomCount, ignore, false, 0, 0, null);
        }
        String[][] info = new String[vibrationNumber][];
        if (line.indexOf("DESCRIPTION") < 0) discardLinesUntilContains("DESCRIPTION");
        while (discardLinesUntilContains("VIBRATION") != null) {
            tokens = getTokens();
            int freqNo = parseInt(tokens[1]);
            tokens[0] = getTokens(readLine())[1];
            if (tokens[2].equals("ATOM")) tokens[2] = null;
            info[freqNo - 1] = tokens;
            if (freqNo == vibrationNumber) break;
        }
        for (int i = vibrationNumber - 1; --i >= 0; ) if (info[i] == null) info[i] = info[i + 1];
        for (int i = 0, n = n0; i < vibrationNumber; i++) {
            if (!bsOK.get(i)) continue;
            atomSetCollection.setCurrentAtomSetIndex(n++);
            atomSetCollection.setAtomSetFrequency(null, info[i][2], info[i][0], null);
        }
    }
}
