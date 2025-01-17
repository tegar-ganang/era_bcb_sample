package com.google.gwt.core.ext.linker;

import com.google.gwt.core.ext.Linker;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.util.Util;
import com.google.gwt.util.tools.Utility;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An artifact that will be emitted into the output. All EmittedArtifacts
 * contained in the {@link ArtifactSet} at the end of the Linking process will
 * be emitted by the compiler into the module's output directory. This type may
 * be extended by Linker providers to provide alternative implementations of
 * {@link #getContents(TreeLogger)}.
 * 
 * TODO(bobv): provide a timestamp so we can make the time on output files match
 * that of input files?
 */
public abstract class EmittedArtifact extends Artifact<EmittedArtifact> {

    private final String partialPath;

    /**
   * This is mutable because it has no effect on identity.
   */
    private boolean isPrivate;

    protected EmittedArtifact(Class<? extends Linker> linker, String partialPath) {
        super(linker);
        assert partialPath != null;
        this.partialPath = partialPath;
    }

    /**
   * Provides access to the contents of the EmittedResource.
   */
    public abstract InputStream getContents(TreeLogger logger) throws UnableToCompleteException;

    /**
   * Returns the time, measured in milliseconds from the epoch, at which the
   * Artifact was last modified. This will be used to set the last-modified
   * timestamp on the files written to disk.
   * <p>
   * The default implementation always returns the current time. Subclasses
   * should override this method to provide a type-appropriate value.
   * 
   * @return the time at which the Artifact was last modified
   */
    public long getLastModified() {
        return System.currentTimeMillis();
    }

    /**
   * Returns the partial path within the output directory of the
   * EmittedArtifact.
   */
    public final String getPartialPath() {
        return partialPath;
    }

    @Override
    public final int hashCode() {
        return getPartialPath().hashCode();
    }

    /**
   * Returns whether or not the data contained in the EmittedArtifact should be
   * written into the module output directory or into an auxiliary directory.
   * <p>
   * EmittedArtifacts that return <code>true</code> for this method will not
   * be emitted into the normal module output location, but will instead be
   * written into a directory that is a sibling to the module output directory.
   * The partial path of the EmittedArtifact will be prepended with the
   * short-name of the Linker type that created the EmittedArtifact.
   * <p>
   * Private EmittedArtifacts are intended for resources that generally should
   * not be deployed to the server in the same location as the module
   * compilation artifacts.
   */
    public boolean isPrivate() {
        return isPrivate;
    }

    /**
   * Sets the private attribute of the EmittedResource.
   */
    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    @Override
    public String toString() {
        return getPartialPath();
    }

    /**
   * Provides access to the contents of the EmittedResource.
   */
    public void writeTo(TreeLogger logger, OutputStream out) throws UnableToCompleteException {
        try {
            InputStream in = getContents(logger);
            Util.copyNoClose(in, out);
            Utility.close(in);
        } catch (IOException e) {
            logger.log(TreeLogger.ERROR, "Unable to read or write stream", e);
            throw new UnableToCompleteException();
        }
    }

    @Override
    protected final int compareToComparableArtifact(EmittedArtifact o) {
        return getPartialPath().compareTo(o.getPartialPath());
    }

    @Override
    protected final Class<EmittedArtifact> getComparableArtifactType() {
        return EmittedArtifact.class;
    }
}
