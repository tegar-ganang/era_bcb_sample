package components.functionUnits;

import components.Util;
import components.exceptions.BinaryToIntException;
import components.exceptions.IntToBinaryException;
import components.memory.Memory;
import components.register.RegisterGroup;

/**
 * @author Chz
 * 
 */
public class ALU {

    private static final int OPLEN = Memory.WORDLENGTH;

    private ALU() {
    }

    public static int[] add(int[] op1, int[] op2) {
        int len = OPLEN;
        int[] res = new int[len];
        try {
            int op1Int = Util.binaryToInt(op1);
            int op2Int = Util.binaryToInt(op2);
            int originalResult = op1Int + op2Int;
            res = Util.intToBinary(originalResult, len, false);
            if ((res[0] == 0 && originalResult < 0) || (res[0] == 1 && originalResult >= 0)) {
                RegisterGroup.getInstance().setCC(RegisterGroup.CC_OVERFLOW, 1);
            }
        } catch (BinaryToIntException e) {
            e.printStackTrace();
        } catch (IntToBinaryException e) {
            e.printStackTrace();
        }
        return res;
    }

    public static int[] sub(int[] op1, int[] op2) {
        int len = OPLEN;
        int[] res = new int[len];
        try {
            int op1Int = Util.binaryToInt(op1);
            int op2Int = Util.binaryToInt(op2);
            int originalResult = op1Int - op2Int;
            res = Util.intToBinary(originalResult, len, false);
            if ((res[0] == 0 && originalResult < 0) || (res[0] == 1 && originalResult >= 0)) {
                RegisterGroup.getInstance().setCC(RegisterGroup.CC_UNDERFLOW, 1);
            }
        } catch (IntToBinaryException e) {
            e.printStackTrace();
        } catch (BinaryToIntException e) {
            e.printStackTrace();
        }
        return res;
    }

    public static int[] mul(int[] op1, int[] op2) {
        int op1Int = 0, op2Int = 0;
        int[] res = null;
        try {
            op1Int = Util.binaryToInt(op1, true);
            op2Int = Util.binaryToInt(op2, true);
        } catch (BinaryToIntException e) {
            e.printStackTrace();
            System.err.println("fatal error: Fail in ALU.MUL!");
        }
        int resInt = op1Int * op2Int;
        int resLen = op1.length + op2.length;
        if (resLen <= 16) resLen = 16; else if (resLen <= 32) resLen = 32; else if (resLen <= 64) resLen = 64; else resLen = 128;
        try {
            res = Util.intToBinary(resInt, resLen, false);
            if ((res[0] == 0 && resInt < 0) || (res[0] == 1 && resInt >= 0)) {
                RegisterGroup.getInstance().setCC(RegisterGroup.CC_OVERFLOW, 1);
            }
        } catch (IntToBinaryException e) {
            e.printStackTrace();
        }
        return res;
    }

    public static int[] div(int[] op1, int[] op2) {
        int op1Int = 0, op2Int = 1;
        try {
            op1Int = Util.binaryToInt(op1, true);
            op2Int = Util.binaryToInt(op2, true);
            if (op2Int == 0) {
                RegisterGroup.getInstance().setCC(RegisterGroup.CC_DIVISIONALBY0, 1);
                return null;
            }
        } catch (BinaryToIntException e) {
            e.printStackTrace();
        }
        int quotient = op1Int / op2Int;
        int remainder = op1Int % op2Int;
        int len = Util.trimLength(Math.max(op1.length, op2.length));
        int[] quitientArray = {};
        int[] remainderArray = {};
        try {
            quitientArray = Util.intToBinary(quotient, len);
            remainderArray = Util.intToBinary(remainder, len);
        } catch (IntToBinaryException e) {
            e.printStackTrace();
        }
        int[] resArray = new int[len * 2];
        for (int i = 0; i < len; i++) {
            resArray[i] = quitientArray[i];
            resArray[len + i] = remainderArray[i];
        }
        return resArray;
    }

    public static int tst(int[] op1, int[] op2) {
        int[] trimedOp1 = Util.trimArray(op1);
        int[] trimedOp2 = Util.trimArray(op2);
        if (trimedOp1.length != trimedOp2.length) return 0;
        for (int i = 0; i < trimedOp1.length; ++i) {
            if (trimedOp1[i] != trimedOp2[i]) return 0;
        }
        return 1;
    }

    public static int[] and(int[] op1, int[] op2) {
        int cnt = 0;
        int i = 0;
        int[] op1OpArray = new int[OPLEN];
        int[] op2OpArray = new int[OPLEN];
        for (i = 0; i < op1.length; i++) {
            op1OpArray[OPLEN - i - 1] = op1[op1.length - i - 1];
        }
        for (; i < OPLEN - 1; i++) op1OpArray[OPLEN - i - 1] = 0;
        for (i = 0; i < op2.length; i++) {
            op2OpArray[OPLEN - i - 1] = op2[op2.length - i - 1];
        }
        for (; i < OPLEN - 1; i++) op2OpArray[OPLEN - i - 1] = 0;
        int[] res = new int[OPLEN];
        for (cnt = 0; cnt < OPLEN; cnt++) {
            res[OPLEN - cnt - 1] = op1OpArray[OPLEN - cnt - 1] & op2OpArray[OPLEN - cnt - 1];
        }
        return res;
    }

    public static int[] or(int[] op1, int[] op2) {
        int cnt = 0;
        int i = 0;
        int[] op1OpArray = new int[OPLEN];
        int[] op2OpArray = new int[OPLEN];
        for (i = 0; i < op1.length; i++) {
            op1OpArray[OPLEN - i - 1] = op1[op1.length - i - 1];
        }
        for (; i < OPLEN - 1; i++) op1OpArray[OPLEN - i - 1] = 0;
        for (i = 0; i < op2.length; i++) {
            op2OpArray[OPLEN - i - 1] = op2[op2.length - i - 1];
        }
        for (; i < OPLEN - 1; i++) op2OpArray[OPLEN - i - 1] = 0;
        int[] res = new int[OPLEN];
        for (cnt = 0; cnt < OPLEN; cnt++) {
            res[OPLEN - cnt - 1] = op1OpArray[OPLEN - cnt - 1] | op2OpArray[OPLEN - cnt - 1];
        }
        return res;
    }

    public static int[] not(int[] op1) {
        int[] res = new int[op1.length];
        for (int i = 0; i < op1.length; i++) res[i] = 1 - op1[i];
        return res;
    }

    public static int[] src(int[] op, int lr, int al, int count) {
        int len = op.length;
        int[] res = new int[len];
        if (lr == 0) {
            if (al == 0) {
                for (int i = 0; i < count; i++) {
                    for (int j = len - 1; j > 1; j--) {
                        res[j - 1] = op[j];
                    }
                    res[1] = 0;
                }
            } else {
                for (int i = 0; i < count; i++) {
                    for (int j = len - 1; j > 0; j--) {
                        res[j - 1] = op[j];
                    }
                    res[0] = 0;
                }
            }
        } else {
            if (al == 0) {
                for (int i = 0; i < count; i++) {
                    for (int j = 1; j < len - 1; j++) {
                        res[j] = op[j + 1];
                    }
                    res[len - 1] = 0;
                }
            } else {
                for (int i = 0; i < count; i++) {
                    for (int j = 0; j < len - 1; j++) {
                        res[j] = op[j + 1];
                    }
                    res[len - 1] = 0;
                }
            }
        }
        return res;
    }

    public static int[] rrc(int[] op, int lr, int al, int count) {
        int tmp = 0;
        int len = op.length;
        int[] res = new int[len];
        if (lr == 0) {
            if (al == 0) {
                for (int i = 0; i < count; i++) {
                    tmp = op[len - 1];
                    for (int j = len - 1; j > 1; j--) {
                        res[j - 1] = op[j];
                    }
                    res[1] = tmp;
                }
            } else {
                for (int i = 0; i < count; i++) {
                    tmp = op[len - 1];
                    for (int j = len - 1; j > 0; j--) {
                        res[j - 1] = op[j];
                    }
                    res[0] = tmp;
                }
            }
        } else {
            if (al == 0) {
                for (int i = 0; i < count; i++) {
                    tmp = op[1];
                    for (int j = 1; j < len - 1; j++) {
                        res[j] = op[j + 1];
                    }
                    res[len - 1] = tmp;
                }
            } else {
                for (int i = 0; i < count; i++) {
                    tmp = op[0];
                    for (int j = 0; j < len - 1; j++) {
                        res[j] = op[j + 1];
                    }
                    res[len - 1] = tmp;
                }
            }
        }
        return res;
    }
}
