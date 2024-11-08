package net.sourceforge.refactor4pdt.core.filescope;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import net.sourceforge.refactor4pdt.core.PhpRefactoringInfo;
import net.sourceforge.refactor4pdt.core.PhpRefactoringVisitor;
import net.sourceforge.refactor4pdt.log.Log;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.php.internal.core.ast.nodes.ASTParser;
import org.eclipse.php.internal.core.ast.nodes.Program;

public abstract class Scope {

    public static Scope createScope(int type, PhpRefactoringInfo info) {
        Log.write(net.sourceforge.refactor4pdt.log.Log.Level.INFO_L3, net.sourceforge.refactor4pdt.core.filescope.Scope.class, "<static>", "Scope creation");
        Scope result;
        switch(type) {
            case 0:
                Log.write(net.sourceforge.refactor4pdt.log.Log.Level.INFO_L3, net.sourceforge.refactor4pdt.core.filescope.Scope.class, "<static> createScope", "static create new single file");
                result = new ScopeFile(info);
                break;
            case 1:
                Log.write(net.sourceforge.refactor4pdt.log.Log.Level.INFO_L3, net.sourceforge.refactor4pdt.core.filescope.Scope.class, "<static> createScope", "static create new project scope");
                result = new ScopeProject(info);
                break;
            case 2:
                Log.write(net.sourceforge.refactor4pdt.log.Log.Level.WARNING, net.sourceforge.refactor4pdt.core.filescope.Scope.class, "<static> createScope", "Not implemented! static create new include scope");
                result = null;
                break;
            case 3:
                Log.write(net.sourceforge.refactor4pdt.log.Log.Level.INFO_L3, net.sourceforge.refactor4pdt.core.filescope.Scope.class, "<static> createScope", "static create new workspace scope");
                result = new ScopeWorkspace(info);
                break;
            default:
                Log.write(net.sourceforge.refactor4pdt.log.Log.Level.ERROR, net.sourceforge.refactor4pdt.core.filescope.Scope.class, "<static> createScope", (new StringBuilder("Scope doesn't exists: ")).append(type).toString());
                result = null;
                break;
        }
        return result;
    }

    public Scope(int type, PhpRefactoringInfo info) {
        this.info = info;
        this.type = type;
        rootNode = new HashMap<IFile, Program>();
        visitors = new HashMap<IFile, PhpRefactoringVisitor>();
        documents = new HashMap<IFile, Document>();
        files = new HashSet<IFile>();
        files.add(info.getSelectedFile());
        loaded = false;
    }

    public Program getRootNode(IFile file) {
        return rootNode.get(file);
    }

    public Program getSelectedRootNode() {
        return rootNode.get(info.getSelectedFile());
    }

    public void setRootNode(IFile file, Program rootNode) {
        this.rootNode.put(file, rootNode);
    }

    public void loadAllPhpAst(RefactoringStatus status) {
        loadAllFile(status);
        for (Iterator<IFile> iterator = files.iterator(); iterator.hasNext(); ) {
            IFile file = iterator.next();
            if (!rootNode.containsKey(file)) loadPhpAst(file, status);
        }
        loaded = true;
    }

    public HashSet<IFile> getFiles() {
        return files;
    }

    public Set<IFile> getLoadedFiles() {
        return rootNode.keySet();
    }

    public int getType() {
        return type;
    }

    public RefactoringStatus runVisitors(RefactoringStatus status) {
        loadAllPhpAst(status);
        for (Iterator iterator = getLoadedFiles().iterator(); iterator.hasNext(); ) {
            IFile file = (IFile) iterator.next();
            if (!visitors.containsKey(file)) {
                PhpRefactoringVisitor visitor = info.getFactory().makeVisitor();
                if (visitor == null) {
                    Log.write(net.sourceforge.refactor4pdt.log.Log.Level.ERROR, getClass(), "runVisitors", "visitor is null!!");
                    status.addFatalError(Messages.Scope_VisitorIsNull);
                } else {
                    getRootNode(file).traverseTopDown(visitor);
                    visitors.put(file, visitor);
                    Log.write(net.sourceforge.refactor4pdt.log.Log.Level.INFO_L1, getClass(), "runVisitors", (new StringBuilder("Visitor should be loaded!!! file: ")).append(file.getName()).toString());
                }
            } else {
                Log.write(net.sourceforge.refactor4pdt.log.Log.Level.INFO_L3, getClass(), "runVisitors", "visitor already loaded!");
            }
        }
        return status;
    }

    public Collection<PhpRefactoringVisitor> getVisitors() {
        return visitors.values();
    }

    public PhpRefactoringVisitor getVisitor(IFile file) {
        return visitors.get(file);
    }

    protected abstract void loadAllFile(RefactoringStatus refactoringstatus);

    public int getLine(IFile file, int offset) {
        int result = 0;
        if (documents.containsKey(file)) try {
            result = 1 + documents.get(file).getLineOfOffset(offset);
        } catch (BadLocationException e) {
            Log.write(net.sourceforge.refactor4pdt.log.Log.Level.ERROR, getClass(), "getLine", "Could not found offset in file", e);
        } else Log.write(net.sourceforge.refactor4pdt.log.Log.Level.ERROR, getClass(), "getLine", "Could not found file");
        return result;
    }

    public boolean checkChar(IFile file, int offset, String regex_match) {
        return getCode(file, offset, 1).matches(regex_match);
    }

    public String getCode(IFile file, int offset, int length) {
        String result = "";
        if (documents.containsKey(file)) try {
            result = documents.get(file).get(offset, length);
        } catch (BadLocationException e) {
            Log.write(net.sourceforge.refactor4pdt.log.Log.Level.ERROR, getClass(), "getCode", "Could not found offset in file", e);
        } else Log.write(net.sourceforge.refactor4pdt.log.Log.Level.ERROR, getClass(), "getCode", "Could not found file");
        return result;
    }

    public String getLineDelimiter(IFile file) {
        String result = "";
        if (documents.containsKey(file)) result = documents.get(file).getDefaultLineDelimiter(); else Log.write(net.sourceforge.refactor4pdt.log.Log.Level.ERROR, getClass(), "getLineDelimiter", "Could not found file");
        return result;
    }

    private boolean loadPhpAst(IFile file, RefactoringStatus status) {
        boolean result = false;
        if (file == null || !file.exists()) {
            Log.write(net.sourceforge.refactor4pdt.log.Log.Level.FATAL_ERROR, getClass(), "loadPhpAst", "no source file");
            status.addFatalError(Messages.Scope_NoSourceFile);
        } else if (file.isReadOnly()) {
            Log.write(net.sourceforge.refactor4pdt.log.Log.Level.FATAL_ERROR, getClass(), "loadPhpAst", "read only");
            status.addFatalError(Messages.Scope_ReadOnly);
        } else {
            try {
                String content = readFileContent(file, status);
                ASTParser parser = ASTParser.newParser(new InputStreamReader(file.getContents()), ASTParser.VERSION_PHP5, false);
                rootNode.put(file, parser.createAST(null));
                documents.put(file, new Document(content));
                Log.write(net.sourceforge.refactor4pdt.log.Log.Level.INFO_L2, getClass(), "loadPhpAst", (new StringBuilder("loadPhpAst: \n\n")).append(rootNode.get(file).toString()).append("\n").toString());
                result = true;
            } catch (Exception e) {
                Log.write(net.sourceforge.refactor4pdt.log.Log.Level.FATAL_ERROR, getClass(), "getLine", "Error in ASTParser.parse", e);
                status.addFatalError(Messages.Scope_ParserError);
            }
        }
        return result;
    }

    private String readFileContent(IFile file, RefactoringStatus status) {
        String result = null;
        try {
            InputStream is = file.getContents();
            byte buf[] = new byte[1024];
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            for (int len = is.read(buf); len > 0; len = is.read(buf)) bos.write(buf, 0, len);
            is.close();
            result = new String(bos.toByteArray());
        } catch (Exception _ex) {
            String msg = Messages.Scope_FaildToReadFile.replaceFirst("\\[FileName\\]", file.getName());
            Log.write(net.sourceforge.refactor4pdt.log.Log.Level.FATAL_ERROR, getClass(), "readFileContent", msg);
            status.addFatalError(msg);
        }
        return result;
    }

    protected static final String PHP_EXTENSIONS = "|php|php3|php4|php5|phtml|inc|tpl|";

    public static final int SCOPE_SINGLEFILE = 0;

    public static final int SCOPE_PROJECT = 1;

    public static final int SCOPE_INCLUDE = 2;

    public static final int SCOPE_WORKSPACE = 3;

    public static final int SCOPE_ARRAY_SIZE = 4;

    protected HashMap<IFile, Program> rootNode;

    protected PhpRefactoringInfo info;

    protected HashSet<IFile> files;

    protected boolean loaded;

    protected int type;

    protected HashMap<IFile, PhpRefactoringVisitor> visitors;

    protected HashMap<IFile, Document> documents;
}
