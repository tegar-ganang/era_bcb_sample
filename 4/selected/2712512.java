package sun.tools.tree;

import sun.tools.java.*;
import sun.tools.tree.*;
import java.util.Vector;

/**
 * A local Field
 *
 * WARNING: The contents of this source file are not part of any
 * supported API.  Code that depends on them does so at its own risk:
 * they are subject to change or removal without notice.
 */
public class LocalMember extends MemberDefinition {

    /**
     * The number of the variable
     */
    int number = -1;

    /**
     * Some statistics
     */
    int readcount;

    int writecount;

    /**
     * An indication of which block the variable comes from.
     * Helps identify uplevel references.
     */
    int scopeNumber;

    /**
     * Return current nesting level, i.e., the value of 'scopeNumber'.
     * Made public for the benefit of 'ClassDefinition.resolveName'.
     */
    public int getScopeNumber() {
        return scopeNumber;
    }

    /**
     * Used by copyInline to record the original of this copy.
     */
    LocalMember originalOfCopy;

    /**
     * The previous local variable, this list is used to build a nested
     * context of local variables.
     */
    LocalMember prev;

    /**
     * Constructor
     */
    public LocalMember(long where, ClassDefinition clazz, int modifiers, Type type, Identifier name) {
        super(where, clazz, modifiers, type, name, null, null);
    }

    /**
     * Constructor for a block-inner class.
     */
    public LocalMember(ClassDefinition innerClass) {
        super(innerClass);
        name = innerClass.getLocalName();
    }

    /**
     * Constructor for a proxy to an instance or class variable.
     */
    LocalMember(MemberDefinition field) {
        this(0, null, 0, field.getType(), idClass);
        accessPeer = field;
    }

    /**
     * Is this a proxy for the given field?
     */
    final MemberDefinition getMember() {
        return (name == idClass) ? accessPeer : null;
    }

    /**
     * Special checks
     */
    public boolean isLocal() {
        return true;
    }

    /**
     * Make a copy of this field, which is an argument to a method
     * or constructor.  Arrange so that when occurrences of the field
     * are encountered in an immediately following copyInline() operation,
     * the expression nodes will replace the original argument by the
     * fresh copy.
     */
    public LocalMember copyInline(Context ctx) {
        LocalMember copy = new LocalMember(where, clazz, modifiers, type, name);
        copy.readcount = this.readcount;
        copy.writecount = this.writecount;
        copy.originalOfCopy = this;
        copy.addModifiers(M_LOCAL);
        if (this.accessPeer != null && (this.accessPeer.getModifiers() & M_LOCAL) == 0) {
            throw new CompilerError("local copyInline");
        }
        this.accessPeer = copy;
        return copy;
    }

    /**
     * Returns the previous result of copyInline(ctx).
     * Must be called in the course of an Expression.copyInline()
     * operation that immediately follows the LocalMember.copyInline().
     * Return "this" if there is no such copy.
     */
    public LocalMember getCurrentInlineCopy(Context ctx) {
        MemberDefinition accessPeer = this.accessPeer;
        if (accessPeer != null && (accessPeer.getModifiers() & M_LOCAL) != 0) {
            LocalMember copy = (LocalMember) accessPeer;
            return copy;
        }
        return this;
    }

    /**
     * May inline copies of all the arguments of the given method.
     */
    public static LocalMember[] copyArguments(Context ctx, MemberDefinition field) {
        Vector v = field.getArguments();
        LocalMember res[] = new LocalMember[v.size()];
        v.copyInto(res);
        for (int i = 0; i < res.length; i++) {
            res[i] = res[i].copyInline(ctx);
        }
        return res;
    }

    /**
     * Call this when finished with the result of a copyArguments() call.
     */
    public static void doneWithArguments(Context ctx, LocalMember res[]) {
        for (int i = 0; i < res.length; i++) {
            if (res[i].originalOfCopy.accessPeer == res[i]) {
                res[i].originalOfCopy.accessPeer = null;
            }
        }
    }

    /**
     * Is this local variable's value stable and simple enough to be directly
     * substituted for occurrences of the variable itself?
     * (This decision is made by VarDeclarationStatement.inline().)
     */
    public boolean isInlineable(Environment env, boolean fromFinal) {
        return (getModifiers() & M_INLINEABLE) != 0;
    }

    /**
     * Check if used
     */
    public boolean isUsed() {
        return (readcount != 0) || (writecount != 0);
    }

    LocalMember getAccessVar() {
        return (LocalMember) accessPeer;
    }

    void setAccessVar(LocalMember f) {
        accessPeer = f;
    }

    MemberDefinition getAccessVarMember() {
        return accessPeer;
    }

    void setAccessVarMember(MemberDefinition f) {
        accessPeer = f;
    }

    /**
     * Return value
     */
    public Node getValue(Environment env) {
        return (Expression) getValue();
    }

    /**
     * Value number for vsets, or -1 if none.
     */
    public int getNumber(Context ctx) {
        return number;
    }
}
