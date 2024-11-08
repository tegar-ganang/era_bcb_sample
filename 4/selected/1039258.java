package sibilant.operations;

import sibilant.object.*;
import sibilant.object.Thread;
import sibilant.object_util.*;
import sibilant.util.BinaryString;
import sibilant.util.StdIO;
import java.io.IOException;

public final class Operations {

    private Operations() {
    }

    public static final Proc _default = lastArg;

    private static class LastArg extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Atom a = Lists.read(argv);
            if (a == null) {
                return Null.instance;
            }
            return a;
        }
    }

    public static final Proc puts = new Puts();

    private static class Puts extends SimpleProc {

        public Null call(List<Atom> argv) throws IOException {
            Atom a = Lists.read(argv);
            while (Lists.peek(argv) != null) {
                StdIO.put(new BinaryString(a.toString()));
                a = Lists.read(argv);
            }
            if (a != null) {
                StdIO.puts(new BinaryString(a.toString()));
            } else {
                StdIO.puts();
            }
            return Null.instance;
        }
    }

    public static final Proc gets = new Gets();

    private static class Gets extends SimpleProc {

        public List<Num> call(List<Atom> argv) throws IOException {
            Lists.assertRem(argv, 0);
            return Lists.newString(StdIO.gets());
        }
    }

    public static final Proc nop = new Nop();

    private static class Nop extends SimpleProc {

        public Null call(List<Atom> argv) {
            Lists.assertRem(argv, 0);
            return Null.instance;
        }
    }

    public static final Proc isNull = new IsNull();

    private static class IsNull extends SimpleProc {

        public Bool call(List<Atom> argv) {
            Lists.assertRem(argv, 1);
            return new Bool(Atoms.isa(Lists.read(argv), Type.NULL));
        }
    }

    public static final Proc isProc = new IsProc();

    private static class IsProc extends SimpleProc {

        public Bool call(List<Atom> argv) {
            Lists.assertRem(argv, 1);
            return new Bool(Atoms.isa(Lists.read(argv), Type.PROC));
        }
    }

    public static final Proc isList = new IsList();

    private static class IsList extends SimpleProc {

        public Bool call(List<Atom> argv) {
            Lists.assertRem(argv, 1);
            return new Bool(Atoms.isa(Lists.read(argv), Type.LIST));
        }
    }

    public static final Proc isDict = new IsDict();

    private static class IsDict extends SimpleProc {

        public Bool call(List<Atom> argv) {
            Lists.assertRem(argv, 1);
            return new Bool(Atoms.isa(Lists.read(argv), Type.DICT));
        }
    }

    public static final Proc isString = new IsString();

    private static class IsString extends SimpleProc {

        public Bool call(List<Atom> argv) {
            Lists.assertRem(argv, 1);
            return new Bool(Atoms.isa(Lists.read(argv), Type.STRING));
        }
    }

    public static final Proc isNum = new IsNum();

    private static class IsNum extends SimpleProc {

        public Bool call(List<Atom> argv) {
            Lists.assertRem(argv, 1);
            return new Bool(Atoms.isa(Lists.read(argv), Type.NUM));
        }
    }

    public static final Proc isByte = new IsByte();

    private static class IsByte extends SimpleProc {

        public Bool call(List<Atom> argv) {
            Lists.assertRem(argv, 1);
            return new Bool(Atoms.isa(Lists.read(argv), Type.BYTE));
        }
    }

    public static final Proc isBool = new IsBool();

    private static class IsBool extends SimpleProc {

        public Bool call(List<Atom> argv) {
            Lists.assertRem(argv, 1);
            return new Bool(Atoms.isa(Lists.read(argv), Type.BOOL));
        }
    }

    public static final Proc isSlot = new IsSlot();

    private static class IsSlot extends SimpleProc {

        public Bool call(List<Atom> argv) {
            Lists.assertRem(argv, 1);
            return new Bool(Atoms.isa(Lists.read(argv), Type.SLOT));
        }
    }

    public static final Proc put = new Put();

    private static class Put extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom slot = Lists.read(argv);
            Atom v = Lists.read(argv);
            Atoms.assertIsa(slot, SLOT);
            Slots.put((sibilant.object.Slot) slot, v);
            return v;
        }
    }

    public static final Proc get = new Get();

    private static class Get extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 1);
            Atom a = Lists.read(argv);
            Atoms.assertIsa(a, SLOT);
            return Slots.get((sibilant.object.Slot) a);
        }
    }

    public static final Proc lNeg = new LNeg();

    private static class LNeg extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 1);
            Atom a = Lists.read(argv);
            Atoms.assertIsa(a, BOOL);
            return new Bool(!((Bool) a).data);
        }
    }

    public static final Proc lAnd = new LAnd();

    private static class LAnd extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atom b = Lists.read(argv);
            Atoms.assertIsa(a, Type.BOOL);
            Atoms.assertIsa(b, Type.BOOL);
            return new Bool(((Bool) a).data && ((Bool) b).data);
        }
    }

    public static final Proc lOr = new LOr();

    private static class LOr extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atom b = Lists.read(argv);
            Atoms.assertIsa(a, Type.BOOL);
            Atoms.assertIsa(b, Type.BOOL);
            return new Bool(((Bool) a).data || ((Bool) b).data);
        }
    }

    public static final Proc lXor = new LXor();

    private static class LXor extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atom b = Lists.read(argv);
            Atoms.assertIsa(a, Type.BOOL);
            Atoms.assertIsa(b, Type.BOOL);
            return new Bool(((Bool) a).data != ((Bool) b).data);
        }
    }

    public static final Proc lNeg_bang = new LNeg();

    private static class LNeg_bang extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 1);
            Atom a = Lists.read(argv);
            Atoms.assertIsa(a, BOOL);
            ((Bool) a).data = !((Bool) a).data;
            return a;
        }
    }

    public static final Proc lAnd_bang = new LAnd();

    private static class LAnd_bang extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atom b = Lists.read(argv);
            Atoms.assertIsa(a, Type.BOOL);
            Atoms.assertIsa(b, Type.BOOL);
            ((Bool) a).data = ((Bool) a).data && ((Bool) b).data;
            return a;
        }
    }

    public static final Proc lOr_bang = new LOr();

    private static class LOr_bang extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atom b = Lists.read(argv);
            Atoms.assertIsa(a, Type.BOOL);
            Atoms.assertIsa(b, Type.BOOL);
            ((Bool) a).data = ((Bool) a).data || ((Bool) b).data;
            return a;
        }
    }

    public static final Proc lXor_bang = new LXor();

    private static class LXor_bang extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atom b = Lists.read(argv);
            Atoms.assertIsa(a, Type.BOOL);
            Atoms.assertIsa(b, Type.BOOL);
            ((Bool) a).data = ((Bool) a).data != ((Bool) b).data;
            return a;
        }
    }

    public static final Proc bwNeg = new BwNeg();

    private static class BwNeg extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 1);
            Atom a = Lists.read(argv);
            Atoms.assertIsa(a, NUM);
            return Nums.bwNeg((Num) a);
        }
    }

    public static final Proc bwAnd = new BwAnd();

    private static class BwAnd extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atom b = Lists.read(argv);
            Atoms.assertIsa(a, Type.NUM);
            Atoms.assertIsa(b, Type.NUM);
            return Nums.bwAnd((Num) a, (Num) b);
        }
    }

    public static final Proc bwOr = new BwOr();

    private static class BwOr extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atom b = Lists.read(argv);
            Atoms.assertIsa(a, Type.NUM);
            Atoms.assertIsa(b, Type.NUM);
            return Nums.bwOr((Num) a, (Num) b);
        }
    }

    public static final Proc bwXor = new BwXor();

    private static class BwXor extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atom b = Lists.read(argv);
            Atoms.assertIsa(a, Type.NUM);
            Atoms.assertIsa(b, Type.NUM);
            return Nums.bwXor((Num) a, (Num) b);
        }
    }

    public static final Proc bwNeg_bang = new BwNeg();

    private static class BwNeg_bang extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 1);
            Atom a = Lists.read(argv);
            Atoms.assertIsa(a, NUM);
            return Nums.bwNegInPlace((Num) a);
        }
    }

    public static final Proc bwAnd_bang = new BwAnd();

    private static class BwAnd_bang extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atom b = Lists.read(argv);
            Atoms.assertIsa(a, Type.NUM);
            Atoms.assertIsa(b, Type.NUM);
            return Nums.bwAndInPlace((Num) a, (Num) b);
        }
    }

    public static final Proc bwOr_bang = new BwOr();

    private static class BwOr_bang extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atom b = Lists.read(argv);
            Atoms.assertIsa(a, Type.NUM);
            Atoms.assertIsa(b, Type.NUM);
            return Nums.bwOrInPlace((Num) a, (Num) b);
        }
    }

    public static final Proc bwXor_bang = new BwXor();

    private static class BwXor_bang extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atom b = Lists.read(argv);
            Atoms.assertIsa(a, Type.NUM);
            Atoms.assertIsa(b, Type.NUM);
            return Nums.bwXorInPlace((Num) a, (Num) b);
        }
    }

    public static final Proc inc = new Inc();

    private static class Inc extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 1);
            Atom a = Lists.read(argv);
            Atoms.assertIsa(a, NUM);
            return Nums.inc((Num) a);
        }
    }

    public static final Proc dec = new Dec();

    private static class Dec extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 1);
            Atom a = Lists.read(argv);
            Atoms.assertIsa(a, NUM);
            return Nums.dec((Num) a);
        }
    }

    public static final Proc mul = new Mul();

    private static class Mul extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atom b = Lists.read(argv);
            Atoms.assertIsa(a, Type.NUM);
            Atoms.assertIsa(b, Type.NUM);
            return Nums.mul((Num) a, (Num) b);
        }
    }

    public static final Proc div = new Div();

    private static class Div extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atom b = Lists.read(argv);
            Atoms.assertIsa(a, Type.NUM);
            Atoms.assertIsa(b, Type.NUM);
            return Nums.div((Num) a, (Num) b);
        }
    }

    public static final Proc rem = new Rem();

    private static class Rem extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atom b = Lists.read(argv);
            Atoms.assertIsa(a, Type.NUM);
            Atoms.assertIsa(b, Type.NUM);
            return Nums.rem((Num) a, (Num) b);
        }
    }

    public static final Proc add = new Add();

    private static class Add extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atom b = Lists.read(argv);
            Atoms.assertIsa(a, Type.NUM);
            Atoms.assertIsa(b, Type.NUM);
            return Nums.add((Num) a, (Num) b);
        }
    }

    public static final Proc negOrSub = new NegOrSub();

    private static class NegOrSub extends SimpleProc {

        public Atom call(List<Atom> argv) {
            long rem = Lists.getrem(argv);
            if (rem == 1) {
                Atom a = Lists.read(argv);
                Atoms.assertIsa(a, Type.NUM);
                return Nums.neg((Num) a);
            } else if (rem == 2) {
                Atom a = Lists.read(argv);
                Atom b = Lists.read(argv);
                Atoms.assertIsa(a, Type.NUM);
                Atoms.assertIsa(b, Type.NUM);
                return Nums.sub((Num) a, (Num) b);
            } else {
                throw new IllegalArgumentException("Incorrect number of arguments. Expected: 1 or 2, Actual: " + String.valueOf(rem));
            }
        }
    }

    public static final Proc shl = new Shl();

    private static class Shl extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atom b = Lists.read(argv);
            Atoms.assertIsa(a, Type.NUM);
            Atoms.assertIsa(b, Type.NUM);
            return Nums.shl((Num) a, (Num) b);
        }
    }

    public static final Proc shr = new Shr();

    private static class Shr extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atom b = Lists.read(argv);
            Atoms.assertIsa(a, Type.NUM);
            Atoms.assertIsa(b, Type.NUM);
            return Nums.shr((Num) a, (Num) b);
        }
    }

    public static final Proc inc_bang = new Inc();

    private static class Inc_bang extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 1);
            Atom a = Lists.read(argv);
            Atoms.assertIsa(a, NUM);
            return Nums.incInPlace((Num) a);
        }
    }

    public static final Proc dec_bang = new Dec();

    private static class Dec_bang extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 1);
            Atom a = Lists.read(argv);
            Atoms.assertIsa(a, NUM);
            return Nums.decInPlace((Num) a);
        }
    }

    public static final Proc mul_bang = new Mul();

    private static class Mul_bang extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atom b = Lists.read(argv);
            Atoms.assertIsa(a, Type.NUM);
            Atoms.assertIsa(b, Type.NUM);
            return Nums.mulInPlace((Num) a, (Num) b);
        }
    }

    public static final Proc div_bang = new Div();

    private static class Div_bang extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atom b = Lists.read(argv);
            Atoms.assertIsa(a, Type.NUM);
            Atoms.assertIsa(b, Type.NUM);
            return Nums.divInPlace((Num) a, (Num) b);
        }
    }

    public static final Proc rem_bang = new Rem();

    private static class Rem_bang extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atom b = Lists.read(argv);
            Atoms.assertIsa(a, Type.NUM);
            Atoms.assertIsa(b, Type.NUM);
            return Nums.remInPlace((Num) a, (Num) b);
        }
    }

    public static final Proc add_bang = new Add();

    private static class Add_bang extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atom b = Lists.read(argv);
            Atoms.assertIsa(a, Type.NUM);
            Atoms.assertIsa(b, Type.NUM);
            return Nums.addInPlace((Num) a, (Num) b);
        }
    }

    public static final Proc negOrSub_bang = new NegOrSub();

    private static class NegOrSub_bang extends SimpleProc {

        public Atom call(List<Atom> argv) {
            long rem = Lists.getrem(argv);
            if (rem == 1) {
                Atom a = Lists.read(argv);
                Atoms.assertIsa(a, Type.NUM);
                return Nums.negInPlace((Num) a);
            } else if (rem == 2) {
                Atom a = Lists.read(argv);
                Atom b = Lists.read(argv);
                Atoms.assertIsa(a, Type.NUM);
                Atoms.assertIsa(b, Type.NUM);
                return Nums.subInPlace((Num) a, (Num) b);
            } else {
                throw new IllegalArgumentException("Incorrect number of arguments. Expected: 1 or 2, Actual: " + String.valueOf(rem));
            }
        }
    }

    public static final Proc shl_bang = new Shl();

    private static class Shl_bang extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atom b = Lists.read(argv);
            Atoms.assertIsa(a, Type.NUM);
            Atoms.assertIsa(b, Type.NUM);
            return Nums.shlInPlace((Num) a, (Num) b);
        }
    }

    public static final Proc shr_bang = new Shr();

    private static class Shr_bang extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atom b = Lists.read(argv);
            Atoms.assertIsa(a, Type.NUM);
            Atoms.assertIsa(b, Type.NUM);
            return Nums.shrInPlace((Num) a, (Num) b);
        }
    }

    public static final Proc lt = new Lt();

    private static class Lt extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atom b = Lists.read(argv);
            Atoms.assertIsa(a, Type.NUM);
            Atoms.assertIsa(b, Type.NUM);
            return Nums.lt((Num) a, (Num) b);
        }
    }

    public static final Proc gt = new Gt();

    private static class Gt extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atom b = Lists.read(argv);
            Atoms.assertIsa(a, Type.NUM);
            Atoms.assertIsa(b, Type.NUM);
            return Nums.gt((Num) a, (Num) b);
        }
    }

    public static final Proc lte = new Lte();

    private static class Lte extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atom b = Lists.read(argv);
            Atoms.assertIsa(a, Type.NUM);
            Atoms.assertIsa(b, Type.NUM);
            return Nums.lte((Num) a, (Num) b);
        }
    }

    public static final Proc gte = new Gte();

    private static class Gte extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atom b = Lists.read(argv);
            Atoms.assertIsa(a, Type.NUM);
            Atoms.assertIsa(b, Type.NUM);
            return Nums.gte((Num) a, (Num) b);
        }
    }

    public static final Proc eq = new Eq();

    private static class Eq extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            return new Bool(Atoms.equals(Lists.read(argv), Lists.read(argv)));
        }
    }

    public static final Proc neq = new Neq();

    private static class Neq extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            return new Bool(!Atoms.equals(Lists.read(argv), Lists.read(argv)));
        }
    }

    public static final Proc clone = new Clone();

    private static class Clone extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 1);
            return Atoms.clone(Lists.read(argv));
        }
    }

    public static final Proc deepclone = new Deepclone();

    private static class Deepclone extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 1);
            return Atoms.deepclone(Lists.read(argv));
        }
    }

    public static final Proc slot = new Slot();

    private static class Slot extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atoms.assertIsa(a, DICT);
            return Dicts.slot((Dict) a, Lists.read(argv));
        }
    }

    public static final Proc search = new Search();

    private static class Search extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atoms.assertIsa(a, DICT);
            return Dicts.search((Dict) a, Lists.read(argv));
        }
    }

    public static final Proc scope = new Scope();

    private static class Scope extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atoms.assertIsa(a, DICT);
            Atom r = ((Dict) a).scope;
            if (r == null) {
                return Null.instance;
            }
            return r;
        }
    }

    public static final Proc read = new Read();

    private static class Read extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 1);
            Atom a = Lists.read(argv);
            Atoms.assertIsa(a, LIST);
            Atom r = Lists.read((List<Atom>) a);
            if (r == null) {
                return Null.instance;
            }
            return r;
        }
    }

    public static final Proc write = new Write();

    private static class Write extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atoms.assertIsa(a, LIST);
            return Lists.write((List<Atom>) a, Lists.read(argv));
        }
    }

    public static final Proc insert = new Insert();

    private static class Insert extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atoms.assertIsa(a, LIST);
            return Lists.insert((List<Atom>) a, Lists.read(argv));
        }
    }

    public static final Proc pop = new Pop();

    private static class Pop extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 1);
            Atom a = Lists.read(argv);
            Atoms.assertIsa(a, LIST);
            Atom r = Lists.pop((List<Atom>) a);
            if (r == null) {
                return Null.instance;
            }
            return r;
        }
    }

    public static final Proc push = new Push();

    private static class Push extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atoms.assertIsa(a, LIST);
            return Lists.push((List<Atom>) a, Lists.read(argv));
        }
    }

    public static final Proc shift = new Shift();

    private static class Shift extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 1);
            Atom a = Lists.read(argv);
            Atoms.assertIsa(a, LIST);
            Atom r = Lists.shift((List<Atom>) a);
            if (r == null) {
                return Null.instance;
            }
            return r;
        }
    }

    public static final Proc cons = new Cons();

    private static class Cons extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atoms.assertIsa(a, LIST);
            return Lists.cons((List<Atom>) a, Lists.read(argv));
        }
    }

    public static final Proc peek = new Peek();

    private static class Peek extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 1);
            Atom a = Lists.read(argv);
            Atoms.assertIsa(a, LIST);
            Atom r = Lists.peek((List<Atom>) a);
            if (r == null) {
                return Null.instance;
            }
            return r;
        }
    }

    public static final Proc writeSC = new WriteSC();

    private static class WriteSC extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atoms.assertIsa(a, LIST);
            return Lists.write(Atoms.clone((List<Atom>) a), Lists.read(argv));
        }
    }

    public static final Proc last = new Last();

    private static class Last extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 1);
            Atom a = Lists.read(argv);
            Atoms.assertIsa(a, LIST);
            Atom r = Lists.last((List<Atom>) a);
            if (r == null) {
                return Null.instance;
            }
            return r;
        }
    }

    public static final Proc pushSC = new PushSC();

    private static class PushSC extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atoms.assertIsa(a, LIST);
            return Lists.push(Atoms.clone((List<Atom>) a), Lists.read(argv));
        }
    }

    public static final Proc insertSC = new InsertSC();

    private static class InsertSC extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atoms.assertIsa(a, LIST);
            return Lists.insert(Atoms.clone((List<Atom>) a), Lists.read(argv));
        }
    }

    public static final Proc first = new First();

    private static class First extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 1);
            Atom a = Lists.read(argv);
            Atoms.assertIsa(a, LIST);
            Atom r = Lists.first((List<Atom>) a);
            if (r == null) {
                return Null.instance;
            }
            return r;
        }
    }

    public static final Proc consSC = new ConsSC();

    private static class ConsSC extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atoms.assertIsa(a, LIST);
            return Lists.cons(Atoms.clone((List<Atom>) a), Lists.read(argv));
        }
    }

    public static final Proc readList = new ReadList();

    private static class ReadList extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 1);
            Atom a = Lists.read(argv);
            Atoms.assertIsa(a, LIST);
            return Lists.readList((List<Atom>) a);
        }
    }

    public static final Proc writeList = new WriteList();

    private static class WriteList extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atom b = Lists.read(argv);
            Atoms.assertIsa(a, Type.LIST);
            Atoms.assertIsa(a, Type.LIST);
            return Lists.writeList((List<Atom>) a, (List<Atom>) b);
        }
    }

    public static final Proc insertList = new InsertList();

    private static class InsertList extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atom b = Lists.read(argv);
            Atoms.assertIsa(a, Type.LIST);
            Atoms.assertIsa(a, Type.LIST);
            return Lists.insertList((List<Atom>) a, Atoms.clone((List<Atom>) b));
        }
    }

    public static final Proc popList = new PopList();

    private static class PopList extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 1);
            Atom a = Lists.read(argv);
            Atoms.assertIsa(a, LIST);
            return Lists.popList((List<Atom>) a);
        }
    }

    public static final Proc pushList = new PushList();

    private static class PushList extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atom b = Lists.read(argv);
            Atoms.assertIsa(a, Type.LIST);
            Atoms.assertIsa(b, Type.LIST);
            return Lists.pushList((List<Atom>) a, Atoms.clone((List<Atom>) b));
        }
    }

    public static final Proc shiftList = new ShiftList();

    private static class ShiftList extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 1);
            Atom a = Lists.read(argv);
            Atoms.assertIsa(a, LIST);
            return Lists.shiftList((List<Atom>) a);
        }
    }

    public static final Proc consList = new ConsList();

    private static class ConsList extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atom b = Lists.read(argv);
            Atoms.assertIsa(a, Type.LIST);
            Atoms.assertIsa(b, Type.LIST);
            return Lists.consList((List<Atom>) a, Atoms.clone((List<Atom>) b));
        }
    }

    public static final Proc readListSC = new ReadListSC();

    private static class ReadListSC extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 1);
            Atom a = Lists.read(argv);
            Atoms.assertIsa(a, LIST);
            return Lists.readList(Atoms.clone((List<Atom>) a));
        }
    }

    public static final Proc writeListSC = new WriteListSC();

    private static class WriteListSC extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atom b = Lists.read(argv);
            Atoms.assertIsa(a, Type.LIST);
            Atoms.assertIsa(b, Type.LIST);
            return Lists.writeList(Atoms.clone((List<Atom>) a), (List<Atom>) b);
        }
    }

    public static final Proc insertListSC = new InsertListSC();

    private static class InsertListSC extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atom b = Lists.read(argv);
            Atoms.assertIsa(a, Type.LIST);
            Atoms.assertIsa(b, Type.LIST);
            return Lists.insertList(Atoms.clone((List<Atom>) a), (List<Atom>) b);
        }
    }

    public static final Proc cdr = new Cdr();

    private static class Cdr extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 1);
            Atom a = Lists.read(argv);
            Atoms.assertIsa(a, LIST);
            return Lists.popList(Atoms.clone((List<Atom>) a));
        }
    }

    public static final Proc cat = new Cat();

    private static class Cat extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atom b = Lists.read(argv);
            Atoms.assertIsa(a, Type.LIST);
            Atoms.assertIsa(b, Type.LIST);
            return Lists.pushList(Atoms.clone((List<Atom>) a), Atoms.clone((List<Atom>) b));
        }
    }

    public static final Proc shiftListSC = new ShiftListSC();

    private static class ShiftListSC extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 1);
            Atom a = Lists.read(argv);
            Atoms.assertIsa(a, LIST);
            return Lists.shiftList((List<Atom>) a);
        }
    }

    public static final Proc consListSC = new ConsListSC();

    private static class ConsListSC extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atom b = Lists.read(argv);
            Atoms.assertIsa(a, Type.LIST);
            Atoms.assertIsa(b, Type.LIST);
            return Lists.consList(Atoms.clone((List<Atom>) a), Atoms.clone((List<Atom>) b));
        }
    }

    public static final Proc seek = new Seek();

    private static class Seek extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atom b = Lists.read(argv);
            Atoms.assertIsa(a, Type.LIST);
            Atoms.assertIsa(b, Type.NUM);
            Lists.seek((List<Atom>) a, ((Num) b).data.longValue());
            return b;
        }
    }

    public static final Proc setpos = new Setpos();

    private static class Setpos extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 2);
            Atom a = Lists.read(argv);
            Atom b = Lists.read(argv);
            Atoms.assertIsa(a, Type.LIST);
            Atoms.assertIsa(b, Type.NUM);
            Lists.setpos((List<Atom>) a, ((Num) b).data.longValue());
            return b;
        }
    }

    public static final Proc getpos = new Getpos();

    private static class Getpos extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 1);
            Atom a = Lists.read(argv);
            Atoms.assertIsa(a, LIST);
            return new Num(Lists.getpos((List<Atom>) a));
        }
    }

    public static final Proc getrem = new GetRem();

    private static class GetRem extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 1);
            Atom a = Lists.read(argv);
            Atoms.assertIsa(a, LIST);
            return new Num(Lists.getrem((List<Atom>) a));
        }
    }

    public static final Proc size = new Size();

    private static class Size extends SimpleProc {

        public Atom call(List<Atom> argv) {
            Lists.assertRem(argv, 1);
            Atom a = Lists.read(argv);
            Atoms.assertIsa(a, LIST);
            return new Num(Lists.size((List<Atom>) a));
        }
    }

    public static final Proc _if = new If();

    private static class If extends Proc {

        public Atom call(Thread thread) throws Exception {
            Atoms.assertIsa(thread.cont.state, Type.LIST);
            List<Atom> argv = (List<Atom>) thread.cont.state;
            long rem = Lists.getrem(argv);
            if (rem == 3) {
                Atom a = Lists.read(argv);
                Atom b = Lists.read(argv);
                Atom c = Lists.read(argv);
                Atoms.assertIsa(a, Type.BOOL);
                if (((Bool) a).data) {
                    if (Atoms.isa(b, Type.PROC)) {
                        return Engine.call(thread, b, thread.cont.arg, 1);
                    } else {
                        return b;
                    }
                } else {
                    if (Atoms.isa(c, Type.PROC)) {
                        return Engine.call(thread, c, thread.cont.arg, 1);
                    } else {
                        return c;
                    }
                }
            } else if (rem == 2) {
                Atom a = Lists.read(argv);
                Atom b = Lists.read(argv);
                Atoms.assertIsa(a, Type.BOOL);
                if (((Bool) a).data) {
                    if (Atoms.isa(b, Type.PROC)) {
                        return Engine.call(thread, b, thread.cont.arg, 1);
                    } else {
                        return b;
                    }
                } else {
                    return Null.instance;
                }
            } else {
                throw new IllegalArgumentException("Incorrect number of arguments. Expected: 2 or 3, Actual: " + String.valueOf(rem));
            }
        }
    }

    public static final Dict operationsDict = initOperationsDict();

    private static Dict initOperationsDict() {
        Dict r = new Dict(null);
        Dicts.put(r, Lists.newString(new BinaryString("puts")), puts);
        Dicts.put(r, Lists.newString(new BinaryString("gets")), gets);
        Dicts.put(r, Lists.newString(new BinaryString("nop")), nop);
        Dicts.put(r, Lists.newString(new BinaryString("null?")), isNull);
        Dicts.put(r, Lists.newString(new BinaryString("proc?")), isProc);
        Dicts.put(r, Lists.newString(new BinaryString("list?")), isList);
        Dicts.put(r, Lists.newString(new BinaryString("dict?")), isDict);
        Dicts.put(r, Lists.newString(new BinaryString("string?")), isString);
        Dicts.put(r, Lists.newString(new BinaryString("num?")), isNum);
        Dicts.put(r, Lists.newString(new BinaryString("byte?")), isByte);
        Dicts.put(r, Lists.newString(new BinaryString("bool?")), isBool);
        Dicts.put(r, Lists.newString(new BinaryString("slot?")), isSlot);
        Dicts.put(r, Lists.newString(new BinaryString(":")), put);
        Dicts.put(r, Lists.newString(new BinaryString("$")), get);
        Dicts.put(r, Lists.newString(new BinaryString("~~")), lNeg);
        Dicts.put(r, Lists.newString(new BinaryString("not")), lNeg);
        Dicts.put(r, Lists.newString(new BinaryString("&&")), lAnd);
        Dicts.put(r, Lists.newString(new BinaryString("and")), lAnd);
        Dicts.put(r, Lists.newString(new BinaryString("||")), lOr);
        Dicts.put(r, Lists.newString(new BinaryString("or")), lOr);
        Dicts.put(r, Lists.newString(new BinaryString("^^")), lXor);
        Dicts.put(r, Lists.newString(new BinaryString("xor")), lXor);
        Dicts.put(r, Lists.newString(new BinaryString("~~!")), lNeg_bang);
        Dicts.put(r, Lists.newString(new BinaryString("not!")), lNeg_bang);
        Dicts.put(r, Lists.newString(new BinaryString("&&!")), lAnd_bang);
        Dicts.put(r, Lists.newString(new BinaryString("and!")), lAnd_bang);
        Dicts.put(r, Lists.newString(new BinaryString("||!")), lOr_bang);
        Dicts.put(r, Lists.newString(new BinaryString("or!")), lOr_bang);
        Dicts.put(r, Lists.newString(new BinaryString("^^!")), lXor_bang);
        Dicts.put(r, Lists.newString(new BinaryString("xor!")), lXor_bang);
        Dicts.put(r, Lists.newString(new BinaryString("~")), bwNeg);
        Dicts.put(r, Lists.newString(new BinaryString("&")), bwAnd);
        Dicts.put(r, Lists.newString(new BinaryString("|")), bwOr);
        Dicts.put(r, Lists.newString(new BinaryString("^")), bwXor);
        Dicts.put(r, Lists.newString(new BinaryString("~!")), bwNeg_bang);
        Dicts.put(r, Lists.newString(new BinaryString("&!")), bwAnd_bang);
        Dicts.put(r, Lists.newString(new BinaryString("|!")), bwOr_bang);
        Dicts.put(r, Lists.newString(new BinaryString("^!")), bwXor_bang);
        Dicts.put(r, Lists.newString(new BinaryString("++")), inc);
        Dicts.put(r, Lists.newString(new BinaryString("--")), dec);
        Dicts.put(r, Lists.newString(new BinaryString("*")), mul);
        Dicts.put(r, Lists.newString(new BinaryString("/")), div);
        Dicts.put(r, Lists.newString(new BinaryString("%")), rem);
        Dicts.put(r, Lists.newString(new BinaryString("+")), add);
        Dicts.put(r, Lists.newString(new BinaryString("-")), negOrSub);
        Dicts.put(r, Lists.newString(new BinaryString("<<")), shl);
        Dicts.put(r, Lists.newString(new BinaryString(">>")), shr);
        Dicts.put(r, Lists.newString(new BinaryString("++!")), inc_bang);
        Dicts.put(r, Lists.newString(new BinaryString("--!")), dec_bang);
        Dicts.put(r, Lists.newString(new BinaryString("*!")), mul_bang);
        Dicts.put(r, Lists.newString(new BinaryString("/!")), div_bang);
        Dicts.put(r, Lists.newString(new BinaryString("%!")), rem_bang);
        Dicts.put(r, Lists.newString(new BinaryString("+!")), add_bang);
        Dicts.put(r, Lists.newString(new BinaryString("-!")), negOrSub_bang);
        Dicts.put(r, Lists.newString(new BinaryString("<<!")), shl_bang);
        Dicts.put(r, Lists.newString(new BinaryString(">>!")), shr_bang);
        Dicts.put(r, Lists.newString(new BinaryString("<")), lt);
        Dicts.put(r, Lists.newString(new BinaryString(">")), gt);
        Dicts.put(r, Lists.newString(new BinaryString("<=")), lte);
        Dicts.put(r, Lists.newString(new BinaryString(">=")), gte);
        Dicts.put(r, Lists.newString(new BinaryString("=?")), eq);
        Dicts.put(r, Lists.newString(new BinaryString("~=?")), neq);
        Dicts.put(r, Lists.newString(new BinaryString("clone")), clone);
        Dicts.put(r, Lists.newString(new BinaryString("deepclone")), deepclone);
        Dicts.put(r, Lists.newString(new BinaryString(".")), slot);
        Dicts.put(r, Lists.newString(new BinaryString("???")), search);
        Dicts.put(r, Lists.newString(new BinaryString("scope")), scope);
        Dicts.put(r, Lists.newString(new BinaryString("<-!")), read);
        Dicts.put(r, Lists.newString(new BinaryString("read!")), read);
        Dicts.put(r, Lists.newString(new BinaryString("->!")), write);
        Dicts.put(r, Lists.newString(new BinaryString("write!")), write);
        Dicts.put(r, Lists.newString(new BinaryString("-^!")), insert);
        Dicts.put(r, Lists.newString(new BinaryString("insert!")), insert);
        Dicts.put(r, Lists.newString(new BinaryString("<-|!")), pop);
        Dicts.put(r, Lists.newString(new BinaryString("pop!")), pop);
        Dicts.put(r, Lists.newString(new BinaryString("->|!")), push);
        Dicts.put(r, Lists.newString(new BinaryString("push!")), push);
        Dicts.put(r, Lists.newString(new BinaryString("<-^!")), shift);
        Dicts.put(r, Lists.newString(new BinaryString("shift!")), shift);
        Dicts.put(r, Lists.newString(new BinaryString("->^!")), cons);
        Dicts.put(r, Lists.newString(new BinaryString("cons!")), cons);
        Dicts.put(r, Lists.newString(new BinaryString("<-")), peek);
        Dicts.put(r, Lists.newString(new BinaryString("read")), peek);
        Dicts.put(r, Lists.newString(new BinaryString("peek")), peek);
        Dicts.put(r, Lists.newString(new BinaryString("->")), writeSC);
        Dicts.put(r, Lists.newString(new BinaryString("write")), writeSC);
        Dicts.put(r, Lists.newString(new BinaryString("<-|")), last);
        Dicts.put(r, Lists.newString(new BinaryString("pop")), last);
        Dicts.put(r, Lists.newString(new BinaryString("last")), last);
        Dicts.put(r, Lists.newString(new BinaryString("->|")), pushSC);
        Dicts.put(r, Lists.newString(new BinaryString("push")), pushSC);
        Dicts.put(r, Lists.newString(new BinaryString("-^")), insertSC);
        Dicts.put(r, Lists.newString(new BinaryString("insert")), insertSC);
        Dicts.put(r, Lists.newString(new BinaryString("<-^")), first);
        Dicts.put(r, Lists.newString(new BinaryString("first")), first);
        Dicts.put(r, Lists.newString(new BinaryString("->^")), consSC);
        Dicts.put(r, Lists.newString(new BinaryString("cons")), consSC);
        Dicts.put(r, Lists.newString(new BinaryString("@<-!")), readList);
        Dicts.put(r, Lists.newString(new BinaryString("readlist!")), readList);
        Dicts.put(r, Lists.newString(new BinaryString("@->!")), writeList);
        Dicts.put(r, Lists.newString(new BinaryString("writelist!")), writeList);
        Dicts.put(r, Lists.newString(new BinaryString("@-^!")), insertList);
        Dicts.put(r, Lists.newString(new BinaryString("insertlist!")), insertList);
        Dicts.put(r, Lists.newString(new BinaryString("@<-|!")), popList);
        Dicts.put(r, Lists.newString(new BinaryString("poplist!")), popList);
        Dicts.put(r, Lists.newString(new BinaryString("@->|!")), pushList);
        Dicts.put(r, Lists.newString(new BinaryString("pushlist!")), pushList);
        Dicts.put(r, Lists.newString(new BinaryString("@<-^!")), shiftList);
        Dicts.put(r, Lists.newString(new BinaryString("shiftlist!")), shiftList);
        Dicts.put(r, Lists.newString(new BinaryString("@->^!")), consList);
        Dicts.put(r, Lists.newString(new BinaryString("conslist!")), consList);
        Dicts.put(r, Lists.newString(new BinaryString("@<-")), readListSC);
        Dicts.put(r, Lists.newString(new BinaryString("readlist")), readListSC);
        Dicts.put(r, Lists.newString(new BinaryString("@->")), writeListSC);
        Dicts.put(r, Lists.newString(new BinaryString("writelist")), writeListSC);
        Dicts.put(r, Lists.newString(new BinaryString("@-^")), insertListSC);
        Dicts.put(r, Lists.newString(new BinaryString("insertlist")), insertListSC);
        Dicts.put(r, Lists.newString(new BinaryString("@<-|")), cdr);
        Dicts.put(r, Lists.newString(new BinaryString("poplist")), cdr);
        Dicts.put(r, Lists.newString(new BinaryString("cdr")), cdr);
        Dicts.put(r, Lists.newString(new BinaryString("@->|")), cat);
        Dicts.put(r, Lists.newString(new BinaryString("pushlist")), cat);
        Dicts.put(r, Lists.newString(new BinaryString("cat")), cat);
        Dicts.put(r, Lists.newString(new BinaryString("@<-^")), shiftListSC);
        Dicts.put(r, Lists.newString(new BinaryString("shiftlist")), shiftListSC);
        Dicts.put(r, Lists.newString(new BinaryString("@->^")), consListSC);
        Dicts.put(r, Lists.newString(new BinaryString("conslist")), consListSC);
        Dicts.put(r, Lists.newString(new BinaryString("seek!")), seek);
        Dicts.put(r, Lists.newString(new BinaryString("setpos!")), setpos);
        Dicts.put(r, Lists.newString(new BinaryString("getpos")), getpos);
        Dicts.put(r, Lists.newString(new BinaryString("getrem")), getrem);
        Dicts.put(r, Lists.newString(new BinaryString("size")), size);
        Dicts.put(r, Lists.newString(new BinaryString("if")), _if);
        return r;
    }
}
