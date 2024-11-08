package org.rubypeople.rdt.internal.core.search.matching;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.rubypeople.rdt.core.IRubyElement;
import org.rubypeople.rdt.core.ISourceFolderRoot;
import org.rubypeople.rdt.core.search.IRubySearchScope;
import org.rubypeople.rdt.core.search.SearchParticipant;
import org.rubypeople.rdt.internal.core.LocalVariable;
import org.rubypeople.rdt.internal.core.index.Index;
import org.rubypeople.rdt.internal.core.search.IndexQueryRequestor;
import org.rubypeople.rdt.internal.core.search.RubySearchScope;
import org.rubypeople.rdt.internal.core.search.indexing.IIndexConstants;
import org.rubypeople.rdt.internal.core.util.Util;

public class LocalVariablePattern extends VariablePattern implements IIndexConstants {

    LocalVariable localVariable;

    public LocalVariablePattern(boolean findDeclarations, boolean readAccess, boolean writeAccess, LocalVariable localVariable, int matchRule) {
        super(LOCAL_VAR_PATTERN, findDeclarations, readAccess, writeAccess, localVariable.getElementName().toCharArray(), matchRule);
        this.localVariable = localVariable;
    }

    public void findIndexMatches(Index index, IndexQueryRequestor requestor, SearchParticipant participant, IRubySearchScope scope, IProgressMonitor progressMonitor) {
        ISourceFolderRoot root = (ISourceFolderRoot) this.localVariable.getAncestor(IRubyElement.SOURCE_FOLDER_ROOT);
        String documentPath;
        String relativePath;
        IPath path = this.localVariable.getPath();
        documentPath = path.toString();
        relativePath = Util.relativePath(path, 1);
        if (scope instanceof RubySearchScope) {
            RubySearchScope javaSearchScope = (RubySearchScope) scope;
            if (!requestor.acceptIndexMatch(documentPath, this, participant)) throw new OperationCanceledException();
        } else if (scope.encloses(documentPath)) {
            if (!requestor.acceptIndexMatch(documentPath, this, participant)) throw new OperationCanceledException();
        }
    }

    protected StringBuffer print(StringBuffer output) {
        if (this.findDeclarations) {
            output.append(this.findReferences ? "LocalVarCombinedPattern: " : "LocalVarDeclarationPattern: ");
        } else {
            output.append("LocalVarReferencePattern: ");
        }
        output.append(this.localVariable.toStringWithAncestors());
        return super.print(output);
    }
}
