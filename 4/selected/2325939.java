package org.eclipse.jdt.internal.core.search.matching;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.search.IJavaSearchConstants;

public abstract class VariablePattern extends JavaSearchPattern {

    protected boolean findDeclarations = false;

    protected boolean findReferences = false;

    protected boolean readAccess = false;

    protected boolean writeAccess = false;

    protected char[] name;

    public static final int FINE_GRAIN_MASK = IJavaSearchConstants.SUPER_REFERENCE | IJavaSearchConstants.QUALIFIED_REFERENCE | IJavaSearchConstants.THIS_REFERENCE | IJavaSearchConstants.IMPLICIT_THIS_REFERENCE;

    public VariablePattern(int patternKind, char[] name, int limitTo, int matchRule) {
        super(patternKind, matchRule);
        this.fineGrain = limitTo & FINE_GRAIN_MASK;
        if (this.fineGrain == 0) {
            switch(limitTo & 0xF) {
                case IJavaSearchConstants.DECLARATIONS:
                    this.findDeclarations = true;
                    break;
                case IJavaSearchConstants.REFERENCES:
                    this.readAccess = true;
                    this.writeAccess = true;
                    break;
                case IJavaSearchConstants.READ_ACCESSES:
                    this.readAccess = true;
                    break;
                case IJavaSearchConstants.WRITE_ACCESSES:
                    this.writeAccess = true;
                    break;
                case IJavaSearchConstants.ALL_OCCURRENCES:
                    this.findDeclarations = true;
                    this.readAccess = true;
                    this.writeAccess = true;
                    break;
            }
            this.findReferences = this.readAccess || this.writeAccess;
        }
        this.name = (this.isCaseSensitive || this.isCamelCase) ? name : CharOperation.toLowerCase(name);
    }

    protected boolean mustResolve() {
        return this.findReferences || this.fineGrain != 0;
    }
}
