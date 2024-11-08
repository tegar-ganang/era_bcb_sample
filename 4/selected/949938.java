package org.rubypeople.rdt.internal.core.search.matching;

import org.rubypeople.rdt.internal.core.util.CharOperation;

public abstract class VariablePattern extends RubySearchPattern {

    protected boolean findDeclarations;

    protected boolean findReferences;

    protected boolean readAccess;

    protected boolean writeAccess;

    protected char[] name;

    public VariablePattern(int patternKind, boolean findDeclarations, boolean readAccess, boolean writeAccess, char[] name, int matchRule) {
        super(patternKind, matchRule);
        this.findDeclarations = findDeclarations;
        this.readAccess = readAccess;
        this.writeAccess = writeAccess;
        this.findReferences = readAccess || writeAccess;
        this.name = (isCaseSensitive() || isCamelCase()) ? name : CharOperation.toLowerCase(name);
    }

    protected boolean mustResolve() {
        return this.findReferences;
    }
}
