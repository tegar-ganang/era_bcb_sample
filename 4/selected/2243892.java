package org.jucetice.javascript.classes;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.auth.StaticUserAuthenticator;
import org.jucetice.javascript.ScriptEngine;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

public class ScriptableZipArchive extends ScriptableObject {

    static final String CLASSNAME = "ZipArchive";

    protected static ArchiveStreamFactory factory = new ArchiveStreamFactory();

    ScriptableFile file;

    ArchiveOutputStream out;

    public ScriptableZipArchive() {
    }

    /**
     * 
     * @param newFile
     * @throws IOException
     * @throws FileSystemException
     * @throws ArchiveException
     */
    protected ScriptableZipArchive(ScriptableFile newFile) throws IOException, FileSystemException, ArchiveException {
        if (newFile == null) throw new IllegalArgumentException("Uninitialized ZipArchive object");
        file = newFile;
        out = factory.createArchiveOutputStream("zip", newFile.jsFunction_createOutputStream());
    }

    /**
     * 
     * @param cx
     * @param args
     * @param ctorObj
     * @param inNewExpr
     * @return
     * @throws IllegalArgumentException
     * @throws IOException
     * @throws FileSystemException
     * @throws ArchiveException
     */
    public static Scriptable jsConstructor(Context cx, Object[] args, Function ctorObj, boolean inNewExpr) throws IllegalArgumentException, IOException, FileSystemException, ArchiveException {
        ScriptEngine engine = (ScriptEngine) cx.getThreadLocal("engine");
        if (args.length == 1 && args[0] != Undefined.instance && args[0] instanceof ScriptableFile) {
            return new ScriptableZipArchive((ScriptableFile) args[0]);
        } else if (args.length == 1 && args[0] != Undefined.instance) {
            return new ScriptableZipArchive(new ScriptableFile(engine.resolveFile(args[0].toString(), null)));
        } else if (args.length == 2 && args[0] != Undefined.instance && args[1] != Undefined.instance && args[1] instanceof StaticUserAuthenticator) {
            return new ScriptableZipArchive(new ScriptableFile(engine.resolveFile(args[0].toString(), (StaticUserAuthenticator) args[1])));
        } else if (args.length == 4 && args[0] != Undefined.instance && args[1] != Undefined.instance && args[2] != Undefined.instance && args[3] != Undefined.instance) {
            StaticUserAuthenticator auth = new StaticUserAuthenticator(args[1].toString(), args[2].toString(), args[3].toString());
            return new ScriptableZipArchive(new ScriptableFile(engine.resolveFile(args[0].toString(), auth)));
        }
        throw new IllegalArgumentException("ZipArchive constructor called without argument");
    }

    /**
     * 
     * @return
     */
    public void jsFunction_addFile(ScriptableFile infile) throws IOException {
        if (!infile.jsFunction_exists()) throw new IllegalArgumentException("Cannot add a file that doesn't exists to an archive");
        ZipArchiveEntry entry = new ZipArchiveEntry(infile.getName());
        entry.setSize(infile.jsFunction_getSize());
        out.putArchiveEntry(entry);
        try {
            InputStream inStream = infile.jsFunction_createInputStream();
            IOUtils.copy(inStream, out);
            inStream.close();
        } finally {
            out.closeArchiveEntry();
        }
    }

    /**
     * 
     * @throws Exception
     */
    public static Object jsFunction_openArchive(Context cx, Scriptable thisObj, Object[] args, Function funObj) throws IOException, ArchiveException {
        ScriptableZipArchive archive = (ScriptableZipArchive) thisObj;
        ArrayList entries = archive.openArchive(cx, thisObj);
        return cx.newObject(thisObj, "Array", entries.toArray());
    }

    /**
     * 
     * @throws IOException
     * @throws FileSystemException
     */
    public void jsFunction_close() throws IOException {
        if (out != null) out.close();
    }

    /**
     * 
     * @return
     * @throws IOException
     * @throws ArchiveException
     */
    protected ArrayList openArchive(Context cx, Scriptable scope) throws IOException, ArchiveException {
        InputStream is = file.jsFunction_createInputStream();
        BufferedInputStream buf = new BufferedInputStream(is);
        ArchiveInputStream in = factory.createArchiveInputStream(buf);
        ArrayList entries = new ArrayList();
        long count = 0;
        try {
            ArchiveEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                Scriptable scriptEntry = cx.newObject(scope, "ZipEntry", new Object[] { file, entry, count });
                entries.add(scriptEntry);
                count++;
            }
        } finally {
            in.close();
            is.close();
        }
        return entries;
    }

    /**
     * 
     * @return
     */
    public static ArchiveStreamFactory getFactory() {
        return factory;
    }

    /**
     * 
     * @return
     */
    public String getClassName() {
        return CLASSNAME;
    }

    /**
     * Static methods !
     * 
     * @param scope
     * @param cx
     */
    public static void register(Scriptable scope, Context cx) {
        Scriptable topLevelScope = ScriptableObject.getTopLevelScope(scope);
        ScriptableObject objProto = (ScriptableObject) topLevelScope.get(CLASSNAME, scope);
        objProto.defineFunctionProperties(new String[] { "copy" }, ScriptableZipArchive.class, ScriptableObject.DONTENUM | ScriptableObject.PERMANENT | ScriptableObject.READONLY);
    }

    /**
	 * 
	 * @param cx
	 * @param scope
	 * @param thisObj
	 * @param args
	 * @return
	 */
    public static void copy(Context cx, Scriptable thisObj, Object[] args, Function funObj) throws IOException {
        InputStream input = (InputStream) ((NativeJavaObject) args[0]).unwrap();
        OutputStream output = (OutputStream) ((NativeJavaObject) args[1]).unwrap();
        IOUtils.copy(input, output);
    }
}
