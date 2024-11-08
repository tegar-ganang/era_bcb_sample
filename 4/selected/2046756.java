package com.gorillalogic.dal.common.table;

import com.gorillalogic.dal.*;
import com.gorillalogic.dal.common.*;
import com.gorillalogic.test.*;

class Family {

    private final Tester tester;

    private final CommonTable member[];

    private final int addIndex;

    private final int remIndex;

    private final int readIndex;

    private final int writeIndex;

    Family(Tester tester, int add, int rem, int read, int write, int max, String name) throws AccessException {
        this(tester, add, rem, read, write, max, name, null);
    }

    Family(Tester tester, int add, int rem, int read, int write, int max, Defr defr) throws AccessException {
        this(tester, add, rem, read, write, max, "Test", defr);
    }

    Family(Tester tester, int add, int rem, int read, int write, int max, String name, Defr defr) throws AccessException {
        this.tester = tester;
        member = new CommonTable[max];
        this.addIndex = add;
        this.remIndex = rem;
        this.readIndex = read;
        this.writeIndex = write;
        populate(0, max, name, defr);
    }

    private void populate(int pos, int count, String name, Defr defr) throws AccessException {
        boolean o2 = defr != null && defr.order() > 0;
        if (pos < count) {
            member[pos] = CommonTable.commonFactory.makeTempCommonTable();
            if (o2) {
                member[pos] = member[pos].secondOrderCommonExtent();
            }
            TypeBuilder builder = member[pos].builder(true);
            if (!o2) {
                builder.setName(name + pos);
            }
            if (pos == 0) {
                if (defr != null) {
                    defr.build(builder);
                }
            } else {
                member[pos - 1].builder(true).adopt(member[pos], true);
            }
            populate(pos + 1, count, name, defr);
        }
    }

    Tester tester() {
        return tester;
    }

    Table rootTable() {
        return member[0];
    }

    Table leafTable() {
        return member[member.length - 1];
    }

    Table addTable() {
        return member[addIndex];
    }

    Table remTable() {
        return member[remIndex];
    }

    Table readTable() {
        return member[readIndex];
    }

    Table writeTable() {
        return member[writeIndex];
    }

    void dump() {
        for (int x = 0; x < member.length; x++) {
            Log.trc("Family #" + x, member[x]);
        }
    }

    void position(String msg) {
        tester.position(msg + " " + addIndex + '-' + remIndex + '-' + readIndex + '-' + writeIndex + '-' + member.length);
    }

    void rowCountTest(String msg, int exp, boolean verbose) {
        rowCountTest(msg, "add", addTable(), exp, verbose);
        rowCountTest(msg, "rem", remTable(), exp, verbose);
        rowCountTest(msg, "read", readTable(), exp, verbose);
        rowCountTest(msg, "write", writeTable(), exp, verbose);
    }

    private void rowCountTest(String msg1, String msg2, Table table, int exp, boolean verbose) {
        int rc = table.rowCount();
        if (verbose || rc != exp) {
            tester.testEq(msg1 + " row_count " + msg2, exp, rc);
        }
        rc = 0;
        Table.Itr itr = table.loopLock();
        while (itr.next()) {
            rc++;
        }
        if (verbose || rc != exp) {
            tester.testEq(msg1 + " row_count_itr_next " + msg2, exp, rc);
        }
        rc = 0;
        itr = table.loopLock();
        while (itr.hasNext()) {
            itr.next();
            rc++;
        }
        if (verbose || rc != exp) {
            tester.testEq(msg1 + " row_count_itr_hasNext " + msg2, exp, rc);
        }
    }

    interface Defr {

        int order();

        void build(TypeBuilder builder) throws AccessException;
    }

    public static void main(String[] argv) {
        Remain.go(argv);
    }
}
