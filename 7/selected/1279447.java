package arrays.liste;

public class MeineArrayList {

    private String[] speicher;

    private int anzahl;

    public MeineArrayList() {
        speicher = new String[3];
        anzahl = 0;
    }

    public boolean add(String str) {
        if (anzahl == speicher.length) grow();
        speicher[anzahl] = str;
        anzahl++;
        return true;
    }

    public String get(int index) {
        if (index >= 0 && index < anzahl) return speicher[index]; else {
            System.out.println("ung�ltiger Index");
            return null;
        }
    }

    public int size() {
        return anzahl;
    }

    public boolean remove(int index) {
        if (index >= 0 && index < anzahl) {
            for (int i = index; i < anzahl - 1; i++) {
                speicher[i] = speicher[i + 1];
            }
            anzahl--;
            return true;
        } else return false;
    }

    public int indexOf(String str) {
        for (int index = 0; index < anzahl; index++) {
            if (str.equals(speicher[index])) return index;
        }
        return -1;
    }

    public boolean contains(String str) {
        return (indexOf(str) >= 0);
    }

    private void grow() {
        String[] newArray = new String[speicher.length * 2];
        for (int i = 0; i < anzahl; i++) newArray[i] = speicher[i];
        speicher = newArray;
    }
}
