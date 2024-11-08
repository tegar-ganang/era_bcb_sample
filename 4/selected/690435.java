package com.lemu.music;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import jm.music.data.Part;
import jm.music.data.Score;
import jm.music.tools.Mod;
import jmms.NoteEvent;
import jmms.TickEvent;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import com.lemu.music.transform.TransformChain;
import ren.gui.ParameterMap;
import ren.io.Domable;
import ren.io.Domc;
import ren.util.PO;
import ren.util.Save;

/**
 * @author wooller
 * 
 * this contains a bunch of LParts and is used to hold
 * meta tnsformations and VGs and get scores from the
 * lpartArr
 */
public class LScore implements TransformChainHolder, Serializable, Domable {

    String name = "unnamed";

    LPart[] lpartArr;

    int lpartNum = 0;

    ParameterMap tempoParam = (new ParameterMap()).construct(0, 210, 40.0, 250.0, 125.0, "tempo");

    TransformChain tc;

    public LScore() {
        init();
    }

    private void init() {
        lpartArr = new LPart[60];
        lpartNum = 0;
        tempoParam = (new ParameterMap()).construct(0, 210, 40.0, 250.0, 125.0, "tempo");
        (tc = new TransformChain()).construct();
        s = new Score(name);
    }

    transient LPart[] pa;

    transient LPart[] p;

    /**
     * adds all the parts from the two lscores into this
     * one, used for morphing, where you need two
     * different parts for each of the parts being
     * morphed to and from
     * 
     * if if one is longer than the other, it puts two
     * lpartArr for each lpart from the one that is
     * longer
     * 
     * When it merges, it should actually retain the idchannels
     * because there is not "adding", and it copies the idchannels
     * 
     * as well as this, is performs a quicksort on each original Part
     * 
     * @param ls1
     * @param ls2
     */
    public void mergeLScores(LScore ls1, LScore ls2) {
        int len = Math.max(ls1.size(), ls2.size()) * 2;
        if (len > lpartArr.length) lpartArr = new LPart[len * 2];
        int i = 0;
        for (i = 0; i < len; i++) {
            if (i % 2 == 0) {
                if ((int) (i * 0.5) < ls1.size()) {
                    Mod.quickSort(ls1.getLPart((int) (i * 0.5)).getPart());
                    if (lpartArr[i] == null) lpartArr[i] = new LPart();
                    lpartArr[i].emptyCopyFrom(ls1.getLPart((int) (i * 0.5)));
                } else {
                    if (lpartArr[i] == null) lpartArr[i] = new LPart();
                    lpartArr[i].emptyCopyFrom(ls2.getLPart((int) (i * 0.5)));
                }
            } else {
                if ((int) (i * 0.5) < ls2.size()) {
                    Mod.quickSort(ls2.getLPart((int) (i * 0.5)).getPart());
                    if (lpartArr[i] == null) lpartArr[i] = new LPart();
                    lpartArr[i].emptyCopyFrom(ls2.getLPart((int) (i * 0.5)));
                } else {
                    if (lpartArr[i] == null) lpartArr[i] = new LPart();
                    lpartArr[i].emptyCopyFrom(ls1.getLPart((int) (i * 0.5)));
                }
            }
        }
        this.lpartNum = len;
    }

    public void record(NoteEvent e) {
        PO.p("going through the parts");
        for (int i = 0; i < this.lpartNum; i++) {
            PO.p("i = " + i + " chan = " + e.getChannel());
            if (lpartArr[i].getPart().getChannel() == e.getChannel()) {
                PO.p("in channel");
                e.getNotePhr().setStartTime(e.getNotePhr().getStartTime() % this.tc.getScopeParam().getValue());
                PO.p(e.getNotePhr().toString());
                lpartArr[i].getPart().add(e.getNotePhr());
            }
        }
    }

    public void add(Score s) {
        for (int i = 0; i < s.size(); i++) {
            add(s.getPart(i));
        }
    }

    /**
     * 
     * 
     * public LScore(int size) { for (int i = 0; i <
     * size; i++) { this.add(new LPart()); } }
     */
    public void add(LPart[] toAdd) {
        for (int i = 0; i < toAdd.length; i++) {
            this.add(toAdd[i]);
        }
    }

    public void add(LPart lpart) {
        lpart.getPart().setIdChannel(lpartNum);
        lpartArr[lpartNum++] = lpart;
    }

    public void add(Part p) {
        add((new LPart()).construct(p));
    }

    transient LPart tis1, tis2;

    public void add(LPart lpart, int pos) {
        tis1 = lpart;
        lpartNum++;
        for (int i = pos; i < lpartNum; i++) {
            tis2 = this.lpartArr[i];
            this.lpartArr[i] = tis1;
            tis1 = tis2;
        }
    }

    public void remove(int pos) {
        tis1 = null;
        for (int i = pos; i < lpartNum; i++) {
            this.lpartArr[i] = this.lpartArr[i + 1];
        }
        lpartNum--;
    }

    public void empty() {
        lpartNum = 0;
    }

    transient Score s;

    public Score getTickScore(TickEvent e) {
        s.empty();
        boolean soloDown = false;
        for (int i = 0; i < lpartNum; i++) {
            if (lpartArr[i].isSolo()) {
                soloDown = true;
                break;
            }
        }
        for (int i = 0; i < lpartNum; i++) {
            if (soloDown) {
                if (lpartArr[i].isSolo()) {
                    if (!lpartArr[i].isMute()) {
                        s.addPart(lpartArr[i].getTickPart(e));
                    }
                }
            } else {
                if (!lpartArr[i].isMute()) {
                    s.addPart(lpartArr[i].getTickPart(e));
                } else {
                }
            }
        }
        return s;
    }

    public TransformChain getTransformChain() {
        return this.tc;
    }

    public Score getJMScore() {
        return getJMScore(true);
    }

    /**
     * creates a new score, but uses the same parts
     * 
     * @return
     */
    public Score getJMScore(boolean ignoreScope) {
        Score score = new Score();
        for (int i = 0; i < lpartNum; i++) {
            if (!ignoreScope) {
                Part p = lpartArr[i].getPart().copy();
                Mod.cropFaster(p, 0., lpartArr[i].getScope().getValue());
                score.addPart(p);
            } else {
                score.addPart(lpartArr[i].getPart());
            }
        }
        score.setTempo(this.tempoParam.getValue());
        return score;
    }

    public void setJMScore(Score score) {
        lpartNum = score.getSize();
        for (int i = 0; i < lpartNum; i++) {
            if (lpartArr[i] == null) lpartArr[i] = (new LPart()).construct((Part) score.getPart(i)); else {
                lpartArr[i].reset();
                lpartArr[i].getPart().copyFrom((Part) score.getPart(i));
            }
        }
        this.tempoParam.setValue(score.getTempo());
    }

    /**
     * get the lpart at the specified position
     * 
     * @param pos
     * @return
     */
    public LPart getLPart(int pos) {
        if (pos > lpartNum) {
            try {
                (new Error("trying to get a part that doesn't exist in the lscore")).fillInStackTrace();
            } catch (Error e) {
                e.fillInStackTrace();
            }
        }
        return lpartArr[pos];
    }

    /**
     * 
     * @return
     * 
     * public LPart [] getLPartArray() { LPart [] toRet =
     * new LPart [this.lpartArr.size()];
     * lpartArr.toArray(toRet); return toRet; }
     */
    public Part getPart(int pos) {
        return this.getLPart(pos).getPart();
    }

    public ParameterMap getTempoParam() {
        return this.tempoParam;
    }

    public int size() {
        return this.lpartNum;
    }

    public void setName(String newName) {
        this.name = newName;
    }

    public String getName() {
        return this.name;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("lscore ");
        sb.append("\nname = " + getName());
        sb.append("\nsize = " + this.size());
        sb.append("\nparts : \n");
        for (int i = 0; i < size(); i++) {
            sb.append("    part " + i);
            sb.append(lpartArr[i].getPart().toString());
        }
        return sb.toString();
    }

    public String print() {
        StringBuffer sb = new StringBuffer();
        sb.append(this.toString());
        sb.append(this.tempoParam.getValue());
        for (int i = 0; i < lpartNum; i++) {
            sb.append(" /n " + lpartArr[i].print());
        }
        return sb.toString();
    }

    private void writeObject(ObjectOutputStream oos) throws Exception {
        oos.defaultWriteObject();
    }

    private void readObject(ObjectInputStream ois) throws Exception {
        ois.defaultReadObject();
        s = new Score(name);
    }

    private String lb = ("<" + this.getClass().getName() + ">");

    private String rb = ("</" + this.getClass().getName() + ">");

    public String getXML() {
        StringBuffer sb = new StringBuffer();
        sb.append(lb);
        sb.append("name = " + "\"" + this.name + "\"");
        sb.append("tempo = " + "\"" + this.tempoParam.getValue() + "\"");
        sb.append(rb);
        return sb.toString();
    }

    /**
     * loads data for this object from the string given.
     * This needs to the tag of only this class (or it
     * may find variables in other tags)
     * 
     * @param xml
     */
    public void loadXML(String xml) {
        int pos = xml.indexOf(lb);
        if (pos == -1) {
            try {
                Exception e = new Exception();
                e.fillInStackTrace();
                throw e;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.setName(Save.getStringVar(xml, "name"));
        this.tempoParam.setValue(Save.getDoubleVar(xml, "tempo"));
    }

    public void dload(Element e) {
        this.setName(e.getAttribute("name"));
        this.getTempoParam().setValue(Double.parseDouble(e.getAttribute("tempo")));
        this.tc = (TransformChain) Domc.lo((Element) (e.getElementsByTagName("tc").item(0)), TransformChain.class, e.getOwnerDocument());
        NodeList lpn = e.getElementsByTagName("lpart");
        for (int i = 0; i < lpn.getLength(); i++) {
            this.add((LPart) Domc.lo(((Element) (lpn.item(i))), LPart.class, e.getOwnerDocument()));
        }
    }

    public void dsave(Element e) {
        e.setAttribute("name", this.name);
        e.setAttribute("tempo", String.valueOf(this.tempoParam.getValue()));
        e.appendChild(Domc.sa(tc, "tc", e.getOwnerDocument()));
        for (int i = 0; i < this.lpartNum; i++) {
            e.appendChild(Domc.sa(this.lpartArr[i], "lpart", e.getOwnerDocument()));
        }
    }

    public boolean shiftDown(int i) {
        if (i >= this.size() || i <= 0) return false;
        swap(i, i - 1);
        return true;
    }

    public boolean shiftUp(int i) {
        if (i >= this.size() - 1 || i < 0) return false;
        swap(i, i + 1);
        return true;
    }

    public void swap(int i, int j) {
        LPart t = lpartArr[i];
        lpartArr[i] = lpartArr[j];
        lpartArr[j] = t;
        lpartArr[i].getPart().setIdChannel(i);
        lpartArr[j].getPart().setIdChannel(j);
    }

    public void mergeSameChan() {
        int[] sps = new int[this.lpartNum];
        for (int i = 0; i < this.lpartNum; i++) {
            sps[i] = lpartArr[i].getScope().getValueInt();
        }
        for (int i = 0; i < this.lpartNum; i++) {
            for (int j = i + 1; j < this.lpartNum; j++) {
                if (lpartArr[i].getPart().getChannel() == lpartArr[j].getPart().getChannel()) {
                    sps[j] = Math.max(sps[j], sps[i]);
                    sps[i] = sps[j];
                }
            }
        }
        int cnt = 0;
        for (int i = 0; i < this.lpartNum; i++) {
            if (lpartArr[i].getScope().getValue() < sps[i]) {
                Mod.repeatRT(lpartArr[i].getPart(), lpartArr[i].getScope().getValue(), sps[i]);
            }
            for (int j = i + 1; j < this.lpartNum; j++) {
                if (lpartArr[i].getPart().getChannel() == lpartArr[j].getPart().getChannel()) {
                    if (lpartArr[j].getScope().getValue() < sps[j]) {
                        Mod.repeatRT(lpartArr[j].getPart(), lpartArr[j].getScope().getValue(), sps[i + cnt]);
                    }
                    lpartArr[i].getPart().addPhraseList(lpartArr[j].getPart().getPhraseArray(), false);
                    Mod.quickSort(lpartArr[i].getPart());
                    this.remove(j);
                    cnt++;
                    j--;
                }
            }
        }
        updateIDChannels();
    }

    public void updateIDChannels() {
        for (int i = 0; i < this.lpartNum; i++) {
            lpartArr[i].getPart().setIdChannel(i);
        }
    }

    /**
     * splits into separate pitched parts
     * 
     * @param sel
     */
    public void splitLPart(int sel) {
        Part p = this.lpartArr[sel].getPart();
        Part[] np = new Part[p.length()];
        int ni = 0;
        np[ni] = p.copyEmpty();
        np[ni].add(p.getPhrase(0));
        for (int i = 1; i < p.length(); i++) {
            int compi = ni;
            int opitch = p.getPhrase(i).getNote(0).getPitch();
            int npitch = np[compi].getPhrase(0).getNote(0).getPitch();
            while (opitch != npitch) {
                compi--;
                if (compi == -1) break;
                npitch = np[compi].getPhrase(0).getNote(0).getPitch();
            }
            if (compi == -1) {
                np[++ni] = p.copyEmpty();
                np[ni].add(p.getPhrase(i));
            } else {
                np[compi].add(p.getPhrase(i));
            }
        }
        this.lpartArr[sel].setPart(np[0]);
        for (int i = 1; i < ni; i++) {
            this.add(np[i]);
        }
    }

    public void setLPart(LPart part, int i) {
        this.lpartArr[i] = part;
        this.lpartNum = Math.max((i + 1), lpartNum);
    }

    public void setKey(int k) {
        for (int i = 0; i < this.lpartNum; i++) {
            this.lpartArr[i].getTonalManager().setRoot(k);
        }
    }

    public void setScale(int s) {
        for (int i = 0; i < this.lpartNum; i++) {
            this.lpartArr[i].getTonalManager().setScale(s);
        }
    }
}
