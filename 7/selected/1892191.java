package net.sf.econtycoon.core;

import java.lang.reflect.Array;
import java.io.Serializable;
import java.util.*;
import net.sf.econtycoon.core.data.Strings;
import net.sf.econtycoon.core.data.Werte;

/**
 * Die Spielerklasse steuert alle M�glichkeiten eines Spielers.
 * Sie speichert den aktuellen Status eines Spielers
 * Hat die Subklasse AISpieler
 *
 * @author Konstantin
 */
public class Spieler implements Serializable, Comparable<Spieler> {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private String name = Strings.getString("Spieler.0");

    private int geld;

    private int besitz[] = new int[Werte.ROHSTOFFZAHL];

    private List<Industrie> indu = new LinkedList<Industrie>();

    private int ekwert[] = new int[Werte.ROHSTOFFZAHL];

    private int rohstoffwert[] = new int[Werte.ROHSTOFFZAHL];

    private Spiel spiel;

    private int lagergroesse[] = new int[4];

    private int rohstoffegesamt[] = new int[4];

    private int kredite;

    private int industriekosten;

    private int gebuehren;

    private double verbrauch[] = new double[Werte.ROHSTOFFZAHL];

    private int lagerwert;

    private float lagerpreisfaktor;

    private int rohstoffegekauft[] = new int[Werte.ROHSTOFFZAHL];

    private int rohstoffeverkauft[] = new int[Werte.ROHSTOFFZAHL];

    private int rohstoffeproduziert[] = new int[Werte.ROHSTOFFZAHL];

    private int rohstoffeverbraucht[] = new int[Werte.ROHSTOFFZAHL];

    private int[] firmenwerthistory = new int[Master.OPTIONEN.getMaximumDiagramm()];

    private int[] geldhistory = new int[Master.OPTIONEN.getMaximumDiagramm()];

    private int historycounter = 0;

    private int historyswitch = 0;

    private boolean historycountereinmaldurch = false;

    private boolean prokurist[] = new boolean[Werte.ROHSTOFFZAHL];

    private int prokuristpreis[] = new int[Werte.ROHSTOFFZAHL];

    private LinkedList<Rohstoff> warteschleife_kaufen[] = new LinkedList[Optionen.MAXIMUM_HANDELSVERZOEGERUNG];

    private LinkedList<Rohstoff> warteschleife_verkaufen[] = new LinkedList[Optionen.MAXIMUM_HANDELSVERZOEGERUNG];

    private int warteschleife_lesekopf = 0;

    /**
	 * Resetet den Spieler; setzt alle Werte auf 0 zur�ck oder auf den Startwert.
	 */
    public void reset() {
        geld = 2000;
        for (int i = 0; i < Werte.ROHSTOFFZAHL; i++) {
            besitz[i] = 0;
            ekwert[i] = 0;
            rohstoffwert[i] = 0;
            verbrauch[i] = 0;
            prokurist[i] = false;
            prokuristpreis[i] = 1000000;
            rohstoffegekauft[i] = 0;
            rohstoffeverkauft[i] = 0;
            rohstoffeproduziert[i] = 0;
            rohstoffeverbraucht[i] = 0;
        }
        for (int i = 0; i < Master.OPTIONEN.getMaximumDiagramm(); i++) {
            firmenwerthistory[i] = 0;
            geldhistory[i] = 0;
        }
        indu.clear();
        lagergroesse[0] = 2;
        lagergroesse[1] = 3;
        lagergroesse[2] = 0;
        lagergroesse[3] = 0;
        rohstoffegesamt[0] = 0;
        rohstoffegesamt[1] = 0;
        rohstoffegesamt[2] = 0;
        rohstoffegesamt[3] = 0;
        kredite = 0;
        industriekosten = 0;
        gebuehren = 0;
        lagerwert = 0;
        lagerpreisfaktor = 1;
        warteschleife_lesekopf = 0;
        for (int i = 0; i < Optionen.MAXIMUM_HANDELSVERZOEGERUNG; i++) {
            warteschleife_kaufen[i] = new LinkedList<Rohstoff>();
            warteschleife_verkaufen[i] = new LinkedList<Rohstoff>();
        }
    }

    /**
	 * Konstruiert einen neuen Spieler
	 *
	 * @param spiel the spiel
	 */
    public Spieler(Spiel spiel) {
        this.spiel = spiel;
        reset();
    }

    /**
	 * Vergleicht 2 Spieler nach dem Firmenwert (f�r die Highscore)
	 * @param vergleichsspieler Spieler, mit dem verglichen werden soll
	 * @return int +1, wenn Vergleichsspieler mehr Firmenwert hat; -1, wenn Vergleichsspieler
	 * weniger Firmenwert hat und 0 wenn beide gleich viel haben (^= Sortierung von oben nach unten)
	 */
    public int compareTo(Spieler vergleichsspieler) {
        if (getFirmenwert() < vergleichsspieler.getFirmenwert()) return 1;
        if (getFirmenwert() > vergleichsspieler.getFirmenwert()) return -1;
        return 0;
    }

    /**
	 * liefert den aktuellen Geldbesitz.
	 *
	 * @return int
	 */
    public int getGeld() {
        return geld;
    }

    /**
	 * bestimmt den aktuellen Geldbesitz.
	 *
	 * @param geld Geldwert
	 */
    private void setGeld(int geld) {
        this.geld = geld;
    }

    /**
	 * @param rohstoff
	 */
    public void kaufen(Rohstoff rohstoff) {
        if (checkPreis(rohstoff.getPreisAsInt()) && checkLager(rohstoff.getTyp())) {
            setGeld(getGeld() - rohstoff.getPreisAsInt());
            rohstoff.kaufen();
            ekwert[rohstoff.getID()] += rohstoff.getPreisAsInt();
            addKaufToQueue(rohstoff, 1);
            setRohstoffegekauft(getRohstoffegekauft(rohstoff.getID()) + 1, rohstoff.getID());
        }
    }

    public void verkaufen(Rohstoff rohstoff) {
        if (checkRohstoff(rohstoff)) {
            rohstoff.verkaufen();
            removeRohstoff(rohstoff);
            addRohstoffVerkaufen(rohstoff, 1);
            setRohstoffeverkauft(getRohstoffeverkauft(rohstoff.getID()) + 1, rohstoff.getID());
        }
    }

    /**
	 * F�gt den Rohstoff der Variable besitz_[rohstoff] hinzu (OHNE Kosten).
	 *
	 * @param rohstoff Rohstoff
	 */
    private void addRohstoff(Rohstoff rohstoff) {
        if (rohstoffegesamt[rohstoff.getTyp()] < lagergroesse[rohstoff.getTyp()]) {
            besitz[rohstoff.getID()]++;
            rohstoffegesamt[rohstoff.getTyp()]++;
        }
    }

    /**
	 * F�gt den Rohstoff der Variable besitz_[rohstoff] hinzu (OHNE Kosten).
	 *
	 * @param rohstoff Rohstoff
	 * @param menge Anzahl
	 */
    private void addRohstoff(Rohstoff rohstoff, int menge) {
        for (int i = 0; i < menge; i++) {
            if (rohstoffegesamt[rohstoff.getTyp()] < lagergroesse[rohstoff.getTyp()]) {
                besitz[rohstoff.getID()]++;
                rohstoffegesamt[rohstoff.getTyp()]++;
            }
        }
    }

    /**
	 * Zieht den Rohstoff von besitz_[rohstoff] ab (OHNE Kosten).
	 *
	 * @param rohstoff Rohstoff
	 */
    public void removeRohstoff(Rohstoff rohstoff) {
        rohstoffegesamt[rohstoff.getTyp()]--;
        ekwert[rohstoff.getID()] -= (ekwert[rohstoff.getID()] / besitz[rohstoff.getID()]);
        besitz[rohstoff.getID()]--;
    }

    /**
	 * Zieht (int Menge) Rohstoffe von besitz_[rohstoff] ab (OHNE Kosten).
	 *
	 * @param rohstoff Rohstoff
	 * @param menge Menge
	 */
    public void removeRohstoff(Rohstoff rohstoff, int menge) {
        rohstoffegesamt[rohstoff.getTyp()] -= menge;
        for (int i = 0; i < menge; i++) {
            ekwert[rohstoff.getID()] -= (ekwert[rohstoff.getID()] / besitz[rohstoff.getID()]);
            besitz[rohstoff.getID()]--;
        }
    }

    /**
	 * Liefert true, wenn von dem Rohstoff mindestens 1 im Lager ist.
	 *
	 * @param r Rohstoff
	 * @return boolean
	 */
    public boolean checkRohstoff(Rohstoff r) {
        if (besitz[r.getID()] > 0) return true; else return false;
    }

    public boolean checkPreis(int preis) {
        if (getGeld() >= preis) {
            return true;
        } else {
            return false;
        }
    }

    private void addKaufToQueue(Rohstoff r, int menge) {
        if (Master.OPTIONEN.getHandelsverzoegerung() != 0) {
            if (warteschleife_lesekopf == 0) {
                for (int i = 0; i < menge; i++) {
                    warteschleife_kaufen[Master.OPTIONEN.getHandelsverzoegerung() - 1].add(r);
                }
            } else {
                for (int i = 0; i < menge; i++) {
                    warteschleife_kaufen[warteschleife_lesekopf - 1].add(r);
                }
            }
        } else {
            addRohstoff(r, menge);
        }
    }

    private void addRohstoffVerkaufen(Rohstoff r, int menge) {
        if (Master.OPTIONEN.getHandelsverzoegerung() != 0) {
            if (warteschleife_lesekopf == 0) {
                for (int i = 0; i < menge; i++) {
                    warteschleife_verkaufen[Master.OPTIONEN.getHandelsverzoegerung() - 1].add(r);
                }
            } else {
                for (int i = 0; i < menge; i++) {
                    warteschleife_verkaufen[warteschleife_lesekopf - 1].add(r);
                }
            }
        } else {
            geld += r.getPreis() * menge;
        }
    }

    private void warteschleifeAuswerten() {
        warteschleife_lesekopf++;
        if (warteschleife_lesekopf >= Master.OPTIONEN.getHandelsverzoegerung()) {
            warteschleife_lesekopf = 0;
        }
        for (int i = 0; i < warteschleife_kaufen[warteschleife_lesekopf].size(); i++) {
            addRohstoff(warteschleife_kaufen[warteschleife_lesekopf].get(i));
        }
        warteschleife_kaufen[warteschleife_lesekopf].clear();
        for (int i = 0; i < warteschleife_verkaufen[warteschleife_lesekopf].size(); i++) {
            geld += warteschleife_verkaufen[warteschleife_lesekopf].get(i).getPreisAsInt();
        }
        warteschleife_verkaufen[warteschleife_lesekopf].clear();
    }

    /**
	 * Liefert true wenn von dem Rohstoff mindestens (int menge) im Lager sind.
	 *
	 * @param r Rohstoff
	 * @param menge Menge
	 * @return boolean
	 */
    public boolean checkRohstoff(Rohstoff r, int menge) {
        if (besitz[r.getID()] >= menge) return true; else return false;
    }

    /**
	 * Liefert den Rohstoffbesitz.
	 *
	 * @param r Rohstoff
	 * @return Menge der vorhandenen Rohstoffe im Spielerlager
	 */
    public int getRohstoffbesitz(Rohstoff r) {
        return besitz[r.getID()];
    }

    /**
	 * Steuert alle Industrie-tick()-Methoden, die Prokuristen und die Ausgaben pro Runde
	 */
    public void tick() {
        warteschleifeAuswerten();
        industrieTick();
        prokurist();
        ausgaben();
        statistiken();
    }

    private void industrieTick() {
        for (int i = 0; i < indu.size(); i++) {
            indu.get(i).tick();
            if (indu.get(i).checkProd()) {
                addRohstoff(indu.get(i).getProdukt1(), indu.get(i).getProdukt1menge());
                rohstoffeproduziert[indu.get(i).getProdukt1().getID()] += indu.get(i).getProdukt1menge();
                if (indu.get(i).hasProdukt2()) {
                    addRohstoff(indu.get(i).getProdukt2(), indu.get(i).getProdukt2menge());
                    rohstoffeproduziert[indu.get(i).getProdukt2().getID()] += indu.get(i).getProdukt2menge();
                    if (indu.get(i).hasProdukt3()) {
                        addRohstoff(indu.get(i).getProdukt3(), indu.get(i).getProdukt3menge());
                        rohstoffeproduziert[indu.get(i).getProdukt3().getID()] += indu.get(i).getProdukt3menge();
                    }
                }
            }
        }
    }

    private void statistiken() {
        historyswitch++;
        if (historyswitch == 3) {
            if (historycountereinmaldurch == false) {
                geldhistory[historycounter] = (int) getGeld();
                firmenwerthistory[historycounter] = (int) getFirmenwert();
                if (geldhistory[historycounter] == 0) {
                    geldhistory[historycounter] = -1;
                }
                if (firmenwerthistory[historycounter] == 0) {
                    firmenwerthistory[historycounter] = -1;
                }
                historycounter++;
                if (historycounter == Master.OPTIONEN.getMaximumDiagramm()) {
                    historycountereinmaldurch = true;
                }
            } else {
                for (int i = 0; i < Master.OPTIONEN.getMaximumDiagramm() - 1; i++) {
                    geldhistory[i] = geldhistory[i + 1];
                    firmenwerthistory[i] = firmenwerthistory[i + 1];
                }
                geldhistory[Master.OPTIONEN.getMaximumDiagramm() - 1] = (int) getGeld();
                firmenwerthistory[Master.OPTIONEN.getMaximumDiagramm() - 1] = (int) getFirmenwert();
                if (geldhistory[Master.OPTIONEN.getMaximumDiagramm() - 1] == 0) {
                    geldhistory[Master.OPTIONEN.getMaximumDiagramm() - 1] = -1;
                }
                if (firmenwerthistory[Master.OPTIONEN.getMaximumDiagramm() - 1] == 0) {
                    firmenwerthistory[Master.OPTIONEN.getMaximumDiagramm() - 1] = -1;
                }
            }
            historyswitch = 0;
        }
    }

    /**
	 * Ausgaben, die jede Runde gezahlt werden m�ssen
	 */
    private void ausgaben() {
        int ausgaben = 0;
        gebuehren = (int) (0.0005 * getFirmenwert() * Master.OPTIONEN.getGebuehrenprozent() * 0.01);
        if (gebuehren == 0) gebuehren = 1;
        ausgaben += gebuehren;
        industriekosten = 0;
        for (int i = 0; i < indu.size(); i++) {
            industriekosten += ((indu.get(i).getMoment_Unterhalt()));
        }
        ausgaben += industriekosten;
        ausgaben += getZinsen();
        ausgaben += getProkuristenkosten();
        geld -= ausgaben;
    }

    /**
	 * Industriekaufen. (MIT Kosten)
	 *
	 * @param ID ID der industrie
	 */
    public void industriekaufen(int ID) {
        if (getGeld() > Werte.INDUSTRIE[ID][Werte.PREIS]) {
            indu.add(new Industrie(ID, this, spiel));
            geld -= Werte.INDUSTRIE[ID][Werte.PREIS];
        }
    }

    /**
	 * Liefert true, wenn im Lager noch Platz ist.
	 *
	 * @param typ the typ
	 * @return boolean
	 */
    public boolean checkLager(int typ) {
        if (rohstoffegesamt[typ] >= lagergroesse[typ]) return false; else return true;
    }

    /**
	 * Lager bauen.
	 *
	 * @param typ the typ
	 * @param stufe the stufe
	 */
    public void lagerBauen(int typ, int stufe) {
        int preis = (int) ((Werte.LAGERPREIS[typ][stufe]) * lagerpreisfaktor);
        if (geld >= preis) {
            lagergroesse[typ] += Werte.LAGERGROESSE[typ][stufe];
            geld -= preis;
            lagerwert += preis;
            lagerpreisfaktor *= 1.02;
        }
        spiel.getMaster().getGUI().tfUpdate();
    }

    /**
	 * Gets the lagergroesse.
	 *
	 * @param typ the typ
	 * @return the lagergroesse
	 */
    public int getLagergroesse(int typ) {
        return lagergroesse[typ];
    }

    /**
	 * Gets the lagerinhalt.
	 *
	 * @param typ the typ
	 * @return the lagerinhalt
	 */
    public int getLagerinhalt(int typ) {
        return rohstoffegesamt[typ];
    }

    /**
	 * Gets the lagerpreisfaktor.
	 *
	 * @return the lagerpreisfaktor
	 */
    public float getLagerpreisfaktor() {
        return lagerpreisfaktor;
    }

    /**
	 * Spielerevents (zuk�nftig)
	 */
    public void event() {
    }

    /**
	 * Gets den Wert aller Rohstoffe eines Typs
	 *
	 * @param i ID des Rohstoffes
	 * @return the rohstoffwert
	 */
    public int getRohstoffwert(int i) {
        rohstoffwert[i] = besitz[i] * spiel.getRohstoffint(i).getPreisAsInt();
        return rohstoffwert[i];
    }

    /**
	 * Gets den Einkaufswert aller Rohstoffe eines Types
	 * Durch Industrie erworbene Rohstoffe erhalten als Ek-Wert
	 * den Preis des Rohstoffes zur Zeit der Produktion
	 *
	 * @param i RohstoffID
	 * @return Einkaufswert
	 */
    public int getEkwert(int i) {
        return ekwert[i];
    }

    /**
	 * Gets the kredite.
	 *
	 * @return the kredite
	 */
    public int getKredite() {
        return kredite;
    }

    /**
	 * Kreditaufnehmen. (immer 1000)
	 */
    public void kreditaufnehmen() {
        kredite += 1000;
        geld += 1000;
        spiel.getMaster().getGUI().tfUpdate();
    }

    /**
	 * Kreditzurueckzahlen. (immer 1000)
	 */
    public void kreditzurueckzahlen() {
        if (geld >= 1000) {
            if (kredite >= 1000) {
                kredite -= 1000;
                geld -= 1000;
            }
        }
        spiel.getMaster().getGUI().tfUpdate();
    }

    /**
	 * Gets the firmenwert.
	 *
	 * @return the firmenwert
	 */
    public int getFirmenwert() {
        int fw = geld;
        for (int i = 0; i < Werte.ROHSTOFFZAHL; i++) {
            fw += besitz[i] * spiel.getRohstoffint(i).getPreisAsInt();
        }
        for (int i = 0; i < indu.size(); i++) {
            fw += indu.get(i).getPreis();
        }
        fw -= kredite;
        fw += lagerwert;
        return fw;
    }

    /**
	 * Gets die Kosten, die alle Industrien zusammen haben
	 *
	 * @return the industriekosten
	 */
    public int getIndustriekosten() {
        return industriekosten;
    }

    /**
	 * Gets die Gebuehren (sind vom Firmenwert abh�ngig)
	 *
	 * @return the gebuehren
	 */
    public int getGebuehren() {
        return gebuehren;
    }

    /**
	 * Kalkuliert den Rohstoffverbrauch/produktion aller Industrien pro Runde
	 */
    public void calculateVerbrauch() {
        for (int i = 0; i < Array.getLength(verbrauch); i++) {
            verbrauch[i] = 0;
        }
        for (int i = 0; i < indu.size(); i++) {
            verbrauch[indu.get(i).getProdukt1().getID()] += ((indu.get(i).getProdukt1menge() * 1.0) / (indu.get(i).getDauer() * 1.0));
            if (indu.get(i).hasProdukt2()) verbrauch[indu.get(i).getProdukt2().getID()] += ((indu.get(i).getProdukt2menge() * 1.0) / (indu.get(i).getDauer() * 1.0));
            if (indu.get(i).hasProdukt3()) verbrauch[indu.get(i).getProdukt3().getID()] += ((indu.get(i).getProdukt3menge() * 1.0) / (indu.get(i).getDauer() * 1.0));
            if (indu.get(i).hasVerbrauch1()) verbrauch[indu.get(i).getVerbrauch1().getID()] -= ((1.0 * indu.get(i).getVerbrauch1menge()) / (indu.get(i).getDauer() * 1.0));
            if (indu.get(i).hasVerbrauch2()) verbrauch[indu.get(i).getVerbrauch2().getID()] -= ((1.0 * indu.get(i).getVerbrauch2menge()) / (indu.get(i).getDauer() * 1.0));
            if (indu.get(i).hasVerbrauch3()) verbrauch[indu.get(i).getVerbrauch3().getID()] -= ((1.0 * indu.get(i).getVerbrauch3menge()) / (indu.get(i).getDauer() * 1.0));
        }
    }

    /**
	 * Steuert Prokuristenhandlungen. Wenn der Preis eines Rohstoffes h�her als der im
	 * Prokuristenmen� eingestellte preis ist und der prokurist aktiviert ist, werden
	 * automatisch die Rohstoffe verkauft
	 */
    private void prokurist() {
        for (int i = 0; i < Werte.ROHSTOFFZAHL; i++) {
            if (prokurist[i]) {
                if (getRohstoffbesitz(spiel.getRohstoffint(i)) > 0) {
                    for (int j = 0; j < getRohstoffbesitz(spiel.getRohstoffint(i)); j++) {
                        if (spiel.getRohstoffint(i).getPreisAsInt() > prokuristpreis[i]) {
                            spiel.verkaufen(spiel.getRohstoffint(i), this, 1);
                        } else {
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
	 * Liefert den Rohstoffverbrauch/produktion pro Runde auf 3 Nachkommastellen genau
	 *
	 * @param rohstoffID RohstoffID
	 * @return the verbrauch
	 */
    public double getVerbrauch(int rohstoffID) {
        return (Math.round(verbrauch[rohstoffID] * 1000)) / 1000.0;
    }

    /**
	 * Gets the industrie.
	 *
	 * @param i Industrienummer
	 * @return the industrie
	 */
    public Industrie getIndustrie(int i) {
        return indu.get(i);
    }

    public void removeIndustrie(int nr) {
        indu.remove(nr);
    }

    /**
	 * Gets die Anzahl der Industrien vom Spieler
	 *
	 * @return the industriesize
	 */
    public int getIndustriesize() {
        return indu.size();
    }

    /**
	 * Gets die Anzahl an Rohstoffen, die ein Spieler von einem best. lagertyp hat
	 *
	 * @param typ Lagertyp
	 * @return Anzahl der Rohstoffe
	 */
    public int getRohstoffegesamt(int typ) {
        return rohstoffegesamt[typ];
    }

    /**
	 * Checkt, ob ein prokurist aktiviert ist
	 *
	 * @param i RohstoffID
	 * @return true, wenn der Prokurist aktiviert ist
	 */
    public boolean isProkurist(int i) {
        return prokurist[i];
    }

    /**
	 * Sets the prokurist.
	 *
	 * @param i RohstoffID
	 * @param aktiv true, wenn prokurist aktiviert sein soll
	 */
    public void setProkurist(int i, boolean aktiv) {
        prokurist[i] = aktiv;
    }

    /**
	 * Sets den Preis, ab dem der Prokurist verkaufen soll
	 *
	 * @param i RohstoffID
	 * @param preis mindestpreis
	 */
    public void setProkuristpreis(int i, int preis) {
        prokuristpreis[i] = preis;
    }

    /**
	 * Gets the prokuristenkosten.
	 *
	 * @return the prokuristenkosten
	 */
    public int getProkuristenkosten() {
        int prokuristenkosten = 0;
        for (int i = 0; i < Werte.ROHSTOFFZAHL; i++) {
            if (prokurist[i]) {
                if (Werte.ROHSTOFF[i][3] == -1) {
                    prokuristenkosten += (int) ((Werte.ROHSTOFF[i][1] / 2));
                } else {
                    prokuristenkosten += (int) (((Werte.ROHSTOFF[Werte.ROHSTOFF[i][3]][1] * Werte.ROHSTOFF[i][4]) / 200));
                }
            }
        }
        return prokuristenkosten;
    }

    /**
	 * Gets the zinsen.
	 *
	 * @return the zinsen
	 */
    public int getZinsen() {
        int zinsen = 0;
        zinsen += kredite * 0.005;
        if (geld < 0) {
            if (Master.OPTIONEN.isAlleschuldenverzinsen()) {
                zinsen += geld * -1 * 0.001;
            }
        }
        return zinsen;
    }

    /**
	 * @param name the name to set
	 */
    public void setName(String name) {
        this.name = name;
    }

    /**
	 * @return the name
	 */
    public String getName() {
        return name;
    }

    public int getRohstoffegekauft(int i) {
        return rohstoffegekauft[i];
    }

    private void setRohstoffegekauft(int rohstoffegekauft, int i) {
        this.rohstoffegekauft[i] = rohstoffegekauft;
    }

    public int getRohstoffeverkauft(int i) {
        return rohstoffeverkauft[i];
    }

    private void setRohstoffeverkauft(int rohstoffeverkauft, int i) {
        this.rohstoffeverkauft[i] = rohstoffeverkauft;
    }

    public int getRohstoffeproduziert(int i) {
        return rohstoffeproduziert[i];
    }

    public void setRohstoffeproduziert(int rohstoffeproduziert, int i) {
        this.rohstoffeproduziert[i] = rohstoffeproduziert;
    }

    public int getRohstoffeverbraucht(int i) {
        return rohstoffeverbraucht[i];
    }

    public void setRohstoffeverbraucht(int rohstoffeverbraucht, int i) {
        this.rohstoffeverbraucht[i] = rohstoffeverbraucht;
    }

    /**
	 * @return the firmenwerthistory
	 */
    public int[] getFirmenwerthistory() {
        return firmenwerthistory;
    }

    /**
	 * @return the geldhistory
	 */
    public int[] getGeldhistory() {
        return geldhistory;
    }
}
