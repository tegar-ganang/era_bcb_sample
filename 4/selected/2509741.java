package net.sourceforge.processdash.tool.diff.impl.svn;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import net.sourceforge.processdash.util.Disposable;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.TempFileFactory;

public abstract class SvnCatFile implements SvnFileVersion, SvnTask, Disposable {

    private List<String> catArgs;

    private File tmpFile;

    protected SvnTaskHelper helper;

    public SvnCatFile(String... catArgs) {
        this.catArgs = Arrays.asList(catArgs);
        this.helper = new SvnTaskHelper(this);
    }

    public InputStream getContents() throws IOException {
        helper.waitTillReady();
        return new BufferedInputStream(new FileInputStream(tmpFile));
    }

    public void dispose() {
        if (tmpFile != null) tmpFile.delete();
    }

    public SvnTaskHelper getTaskHelper() {
        return helper;
    }

    public void execute(SvnExecutor svn) throws Exception {
        File result = TempFileFactory.get().createTempFile("svnLocDiffFile", ".tmp");
        InputStream in = svn.exec("cat", catArgs);
        OutputStream out = new FileOutputStream(result);
        FileUtils.copyFile(in, out);
        in.close();
        out.close();
        this.tmpFile = result;
    }
}
