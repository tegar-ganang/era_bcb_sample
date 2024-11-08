package com.lemu.music;

import javax.swing.DefaultComboBoxModel;
import jm.music.data.Part;
import jm.music.data.Phrase;
import jmms.TickEvent;
import org.w3c.dom.Element;
import ren.gui.ParameterMap;
import ren.io.Domable;
import ren.io.Domc;
import ren.tonal.TonalManager;
import ren.util.Make;
import ren.util.PO;
import ren.util.RMath;
import ai.An;
import com.lemu.music.morph.rt.MarkovMorph2RT;
import com.lemu.music.morph.rt.MorphRTFactory;
import com.lemu.music.morph.rt.MorpherRT;
import com.lemu.music.morph.transearch.TraseMorph;
import com.lemu.play.LPlayer;

public class PartMorph implements Domable {

    /**
     * holds the current morphers
     */
    private DefaultComboBoxModel morphStrucList, morphInst;

    private static String SOLO = "solo";

    private boolean solo = false, mute = false;

    private static String STRUC_CO = "strucCO", STRUC_GR = "strucGR", STRUC_EX = "strucEX";

    private ParameterMap strucCO = Make.crossOver(STRUC_CO), strucGR = Make.gradient(STRUC_GR), strucEX = Make.exponential(STRUC_EX);

    private static String SCOPE_CO = "scopeCO", SCOPE_GR = "scopeGR";

    private ParameterMap scopeCO = Make.crossOver(SCOPE_CO), scopeGR = Make.gradient(SCOPE_GR);

    private static String QUA_CO = "quantiseCO", QUA_GR = "quantiseGR";

    private ParameterMap quaCO = Make.crossOver(QUA_CO), quaGR = Make.gradient(QUA_GR);

    private static String SHU_CO = "shuffleCO", SHU_GR = "shuffleGR";

    private ParameterMap shuCO = Make.crossOver(SHU_CO), shuGR = Make.gradient(SHU_GR);

    private static String VOL_EARL = "volEarliness";

    private ParameterMap volEarl = Make.gradient(VOL_EARL);

    private static String VOL_CO = "volCO";

    private ParameterMap volCO = Make.crossOver(VOL_CO);

    private static String VOL_CO2 = "volCO2";

    private ParameterMap volCO2 = Make.crossOver(VOL_CO2);

    private static String MI_QUA = "indexQuantise";

    private ParameterMap miqua = (new ParameterMap()).construct(1, 17, 4, "indexQuantise");

    private static String REP = "repeatLoop";

    private ParameterMap rep = (new ParameterMap()).construct(0, 1, 0, "repeat");

    /**
     * temporarily holds the result of the morph
     */
    private Part[] morphResult = new Part[2];

    /** temporarily holds the parts to be morphed */
    private Part to, from;

    private LPart lto, lfrom;

    private LPart[] fromTo = new LPart[2];

    private LPart[] lpmorph = new LPart[2];

    private boolean enabled = true;

    private MultiMorph multi;

    private double inrepat = -1;

    private double misto = 0;

    private RepeatPartArr reph = new RepeatPartArr();

    private boolean fotole = false;

    private double mi = -1;

    private boolean muteNextBar = false;

    private static final int DFMNB = Integer.MIN_VALUE;

    private int muteNextBarCounter = DFMNB;

    private int muteNextBarCutOff = 2;

    private static String FOTOLE = "fotole";

    public PartMorph() {
        super();
        initMorphAlgorithmModels();
    }

    public PartMorph construct(MultiMorph multin) {
        this.multi = multin;
        return this;
    }

    public void initMorphAlgorithmModels() {
        morphStrucList = new DefaultComboBoxModel(MorphRTFactory.createAllStruc());
        morphInst = new DefaultComboBoxModel(new String[] { "together", "alternate" });
    }

    /**
     * (morphParams are scope)
     * 
     * it is separate so that it can be overridden
     * easily if necessary
     * 
     * @param i
     *            partIndex
     * @return
     */
    protected Part[] getTickMorph(double position, TickEvent e) {
        if (this.muteNextBar) {
            if (this.muteNextBarCounter == this.DFMNB) {
                this.muteNextBarCounter = 0;
            }
            if (e.at() % 4.0 == 0) {
                this.muteNextBarCounter++;
            }
            if (this.muteNextBarCounter == muteNextBarCutOff) {
                this.muteNextBar(false);
                this.setMute(true);
            }
        }
        if (this.mute) {
            morphResult[0] = lpmorph[0].getPart().copyEmpty();
            morphResult[1] = lpmorph[1].getPart().copyEmpty();
            return morphResult;
        }
        if (mi > -1) {
            position = mi;
        }
        if (!this.enabled) return null;
        morphParameters(this.lfrom, this.lto, position);
        if (this.rep.getValue() == 0) {
            makeMorphResult(position, e);
            inrepat = -1;
        } else {
            if (inrepat == -1) {
                inrepat = e.at();
                this.reph.construct(lpmorph[0].getScope().getValue(), e.getRes());
            }
            if (inrepat + lpmorph[0].getScope().getValue() >= e.at()) {
                makeMorphResult(position, e);
                this.reph.setAt(e.at(), e.getRes(), morphResult);
            } else {
                if (transformStrucIndex(position) != misto) inrepat = -1;
                morphResult = reph.getAt(e.at(), e.getRes());
                if (morphResult == null || morphResult[0] == null || morphResult[1] == null) {
                    morphResult[0] = lpmorph[0].getPart().copyEmpty();
                    morphResult[1] = lpmorph[1].getPart().copyEmpty();
                }
            }
        }
        morphInst(morphResult);
        morphVolume(morphResult, position, from.getVolume(), to.getVolume());
        if (this.VB_VOL) PO.p("end morph restul volume = \n m1v" + morphResult[0].getVolume() + "  m2v = " + morphResult[1].getVolume());
        return morphResult;
    }

    private void makeMorphResult(double position, TickEvent e) {
        misto = transformStrucIndex(position);
        morphResult = ((MorpherRT) this.morphStruc()).morphRT(this.fromTo, this.lpmorph, misto, e, ((LPlayer) e.getSource()).getChIDHistQ(from.getIdChannel()));
        morphResult[0].setChannel(this.fromTo[0].getPart().getChannel());
        morphResult[1].setChannel(this.fromTo[1].getPart().getChannel());
        if (this.lpmorph == null) {
        }
        if (this.lpmorph[0] == null) {
        }
        if (this.multi == null) {
        }
        if (this.fotole && this.lpmorph[0].getTonalManager() != this.multi.getTonalLeadObj()) {
            lpmorph[0].setPart(morphResult[0]);
            lpmorph[0].convertToDEPA();
            lpmorph[0].initToTonalManager(this.multi.getTonalLeadObj());
            lpmorph[0].convertFromDEPA();
            lpmorph[1].setPart(morphResult[1]);
            lpmorph[1].convertToDEPA();
            lpmorph[1].initToTonalManager(this.multi.getTonalLeadObj());
            lpmorph[1].convertFromDEPA();
            this.morphResult[0] = lpmorph[0].getPart();
            this.morphResult[1] = lpmorph[1].getPart();
        }
    }

    private double transformStrucIndex(double i) {
        double x = RMath.linearFunc(i, 1 / strucGR.getValue(), strucCO.getValue() - 0.5);
        x = Math.pow(x, this.strucEX.getValue());
        if (miqua.getValueInt() == 17) return x;
        if (x == 1.0) x = 0.999999; else if (x == 0) x = 0.0000001;
        double segspa = 1.0 / this.miqua.getValue();
        x = (int) (x / segspa);
        x = x * segspa + segspa / 2.0;
        return x;
    }

    /**
     * used for parameterMaps the control the gradient and cross over of the 
     * index of the morph relating to that parameter
     * @param i
     * @param m
     * @param c
     * @return
     */
    private double transform(double i, ParameterMap m, ParameterMap c) {
        return RMath.linearFunc(i, 1 / m.getValue(), c.getValue() - 0.5);
    }

    private void morphInst(Part[] tmorph) {
        if (tmorph[0].getChannel() != tmorph[1].getChannel() && this.morphInst.getSelectedItem().equals("together")) {
            Part mrc1 = tmorph[0].copy();
            Part mrc2 = tmorph[1].copy();
            for (int j = 0; j < mrc2.size(); j++) {
                Phrase sam = An.getSame(tmorph[0], mrc2.getPhrase(j));
                if (sam == null) {
                    tmorph[0].addPhrase(mrc2.getPhrase(j));
                }
            }
            for (int j = 0; j < mrc1.size(); j++) {
                Phrase sam = An.getSame(tmorph[1], mrc1.getPhrase(j));
                if (sam == null) {
                    tmorph[1].addPhrase(mrc1.getPhrase(j));
                }
            }
        }
    }

    private boolean VB_VOL = false;

    private void morphVolume(Part[] tmorph, double pos, int vol1, int vol2) {
        if (VB_VOL) PO.p("\nmorphing volume");
        if (tmorph[0].getChannel() == tmorph[1].getChannel()) {
            if (VB_VOL) PO.p("same channel");
            pos = RMath.linearFunc(pos, (1 / this.volEarl.getValue()), this.volCO.getValue() - 0.5);
            tmorph[0].setVolume((int) (vol1 + (vol2 - vol1) * pos));
            tmorph[1].setVolume(tmorph[0].getVolume());
        } else {
            if (VB_VOL) PO.p("not same channel. pre \n vol 1 " + vol1 + " vol 2 " + vol2);
            vol1 = (int) (vol1 * 1.0 * RMath.boundHard((1.0 - pos) * (1.0 / this.volEarl.getValue()) + this.volCO.getValue() - 0.5));
            if (VB_VOL) PO.p("new vol2 = " + vol2 + " * " + (1.0 * RMath.boundHard((pos))) + " * " + (1.0 / this.volEarl.getValue()) + " + " + (this.volCO2.getValue() - 0.5) + " = ");
            vol2 = (int) (vol2 * 1.0 * RMath.boundHard((pos) * (1.0 / this.volEarl.getValue()) + this.volCO2.getValue() - 0.5));
            if (VB_VOL) PO.p(vol2 + " ");
            tmorph[0].setVolume(vol1);
            tmorph[1].setVolume(vol2);
            if (VB_VOL) PO.p("post morphing volume \n vol 1 " + vol1 + " vol 2 " + vol2);
        }
    }

    private void morphParameters(LPart fr, LPart to, double pos) {
        for (int i = 0; i < this.lpmorph.length; i++) {
            interpolate(fr.getShuffle(), to.getShuffle(), lpmorph[i].getShuffle(), this.transform(pos, shuGR, shuCO));
            interpolate(fr.getQuantise(), to.getQuantise(), lpmorph[i].getQuantise(), this.transform(pos, quaGR, quaCO));
            if ((((MorpherRT) this.morphStrucList.getSelectedItem()) instanceof TraseMorph)) {
            } else if ((((MorpherRT) this.morphStrucList.getSelectedItem()) instanceof MarkovMorph2RT)) {
                if (pos < 0.5) lpmorph[i].getScope().setValue(fr.getScope().getValue()); else lpmorph[i].getScope().setValue(to.getScope().getValue());
            } else {
                interpolate(fr.getScope(), to.getScope(), lpmorph[i].getScope(), this.transform(pos, scopeGR, scopeCO));
            }
        }
    }

    private void interpolate(ParameterMap f, ParameterMap t, ParameterMap m, double p) {
        m.setClosestValue(f.getValue() * (1 - p) + t.getValue() * p);
    }

    public void initParts(LScore startScore, LScore endScore, LScore mscore, int i, int morphLength) {
        this.lpmorph[0] = mscore.getLPart(i * 2);
        this.lpmorph[1] = mscore.getLPart(i * 2 + 1);
        if (startScore.size() > i) {
            lfrom = startScore.getLPart(i);
            from = lfrom.getPart();
        }
        if (endScore.size() > i) {
            lto = endScore.getLPart(i);
            to = lto.getPart();
        }
        if (endScore.size() <= i) {
            to = new Part();
            to.setChannel(from.getChannel());
            to.setInstrument(from.getInstrument());
            to.setIdChannel(from.getIdChannel());
            to.setVolume(0);
            lto = lfrom;
        } else if (startScore.size() <= i) {
            from = new Part();
            from.setChannel(to.getChannel());
            from.setInstrument(to.getInstrument());
            from.setIdChannel(to.getIdChannel());
            from.setVolume(0);
            lfrom = lto;
        }
        this.fromTo[0] = lfrom;
        this.fromTo[1] = lto;
        morphStruc().initParts(lfrom, lto, lpmorph, morphLength);
    }

    public void startInit() {
        this.inrepat = -1;
        this.misto = 0;
        morphStruc().startInit();
    }

    public void finishInit() {
        morphStruc().finish();
    }

    /**
     * convenience method for the morph structure
     * algorithm
     */
    private MorpherRT morphStruc() {
        return (MorpherRT) this.morphStrucList.getSelectedItem();
    }

    public DefaultComboBoxModel getMorphStrucList() {
        return morphStrucList;
    }

    public void setMorphStrucList(DefaultComboBoxModel morphStrucList) {
        this.morphStrucList = morphStrucList;
    }

    public DefaultComboBoxModel getMorphInst() {
        return this.morphInst;
    }

    public void dload(Element e) {
        BasicMorphMusicGen.setCBModel(this.morphStrucList, ((Element) (e.getElementsByTagName(STRUC).item(0))), e.getOwnerDocument());
        if (e.getAttribute(MINST).length() > 0) this.morphInst.setSelectedItem(this.morphInst.getElementAt(Integer.parseInt(e.getAttribute(MINST))));
        strucCO.setValue(e.getAttribute(STRUC_CO));
        if (e.hasAttribute(STRUC_EX)) ;
        strucEX.setValue(e.getAttribute(STRUC_EX));
        strucGR.setValue(e.getAttribute(STRUC_GR));
        scopeCO.setValue(e.getAttribute(SCOPE_CO));
        scopeGR.setValue(e.getAttribute(SCOPE_GR));
        quaCO.setValue(e.getAttribute(QUA_CO));
        quaGR.setValue(e.getAttribute(QUA_GR));
        shuCO.setValue(e.getAttribute(SHU_CO));
        shuGR.setValue(e.getAttribute(SHU_GR));
        volEarl.setValue(e.getAttribute(VOL_EARL));
        if (e.hasAttribute(VOL_CO)) volCO.setValue(e.getAttribute(VOL_CO));
        if (e.hasAttribute(VOL_CO2)) this.volCO2.setValue(e.getAttribute(VOL_CO2));
        if (e.hasAttribute(MI_QUA)) this.miqua.setValue(e.getAttribute(MI_QUA));
        if (e.hasAttribute(SOLO)) this.solo = Boolean.valueOf(e.getAttribute(SOLO)).booleanValue();
        if (e.hasAttribute(REP)) this.rep.setValue(e.getAttribute(REP));
        if (e.hasAttribute(FOTOLE)) this.fotole = Boolean.valueOf(e.getAttribute(FOTOLE)).booleanValue();
        if (e.getElementsByTagName(FROM).getLength() > 0) {
            this.lfrom = (LPart) (Domc.lo((Element) (e.getElementsByTagName(FROM).item(0)), e.getOwnerDocument()));
            this.from = lfrom.getPart();
            this.fromTo[0] = lfrom;
            this.lpmorph[0] = lfrom.copyEmpty();
        } else {
        }
        if (e.getElementsByTagName(TO).getLength() > 0) {
            this.lto = (LPart) (Domc.lo((Element) (e.getElementsByTagName(TO)).item(0), e.getOwnerDocument()));
            this.to = lto.getPart();
            this.fromTo[1] = lto;
            this.lpmorph[1] = lto.copyEmpty();
        }
        if (e.getElementsByTagName(MULTI).getLength() > 0) {
            this.multi = (MultiMorph) (Domc.lo((Element) (e.getElementsByTagName(MULTI)).item(0), e.getOwnerDocument()));
        }
    }

    private static String STRUC = "mstruc";

    private static String MINST = "minst";

    private static String FROM = "from";

    private static String TO = "to";

    private static String MULTI = "multi";

    public void dsave(Element e) {
        e.appendChild(Domc.sa(morphStruc(), STRUC, e.getOwnerDocument()));
        e.appendChild(Domc.sa(this.lfrom, FROM, e.getOwnerDocument()));
        e.appendChild(Domc.sa(this.lto, TO, e.getOwnerDocument()));
        e.setAttribute(MINST, Integer.toString(this.morphInst.getIndexOf(this.morphInst.getSelectedItem())));
        e.setAttribute(STRUC_CO, String.valueOf(strucCO.getValue()));
        e.setAttribute(STRUC_GR, String.valueOf(strucGR.getValue()));
        e.setAttribute(STRUC_EX, String.valueOf(strucEX.getValue()));
        e.setAttribute(SCOPE_CO, String.valueOf(scopeCO.getValue()));
        e.setAttribute(SCOPE_GR, String.valueOf(scopeGR.getValue()));
        e.setAttribute(QUA_CO, String.valueOf(quaCO.getValue()));
        e.setAttribute(QUA_GR, String.valueOf(quaGR.getValue()));
        e.setAttribute(SHU_CO, String.valueOf(shuCO.getValue()));
        e.setAttribute(SHU_GR, String.valueOf(shuGR.getValue()));
        e.setAttribute(VOL_EARL, String.valueOf(volEarl.getValue()));
        e.setAttribute(VOL_CO, String.valueOf(volCO.getValue()));
        e.setAttribute(this.VOL_CO2, String.valueOf(this.volCO2.getValue()));
        e.setAttribute(MI_QUA, this.miqua.getValueStr());
        e.setAttribute(SOLO, String.valueOf(solo));
        e.setAttribute(REP, rep.getValueStr());
        e.setAttribute(FOTOLE, String.valueOf(fotole));
        e.appendChild(Domc.sa(this.multi, MULTI, e.getOwnerDocument()));
    }

    public MultiMorph getMultiMorph() {
        return multi;
    }

    public ParameterMap getStrucCO() {
        return this.strucCO;
    }

    public ParameterMap getStrucGR() {
        return this.strucGR;
    }

    public ParameterMap getStrucEX() {
        return this.strucEX;
    }

    public ParameterMap getQuaCO() {
        return quaCO;
    }

    public ParameterMap getQuaGR() {
        return quaGR;
    }

    public ParameterMap getScopeCO() {
        return scopeCO;
    }

    public ParameterMap getScopeGR() {
        return scopeGR;
    }

    public ParameterMap getShuCO() {
        return shuCO;
    }

    public ParameterMap getShuGR() {
        return shuGR;
    }

    public ParameterMap getVolEarl() {
        return volEarl;
    }

    public ParameterMap getVolCO() {
        return volCO;
    }

    public ParameterMap getVolCO2() {
        return volCO2;
    }

    public ParameterMap getMIQua() {
        return this.miqua;
    }

    public boolean getSolo() {
        return solo;
    }

    public void setSolo(boolean b) {
        this.solo = b;
    }

    public void setEnabled(boolean b) {
        this.enabled = b;
    }

    public ParameterMap getRep() {
        return this.rep;
    }

    /**
	 * check to see if it is following the tonal lead in multimorph
	 * @return
	 */
    public boolean isTonalFollower() {
        return this.fotole;
    }

    /**
	 * wether to follow the tonal leader (in multimorph) or not
	 * @param n
	 */
    public void setTonalFollower(boolean n) {
        this.fotole = n;
    }

    public TonalManager getCurrentTonalManager() {
        return this.lpmorph[0].getTonalManager();
    }

    public String getTitle() {
        if (this.lfrom != null) {
            return lfrom.getPart().getTitle();
        } else {
            return "     ";
        }
    }

    public LPart getFrom() {
        return this.lfrom;
    }

    public LPart getTo() {
        return this.lto;
    }

    public LPart[] getFromTo() {
        return this.fromTo;
    }

    public void setMI(double mi) {
        this.mi = mi;
    }

    public ParameterMap getMorphScope() {
        if (this.lpmorph[0] == null) {
            PO.p(" not scoping part morphg = " + this.getTitle());
        }
        return this.lpmorph[0].getScope();
    }

    public void setMute(boolean nmute) {
        this.mute = nmute;
    }

    public boolean getMute() {
        return mute;
    }

    public void muteNextBar(boolean b) {
        if (!b) {
            this.mute = false;
        }
        this.muteNextBarCounter = DFMNB;
        this.muteNextBar = b;
    }

    public boolean isMuteNextBar() {
        return this.muteNextBar;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("\nfrom = " + from.toString());
        sb.append("\n\nto = " + to.toString());
        return sb.toString();
    }
}

class RepeatPartArr {

    private Part[][] par;

    private int pos = 0;

    private double scope;

    public RepeatPartArr() {
    }

    public RepeatPartArr construct(double scope, double res) {
        this.scope = scope;
        par = new Part[(int) (scope / res + 0.5)][2];
        return this;
    }

    public void setAt(double at, double res, Part[] tost) {
        at = at % scope;
        int in = (int) (at * 1.0 / res * 1.0 + 0.5);
        par[in][0] = tost[0].copy();
        par[in][1] = tost[1].copy();
    }

    public Part[] getAt(double at, double res) {
        at = at % scope;
        int in = (int) (at * 1.0 / res * 1.0 + 0.5);
        return new Part[] { par[in][0], par[in][1] };
    }
}
