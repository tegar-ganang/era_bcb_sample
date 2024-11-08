package a3;

/**
 * 
 * @author Timo Briddigkeit (Matrikelnummer: 2011218)
 * 
 */
public class UsedSpacePriorityList {

    private Datei[] dateien = null;

    private int size;

    public UsedSpacePriorityList(int size) {
        this.setSize(size);
        dateien = new Datei[this.size];
    }

    public boolean add(Datei datei) {
        this.sort();
        boolean added = false;
        for (int i = 0; i < this.size; i++) {
            if (dateien[i] == null && !added) {
                dateien[i] = datei;
                added = true;
            }
        }
        if (dateien[size - 1] != null) {
            if (datei.compareTo(dateien[size - 1]) > 0) {
                dateien[size - 1] = datei;
                this.sort();
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    private void sort() {
        boolean unsortiert = true;
        Datei tmp = null;
        while (unsortiert) {
            unsortiert = false;
            for (int i = 0; i < this.size - 1; i++) {
                if (dateien[i] != null && dateien[i + 1] != null) {
                    if (dateien[i].compareTo(dateien[i + 1]) < 0) {
                        tmp = dateien[i];
                        dateien[i] = dateien[i + 1];
                        dateien[i + 1] = tmp;
                        unsortiert = true;
                    }
                }
            }
        }
    }

    private void setSize(int size) {
        if (size > 0) {
            this.size = size;
        } else {
            this.size = 0;
        }
    }

    public int size() {
        return size;
    }

    public Datei get(int element) {
        if (element < size) {
            return dateien[element];
        } else {
            return null;
        }
    }
}
