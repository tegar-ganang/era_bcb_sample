/*
 * BZip2.java
 *
 * Created on August 26, 2004, 11:24 PM
 *
 * Copyright (c) Joerg Wassmer
 * This library is free software. You can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 2 or above
 * as published by the Free Software Foundation.
 * For more information please visit <http://jaxlib.sourceforge.net>.
 */

package jaxlib.arc.bzip2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.concurrent.Callable;

import jaxlib.io.IO;
import jaxlib.io.stream.BufferedXInputStream;
import jaxlib.io.stream.BufferedXOutputStream;
import jaxlib.io.stream.NullWriter;
import jaxlib.io.stream.XWriter;
import jaxlib.io.stream.adapter.AdapterWriter;
import jaxlib.io.stream.adapter.PrintStreamAdapter;
import jaxlib.system.SystemProperties;

/**
 * Java port of the original <i>BZip2 v1.0.2 Linux</i> command.
 * <p>
 * <i>Note:</i> This class supports all options of the original <i>bzip2</i> command, but the format
 * of output messages is different.
 * </p>
 *
 * @author  <a href="mailto:joerg.wassmer@web.de">Joerg Wassmer</a>
 * @since   JaXLib 1.0
 * @version $Id: BZip2.java 3034 2012-01-02 22:25:52Z joerg_wassmer $
 */
public final class BZip2 extends Object implements Callable<Void>
{


  public static void main(String[] args) throws IOException
  {
    BZip2 bz = new BZip2(args);
    bz.setLogWriter(System.out);
    bz.call();
  }




  private static final HashMap<String,BZip2.Switch> switchesByOption = new HashMap<>();
  private static final XWriter NULL_LOG = new NullWriter();
  private static final int MAX_PROGRESS = 50;
  private static final String VERSION
    = "bzip2, a block-sorting file compressor. jaxlib.sourceforge.net java port 1.0, 30-Aug-2004.";

  private static BZip2Exception error(String msg)
  {
    return new BZip2Exception("bzip2: ERROR - " + msg);
  }




  private final LinkedHashSet<String> fileNames = new LinkedHashSet<>();
  private final EnumSet<BZip2.Switch> switches = EnumSet.noneOf(BZip2.Switch.class);
  private final String[] args;

  private File baseDir = new File(System.getProperty(SystemProperties.USER_DIR));
  private int blockSize = -1;
  private boolean debug = false;
  private boolean verbose = false;
  private boolean warn = true;

  private XWriter log = NULL_LOG;



  public BZip2(final String... args)
  {
    super();

    if (args == null)
      throw new NullPointerException("args");
    this.args = args;
  }



  private boolean analyseOptions() throws IOException, BZip2Exception
  {
    boolean failIfNoFiles = true;

    this.warn = !this.switches.contains(Switch.quiet);

    if (
      this.switches.contains(Switch.version) ||
      this.switches.contains(Switch.license) ||
      this.switches.contains(Switch.help)
    )
    {
      this.log.println(VERSION);
      this.log.println();
      failIfNoFiles = false;
    }

    if (this.switches.contains(Switch.license))
    {
      // TODO
    }

    if (this.switches.contains(Switch.help))
    {
      // TODO
      return false;
    }


    if (this.fileNames.isEmpty())
    {
      if (failIfNoFiles)
        throw BZip2.error("no file specified");
      return false;
    }


    if (!this.switches.contains(Switch.compress) && !this.switches.contains(Switch.decompress))
      throw BZip2.error("neither \"compress\" nor \"decompress\" specified");
    if (this.switches.contains(Switch.compress) && this.switches.contains(Switch.decompress))
      throw BZip2.error("specify either \"compress\" or \"decompress\", but not both");


    for (Switch sw : this.switches)
    {
      if ((sw.ordinal() >= Switch.blockSize1.ordinal()) && (sw.ordinal() <= Switch.blockSize9.ordinal()))
        this.blockSize = sw.ordinal() + 1;
      else if (sw == Switch.verbose)
      {
        if (this.verbose)
          this.debug = true;
        else
          this.verbose = true;
      }
    }


    if (this.verbose)
    {
      this.log.print("base directory:     ").println(this.baseDir);
      this.log.print("compress:           ").println(this.switches.contains(Switch.compress));
      this.log.print("debug:              ").println(this.debug);
      this.log.print("decompress:         ").println(this.switches.contains(Switch.decompress));
      this.log.print("force:              ").println(this.switches.contains(Switch.force));
      this.log.print("help:               ").println(this.switches.contains(Switch.help));
      this.log.print("maximum blocksize:  ").println((this.blockSize == -1) ? "auto" : this.blockSize);
      this.log.print("quiet:              ").println(!this.warn);
      this.log.print("show license:       ").println(this.switches.contains(Switch.license));
      this.log.print("show progress:      ").println(this.switches.contains(Switch.showProgress));
      this.log.print("show version:       ").println(this.switches.contains(Switch.version));
      this.log.print("small:              ").println(this.switches.contains(Switch.small));
      this.log.print("stdout:             ").println(this.switches.contains(Switch.stdout));
      this.log.print("test:               ").println(this.switches.contains(Switch.test));
      this.log.print("verbose:            ").println(this.verbose);
    }

    return true;
  }


  private boolean checkDestFile(File dst) throws IOException
  {
    if (dst.exists())
    {
      if (this.switches.contains(Switch.force))
      {
        if (!dst.isFile() || !dst.delete())
          throw error("can not overwrite existing file: " + dst);
      }
      else
      {
        if (this.verbose)
          this.log.print("skipping existing file: ").println(dst);
        return false;
      }
    }
    return true;
  }



  private void checkSourceFile(File src) throws IOException
  {
    if (src.isFile())
    {
      if (!src.canRead())
        throw error("can not read file: " + src);
    }
    else
    {
      if (src.exists())
        throw error("not a normal file: " + src);
      throw error("file not found: " + src);
    }
  }


  @SuppressWarnings("finally")
  private void compress(File src) throws IOException
  {
    if (this.switches.contains(Switch.test))
      return;

    checkSourceFile(src);
    if (src.getPath().endsWith(".bz2"))
    {
      this.log.println("WARNING: skipping file because it already has .bz2 suffix:").println(src);
      return;
    }

    final File dst = new File(src.getPath() + ".bz2").getAbsoluteFile();
    if (!checkDestFile(dst))
      return;

    FileChannel       inChannel   = null;
    FileChannel       outChannel  = null;
    FileOutputStream  fileOut     = null;
    BZip2OutputStream bzOut       = null;
    FileLock          inLock      = null;
    FileLock          outLock     = null;

    try
    {
      inChannel = new FileInputStream(src).getChannel();
      final long inSize = inChannel.size();
      inLock = inChannel.tryLock(0, inSize, true);
      if (inLock == null)
        throw error("source file locked by another process: " + src);

      fileOut     = new FileOutputStream(dst);
      outChannel  = fileOut.getChannel();
      bzOut       = new BZip2OutputStream(
        new BufferedXOutputStream(fileOut, 8192),
        Math.min(
          (this.blockSize == -1) ? BZip2OutputStream.MAX_BLOCK_SIZE : this.blockSize,
          BZip2OutputStream.chooseBlockSize(inSize)
        )
      );

      outLock = outChannel.tryLock();
      if (outLock == null)
        throw error("destination file locked by another process: " + dst);

      final boolean showProgress = this.switches.contains(Switch.showProgress);
      long pos = 0;
      int progress = 0;

      if (showProgress || this.verbose)
      {
        this.log.print("source: " + src).print(": size=").println(inSize);
        this.log.println("target: " + dst);
      }

      while (true)
      {
        final long maxStep = showProgress ? Math.max(8192, (inSize - pos) / MAX_PROGRESS) : (inSize - pos);
        if (maxStep <= 0)
        {
          if (showProgress)
          {
            for (int i = progress; i < MAX_PROGRESS; i++)
              this.log.print('#');
            this.log.println(" done");
          }
          break;
        }
        else
        {
          final long step = inChannel.transferTo(pos, maxStep, bzOut);
          if ((step == 0) && (inChannel.size() != inSize))
            throw error("file " + src + " has been modified concurrently by another process");

          pos += step;
          if (showProgress)
          {
            final double  p           = (double) pos / (double) inSize;
            final int     newProgress = (int) (MAX_PROGRESS * p);
            for (int i = progress; i < newProgress; i++)
              this.log.print('#');
            progress = newProgress;
          }
        }
      }

      inLock.release();
      inChannel.close();
      bzOut.closeInstance();
      final long outSize = outChannel.position();
      outChannel.truncate(outSize);
      outLock.release();
      fileOut.close();

      if (this.verbose)
      {
        final double ratio = (inSize == 0) ? (outSize * 100) : ((double) outSize / (double) inSize);
        this.log.print("raw size: ").print(inSize)
          .print("; compressed size: ").print(outSize)
          .print("; compression ratio: ").print(ratio).println('%');
      }

      if (!this.switches.contains(Switch.keep))
      {
        if (!src.delete())
          throw error("unable to delete sourcefile: " + src);
      }
    }
    catch (final IOException ex)
    {
      IO.tryClose(inChannel);
      IO.tryClose(bzOut);
      IO.tryClose(fileOut);
      IO.tryRelease(inLock);
      IO.tryRelease(outLock);
      try
      {
        this.log.println();
      }
      finally
      {
        throw ex;
      }
    }
  }



  @SuppressWarnings("finally")
  private void decompress(final File src) throws IOException
  {
    final String srcPath = src.getPath();
    checkSourceFile(src);
    final boolean test = this.switches.contains(Switch.test);

    final File dst;
    if (test)
      dst = File.createTempFile("jaxlib-bzip", null);
    else
    {
      if (srcPath.endsWith(".bz2"))
        dst = new File(srcPath.substring(0, srcPath.length() - 4));
      else
      {
        this.log.println("WARNING: Can't guess original name, using extension \".out\":").println(srcPath);
        dst = new File(srcPath + ".out");
      }
    }
    if (!checkDestFile(dst))
      return;

    final boolean showProgress = this.switches.contains(Switch.showProgress);

    BZip2InputStream  in          = null;
    FileOutputStream  out         = null;
    FileChannel       outChannel  = null;
    FileLock          inLock      = null;
    FileLock          outLock     = null;

    try
    {
      final FileInputStream in0       = new FileInputStream(src);
      final FileChannel     inChannel = in0.getChannel();
      final long            inSize    = inChannel.size();

      inLock = inChannel.tryLock(0, inSize, true);
      if (inLock == null)
        throw error("source file locked by another process: " + src);

      in          = new BZip2InputStream(new BufferedXInputStream(in0, 8192));
      out         = new FileOutputStream(dst);
      outChannel  = out.getChannel();

      outLock = outChannel.tryLock();
      if (outLock == null)
        throw error("destination file locked by another process: " + dst);

      if (showProgress || this.verbose)
      {
        this.log.print("source: " + src).print(": size=").println(inSize);
        this.log.println("target: " + dst);
      }

      long  pos       = 0;
      int   progress  = 0;
      final long maxStep = showProgress ? Math.max(8192, inSize / MAX_PROGRESS) : Integer.MAX_VALUE;

      while (true)
      {
        final long step = outChannel.transferFrom(in, pos, maxStep);
        if (step <= 0)
        {
          final long a = inChannel.size();
          if (a != inSize)
            throw error("file " + src + " has been modified concurrently by another process");

          if (inChannel.position() >= inSize)
          {
            if (showProgress)
            {
              for (int i = progress; i < MAX_PROGRESS; i++)
                this.log.print('#');
              this.log.println(" done");
            }
            break;
          }
        }
        else
        {
          pos += step;
          if (showProgress)
          {
            final double  p           = (double) inChannel.position() / (double) inSize;
            final int     newProgress = (int) (MAX_PROGRESS * p);
            for (int i = progress; i < newProgress; i++)
              this.log.print('#');
            progress = newProgress;
          }
        }
      }

      final long outSize = outChannel.size();
      in.close();
      out.close();

      if (this.verbose)
      {
        final double ratio = (outSize == 0) ? (inSize * 100) : ((double) inSize / (double) outSize);
        this.log.print("compressed size: ").print(inSize)
          .print("; decompressed size: ").print(outSize)
          .print("; compression ratio: ").print(ratio).println('%');
      }

      if (!test && !this.switches.contains(Switch.keep))
      {
        if (!src.delete())
          throw error("unable to delete sourcefile: " + src);
      }

      if (test && !dst.delete())
        throw error("unable to delete testfile: " + dst);
    }
    catch (final IOException ex)
    {
      IO.tryClose(in);
      IO.tryClose(out);
      IO.tryRelease(inLock);
      IO.tryRelease(outLock);
      try
      {
        this.log.println();
      }
      finally
      {
        throw ex;
      }
    }

  }



  private void execute() throws IOException, BZip2Exception
  {
    parseOptions();
    if (!analyseOptions())
      return;

    final boolean compress = this.switches.contains(Switch.compress);
    for (final String fileName : this.fileNames)
    {
      final File f = (fileName.startsWith(File.separator) ? new File(fileName) : new File(this.baseDir, fileName))
        .getAbsoluteFile();
      if (compress)
        compress(f);
      else
        decompress(f);
    }
  }



  private void parseOptions() throws BZip2Exception
  {
    final String[] args = this.args;
    boolean eachIsFileName = false;

    int optionIndex = 1;

    for (int i = 0; i < args.length; i++)
    {
      final String arg = args[i];

      if (eachIsFileName)
      {
        // after option "--" or after first filename
        this.fileNames.add(arg);
        optionIndex++;
      }
      else if (arg.startsWith("--"))
      {
        // long option
        if (arg.equals("--"))
        {
          eachIsFileName = true;
        }
        else
        {
          final Switch sw = BZip2.switchesByOption.get(arg.substring(2));
          if (sw == null)
            throw BZip2.error("unknown option " + optionIndex + ":\"" + arg + "\"");
          this.switches.add(sw);
        }
        optionIndex++;
      }
      else if (arg.startsWith("-"))
      {
        // short option

        if (arg.length() == 1)
          throw BZip2.error("malformed option " + (i + 1) + ":\"" + arg + "\"");

        for (int j = 1, hj = arg.length(); j < hj; j++)
        {
          optionIndex++;
          final String opt  = arg.substring(j, j + 1);
          final Switch sw   = BZip2.switchesByOption.get(opt);
          if (sw == null)
            throw BZip2.error("unknown option " + optionIndex + ":\"" + opt + "\"");
          this.switches.add(sw);
        }
      }
      else
      {
        // filename
        this.fileNames.add(arg);
        eachIsFileName = true;
      }
    }
  }




  public void setLogWriter(final PrintStream log)
  {
    this.log = (log == null) ? NULL_LOG : new PrintStreamAdapter(log);
  }

  public void setLogWriter(final Writer log)
  {
    this.log = (log == null) ? NULL_LOG : AdapterWriter.asXWriter(log);
  }



  @Override
  public Void call() throws IOException, BZip2Exception
  {
    Throwable ex = null;

    try
    {
      execute();
    }
    catch (final RuntimeException sex)
    {
      ex = sex;
    }
    catch (final IOException sex)
    {
      ex = sex;
    }
    finally
    {
      if (ex != null)
      {
        final String msg = ex.getMessage();
        this.log.println((msg == null) ? ex.toString() : msg);
      }
    }

    return null;
  }












  private static enum Switch
  {

    blockSize1  ("1", "fast"),
    blockSize2  ("2", null),
    blockSize3  ("3", null),
    blockSize4  ("4", null),
    blockSize5  ("5", null),
    blockSize6  ("6", null),
    blockSize7  ("7", null),
    blockSize8  ("8", null),
    blockSize9  ("9", "best"),
    compress    ("z", "compress"),
    decompress  ("d", "decompress"),
    force       ("f", "force"),
    help        ("h", "help"),
    keep        ("k", "keep"),
    license     ("L", "license"),
    quiet       ("q", "quiet"),
    showProgress("p", "show-progress"),
    small       ("s", "small"),
    stdout      ("c", "stdout"),
    test        ("t", "test"),
    verbose     ("v", "verbose"),
    version     ("V", "version");




    final String longOption;
    final String shortOption;

    Switch(final String shortOption, final String longOption)
    {
      this.shortOption = shortOption;
      this.longOption = longOption;

      if (longOption != null)
        BZip2.switchesByOption.put(longOption, this);
      if (shortOption != null)
        BZip2.switchesByOption.put(shortOption, this);
    }

  }





}
