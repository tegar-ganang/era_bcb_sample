/*
 * ReJar.java
 *
 * Created on December 4, 2006, 19:03 PM
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.util.ArrayList;

import jaxlib.arc.tar.TarEntry;
import jaxlib.arc.tar.TarInputStream;
import jaxlib.io.IO;
import jaxlib.io.file.Files;
import jaxlib.io.file.unix.UnixFilePermissions;
import jaxlib.util.CheckArg;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.types.selectors.SelectorUtils;
import org.apache.tools.ant.util.FileUtils;


/**
 * Reimplementation of {@link org.apache.tools.ant.taskdefs.Tar} as of {@code Ant 1.6.5}. Somewhat faster
 * in case of {@code BZip2} compression.
 * <p>
 * All stuff deprecated in the original has been removed.
 * </p><p>
 * This class depends on {@code JRE 1.5} and on the {@code JaXLib} distribution archives {@code jaxlib-lang},
 * {@code jaxlib-core} and {@code jaxlib-arc}.
 * </p><p>
 * Note: the performance improvements found here, namingly the BZip2 implementation, are shipped with
 * {@code Ant 1.7}. Ant 1.7 is not depending on JaXLib.
 * </p>
 *
 * @author  jw
 * @since   JaXLib 1.0
 * @version $Id: Untar.java 3029 2011-12-29 00:36:48Z joerg_wassmer $
 */
public class Untar extends Task
{


  private CompressionAttribute compression;
  private File destDir;
  private File archiveFile;
  private boolean overwrite = true;
  private ArrayList<FileSet> filesets;
  private ArrayList<PatternSet> patternsets;
  private boolean restoreGroups;
  private boolean restorePermissions;
  private int verbose;

  private boolean useChmod;
  private boolean useChown;
  private boolean useLn;
  private Boolean defaultGroupIsUser;
  private String userName;



  public Untar()
  {
    super();

    this.compression        = new CompressionAttribute();
    this.filesets           = new ArrayList<>();
    this.patternsets        = new ArrayList<>();
    this.restorePermissions = true;
    this.useChmod           = Os.isFamily("unix");
    this.useChown           = this.useChmod;
    this.useLn              = false;
    this.verbose            = Project.MSG_VERBOSE;
  }




  private void expandArchive(final File archiveFile, final File destDir)
  {
    log("Restoring " + archiveFile + " into " + destDir, Project.MSG_INFO);
    FileInputStream fIn = null;
    try
    {
      fIn = new FileInputStream(archiveFile);
      final TarInputStream tIn = new TarInputStream(this.compression.createInputStream(fIn));
      for (TarEntry e; (e = tIn.openEntry()) != null;)
      {
        if (includeEntry(e.getPath()))
        {
          final File destFile = FileUtils.getFileUtils().resolveFile(destDir, e.getPath());
          if (!this.overwrite && destFile.exists() && (destFile.lastModified() >= e.getTimeLastModified()))
          {
            log("Skipping " + destFile + " as it is up-to-date", this.verbose);
          }
          else if (e.getType().isSymbolicLink())
          {
            restoreSymlink(e, destFile, destDir);
          }
          else if (e.getType().isDirectory())
          {
            restoreDirectory(tIn, e, destFile);
            restoreOwningGroup(e, destFile);
            restorePermissions(e, destFile);
          }
          else
          {
            restoreFile(tIn, e, destFile);
            restoreOwningGroup(e, destFile);
            restorePermissions(e, destFile);
          }
        }
      }
      log("restore complete", this.verbose);
      tIn.close();
      fIn = null;
    }
    catch (final IOException ex)
    {
      throw new BuildException("Error while expanding " + archiveFile, ex, getLocation());
    }
    finally
    {
      FileUtils.close(fIn);
    }
  }



  private boolean includeEntry(String name)
  {
    if (this.patternsets.isEmpty())
      return true;

    name = name.replace('/', File.separatorChar).replace('\\', File.separatorChar);
    final Project project = getProject();

    for (final PatternSet p : this.patternsets)
    {
      boolean included = false;
      String[] includes = p.getIncludePatterns(project);
      if ((includes == null) || (includes.length == 0))
      {
        included = true; // no include pattern implicitly means includes="**"
      }
      else
      {
        for (String pattern : includes)
        {
          pattern = pattern.replace('/', File.separatorChar).replace('\\', File.separatorChar);
          if (pattern.endsWith(File.separator))
            pattern += "**";
          included = SelectorUtils.matchPath(pattern, name);
          if (included)
            break;
        }
        includes = null;
      }

      if (!included)
        return false;

      final String[] excludes = p.getExcludePatterns(project);
      if (excludes != null)
      {
        for (String pattern : excludes)
        {
          pattern = pattern.replace('/', File.separatorChar).replace('\\', File.separatorChar);
          if (pattern.endsWith(File.separator))
            pattern += "**";
          included = !SelectorUtils.matchPath(pattern, name);
          if (!included)
            return false;
        }
      }
    }

    return true;
  }



  private void restoreFile(final TarInputStream in, final TarEntry tarEntry, final File destFile)
  throws IOException
  {
    try
    {
      final File parentDir = destFile.getParentFile();
      if (parentDir != null)
        parentDir.mkdirs();

      final long size = tarEntry.getSize();
      RandomAccessFile out = new RandomAccessFile(destFile, "rw");
      try
      {
        if (size > 0)
        {
          final FileChannel fileChannel = out.getChannel();
          if (fileChannel.tryLock(0, Long.MAX_VALUE, false) == null)
            throw new IOException("file is locked: " + destFile);
          if (size > 512)
            out.setLength(size);
          if (in.transferToByteChannel(fileChannel, size) != size)
            throw new IOException();
        }
        out.close();
        out = null;
      }
      finally
      {
        IO.tryClose(out);
      }
      destFile.setLastModified(tarEntry.getTimeLastModified());
      log(tarEntry.getPath() + " => " + destFile, this.verbose);
    }
    catch (final FileNotFoundException ex)
    {
      log("Unable to restore file " + destFile.getPath(), Project.MSG_WARN);
    }
  }



  private void restoreDirectory(final TarInputStream in, final TarEntry tarEntry, final File destFile)
  throws IOException
  {
    if (!destFile.mkdirs() && !destFile.isDirectory())
    {
      log("Unable to restore directory " + destFile.getPath(), Project.MSG_WARN);
    }
    else
    {
      log(tarEntry.getPath() + " => " + destFile, this.verbose);
      destFile.setLastModified(tarEntry.getTimeLastModified());
    }
  }



  private void restoreOwningGroup(final TarEntry e, final File f)
  {
    if (
         !this.restoreGroups
      || !this.useChown
      || !f.toPath().getFileSystem().supportedFileAttributeViews().contains("posix")
    )
      return;

    if (this.defaultGroupIsUser == null)
    {
      this.userName = System.getProperty("user.name");
      try
      {
        final PosixFileAttributes a = Files.readAttributes(f.toPath(), PosixFileAttributes.class);
        final String g = a.group().getName();
        this.defaultGroupIsUser = (g != null) && g.equals(userName);
      }
      catch (final IOException ex)
      {
        this.useChown = false;
        log(
          "Unable to use chown, will not be able to restore group ownership of any files:" +
          "\nfile          = " + f +
          "\nerror message = " + ex.getMessage(),
          Project.MSG_WARN
        );
      }
    }


    String groupName = e.getGroupName();
    if (groupName != null)
    {
      groupName = groupName.trim();
      if (!groupName.isEmpty() && (!this.defaultGroupIsUser || !groupName.equals(this.userName))
      )
      {
        final PosixFileAttributeView a = Files.getFileAttributeView(f.toPath(), PosixFileAttributeView.class);
        try
        {
          a.setGroup(f.toPath().getFileSystem().getUserPrincipalLookupService().lookupPrincipalByGroupName(groupName));
        }
        catch (final IOException ex)
        {
          log(
            "Unable to set group ownership:" +
            "\nfile          = " + f +
            "\ngroup         = " + groupName +
            "\nerror message = " + ex.getMessage(),
            Project.MSG_WARN
          );
        }
      }
    }
  }



  /**
   * Restore permissions.
   * Because java.io.File provides no support for user groups, on non-Unix systems the only safe thing we can
   * do is to distinguish between owner and "other". On Unix like systems we use "chmod".
   */
  private void restorePermissions(final TarEntry e, final File f)
  {
    if (!this.restorePermissions)
      return;

    final UnixFilePermissions permissions = e.getPermissions();

    if (
         this.useChmod
      && ((permissions.flags | UnixFilePermissions.OWNER_ALL) != UnixFilePermissions.OWNER_ALL)
    )
    {
      try
      {
        Files.setPosixFilePermissions(f.toPath(), permissions.toPosix());
        return;
      }
      catch (final UnsupportedOperationException ex)
      {
        this.useChmod = false;
      }
      catch (final IOException ex)
      {
        this.useChmod = false;
        log(
          "Unable to use chmod, will not be able to restore group permissions of any files." +
          "\nfile          = " + f +
          "\nerror message = " + ex.getMessage(),
          Project.MSG_WARN
        );
      }
    }

    f.setReadable(true, !permissions.isReadableByOther());
    f.setWritable(
      permissions.isWritableByOwner(),
      permissions.isWritableByOwner() && !permissions.isWritableByOther()
    );
    if (f.isDirectory())
    {
      f.setExecutable(true, !permissions.isExecutableByOther());
    }
    else
    {
      f.setExecutable(
        permissions.isExecutableByOwner(),
        permissions.isExecutableByOwner() && !permissions.isExecutableByOther()
      );
    }
  }



  private void restoreSymlink(final TarEntry e, final File link, final File destDir)
  {
    if (this.useLn)
    {
      String targetPath = e.getLinkedPath();
      if (targetPath != null)
      {
        targetPath = targetPath.trim();
        if (targetPath.length() > 0)
        {
          final File targetFile = FileUtils.getFileUtils().resolveFile(destDir, targetPath);
          try
          {
            Files.createLink(link.toPath(), targetFile.toPath());
          }
          catch (final IOException ex)
          {
            log(
              "Unable to restore symbolic link:" +
              "\nlink          = " + link +
              "\ntarget file   = " + targetFile +
              "\nerror message = " + ex.getMessage(),
              Project.MSG_WARN
            );
          }
        }
      }
    }
  }



  @Override
  public void execute()
  {
    if ((this.archiveFile == null) && this.filesets.isEmpty())
      throw new BuildException("src attribute and/or filesets must be specified");
    if (this.destDir == null)
      throw new BuildException("dest attribute must be specified");
    if (this.destDir.exists() && !destDir.isDirectory())
      throw new BuildException("Dest must be a directory: " + this.destDir, getLocation());

    if (this.archiveFile != null)
    {
      if (this.archiveFile.isDirectory())
        throw new BuildException("src must not be a directory. Use nested filesets instead.", getLocation());
      else
        expandArchive(this.archiveFile, this.destDir);
    }

    final Project project = getProject();
    for (final FileSet fs : this.filesets)
    {
      final File fromDir = fs.getDir(project);
      for (final String childPath : fs.getDirectoryScanner(project).getIncludedFiles())
        expandArchive(new File(fromDir, childPath), this.destDir);
    }
  }


  public void addFileset(final FileSet set)
  {
    CheckArg.notNull(set, "set");
    this.filesets.add(set);
  }


  public void addPatternset(final PatternSet set)
  {
    CheckArg.notNull(set, "set");
    this.patternsets.add(set);
  }



  public final CompressionAttribute getCompression()
  {
    return this.compression;
  }


  public final File getDest()
  {
    return this.destDir;
  }


  public final boolean getOverwrite()
  {
    return this.overwrite;
  }


  public final boolean getRestoreGroups()
  {
    return this.restoreGroups;
  }


  public final boolean getRestorePermissions()
  {
    return this.restorePermissions;
  }


  public final File getSrc()
  {
    return this.archiveFile;
  }


  public final boolean isVerbose()
  {
    return this.verbose != Project.MSG_VERBOSE;
  }


  /**
   * Set decompression algorithm to use; default=none.
   */
  public void setCompression(final CompressionAttribute v)
  {
    CheckArg.notNull(v, "compression");
    this.compression = v;
  }


  /**
   * Set the destination directory. File will be unzipped into the destination directory.
   *
   * @param v
   *  Path to the directory.
   */
  public void setDest(final File v)
  {
    this.destDir = v;
  }


  /**
   * Should we overwrite files in dest, even if they are newer than
   * the corresponding entries in the archive?
   */
  public void setOverwrite(final boolean v)
  {
    this.overwrite = v;
  }


  public void setRestoreGroups(final boolean v)
  {
    this.restoreGroups = v;
  }


  public void setRestorePermissions(final boolean v)
  {
    this.restorePermissions = v;
  }


  /**
   * Set the path of the Tar archive file.
   */
  public void setSrc(final File v)
  {
    this.archiveFile = v;
  }


  public void setVerbose(final boolean v)
  {
    this.verbose = v ? Project.MSG_INFO : Project.MSG_VERBOSE;
  }

}
