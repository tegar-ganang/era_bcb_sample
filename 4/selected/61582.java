package wb;

import static wb.Ents.*;
import static wb.Stats.*;
import static wb.Blink.*;
import static wb.Del.*;
import static wb.Handle.*;
import static wb.Wbdefs.*;
import static wb.Wbsys.*;
import static wb.Blk.*;
import static wb.BooleanMethods.*;
import static wb.Pkt.*;
import static wb.Seg.*;
import static wb.Han.*;
import static wb.Ent.*;

public class Scan {

    public static int btScan(Han han, int operation, byte[] kstr1, int len1, byte[] kstr2, int len2, java.lang.reflect.Method func, int[] longTab, int[] respkt, int blkLimit) {
        LbtScan: while (true) {
            {
                int[] pkt = new int[pktSize];
                int[] opkt = new int[pktSize];
                Ent ent = null;
                byte[] vstr = new byte[0x100];
                int accmode = ((operation) == (countScan) ? accread : accwrite);
                int result = success;
                if ((len1) < -2) {
                    dprintf(">>>>ERROR<<<< " + ("btScan") + ": bad length string1 " + (len1) + "\n");
                    return argerr;
                } else if ((len2) < -1) {
                    dprintf(">>>>ERROR<<<< " + ("btScan") + ": bad length string2 " + (len2) + "\n");
                    return argerr;
                } else if ((operation) == (modifyScan) && !(a2b(func))) {
                    dprintf(">>>>ERROR<<<< " + ("btScan") + ": MODIFY-SCAN requires func be specified\n");
                    return argerr;
                } else {
                    ent = chainFindEnt(han, accmode, kstr1, len1, pkt);
                    if (a2b(ent) && a2b(blk_FindPos(ent_Blk(ent), kstr2, len2, opkt))) {
                        if ((operation) == (countScan)) {
                            Ent nent = allocateEnt();
                            entCopy(nent, ent);
                            releaseEnt(ent, accmode);
                            result = chainScan(nent, operation, pkt, opkt, kstr1, func, longTab, vstr, respkt, han_Wcb(han));
                            recycleEnt(nent);
                        } else {
                            result = chainScan(ent, operation, pkt, opkt, kstr1, func, longTab, vstr, respkt, han_Wcb(han));
                            releaseEnt(ent, accmode);
                            if ((result) > 0) {
                                result = btPut(han, kstr1, pkt_SkeyLen(respkt), vstr, result);
                                if ((result) == (success)) {
                                    pkt_SetSkeyCount(respkt, (pkt_SkeyCount(respkt)) + 1);
                                    pkt_SetSkeyLen(respkt, incrementString(kstr1, pkt_SkeyLen(respkt), 0x100));
                                    result = notpres;
                                }
                            }
                        }
                        if ((result) == (notpres) && 0 != (blkLimit)) {
                            len1 = pkt_SkeyLen(respkt);
                            blkLimit = (blkLimit) - 1;
                            continue LbtScan;
                        } else return result;
                    } else {
                        if (a2b(ent)) releaseEnt(ent, accmode);
                        remFct = 1 + (remFct);
                        return unkerr;
                    }
                }
            }
        }
    }

    public static int incrementString(byte[] str, int len, int maxlen) {
        if ((len) < (maxlen)) {
            str[len] = (byte) (0xff & 0);
            return (len) + 1;
        } else {
            int oldval = (str[(len) - 1] & 0xFF);
            str[(len) - 1] = (byte) (0xff & (1 + (oldval)));
            return len;
        }
    }

    public static int chainScan(Ent ent, int operation, int[] pkt, int[] opkt, byte[] keyStr, java.lang.reflect.Method func, int[] longTab, byte[] vstr, int[] respkt, int wcb) {
        {
            byte[] blk = ent_Blk(ent);
            int result = success;
            if ((operation) == (remScan) && !(a2b(func)) && (pkt_MatchPos(opkt)) > (pkt_MatchPos(pkt)) && (pkt_MatchPos(pkt)) == (blkDataStart) && atSplitKeyPos_P(blk, pkt_MatchPos(opkt))) {
                {
                    int keyLen = reconThisKey(blk, pkt_MatchPos(opkt), keyStr, 0, 0x100);
                    substringMove(keyStr, 0, keyLen, blk, (blkDataStart) + 2);
                    setFieldLen(blk, (blkDataStart) + 1, keyLen);
                    blk_SetEnd(blk, (blkDataStart) + 2 + (keyLen));
                }
                pkt_SetSkeyCount(respkt, (pkt_SkeyCount(respkt)) + 1);
                remCt = 1 + (remCt);
                ent_SetDty(ent, true);
                pkt_SetMatchPos(opkt, blkDataStart);
            } else {
                int oldct = pkt_SkeyCount(respkt);
                byte[] ckstr = new byte[0x100];
                int clen = 0;
                if (a2b(func)) clen = reconThisKey(blk, pkt_MatchPos(pkt), ckstr, 0, 0x100);
                pkt_SetMatchType(pkt, match);
                result = scanLoop(ent_Blk(ent), operation, pkt, opkt, func, longTab, respkt, ckstr, clen, vstr, seg_Bsiz(ent_Seg(ent)));
                if ((operation) != (countScan) && (pkt_SkeyCount(respkt)) > (oldct)) ent_SetDty(ent, true);
            }
            if ((operation) == (remScan) && blkEmpty_P(blk) && !(endOfChain_P(blk))) {
                del_DeleteBck(ent);
            } else if (ent_Dty_P(ent)) if (((operation) == (remScan) && (0 != ((wcbSar) & (wcb)) || (blk_Level(blk)) > (leaf)) || (operation) == (modifyScan) && 0 != ((wcbSap) & (wcb)))) ents_EntWrite(ent);
            if ((result) != (success)) {
                pkt_SetSkeyLen(respkt, reconThisKey(blk, pkt_MatchPos(pkt), keyStr, 0, 0x100));
                return result;
            } else if ((pkt_MatchType(opkt)) == (pastend) && !(endOfChain_P(blk))) {
                pkt_SetSkeyLen(respkt, reconThisKey(blk, pkt_MatchPos(pkt), keyStr, 0, 0x100));
                return notpres;
            } else {
                pkt_SetSkeyLen(respkt, 0);
                return success;
            }
        }
    }

    public static int scanLoop(byte[] blk, int operation, int[] pkt, int[] opkt, java.lang.reflect.Method func, int[] longTab, int[] respkt, byte[] ckstr, int clen, byte[] vstr, int blksize) {
        LscanLoop: while (true) {
            if ((pkt_MatchPos(opkt)) > (pkt_MatchPos(pkt))) {
                int oldBend = blk_End(blk);
                int nextPos = nextCnvpair(blk, pkt_MatchPos(pkt));
                int result = success;
                if (a2b(func)) {
                    int vpos = nextField(blk, 1 + (pkt_MatchPos(pkt)));
                    int vlen = fieldLen(blk, vpos);
                    substringMove(blk, (vpos) + 1, (vpos) + (vlen) + 1, vstr, 0);
                    result = intFunInvoke(func, null, new Object[] { ckstr, clen, vstr, vlen, longTab });
                }
                if ((result) >= (success)) {
                    if ((operation) == (remScan)) {
                        blk_RemoveKeyAndVal(blk, pkt_MatchPos(pkt), blksize);
                        pkt_SetSkeyCount(respkt, (pkt_SkeyCount(respkt)) + 1);
                        remCt = 1 + (remCt);
                        if ((pkt_MatchPos(opkt)) == (nextPos)) {
                            pkt_SetMatchPos(opkt, pkt_MatchPos(pkt));
                        } else pkt_SetMatchPos(opkt, (pkt_MatchPos(opkt)) - ((oldBend) - (blk_End(blk))));
                        nextPos = pkt_MatchPos(pkt);
                    } else if ((operation) == (countScan)) {
                        pkt_SetSkeyCount(respkt, (pkt_SkeyCount(respkt)) + 1);
                        pkt_SetMatchPos(pkt, nextPos);
                    } else if (a2b(changeExistingValue(blk, pkt_MatchPos(pkt), ckstr, clen, vstr, result, blksize))) {
                        pkt_SetSkeyCount(respkt, (pkt_SkeyCount(respkt)) + 1);
                        nextPos = (nextPos) - ((oldBend) - (blk_End(blk)));
                        pkt_SetMatchPos(opkt, (pkt_MatchPos(opkt)) - ((oldBend) - (blk_End(blk))));
                        pkt_SetMatchPos(pkt, nextPos);
                        result = success;
                    } else dprintf("" + ("scanLoop") + ": hit modify special case\n");
                } else if ((result) == (notpres)) pkt_SetMatchPos(pkt, nextPos);
                if (((result) == (success) || (result) == (notpres))) {
                    if (a2b(func)) {
                        clen = (fieldLen(blk, nextPos)) + (fieldLen(blk, 1 + (nextPos)));
                        substringMove(blk, (nextPos) + 2, (nextPos) + 2 + (fieldLen(blk, 1 + (nextPos))), ckstr, fieldLen(blk, nextPos));
                    }
                    continue LscanLoop;
                } else return result;
            } else return success;
        }
    }
}
