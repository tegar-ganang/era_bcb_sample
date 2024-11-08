package net.sourceforge.javabits.tool.copier;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import net.sourceforge.javabits.error.ErrorHandler;
import net.sourceforge.javabits.io.Files;
import net.sourceforge.javabits.task.AbstractFileTask;
import org.codehaus.plexus.util.FileUtils;

public class CopyTask extends AbstractFileTask {

    private File source;

    private File target;

    public CopyTask() {
    }

    public CopyTask(File baseDirectory, File source, File target) {
        super("Copying '{this.localFile(this.source)}' to '{this.localFile(this.target)}'.", baseDirectory);
        this.source = source;
        this.target = target;
    }

    @Override
    protected void executeTask(ErrorHandler err) throws Exception {
        try {
            File resolvedSource = Files.resolve(getBaseDirectory(), this.source);
            File resolvedTarget = Files.resolve(getBaseDirectory(), this.target);
            FileUtils.copyFile(resolvedSource, resolvedTarget);
        } catch (IOException e) {
            err.error(e);
        }
    }

    public File getSource() {
        return this.source;
    }

    public File getTarget() {
        return this.target;
    }

    /**
     * @see net.sourceforge.javabits.task.AbstractFileTask#getSourceCollection()
     */
    @Override
    public Collection<File> getSourceCollection() {
        return Collections.singleton(source);
    }

    /**
     * @see net.sourceforge.javabits.task.AbstractFileTask#getTargetCollection()
     */
    @Override
    public Collection<File> getTargetCollection() {
        return Collections.singleton(target);
    }
}
