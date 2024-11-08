package wb;

import static wb.Ents.*;
import static wb.Stats.*;
import static wb.Wbdefs.*;
import static wb.Wbsys.*;
import static wb.Blk.*;
import static wb.BooleanMethods.*;
import static wb.Pkt.*;
import static wb.Ent.*;
import static wb.Seg.*;

public class Blink {

    public static void short2str(byte[] str, int pos, int cint) {
        str[(pos) + 1] = (byte) (0xff & ((cint) % 0x100));
        str[(pos) + 0] = (byte) (0xff & ((cint) / 0x100));
    }

    public static int str2short(byte[] str, int pos) {
        return ((str[(pos) + 1] & 0xFF)) + (0x100 * ((str[pos] & 0xFF)));
    }

    public static void long2str(byte[] str, int pos, int clong) {
        str[(pos) + 3] = (byte) (0xff & ((clong) % 0x100));
        str[(pos) + 2] = (byte) (0xff & (((clong) / 0x100) % 0x100));
        str[(pos) + 1] = (byte) (0xff & (((clong) / 0x10000) % 0x100));
        str[(pos) + 0] = (byte) (0xff & ((clong) / 0x1000000));
    }

    public static int str2long(byte[] str, int pos) {
        return ((str[(pos) + 3] & 0xFF)) + (0x100 * (((str[(pos) + 2] & 0xFF)) + (0x100 * (((str[(pos) + 1] & 0xFF)) + (0x100 * ((str[pos] & 0xFF)))))));
    }

    public static int setField(byte[] blk, int bPos, byte[] valStr, int fPos, int fLen) {
        setFieldLen(blk, bPos, fLen);
        substringMove(valStr, fPos, (fPos) + (fLen), blk, 1 + (bPos));
        return (fLen) + 1 + (bPos);
    }

    public static byte[] leafSplitKeyStr = { -1, (leaf) };

    public static void initLeafBlk(byte[] nblk, int bnum, int typ) {
        nblk[(blkSize) - 1] = (byte) (0xff & 0xa);
        blk_SetId(nblk, bnum);
        blk_SetNxtId(nblk, 0);
        blk_SetTopId(nblk, bnum);
        blk_SetTime(nblk, 0);
        blk_SetLevel(nblk, leaf);
        blk_SetTyp(nblk, typ);
        setFieldLen(nblk, blkDataStart, 0);
        setField(nblk, (blkDataStart) + 1, leafSplitKeyStr, 0, 2);
        blk_SetEnd(nblk, (blkDataStart) + (((typ) == (seqTyp) ? 0 : 4)));
    }

    public static void reroot(byte[] rblk, byte[] nblk, int bnum, int bsiz) {
        int rpos = blkDataStart;
        blk_SetId(nblk, bnum);
        substringMove(rblk, 4, bsiz, nblk, 4);
        blk_SetNxtId(rblk, 0);
        blk_SetLevel(rblk, (blk_Level(rblk)) + 1);
        setFieldLen(rblk, rpos, 0);
        rpos = setField(rblk, (rpos) + 1, leafSplitKeyStr, 0, 2);
        rblk[(rpos) - 1] = (byte) (0xff & ((blk_Level(rblk)) - 1));
        rpos = setField(rblk, rpos, nblk, 0, 4);
        setFieldLen(rblk, rpos, 1);
        rpos = setField(rblk, (rpos) + 1, leafSplitKeyStr, 0, 1);
        rblk[(rpos) - 1] = (byte) (0xff & (blk_Level(rblk)));
        blk_SetEnd(rblk, rpos);
    }

    public static void initNextBlk(byte[] blk, byte[] nblk) {
        nblk[(blkSize) - 1] = (byte) (0xff & 0xa);
        blk_SetNxtId(nblk, blk_NxtId(blk));
        blk_SetTopId(nblk, blk_TopId(blk));
        blk_SetLevel(nblk, blk_Level(blk));
        blk_SetTyp(nblk, blk_Typ(blk));
        blk_SetNxtId(blk, blk_Id(nblk));
        setFieldLen(nblk, blkDataStart, 0);
        setField(nblk, (blkDataStart) + 1, noByts, 0, 0);
        blk_SetEnd(nblk, (blkDataStart) + 2);
    }

    public static int splitKeyPos(byte[] blk) {
        int bEnd = blk_End(blk);
        {
            int bPos = blkDataStart;
            Llp: while (true) {
                {
                    int sPos = nextField(blk, 1 + (bPos));
                    if ((sPos) == (bEnd)) return bPos; else if ((sPos) < (bEnd)) {
                        bPos = nextCnvpair(blk, bPos);
                        continue Llp;
                    } else {
                        dprintf(">>>>ERROR<<<< " + ("splitKeyPos") + ": blk past end " + (blk_Id(blk)) + " " + (sPos) + "\n");
                        return 0;
                    }
                }
            }
        }
    }

    public static int blk_FindPos(byte[] blk, byte[] keyStr, int kLen, int[] pkt) {
        if ((kLen) < 0) {
            if ((kLen) == (endOfChain)) {
                int skpos = splitKeyPos(blk);
                pkt_Pack(pkt, (endOfChain_P(blk) ? qpastp : pastend), skpos, 0, blk_PrevKey(blk, skpos));
            } else pkt_Pack(pkt, qpastp, blkDataStart, 0, 0);
            return 1;
        } else {
            int kPos = 0;
            int bEnd = blk_End(blk);
            {
                int bPos = blkDataStart;
                int pPos = 0;
                Lchknxt: while (true) {
                    if ((fieldLen(blk, bPos)) < (kPos)) {
                        pkt_Pack(pkt, qpastp, bPos, kPos, pPos);
                        return 1;
                    } else if ((fieldLen(blk, bPos)) > (kPos)) {
                        int sPos = nextField(blk, (bPos) + 1);
                        if ((sPos) < (bEnd)) {
                            int T_bPos = nextCnvpair(blk, bPos);
                            pPos = bPos;
                            bPos = T_bPos;
                            continue Lchknxt;
                        } else if ((sPos) == (bEnd)) {
                            pkt_Pack(pkt, pastend, bPos, kPos, pPos);
                            return 1;
                        } else {
                            dprintf(">>>>ERROR<<<< " + ("blkFindPos") + "1: blk past end " + (blk_Id(blk)) + " " + (sPos) + "\n");
                            return 0;
                        }
                    } else {
                        int i = (bPos) + 2;
                        int fLen = fieldLen(blk, (bPos) + 1);
                        Lmchlp: while (true) {
                            if ((kPos) >= (kLen)) if ((fLen) > 0) {
                                pkt_Pack(pkt, pastp, bPos, kPos, pPos);
                                return 1;
                            } else {
                                int sPos = nextField(blk, (bPos) + 1);
                                if ((sPos) < (bEnd)) {
                                    pkt_Pack(pkt, match, bPos, kLen, pPos);
                                    return 1;
                                } else if ((sPos) == (bEnd)) {
                                    pkt_Pack(pkt, matchend, bPos, kPos, pPos);
                                    return 1;
                                } else {
                                    dprintf(">>>>ERROR<<<< " + ("blkFindPos") + "2: blk past end " + (blk_Id(blk)) + " " + (sPos) + "\n");
                                    return 0;
                                }
                            } else if (((fLen) <= 0 || ((blk[i] & 0xFF)) < ((keyStr[kPos] & 0xFF)))) {
                                int sPos = nextField(blk, (bPos) + 1);
                                if ((sPos) < (bEnd)) {
                                    int T_bPos = nextCnvpair(blk, bPos);
                                    pPos = bPos;
                                    bPos = T_bPos;
                                    continue Lchknxt;
                                } else if ((sPos) == (bEnd)) {
                                    pkt_Pack(pkt, pastend, bPos, kPos, pPos);
                                    return 1;
                                } else {
                                    dprintf(">>>>ERROR<<<< " + ("blkFindPos") + "3: blk past end " + (blk_Id(blk)) + " " + (sPos) + "\n");
                                    return 0;
                                }
                            } else if (((blk[i] & 0xFF)) > ((keyStr[kPos] & 0xFF))) {
                                pkt_Pack(pkt, ((kPos) > (fieldLen(blk, bPos)) ? pastp : qpastp), bPos, kPos, pPos);
                                return 1;
                            } else {
                                kPos = (kPos) + 1;
                                {
                                    i = 1 + (i);
                                    fLen = (fLen) - 1;
                                    continue Lmchlp;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static Ent chainFind(Ent ent, int accmode, byte[] keyStr, int kLen, int[] pkt) {
        LchainFind: while (true) {
            {
                byte[] blk = ent_Blk(ent);
                if (!(a2b(blk_FindPos(blk, keyStr, kLen, pkt)))) {
                    releaseEnt(ent, accmode);
                    return null;
                } else if (!(((pkt_MatchType(pkt)) == (matchend) || (pkt_MatchType(pkt)) == (pastend)))) return ent; else if (endOfChain_P(blk)) {
                    dprintf(">>>>ERROR<<<< " + ("chainFind") + ": matched or past end of chain " + (seg_Id(ent_Seg(ent))) + ":" + (ent_Id(ent)) + "\n");
                    pkt_SetMatchType(pkt, qpastp);
                    return ent;
                } else {
                    chainsToNext = 1 + (chainsToNext);
                    ent = switchEnt(ent, accmode, blk_NxtId(blk), accmode);
                    if (a2b(ent)) continue LchainFind; else return null;
                }
            }
        }
    }

    public static Ent findEnt(Ent ent, int desiredLevel, int lastLevel, byte[] keyStr, int kLen) {
        LfindEnt: while (true) {
            if (a2b(ent)) if (ents_EntUpdateAccess_P(ent, accnone, accread)) {
                byte[] blk = ent_Blk(ent);
                int blvl = blk_Level(blk);
                if ((blvl) == (desiredLevel)) return ent; else if ((blvl) < (desiredLevel)) {
                    dprintf(">>>>ERROR<<<< bad blk level " + (blvl) + " (des=" + (desiredLevel) + ") in " + (seg_Id(ent_Seg(ent))) + ":" + (ent_Id(ent)) + "\n");
                    return null;
                } else if ((lastLevel) >= 0 && (blvl) != ((lastLevel) - 1)) {
                    dprintf(">>>>ERROR<<<< bad blk level " + (blvl) + " last=" + (lastLevel) + " in " + (seg_Id(ent_Seg(ent))) + ":" + (ent_Id(ent)) + "\n");
                    return null;
                } else {
                    int[] pkt = new int[pktSize];
                    ent = chainFind(ent, accread, keyStr, kLen, pkt);
                    if (a2b(ent)) {
                        int pos = nextField(blk, 1 + (pkt_MatchPos(pkt)));
                        blk = ent_Blk(ent);
                        switch(pkt_MatchType(pkt)) {
                            case qpastp:
                            case pastp:
                                ;
                                break;
                            case match:
                                if ((blk_End(blk)) == (pos)) {
                                    pos = pkt_MatchPos(pkt);
                                } else pos = nextField(blk, pos);
                                break;
                            default:
                                pos = 0;
                                break;
                        }
                        if (a2b(pos)) {
                            pos = nextField(blk, 1 + (pkt_MatchPos(pkt)));
                            {
                                ent = switchEnt(ent, accread, ((blk_End(blk)) == (pos) ? (endOfChain_P(blk) ? str2long(blk, -6 + (pos)) : blk_NxtId(blk)) : str2long(blk, 1 + (pos))), accnone);
                                lastLevel = ((blk_End(blk)) == (pos) && !(endOfChain_P(blk)) ? (blk_Level(blk)) + 1 : blk_Level(blk));
                                continue LfindEnt;
                            }
                        } else {
                            dprintf(">>>>ERROR<<<< " + ("findEnt") + ": bad-MATCH-TYPE " + (pkt_MatchPos(pkt)) + " blk " + (seg_Id(ent_Seg(ent))) + ":" + (ent_Id(ent)) + "\n");
                            return null;
                        }
                    } else return null;
                }
            } else return null; else return null;
        }
    }

    public static int blk_PrevKey(byte[] blk, int pos) {
        {
            int bPos = blkDataStart;
            int pPos = 0;
            while (!((bPos) >= (pos))) {
                {
                    int T_bPos = nextCnvpair(blk, bPos);
                    pPos = bPos;
                    bPos = T_bPos;
                }
            }
            if ((bPos) > (pos)) {
                dprintf(">>>>ERROR<<<< " + ("blkPrevKey") + ": blk past end " + (blk_Id(blk)) + " " + (pPos) + "\n");
                return 0;
            } else return pPos;
        }
    }

    public static int getThisVal(byte[] blk, int bPos, byte[] ansStr) {
        bPos = nextField(blk, (bPos) + 1);
        {
            int alen = fieldLen(blk, bPos);
            substringMove(blk, (bPos) + 1, (bPos) + 1 + (alen), ansStr, 0);
            return alen;
        }
    }

    public static int getThisKey(byte[] blk, int bPos, byte[] keyStr, byte[] ansStr, Ent ent, int kLen, int[] pkt) {
        {
            int bEnd = blk_End(blk);
            int sPos = nextField(blk, (bPos) + 1);
            if ((sPos) < (bEnd)) {
                int fPos = fieldLen(blk, bPos);
                int fSiz = fieldLen(blk, (bPos) + 1);
                int alen = (fPos) + (fSiz);
                if ((keyStr) != (ansStr)) substringMove(keyStr, 0, fPos, ansStr, 0);
                substringMove(blk, (bPos) + 2, (bPos) + 2 + (fSiz), ansStr, fPos);
                releaseEnt(ent, accread);
                return alen;
            } else if ((sPos) != (bEnd)) {
                dprintf(">>>>ERROR<<<< " + ("chainNext") + ": blk past end " + (blk_Id(blk)) + " " + (sPos) + "\n");
                releaseEnt(ent, accread);
                return strangerr;
            } else if (endOfChain_P(blk)) {
                releaseEnt(ent, accread);
                return notpres;
            } else {
                ent = switchEnt(ent, accread, blk_NxtId(blk), accread);
                if (a2b(ent)) ent = chainFind(ent, accread, keyStr, kLen, pkt);
                if (a2b(ent)) return chainNext(ent, keyStr, kLen, ansStr, pkt); else return unkerr;
            }
        }
    }

    public static int chainNext(Ent ent, byte[] keyStr, int kLen, byte[] ansStr, int[] pkt) {
        pkt_SetBlkToCache(pkt, ent_Id(ent));
        switch(pkt_MatchType(pkt)) {
            case pastp:
            case qpastp:
                return getThisKey(ent_Blk(ent), pkt_MatchPos(pkt), keyStr, ansStr, ent, kLen, pkt);
            case match:
                return getThisKey(ent_Blk(ent), nextCnvpair(ent_Blk(ent), pkt_MatchPos(pkt)), keyStr, ansStr, ent, kLen, pkt);
            default:
                releaseEnt(ent, accread);
                return notpres;
        }
    }

    public static int blk_ChangeSize(byte[] blk, int loc, int growth, int bsiz) {
        int bEnd = blk_End(blk);
        if (0 == (growth)) return 1; else if (((bEnd) + (growth)) > (bsiz)) return 0; else if (0 > (growth)) {
            substringMoveLeft(blk, loc, bEnd, blk, (loc) + (growth));
            blk_SetEnd(blk, (bEnd) + (growth));
            return 1;
        } else {
            substringMoveRight(blk, loc, bEnd, blk, (loc) + (growth));
            blk_SetEnd(blk, (bEnd) + (growth));
            return 1;
        }
    }

    public static int blk_RemoveKeyAndVal(byte[] blk, int bPos, int bsiz) {
        int nbPos = nextCnvpair(blk, bPos);
        if ((fieldLen(blk, nbPos)) > (fieldLen(blk, bPos))) {
            int delkPos = (fieldLen(blk, nbPos)) - (fieldLen(blk, bPos));
            setFieldLen(blk, 1 + (bPos), (fieldLen(blk, 1 + (nbPos))) + (delkPos));
            return blk_ChangeSize(blk, 2 + (nbPos), ((bPos) - (nbPos)) + (delkPos), bsiz);
        } else return blk_ChangeSize(blk, nbPos, (bPos) - (nbPos), bsiz);
    }

    public static boolean deferInsertUpdates_P = false;

    public static int parentInsertUpdate(Seg seg, int topId, int level, byte[] nkeyStr, int nkLen, int nId) {
        int[] pkt = new int[pktSize];
        {
            Ent ent = findEnt(getEnt(seg, topId, accnone), 1 + (level), -1, nkeyStr, nkLen);
            Ent xent = null;
            boolean screwCase_P = false;
            byte[] blkidstr = new byte[4];
            byte[] blk = null;
            if (a2b(ent)) {
                long2str(blkidstr, 0, nId);
                if (ents_EntUpdateAccess_P(ent, accread, accwrite)) {
                    ent = chainFind(ent, accwrite, nkeyStr, nkLen, pkt);
                    blk = ent_Blk(ent);
                } else {
                    releaseEnt(ent, accread);
                    ent = null;
                }
                if (a2b(ent) && atSplitKeyPos_P(blk, pkt_MatchPos(pkt))) {
                    screwCase_P = (true);
                    xent = nextNonemptyEnt(ent_Seg(ent), blk_NxtId(blk));
                    if (!(a2b(xent))) dprintf(">>>>ERROR<<<< No next key found for index insert " + (seg_Id(ent_Seg(ent))) + ":" + (blk_Id(blk)) + "\n");
                }
                if (!(deferInsertUpdates_P) && a2b(ent) && (!(screwCase_P) || a2b(xent)) && a2b(chainPut(ent, nkeyStr, nkLen, blkidstr, 4, pkt, xent, wcbSar))) return 1; else {
                    dprintf("WARNING: " + ("parentInsertUpdate") + ": couldn't update parent n-id=" + (nId) + " nk-len=" + (nkLen) + "\n");
                    deferredInserts = 1 + (deferredInserts);
                    if (a2b(ent)) releaseEnt(ent, accwrite);
                    return 0;
                }
            } else return 0;
        }
    }

    public static boolean atSplitKeyPos_P(byte[] blk, int pos) {
        return (blk_End(blk)) == (nextField(blk, 1 + (pos)));
    }

    public static Ent nextNonemptyEnt(Seg seg, int blknum) {
        if ((blknum) <= 0) return null; else {
            Ent xent = getEnt(seg, blknum, accread);
            Lloop: while (true) {
                if (a2b(xent)) ents_EntUpdateAccess_P(xent, accread, accwrite);
                if (!(a2b(xent))) return null; else if (!(blkEmpty_P(ent_Blk(xent)))) return xent; else if (0 == (blk_NxtId(ent_Blk(xent)))) {
                    releaseEnt(xent, accwrite);
                    return null;
                } else {
                    xent = switchEnt(xent, accwrite, blk_NxtId(ent_Blk(xent)), accwrite);
                    continue Lloop;
                }
            }
        }
    }

    public static int reconThisKey(byte[] blk, int pos, byte[] keyStr, int kPos, int kLen) {
        {
            int bPos = blkDataStart;
            int kSize = 0;
            while (!((bPos) > (pos))) {
                if ((kSize) > (fieldLen(blk, bPos)) && ((blk[(bPos) + 2] & 0xFF)) <= ((keyStr[(kPos) + (fieldLen(blk, bPos))] & 0xFF))) dprintf(">>>>ERROR<<<< bad key sequence " + (blk_Id(blk)) + " @ " + (bPos) + "\n");
                kSize = (fieldLen(blk, bPos)) + (fieldLen(blk, 1 + (bPos)));
                if ((kSize) >= (kLen)) dprintf(">>>>ERROR<<<< not-enough-room " + (kLen) + "\n");
                substringMove(blk, (bPos) + 2, (bPos) + 2 + (fieldLen(blk, 1 + (bPos))), keyStr, (kPos) + (fieldLen(blk, bPos)));
                bPos = nextField(blk, 1 + (bPos));
                if ((bPos) < (blk_End(blk))) bPos = nextField(blk, bPos);
                {
                }
            }
            return kSize;
        }
    }

    public static int insertAndAdjust(byte[] blk, int bPos, int kPos, byte[] keyStr, int kLen, byte[] valStr, int vLen, int bsiz) {
        {
            int oldkPos = fieldLen(blk, bPos);
            int oldilen = fieldLen(blk, 1 + (bPos));
            int ilen = (kLen) - (oldkPos);
            if (a2b(blk_ChangeSize(blk, bPos, 2 + ((kLen) - (kPos)) + 1 + (vLen), bsiz))) {
                bPos = 1 + (bPos);
                bPos = setField(blk, bPos, keyStr, oldkPos, ilen);
                bPos = setField(blk, bPos, valStr, 0, vLen);
                setFieldLen(blk, bPos, kPos);
                setFieldLen(blk, (bPos) + 1, (oldilen) - ((kPos) - (oldkPos)));
                return 1;
            } else return 0;
        }
    }

    public static int simpleInsert(byte[] blk, int bPos, int kPos, byte[] keyStr, int kLen, byte[] valStr, int vLen, int bsiz) {
        int ilen = (kLen) - (kPos);
        if (a2b(blk_ChangeSize(blk, bPos, 3 + (vLen) + (ilen), bsiz))) {
            setFieldLen(blk, bPos, kPos);
            bPos = 1 + (bPos);
            bPos = setField(blk, bPos, keyStr, kPos, ilen);
            setField(blk, bPos, valStr, 0, vLen);
            return 1;
        } else return 0;
    }

    public static int changeExistingValue(byte[] blk, int bPos, byte[] keyStr, int kLen, byte[] valStr, int vLen, int bsiz) {
        int ovLen = 0;
        int vPos = nextField(blk, 1 + (bPos));
        ovLen = fieldLen(blk, vPos);
        if (a2b(blk_ChangeSize(blk, (vPos) + (ovLen) + 1, (vLen) - (ovLen), bsiz))) {
            setField(blk, vPos, valStr, 0, vLen);
            return 1;
        } else return 0;
    }

    public static int valLeafSplit(byte[] blk, byte[] nblk, int bPos, byte[] keyStr, int kPos, int kLen, byte[] valStr, int vLen) {
        {
            int vPos = nextField(blk, 1 + (bPos));
            int sPos = nextField(blk, vPos);
            int bEnd = blk_End(blk);
            setFieldLen(nblk, blkDataStart, 0);
            if (((bEnd) - (sPos)) > ((vPos) - (blkDataStart))) {
                int mLen = fieldLen(blk, sPos);
                int fChr = (blk[(sPos) + 2] & 0xFF);
                setFieldLen(nblk, (blkDataStart) + 1, (mLen) + (fieldLen(blk, 1 + (sPos))));
                substringMove(keyStr, 0, mLen, nblk, (blkDataStart) + 2);
                substringMove(blk, (sPos) + 2, bEnd, nblk, (blkDataStart) + (mLen) + 2);
                blk_SetEnd(nblk, ((bEnd) - (sPos)) + (mLen) + (blkDataStart));
                bPos = setField(blk, vPos, valStr, 0, vLen);
                blk[(bPos) + 2] = (byte) (0xff & (fChr));
                setFieldLen(blk, bPos, mLen);
            } else {
                int nbPos = (blkDataStart) + 1;
                nbPos = setField(nblk, nbPos, keyStr, 0, kLen);
                nbPos = setField(nblk, nbPos, valStr, 0, vLen);
                substringMove(blk, sPos, bEnd, nblk, nbPos);
                blk_SetEnd(nblk, (nbPos) + ((bEnd) - (sPos)));
            }
            setFieldLen(blk, (bPos) + 1, 1);
            blk_SetEnd(blk, (bPos) + 3);
            return bPos;
        }
    }

    public static int qpastpLeafSplit(byte[] blk, byte[] nblk, int bPos, byte[] keyStr, int kPos, int kLen, byte[] valStr, int vLen) {
        {
            int bEnd = blk_End(blk);
            setFieldLen(nblk, blkDataStart, 0);
            if (((bEnd) - (bPos)) > ((bPos) - (blkDataStart))) {
                int mLen = fieldLen(blk, bPos);
                int fChr = (blk[(bPos) + 2] & 0xFF);
                setFieldLen(nblk, (blkDataStart) + 1, (mLen) + (fieldLen(blk, 1 + (bPos))));
                substringMove(keyStr, 0, mLen, nblk, (blkDataStart) + 2);
                substringMove(blk, (bPos) + 2, bEnd, nblk, (blkDataStart) + (mLen) + 2);
                blk_SetEnd(nblk, ((bEnd) - (bPos)) + (mLen) + (blkDataStart));
                setFieldLen(blk, bPos, kPos);
                bPos = setField(blk, (bPos) + 1, keyStr, kPos, (kLen) - (kPos));
                bPos = setField(blk, bPos, valStr, 0, vLen);
                blk[(bPos) + 2] = (byte) (0xff & (fChr));
                setFieldLen(blk, bPos, mLen);
            } else {
                int nbPos = (blkDataStart) + 1;
                nbPos = setField(nblk, nbPos, keyStr, 0, kLen);
                nbPos = setField(nblk, nbPos, valStr, 0, vLen);
                substringMove(blk, bPos, bEnd, nblk, nbPos);
                blk_SetEnd(nblk, (nbPos) + ((bEnd) - (bPos)));
                setFieldLen(blk, bPos, kPos);
                blk[(bPos) + 2] = (byte) (0xff & ((keyStr[kPos] & 0xFF)));
            }
            setFieldLen(blk, (bPos) + 1, 1);
            blk_SetEnd(blk, (bPos) + 3);
            return bPos;
        }
    }

    public static int pastpLeafSplit(byte[] blk, byte[] nblk, int bPos, byte[] keyStr, int kPos, int kLen, byte[] valStr, int vLen) {
        {
            int mLen = fieldLen(blk, bPos);
            int bEnd = blk_End(blk);
            setFieldLen(nblk, blkDataStart, 0);
            if (((bEnd) - (bPos)) > ((bPos) - (blkDataStart))) {
                int fChr = (blk[(bPos) + 2 + ((kPos) - (fieldLen(blk, bPos)))] & 0xFF);
                setFieldLen(nblk, (blkDataStart) + 1, (mLen) + (fieldLen(blk, 1 + (bPos))));
                substringMove(keyStr, 0, mLen, nblk, (blkDataStart) + 2);
                substringMove(blk, (bPos) + 2, bEnd, nblk, (blkDataStart) + (mLen) + 2);
                blk_SetEnd(nblk, ((bEnd) - (bPos)) + (mLen) + (blkDataStart));
                setFieldLen(blk, bPos, mLen);
                bPos = setField(blk, (bPos) + 1, keyStr, mLen, (kLen) - (mLen));
                bPos = setField(blk, bPos, valStr, 0, vLen);
                blk[(bPos) + 2] = (byte) (0xff & (fChr));
                setFieldLen(blk, bPos, kPos);
            } else {
                int nbPos = (blkDataStart) + 1;
                int cPos = (bPos) + 2 + ((kPos) - (mLen));
                nbPos = setField(nblk, nbPos, keyStr, 0, kLen);
                nbPos = setField(nblk, nbPos, valStr, 0, vLen);
                setFieldLen(nblk, nbPos, kPos);
                setFieldLen(nblk, (nbPos) + 1, ((fieldLen(blk, 1 + (bPos))) + (mLen)) - (kPos));
                substringMove(blk, cPos, bEnd, nblk, (nbPos) + 2);
                blk_SetEnd(nblk, (nbPos) + 2 + ((bEnd) - (cPos)));
            }
            setFieldLen(blk, (bPos) + 1, 1);
            blk_SetEnd(blk, (bPos) + 3);
            return bPos;
        }
    }

    public static int dummyLeafSplit(byte[] blk, byte[] nblk, int bPos, byte[] keyStr, int kPos, int kLen, byte[] valStr, int vLen) {
        dprintf(">>>>ERROR<<<< " + ("dummyLeafSplit") + ": bad-MATCH-TYPE blk " + (blk_Id(blk)) + "\n");
        return 0;
    }

    public static boolean chainPut(Ent ent, byte[] keyStr, int kLen, byte[] valStr, int vLen, int[] pkt, Ent xent, int wcb) {
        {
            byte[] blk = ent_Blk(ent);
            int blklev = blk_Level(blk);
            boolean index_P = (blklev) > (leaf);
            int rootId = blk_TopId(blk);
            Ent nent = null;
            Ent nrent = null;
            Seg seg = ent_Seg(ent);
            int bsiz = seg_Bsiz(seg);
            boolean result_P = false;
            boolean split_P = false;
            Ent nkeyEnt = ent;
            int nkeyPos = pkt_MatchPos(pkt);
            Ent okeyEnt = ent;
            int okeyPos = blkDataStart;
            int nId = 0;
            int sPos = 0;
            byte[] splitStr = new byte[0x100];
            int sLen = 0;
            pkt_SetBlkToCache(pkt, ent_Id(ent));
            if ((pkt_MatchType(pkt)) == (pastp) && a2b(insertAndAdjust(blk, pkt_MatchPos(pkt), pkt_KeyPos(pkt), keyStr, kLen, valStr, vLen, bsiz))) {
                result_P = (true);
            } else if ((pkt_MatchType(pkt)) == (qpastp) && a2b(simpleInsert(blk, pkt_MatchPos(pkt), pkt_KeyPos(pkt), keyStr, kLen, valStr, vLen, bsiz))) {
                result_P = (true);
            } else if ((pkt_MatchType(pkt)) == (match) && a2b(changeExistingValue(blk, pkt_MatchPos(pkt), keyStr, kLen, valStr, vLen, bsiz))) {
                result_P = (true);
            } else nent = createNewBlkEnt(seg);
            if (!(a2b(nent))) ; else {
                split_P = (true);
                {
                    byte[] nblk = ent_Blk(nent);
                    nId = ent_Id(nent);
                    initNextBlk(blk, nblk);
                    blockSplits = (blockSplits) + 1;
                    switch(pkt_MatchType(pkt)) {
                        case pastp:
                            sPos = pastpLeafSplit(blk, nblk, pkt_MatchPos(pkt), keyStr, pkt_KeyPos(pkt), kLen, valStr, vLen);
                            break;
                        case qpastp:
                            sPos = qpastpLeafSplit(blk, nblk, pkt_MatchPos(pkt), keyStr, pkt_KeyPos(pkt), kLen, valStr, vLen);
                            break;
                        case match:
                            sPos = valLeafSplit(blk, nblk, pkt_MatchPos(pkt), keyStr, pkt_KeyPos(pkt), kLen, valStr, vLen);
                            break;
                        default:
                            sPos = dummyLeafSplit(blk, nblk, pkt_MatchPos(pkt), keyStr, pkt_KeyPos(pkt), kLen, valStr, vLen);
                            break;
                    }
                    sLen = 1 + (fieldLen(blk, sPos));
                    substringMove(nblk, (blkDataStart) + 2, 1 + (fieldLen(blk, sPos)) + ((blkDataStart) + 2), splitStr, 0);
                    if (index_P) {
                        okeyEnt = nent;
                        if ((pkt_MatchPos(pkt)) != (sPos)) {
                            splitIndexInserts = 1 + (splitIndexInserts);
                        } else {
                            okeyPos = nextCnvpair(nblk, blkDataStart);
                            nkeyEnt = nent;
                            nkeyPos = blkDataStart;
                        }
                    }
                    if ((pkt_MatchPos(pkt)) == (sPos)) pkt_SetBlkToCache(pkt, ent_Id(nent));
                    if (root_P(blk)) {
                        nrent = createNewBlkEnt(seg);
                        if (a2b(nrent)) {
                            reroot(blk, ent_Blk(nrent), ent_Id(nrent), seg_Bsiz(seg));
                            if ((nkeyEnt) == (ent)) {
                                nkeyEnt = nrent;
                                pkt_SetBlkToCache(pkt, ent_Id(nrent));
                            }
                        }
                    }
                    result_P = (true);
                }
            }
            if (result_P && index_P) {
                if (a2b(xent)) {
                    indexScrewCase = 1 + (indexScrewCase);
                    okeyEnt = xent;
                    okeyPos = blkDataStart;
                } else if (!(split_P)) okeyPos = nextCnvpair(blk, pkt_MatchPos(pkt));
                {
                    byte[] tmpstr = new byte[4];
                    int oldvPos = (nextField(ent_Blk(okeyEnt), (okeyPos) + 1)) + 1;
                    int newvPos = (nextField(ent_Blk(nkeyEnt), (nkeyPos) + 1)) + 1;
                    substringMoveLeft(ent_Blk(okeyEnt), oldvPos, (oldvPos) + 4, tmpstr, 0);
                    substringMoveLeft(ent_Blk(nkeyEnt), newvPos, (newvPos) + 4, ent_Blk(okeyEnt), oldvPos);
                    substringMoveLeft(tmpstr, 0, 4, ent_Blk(nkeyEnt), newvPos);
                }
            }
            if (a2b(nrent)) {
                ents_EntWrite(nrent);
                releaseEnt(nrent, accwrite);
            }
            if (a2b(nent)) {
                ents_EntWrite(nent);
                ents_EntUpdateAccess_P(nent, accwrite, accnone);
            }
            if (result_P) {
                ent_SetDty(ent, true);
                if ((split_P || a2b(xent) || 0 != ((wcbSap) & (wcb)))) ents_EntWrite(ent);
                releaseEnt(ent, accwrite);
            }
            if (a2b(xent)) {
                ent_SetDty(xent, true);
                ents_EntWrite(xent);
                releaseEnt(xent, accwrite);
            }
            if (split_P) parentInsertUpdate(seg, rootId, blklev, splitStr, sLen, nId);
            if (a2b(nent)) releaseEnt(nent, accnone);
            return result_P;
        }
    }
}
