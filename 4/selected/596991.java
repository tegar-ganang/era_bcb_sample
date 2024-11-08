/*
 * Tar.java
 *
 * Created on December 3, 2006, 1:03 AM
 *
 * Copyright (c) Joerg Wassmer
 * This library is free software. You can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 2 or above
 * as published by the Free Software Foundation.
 * For more information please visit <http://jaxlib.sourceforge.net>.
 */

package jaxlib.ant.taskdefs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jaxlib.arc.tar.TarEntry;
import jaxlib.arc.tar.TarEntryType;
import jaxlib.arc.tar.TarOutputStream;
import jaxlib.io.IO;
import jaxlib.io.file.unix.UnixFilePermissions;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.MergingMapper;
import org.apache.tools.ant.util.SourceFileScanner;



/**
 * Reimplementation of {@link org.apache.tools.ant.taskdefs.Tar} as of {@code Ant 1.6.5}. At least twice as
 * fast in case of {@code BZip2} compression.
 * <p>
 * All stuff deprecated in the original has been removed.
 * The possible values for the "longFile" attribute have been reduced to "warn", "fail" and "gnu", the
 * default has been changed to "gnu".
 * All other XML attributes and elements are the same as in the original.
 * </p><p>
 * This class depends on {@code JRE 1.5} and the {@code JaXLib} distribution archives {@code jaxlib-lang},
 * {@code jaxlib-core} and {@code jaxlib-arc}.
 * </p><p>
 * Note: the major performance improvements found here, namingly the BZip2 implementation, are shipped with
 * {@code Ant 1.7}. Ant 1.7 is not depending on JaXLib.
 * </p>
 *
 * @author  jw
 * @since   JaXLib 1.0
 * @version $Id: Tar.java 3029 2011-12-29 00:36:48Z joerg_wassmer $
 */
public final class Tar extends MatchingTask
{

  private File tarFile;
  private File baseDir;

  private TarLongFileMode longFileMode = new TarLongFileMode();
  private List<TarFileSet> filesets;

  /**
   * Indicates whether the user has been warned about long files already.
   */
  private boolean longWarningGiven = false;

  private CompressionAttribute compression = new CompressionAttribute();



  public Tar()
  {
    super();
    this.filesets = new ArrayList<>();
    this.longFileMode.setValue(TarLongFileMode.GNU);
  }




  /**
   * Is the archive up to date in relationship to a list of files.
   * @param files the files to check
   * @param dir   the base directory for the files.
   * @return true if the archive is up to date.
   * @since Ant 1.5.2
   */
  private boolean archiveIsUpToDate(final String[] files, final File dir)
  {
    final SourceFileScanner sfs = new SourceFileScanner(this);
    final MergingMapper mm = new MergingMapper();
    mm.setTo(this.tarFile.getAbsolutePath());
    return sfs.restrict(files, dir, null, mm).length == 0;
  }



  /**
   * tar a file
   * @param file the file to tar
   * @param tOut the output stream
   * @param vPath the path name of the file to tar
   * @param tarFileSet the fileset that the file came from.
   * @throws IOException on error
   */
  private void tarFile(
    final File            file,
    final TarOutputStream tOut,
          String          vPath,
    final TarFileSet      tarFileSet
  )
  throws IOException
  {
    final String fullpath = tarFileSet.getFullpath();
    if (fullpath.length() > 0)
      vPath = fullpath;
    else if (vPath.length() <= 0)
      return; // don't add "" to the archive
    else
    {
      if (file.isDirectory() && !vPath.endsWith("/"))
        vPath += "/";

      final String prefix = tarFileSet.getPrefix();
      // '/' is appended for compatibility with the zip task.
      if ((prefix.length() > 0) && !prefix.endsWith("/"))
        vPath = prefix + "/" + vPath;
      else
        vPath = prefix + vPath;
    }

    while (vPath.startsWith("/") && !tarFileSet.getPreserveLeadingSlashes())
    {
      if (vPath.length() <= 1)
        return; // we would end up adding "" to the archive
      vPath = vPath.substring(1);
    }

    FileInputStream fIn = null;
    try
    {
      if (vPath.length() > TarEntry.SHORT_NAME_LENGTH)
      {
        if (this.longFileMode.isWarnMode())
        {
          log(
            "Entry: " + vPath + " longer than " + TarEntry.SHORT_NAME_LENGTH + " characters.",
            Project.MSG_WARN
          );
          if (!this.longWarningGiven)
          {
            log(
              "Resulting tar file can only be processed successfully by GNU compatible tar commands",
              Project.MSG_WARN
            );
            this.longWarningGiven = true;
          }
        }
        else if (this.longFileMode.isFailMode())
        {
          throw new BuildException(
            "Entry: " + vPath + " longer than " + TarEntry.SHORT_NAME_LENGTH + "characters.",
            getLocation()
          );
        }
      }

      final TarEntry te;
      if (file.isDirectory())
      {
        te = new TarEntry(TarEntryType.DIRECTORY);
        te.setPermissions(tarFileSet.dirMode);
      }
      else
      {
        te = new TarEntry(TarEntryType.NORMAL);
        if (file.canExecute())
        {
          te.setPermissions(UnixFilePermissions.valueOf(
            tarFileSet.fileMode.flags | UnixFilePermissions.OWNER_EXEC
          ));
        }
        else
        {
          te.setPermissions(tarFileSet.fileMode);
        }
        if (file.length() <= 0)
        {
          te.setSize(0);
        }
        else
        {
          fIn = new FileInputStream(file);
          final FileChannel inChannel = fIn.getChannel();
          if (inChannel.tryLock(0, te.getSize(), true) == null)
            throw new BuildException("input file is locked exclusively by another process: " + file);
          te.setSize(inChannel.size());
        }
      }
      te.setModTime(file.lastModified());
      te.setGroupId(tarFileSet.getGid());
      te.setGroupName(tarFileSet.getGroup());
      te.setUserId(tarFileSet.getUid());
      te.setUserName(tarFileSet.getUserName());
      te.setPath(vPath);
      tOut.openEntry(te);

      if (fIn != null)
      {
        if (tOut.transferFrom(fIn, te.getSize()) != te.getSize())
          throw new IOException(); // should not happen

        fIn.close();
        fIn = null;
      }

      tOut.closeEntry();
    }
    finally
    {
      IO.tryClose(fIn);
    }
  }





  /**
   * Add a new fileset with the option to specify permissions.
   *
   * @return
   *  the tar fileset to be used as the nested element.
   */
  public final TarFileSet createTarFileSet()
  {
    final TarFileSet fileset = new TarFileSet();
    this.filesets.add(fileset);
    return fileset;
  }



  @Override
  public final void execute() throws BuildException
  {
    if (this.tarFile == null)
      throw new BuildException("tarfile attribute must be set!", getLocation());
    if (this.tarFile.exists() && this.tarFile.isDirectory())
      throw new BuildException("tarfile is a directory!", getLocation());
    if (this.tarFile.exists() && !this.tarFile.canWrite())
      throw new BuildException("Can not write to the specified tarfile!", getLocation());

    final List<TarFileSet> savedFileSets = new ArrayList<>(this.filesets);
    try
    {
      if (this.baseDir != null)
      {
        if (!this.baseDir.exists())
          throw new BuildException("basedir does not exist!", getLocation());

        // add the main fileset to the list of filesets to process.
        final TarFileSet mainFileSet = new TarFileSet(this.fileset);
        mainFileSet.setDir(this.baseDir);
        this.filesets.add(mainFileSet);
      }

      if (this.filesets.isEmpty())
      {
        throw new BuildException(
          "You must supply either a basedir attribute or some nested filesets.",
          getLocation()
        );
      }

      final Project project = getProject();
      LinkedHashMap<FileEntry,TarFileSet> files = null; // lazily initialized

      // check if tar is out of date with respect to each fileset
      boolean upToDate = this.tarFile.isFile();
      for (final TarFileSet fs : this.filesets)
      {
        final String[] fileNames = fs.getFiles(project);
        if ((fileNames.length > 1) && (fs.getFullpath().length() > 0))
        {
          throw new BuildException(
            "fullpath attribute may only be specified for filesets that specify a single file."
          );
        }
        Arrays.sort(fileNames); // sort files for better performance when synchronizing .tar via RSync
        final File dir = fs.getDir(project);
        if (upToDate && !archiveIsUpToDate(fileNames, dir))
          upToDate = false;

        for (final String fileName : fileNames)
        {
          if (files == null)
            files = new LinkedHashMap<>(Math.max(1024, fileNames.length * 2));

          final FileEntry f = new FileEntry(fileName, new File(dir, fileName));
          if (files.put(f, fs) == null)
          {
            if (this.tarFile.equals(f.file))
              throw new BuildException("A tar file cannot include itself", getLocation());
          }
        }
      }

      if (upToDate)
      {
        log("Nothing to do: " + tarFile.getAbsolutePath() + " is up to date.", Project.MSG_INFO);
        return;
      }

      log("Building tar: " + tarFile.getAbsolutePath(), Project.MSG_INFO);

      FileOutputStream fOut = null;
      TarOutputStream tOut = null;
      try
      {
        fOut = new FileOutputStream(this.tarFile);
        final FileLock lock = fOut.getChannel().tryLock();
        if (lock == null)
          throw new BuildException("destination file is write locked: " + this.tarFile);
        tOut = new TarOutputStream(this.compression.createOutputStream(fOut));
        final boolean compressed = !CompressionAttribute.NONE.equals(this.compression.getValue());
        //tOut.setDebug(true);
        //tOut.setLongFileMode(TarOutputStream.LONGFILE_GNU);

        if ((files != null) && !files.isEmpty())
        {
          this.longWarningGiven = false;
          for (
            final Iterator<Map.Entry<FileEntry,TarFileSet>> it = files.entrySet().iterator();
            it.hasNext();
          )
          {
            final Map.Entry<FileEntry,TarFileSet> e = it.next();
            final FileEntry f = e.getKey();
            final TarFileSet fs = e.getValue();
            it.remove();
            tarFile(f.file, tOut, f.childName, fs);
          }
          files = null;
        }

        tOut.close();
        tOut = null;
        fOut = null;
      }
      catch (final IOException ex)
      {
        throw new BuildException("Problem creating TAR: " + ex.getMessage(), ex, getLocation());
      }
      finally
      {
        FileUtils.close(tOut);
        FileUtils.close(fOut);
      }
    }
    finally
    {
      this.filesets = savedFileSets;
    }
  }



  /**
   * Set is the name/location of where to create the tar file.
   * @since Ant 1.5
   * @param destFile The output of the tar
   */
  public final void setDestFile(final File destFile)
  {
    this.tarFile = destFile;
  }

  /**
   * This is the base directory to look in for things to tar.
   * @param baseDir the base directory.
   */
  public final void setBasedir(final File baseDir)
  {
    this.baseDir = baseDir;
  }


  /**
   * Set compression method.
   * Allowable values are
   * <ul>
   * <li>  none - no compression
   * <li>  gzip - Gzip compression
   * <li>  bzip2 - Bzip2 compression
   * </ul>
   * @param mode the compression method.
   */
  public final void setCompression(final CompressionAttribute mode)
  {
    this.compression = mode;
  }


  /**
   * Set how to handle long files, those with a path&gt;100 chars.
   * Optional, default=warn.
   * <p>
   * Allowable values are
   * <ul>
   * <li>  truncate - paths are truncated to the maximum length
   * <li>  fail - paths greater than the maximum cause a build exception
   * <li>  warn - paths greater than the maximum cause a warning and GNU is used
   * <li>  gnu - GNU extensions are used for any paths greater than the maximum.
   * <li>  omit - paths greater than the maximum are omitted from the archive
   * </ul>
   * @param mode the mode to handle long file names.
   */
  public final void setLongfile(final TarLongFileMode mode)
  {
    this.longFileMode = mode;
  }




  private static final class FileEntry extends Object
  {

    final String childName;
    final File file;

    FileEntry(final String childName, final File file)
    {
      super();
      this.childName = childName;
      this.file = file;
    }


    @Override
    public final boolean equals(final Object o)
    {
      return this.file.equals(((FileEntry) o).file);
    }


    @Override
    public final int hashCode()
    {
      return this.childName.hashCode();
    }
  }





  /**
   * This is a FileSet with the option to specify permissions and other attributes.
   */
  public static final class TarFileSet extends FileSet
  {
    private String[] files = null;

    UnixFilePermissions fileMode = UnixFilePermissions.valueOf(
      UnixFilePermissions.ALL_READ | UnixFilePermissions.OWNER_WRITE
    );
    UnixFilePermissions dirMode = UnixFilePermissions.valueOf(
      UnixFilePermissions.ALL_READ | UnixFilePermissions.ALL_EXEC | UnixFilePermissions.OWNER_WRITE
    );


    private String userName = "";
    private String groupName = "";
    private int    uid;
    private int    gid;
    private String prefix = "";
    private String fullpath = "";
    private boolean preserveLeadingSlashes = false;


    /**
     * Creates a new <code>TarFileSet</code> instance.
     *
     */
    public TarFileSet()
    {
      super();
    }


    /**
     * Creates a new <code>TarFileSet</code> instance.
     * Using a fileset as a constructor argument.
     *
     * @param fileset a <code>FileSet</code> value
     */
    public TarFileSet(final FileSet fileset)
    {
      super(fileset);
    }



    /**
     * @return
     *  the current directory mode
     */
    public final int getDirMode()
    {
      return this.dirMode.flags;
    }


    /**
     * Get a list of files and directories specified in the fileset.
     *
     * @param
     *  p the current project.
     *
     * @return
     *  a list of file and directory names, relative to the baseDir for the project.
     */
    public final String[] getFiles(final Project p)
    {
      if (this.files == null)
      {
        final DirectoryScanner ds = getDirectoryScanner(p);
        final String[] directories = ds.getIncludedDirectories();
        final String[] filesPerSe = ds.getIncludedFiles();
        this.files = new String[directories.length + filesPerSe.length];
        System.arraycopy(directories, 0, this.files, 0, directories.length);
        System.arraycopy(filesPerSe, 0, this.files, directories.length, filesPerSe.length);
      }
      return this.files;
    }


    /**
     * @return
     *  the path to use for a single file fileset.
     */
    public final String getFullpath()
    {
      return this.fullpath;
    }


    /**
     * @return
     *  the group identifier.
     */
    public final int getGid()
    {
      return this.gid;
    }


    /**
     * @return
     *  the group name string.
     */
    public final String getGroup()
    {
      return this.groupName;
    }


    /**
     * @return
     *  the current mode.
     */
    public final int getMode()
    {
      return this.fileMode.flags;
    }


    /**
     * @return
     *  the path prefix for the files in the fileset.
     */
    public final String getPrefix()
    {
      return this.prefix;
    }


    /**
     * @return
     *  the leading slashes flag.
     */
    public final boolean getPreserveLeadingSlashes()
    {
      return this.preserveLeadingSlashes;
    }


    /**
     * @return
     *  the uid for the tar entry
     */
    public final int getUid()
    {
      return this.uid;
    }


    /**
     * @return
     *  the user name for the tar entry
     */
    public final String getUserName()
    {
      return this.userName;
    }


    /**
     * A 3 digit octal string, specify the user, group and
     * other modes in the standard Unix fashion;
     * optional, default=0755
     *
     * @param octalString a 3 digit octal string.
     * @since Ant 1.6
     */
    public final void setDirMode(final String octalString)
    {
      this.dirMode = UnixFilePermissions.valueOf(octalString);
    }



    /**
     * If the fullpath attribute is set, the file in the fileset
     * is written with that path in the archive. The prefix attribute,
     * if specified, is ignored. It is an error to have more than one file specified in
     * such a fileset.
     *
     * @param fullpath
     *   the path to use for the file in a fileset.
     */
    public final void setFullpath(final String fullpath)
    {
      this.fullpath = fullpath;
    }



    /**
     * The GID for the tar entry; optional, default="0"
     * This is not the same as the group name.
     *
     * @param
     *   gid the group id.
     */
    public final void setGid(final int gid)
    {
      this.gid = gid;
    }


    /**
     * The groupname for the tar entry; optional, default=""
     * This is not the same as the GID.
     *
     * @param
     *  groupName the group name string.
     */
    public final void setGroup(final String v)
    {
      this.groupName = v;
    }


    /**
     * A 3 digit octal string, specify the user, group and other modes in the standard Unix fashion;
     * optional, default=0644
     *
     * @param
     *  octalString a 3 digit octal string.
     */
    public final void setMode(final String octalString)
    {
      this.fileMode = UnixFilePermissions.valueOf(octalString);
    }


    /**
     * If the prefix attribute is set, all files in the fileset are prefixed with that path in the archive.
     * optional.
     *
     * @param
     *  prefix the path prefix.
     */
    public final void setPrefix(final String prefix)
    {
      this.prefix = prefix;
    }


    /**
     * Flag to indicates whether leading `/'s should be preserved in the file names.
     * Optional, default is <code>false</code>.
     *
     * @param b
     *   the leading slashes flag.
     */
    public final void setPreserveLeadingSlashes(final boolean b)
    {
      this.preserveLeadingSlashes = b;
    }


    /**
     * The uid for the tar entry
     * This is not the same as the User name.
     *
     * @param
     *  uid the id of the user for the tar entry.
     */
    public final void setUid(int uid)
    {
      this.uid = uid;
    }


    /**
     * The username for the tar entry
     * This is not the same as the UID.
     *
     * @param
     *   userName the user name for the tar entry.
     */
    public final void setUserName(final String userName)
    {
      this.userName = userName;
    }
  }








  /**
   * Set of options for long file handling in the task.
   *
   */
  public static final class TarLongFileMode extends EnumeratedAttribute
  {

    /** permissible values for longfile attribute */
    public static final String WARN = "warn";
    public static final String FAIL = "fail";
    public static final String GNU  = "gnu";

    private final String[] validModes = {WARN, FAIL, GNU};


    public TarLongFileMode()
    {
      super();
      setValue(WARN);
    }


    /**
     * @return
     *  the possible values for this enumerated type.
     */
    @Override
    public final String[] getValues()
    {
      return validModes;
    }


    /**
     * @return
     *  true if value is "fail".
     */
    public final boolean isFailMode()
    {
      return FAIL.equalsIgnoreCase(getValue());
    }


    /**
     * @return
     *  true if value is "gnu".
     */
    public final boolean isGnuMode()
    {
      return GNU.equalsIgnoreCase(getValue());
    }


    /**
     * @return
     *  true if value is "warn".
     */
    public final boolean isWarnMode()
    {
      return WARN.equalsIgnoreCase(getValue());
    }
  }


}
