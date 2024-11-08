package boggle.spel;

import java.util.Random;

public class Speelveld {

    private static final int AANTAL_RIJEN = 4;

    public char[][] genereerSpeelveld(char[][] dicelijst) {
        char[][] speelveld = new char[AANTAL_RIJEN][AANTAL_RIJEN];
        int aantalvakken = AANTAL_RIJEN * AANTAL_RIJEN;
        Random gen = new Random();
        for (int i = 0; i < aantalvakken; i++) {
            int nextdicepos = gen.nextInt(dicelijst.length);
            speelveld[i / AANTAL_RIJEN][i % AANTAL_RIJEN] = willekeurigeDobbelZijde(dicelijst[nextdicepos]);
            dicelijst = herschikDobbelstenen(dicelijst, nextdicepos);
        }
        return speelveld;
    }

    private char[][] herschikDobbelstenen(char[][] dicelijst, int erasedpos) {
        char[][] arrangeddices = new char[dicelijst.length - 1][boggle.data.TextReader.AANTAL_ZIJDEN];
        int i = 0;
        while (i < erasedpos) {
            arrangeddices[i] = dicelijst[i];
            i++;
        }
        while (i < arrangeddices.length) {
            arrangeddices[i] = dicelijst[i + 1];
            i++;
        }
        return arrangeddices;
    }

    private char willekeurigeDobbelZijde(char[] dobbelsteen) {
        char randchar;
        Random gen = new Random();
        int nextval = gen.nextInt(dobbelsteen.length);
        randchar = dobbelsteen[nextval];
        return randchar;
    }
}
