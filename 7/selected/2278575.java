package bomb.logic;

import org.newdawn.slick.SlickException;
import bomb.utils.PlaceTest;

public class Desfazer {

    private static String move[] = new String[20];

    private static Bomba[] bomb = new Bomba[20];

    public static void setBomb(Bomba bomba) {
        System.arraycopy(bomb, 0, bomb, 1, 19);
        bomb[0] = bomba;
    }

    public static void subBomb() {
        for (int k = 0; k < 19; k++) {
            bomb[k] = bomb[k + 1];
            bomb[19] = null;
        }
    }

    public static void setMove(String valor) {
        System.arraycopy(move, 0, move, 1, 19);
        move[0] = valor;
    }

    public static void subMove() {
        for (int k = 0; k < 19; k++) {
            move[k] = move[k + 1];
            move[19] = null;
        }
    }

    public static void undo(Personagem hero, Bomba[] bombs, Lugar[] places) throws SlickException {
        if ("heroEsq".equals(move[0])) {
            hero.addX(40);
            hero.subMovs();
            hero.setImg("esq0" + (hero.getMovs() % 3) + ".png");
            subMove();
        } else if ("heroDir".equals(move[0])) {
            hero.addX(-40);
            hero.subMovs();
            hero.setImg("dir0" + (hero.getMovs() % 3) + ".png");
            subMove();
        } else if ("heroCima".equals(move[0])) {
            hero.addY(40);
            hero.subMovs();
            hero.setImg("cima0" + (hero.getMovs() % 3) + ".png");
            subMove();
        } else if ("heroBaixo".equals(move[0])) {
            hero.addY(-40);
            hero.subMovs();
            hero.setImg("baixo0" + (hero.getMovs() % 3) + ".png");
            subMove();
        } else if ("bombEsq".equals(move[0])) {
            if (PlaceTest.test(places, bomb[0].getX(), bomb[0].getY())) {
                Lugar lugar = PlaceTest.qualLugar(places, bomb[0].getX(), bomb[0].getY());
                bomb[0].setImg(0);
                lugar.unDone();
            }
            if (PlaceTest.test(places, bomb[0].getX() + 40, bomb[0].getY())) {
                Lugar lugar = PlaceTest.qualLugar(places, bomb[0].getX() + 40, bomb[0].getY());
                bomb[0].setImg(1);
                lugar.Done();
            }
            bomb[0].addX(40);
            hero.addX(40);
            hero.subMovs();
            hero.setImg("esq0" + (hero.getMovs() % 3) + ".png");
            subMove();
            subBomb();
        } else if ("bombDir".equals(move[0])) {
            if (PlaceTest.test(places, bomb[0].getX(), bomb[0].getY())) {
                Lugar lugar = PlaceTest.qualLugar(places, bomb[0].getX(), bomb[0].getY());
                bomb[0].setImg(0);
                lugar.unDone();
            }
            if (PlaceTest.test(places, bomb[0].getX() - 40, bomb[0].getY())) {
                Lugar lugar = PlaceTest.qualLugar(places, bomb[0].getX() - 40, bomb[0].getY());
                bomb[0].setImg(1);
                lugar.Done();
            }
            bomb[0].addX(-40);
            hero.addX(-40);
            hero.subMovs();
            hero.setImg("dir0" + (hero.getMovs() % 3) + ".png");
            subMove();
            subBomb();
        } else if ("bombCima".equals(move[0])) {
            if (PlaceTest.test(places, bomb[0].getX(), bomb[0].getY())) {
                Lugar lugar = PlaceTest.qualLugar(places, bomb[0].getX(), bomb[0].getY());
                bomb[0].setImg(0);
                lugar.unDone();
            }
            if (PlaceTest.test(places, bomb[0].getX(), bomb[0].getY() + 40)) {
                Lugar lugar = PlaceTest.qualLugar(places, bomb[0].getX(), bomb[0].getY() + 40);
                bomb[0].setImg(1);
                lugar.Done();
            }
            bomb[0].addY(40);
            hero.addY(40);
            hero.subMovs();
            hero.setImg("cima0" + (hero.getMovs() % 3) + ".png");
            subMove();
            subBomb();
        } else if ("bombBaixo".equals(move[0])) {
            if (PlaceTest.test(places, bomb[0].getX(), bomb[0].getY())) {
                Lugar lugar = PlaceTest.qualLugar(places, bomb[0].getX(), bomb[0].getY());
                bomb[0].setImg(0);
                lugar.unDone();
            }
            if (PlaceTest.test(places, bomb[0].getX(), bomb[0].getY() - 40)) {
                Lugar lugar = PlaceTest.qualLugar(places, bomb[0].getX(), bomb[0].getY() - 40);
                bomb[0].setImg(1);
                lugar.Done();
            }
            bomb[0].addY(-40);
            hero.addY(-40);
            hero.subMovs();
            hero.setImg("baixo0" + (hero.getMovs() % 3) + ".png");
            subMove();
            subBomb();
        }
    }
}
