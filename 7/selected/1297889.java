package client;

public class Sorozat {

    Ko kovek[] = new Ko[14];

    int db = 0;

    public Sorozat() {
    }

    public Sorozat(String s) {
    }

    public void add(Ko ko) {
        kovek[db++] = ko;
    }

    public void remove(Ko ko) {
        for (int i = 0; i < db; i++) if (kovek[i] == ko) {
            db--;
            for (int j = i; j < db; j++) kovek[j] = kovek[j + 1];
            break;
        }
    }

    public void rendez() {
        for (int i = 0; i < db; i++) for (int j = i + 1; j < db; j++) if (kovek[i].nagyobb(kovek[j])) {
            Ko temp = kovek[i];
            kovek[i] = kovek[j];
            kovek[j] = temp;
        }
    }

    public boolean check() {
        boolean igen1 = true;
        for (int i = 0; i < db; i++) for (int j = i + 1; j < db; j++) if (!kovek[i].azonos_szamu(kovek[j])) igen1 = false;
        boolean igen2 = true;
        for (int i = 0; i < db; i++) for (int j = i + 1; j < db; j++) if (!kovek[i].azonos_szinu(kovek[j])) igen2 = false;
        boolean igen3 = true;
        for (int i = 0; i < db; i++) for (int j = i + 1; j < db; j++) if (kovek[i].azonos_szinu(kovek[j])) igen3 = false;
        boolean igen4 = true;
        for (int i = 0; i < db - 1; i++) if (!kovek[i].szomszedos(kovek[i + 1])) igen4 = false;
        return ((igen2 && igen4) || (igen1 && igen3));
    }
}
