package com.lemu.music;

import javax.swing.ButtonModel;
import javax.swing.JToggleButton;
import jm.music.data.Part;
import jm.util.Convert;
import jmms.TickEvent;
import org.w3c.dom.Element;
import com.lemu.music.transform.TransformChain;
import ren.gui.ParameterMap;
import ren.io.Domable;
import ren.io.Domc;
import ren.tonal.TonalManager;

/**
 * @author wooller
 * 
 * TODO To change the template for this generated type
 * comment go to Window - Preferences - Java - Code
 * Style - Code Templates
 */
public class LPart implements TransformChainHolder, Domable {

    private int[] midiControlTypes = new int[] { 3, 1, 4 };

    private Part part;

    private TransformChain tc;

    private JToggleButton.ToggleButtonModel mute = new JToggleButton.ToggleButtonModel();

    public static final String MUTE = "mute";

    private JToggleButton.ToggleButtonModel solo = new JToggleButton.ToggleButtonModel();

    public static final String SOLO = "solo";

    private TonalManager tm = new TonalManager();

    private boolean DEPACHE_MODE = false;

    private static final String DEPACHE_MODE_STR = "depmode";

    public LPart() {
    }

    public LPart construct(Part p) {
        this.part = p;
        tm = new TonalManager();
        tm.addLPart(this);
        (tc = new TransformChain()).construct();
        return this;
    }

    public void reset() {
        this.part.empty();
        this.tc.construct();
    }

    public LPart copyEmpty() {
        Part p = new Part(this.part.getTitle(), this.part.getInstrument(), this.part.getChannel());
        LPart l = new LPart();
        l.setPart(p);
        l.setTransformChain(this.getTransformChain().copy());
        l.getQuantise().setValue(this.getQuantise().getValue());
        l.getShuffle().setValue(this.getShuffle().getValue());
        l.getMuteModel().setSelected(this.mute.isSelected());
        l.getSoloModel().setSelected(this.solo.isSelected());
        l.setTonalManager(this.getTonalManager().copy());
        l.getTonalManager().addLPart(l);
        l.setDEPA(this.isDEPA());
        return l;
    }

    private void setDEPA(boolean b) {
        this.DEPACHE_MODE = b;
    }

    public LPart copy() {
        LPart tr = copyEmpty();
        tr.setPart(getPart().copy());
        return tr;
    }

    /**
     * this is designed for use in lscore when you need
     * to initialise the lscore for morphing
     * 
     * @param lp
     * @param newTC
     *            wether or not to make a new copy of
     *            the transform chain
     */
    public void emptyCopyFrom(LPart lp, boolean newTC) {
        this.part = new Part();
        lp.getPart().copyAttributes(this.part);
        if (newTC) {
            this.tc = lp.getTransformChain().copy();
        } else {
            this.tc = lp.getTransformChain();
        }
    }

    /**
     * beware: this links the transform chain from the
     * lpart that is coming in
     * 
     * @param lp
     */
    public void emptyCopyFrom(LPart lp) {
        emptyCopyFrom(lp, true);
    }

    /**
     * the output will always be in absolute pitches (not DEPA)
     */
    public Part getTickPart(TickEvent e) {
        Part tempp = tc.transform(part, null, e);
        tempp.setCCEnvs(part.copyCCEnv(e.at(), e.at() + e.getRes()));
        if (this.DEPACHE_MODE) {
            this.convertFromDEPA(tempp);
        }
        return tempp;
    }

    public void setTransformChain(TransformChain tc) {
        this.tc = tc;
    }

    public TransformChain getTransformChain() {
        return tc;
    }

    public boolean isMute() {
        return mute.isSelected();
    }

    public boolean isSolo() {
        return solo.isSelected();
    }

    /**
     * @return Returns the part.
     */
    public Part getPart() {
        return part;
    }

    /**
     * @param part
     *            The part to set.
     */
    public void setPart(Part part) {
        this.part = part;
    }

    public String toString() {
        return part.getTitle();
    }

    public String print() {
        StringBuffer sb = new StringBuffer();
        sb.append(part.toString() + " " + getQuantise().toString() + " " + getQuantise().getValue() + " " + getShuffle().toString() + " " + getShuffle().getValue() + " \n" + "tchain = " + tc.toString());
        sb.append(" mute = " + this.mute.isSelected());
        sb.append(" solo = " + this.solo.isSelected());
        return sb.toString();
    }

    public ParameterMap getQuantise() {
        return tc.getQuantiseParam();
    }

    public ParameterMap getShuffle() {
        return tc.getShuffleParam();
    }

    public ParameterMap getScope() {
        return this.tc.getScopeParam();
    }

    public TonalManager getTonalManager() {
        return this.tm;
    }

    public void setTonalManager(TonalManager ntm) {
        if (tm == null) tm = ntm;
        this.tm.removeLPart(this);
        this.tm = ntm;
        tm.addLPart(this);
    }

    public void dload(Element e) {
        try {
            this.construct(Convert.xmlStringToPart(e.getAttribute("part")));
            this.tc = (TransformChain) Domc.lo((Element) (e.getElementsByTagName("tc").item(0)), TransformChain.class, e.getOwnerDocument());
            Element tme = (Element) (e.getElementsByTagName("tm").item(0));
            if (tme != null) this.tm = (TonalManager) Domc.lo(tme, TonalManager.class, e.getOwnerDocument());
            this.mute.setSelected(Boolean.getBoolean(e.getAttribute(MUTE)));
            this.solo.setSelected(Boolean.getBoolean(e.getAttribute(SOLO)));
            if (e.hasAttribute(DEPACHE_MODE_STR)) {
                this.DEPACHE_MODE = Boolean.valueOf(e.getAttribute(DEPACHE_MODE_STR)).booleanValue();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void dsave(Element e) {
        e.setAttribute("part", Convert.partToXMLString(this.part));
        e.appendChild((Element) Domc.sa(this.tc, "tc", e.getOwnerDocument()));
        e.appendChild((Element) Domc.sa(this.tm, "tm", e.getOwnerDocument()));
        e.setAttribute(MUTE, String.valueOf(this.mute.isSelected()));
        e.setAttribute(SOLO, String.valueOf(this.solo.isSelected()));
        e.setAttribute(this.DEPACHE_MODE_STR, String.valueOf(this.DEPACHE_MODE));
    }

    public int size() {
        return this.part.size();
    }

    public ButtonModel getMuteModel() {
        return this.mute;
    }

    public ButtonModel getSoloModel() {
        return this.solo;
    }

    public boolean automate(String name, boolean val) {
        if (name.equals(MUTE)) {
            this.mute.setSelected(val);
            return true;
        } else if (name.equals(SOLO)) {
            this.solo.setSelected(val);
            return true;
        }
        return false;
    }

    public Part getPartBounded() {
        return this.part.copyRT(0, this.getScope().getValue());
    }

    /**
	 * if this LPart is in DEPA format, then this is the preferred method to
	 * change the scale.  This is because the pitch data within the part
	 * needs to be rescaled to match the length of the scale.  Eg, if there are
	 * 7 degrees in the scale, there will be 14 DEPA steps per octave.  If
	 * the scale to one where there are 8 degrees, there will be 16 steps, and
	 * the data will need to be remapped.
	 * 
	 * 
	 * @param ns
	 */
    public void setScale(int ns) {
        if (this.DEPACHE_MODE && tm.getScale().length != tm.getScales().getScale(ns).length) {
            for (int i = 0; i < part.getSize(); i++) {
                part.getPhrase(i).getNote(0).setPitch(tm.convertDEPA(part.getPhrase(i).getNote(0).getPitch(), 3, tm.getScale().length, 3, tm.getScales().getScale(ns).length));
            }
            this.tm.setScale(ns, true);
        } else {
            this.tm.setScale(ns);
        }
    }

    public void setScale(String ns) {
        this.setScale(this.tm.getScaleIndex(ns));
    }

    /**
	 * 
	 * If you change the scale to have a different length you'll need to
	 * 
	 * 
	 * @return
	 */
    public LPart convertToDEPA() {
        if (this.DEPACHE_MODE == true) {
            Exception e = new Exception("can't convert to DEPA unless it " + "is in absolute mode ");
            try {
                e.fillInStackTrace();
                throw e;
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        }
        for (int i = 0; i < this.part.length(); i++) {
            part.getPhrase(i).getNote(0).setPitch(this.tm.getDEPA(part.getPhrase(i).getNote(0).getPitch()));
        }
        this.DEPACHE_MODE = true;
        return this;
    }

    public LPart convertFromDEPA() {
        if (this.DEPACHE_MODE != true) {
            Exception e = new Exception("can't convert from DEPA unless it " + "is in DEPA mode already ");
            try {
                e.fillInStackTrace();
                throw e;
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        }
        for (int i = 0; i < this.part.length(); i++) {
            part.getPhrase(i).getNote(0).setPitch(this.tm.getABS(part.getPhrase(i).getNote(0).getPitch()));
        }
        this.DEPACHE_MODE = false;
        return this;
    }

    /**
	 * used by get tick part, to ensure that the returned tick part is not
	 * in DEPA format
	 *
	 * @param p
	 * @return
	 */
    private Part convertFromDEPA(Part p) {
        for (int i = 0; i < p.length(); i++) {
            p.getPhrase(i).getNote(0).setPitch(this.tm.getABS(p.getPhrase(i).getNote(0).getPitch()));
        }
        return p;
    }

    public boolean isDEPA() {
        return this.DEPACHE_MODE;
    }

    public void initToTonalManager(TonalManager t) {
        setScale(t.getScaleType());
        this.getTonalManager().setRoot(t.getRoot());
    }

    public int[] getMIDIControlTypes() {
        return this.midiControlTypes;
    }
}
