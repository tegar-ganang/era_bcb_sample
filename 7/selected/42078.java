package com.sf.plctest.s7emulator.core;

import com.sf.plctest.s7emulator.shared.Config;
import com.sf.plctest.shared.Tools;

/**
 * Akkumulatur-Funktionalitt. Diese Klasse implementiert einen Satz aus
 * {@link shared.Const#NUMAKKUS} einzelnen 32-Bit-Akkus, auf die byteweise,
 * wortweise und doppelwortweise zugegriffen werden kann.
 * 
 * @author <a href="mailto:lst@tzi.de">Lars Stru</a>
 */
class Accu {

    /** internes Array zur Aufnahme der Akkuwerte. */
    private int[] accu = new int[Config.NUMAKKUS];

    /**
	 * Ldt einen Wert in den ersten Akku; dieser wird zuvor in den zweiten
	 * gesichert usw.. (vgl. {@link #push})
	 * 
	 * @param accu0
	 *            zu ladender Wert
	 */
    public void load(int accu0) {
        for (int i = Config.NUMAKKUS - 1; i > 0; --i) accu[i] = accu[i - 1];
        accu[0] = accu0;
    }

    /**
	 * Ldt den Wert eines Parameters in den ersten Akku; dieser wird zuvor in
	 * den zweiten gesichert usw.. (vgl. {@link #push})
	 * 
	 * @param p
	 *            Parameter, dessen Wert geladen werden soll.
	 * @see #store
	 */
    public void load(Param p) {
        load(p.getValue());
    }

    /**
	 * Schreibt den Inhalt des ersten Akkus an die durch den Parameter
	 * spezifizierte Speicherstelle.
	 * 
	 * @param p
	 *            Parameter fr Zielort.
	 * @see #load
	 */
    public void store(Param p) {
        p.setValue(accu[0]);
    }

    /**
	 * Kopiert den ersten Akku in den zweiten, nachdem der zweite in den dritten
	 * kopiert wurde usw.. Der erste Akku wird nicht verndert.
	 * 
	 * @see #load
	 * @see #enter
	 * @see #pop
	 */
    public void push() {
        for (int i = Config.NUMAKKUS - 1; i > 0; --i) accu[i] = accu[i - 1];
    }

    /**
	 * Kopiert den zweiten Akku in den ersten, den dritten in den zweiten, usw..
	 * Der letzte AKku wird nicht verndert.
	 * 
	 * @see #leave
	 * @see #push
	 */
    public void pop() {
        for (int i = 0; i < Config.NUMAKKUS - 1; ++i) accu[i] = accu[i + 1];
    }

    /**
	 * Kopiert den zweiten Akku in den dritten, nachdem der dritte in den
	 * vierten kopiert wurde usw.. Die ersten beiden Akkus werden nicht
	 * verndert.
	 * 
	 * @see #leave
	 * @see #push
	 */
    public void enter() {
        for (int i = Config.NUMAKKUS - 1; i > 1; --i) accu[i] = accu[i - 1];
    }

    /**
	 * Kopiert den dritten Akku in den zweiten, den vierten in den dritten,
	 * usw.. Der erste und letzte AKku werden nicht verndert.
	 * 
	 * @see #leave
	 * @see #pop
	 */
    public void leave() {
        for (int i = 1; i < Config.NUMAKKUS - 1; ++i) accu[i] = accu[i + 1];
    }

    /**
	 * Liest ein Byte aus dem Akku-Set.
	 * 
	 * @param accuIdx
	 *            Nummer des Akkus, zwischen 0 und {@link shared.Const#NUMAKKUS}
	 *            -1.
	 * @param byteIdx
	 *            das Byte innerhalb des Akkus (0 bis 3).
	 * @return Das betreffende Byte.
	 */
    public int getByte(int accuIdx, int byteIdx) {
        return (accu[accuIdx] >> (byteIdx * 8)) & 0xFF;
    }

    /**
	 * Liest ein Low-Word (16 Bit signed) aus dem Akku-Set.
	 * 
	 * @param accuIdx
	 *            Nummer des Akkus, zwischen 0 und {@link shared.Const#NUMAKKUS}
	 *            -1.
	 * @return Das niederwertige Wort des angegebenen Akkus. (signed mit
	 *         Vorzeichenerweiterung)
	 * @see #getShortH
	 * @see #getUShortL
	 * @see #getUShortH
	 */
    public int getShortL(int accuIdx) {
        return (short) accu[accuIdx];
    }

    /**
	 * Liest ein Low-Word (16 Bit unsigned) aus dem Akku-Set.
	 * 
	 * @param accuIdx
	 *            Nummer des Akkus, zwischen 0 und {@link shared.Const#NUMAKKUS}
	 *            -1.
	 * @return Das niederwertige Wort des angegebenen Akkus. (unsigned ohne
	 *         Vorzeichenerweiterung)
	 * @see #getShortL
	 * @see #getShortH
	 * @see #getUShortH
	 */
    public int getUShortL(int accuIdx) {
        return accu[accuIdx] & 0xFFFF;
    }

    /**
	 * Liest ein High-Word (16 Bit signed) aus dem Akku-Set.
	 * 
	 * @param accuIdx
	 *            Nummer des Akkus, zwischen 0 und {@link shared.Const#NUMAKKUS}
	 *            -1.
	 * @return Das hherwertige Wort des angegebenen Akkus. (signed mit
	 *         Vorzeichenerweiterung)
	 * @see #getShortL
	 * @see #getUShortL
	 * @see #getUShortH
	 */
    public int getShortH(int accuIdx) {
        return accu[accuIdx] >> 16;
    }

    /**
	 * Liest ein High-Word (16 Bit unsigned) aus dem Akku-Set.
	 * 
	 * @param accuIdx
	 *            Nummer des Akkus, zwischen 0 und {@link shared.Const#NUMAKKUS}
	 *            -1.
	 * @return Das hherwertige Wort des angegebenen Akkus. (unsigned ohne
	 *         Vorzeichenerweiterung)
	 * @see #getShortL
	 * @see #getShortH
	 * @see #getUShortL
	 */
    public int getUShortH(int accuIdx) {
        return accu[accuIdx] >>> 16;
    }

    /**
	 * Liest einen Akku (32 Bit) aus dem Akku-Set.
	 * 
	 * @param accuIdx
	 *            Nummer des Akkus, zwischen 0 und {@link shared.Const#NUMAKKUS}
	 *            -1.
	 * @return Der Inhalt des angegebenen Akkus.
	 */
    public int getInt(int accuIdx) {
        return accu[accuIdx];
    }

    /**
	 * Liest einen Akku als Fliekommazahl (32 Bit float) aus dem Akku-Set. Es
	 * erfolgt keine Umwandlung von int nach Float, sondern es wird diejenige
	 * Float-Zahl zurckgegeben, deren Bitmuster dem Inhalt des angegebenen Akkus
	 * entspricht.
	 * 
	 * @param accuIdx
	 *            Nummer des Akkus, zwischen 0 und {@link shared.Const#NUMAKKUS}
	 *            -1.
	 * @return Der Inhalt des angegebenen Akkus als float.
	 */
    public float getFloat(int accuIdx) {
        return Float.intBitsToFloat(accu[accuIdx]);
    }

    /**
	 * Setzt ein Byte im Akku-Set.
	 * 
	 * @param accuIdx
	 *            Nummer des Akkus, zwischen 0 und {@link shared.Const#NUMAKKUS}
	 *            -1.
	 * @param byteIdx
	 *            das Byte innerhalb des Akkus (0 bis 3).
	 * @param value
	 *            Der zu setzende Wert.
	 */
    public void setByte(int accuIdx, int byteIdx, int value) {
        byteIdx *= 8;
        accu[accuIdx] = (accu[accuIdx] & ~(0xFF << byteIdx)) | ((value & 0xFF) << byteIdx);
    }

    /**
	 * Setzt ein Low-Word (16 Bit) im Akku-Set.
	 * 
	 * @param accuIdx
	 *            Nummer des Akkus, zwischen 0 und {@link shared.Const#NUMAKKUS}
	 *            -1.
	 * @param value
	 *            Der zu setzende Wert.
	 * @return Der Parameter <code>value</code>.
	 */
    public int setShortL(int accuIdx, int value) {
        value &= 0xFFFF;
        accu[accuIdx] = (accu[accuIdx] & 0xFFFF0000) | value;
        return value;
    }

    /**
	 * Setzt ein High-Word (16 Bit) im Akku-Set.
	 * 
	 * @param accuIdx
	 *            Nummer des Akkus, zwischen 0 und {@link shared.Const#NUMAKKUS}
	 *            -1.
	 * @param value
	 *            Der zu setzende Wert.
	 */
    public void setShortH(int accuIdx, int value) {
        accu[accuIdx] = (accu[accuIdx] & 0xFFFF) | (value << 16);
    }

    /**
	 * setzt einen Akku (32 Bit) im Akku-Set.
	 * 
	 * @param accuIdx
	 *            Nummer des Akkus, zwischen 0 und {@link shared.Const#NUMAKKUS}
	 *            -1.
	 * @param value
	 *            Der zu setzende Wert.
	 * @return Der Parameter <code>value</code>.
	 */
    public int setInt(int accuIdx, int value) {
        accu[accuIdx] = value;
        return value;
    }

    /**
	 * Setzt einen Akku aus dem Akku-Set auf eine Fliekommazahl (32 Bit float).
	 * Es erfolgt keine Umwandlung von float nach int, sondern es wird diejenige
	 * int-Zahl in den Akku geschrieben, deren Bitmuster dem Inhalt des
	 * bergebenen floats entspricht.
	 * 
	 * @param accuIdx
	 *            Nummer des Akkus, zwischen 0 und {@link shared.Const#NUMAKKUS}
	 *            -1.
	 * @param value
	 *            Der zu setzende Wert.
	 */
    public void setFloat(int accuIdx, float value) {
        accu[accuIdx] = Float.floatToRawIntBits(value);
    }

    /**
	 * Wandelt in Inhalt vom ersten Akku vom BCD-Format ins Binrformat. Das
	 * Ergebnis wird bei Erfolg wieder im ersten Akku abgelegt.
	 * 
	 * @param dwordFlag
	 *            true, wenn die Umwandlung mit 32 Bit erfolgen soll.
	 * @return true, wenn die Umwandlung erfolgreich war.
	 * @see shared.Tools#convertBCD2Int
	 */
    public boolean convertBCD2Int(boolean dwordFlag) {
        Integer intptr = Tools.convertBCD2Int(getInt(0), dwordFlag);
        if (intptr == null) return false;
        if (dwordFlag) setInt(0, intptr.intValue()); else setShortL(0, intptr.intValue());
        return true;
    }

    /**
	 * Wandelt in Inhalt vom ersten Akku vom Binrformat ins BCD-Format. Das
	 * Ergebnis wird bei Erfolg wieder im ersten Akku abgelegt.
	 * 
	 * @param dwordFlag
	 *            true, wenn die Umwandlung mit 32 Bit erfolgen soll.
	 * @return true, wenn die Umwandlung erfolgreich war.
	 * @see shared.Tools#convertInt2BCD
	 */
    public boolean convertInt2BCD(boolean dwordFlag) {
        Integer intptr = Tools.convertInt2BCD(getInt(0), dwordFlag);
        if (intptr == null) return false;
        if (dwordFlag) setInt(0, intptr.intValue()); else setShortL(0, intptr.intValue());
        return true;
    }

    /**
	 * Wandelt den Inhalt des ersten Akkus in eine 32-Bit-Zahl um, in dem das
	 * High-Byte mit dem Vorzeichenbit des Low-Bytes gefllt wird.
	 * 
	 * @return true bei Erfolg
	 */
    public boolean convertShort2Int() {
        setShortH(0, (getShortL(0) & 0x8000) == 0 ? 0 : 0xFFFF);
        return true;
    }

    /**
	 * Wandelt den Integer-Inhalt des ersten Akkus durch Abschneiden der
	 * Nachkommastellen in eine Float-Zahl um.
	 * 
	 * @return true bei Erfolg
	 */
    public boolean convertInt2Float() {
        setFloat(0, getInt(0));
        return true;
    }
}
