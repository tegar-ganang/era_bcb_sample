package com.wxz.sanguo.game.battle;

import java.util.ArrayList;
import java.util.List;
import com.wxz.sanguo.game.soldier.Soldier;
import com.wxz.sanguo.game.soldier.SoldierAction;
import com.wxz.sanguo.game.soldier.SoldierType;
import common.Logger;

/***
 * ʿ���ѭ�����С�
 * 
 * @author wxz
 * 
 */
public class SoldierQueue {

    private static Logger logger = Logger.getLogger(SoldierQueue.class);

    private Soldier[] soldiers;

    private int len;

    private int firstSoldierId;

    public SoldierQueue(List<Soldier> attackSoldierList, List<Soldier> defendSoldierList) {
        len = attackSoldierList.size() + defendSoldierList.size();
        soldiers = new Soldier[len];
        int currentPositon = 0;
        for (Soldier soldier : attackSoldierList) {
            if (soldier.getType() == SoldierType.BU) {
                soldiers[currentPositon] = soldier;
                currentPositon++;
            }
        }
        for (Soldier soldier : defendSoldierList) {
            if (soldier.getType() == SoldierType.BU) {
                soldiers[currentPositon] = soldier;
                currentPositon++;
            }
        }
        for (Soldier soldier : attackSoldierList) {
            if (soldier.getType() == SoldierType.GO) {
                soldiers[currentPositon] = soldier;
                currentPositon++;
            }
        }
        for (Soldier soldier : defendSoldierList) {
            if (soldier.getType() == SoldierType.GO) {
                soldiers[currentPositon] = soldier;
                currentPositon++;
            }
        }
        for (Soldier soldier : attackSoldierList) {
            if (soldier.getType() == SoldierType.QI) {
                soldiers[currentPositon] = soldier;
                currentPositon++;
            }
        }
        for (Soldier soldier : defendSoldierList) {
            if (soldier.getType() == SoldierType.QI) {
                soldiers[currentPositon] = soldier;
                currentPositon++;
            }
        }
        for (Soldier soldier : attackSoldierList) {
            if (soldier.getType() == SoldierType.WU) {
                soldiers[currentPositon] = soldier;
                currentPositon++;
            }
        }
        for (Soldier soldier : defendSoldierList) {
            if (soldier.getType() == SoldierType.WU) {
                soldiers[currentPositon] = soldier;
                currentPositon++;
            }
        }
        this.firstSoldierId = soldiers[0].getId();
    }

    public List<Soldier> findSoldiersByGeneralId(int generalId) {
        List<Soldier> soldierList = new ArrayList<Soldier>();
        for (int i = 0; i < len; i++) {
            if (soldiers[i].getGeneralId() == generalId) {
                soldierList.add(soldiers[i]);
            }
        }
        return soldierList;
    }

    public List<Soldier> findSoldiersByGeneralAndType(int generalId, SoldierType type) {
        List<Soldier> soldierList = new ArrayList<Soldier>();
        for (int i = 0; i < len; i++) {
            if (soldiers[i].getGeneralId() == generalId && soldiers[i].getType() == type) {
                soldierList.add(soldiers[i]);
            }
        }
        return soldierList;
    }

    /****
	 * һ�ֽ����ˢ��״̬��
	 *  1ɾ������ʿ��
	 *   2������ͷ����ʿ�����Ϊ0����circle��
	 *   3���������佫��ID
	 */
    public int refreshStateAtTurnEnd() {
        int deadGeneralId = 0;
        for (int i = 0; i < len; i++) {
            if (soldiers[i].getNum() <= 0) {
                logger.info("ɾ������" + soldiers[i]);
                if (soldiers[i].getType() == SoldierType.WU) {
                    deadGeneralId = soldiers[i].getGeneralId();
                }
                if (soldiers[i].getId() == firstSoldierId) {
                    if (i < len - 1) {
                        firstSoldierId = soldiers[i + 1].getId();
                    } else {
                        firstSoldierId = soldiers[0].getId();
                    }
                }
                for (int j = i; j < len - 1; j++) {
                    soldiers[j] = soldiers[j + 1];
                }
                len--;
            }
        }
        if (soldiers[0].getMove() <= 0) {
            soldiers[0].setAction(SoldierAction.STOP);
            circle();
        }
        return deadGeneralId;
    }

    public List<Soldier> findAllSoldier() {
        List<Soldier> sl = new ArrayList<Soldier>();
        for (int i = 0; i < len; i++) {
            sl.add(soldiers[i]);
        }
        return sl;
    }

    /***
	 * ע������г�������2���ƶ���Ϊ0�������Ŀǰ�Ĳ�����֡�
	 * 
	 * @return
	 */
    public Soldier peek() {
        return soldiers[0];
    }

    public int size() {
        return len;
    }

    /***
	 * ����ͷ����Ԫ�أ������β��
	 */
    public void circle() {
        Soldier soldier = soldiers[0];
        for (int i = 0; i < len - 1; i++) {
            soldiers[i] = soldiers[i + 1];
        }
        soldiers[len - 1] = soldier;
        logger.info(soldier + "�������β��len=[" + this.len + "]");
    }

    /***
	 * �Ƿ����е�ʿ��ִ����һ�顣
	 * 
	 * @return
	 */
    public boolean isAllExecute() {
        return soldiers[0].getId() == firstSoldierId;
    }

    public void refreshBaseData() {
        for (Soldier soldier : this.soldiers) {
            if (soldier != null) {
                soldier.refreshBaseData();
            }
        }
    }
}
