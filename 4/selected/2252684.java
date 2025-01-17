package org.rubypeople.rdt.internal.core.search.matching;

import org.rubypeople.rdt.core.search.SearchPattern;
import org.rubypeople.rdt.internal.core.search.indexing.IIndexConstants;
import org.rubypeople.rdt.internal.core.util.CharOperation;

public class FieldPattern extends VariablePattern implements IIndexConstants {

    protected char[] declaringQualification;

    protected char[] declaringSimpleName;

    protected static char[][] REF_CATEGORIES = { REF };

    protected static char[][] REF_AND_DECL_CATEGORIES = { REF, FIELD_DECL };

    protected static char[][] DECL_CATEGORIES = { FIELD_DECL };

    public static char[] createIndexKey(char[] fieldName) {
        return fieldName;
    }

    public FieldPattern(boolean findDeclarations, boolean readAccess, boolean writeAccess, char[] name, char[] declaringQualification, char[] declaringSimpleName, int matchRule) {
        super(FIELD_PATTERN, findDeclarations, readAccess, writeAccess, name, matchRule);
        this.declaringQualification = isCaseSensitive() ? declaringQualification : CharOperation.toLowerCase(declaringQualification);
        this.declaringSimpleName = isCaseSensitive() ? declaringSimpleName : CharOperation.toLowerCase(declaringSimpleName);
        ((InternalSearchPattern) this).mustResolve = mustResolve();
    }

    public void decodeIndexKey(char[] key) {
        this.name = key;
    }

    public SearchPattern getBlankPattern() {
        return new FieldPattern(false, false, false, null, null, null, R_EXACT_MATCH | R_CASE_SENSITIVE);
    }

    public char[] getIndexKey() {
        return this.name;
    }

    public char[][] getIndexCategories() {
        if (this.findReferences) return this.findDeclarations || this.writeAccess ? REF_AND_DECL_CATEGORIES : REF_CATEGORIES;
        if (this.findDeclarations) return DECL_CATEGORIES;
        return CharOperation.NO_CHAR_CHAR;
    }

    public boolean matchesDecodedKey(SearchPattern decodedPattern) {
        return true;
    }

    protected boolean mustResolve() {
        if (this.declaringSimpleName != null || this.declaringQualification != null) return true;
        return super.mustResolve();
    }

    protected StringBuffer print(StringBuffer output) {
        if (this.findDeclarations) {
            output.append(this.findReferences ? "FieldCombinedPattern: " : "FieldDeclarationPattern: ");
        } else {
            output.append("FieldReferencePattern: ");
        }
        if (declaringQualification != null) output.append(declaringQualification).append('.');
        if (declaringSimpleName != null) output.append(declaringSimpleName).append('.'); else if (declaringQualification != null) output.append("*.");
        if (name == null) {
            output.append("*");
        } else {
            output.append(name);
        }
        return super.print(output);
    }
}
