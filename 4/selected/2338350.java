package net.sf.odinms.server;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleStat;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.tools.MaplePacketCreator;

/**
 *
 * @author FloppyDisk
 */
public class MapleOxQuiz {

    private int round = 1;

    private int question = 1;

    private MapleMap map = null;

    private long delay = 5000;

    private boolean isAnswered = false;

    private int expGain = 200;

    public MapleOxQuiz(String ident, MapleMap map, int round, int question) {
        this.map = map;
        this.round = round;
        this.question = question;
        this.isAnswered = false;
    }

    private boolean isCorrectAnswer(MapleCharacter chr, int answer) {
        double x = chr.getPosition().getX();
        double y = chr.getPosition().getY();
        if (x > -234 && y > -26) {
            if (answer == 0) {
                isAnswered = true;
                chr.dropMessage("Correct!");
                return true;
            }
            return false;
        } else if (x < -234 && y > -26) {
            if (answer == 1) {
                isAnswered = true;
                chr.dropMessage("Correct!");
                return true;
            }
            return false;
        }
        return false;
    }

    public void scheduleOx(final MapleMap map) {
        scheduleOx(map, delay);
    }

    private void scheduleOx(final MapleMap map, final long newDelay) {
        TimerManager.getInstance().schedule(new Runnable() {

            public void run() {
                map.broadcastMessage(MaplePacketCreator.serverNotice(6, MapleOxQuizFactory.getOXQuestion(round, question)));
                TimerManager.getInstance().schedule(new Runnable() {

                    public void run() {
                        for (MapleCharacter curChar : map.getCharacters()) {
                            if (isCorrectAnswer(curChar, MapleOxQuizFactory.getOXAnswer(round, question))) {
                                curChar.gainExp(expGain * curChar.getClient().getChannelServer().getExpRate(), true, false);
                            } else {
                                curChar.setHp(0);
                                curChar.updateSingleStat(MapleStat.HP, 0);
                            }
                        }
                        scheduleAnswer(map);
                    }
                }, 15 * 1000);
            }
        }, newDelay);
    }

    private void scheduleAnswer(final MapleMap map) {
        TimerManager.getInstance().schedule(new Runnable() {

            public void run() {
                map.broadcastMessage(MaplePacketCreator.serverNotice(6, MapleOxQuizFactory.getOXExplain(round, question - 1)));
                if (map.isOxQuiz()) {
                    scheduleOx(map, delay);
                } else {
                    map.broadcastMessage(MaplePacketCreator.serverNotice(6, "The Ox Quiz has ended."));
                    map.setOx(null);
                    map.setOxQuiz(false);
                }
            }
        }, 1000);
        doQuestion();
    }

    private void doQuestion() {
        if ((round == 1 && question == 29) || ((round == 2 || round == 3) && question == 17) || ((round == 4 || round == 8) && question == 12) || (round == 5 && question == 26) || (round == 9 && question == 44) || ((round == 6 || round == 7) && question == 16)) {
            question = 100;
        } else {
            question++;
        }
    }
}
