package wb;

import static wb.Ents.*;
import static wb.Prev.*;
import static wb.Stats.*;
import static wb.Blink.*;
import static wb.Wbdefs.*;
import static wb.Wbsys.*;
import static wb.Blk.*;
import static wb.BooleanMethods.*;
import static wb.Pkt.*;
import static wb.Seg.*;
import static wb.Ent.*;

public class Del {

    public static boolean del_DeferBlockDeletes_P = false;

    public static boolean del_DeleteBck(Ent ent) {
        byte[] blk = ent_Blk(ent);
        boolean win_P = !(del_DeferBlockDeletes_P);
        if (win_P) {
            ents_EntUpdateAccess_P(ent, accwrite, accnone);
            {
                Ent prent = prevBlkEnt(ent, blk_Level(blk));
                win_P = ents_EntUpdateAccess_P(ent, accnone, accwrite);
                if (win_P && a2b(prent)) win_P = ents_EntUpdateAccess_P(prent, accread, accwrite);
                win_P = win_P && 1 == (ent_Ref(ent));
                if (win_P) {
                    if (!(atRootLevel_P(ent_Seg(ent), blk))) {
                        int skeyPos = splitKeyPos(blk);
                        if (a2b(skeyPos)) {
                            int topNum = blk_TopId(blk);
                            Seg seg = ent_Seg(ent);
                            int level = blk_Level(blk);
                            byte[] keyStr = new byte[0x100];
                            int kLen = reconThisKey(blk, skeyPos, keyStr, 0, 0x100);
                            win_P = parentDeleteUpdate_P(seg, topNum, level, ent_Id(ent), keyStr, kLen);
                        }
                    }
                    win_P = win_P && 1 == (ent_Ref(ent));
                    if (win_P) {
                        if (a2b(prent)) {
                            blk_SetNxtId(ent_Blk(prent), blk_NxtId(blk));
                            ent_SetDty(prent, true);
                            ents_EntWrite(prent);
                        }
                        win_P = blkFree(ent);
                        if (!(win_P)) dprintf(">>>>ERROR<<<< " + ("blkDelete") + ": could not free " + (seg_Id(ent_Seg(ent))) + ":" + (ent_Id(ent)) + "\n");
                    }
                }
                if (a2b(prent)) releaseEnt(prent, ent_Acc(prent));
            }
        }
        if (win_P) {
            blockDeletes = (blockDeletes) + 1;
        } else {
            deferredDeletes = 1 + (deferredDeletes);
            dprintf("Can't delete block " + (ent_Id(ent)) + "\n");
        }
        return win_P;
    }

    public static boolean parentDeleteUpdate_P(Seg seg, int topId, int level, int oldId, byte[] keyStr, int kLen) {
        int[] pkt = new int[pktSize];
        int ans = -1;
        byte[] ansStr = new byte[4];
        {
            Ent ent = findEnt(getEnt(seg, topId, accnone), 1 + (level), -1, keyStr, kLen);
            if (!(a2b(ent))) ; else if (ents_EntUpdateAccess_P(ent, accread, accwrite)) {
                ent = chainFind(ent, accwrite, keyStr, kLen, pkt);
            } else {
                releaseEnt(ent, accread);
                ent = null;
            }
            if (a2b(ent)) {
                ans = del_ChainRem(ent, keyStr, kLen, ansStr, pkt, wcbSar);
                if ((ans) >= 0) if ((oldId) != (str2long(ansStr, 0))) dprintf(">>>>ERROR<<<< " + ("parentDeleteUpdate_P") + ": bad value " + (str2long(ansStr, 0)) + " in deleted down pointer " + (oldId) + " told\n");
                releaseEnt(ent, accwrite);
            }
            if (!((a2b(ent) || (ans) >= 0))) {
                dprintf("WARNING: " + ("parentDeleteUpdate_P") + " blk=" + (seg_Id(seg)) + ":" + (oldId) + ", level=" + (level) + ", key=" + (kLen) + "\n");
                return false;
            } else return (true);
        }
    }

    public static int del_ChainRem(Ent ent, byte[] keyStr, int kLen, byte[] ansStr, int[] pkt, int wcb) {
        if ((pkt_MatchType(pkt)) == (match)) {
            int alen = success;
            if (a2b(ansStr)) alen = getThisVal(ent_Blk(ent), pkt_MatchPos(pkt), ansStr);
            blk_RemoveKeyAndVal(ent_Blk(ent), pkt_MatchPos(pkt), seg_Bsiz(ent_Seg(ent)));
            ent_SetDty(ent, true);
            if (blkEmpty_P(ent_Blk(ent)) && !(endOfChain_P(ent_Blk(ent)))) {
                del_DeleteBck(ent);
            } else {
                if ((0 != ((wcbSar) & (wcb)) || (blk_Level(ent_Blk(ent))) > (leaf))) ents_EntWrite(ent);
            }
            return alen;
        } else return notpres;
    }
}
