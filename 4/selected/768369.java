package jparse;

import java.util.HashSet;
import jparse.expr.VarAST;

/**
 * A list of variables whose values are read by an expression or statement,
 * whose values are written by an expression or statement, or which are
 * declared by a statement
 *
 * @version $Revision: 1.1.1.1 $, $Date: 2006/10/22 16:11:43 $
 * @author Jerry James
 */
public final class VarList {

    /**
     * The list of variables whose values are read
     */
    public final VarAST[] read;

    /**
     * The list of variables whose values are written
     */
    public final VarAST[] write;

    /**
     * The list of variables that are declared
     */
    public final VarAST[] decl;

    /**
     * Create an empty list of variables
     */
    public VarList() {
        decl = write = read = new VarAST[0];
    }

    /**
     * Create a new list of variables from a single variable
     *
     * @param readVar a variable that is read by the expression or statement
     */
    public VarList(final VarAST readVar) {
        read = (readVar == null) ? new VarAST[0] : new VarAST[] { readVar };
        decl = write = new VarAST[0];
    }

    /**
     * Create a new list of variables from an array of declarations
     *
     * @param decls the declarations
     */
    public VarList(final VarAST[] decls) {
        read = write = new VarAST[0];
        decl = decls;
    }

    /**
     * Create a new list of variables by adding a declaration to an existing
     * list
     *
     * @param list the existing list
     * @param declaration the declaration to add
     */
    public VarList(final VarList list, final VarAST declaration) {
        read = list.read;
        write = list.write;
        decl = new VarAST[list.decl.length + 1];
        System.arraycopy(list.decl, 0, decl, 0, list.decl.length);
        decl[list.decl.length] = declaration;
    }

    /**
     * Create a new list of variables by merging two other lists
     *
     * @param list1 the first list to merge
     * @param list2 the second list to merge
     */
    public VarList(final VarList list1, final VarList list2) {
        this(list1, list2, false);
    }

    /**
     * Create a new list of variables by merging two other lists, and
     * optionally changing reads to writes for the first list
     *
     * @param list1 the first list to merge
     * @param list2 the second list to merge
     * @param assign <code>true</code> if this is for an assignment
     * expression, so that reads should be changed to writes for
     * <var>list1</var>
     */
    public VarList(final VarList list1, final VarList list2, final boolean assign) {
        if (assign || list1.read.length == 0) {
            read = list2.read;
        } else if (list2.read.length == 0) {
            read = list1.read;
        } else {
            final HashSet merge = new HashSet();
            for (int i = 0; i < list1.read.length; i++) {
                merge.add(list1.read[i]);
            }
            for (int i = 0; i < list2.read.length; i++) {
                merge.add(list2.read[i]);
            }
            read = new VarAST[merge.size()];
            merge.toArray(read);
        }
        if (list1.write.length == 0 && (!assign || list1.read.length == 0)) {
            write = list2.write;
        } else if (list2.write.length == 0 && (!assign || list1.read.length == 0)) {
            write = list1.write;
        } else {
            final HashSet merge = new HashSet();
            for (int i = 0; i < list1.write.length; i++) {
                merge.add(list1.write[i]);
            }
            for (int i = 0; i < list2.write.length; i++) {
                merge.add(list2.write[i]);
            }
            if (assign) {
                for (int i = 0; i < list1.read.length; i++) {
                    merge.add(list1.read[i]);
                }
            }
            write = new VarAST[merge.size()];
            merge.toArray(write);
        }
        if (list1.decl.length == 0) {
            decl = list2.decl;
        } else if (list2.decl.length == 0) {
            decl = list1.decl;
        } else {
            final HashSet merge = new HashSet();
            for (int i = 0; i < list1.decl.length; i++) {
                merge.add(list1.decl[i]);
            }
            for (int i = 0; i < list2.decl.length; i++) {
                merge.add(list2.decl[i]);
            }
            decl = new VarAST[merge.size()];
            merge.toArray(decl);
        }
    }

    /**
     * Create a new list of variables by merging three other lists
     *
     * @param list1 the first list to merge
     * @param list2 the second list to merge
     * @param list3 the third list to merge
     */
    public VarList(final VarList list1, final VarList list2, final VarList list3) {
        final HashSet merge = new HashSet();
        for (int i = 0; i < list1.read.length; i++) {
            merge.add(list1.read[i]);
        }
        for (int i = 0; i < list2.read.length; i++) {
            merge.add(list2.read[i]);
        }
        for (int i = 0; i < list3.read.length; i++) {
            merge.add(list3.read[i]);
        }
        read = new VarAST[merge.size()];
        merge.toArray(read);
        merge.clear();
        for (int i = 0; i < list1.write.length; i++) {
            merge.add(list1.write[i]);
        }
        for (int i = 0; i < list2.write.length; i++) {
            merge.add(list2.write[i]);
        }
        for (int i = 0; i < list3.write.length; i++) {
            merge.add(list3.write[i]);
        }
        write = new VarAST[merge.size()];
        merge.toArray(write);
        merge.clear();
        for (int i = 0; i < list1.decl.length; i++) {
            merge.add(list1.decl[i]);
        }
        for (int i = 0; i < list2.decl.length; i++) {
            merge.add(list2.decl[i]);
        }
        for (int i = 0; i < list3.decl.length; i++) {
            merge.add(list3.decl[i]);
        }
        decl = new VarAST[merge.size()];
        merge.toArray(decl);
    }

    /**
     * Create a new list of variables by merging an array of lists
     *
     * @param lists the array of lists to merge
     */
    public VarList(final VarList[] lists) {
        final HashSet merge = new HashSet();
        for (int i = 0; i < lists.length; i++) {
            final VarList theList = lists[i];
            for (int j = 0; j < theList.read.length; j++) {
                merge.add(theList.read[j]);
            }
        }
        read = new VarAST[merge.size()];
        merge.toArray(read);
        merge.clear();
        for (int i = 0; i < lists.length; i++) {
            final VarList theList = lists[i];
            for (int j = 0; j < theList.write.length; j++) {
                merge.add(theList.write[j]);
            }
        }
        write = new VarAST[merge.size()];
        merge.toArray(write);
        merge.clear();
        for (int i = 0; i < lists.length; i++) {
            final VarList theList = lists[i];
            for (int j = 0; j < theList.decl.length; j++) {
                merge.add(theList.decl[j]);
            }
        }
        decl = new VarAST[merge.size()];
        merge.toArray(decl);
    }
}
