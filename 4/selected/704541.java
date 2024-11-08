package com.lolcode.eclipselol.impl.lim;

import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;
import lolcode.Lolcode;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import com.lolcode.eclipselol.core.builder.CompileException;
import com.lolcode.eclipselol.core.builder.Compiler;
import com.lolcode.eclipselol.core.builder.CompilerContext;

/**
 * This is an implementation of the LOLCode Compiler for LIM.
 * 
 * @author J. Suereth
 *
 */
public class LIMCompiler implements Compiler {

    public static final String COMPILED_EXTENSION = "lmao";

    /**
	 * Check for compiled (.lmao) files and deletes them.
	 */
    public void clean(CompilerContext context) throws CompileException {
        IPath path = context.getFile().getProjectRelativePath().removeFileExtension().addFileExtension(COMPILED_EXTENSION);
        IFile file = context.getFile().getProject().getFile(path);
        if (file.exists()) {
            try {
                file.delete(true, null);
            } catch (CoreException e) {
                throw new CompileException(e);
            }
        }
    }

    /**
	 * Compiled a .lol file with LIM.
	 */
    public void compile(CompilerContext context) throws CompileException {
        try {
            Lolcode compiler = new Lolcode();
            StringWriter writer = new StringWriter();
            InputStream input = context.getFile().getContents();
            while (input.available() > 0) {
                writer.write(input.read());
            }
            if (compiler.compile(writer.toString())) {
                File file = context.getFile().getRawLocation().removeFileExtension().addFileExtension(COMPILED_EXTENSION).toFile();
                if (!file.exists()) {
                    file.createNewFile();
                }
                compiler.serialize(file.getCanonicalPath());
                context.getFile().getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
                IFile compiledFile = context.getFile().getProject().getFile(context.getFile().getProjectRelativePath().removeFileExtension().addFileExtension(COMPILED_EXTENSION));
                if (compiledFile.exists()) {
                    compiledFile.setDerived(true);
                }
            } else {
                context.addError(compiler.getErrorLine(), 0, "Problem compiling this line");
            }
        } catch (Exception e) {
            throw new CompileException(e);
        }
    }
}
