package skat09;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Observable;
import java.util.SortedSet;
import java.util.TreeSet;
import skat09.spielart.Farbspiel;
import skat09.spielart.Spielartbezeichnung;
import skat09.spieler.Position;
import skat09.spielkarte.Farbe;
import skat09.spielkarte.Spielkarte;
import skat09.spielkarte.Wert;
import skat09.test.interfaces.IMenschlicherSpieler;
import skat09.test.interfaces.ISpielart;
import skat09.test.interfaces.ISpieler;

/**
 * Die Klasse Tisch bildet den momentanen Spielzustand ab und wird durch den
 * Controller gesteuert.
 * 
 * MVC: Der Tisch ist das Model, er erweitert die Klasse Observable. Eventuell
 * m&uuml;ssen noch Methoden &uuml;berschrieben werden.
 * 
 * @author Ann-Christine Kycler, Sebastian Schlatow, Mathias Stoislow, Martin
 *         Bruhns
 * @version 03.07.2009
 */
public class Tisch extends Observable {

    /**
	 * Der erste Spieler am Tisch
	 */
    private ISpieler spieler1;

    /**
	 * Der zweite Spieler am Tisch
	 */
    private ISpieler spieler2;

    /**
	 * Der dritte Spieler am Tisch
	 */
    private ISpieler spieler3;

    /**
	 * Die aktuell gespielte Spielart
	 */
    private ISpielart spielart;

    /**
	 * Das gesamte Kartenspiel
	 */
    private ArrayList<Spielkarte> deck;

    /**
	 * Karten die auf dem Tisch liegen
	 */
    private Spielkarte[] gespielteKarten;

    /**
	 * Die Karten, die im Skat liegen
	 */
    private Spielkarte[] skat;

    /**
	 * True, wenn Handspiel gespielt wird
	 */
    private boolean handspiel;

    /**
	 * True, wenn Ouvert gespielt wird
	 */
    private boolean ouvert;

    /**
	 * True, wenn Schneider angesagt wurde
	 */
    private boolean schneider;

    /**
	 * True, wenn Schwarz angesagt wurde
	 */
    private boolean schwarz;

    /**
	 * Der aktuell gebotene Reizwert
	 */
    private int reizwert;

    /**
	 * Falls der Reizagent genutzt wird, wird hier der max. Reizwert bis zu dem
	 * der Spieler mitgeht gespeichert.
	 */
    private int reizagentWert;

    /**
	 * Skatvariante: Raeuberskat oder normales Skat oder Skat mit Ramsch und
	 * Bock?
	 */
    private Spielvariante variante;

    /**
	 * Alle Reizwerte die vorkommen koennen
	 */
    private SortedSet<Integer> reizwerte;

    /**
	 * z&auml;hlt die bisher gespielten Spiele mit
	 */
    private int anzahlSpiele;

    /**
	 * Die Tr&uuml;mpfe, mit denen der Alleinspieler spielt
	 */
    private Spielkarte[] truempfe = new Spielkarte[12];

    /**
	 * True, wenn die Situation Spaltarsch aufgetreten ist
	 */
    private boolean spaltarsch;

    /**
	 * True, wenn ein Bockspiel gespielt wird
	 */
    private boolean bock;

    /**
	 * Die noch zu spielenden Ramschspiele
	 */
    private int ramschrunden;

    /**
	 * Die noch zu spielenden Bockspiele
	 */
    private int bockrunden;

    /**
	 * True, wenn Sechserskat gespielt wird
	 */
    private boolean sechserskat;

    /**
	 * Gewinngrenze ist bei 32 Karten 61, beim Sechserskat 73
	 */
    private int gewinngrenze;

    /**
	 * Spaltarschwert ist bei 32 Karten 60, beim Sechserskat 72
	 */
    private int spaltarschwert;

    /**
	 * Schneidergrenze ist bei 32 Karten 30, beim Sechserskat 36
	 */
    private int schneidergrenze;

    /**
	 * Schneidergrenze ist bei 32 Karten 90, beim Sechserskat 108
	 */
    private int gegnerschneider;

    /**
	 * Maximale Augenzahl ist bei 32 Karten 120, beim Sechserskat 144
	 */
    private int maximaleaugen;

    /**
	 * Anzahl der Karten, die ein Spieler am Ende haben muss, wenn er alle
	 * Stiche gewonnen hat. Bei 32 Karten 30, beim Sechserskat 33
	 */
    private int allestiche;

    /**
	 * Gibt an, ob das letzte Spiel ueberreizt war.
	 */
    private boolean ueberreizt;

    /**
	 * Enth&auml;lt so viele Eintr&auml;ge, wie Spiele gespielt wurden. F&uuml;r
	 * jedes Spiel ist notiert, ob das Spiel &uuml;berreizt war oder nicht.
	 */
    public ArrayList<Boolean> ueberreizliste;

    /**
	 * Enth&auml;lt so viele Eintr&auml;ge, wie Spiele gespielt wurden. F&uuml;r
	 * jedes Spiel ist notiert, wie viele Augen gewonnen wurden.
	 */
    public ArrayList<Integer> augenliste;

    /**
	 * Enth&auml;lt so viele Eintr&auml;ge, wie Spiele gespielt wurden. F&uuml;r
	 * jedes Spiel ist notiert, wie viele Punkte gewonnen wurden.
	 */
    public ArrayList<Integer> punkteliste;

    /**
	 * Enth&auml;lt so viele Eintr&auml;ge, wie Spiele gespielt wurden. F&uuml;r
	 * jedes Spiel ist notiert, wie hoch der Grundwert des Spiels war.
	 */
    public ArrayList<Integer> grundwertliste;

    /**
	 * Instanziert einen Tisch
	 */
    public Tisch() {
        super();
        anzahlSpiele = 0;
        gespielteKarten = new Spielkarte[3];
        skat = new Spielkarte[3];
        deck = new ArrayList<Spielkarte>();
        grundwertliste = new ArrayList<Integer>();
        augenliste = new ArrayList<Integer>();
        punkteliste = new ArrayList<Integer>();
        ueberreizliste = new ArrayList<Boolean>();
        reizwerte = new TreeSet<Integer>();
        reizwert = 18;
        reizagentWert = 0;
        handspiel = false;
        schneider = false;
        schwarz = false;
        ouvert = false;
        sechserskat = false;
        gewinngrenze = 61;
        spaltarschwert = 60;
        schneidergrenze = 30;
        gegnerschneider = 90;
        maximaleaugen = 120;
        allestiche = 30;
        reizwerteErzeugen();
    }

    /**
	 * Liefert den 1. Spieler zur&uuml;ck.
	 * 
	 * @return spieler1
	 */
    public ISpieler getSpieler1() {
        return spieler1;
    }

    /**
	 * Liefert den 2. Spieler zur&uuml;ck.
	 * 
	 * @return spieler2
	 */
    public ISpieler getSpieler2() {
        return spieler2;
    }

    /**
	 * Liefert den 3. Spieler zur&uuml;ck.
	 * 
	 * @return spieler3
	 */
    public ISpieler getSpieler3() {
        return spieler3;
    }

    /**
	 * Liefert die bereits auf dem Tisch liegenden Karten zur&uuml;ck.
	 * 
	 * @return auf dem Tisch liegende Karten
	 */
    public Spielkarte[] getGespielteKarten() {
        return gespielteKarten;
    }

    /**
	 * Gib das Deck des Tisches zur&uuml;ck.
	 * 
	 * @return deck des Tisches
	 */
    public ArrayList<Spielkarte> getDeck() {
        return deck;
    }

    /**
	 * Liefert alle existierenden Reizwerte in einem SortedSet zur&uuml;ck.
	 * 
	 * @return SortedSet (sortierte Reizwerte, die nicht doppelt vorkommen
	 *         k&ouml;nnen)
	 */
    public SortedSet getReizwerte() {
        return reizwerte;
    }

    /**
	 * Gibt den Skat des Tisches aus.
	 * 
	 * @return Skat vom Tisch
	 */
    public Spielkarte[] getSkat() {
        return skat;
    }

    /**
	 * Liefert die Variable handspiel zur&uuml;ck.
	 * 
	 * @return true, falls handspiel
	 */
    public boolean getHandspiel() {
        return handspiel;
    }

    /**
	 * Liefert die aktuell gesetzte Spielart zur&uuml;ck.
	 * 
	 * @return spielart der aktuellen Runde
	 */
    public ISpielart getSpielart() {
        return spielart;
    }

    /**
	 * Liefert die Spielvariante.
	 * 
	 * @return gew&auml;tle variante
	 */
    public Spielvariante getVariante() {
        return variante;
    }

    /**
	 * Liefert den aktuell gesetzten Reizwert zur&uuml;ck.
	 * 
	 * @return aktueller reizwert
	 */
    public int getReizwert() {
        return reizwert;
    }

    /**
	 * Liefert den maximalen Wert, den ein Spieler, der den Reizagenten benutzt,
	 * zu reizen bereit ist.
	 * 
	 * @return maximalWert
	 */
    public int getReizagentWert() {
        return reizagentWert;
    }

    /**
	 * Liefert zur&uuml;ck, ob das letzte Spiel 60-60 war.
	 * 
	 * @return true, falls letztes Spiel 60-60 war.
	 */
    public boolean getSpaltarsch() {
        return spaltarsch;
    }

    /**
	 * @return Die Anzahl der verbleibenden Bockspiele
	 */
    public int getBockrunden() {
        return bockrunden;
    }

    /**
	 * @return Die Anzahl der verbleibenden Ramschspiele
	 */
    public int getRamschrunden() {
        return ramschrunden;
    }

    /**
	 * @return bock Ob das Spiel ein Bockspiel ist oder nicht.
	 */
    public boolean getBock() {
        return bock;
    }

    /**
	 * Die Methode gibt die Anzahl der Spiele zur&uuml;ck
	 * 
	 * @return anzahlSpiele Die Anzahl der Spiele
	 */
    public int getAnzahlSpiele() {
        return anzahlSpiele;
    }

    /**
	 * Liefert die truempfe Variable zur&uuml;ck.
	 * 
	 * @return truempfe
	 */
    public Spielkarte[] getTruempfe() {
        return truempfe;
    }

    /**
	 * Die Methode gibt an, ob Sechserskat gespielt wird oder nicht
	 * 
	 * @return sechserskat Ob Sechserskat gespielt wird
	 */
    public boolean getSechserskat() {
        return sechserskat;
    }

    /**
	 * Ouvert ist true, falls der Alleinspieler Ouvert spielt.
	 * 
	 * @return ouvert
	 */
    public boolean getOuvert() {
        return ouvert;
    }

    /**
	 * Schwarz ist true, falls der Alleinspieler Schwarz angesagt hat.
	 * 
	 * @return schwarz
	 */
    public boolean getSchwarz() {
        return schwarz;
    }

    /**
	 * Schneider ist true, falls der Alleinspieler Schneider angesagt hat.
	 * 
	 * @return schneider
	 */
    public boolean getSchneider() {
        return schneider;
    }

    /**
	 * Gibt die ArrayList ueberreizliste zur&uuml;ck
	 * 
	 * @return ueberreizliste
	 */
    public ArrayList<Boolean> getUeber() {
        return ueberreizliste;
    }

    /**
	 * Gibt die ArrayList augenliste zur&uuml;ck
	 * 
	 * @return augenliste
	 */
    public ArrayList<Integer> getAugenListe() {
        return augenliste;
    }

    /**
	 * Gibt die ArrayList punkteliste zur&uuml;ck
	 * 
	 * @return punkteliste
	 */
    public ArrayList<Integer> getPunkteListe() {
        return punkteliste;
    }

    /**
	 * Gibt die ArrayList grundwertliste zur&uuml;ck
	 * 
	 * @return grundwertliste
	 */
    public ArrayList<Integer> getGrundwertListe() {
        return grundwertliste;
    }

    /**
	 * @param ueberreizt
	 *            the ueberreizt to set
	 */
    public void setUeberreizt(boolean ueberreizt) {
        this.ueberreizt = ueberreizt;
    }

    /**
	 * @return the ueberreizt
	 */
    public boolean istUeberreizt() {
        return ueberreizt;
    }

    /**
	 * @param sechserskat
	 *            the sechserskat to set
	 */
    public void setSechserskat(boolean sechserskat) {
        this.sechserskat = sechserskat;
        gewinngrenze = 73;
        spaltarschwert = 72;
        schneidergrenze = 36;
        gegnerschneider = 108;
        maximaleaugen = 144;
        allestiche = 33;
    }

    /**
	 * Setzt den ersten Spieler neu.
	 * 
	 * @param spieler1
	 *            - der neue Spieler
	 */
    public void setSpieler1(ISpieler spieler1) {
        this.spieler1 = spieler1;
    }

    /**
	 * Setzt den zweiten Spieler neu.
	 * 
	 * @param spieler2
	 *            - der neue Spieler
	 */
    public void setSpieler2(ISpieler spieler2) {
        this.spieler2 = spieler2;
    }

    /**
	 * Setzt den dritten Spieler neu.
	 * 
	 * @param spieler3
	 *            - der neue Spieler
	 */
    public void setSpieler3(ISpieler spieler3) {
        this.spieler3 = spieler3;
    }

    /**
	 * Setzt das schneider-flag mit dem &uuml;bergebenen Wert.
	 * 
	 * @param wert
	 *            - true falls gesetzt
	 */
    public void setSchneider(boolean wert) {
        this.schneider = wert;
    }

    /**
	 * Setzt das schwarz-flag mit dem &uuml;bergebenen Wert.
	 * 
	 * @param wert
	 *            - true falls gesetzt
	 */
    public void setSchwarz(boolean wert) {
        this.schwarz = wert;
    }

    /**
	 * Setzt das ouvert-flag mit dem &uuml;bergebenen Wert.
	 * 
	 * @param wert
	 *            - true falls gesetzt
	 */
    public void setOuvert(boolean wert) {
        this.ouvert = wert;
    }

    /**
	 * Setzt das gespielteKartenArray.
	 * 
	 * @param gespielteKarten
	 *            - ein Array, dass die drei gespielten Karten.
	 */
    public void setGespielteKarten(Spielkarte[] gespielteKarten) {
        this.gespielteKarten = gespielteKarten;
        super.setChanged();
        super.notifyObservers(gespielteKarten);
    }

    /**
	 * Legt zwei Karten in den Skat.
	 * 
	 * @param skatkarten
	 *            - die in den Skat gelegt werden sollen.
	 */
    public void setSkat(Spielkarte[] skatkarten) {
        skat = skatkarten;
    }

    /**
	 * Legt die Spielart der aktuellen Runde fest.
	 * 
	 * @param spielart
	 *            f&uuml;r die akutelle Runde
	 */
    public void setSpielart(ISpielart spielart) {
        this.spielart = spielart;
    }

    public void setVariante(Spielvariante variante) {
        this.variante = variante;
    }

    /**
	 * Setzt den Reizwert des Tisches.
	 * 
	 * @param wert
	 *            - reizwert
	 */
    public void setReizwert(int wert) {
        this.reizwert = wert;
    }

    /**
	 * Setzt den Reizagentwert des Tisches.
	 * 
	 * @param wert
	 *            - maximal gereizter Wert
	 */
    public void setReizagentWert(int wert) {
        reizagentWert = wert;
    }

    /**
	 * Setzt den booleanWert für Handspiel.
	 */
    public void setHandspiel(boolean wert) {
        this.handspiel = wert;
    }

    /**
	 * Setzt die Variable spaltarsch. Soll nur vom Controller auf false gesetzt
	 * werden, nachdem sie gelesen wurde.
	 * 
	 * @param b
	 */
    public void setSpaltarsch(boolean b) {
        spaltarsch = b;
    }

    /**
	 * @param ramschrunden
	 *            Die Anzahl der verbleibenden Bockrunden.
	 */
    public void setRamschrunden(int ramschrunden) {
        this.ramschrunden = ramschrunden;
    }

    /**
	 * @param bockrunden
	 *            Die Anzahl der verbleibenden Bockrunden.
	 */
    public void setBockrunden(int bockrunden) {
        this.bockrunden = bockrunden;
    }

    /**
	 * @param bock
	 *            , ist Spiel Bockspiel oder nicht.
	 */
    public void setBock(boolean bock) {
        this.bock = bock;
    }

    /**
	 * setzt die truempfe Variable neu.
	 * 
	 * @param truempfe
	 *            - die neuen truempfe
	 */
    public void setTruempfe(Spielkarte[] truempfe) {
        this.truempfe = truempfe;
    }

    /**
	 * Erstellt ein neues Deck mit 32 Karten. Jede Karte darf nur einmal
	 * vorkommen.
	 */
    public void erstelleDeck() {
        Spielkarte karte;
        deck.clear();
        for (Farbe farbe : Farbe.values()) {
            for (Wert wert : Wert.values()) {
                karte = new Spielkarte(farbe, wert);
                deck.add(karte);
                if ((karte.getWert() == Wert.SECHS) && (sechserskat == false)) {
                    deck.remove(karte);
                }
            }
        }
    }

    /**
	 * Mischt das &uuml;bergebene Deck und liefert es zur&uuml;ck.
	 */
    public void mischeDeck() {
        Collections.shuffle(deck);
    }

    /**
	 * Wertet einen gespielten Stich aus und gibt den Gewinner zur&uuml;ck.
	 * 
	 * @param gespielteKarten
	 *            - im Stich enthaltene Karten@return das neue Deck bestehend
	 *            aus 32 Karten in einer ArrayList
	 * @param spielart
	 *            - &uuml;bergibt die Spielart
	 * @return Spieler, der den Stich gewonnen hat
	 */
    public ISpieler stichAuswerten(ISpielart spielart, Spielkarte[] gespielteKarten) throws NullPointerException {
        ISpieler stichGewinner = null;
        Spielkarte hoechsteKarte = null;
        hoechsteKarte = spielart.hoehereKarte(gespielteKarten[0], gespielteKarten[1]);
        hoechsteKarte = spielart.hoehereKarte(hoechsteKarte, gespielteKarten[2]);
        stichGewinner = hoechsteKarte.getBesitzer();
        return stichGewinner;
    }

    /**
	 * Eine bestimmte Anzahl an Karten in ein Blatt reintun.
	 * 
	 * @param blatt
	 *            - in das die Karten rein sollen
	 * @param kartenAnzahl
	 *            - Anzahl der Karten, die in das Blatt rein sollen
	 */
    private void kartenInsBlatt(ArrayList<Spielkarte> blatt, int kartenAnzahl) {
        for (int i = 0; i < kartenAnzahl; i++) {
            blatt.add(deck.remove(0));
        }
    }

    /**
	 * Entnimmt die Spielkarten aus dem vom Tisch erzeugten Deck und verteilt
	 * sie nach den Vorgaben der internationalen Skatordnung an die Spieler und
	 * in den Skat.
	 */
    public void kartenAusteilen() {
        anzahlSpiele = anzahlSpiele + 1;
        ArrayList<Spielkarte> blatt1 = new ArrayList<Spielkarte>();
        ArrayList<Spielkarte> blatt2 = new ArrayList<Spielkarte>();
        ArrayList<Spielkarte> blatt3 = new ArrayList<Spielkarte>();
        int[] kartenAnzahl = new int[3];
        kartenAnzahl[0] = 3;
        kartenAnzahl[1] = 4;
        kartenAnzahl[2] = 3;
        if (sechserskat) {
            kartenAnzahl[2] = 4;
        }
        for (int i = 0; i < 3; i++) {
            kartenInsBlatt(blatt1, kartenAnzahl[i]);
            kartenInsBlatt(blatt2, kartenAnzahl[i]);
            kartenInsBlatt(blatt3, kartenAnzahl[i]);
            if (i == 0) {
                skat[0] = deck.remove(0);
                skat[1] = deck.remove(0);
                if (sechserskat) {
                    skat[2] = deck.remove(0);
                }
            }
        }
        spieler1.setBlatt(blatt1);
        spieler2.setBlatt(blatt2);
        spieler3.setBlatt(blatt3);
    }

    /**
	 * Addiert ein neues Spiel zu anzahlSpiele.
	 */
    public void addAnzahlSpiele() {
        anzahlSpiele = anzahlSpiele + 1;
    }

    /**
	 * Weist jedem Blatt der Spieler den jeweiligen Spieler als Besitzer zu.
	 */
    public void kartenBesitzergeben() {
        ArrayList<Spielkarte> blatt1 = spieler1.getBlatt();
        for (Spielkarte karte : blatt1) {
            karte.setBesitzer(spieler1);
        }
        ArrayList<Spielkarte> blatt2 = spieler2.getBlatt();
        for (Spielkarte karte : blatt2) {
            karte.setBesitzer(spieler2);
        }
        ArrayList<Spielkarte> blatt3 = spieler3.getBlatt();
        for (Spielkarte karte : blatt3) {
            karte.setBesitzer(spieler3);
        }
    }

    /**
	 * Diese Methode liefert den Alleinspieler zur&uuml;ck.
	 * 
	 * @return der Alleinspieler
	 */
    public ISpieler ermittleAlleinspieler() {
        ISpieler alleinSpieler = null;
        if (spieler1.getIstAlleinspieler()) {
            alleinSpieler = spieler1;
        } else if (spieler2.getIstAlleinspieler()) {
            alleinSpieler = spieler2;
        } else if (spieler3.getIstAlleinspieler()) {
            alleinSpieler = spieler3;
        }
        return alleinSpieler;
    }

    /**
	 * Diese Methode ermittelt den Mitspieler zur&uuml;ck.
	 * 
	 * @return der Mitspieler
	 */
    public ISpieler ermittleMitspieler(ISpieler spieler) {
        ISpieler mitspieler = null;
        String spielername = spieler.getName();
        String alleinspielername = ermittleAlleinspieler().getName();
        if (!spieler1.getName().equals(spielername) && !spieler1.getName().equals(alleinspielername)) {
            mitspieler = spieler1;
        } else if (!spieler2.getName().equals(spielername) && !spieler2.getName().equals(alleinspielername)) {
            mitspieler = spieler2;
        } else {
            mitspieler = spieler3;
        }
        return mitspieler;
    }

    /**
	 * Setzt die Mitspieler in den Spielern, die nicht Alleinspieler sind.
	 */
    public void mitspielerSetzen() {
        if (spielart.getSpielart() != Spielartbezeichnung.RAMSCH) {
            if (!spieler1.getIstAlleinspieler()) {
                spieler1.setMitspieler(ermittleMitspieler(spieler1));
            } else {
                spieler1.setMitspieler(null);
            }
            if (!spieler2.getIstAlleinspieler()) {
                spieler2.setMitspieler(ermittleMitspieler(spieler2));
            } else {
                spieler2.setMitspieler(null);
            }
            if (!spieler3.getIstAlleinspieler()) {
                spieler3.setMitspieler(ermittleMitspieler(spieler3));
            } else {
                spieler3.setMitspieler(null);
            }
        } else {
            spieler1.setMitspieler(null);
            spieler2.setMitspieler(null);
            spieler3.setMitspieler(null);
        }
    }

    /**
	 * PositionInitialisieren weist jedem Spieler eine Anfangsposition am Tisch
	 * zu.
	 */
    public void positionInitialisieren() {
        spieler1.setPosition(Position.VORHAND);
        spieler2.setPosition(Position.MITTELHAND);
        spieler3.setPosition(Position.HINTERHAND);
    }

    /**
	 * Diese Methode sorgt daf&uuml;r, dass nach einem Spiel die Positionen der
	 * Spieler gewechselt werden.
	 */
    public void positionWechseln() {
        Position position = spieler1.getPosition();
        switch(position) {
            case VORHAND:
                spieler1.setPosition(Position.HINTERHAND);
                spieler2.setPosition(Position.VORHAND);
                spieler3.setPosition(Position.MITTELHAND);
                break;
            case MITTELHAND:
                spieler1.setPosition(Position.VORHAND);
                spieler2.setPosition(Position.MITTELHAND);
                spieler3.setPosition(Position.HINTERHAND);
                break;
            case HINTERHAND:
                spieler1.setPosition(Position.MITTELHAND);
                spieler2.setPosition(Position.HINTERHAND);
                spieler3.setPosition(Position.VORHAND);
                break;
        }
    }

    /**
	 * Bef&uuml;llt das Reizwerte-Array mit allen Reizwerten, die vorkommen
	 * k&ouml;nnen.
	 */
    public void reizwerteErzeugen() {
        for (int i = 18; i <= 162; i += 9) {
            reizwerte.add(i);
        }
        for (int i = 20; i <= 180; i += 10) {
            reizwerte.add(i);
        }
        for (int i = 22; i <= 198; i += 11) {
            reizwerte.add(i);
        }
        for (int i = 24; i <= 216; i += 12) {
            reizwerte.add(i);
        }
        reizwerte.add(23);
        reizwerte.add(35);
        reizwerte.add(46);
        reizwerte.add(59);
        reizwerte.add(240);
        reizwerte.add(264);
        reizwerte.add(171);
        reizwerte.add(190);
        reizwerte.add(209);
        reizwerte.add(228);
    }

    /**
	 * Ermittelt den Vorhandspieler und liefert ihn zur&uuml;ck.
	 * 
	 * @return den Vorhandspieler
	 */
    public ISpieler getVorhand() {
        ISpieler vorhand;
        if (spieler1.getPosition() == Position.VORHAND) {
            vorhand = spieler1;
        } else if (spieler2.getPosition() == Position.VORHAND) {
            vorhand = spieler2;
        } else {
            vorhand = spieler3;
        }
        return vorhand;
    }

    /**
	 * Ermittelt den Mittelhandspieler und liefert ihn zur&uuml;ck.
	 * 
	 * @return den Mittelhandspieler
	 */
    public ISpieler getMittelhand() {
        ISpieler mittelhand;
        if (spieler1.getPosition() == Position.MITTELHAND) {
            mittelhand = spieler1;
        } else if (spieler2.getPosition() == Position.MITTELHAND) {
            mittelhand = spieler2;
        } else {
            mittelhand = spieler3;
        }
        return mittelhand;
    }

    /**
	 * Ermittelt den Hinterhandspieler und liefert ihn zur&uuml;ck.
	 * 
	 * @return den Hinterhandspieler
	 */
    public ISpieler getHinterhand() {
        ISpieler hinterhand = null;
        if (spieler1.getPosition() == Position.HINTERHAND) {
            hinterhand = spieler1;
        } else if (spieler2.getPosition() == Position.HINTERHAND) {
            hinterhand = spieler2;
        } else {
            hinterhand = spieler3;
        }
        return hinterhand;
    }

    /**
	 * Die Methode erwartet einen Reizwert und gibt den darauf folgenden
	 * Reizwert zur&uuml;ck. Falls es keinen h&ouml;heren Reizwert gibt, wird
	 * der Eingabewert zur&uuml;ckgegeben.
	 * 
	 * @param reizwert
	 * @return der folgende Reizwert
	 */
    public int naechstHoehererReizwert(int reizwert) {
        int ergebnis = reizwert;
        for (Integer element : reizwerte) {
            if (element > reizwert) {
                ergebnis = element;
                break;
            }
        }
        return ergebnis;
    }

    /**
	 * Liefert zu einem Reizwert den n&auml;chst kleineren Reizwert zur&uuml;ck.
	 * 
	 * @param reizwert
	 *            - reizwert, zudem der kleinere Wert gesucht ist
	 * @return kleineren Reizwert
	 */
    public int naechstNiedrigererReizwert(int reizwert) {
        int ergebnis = reizwert;
        for (int i = reizwert - 1; i >= 18; i--) {
            if (reizwerte.contains(i)) {
                ergebnis = i;
                break;
            }
        }
        return ergebnis;
    }

    /**
	 * Ermittelt ob der neue Reizwert existiert und tats&auml;chlich h&ouml;her
	 * ist als der alte Reizwert.
	 * 
	 * @param alterWert
	 *            - zuvor gereizter Wert
	 * @param neuerWert
	 *            - vom Spieler neu vorgeschlagener Reizwert
	 * @return true - falls der neue Reizwert g&uuml;ltig ist
	 */
    public boolean pruefeNeuenReizwert(int alterWert, int neuerWert) {
        boolean ergebnis = false;
        if (neuerWert == 0) {
            ergebnis = true;
        } else if (neuerWert > alterWert) {
            for (Integer element : reizwerte) {
                if (element == neuerWert) {
                    ergebnis = true;
                }
            }
        }
        return ergebnis;
    }

    /**
	 * Diese Methode liefert den Folgespielers eines Spielers zur&uuml;ck, wenn
	 * im Uhrzeigersinn gespielt wird.
	 * 
	 * @param spieler
	 *            - aktueller Spieler
	 * @return der n&auml;chste Spieler
	 */
    public ISpieler naechsterSpieler(ISpieler spieler) {
        ISpieler ergebnis = null;
        if (spieler.getPosition() == Position.VORHAND) {
            ergebnis = getMittelhand();
        } else if (spieler.getPosition() == Position.MITTELHAND) {
            ergebnis = getHinterhand();
        } else {
            ergebnis = getVorhand();
        }
        return ergebnis;
    }

    /**
	 * Diese Methode liefert den menschlichen Spieler zur&uuml;ck.
	 */
    public ISpieler gibMenschlicherSpieler() {
        ISpieler ergebnis = null;
        if (spieler1 instanceof IMenschlicherSpieler) {
            ergebnis = spieler1;
        } else if (spieler2 instanceof IMenschlicherSpieler) {
            ergebnis = spieler2;
        } else {
            ergebnis = spieler3;
        }
        return ergebnis;
    }

    /**
	 * Bestimmt ob der Alleinspieler das Spiel gewonnen hat. Ausserdem werden
	 * zus&auml;tzlich weitere Variablen f&uers Ramschen gesetzt, falls
	 * notwendig.
	 * 
	 * @return true, falls der &uuml;bergebene Spieler das Spiel gewonnen hat
	 */
    public boolean spielAuswerten() {
        boolean gewonnen = false;
        if (spielart.getSpielart() != Spielartbezeichnung.RAMSCH) {
            ArrayList<Spielkarte> temp = new ArrayList<Spielkarte>();
            temp = ermittleAlleinspieler().getStiche();
            int augen = werteAugen(temp);
            int punkte = wertePunkte(augen);
            augenliste.add(augen);
            punkteliste.add(punkte);
            if (punkte > 0) {
                gewonnen = true;
            }
            if (punkte == spaltarschwert) {
                spaltarsch = true;
                ramschrunden = 3;
                bockrunden = 3;
            }
            if (spieler1.getIstAlleinspieler() == true) {
                spieler1.neuerEintrag(punkte);
            } else {
                spieler1.neuerEintrag(0);
            }
            if (spieler2.getIstAlleinspieler() == true) {
                spieler2.neuerEintrag(punkte);
            } else {
                spieler2.neuerEintrag(0);
            }
            if (spieler3.getIstAlleinspieler() == true) {
                spieler3.neuerEintrag(punkte);
            } else {
                spieler3.neuerEintrag(0);
            }
        } else {
            gewonnen = ramschAuswertung();
        }
        if (ueberreizt) {
            ueberreizliste.add(true);
        } else {
            ueberreizliste.add(false);
        }
        return gewonnen;
    }

    /**
	 * Ist die Spielart ein Ramsch, wird diese zur Wertung der Punkte gebraucht.
	 * Die Methode schreibt die gewonnenen Punkte in das Spielearray der
	 * Spieler.
	 * 
	 * @return Gibt true zur&uuml;ck, falls der menschliche Spieler 0 oder 120
	 *         Punkte erreicht hat.
	 */
    private boolean ramschAuswertung() {
        boolean gewonnen = false;
        ISpieler[] spieler = new ISpieler[3];
        spieler[0] = spieler1;
        spieler[1] = spieler2;
        spieler[2] = spieler3;
        int bock = 1;
        if (this.bock) {
            bock = 2;
        }
        spieler = sortiereSpielerRamsch(spieler);
        ArrayList<Spielkarte> skat = new ArrayList<Spielkarte>();
        skat.add(this.skat[0]);
        skat.add(this.skat[1]);
        if (sechserskat) {
            skat.add(this.skat[2]);
        }
        int skataugen = werteAugen(skat);
        spieler = entscheideRamsch(spieler, skataugen, bock);
        for (int i = 0; i < spieler.length; i++) {
            if (spieler[i].getName() == spieler1.getName()) {
                spieler1.setSpiele(spieler[i].getSpiele());
            }
            if (spieler[i].getName() == spieler2.getName()) {
                spieler2.setSpiele(spieler[i].getSpiele());
            }
            if (spieler[i].getName() == spieler3.getName()) {
                spieler3.setSpiele(spieler[i].getSpiele());
            }
        }
        int l = gibMenschlicherSpieler().getSpiele().size();
        if (gibMenschlicherSpieler().getSpiele().get(l - 1) >= 0) {
            gewonnen = true;
        }
        return gewonnen;
    }

    /**
	 * Erstellt ein Array, dass alle Spieler enthaelt, aufsteigend sortiert, der
	 * Spieler mit den wenigsten Augen zuerst.
	 * 
	 * @param spieler
	 *            - array mit allen Spielern
	 * @return das sortierte Array
	 */
    public ISpieler[] sortiereSpielerRamsch(ISpieler[] spieler) {
        for (int i = 0; i < spieler.length; i++) {
            for (int j = 0; j < spieler.length - 1; j++) {
                if (werteAugen(spieler[j].getStiche()) > werteAugen(spieler[j + 1].getStiche())) {
                    ISpieler a = spieler[j];
                    spieler[j] = spieler[j + 1];
                    spieler[j + 1] = a;
                }
            }
        }
        return spieler;
    }

    /**
	 * Entscheidet, welcher Spieler das Spiel gewonnen hat und verteilt die
	 * Punkte.
	 * 
	 * @param spieler
	 *            Bekommt das sortierte Array mit den Spielern
	 * @param skataugen
	 *            Die Augen, die im Skat lagen
	 * @param bock
	 *            Ob Bock gespielt wird oder nicht
	 * @return Das ver&auml;nderte Spielerarray
	 */
    public ISpieler[] entscheideRamsch(ISpieler[] spieler, int skataugen, int bock) {
        grundwertliste.add(0);
        if (werteAugen(spieler[2].getStiche()) == maximaleaugen) {
            spieler[2].getSpiele().add(maximaleaugen * bock);
            augenliste.add(maximaleaugen);
            punkteliste.add(maximaleaugen * bock);
            spieler[1].getSpiele().add(0);
            spieler[0].getSpiele().add(0);
        } else if (werteAugen(spieler[0].getStiche()) == 0) {
            spieler[2].getSpiele().add(-((werteAugen(spieler[2].getStiche()) + skataugen) * 2) * bock);
            augenliste.add((werteAugen(spieler[2].getStiche()) + skataugen));
            punkteliste.add(-((werteAugen(spieler[2].getStiche()) + skataugen) * 2) * bock);
            spieler[1].getSpiele().add(0);
            spieler[0].getSpiele().add(0);
        } else {
            spieler[2].getSpiele().add(-((werteAugen(spieler[2].getStiche()) + skataugen)) * bock);
            augenliste.add((werteAugen(spieler[2].getStiche()) + skataugen));
            punkteliste.add(-(werteAugen(spieler[2].getStiche()) + skataugen) * bock);
            spieler[1].getSpiele().add(0);
            spieler[0].getSpiele().add(0);
        }
        return spieler;
    }

    /**
	 * Berechnet die aktuelle Punktzahl eines Spielers aus seinem Spiele-Array.
	 * 
	 * @param spieler
	 *            - spieler dessen Punkte berechnet werden sollen
	 * @return Punktzahl des Spielers
	 */
    public int getAktuellePunkte(ISpieler spieler) {
        int ergebnis = 0;
        for (int i = 0; i < spieler.getSpiele().size(); i++) {
            ergebnis += spieler.getSpiele().get(i);
        }
        return ergebnis;
    }

    /**
	 * Berechnet, wie h&auml;ufig ein Spieler Alleinspieler war und gibt die
	 * Prozentzahl aus.
	 * 
	 * @param spieler
	 *            - spieler dessen Prozentwert gesucht ist
	 * @return Prozent Alleinspieler
	 */
    public int getProzentAllein(ISpieler spieler) {
        int erg = 0;
        for (int i = 0; i < spieler.getSpiele().size(); i++) {
            if (spieler.getSpiele().get(i) != 0) {
                erg = erg + 1;
            }
        }
        return ((100 * erg) / spieler.getSpiele().size());
    }

    /**
	 * Berechnet, wie h&auml;ufig ein Spieler Alleinspieler war.
	 * 
	 * @param spieler
	 *            - Spieler dessen Anzahl der Alleinspiele gesucht ist
	 * @return Anzahl der Spiele
	 */
    public int getAnzahlAllein(ISpieler spieler) {
        int erg = 0;
        for (int i = 0; i < spieler.getSpiele().size(); i++) {
            if (spieler.getSpiele().get(i) != 0) {
                erg = erg + 1;
            }
        }
        return erg;
    }

    /**
	 * Ermittelt die Punkte eines Spielers nach Spielabschluss.
	 * 
	 * 
	 * @param augenzahl
	 *            Zahl der Augen, die der Spieler bekommen hat
	 * @return Punkte des Spielers
	 */
    public int wertePunkte(int augenzahl) {
        int erg = 0;
        ueberreizt = false;
        erg = punkteVarianten(erg, augenzahl);
        if (variante == Spielvariante.RAMSCHBOCK && bock == true) {
            erg = erg * 2;
        }
        if (checkVerloren(augenzahl)) {
            erg = erg * (-2);
        }
        if (variante == Spielvariante.SKAT || variante == Spielvariante.RAMSCHBOCK) {
            if (ueberreizCheck(erg) != 0 && reizwert != 0) {
                erg = ueberreizCheck(erg);
            }
        }
        return erg;
    }

    public int punkteVarianten(int erg, int augenzahl) {
        if (spielart.getSpielart() == Spielartbezeichnung.NULL) {
            grundwertliste.add(23);
            erg = punkteNullspiel();
        }
        if (spielart.getSpielart() == Spielartbezeichnung.GRAND) {
            grundwertliste.add(24);
            erg = punkteGrandspiel(augenzahl);
        }
        if (spielart.getSpielart() == Spielartbezeichnung.FARBE) {
            erg = punkteFarbspiel(augenzahl);
        }
        return erg;
    }

    /**
	 * Ermittelt die Augen eines Spielers nach Spielschluss.
	 * 
	 * @param stiche
	 *            - Stiche, die der Spieler gewonnen hat
	 * @return Augen des Spielers
	 */
    public int werteAugen(ArrayList<Spielkarte> stiche) {
        int erg = 0;
        for (int i = 0; i < stiche.size(); i++) {
            if (stiche.get(i).getWert() == Wert.ASS) {
                erg += 11;
            } else if (stiche.get(i).getWert() == Wert.ZEHN) {
                erg += 10;
            } else if (stiche.get(i).getWert() == Wert.BUBE) {
                erg += 2;
            } else if (stiche.get(i).getWert() == Wert.DAME) {
                erg += 3;
            } else if (stiche.get(i).getWert() == Wert.KOENIG) {
                erg += 4;
            } else if (stiche.get(i).getWert() == Wert.SECHS) {
                erg += 6;
            }
        }
        return erg;
    }

    /**
	 * Berechnet die Gewinnstufe des abgeschlossenen Spiels. (Erster Teil)
	 * 
	 * @param augenzahl
	 *            Bekommt die erreichte Augenzahl &uuml;bergeben.
	 * @return Die Gewinnstufe
	 */
    public int berechneStufe(int augenzahl) {
        int stufe = 1;
        if (handspiel == true) {
            stufe = 2;
        }
        if ((augenzahl > gegnerschneider) || (augenzahl < schneidergrenze)) {
            stufe = stufe + 1;
        }
        if ((augenzahl == maximaleaugen && ermittleAlleinspieler().getStiche().size() == allestiche) || (augenzahl == 0 && ermittleAlleinspieler().getStiche().size() == 0)) {
            stufe = stufe + 1;
        }
        stufe = berechnestufe2(stufe);
        return stufe;
    }

    /**
	 * Berechnet Gewinnstufe des abgeschlossenen Spiels. (2. Teil)
	 * 
	 * @param stufe
	 *            - bisher berechnete Stufe
	 * @return fertig berechnete Stufe
	 */
    private int berechnestufe2(int stufe) {
        if (schneider == true) {
            stufe = stufe + 1;
        }
        if (schwarz == true) {
            stufe = stufe + 1;
        }
        if (ouvert == true) {
            stufe = stufe + 1;
        }
        return stufe;
    }

    /**
	 * Die Methode pr&uuml;ft, ob ein Spiel gewonnen wurde oder nicht.
	 * 
	 * @param augenzahl
	 *            Die erreichte Augenzahl des Alleinspielers
	 * @return false f&uuml;r gewonnen, true f&uuml;r verloren
	 */
    public boolean checkVerloren(int augenzahl) {
        boolean verloren = false;
        if (spielart.getSpielart() == Spielartbezeichnung.NULL) {
            verloren = nullVerloren();
        } else if (spielart.getSpielart() != Spielartbezeichnung.NULL) {
            verloren = anderesSpielVerloren(augenzahl);
        }
        return verloren;
    }

    /**
	 * Die Methode pr&uuml;ft, ob ein Nullspiel gewonnen wurde oder nicht.
	 * 
	 * @return false f&uuml;r gewonnen, true f&uuml;r verloren
	 */
    public boolean nullVerloren() {
        boolean verloren = false;
        if (ermittleAlleinspieler().getStiche().size() > 0) {
            verloren = true;
        }
        return verloren;
    }

    /**
	 * Die Methode pr&uuml;ft, ob ein Farbspiel oder Grandspiel gewonnen wurde
	 * oder nicht.
	 * 
	 * @param augenzahl
	 *            - die erreichte Augenzahl des Alleinspielers
	 * @return false f&uuml;r gewonnen, true f&uuml;r verloren
	 */
    public boolean anderesSpielVerloren(int augenzahl) {
        boolean verloren = false;
        if (augenzahl < gewinngrenze) {
            verloren = true;
        } else if (schneider && augenzahl <= gegnerschneider) {
            verloren = true;
        } else if (schwarz && ermittleAlleinspieler().getStiche().size() < allestiche) {
            verloren = true;
        } else if (ouvert && ermittleAlleinspieler().getStiche().size() < allestiche) {
            verloren = true;
        }
        return verloren;
    }

    /**
	 * Die Methode pr&uuml;ft, ob sich der Alleinspieler &uuml;berreizt hat. Die
	 * Methode wird zur Auswertung ben&ouml;tigt.
	 * 
	 * @param punkte
	 *            Die erreichten Punkte werden &uuml;bergeben
	 * @return 0 f&uuml;r nicht &uuml;berreizt, die negative Punktzahl, falls
	 *         &uuml;berreizt wurde.
	 */
    public int ueberreizCheck(int punkte) {
        int zwierg = 0;
        int erg = 0;
        int stufe = 1;
        if (spielart.getSpielart() == Spielartbezeichnung.GRAND) {
            zwierg = 24;
        }
        if (spielart.getSpielart() == Spielartbezeichnung.FARBE) {
            Farbspiel spiel = (Farbspiel) spielart;
            zwierg = spiel.getTrumpffarbe().wert();
        }
        if (punkte < 0) {
            punkte = Math.abs(punkte / 2);
        }
        stufe = ueberreizcheck2(stufe);
        if (((Math.abs(ermittleAlleinspieler().spitzenZahl()) + stufe) * zwierg) < reizwert && spielart.getSpielart() != Spielartbezeichnung.NULL) {
            if (reizwert > punkte) {
                setUeberreizt(true);
                while (reizwert > erg) {
                    erg = erg + zwierg;
                }
            }
        }
        return (-erg) * 2;
    }

    /**
	 * Zweiter Teil des &Uuml;berreizchecks. Es werden zus&auml;tzlich
	 * schneider, schwarz, ouvert und handspiel &uuml;berpr&uuml;ft.
	 * 
	 * @param stufe
	 *            - vorher berechnete Stufe
	 * @return berechnete Stufe
	 */
    private int ueberreizcheck2(int stufe) {
        if (schneider) {
            stufe = stufe + 1;
        }
        if (schwarz) {
            stufe = stufe + 1;
        }
        if (handspiel) {
            stufe = stufe + 1;
        }
        if (ouvert) {
            stufe = stufe + 1;
        }
        return stufe;
    }

    /**
	 * Gibt die Anzahl der gewonnenen Spiele als Alleinspieler zur&uuml;ck.
	 * 
	 * @param spieler
	 *            - spieler, dessen gewonnen Spiele ausgegeben werden sollen
	 * @return Anzahl der gewonnen Spiele
	 */
    public int anzahlderGewinne(ISpieler spieler) {
        int ergebnis = 0;
        for (int i = 0; i < spieler.getSpiele().size(); i++) {
            if (spieler.getSpiele().get(i) > 0) {
                ergebnis++;
            }
        }
        return ergebnis;
    }

    /**
	 * Berechnet die Punkte, die ein Spieler f&uuml;r sein Nullspiel
	 * erh&auml;lt.
	 * 
	 * @return gewonnene Punkte
	 */
    public int punkteNullspiel() {
        int punkte = 23;
        if (handspiel == true && ouvert == true) {
            punkte = 59;
        } else if (handspiel == true) {
            punkte = 35;
        } else if (ouvert == true) {
            punkte = 46;
        }
        return punkte;
    }

    /**
	 * Berechnet wieviele Punkte ein Spieler f&uuml;r ein beendetes Grandspiel
	 * erh&auml;lt.
	 * 
	 * @param augenzahl
	 *            - Die vom Spieler gemachten Augen
	 * @return gewonnene Punkte
	 */
    public int punkteGrandspiel(int augenzahl) {
        int punkte = 0;
        punkte = (Math.abs(ermittleAlleinspieler().spitzenZahl()) + berechneStufe(augenzahl)) * 24;
        return punkte;
    }

    /**
	 * Berechnet, wieviele Punkte ein Spieler f&uuml;r ein gewonnenes Farbspiel
	 * erh&auml;lt.
	 * 
	 * @param augenzahl
	 *            - vom Spieler erreichte Augenzahl
	 * @return gewonnene Punkte
	 */
    public int punkteFarbspiel(int augenzahl) {
        int punkte = 0;
        int grundwert = 0;
        Farbspiel spiel = (Farbspiel) spielart;
        grundwert = spiel.getTrumpffarbe().wert();
        grundwertliste.add(grundwert);
        punkte = (Math.abs(ermittleAlleinspieler().spitzenZahl()) + berechneStufe(augenzahl)) * grundwert;
        return punkte;
    }
}
