package org.tigris.subversion.svnant.commands;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.tigris.subversion.svnant.SvnAntException;
import org.tigris.subversion.svnant.SvnAntValidationException;
import org.tigris.subversion.svnclientadapter.SVNRevision;
import org.tigris.subversion.svnclientadapter.SVNUrl;

/**
 * svn Cat. 
 * @author C�dric Chabanois 
 *         <a href="mailto:cchabanois@ifrance.com">cchabanois@ifrance.com</a>
 */
public class Cat extends SvnCommand {

    /** url */
    private SVNUrl url = null;

    /** destination file. */
    private File destFile = null;

    /** revision */
    private SVNRevision revision = SVNRevision.HEAD;

    public void execute() throws SvnAntException {
        InputStream is = null;
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(destFile);
            is = svnClient.getContent(url, revision);
            byte[] buffer = new byte[5000];
            int read;
            while ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
        } catch (Exception e) {
            throw new SvnAntException("Can't get the content of the specified file", e);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
	 * Ensure we have a consistent and legal set of attributes
	 */
    protected void validateAttributes() throws SvnAntValidationException {
        if (url == null) throw new SvnAntValidationException("you must set url attr");
        if (destFile == null) destFile = new File(getProject().getBaseDir(), url.getLastPathSegment());
        if (revision == null) throw SvnAntValidationException.createInvalidRevisionException();
    }

    /**
	 * Sets the URL; required.
	 * @param url The url to set
	 */
    public void setUrl(SVNUrl url) {
        this.url = url;
    }

    /**
	 * @param destFile the destFile to set
	 */
    public void setDestFile(File destFile) {
        this.destFile = destFile;
    }

    /**
	 * Sets the revision
	 * 
	 * @param revision
	 */
    public void setRevision(String revision) {
        this.revision = getRevisionFrom(revision);
    }
}
