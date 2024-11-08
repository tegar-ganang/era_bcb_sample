package arrays.bibliothek;

public class BuchVerwaltung {

    private Buch[] buecher;

    private int anzahl;

    public BuchVerwaltung(int kapazitaet) {
        buecher = new Buch[kapazitaet];
        anzahl = 0;
    }

    /**
     * ein Buch wird in die Verwaltung aufgenommen
     * @param b
     * @return der R�ckgabewert gibt an, ob das Buch aufgenommen werden konnte
     */
    public boolean aufnehmen(Buch b) {
        boolean aufgenommen = false;
        if (b != null) {
            if (anzahl < buecher.length) {
                buecher[anzahl] = b;
                anzahl++;
                aufgenommen = true;
            }
        }
        return aufgenommen;
    }

    /**
     * liefert Referenz auf das Buch beim �bergebenen Index
     * @param index Index des gew�nschten Buches
     * @return Referenz auf das Buch oder null, falls Index nicht g�ltig
     */
    public Buch get(int index) {
        if (index >= 0 && index < anzahl) return buecher[index]; else return null;
    }

    /**
     * Ein Buch wird ausgemustert (aus der Verwaltung entfernt)
     * @param index ist der Index des Buches, welches entfernt werden soll
     * @return der R�ckgabewert gibt an, ob tats�chlich ein Buch entfernt wurde.
     */
    public boolean ausmustern(int index) {
        boolean ausgemustert = false;
        if (index >= 0 && index < anzahl) {
            for (int i = index; i < anzahl - 1; i++) buecher[i] = buecher[i + 1];
            anzahl--;
            ausgemustert = true;
        }
        return ausgemustert;
    }

    /**
     * Ein Buch wird in der Verwaltung gesucht.
     * Wenn es gefunden wird, wird der Index (erstes Vorkommen) zur�ckgeliefert,
     * ansonsten -1;
     * @param b Referenz auf die Buch-Instanz, die gesucht werden soll
     * @return der Index der gesuchten Buch-Referenz oder -1, falls nicht gefunden
     */
    public int suchen(Buch b) {
        for (int i = 0; i < anzahl; i++) {
            if (buecher[i] == b) return i;
        }
        return -1;
    }

    /**
     * Ein Buch wird ausgemustert (aus der Verwaltung entfernt)
     * @param Referenz auf das Buch, welches ausgemustert werden soll
     * @return der R�ckgabewert gibt an, ob tats�chlich ein Buch entfernt wurde.
     */
    public boolean ausmustern(Buch b) {
        int index = suchen(b);
        if (index >= 0) {
            ausmustern(index);
            return true;
        } else return false;
    }

    /**
     * liefert die Anzahl der B�cher zur�ck, welche mehr Seiten haben als im Parameter
     * �bergeben wird.
     * @param Anzahl der Seiten
     * @return Anzahl der B�cher, welche mehr Seiten haben als im Parameter �bergeben.
     */
    public int mehrSeiten(int seiten) {
        int n = 0;
        for (int i = 0; i < anzahl; i++) {
            if (buecher[i].getSeiten() > seiten) n++;
        }
        return n;
    }

    public void liste() {
        System.out.println("============ B�cherliste =====================");
        for (int i = 0; i < anzahl; i++) System.out.println(buecher[i].getAutor() + "\t" + buecher[i].getTitel() + "\t" + buecher[i].getSeiten() + " Seiten");
    }
}
