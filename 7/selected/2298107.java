package org.jmol.viewer;

import org.jmol.g3d.Graphics3D;
import org.jmol.bspt.Tuple;
import java.util.Hashtable;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Point3i;

final class Atom implements Tuple {

    static final byte VISIBLE_FLAG = 0x01;

    static final byte VIBRATION_VECTOR_FLAG = 0x02;

    static final byte IS_HETERO_FLAG = 0x04;

    Group group;

    int atomIndex;

    Point3f point3f;

    int screenX;

    int screenY;

    int screenZ;

    short screenDiameter;

    short modelIndex;

    byte elementNumber;

    byte formalChargeAndFlags;

    byte alternateLocationID;

    short madAtom;

    short colixAtom;

    Bond[] bonds;

    Atom(Viewer viewer, Frame frame, int modelIndex, int atomIndex, byte elementNumber, String atomName, int formalCharge, float partialCharge, int occupancy, float bfactor, float x, float y, float z, boolean isHetero, int atomSerial, char chainID, float vibrationX, float vibrationY, float vibrationZ, char alternateLocationID, Object clientAtomReference) {
        this.modelIndex = (short) modelIndex;
        this.atomIndex = atomIndex;
        this.elementNumber = elementNumber;
        this.formalChargeAndFlags = (byte) (formalCharge << 3);
        this.colixAtom = viewer.getColixAtom(this);
        this.alternateLocationID = (byte) alternateLocationID;
        setMadAtom(viewer.getMadAtom());
        this.point3f = new Point3f(x, y, z);
        if (isHetero) formalChargeAndFlags |= IS_HETERO_FLAG;
        if (atomName != null) {
            if (frame.atomNames == null) frame.atomNames = new String[frame.atoms.length];
            frame.atomNames[atomIndex] = atomName.intern();
        }
        byte specialAtomID = lookupSpecialAtomID(atomName);
        if (specialAtomID != 0) {
            if (frame.specialAtomIDs == null) frame.specialAtomIDs = new byte[frame.atoms.length];
            frame.specialAtomIDs[atomIndex] = specialAtomID;
        }
        if (occupancy < 0) occupancy = 0; else if (occupancy > 100) occupancy = 100;
        if (occupancy != 100) {
            if (frame.occupancies == null) frame.occupancies = new byte[frame.atoms.length];
            frame.occupancies[atomIndex] = (byte) occupancy;
        }
        if (atomSerial != Integer.MIN_VALUE) {
            if (frame.atomSerials == null) frame.atomSerials = new int[frame.atoms.length];
            frame.atomSerials[atomIndex] = atomSerial;
        }
        if (!Float.isNaN(partialCharge)) {
            if (frame.partialCharges == null) frame.partialCharges = new float[frame.atoms.length];
            frame.partialCharges[atomIndex] = partialCharge;
        }
        if (!Float.isNaN(bfactor) && bfactor != 0) {
            if (frame.bfactor100s == null) frame.bfactor100s = new short[frame.atoms.length];
            frame.bfactor100s[atomIndex] = (short) (bfactor * 100);
        }
        if (!Float.isNaN(vibrationX) && !Float.isNaN(vibrationY) && !Float.isNaN(vibrationZ)) {
            if (frame.vibrationVectors == null) frame.vibrationVectors = new Vector3f[frame.atoms.length];
            frame.vibrationVectors[atomIndex] = new Vector3f(vibrationX, vibrationY, vibrationZ);
            formalChargeAndFlags |= VIBRATION_VECTOR_FLAG;
        }
        if (clientAtomReference != null) {
            if (frame.clientAtomReferences == null) frame.clientAtomReferences = new Object[frame.atoms.length];
            frame.clientAtomReferences[atomIndex] = clientAtomReference;
        }
    }

    void setGroup(Group group) {
        this.group = group;
    }

    boolean isBonded(Atom atomOther) {
        return getBond(atomOther) != null;
    }

    Bond getBond(Atom atomOther) {
        if (bonds != null) for (int i = bonds.length; --i >= 0; ) {
            Bond bond = bonds[i];
            if ((bond.atom1 == atomOther) || (bond.atom2 == atomOther)) return bond;
        }
        return null;
    }

    Bond bondMutually(Atom atomOther, short order, Frame frame) {
        if (isBonded(atomOther)) return null;
        Bond bond = new Bond(this, atomOther, order, frame);
        addBond(bond, frame);
        atomOther.addBond(bond, frame);
        return bond;
    }

    private void addBond(Bond bond, Frame frame) {
        if (bonds == null) {
            bonds = new Bond[1];
            bonds[0] = bond;
        } else {
            bonds = frame.addToBonds(bond, bonds);
        }
    }

    void deleteBondedAtom(Atom atomToDelete) {
        if (bonds == null) return;
        for (int i = bonds.length; --i >= 0; ) {
            Bond bond = bonds[i];
            Atom atomBonded = (bond.atom1 != this) ? bond.atom1 : bond.atom2;
            if (atomBonded == atomToDelete) {
                deleteBond(i);
                return;
            }
        }
    }

    void deleteAllBonds() {
        if (bonds == null) return;
        for (int i = bonds.length; --i >= 0; ) group.chain.frame.deleteBond(bonds[i]);
        if (bonds != null) {
            System.out.println("bond delete error");
            throw new NullPointerException();
        }
    }

    void deleteBond(Bond bond) {
        for (int i = bonds.length; --i >= 0; ) if (bonds[i] == bond) {
            deleteBond(i);
            return;
        }
    }

    void deleteBond(int i) {
        int newLength = bonds.length - 1;
        if (newLength == 0) {
            bonds = null;
            return;
        }
        Bond[] bondsNew = new Bond[newLength];
        int j = 0;
        for (; j < i; ++j) bondsNew[j] = bonds[j];
        for (; j < newLength; ++j) bondsNew[j] = bonds[j + 1];
        bonds = bondsNew;
    }

    void clearBonds() {
        bonds = null;
    }

    int getBondedAtomIndex(int bondIndex) {
        Bond bond = bonds[bondIndex];
        return (((bond.atom1 == this) ? bond.atom2 : bond.atom1).atomIndex & 0xFFFF);
    }

    void setMadAtom(short madAtom) {
        if (this.madAtom == JmolConstants.MAR_DELETED) return;
        this.madAtom = convertEncodedMad(madAtom);
    }

    short convertEncodedMad(int size) {
        if (size == -1000) {
            int diameter = getBfactor100() * 10 * 2;
            if (diameter > 4000) diameter = 4000;
            size = diameter;
        } else if (size == -1001) size = (getBondingMar() * 2); else if (size < 0) {
            size = -size;
            if (size > 200) size = 200;
            size = (size * getVanderwaalsMar() / 50);
        }
        return (short) size;
    }

    int getRasMolRadius() {
        if (madAtom == JmolConstants.MAR_DELETED) return 0;
        return madAtom / (4 * 2);
    }

    int getCovalentBondCount() {
        if (bonds == null) return 0;
        int n = 0;
        for (int i = bonds.length; --i >= 0; ) if ((bonds[i].order & JmolConstants.BOND_COVALENT_MASK) != 0) ++n;
        return n;
    }

    int getHbondCount() {
        if (bonds == null) return 0;
        int n = 0;
        for (int i = bonds.length; --i >= 0; ) if ((bonds[i].order & JmolConstants.BOND_HYDROGEN_MASK) != 0) ++n;
        return n;
    }

    Bond[] getBonds() {
        return bonds;
    }

    void setColixAtom(short colixAtom) {
        if (colixAtom == 0) colixAtom = group.chain.frame.viewer.getColixAtomPalette(this, "cpk");
        this.colixAtom = colixAtom;
    }

    void setTranslucent(boolean isTranslucent) {
        colixAtom = Graphics3D.setTranslucent(colixAtom, isTranslucent);
    }

    Vector3f getVibrationVector() {
        Vector3f[] vibrationVectors = group.chain.frame.vibrationVectors;
        return vibrationVectors == null ? null : vibrationVectors[atomIndex];
    }

    void setLabel(String strLabel) {
        group.chain.frame.setLabel(strLabel, atomIndex);
    }

    void transform(Viewer viewer) {
        if (madAtom == JmolConstants.MAR_DELETED) return;
        Point3i screen;
        Vector3f[] vibrationVectors;
        if ((formalChargeAndFlags & VIBRATION_VECTOR_FLAG) == 0 || (vibrationVectors = group.chain.frame.vibrationVectors) == null) screen = viewer.transformPoint(point3f); else screen = viewer.transformPoint(point3f, vibrationVectors[atomIndex]);
        screenX = screen.x;
        screenY = screen.y;
        screenZ = screen.z;
        screenDiameter = viewer.scaleToScreen(screenZ, madAtom);
    }

    byte getElementNumber() {
        return elementNumber;
    }

    String getElementSymbol() {
        return JmolConstants.elementSymbols[elementNumber];
    }

    String getAtomNameOrNull() {
        String[] atomNames = group.chain.frame.atomNames;
        return atomNames == null ? null : atomNames[atomIndex];
    }

    String getAtomName() {
        String atomName = getAtomNameOrNull();
        return (atomName != null ? atomName : JmolConstants.elementSymbols[elementNumber]);
    }

    String getPdbAtomName4() {
        String atomName = getAtomNameOrNull();
        return atomName != null ? atomName : "";
    }

    String getGroup3() {
        return group.getGroup3();
    }

    String getGroup1() {
        if (group == null) return null;
        return group.getGroup1();
    }

    boolean isGroup3(String group3) {
        return group.isGroup3(group3);
    }

    boolean isGroup3Match(String strWildcard) {
        return group.isGroup3Match(strWildcard);
    }

    int getSeqcode() {
        if (group == null) return -1;
        return group.seqcode;
    }

    int getResno() {
        if (group == null) return -1;
        return group.getResno();
    }

    boolean isAtomNameMatch(String strPattern) {
        String atomName = getAtomNameOrNull();
        int cchAtomName = atomName == null ? 0 : atomName.length();
        int cchPattern = strPattern.length();
        int ich;
        for (ich = 0; ich < cchPattern; ++ich) {
            char charWild = Character.toUpperCase(strPattern.charAt(ich));
            if (charWild == '?') continue;
            if (ich >= cchAtomName || charWild != Character.toUpperCase(atomName.charAt(ich))) return false;
        }
        return ich >= cchAtomName;
    }

    boolean isAlternateLocationMatch(String strPattern) {
        if (strPattern == null) return true;
        if (strPattern.length() != 1) return false;
        return alternateLocationID == strPattern.charAt(0);
    }

    int getAtomNumber() {
        int[] atomSerials = group.chain.frame.atomSerials;
        if (atomSerials != null) return atomSerials[atomIndex];
        if (group.chain.frame.modelSetTypeName == "xyz" && group.chain.frame.viewer.getZeroBasedXyzRasmol()) return atomIndex;
        return atomIndex + 1;
    }

    boolean isHetero() {
        return (formalChargeAndFlags & IS_HETERO_FLAG) != 0;
    }

    int getFormalCharge() {
        return formalChargeAndFlags >> 3;
    }

    boolean isVisible() {
        return (formalChargeAndFlags & VISIBLE_FLAG) != 0;
    }

    float getPartialCharge() {
        float[] partialCharges = group.chain.frame.partialCharges;
        return partialCharges == null ? 0 : partialCharges[atomIndex];
    }

    Point3f getPoint3f() {
        return point3f;
    }

    float getAtomX() {
        return point3f.x;
    }

    float getAtomY() {
        return point3f.y;
    }

    float getAtomZ() {
        return point3f.z;
    }

    public float getDimensionValue(int dimension) {
        return (dimension == 0 ? point3f.x : (dimension == 1 ? point3f.y : point3f.z));
    }

    short getVanderwaalsMar() {
        return JmolConstants.vanderwaalsMars[elementNumber];
    }

    float getVanderwaalsRadiusFloat() {
        return JmolConstants.vanderwaalsMars[elementNumber] / 1000f;
    }

    short getBondingMar() {
        return JmolConstants.getBondingMar(elementNumber, formalChargeAndFlags >> 3);
    }

    float getBondingRadiusFloat() {
        return getBondingMar() / 1000f;
    }

    int getCurrentBondCount() {
        return bonds == null ? 0 : bonds.length;
    }

    Bond getLongestBondToDiscard(Atom atomChallenger) {
        float dist2Longest = point3f.distanceSquared(atomChallenger.point3f);
        Bond bondLongest = null;
        for (int i = bonds.length; --i >= 0; ) {
            Bond bond = bonds[i];
            Atom atomOther = bond.atom1 != this ? bond.atom1 : bond.atom2;
            float dist2 = point3f.distanceSquared(atomOther.point3f);
            if (dist2 > dist2Longest) {
                bondLongest = bond;
                dist2Longest = dist2;
            }
        }
        return bondLongest;
    }

    short getColix() {
        return colixAtom;
    }

    int getArgb() {
        return group.chain.frame.viewer.getColixArgb(colixAtom);
    }

    float getRadius() {
        if (madAtom == JmolConstants.MAR_DELETED) return 0;
        return madAtom / (1000f * 2);
    }

    char getChainID() {
        return group.chain.chainID;
    }

    int getOccupancy() {
        byte[] occupancies = group.chain.frame.occupancies;
        return occupancies == null ? 100 : occupancies[atomIndex];
    }

    int getBfactor100() {
        short[] bfactor100s = group.chain.frame.bfactor100s;
        if (bfactor100s == null) return 0;
        return bfactor100s[atomIndex];
    }

    Group getGroup() {
        return group;
    }

    int getPolymerLength() {
        return group.getPolymerLength();
    }

    int getPolymerIndex() {
        return group.getPolymerIndex();
    }

    int getSelectedGroupCountWithinChain() {
        return group.chain.getSelectedGroupCount();
    }

    int getSelectedGroupIndexWithinChain() {
        return group.chain.getSelectedGroupIndex(group);
    }

    int getSelectedMonomerCountWithinPolymer() {
        if (group instanceof Monomer) {
            return ((Monomer) group).polymer.selectedMonomerCount;
        }
        return 0;
    }

    int getSelectedMonomerIndexWithinPolymer() {
        if (group instanceof Monomer) {
            Monomer monomer = (Monomer) group;
            return monomer.polymer.getSelectedMonomerIndex(monomer);
        }
        return -1;
    }

    int getAtomIndex() {
        return atomIndex;
    }

    Chain getChain() {
        return group.chain;
    }

    Model getModel() {
        return group.chain.model;
    }

    int getModelIndex() {
        return modelIndex;
    }

    String getClientAtomStringProperty(String propertyName) {
        Object[] clientAtomReferences = group.chain.frame.clientAtomReferences;
        return ((clientAtomReferences == null || clientAtomReferences.length <= atomIndex) ? null : (group.chain.frame.viewer.getClientAtomStringProperty(clientAtomReferences[atomIndex], propertyName)));
    }

    boolean isDeleted() {
        return madAtom == JmolConstants.MAR_DELETED;
    }

    void markDeleted() {
        deleteAllBonds();
        madAtom = JmolConstants.MAR_DELETED;
    }

    byte getProteinStructureType() {
        return group.getProteinStructureType();
    }

    short getGroupID() {
        return group.groupID;
    }

    String getSeqcodeString() {
        return group.getSeqcodeString();
    }

    String getModelTag() {
        return group.chain.model.modelTag;
    }

    int getModelTagNumber() {
        try {
            return Integer.parseInt(group.chain.model.modelTag);
        } catch (NumberFormatException nfe) {
            return modelIndex + 1;
        }
    }

    byte getSpecialAtomID() {
        byte[] specialAtomIDs = group.chain.frame.specialAtomIDs;
        return specialAtomIDs == null ? 0 : specialAtomIDs[atomIndex];
    }

    void demoteSpecialAtomImposter() {
        group.chain.frame.specialAtomIDs[atomIndex] = 0;
    }

    private static Hashtable htAtom = new Hashtable();

    static {
        for (int i = JmolConstants.specialAtomNames.length; --i >= 0; ) {
            String specialAtomName = JmolConstants.specialAtomNames[i];
            if (specialAtomName != null) {
                Integer boxedI = new Integer(i);
                htAtom.put(specialAtomName, boxedI);
            }
        }
    }

    static String generateStarredAtomName(String primedAtomName) {
        int primeIndex = primedAtomName.indexOf('\'');
        if (primeIndex < 0) return null;
        return primedAtomName.replace('\'', '*');
    }

    static String generatePrimeAtomName(String starredAtomName) {
        int starIndex = starredAtomName.indexOf('*');
        if (starIndex < 0) return starredAtomName;
        return starredAtomName.replace('*', '\'');
    }

    byte lookupSpecialAtomID(String atomName) {
        if (atomName != null) {
            atomName = generatePrimeAtomName(atomName);
            Integer boxedAtomID = (Integer) htAtom.get(atomName);
            if (boxedAtomID != null) return (byte) (boxedAtomID.intValue());
        }
        return 0;
    }

    String formatLabel(String strFormat) {
        if (strFormat == null || strFormat.equals("")) return null;
        String strLabel = "";
        int ich, ichPercent;
        for (ich = 0; (ichPercent = strFormat.indexOf('%', ich)) != -1; ) {
            if (ich != ichPercent) strLabel += strFormat.substring(ich, ichPercent);
            ich = ichPercent + 1;
            try {
                String strT = "";
                float floatT = 0;
                boolean floatIsSet = false;
                boolean alignLeft = false;
                if (strFormat.charAt(ich) == '-') {
                    alignLeft = true;
                    ++ich;
                }
                boolean zeroPad = false;
                if (strFormat.charAt(ich) == '0') {
                    zeroPad = true;
                    ++ich;
                }
                char ch;
                int width = 0;
                while ((ch = strFormat.charAt(ich)) >= '0' && (ch <= '9')) {
                    width = (10 * width) + (ch - '0');
                    ++ich;
                }
                int precision = -1;
                if (strFormat.charAt(ich) == '.') {
                    ++ich;
                    if ((ch = strFormat.charAt(ich)) >= '0' && (ch <= '9')) {
                        precision = ch - '0';
                        ++ich;
                    }
                }
                switch(ch = strFormat.charAt(ich++)) {
                    case 'i':
                        strT = "" + getAtomNumber();
                        break;
                    case 'a':
                        strT = getAtomName();
                        break;
                    case 'e':
                        strT = JmolConstants.elementSymbols[elementNumber];
                        break;
                    case 'x':
                        floatT = point3f.x;
                        floatIsSet = true;
                        break;
                    case 'y':
                        floatT = point3f.y;
                        floatIsSet = true;
                        break;
                    case 'z':
                        floatT = point3f.z;
                        floatIsSet = true;
                        break;
                    case 'X':
                        strT = "" + atomIndex;
                        break;
                    case 'C':
                        int formalCharge = getFormalCharge();
                        if (formalCharge > 0) strT = "" + formalCharge + "+"; else if (formalCharge < 0) strT = "" + -formalCharge + "-"; else strT = "0";
                        break;
                    case 'P':
                        floatT = getPartialCharge();
                        floatIsSet = true;
                        break;
                    case 'V':
                        floatT = getVanderwaalsRadiusFloat();
                        floatIsSet = true;
                        break;
                    case 'I':
                        floatT = getBondingRadiusFloat();
                        floatIsSet = true;
                        break;
                    case 'b':
                    case 't':
                        floatT = getBfactor100() / 100f;
                        floatIsSet = true;
                        break;
                    case 'q':
                        strT = "" + getOccupancy();
                        break;
                    case 'c':
                    case 's':
                        strT = "" + getChainID();
                        break;
                    case 'L':
                        strT = "" + getPolymerLength();
                        break;
                    case 'M':
                        strT = "/" + getModelTagNumber();
                        break;
                    case 'm':
                        strT = getGroup1();
                        break;
                    case 'n':
                        strT = getGroup3();
                        break;
                    case 'r':
                        strT = getSeqcodeString();
                        break;
                    case 'U':
                        strT = getIdentity();
                        break;
                    case '%':
                        strT = "%";
                        break;
                    case '{':
                        int ichCloseBracket = strFormat.indexOf('}', ich);
                        if (ichCloseBracket > ich) {
                            String propertyName = strFormat.substring(ich, ichCloseBracket);
                            String value = getClientAtomStringProperty(propertyName);
                            if (value != null) strT = value;
                            ich = ichCloseBracket + 1;
                            break;
                        }
                    default:
                        strT = "%" + ch;
                }
                if (floatIsSet) {
                    strLabel += format(floatT, width, precision, alignLeft, zeroPad);
                } else {
                    strLabel += format(strT, width, precision, alignLeft, zeroPad);
                }
            } catch (IndexOutOfBoundsException ioobe) {
                ich = ichPercent;
                break;
            }
        }
        strLabel += strFormat.substring(ich);
        if (strLabel.length() == 0) return null;
        return strLabel.intern();
    }

    String format(float value, int width, int precision, boolean alignLeft, boolean zeroPad) {
        return format(group.chain.frame.viewer.formatDecimal(value, precision), width, 0, alignLeft, zeroPad);
    }

    static String format(String value, int width, int precision, boolean alignLeft, boolean zeroPad) {
        if (precision > value.length()) value = value.substring(0, precision);
        int padLength = width - value.length();
        if (padLength <= 0) return value;
        StringBuffer sb = new StringBuffer();
        if (alignLeft) sb.append(value);
        for (int i = padLength; --i >= 0; ) sb.append((!alignLeft && zeroPad) ? '0' : ' ');
        if (!alignLeft) sb.append(value);
        return "" + sb;
    }

    String getInfo() {
        return getIdentity();
    }

    String getIdentity() {
        StringBuffer info = new StringBuffer();
        String group3 = getGroup3();
        String seqcodeString = getSeqcodeString();
        char chainID = getChainID();
        if (group3 != null && group3.length() > 0) {
            info.append("[");
            info.append(group3);
            info.append("]");
        }
        if (seqcodeString != null) info.append(seqcodeString);
        if (chainID != 0 && chainID != ' ') {
            info.append(":");
            info.append(chainID);
        }
        String atomName = getAtomNameOrNull();
        if (atomName != null) {
            if (info.length() > 0) info.append(".");
            info.append(atomName);
        }
        if (info.length() == 0) {
            info.append(getElementSymbol());
            info.append(" ");
            info.append(getAtomNumber());
        }
        if (group.chain.frame.getModelCount() > 1) {
            info.append("/");
            info.append(getModelTag());
        }
        info.append(" #");
        info.append(getAtomNumber());
        return "" + info;
    }

    boolean isCursorOnTopOfVisibleAtom(int xCursor, int yCursor, int minRadius, Atom competitor) {
        return (((formalChargeAndFlags & VISIBLE_FLAG) != 0) && isCursorOnTop(xCursor, yCursor, minRadius, competitor));
    }

    boolean isCursorOnTop(int xCursor, int yCursor, int minRadius, Atom competitor) {
        int r = screenDiameter / 2;
        if (r < minRadius) r = minRadius;
        int r2 = r * r;
        int dx = screenX - xCursor;
        int dx2 = dx * dx;
        if (dx2 > r2) return false;
        int dy = screenY - yCursor;
        int dy2 = dy * dy;
        int dz2 = r2 - (dx2 + dy2);
        if (dz2 < 0) return false;
        if (competitor == null) return true;
        int z = screenZ;
        int zCompetitor = competitor.screenZ;
        int rCompetitor = competitor.screenDiameter / 2;
        if (z < zCompetitor - rCompetitor) return true;
        int dxCompetitor = competitor.screenX - xCursor;
        int dx2Competitor = dxCompetitor * dxCompetitor;
        int dyCompetitor = competitor.screenY - yCursor;
        int dy2Competitor = dyCompetitor * dyCompetitor;
        int r2Competitor = rCompetitor * rCompetitor;
        int dz2Competitor = r2Competitor - (dx2Competitor + dy2Competitor);
        return (z - Math.sqrt(dz2) < zCompetitor - Math.sqrt(dz2Competitor));
    }

    int getScreenX() {
        return screenX;
    }

    int getScreenY() {
        return screenY;
    }

    int getScreenZ() {
        return screenZ;
    }

    int getScreenD() {
        return screenDiameter;
    }

    boolean isProtein() {
        return group.isProtein();
    }

    boolean isNucleic() {
        return group.isNucleic();
    }

    boolean isDna() {
        return group.isDna();
    }

    boolean isRna() {
        return group.isRna();
    }

    boolean isPurine() {
        return group.isPurine();
    }

    boolean isPyrimidine() {
        return group.isPyrimidine();
    }

    Hashtable getPublicProperties() {
        Hashtable ht = new Hashtable();
        ht.put("element", getElementSymbol());
        ht.put("x", new Double(point3f.x));
        ht.put("y", new Double(point3f.y));
        ht.put("z", new Double(point3f.z));
        ht.put("atomIndex", new Integer(atomIndex));
        ht.put("modelIndex", new Integer(modelIndex));
        ht.put("argb", new Integer(getArgb()));
        ht.put("radius", new Double(getRadius()));
        ht.put("atomNumber", new Integer(getAtomNumber()));
        return ht;
    }
}
