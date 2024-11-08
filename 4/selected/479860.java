package org.rhinojetty.brazil;

import org.rhinojetty.*;
import org.rhinojetty.pico.*;
import org.rhinojetty.bin.shell;
import org.mozilla.javascript.*;
import com.syntelos.iou.*;
import java.io.*;
import java.util.*;

/**
 * This class incorporates the Page cache "page" element, PageBin pico
 * function registration, and the page cache subsystem.  
 *
 * <p> <b>Page Cache</b>
 * 
 * <p> A <tt>JSP</tt> is compiled into page cache for runtime
 * interpretation.  The page cache exists in a subdirectory of each
 * server- request working directory called <tt>`pagecache'</tt>.  A
 * file <tt>`index.html'</tt> is compiled to cache as
 * <tt>`pagecache/index.html'</tt>.
 *
 * <p> The page cache subsystem functions are named <tt>"PC_*"</tt>.
 * The <tt>"PC_init"</tt> must be called to initialize the in- memory
 * super cache.  The <tt>"PC_dir"</tt> can be used to precompile a
 * directory of sources into the page cache and to preload the in-
 * memory super cache.
 * 
 * <p> The <tt>"PageBin"</tt> pico function can be registered
 * independently of the page cache subsystem initialization using
 * <tt>"SetPageBin"</tt>.
 *
 * <p> <b>Page Cache File Format</b>
 *
 * <p> On- disk, the page cache element is a file with the following
 * binary, big- endian format.
 *
 * <pre>
 * [ data-length : uint16
 * 
 *   data-type   : uint16 {BIN,TXT,SGML,LONGLINE}
 * 
 *   [data-length bytes]
 * ]+
 * </pre> 
 *
 * <p> The data type word has a data type constant in its first byte
 * (XDR/NET).  The data type is one of either BIN, TXT, SGML or
 * LONGLINE.  
 *
 * <p> The data type mask is <tt>"0xff000000"</tt>.  The low three
 * bytes have one purpose for TXT, BIN and SGML "simple" types, and
 * another role for the LONGLINE "compound" type.
 *
 * <p> One or more data blocks are concatenated in order and in two
 * dimensions, into a page cache element.  The LONGLINE type contains
 * multiple BIN, TXT or SGML elements as the second dimension.  The
 * first dimension is a series of elements of any type.
 *
 * <p> Each of the "simple" segment types (BIN, TXT and SGML) can
 * exist either on the top level, or contained within LONGLINE
 * objects.
 *
 * <pre>
 * [ data-length : uint16
 *   data-type   : BIN           ; src-type = data-type &amp; 0x0ff0
 *                               ; src-type-version = data-type &amp; 0x000f
 *   [java bytecode compiled from jsp (SGML segment)
 *    with function name `sect_lno_col' ]
 * ]+
 * [ data-length : uint16
 *   data-type   : SGML          ; src-type = data-type &amp; 0x0ff0
 *                               ; src-type-version = data-type &amp; 0x000f
 *   [sgml data]
 * ]+
 * [ data-length : uint16
 *   data-type   : TXT           ; src-type = data-type &amp; 0x00ff0
 *                               ; src-type-version = data-type &amp; 0x000f
 *   [non- sgml data]
 * ]+
 * [ data-length : uint16
 *   data-type   : LONGLINE      ; num-subline = data-type &amp; 0x0fff 
 *   [num-subline many BIN|TXT|SGML elements]
 * ]+
 * </pre> 
 *
 * <p><b>Not MT_SAFE</b>
 *
 * <p> The "page" API is used by only one thread, otherwise its use must be
 * externally serialized.  For this purpose, the <tt>`lock'</tt> field
 * is uninitialized but available.
 * 
 * @see #PC_send
 * @see #PC_dir
 * @see #PC_types
 * @see #SetPageBin 
 * @see PageBin
 *
 * @author John Pritchard 
 */
public final class page implements PageBin, Cloneable {

    private static final short zero = (short) 0;

    public static final file filstr(String str) {
        file rootdir = base.rootpath();
        if (null != rootdir) return file.NewFile(rootdir, str); else return file.NewFile(str);
    }

    private static final Hashtable cachef_cache = new Hashtable(233);

    /**
     * Return cache file for source file, create the cache
     * subdirectory is it doesn't already exist.
     */
    public static final file cachef(file source) {
        String source_path = source.path();
        file cf = (file) cachef_cache.get(source_path);
        if (null != cf) return cf.cloneFile(); else {
            String fname = source.name();
            if (null != fname) fname = fname.replace('.', '-');
            fname = chbuf.cat(fname, ".rjpg");
            if (source.isFSys()) {
                String dir = source.parent();
                if (null == dir) {
                    dir = CACHE_DIRNAME;
                    try {
                        dir = new File(dir).getCanonicalPath();
                    } catch (IOException iox) {
                        dir = new File(dir).getAbsolutePath();
                    }
                } else dir = chbuf.fcat(dir, CACHE_DIRNAME);
                File df = new File(dir);
                if ((!df.exists()) && (!df.mkdirs())) throw new IllegalStateException("Pagecache can't create directory `" + dir + "'.");
                cf = file.NewFile(df, fname);
                cachef_cache.put(source_path, cf);
                return cf.cloneFile();
            } else {
                String dir = source.parent();
                if (null == dir) dir = CACHE_DIRNAME; else dir = chbuf.fcat(file.RJar2User(dir), CACHE_DIRNAME);
                File df = new File(dir);
                if ((!df.exists()) && (!df.mkdirs())) throw new IllegalStateException("Pagecache can't create directory `" + dir + "'.");
                cf = file.NewFile(new File(df, fname));
                cachef_cache.put(source_path, cf);
                return cf.cloneFile();
            }
        }
    }

    /**
     * Return cache JSP directory for page.  Used from PBJS.
     */
    public static final file cachedir(page pg) {
        String cached_path = chbuf.cat(pg.toString(), "-jsp");
        file cf = (file) cachef_cache.get(cached_path);
        if (null != cf) return cf.cloneFile(); else {
            File cached = new File(cached_path);
            if ((!cached.exists()) && (!cached.mkdirs())) throw new IllegalStateException("Pagecache can't create directory `" + cached + "'.");
            cf = file.NewFile(cached);
            cachef_cache.put(cached_path, cf);
            return cf.cloneFile();
        }
    }

    public static final String fread(file loc) throws IOException {
        int len = 4096, read;
        byte[] buf = new byte[len];
        InputStream in = loc.getInputStream();
        if (null == in) throw new IOException("File resource (" + loc.path() + ") not found."); else {
            try {
                bbuf bbu = new bbuf();
                while (0 < (read = in.read(buf, 0, len))) bbu.write(buf, 0, read);
                if (0 < bbu.length()) return new String(utf8.decode(bbu.toByteArray())); else return null;
            } finally {
                in.close();
            }
        }
    }

    public static final void fwrite(file loc, byte[] buf, int ofs, int len) {
        try {
            OutputStream out = loc.getOutputStream();
            if (null == out) throw new IllegalArgumentException("Can't write to resource (" + loc.path() + ")."); else {
                try {
                    out.write(buf, ofs, len);
                    return;
                } finally {
                    out.close();
                }
            }
        } catch (IOException iox) {
            throw new RuntimeException("Error writing `" + loc.toString() + "'.");
        }
    }

    private static final byte LF = (byte) '\n';

    public static final void fwriteln(file loc, byte[] buf, int ofs, int len) {
        try {
            OutputStream out = loc.getOutputStream();
            if (null == out) throw new IllegalArgumentException("Can't write to resource (" + loc.path() + ").");
            try {
                out.write(buf, ofs, len);
                out.write(LF);
                return;
            } finally {
                out.close();
            }
        } catch (IOException iox) {
            if (base.sysdebug) iox.printStackTrace();
            throw new RuntimeException("Error writing `" + loc.toString() + "'.");
        }
    }

    /**
     * If file doesn't exist, create a file with length one (with byte zero).
     */
    public static final void fexist(file fil) throws IOException {
        if (fil.exists()) return; else {
            OutputStream out = fil.getOutputStream();
            if (null != out) {
                try {
                    out.write(zero);
                    return;
                } finally {
                    out.close();
                }
            }
        }
    }

    /**
     * JSP compilation filename filter used by <tt>`PC_dir'</tt> JSP
     * compiler.  Maintains list of filename extensions (types) for
     * interpretation as JSPs.  This list is manipulated by the server
     * configuration <tt>`httpd.properties'</tt> property
     * <tt>"pagecache-types"</tt>.  
     * 
     * <p> The default types <tt>"html"</tt> and <tt>"wml"</tt> are
     * hard- coded into this class.
     * 
     * @see page#PC_dir
     * @see server 
     */
    public static class ffilter extends Hashtable implements FilenameFilter {

        protected String[] types;

        public ffilter() {
            super();
            put("sgml", Boolean.TRUE);
            put("vrml", Boolean.TRUE);
            put("html", Boolean.TRUE);
            put("htm", Boolean.TRUE);
            put("wml", Boolean.TRUE);
            put("xml", Boolean.TRUE);
            types = linebuf.toStringArray(keys());
        }

        public int replace(String[] ext) {
            clear();
            this.types = ext;
            return add(ext);
        }

        public int add(String[] ext) {
            if (null != ext) {
                int added = 0, len = ext.length;
                String arg;
                for (int cc = 0; cc < len; cc++) {
                    arg = ext[cc];
                    if (null != arg) {
                        if ('.' == arg.charAt(0)) arg = arg.substring(1);
                        super.put(arg, Boolean.TRUE);
                        added += 1;
                    }
                }
                if (0 < added) types = linebuf.toStringArray(keys());
                return added;
            } else return 0;
        }

        public boolean accept(File dir, String fname) {
            if (null != fname) {
                int extidx = fname.lastIndexOf('.');
                if (0 < extidx) {
                    extidx += 1;
                    if (extidx < fname.length()) {
                        String ext = fname.substring(extidx);
                        return (null != super.get(ext));
                    } else return false;
                } else return false;
            } else return false;
        }

        public boolean accept(String ext) {
            return (null != super.get(ext));
        }
    }

    private static final Hashtable _PC_cache = new Hashtable(131);

    /**
     * We could have a lock for each filename, but the memory overhead
     * cost only benefits concurrency in the boundary case that
     * someone is editing files while others are using the website.
     * The benefit is less than the cost.  If someone's editing a file
     * that someone else requests, everyone will have to wait for the
     * half- second that it takes to recompile the page.  The more
     * this would be useful (on busier sites), the less likely it is
     * to occur (that editing live web pages is being done on a
     * production server).  And again, the cost if it does is minimal
     * -- anyway -- because the compilation is fairly fast.
     */
    private static final lck _PC_cacheLock = new lck();

    protected static final ffilter filenameFilter = new ffilter();

    /**
     * Add types to page cache for accept- filtering in "PC_dir".
     * Also calls <tt>`file.AddDirfileTypes'</tt>.
     * 
     * @param ext List of filename extensions (types) to cache
     * 
     * @returns Number of extensions added to directory filter.
     * 
     * @see #PC_dir */
    public static final int PC_types(String[] ext) {
        file.AddDirfileTypes(ext);
        try {
            _PC_cacheLock.serialize(pcsclusr);
            return filenameFilter.add(ext);
        } finally {
            _PC_cacheLock.unlock(pcsclusr);
        }
    }

    /**
     * Define JSP interpretor filename extensions.  Also calls <tt>`file.AddDirfileTypes'</tt>.
     */
    public static final int PC_set_types(String[] ext) {
        file.AddDirfileTypes(ext);
        try {
            _PC_cacheLock.serialize(pcsclusr);
            return filenameFilter.replace(ext);
        } finally {
            _PC_cacheLock.unlock(pcsclusr);
        }
    }

    /**
     * Get the filename extensions being used as JSP types.  Default
     * <tt>"html"</tt> and <tt>"wml"</tt>.  Defined in
     * <tt>`httpd.properties'</tt> by <tt>`pagecache-types'</tt>. 
     */
    public static final String[] PC_types() {
        return filenameFilter.types;
    }

    /**
     * Depth- first compile directory and children (page cache types)
     * into page cache(s).
     *
     * <p> Use "PC_types" to setup filename filters.
     * 
     * @param srv Caller
     *
     * @param dir HTDOCS Source directory
     * 
     * @returns Number of files added to page cache.
     * 
     * @see #PC_types */
    public static final int PC_dir(server srv, file dir) throws IOException {
        file files = dir.find(filenameFilter);
        if (null != files) {
            try {
                _PC_cacheLock.serialize(pcsclusr);
                int added = 0;
                String fname;
                Object el;
                while (files.isNotEmpty()) {
                    fname = files.path();
                    if (null == _PC_cache.get(fname)) {
                        try {
                            page pg = new page(srv, files.copy());
                            _PC_cache.put(fname, pg);
                            if (base.sysdebug) System.out.println("Pagecache (" + dir + ") \t" + pg);
                            added += 1;
                        } catch (DropPage optx) {
                            _PC_cache.remove(fname);
                            if (null != optx.exc && (!base.sysdebug)) System.err.println(optx.exc.getMessage());
                        }
                    }
                    files.popAny();
                }
                return added;
            } finally {
                _PC_cacheLock.unlock(pcsclusr);
            }
        } else return 0;
    }

    /**
     * Generate page output from page cache.
     *
     * @param req Either `request' or `File'
     */
    public static final int PC_send(Context cx, request req, file sendf, OutputStream out) throws IOException {
        String filename = sendf.path();
        if (null == filename) return 0; else {
            page pg = null;
            if (null == (pg = (page) cx.getThreadLocal(filename))) {
                pg = (page) _PC_cache.get(filename);
                if (null == pg) {
                    try {
                        _PC_cacheLock.serialize(pcsclusr);
                        pg = (page) _PC_cache.get(filename);
                        if (null == pg) {
                            try {
                                pg = new page(req.srv, sendf);
                                _PC_cache.put(filename, pg);
                            } catch (DropPage optx) {
                                _PC_cache.remove(filename);
                                if (null != optx.exc) {
                                    if (base.sysdebug) optx.exc.printStackTrace();
                                    throw new RuntimeException(optx.exc.getMessage());
                                } else {
                                    try {
                                        req.out_clear();
                                        return sendf.send(null);
                                    } catch (Exception jsx) {
                                        if (base.sysdebug) jsx.printStackTrace();
                                        throw new IOException(jsx.getMessage());
                                    }
                                }
                            }
                        }
                    } finally {
                        _PC_cacheLock.unlock(pcsclusr);
                    }
                }
                pg = pg.clonePage();
                cx.putThreadLocal(filename, pg);
            }
            try {
                return pg.send(req, out);
            } catch (DropPage optx) {
                _PC_cache.remove(filename);
                cx.removeThreadLocal(filename);
                if (null != optx.exc) {
                    if (base.sysdebug) optx.exc.printStackTrace();
                    throw new RuntimeException(optx.exc.getMessage());
                } else {
                    try {
                        req.out_clear();
                        return sendf.send(null);
                    } catch (Exception jsx) {
                        if (base.sysdebug) jsx.printStackTrace();
                        throw new IOException(jsx.getMessage());
                    }
                }
            } catch (ReComp rec) {
                try {
                    _PC_cacheLock.serialize(pcsclusr);
                    pg = (page) _PC_cache.get(filename);
                    try {
                        if (null == pg) {
                            pg = new page(req.srv, sendf);
                            _PC_cache.put(filename, pg);
                        } else pg.compile();
                    } catch (DropPage optx) {
                        _PC_cache.remove(filename);
                        cx.removeThreadLocal(filename);
                        if (null != optx.exc) {
                            if (base.sysdebug) optx.exc.printStackTrace();
                            throw new RuntimeException(optx.exc.getMessage());
                        } else {
                            try {
                                req.out_clear();
                                return sendf.send(null);
                            } catch (Exception jsx) {
                                if (base.sysdebug) jsx.printStackTrace();
                                throw new IOException(jsx.getMessage());
                            }
                        }
                    }
                } finally {
                    _PC_cacheLock.unlock(pcsclusr);
                }
                try {
                    pg = pg.clonePage();
                    cx.putThreadLocal(filename, pg);
                    return pg.send(req, out);
                } catch (DropPage optx) {
                    _PC_cache.remove(filename);
                    cx.removeThreadLocal(filename);
                    if (null != optx.exc) {
                        if (base.sysdebug) optx.exc.printStackTrace();
                        throw new RuntimeException(optx.exc.getMessage());
                    } else {
                        try {
                            req.out_clear();
                            return sendf.send(null);
                        } catch (Exception jsx) {
                            if (base.sysdebug) jsx.printStackTrace();
                            throw new IOException(jsx.getMessage());
                        }
                    }
                }
            }
        }
    }

    /**
     * Identify page source as interpreted (true) or static (false):
     * whether or not the "PC send" function should be used with this
     * source.  The PC send function performs the final determination
     * by parsing the source page.
     * 
     * @param req Service request
     *
     * @param sendf File source 
     */
    public static final boolean PC_page(request req, file sendf) throws IOException {
        if (filenameFilter.accept(null, sendf.path())) {
            file cachef = cachef(sendf);
            if (cachef.exists()) {
                return (2 < cachef.length() || sendf.lastModified() > cachef.lastModified());
            } else {
                try {
                    _PC_cacheLock.serialize(pcsclusr);
                    if (cachef.exists()) {
                        return (2 < cachef.length() || sendf.lastModified() > cachef.lastModified());
                    } else {
                        String filename = sendf.path();
                        page pg = (page) _PC_cache.get(filename);
                        try {
                            if (null == pg) {
                                pg = new page(req.srv, sendf);
                                _PC_cache.put(filename, pg);
                                return true;
                            } else {
                                pg.compile();
                                return true;
                            }
                        } catch (DropPage optx) {
                            _PC_cache.remove(filename);
                            if (null == optx.exc) return false; else {
                                if (base.sysdebug) optx.exc.printStackTrace();
                                throw new RuntimeException(optx.exc.getMessage());
                            }
                        }
                    }
                } finally {
                    _PC_cacheLock.unlock(pcsclusr);
                }
            }
        } else return false;
    }

    /**
     * Core component of multilingual JSP support.  Lookup table maps
     * a JSP directive language name to a <tt>`pica'</tt> high
     * performance invocation object.
     * 
     * @see #SetPageBin */
    private static final Hashtable pagebins = new Hashtable();

    /**
     * Add a page bin method by language and fully qualified method name.
     * 
     * @param lang JSP directive language name, eg,
     * <tt>"javascript"</tt> or <tt>"java"</tt>.
     * 
     * @param lang Full qualified Pagebin function name, eg,
     * <tt>"org.rhinojetty.brazil.page.PBJS"</tt>.  
     * 
     * @exception IllegalArgumentException For null or empty language
     * arg, or invalid method.  
     * 
     * @see PageBin */
    public static final void SetPageBin(String lang, String name) {
        if (null == lang || "".equals(lang)) throw new IllegalArgumentException("Null or empty PageBin language for `page.SetPageBin'."); else {
            pica pb = Pico.NewPica(name);
            if (null == pb) throw new IllegalArgumentException("Pico Function (" + name + ") not found."); else pagebins.put(lang, pb);
        }
    }

    private static final String pcsclusr = "`page._PC_cacheLock'";

    static {
        try {
            SetPageBin("javascript", "org.rhinojetty.brazil.page.PBJS");
            lck.LckDesc(pcsclusr, pcsclusr);
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    /**
     * Invoke PageBin function for page language.  Until
     * <tt>`pg.language'</tt> is set, we invoke the default javascript
     * language interpretor, <tt>`PBJS'</tt>.
     */
    public static final Object PageBinInvoke(page pg) {
        String lang = pg.jsp_language;
        if (null == lang) return null; else {
            pica pagebin = (pica) pagebins.get(lang);
            if (null == pagebin) return null; else return pagebin.invoke(pg);
        }
    }

    public static final String CACHE_DIRNAME = "pagecache";

    public static final short PAGETYPE_NULL = (short) 0x0;

    public static final short PAGETYPE_LLIN = (short) 0x1000;

    public static final short PAGETYPE_TXT = (short) 0x2000;

    public static final short PAGETYPE_SGML = (short) 0x4000;

    public static final short PAGETYPE_BIN = (short) 0x8000;

    /**
     * Mask for 16 bit "type word" type (high = <tt>0xf000</tt>).  
     */
    public static final short PAGETYPE_MASK_TYPE = (short) 0xf000;

    /**
     * Mask for 16 bit "type word" jsp interpreted ("BIN") type
     * (<tt>0x0ff0</tt>).  
     */
    public static final short PAGETYPE_MASK_BIN_TYPE = (short) 0x0ff0;

    /**
     * Mask for 16 bit "type word" content type and jsp interpreted
     * ("BIN") type (<tt>0xfff0</tt>).  
     */
    public static final short PAGETYPE_MASK_TYPES = (short) 0xfff0;

    /**
     * Mask for 16 bit "type word" byte (low = <tt>0x000f</tt>) for
     * language/version.  First version zero is incremented for a
     * pagecache incompatible with a previous iteration.  Pagecache
     * code shall recompile and overwrite a pagecache version it does
     * not recognize. 
     */
    public static final short PAGETYPE_MASK_BIN_TYPE_LANG = (short) 0x000f;

    /**
     * Mask for 16 bit "type word" LONGLINE element (subline) count
     * (low three bytes = <tt>0x0fff</tt>).  
     */
    public static final short PAGETYPE_MASK_LLIN_NUMLIN = (short) 0x0fff;

    /**
     * BIN SOURCE type <tt>(PAGETYPE_BIN|0x10)</tt> in
     * <tt>`PAGETYPE_MASK_TYPES'</tt> bit space.  
     * 
     * @see #BIN_SRC */
    protected static final short PAGETYPE_BIN_SRC = (short) (PAGETYPE_BIN | 0x10);

    /**
     * BIN TARGET (compiled from source or otherwise not needing
     * "source" processing) type, <tt>(PAGETYPE_BIN|0x20)</tt> in
     * <tt>`PAGETYPE_MASK_TYPES'</tt> bit space.
     *
     * @see #BIN_TGT  */
    protected static final short PAGETYPE_BIN_TGT = (short) (PAGETYPE_BIN | 0x20);

    /**
     * BIN type flag for JSP declaration,
     * <tt>(PAGETYPE_BIN|0x100)</tt>.  The use and interpretation of
     * this flag depends on the page bin function.  */
    protected static final short PAGETYPE_BIN__DEC = (short) (PAGETYPE_BIN | 0x100);

    /**
     * BIN type flag for JSP directive, <tt>(PAGETYPE_BIN|0x200)</tt>.
     * The use and interpretation of this flag depends on the page bin
     * function.  */
    protected static final short PAGETYPE_BIN__DIR = (short) (PAGETYPE_BIN | 0x200);

    /**
     * BIN type flag for JSP reference, <tt>(PAGETYPE_BIN|0x400)</tt>.
     * The use and interpretation of this flag depends on the page bin
     * function.  */
    protected static final short PAGETYPE_BIN__REF = (short) (PAGETYPE_BIN | 0x400);

    /**
     * BIN type mask for JSP modifiers <tt>`DEC', `DIR'</tt> and
     * <tt>`REF': (PAGETYPE_BIN|0x700)</tt>.
     * 
     * <pre>
     * type = pg.page_type();
     * 
     * if ( BIN_TGT == (type & BIN_TGT)){
     * 
     *   type &= PAGETYPE_BIN__MODMASK;
     * 
     *   if ( PAGETYPE_BIN__DEC == type){
     *   }
     *   else if (PAGETYPE_BIN__DIR == type){
     *   }
     *   else if (PAGETYPE_BIN__REF == type){
     *   }
     * }
     * </pre> 
     */
    protected static final short PAGETYPE_BIN__MODMASK = (short) (PAGETYPE_BIN | 0x700);

    /**
     * PageBin constant for page type indicating BIN
     * type for JSP source text (SGML tag source). 
     * 
     * <pre>
     * (BIN_SRC == (type & BIN_SRC))?('source'):('error')
     * </pre>
     */
    public static final short BIN_SRC = PAGETYPE_BIN_SRC;

    /**
     * PageBin constant for its compiled page type indicating BIN
     * type for pagebin target text.
     * 
     * <p> The PageBin function can add info to the type or version
     * bits of the type word, as long as the following fundamental
     * relation remains true.
     *
     * <pre>
     * (BIN_TGT == (type & BIN_TGT))?('target'):('error')
     * </pre> 
     *
     * <p> Note that if the PageBin function doesn't convert the BIN
     * segment type from <tt>`BIN_SRC'</tt> to <tt>`BIN_TGT'</tt>, it
     * will never know when to output page data.  
     */
    public static final short BIN_TGT = PAGETYPE_BIN_TGT;

    /**
     * Produce a description of the argument page cache file element type.
     */
    public static final String typeString(int type) {
        switch(type & PAGETYPE_MASK_TYPE) {
            case PAGETYPE_NULL:
                return "NIL";
            case PAGETYPE_LLIN:
                return "LLIN";
            case PAGETYPE_TXT:
                return "TXT";
            case PAGETYPE_SGML:
                return "SGML";
            case PAGETYPE_BIN:
                {
                    chbuf buf = new chbuf();
                    buf.append("BIN:");
                    if (PAGETYPE_BIN_SRC == (type & PAGETYPE_BIN_SRC)) buf.append("SRC"); else if (PAGETYPE_BIN_TGT == (type & PAGETYPE_BIN_TGT)) buf.append("TGT");
                    if (PAGETYPE_BIN__DEC == (type & PAGETYPE_BIN__DEC)) {
                        buf.append(":DEC");
                        if (PAGETYPE_BIN__DEC != (type & PAGETYPE_BIN__MODMASK)) buf.append(":MMERR");
                    } else if (PAGETYPE_BIN__DIR == (type & PAGETYPE_BIN__DIR)) {
                        buf.append(":DIR");
                        if (PAGETYPE_BIN__DIR != (type & PAGETYPE_BIN__MODMASK)) buf.append(":MMERR");
                    } else if (PAGETYPE_BIN__REF == (type & PAGETYPE_BIN__REF)) {
                        buf.append(":REF");
                        if (PAGETYPE_BIN__REF != (type & PAGETYPE_BIN__MODMASK)) buf.append(":MMERR");
                    }
                    return buf.toString();
                }
            default:
                return null;
        }
    }

    public static final String JSP_LANGUAGE = "javascript";

    private boolean isClone = false;

    private server srv;

    private file source = null, cache = null;

    /**
     * Primary dimension `len' and `typ'.
     */
    private short olen = 0, otyp = 0;

    /**
     * Secondary dimension (elements of LONGLINE) `len' and `typ'.
     */
    private short s_olen = 0, s_otyp = 0;

    /**
     * Causes page to be written in optimized format globbing tags and data.
     */
    protected boolean optimize = true;

    /**
     * Causes page without any BIN section to be written as empty cache file.
     */
    protected boolean super_optimize = true;

    /**
     * Internal variables in support of "page" API for PageBin functions.
     */
    protected short pagebin_typ = 0;

    protected byte[] pagebin_src = null;

    protected int pagebin_lno = 0, pagebin_col = 0;

    /**
     * Note that `req' is only non- null during page send (interpretation), not page compile.
     */
    protected request req = null;

    /**
     * Pagecache directory for PageBin functions.
     */
    public file jsp_cached = null;

    /**
     * Segment interpretation counter used by PageBin functions (see
     * the source for `PBJS' in this class).  */
    public int jsp_segment = 0, jsp_segment_count = 0;

    /**
     * Buffers for DECL type JSP segments' output.
     */
    public byte[][] jsp_segment_buffers = null;

    /**
     * Default JSP language is "javascript".  This is set by JSP
     * directive "language" is used for the selection of a PageBin
     * function.
     * 
     * @see #SetPageBin
     * @see PageBin
     */
    protected String jsp_language = JSP_LANGUAGE;

    /**
     * Counter for each time the page is invoked (sent).  A
     * declaration is only evaluated on the first cycle (when <tt>`0
     * == jsp_invocation'</tt>).  This is initialized to zero, and
     * incremented in the bottom of "send".  */
    protected int jsp_invocation = 0;

    /**
     * Read page from file (supports UTF-8 content).
     * 
     * @param source SGML source file
     * 
     * @exception  DropPage If a page is inactive (no JSP).
     * 
     * @exception  ReComp If a clone needs a recompilation.
     * 
     * @exception  IOException In compiling source to pagecache file.
     */
    public page(server srv, file source) throws DropPage, ReComp, IOException {
        super();
        if (source.exists() && source.canRead()) {
            lck.LckDesc(this, chbuf.cat("PAGE `", source.path(), "'"));
            this.srv = srv;
            this.source = source;
            this.cache = cachef(source);
            compile();
        } else throw new IOException(chbuf.cat("File not found (", source.path(), ")."));
    }

    protected void finalize() throws Throwable {
        lck.LckDescRm(this);
    }

    public String toString() {
        return cache.toString();
    }

    public int hashCode() {
        return cache.toString().hashCode();
    }

    public boolean equals(Object ano) {
        if (ano == this) return true; else if (ano instanceof page) return cache.toString().equals(ano.toString()); else return false;
    }

    /**
     * A thread- request page for sending.
     */
    public page clonePage() {
        try {
            page pg = (page) super.clone();
            pg.isClone = true;
            return pg;
        } catch (CloneNotSupportedException cnx) {
            return null;
        }
    }

    /**
     * Line number.
     */
    public final int page_line() {
        return pagebin_lno;
    }

    /**
     * Column SGML element number.
     */
    public final int page_column() {
        return pagebin_col;
    }

    /**
     * PageBin user interface.
     *
     * @returns Type of current segment
     */
    public final int page_type() {
        return pagebin_typ;
    }

    /**
     * PageBin user interface.
     *
     * @returns Current segment
     */
    public final byte[] page_data() {
        return pagebin_src;
    }

    /**
     * PageBin user interface.
     *
     * @returns Current request
     */
    public final request page_req() {
        return req;
    }

    /**
     * Page `store' calls `encode' to convert strings to bytes.  If a
     * multi- column or multi- line segment needs to be encoded
     * together, `encode' can throw an EncP object which is passed
     * back into it for the next segment, and segment output is
     * skipped.  If `encode' returns bytes normally, the `EncP' object
     * is discarded.  
     *
     * <p> This protocol is used for combining multiple BIN segments
     * into one for compilation.  
     */
    public static final class EncP extends IllegalStateException {

        public short type = zero;

        public linebuf codebuf = new linebuf();

        public EncP(short origin_type, String lelle) {
            super();
            this.type = origin_type;
            this.codebuf.append(lelle);
        }
    }

    /**
     * Exception thrown by "page.update" when the page 
     * should not be saved into the in- memory page cache.
     */
    public static final class DropPage extends IllegalStateException {

        public Exception exc = null;

        public DropPage() {
        }

        public DropPage(Exception exc) {
            super();
            this.exc = exc;
        }
    }

    /**
     * When a clone encounters a page that needs to be recompiled, it
     * needs to throw a `ReComp' exception to force the principal to
     * recompile.
     */
    public static final class ReComp extends IllegalStateException {

        public ReComp() {
        }
    }

    /**
     * Page compilation function called by <tt>`PC_dir'</tt>.
     */
    private final void compile() throws DropPage, ReComp, IOException {
        if ((!cache.exists()) || source.lastModified() > cache.lastModified()) {
            if (isClone) throw new ReComp(); else {
                String[][] src = sgmlp.parseValidateServer(source.getInputStream());
                if (null == src) {
                    if (base.sysdebug) System.err.println("Page inactive, not SGML (" + source + ").");
                    fexist(cache);
                    throw new DropPage();
                } else {
                    OutputStream out = cache.getOutputStream();
                    if (null == out) {
                        fexist(cache);
                        throw new DropPage();
                    }
                    this.jsp_segment = 0;
                    this.jsp_segment_count = 0;
                    try {
                        DataOutputStream dout = new DataOutputStream(out);
                        boolean nopt = (!(optimize || super_optimize));
                        int count = src.length;
                        EncP encp = null;
                        bbuf llbbu = new bbuf();
                        String line[], subline;
                        byte[] bb;
                        int strlen, llen, count_bin = 0;
                        for (short pic = zero, s_pic; pic < count; pic++) {
                            try {
                                line = src[pic];
                                if (null == line) {
                                    dout.writeShort(zero);
                                    dout.writeShort(zero);
                                } else {
                                    llen = line.length;
                                    if (1 == llen) {
                                        subline = line[0];
                                        otyp = typeof(subline);
                                        bb = _pg_encode(encp, otyp, subline, true);
                                        if (null != bb) {
                                            strlen = bb.length;
                                            if (PAGETYPE_BIN == (otyp & PAGETYPE_MASK_TYPE)) {
                                                if (null == encp) pagebin_typ = otyp; else {
                                                    pagebin_typ = encp.type;
                                                    encp = null;
                                                }
                                                pagebin_src = bb;
                                                pagebin_lno = pic + 1;
                                                pagebin_col = 0;
                                                try {
                                                    bb = (byte[]) PageBinInvoke(this);
                                                    if (null != bb) {
                                                        count_bin += 1;
                                                        dout.write(bb, 0, bb.length);
                                                    } else {
                                                        dout.writeShort(zero);
                                                        dout.writeShort(zero);
                                                    }
                                                } finally {
                                                    pagebin_typ = 0;
                                                    pagebin_src = null;
                                                }
                                            } else {
                                                encp = null;
                                                dout.write(bb, 0, strlen);
                                            }
                                        } else {
                                            encp = null;
                                            dout.writeShort(zero);
                                            dout.writeShort(zero);
                                        }
                                    } else if (llen < 0x1000) {
                                        s_olen = (short) llen;
                                        llen = 0;
                                        for (s_pic = zero; s_pic < s_olen; s_pic++) {
                                            subline = line[s_pic];
                                            try {
                                                s_otyp = typeof(subline);
                                                bb = _pg_encode(encp, s_otyp, subline, (zero == s_pic));
                                                if (null != bb) {
                                                    if (PAGETYPE_BIN == (s_otyp & PAGETYPE_MASK_TYPE)) {
                                                        if (null == encp) pagebin_typ = s_otyp; else {
                                                            pagebin_typ = encp.type;
                                                            encp = null;
                                                        }
                                                        pagebin_src = bb;
                                                        pagebin_lno = pic + 1;
                                                        pagebin_col = s_pic + 1;
                                                        try {
                                                            bb = (byte[]) PageBinInvoke(this);
                                                            if (null != bb) {
                                                                count_bin += 1;
                                                                llbbu.write(bb, 0, bb.length);
                                                                llen += 1;
                                                            }
                                                        } finally {
                                                            pagebin_typ = 0;
                                                            pagebin_src = null;
                                                        }
                                                    } else {
                                                        encp = null;
                                                        llbbu.write(bb, 0, bb.length);
                                                        llen += 1;
                                                    }
                                                } else {
                                                    encp = null;
                                                    llbbu.write2(zero);
                                                    llbbu.write2(zero);
                                                    llen += 1;
                                                }
                                            } catch (EncP encx) {
                                                encp = encx;
                                            }
                                        }
                                        bb = llbbu.toByteArray();
                                        if (null != bb) {
                                            strlen = bb.length;
                                            dout.writeShort(strlen);
                                            dout.writeShort(PAGETYPE_LLIN | llen);
                                            dout.write(bb);
                                            llbbu.reset();
                                        } else {
                                            dout.writeShort(zero);
                                            dout.writeShort(zero);
                                        }
                                    } else throw new IllegalStateException("Long line (" + llen + " exceeds MAX " + 0xfff + " sublines) doesn't fit into page cache format.");
                                }
                            } catch (EncP ency) {
                                encp = ency;
                            }
                        }
                        if (super_optimize && 1 > count_bin) throw new DropPage();
                    } catch (DropPage optx) {
                        if (base.sysdebug) System.err.println("Page inactive, not JSP (" + source + ").");
                        try {
                            out.close();
                        } catch (IOException iox) {
                        }
                        try {
                            cache.delete();
                        } catch (Throwable t) {
                        }
                        fexist(cache);
                        throw optx;
                    } catch (Exception exc) {
                        if (base.sysdebug) exc.printStackTrace();
                        if (null != srv) srv.log(this, exc);
                        try {
                            out.close();
                        } catch (IOException iox) {
                        }
                        try {
                            cache.delete();
                        } catch (Throwable t) {
                        }
                        throw new DropPage(exc);
                    } finally {
                        try {
                            out.close();
                        } catch (IOException iox) {
                        }
                    }
                }
            }
        } else if (0 == jsp_segment_count) {
            if (null == jsp_cached) jsp_cached = file.NewFile(cache.toString() + "-jsp");
            String[] segments = jsp_cached.list(null);
            if (null != segments) {
                jsp_segment_count = segments.length;
            }
        }
    }

    /**
     * Only called on valid cache files per "PC_page" user protocol.
     */
    private final int send(request req, OutputStream out) throws DropPage, ReComp, IOException {
        compile();
        if (null == req || null == out) return 0;
        this.jsp_segment = 0;
        this.req = req;
        DataInputStream din = cache.getInputStream();
        if (null == din) return 0;
        int bytes = 0;
        pagebin_lno = 0;
        boolean printed = false;
        try {
            int iobuflen = 512, llen, lc, intmp, inrd, indx;
            byte iobuf[] = new byte[iobuflen], binbuf[];
            while (true) {
                pagebin_lno += 1;
                olen = din.readShort();
                otyp = din.readShort();
                if (olen > iobuflen) {
                    iobuflen = olen;
                    iobuf = new byte[iobuflen];
                }
                switch(otyp & PAGETYPE_MASK_TYPE) {
                    case PAGETYPE_TXT:
                    case PAGETYPE_SGML:
                        inrd = olen;
                        indx = 0;
                        while (0 < inrd && inrd <= olen) {
                            intmp = din.read(iobuf, indx, inrd);
                            if (0 < intmp) {
                                inrd -= intmp;
                                indx += intmp;
                            } else throw new IllegalStateException("Pagecache file incomplete.");
                        }
                        olen = _send_dynamic_retranslate(iobuf, 0, olen);
                        out.write(iobuf, 0, olen);
                        bytes += olen;
                        printed = true;
                        break;
                    case PAGETYPE_BIN:
                        pagebin_col = 1;
                        try {
                            inrd = olen;
                            indx = 0;
                            while (0 < inrd && inrd <= olen) {
                                intmp = din.read(iobuf, indx, inrd);
                                if (0 < intmp) {
                                    inrd -= intmp;
                                    indx += intmp;
                                } else throw new IllegalStateException("Pagecache file incomplete.");
                            }
                            pagebin_typ = otyp;
                            pagebin_src = new byte[olen];
                            System.arraycopy(iobuf, 0, pagebin_src, 0, olen);
                            binbuf = (byte[]) PageBinInvoke(this);
                            if (req.committed) {
                                return 0;
                            } else if (null != binbuf) {
                                int blen = binbuf.length;
                                out.write(binbuf, 0, blen);
                                bytes += blen;
                            }
                        } finally {
                            pagebin_typ = 0;
                            pagebin_src = null;
                        }
                        break;
                    case PAGETYPE_LLIN:
                        pagebin_col = 0;
                        llen = (short) (otyp & PAGETYPE_MASK_LLIN_NUMLIN);
                        for (lc = 0; lc < llen; lc++) {
                            pagebin_col += 1;
                            s_olen = din.readShort();
                            s_otyp = din.readShort();
                            if (s_olen > iobuflen) {
                                iobuflen = s_olen;
                                iobuf = new byte[iobuflen];
                            }
                            switch(s_otyp & PAGETYPE_MASK_TYPE) {
                                case PAGETYPE_TXT:
                                case PAGETYPE_SGML:
                                    inrd = s_olen;
                                    indx = 0;
                                    while (0 < inrd && inrd <= s_olen) {
                                        intmp = din.read(iobuf, indx, inrd);
                                        if (0 < intmp) {
                                            inrd -= intmp;
                                            indx += intmp;
                                        } else throw new IllegalStateException("Pagecache file incomplete.");
                                    }
                                    s_olen = _send_dynamic_retranslate(iobuf, 0, s_olen);
                                    out.write(iobuf, 0, s_olen);
                                    bytes += s_olen;
                                    printed = true;
                                    break;
                                case PAGETYPE_BIN:
                                    try {
                                        inrd = s_olen;
                                        indx = 0;
                                        while (0 < inrd && inrd <= s_olen) {
                                            intmp = din.read(iobuf, indx, inrd);
                                            if (0 < intmp) {
                                                inrd -= intmp;
                                                indx += intmp;
                                            } else throw new IllegalStateException("Pagecache file incomplete.");
                                        }
                                        pagebin_typ = s_otyp;
                                        pagebin_src = new byte[s_olen];
                                        System.arraycopy(iobuf, 0, pagebin_src, 0, s_olen);
                                        binbuf = (byte[]) PageBinInvoke(this);
                                        if (req.committed) {
                                            return 0;
                                        } else if (null != binbuf) {
                                            int blen = binbuf.length;
                                            out.write(binbuf, 0, blen);
                                            bytes += blen;
                                        }
                                    } finally {
                                        pagebin_typ = 0;
                                        pagebin_src = null;
                                    }
                                    break;
                                case 0:
                                    printed = true;
                                    break;
                                default:
                                    throw new IllegalStateException("Format error LLIN TYPE `0x" + Integer.toHexString(s_otyp & 0xffff) + "'.");
                            }
                        }
                        break;
                    case 0:
                        printed = true;
                        break;
                    default:
                        throw new IllegalStateException("Format error TYPE `0x" + Integer.toHexString(otyp & 0xffff) + "'.");
                }
                if (printed) {
                    out.write('\n');
                    bytes += 1;
                    printed = false;
                }
            }
        } catch (EOFException end) {
            return bytes;
        } finally {
            jsp_invocation += 1;
            try {
                din.close();
            } catch (Throwable t) {
            }
            this.req = null;
        }
    }

    /**
     * Called within 'send' to translate <tt>"&lt;\%"</tt> and
     * <tt>"%\&gt;"</tt> into JSP tags <tt>"&lt;%"</tt> and
     * <tt>"%&gt;"</tt>.
     * 
     * <p> Permit any depth of enclosure, translating
     * <tt>"&lt;\\%"</tt> and <tt>"%\\&gt;"</tt> into JSP tags
     * <tt>"&lt;\%"</tt> and <tt>"%\&gt;"</tt>, <i>ad infinitum</i>.
     * 
     * @param iobuf I/O buffer
     * @param bofs Buffer offset
     * @param blen Buffer length
     * 
     * @param Translated buffer length
     */
    private static final short _send_dynamic_retranslate(byte[] iobuf, int bofs, short blen) {
        short ret = blen;
        int state = 0;
        for (int cc = bofs, clen = bofs + blen; cc < clen; cc++) {
            switch(iobuf[cc]) {
                case '<':
                    if (0 == state) {
                        state = 1;
                        break;
                    } else {
                        state = 0;
                        break;
                    }
                case '>':
                    if (2 == state) {
                        ret -= 1;
                        System.arraycopy(iobuf, (cc), iobuf, (cc - 1), (clen - cc));
                        clen -= 1;
                        state = 0;
                        break;
                    } else {
                        state = 0;
                        break;
                    }
                case '\\':
                    if (1 == state) {
                        state = 2;
                        break;
                    } else if (2 == state) {
                        break;
                    } else {
                        state = 0;
                        break;
                    }
                case '%':
                    if (2 == state) {
                        ret -= 1;
                        System.arraycopy(iobuf, (cc), iobuf, (cc - 1), (clen - cc));
                        clen -= 1;
                        state = 0;
                        break;
                    } else if (0 == state) {
                        state = 1;
                        break;
                    } else {
                        state = 0;
                        break;
                    }
                default:
                    state = 0;
                    break;
            }
        }
        return ret;
    }

    /**
     * Called from <tt>`store'</tt> to convert string to bytes, typically UTF-8.
     */
    private final byte[] _pg_encode(EncP encp, short type, String lelle, boolean newline) throws EncP {
        byte[] leb = null;
        switch(type & PAGETYPE_MASK_TYPE) {
            case PAGETYPE_BIN:
                if ('<' == lelle.charAt(0) && '>' == lelle.charAt(lelle.length() - 1)) leb = utf8.encode(lelle); else if (null == encp) throw new EncP(type, lelle); else {
                    if (newline) encp.codebuf.append(lelle); else encp.codebuf.line_append(lelle);
                    leb = utf8.encode(encp.codebuf.toCharArray());
                }
                break;
            case PAGETYPE_SGML:
                if (null != encp) {
                    if (newline) encp.codebuf.append(lelle); else encp.codebuf.line_append(lelle);
                    throw encp;
                } else leb = utf8.encode(lelle);
                break;
            case PAGETYPE_TXT:
                if (null != encp) {
                    if (newline) encp.codebuf.append(lelle); else encp.codebuf.line_append(lelle);
                    throw encp;
                } else leb = utf8.encode(lelle);
                break;
            default:
                throw new IllegalArgumentException("BBBUGGG Page encoding with unknown type `0x" + Integer.toHexString(type & 0xffff) + "'.");
        }
        if (null != leb) {
            bbuf bb = new bbuf();
            bb.write2((short) leb.length);
            if (null != encp) bb.write2(encp.type); else bb.write2(type);
            bb.write(leb);
            return bb.toByteArray();
        } else {
            byte[] nil = { 0, 0, 0, 0 };
            return nil;
        }
    }

    /**
     * Discover TXT, SGML or BIN_SRC type of string.
     */
    protected static final short typeof(String subline) {
        if (null == subline) return PAGETYPE_TXT; else {
            int len = subline.length();
            if (1 > len) return PAGETYPE_TXT;
            char ch = subline.charAt(0);
            if ('<' == ch) {
                if (1 < len) {
                    ch = subline.charAt(1);
                    if ('%' == ch) {
                        if (2 < len) {
                            ch = subline.charAt(2);
                            if ('@' == ch) return (BIN_SRC | PAGETYPE_BIN__DIR); else if ('!' == ch) return (BIN_SRC | PAGETYPE_BIN__DEC); else if ('=' == ch) return (BIN_SRC | PAGETYPE_BIN__REF); else return BIN_SRC;
                        } else return BIN_SRC;
                    } else return PAGETYPE_SGML;
                } else return PAGETYPE_SGML;
            } else if (1 < len) {
                int idx = len - 1;
                ch = subline.charAt(idx);
                if ('>' == ch) {
                    idx -= 1;
                    if (-1 < idx) {
                        ch = subline.charAt(idx);
                        if ('%' == ch) return BIN_SRC;
                    }
                    return PAGETYPE_SGML;
                } else return PAGETYPE_TXT;
            } else return PAGETYPE_TXT;
        }
    }

    /**
     * Called from PBJS to compile a <tt>`BIN_SRC'</tt> segment as JavaScript.
     * 
     * @see #PBJS
     */
    private final Script scriptCompile(Context cx, Scriptable scope, int srclno, file binf) throws JavaScriptException {
        if (null == cx) cx = Context.enter();
        if (null == scope) scope = shell.getScope();
        Object secdom = null;
        return shell.scripter(cx, binf, scope, 1, secdom);
    }

    /**
     * Called from PBJS to interpret a <tt>`BIN_TGT'</tt> segment as JavaScript.
     * 
     * @see #PBJS
     */
    private final Object scriptEvaluate(Context cx, Scriptable scope, int srclno, file binf) throws JavaScriptException {
        if (null == cx) cx = Context.getCurrentContext();
        if (null == scope) scope = base.getScope(cx);
        Script scr = scriptCompile(cx, scope, srclno, binf);
        if (null != scr) return scr.exec(cx, scope); else return null;
    }

    /**
     * Code for JSP directive "language"
     */
    private static final int DIR_LANG = 1;

    /**
     * Code for JSP directive "include"
     */
    private static final int DIR_INCL = 2;

    /**
     * Lookup table for JSP directive codes
     * 
     * @see #DIR_LANG
     * @see #DIR_INCL
     */
    private static final Hashtable directives = new Hashtable();

    static {
        directives.put("language", new Integer(DIR_LANG));
        directives.put("include", new Integer(DIR_INCL));
    }

    /**
     * Called from PBJS to interpret a <tt>`BIN__DIR'</tt> JSP
     * <tt>"@"</tt> directives "language" or "include".
     * 
     * <p> <tt><b>language</b></tt>
     * 
     * <p> This directive sets the <tt>`jsp_language'</tt> field of
     * this page class object with the value of this directive.  This
     * causes any subsequent segment to be interpreted by the
     * registered PageBin function for that language, if any is known.
     * 
     * <p> The default is <tt>"javascript"</tt>.
     * 
     * <p> <tt><b>include</b></tt>
     * 
     * <p>For file with mimetype <tt>"application/x-rhinojetty"</tt>
     * (<tt>*.rj</tt>): this function creates a segment with a
     * "require" statement as follows:
     * 
     * <pre>
     *     if (req)
     *        require(req.file("name"));
     *     else
     *        require(new file("name"));
     * </pre>
     * 
     * <p>For file with mimetype <tt>"text/html"</tt>
     * (<tt>*.html</tt>): this function creates a segment with a
     * "page" statement as follows:
     * 
     * <pre>
     *     req.page(req.file("name"));
     * </pre>
     * 
     * <p>For file with mimetypes matching <tt>"text/*"</tt> this
     * function creates a segment with a "send" statement as
     * follows:
     * 
     * <pre>
     *     if (req)
     *        req.file("name").send();
     *     else
     *        new file("name").send();
     * </pre>
     * 
     * <p> Any other file mimetype results in an error.
     * 
     * <p><b>Other directives</b>
     * 
     * <p> Unknown directives are silently discarded.
     * 
     * @see #PBJS 
     * @see #jsp_language
     * @see #SetPageBin */
    private final void scriptDirective(String direct, file binf, int lno) throws JavaScriptException {
        if (null == direct || 1 > direct.length()) return; else {
            String strp[] = linebuf.toStringArray(direct, "= ");
            String str, ref, type;
            int many = (null == strp) ? (0) : (strp.length);
            Integer tt;
            for (int cc = 0; cc < many; cc++) {
                str = strp[cc];
                tt = (Integer) directives.get(str);
                if (null != tt) {
                    switch(tt.intValue()) {
                        case DIR_LANG:
                            cc += 1;
                            if (cc < many) {
                                str = unquote(strp[cc]).toLowerCase();
                                jsp_language = str;
                            }
                            break;
                        case DIR_INCL:
                            cc += 1;
                            if (cc < many) {
                                ref = unquote(strp[cc]);
                                type = mimes.instance.getTypeFile(ref);
                                if (null != type) {
                                    if (request.RJ_TYPE.equals(type)) {
                                        bbuf bbu = new bbuf();
                                        bbu.println("if (req)");
                                        bbu.print("  require( req.file(\"");
                                        bbu.print(ref);
                                        bbu.println("\")); // include file");
                                        bbu.println("else");
                                        bbu.print("  require( new file(\"");
                                        bbu.print(ref);
                                        bbu.println("\")); // include file");
                                        byte[] bb = bbu.toByteArray();
                                        fwrite(binf, bb, 0, bb.length);
                                        this.scriptCompile(null, null, lno, binf);
                                    } else if (type.startsWith("text/html")) {
                                        bbuf bbu = new bbuf();
                                        bbu.print("req.page( req.file( \"");
                                        bbu.print(ref);
                                        bbu.println("\"));");
                                        byte[] bb = bbu.toByteArray();
                                        fwrite(binf, bb, 0, bb.length);
                                        this.scriptCompile(null, null, lno, binf);
                                    } else if (type.startsWith("text/")) {
                                        bbuf bbu = new bbuf();
                                        bbu.println("if (req)");
                                        bbu.print("  req.file( \"");
                                        bbu.print(ref);
                                        bbu.println("\").send();");
                                        bbu.println("else");
                                        bbu.print("  new file( \"");
                                        bbu.print(ref);
                                        bbu.println("\").send();");
                                        byte[] bb = bbu.toByteArray();
                                        fwrite(binf, bb, 0, bb.length);
                                        this.scriptCompile(null, null, lno, binf);
                                    } else throw new RuntimeException("Mimetype `" + type + "' (file `" + ref + "') is not available to the JSP `include' directive.");
                                } else throw new RuntimeException("File `" + ref + "' without mimeyped filename extension is not available to the JSP `include' directive.");
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }

    public static final String unquote(String str) {
        int strl = str.length() - 1;
        char qt0 = str.charAt(0);
        char qt1 = str.charAt(strl);
        switch(qt0) {
            case '\'':
                if ('\'' == qt1) return str.substring(1, strl).trim(); else return str;
            case '"':
                if ('"' == qt1) return str.substring(1, strl).trim(); else return str;
            case '`':
                if ('`' == qt1) return str.substring(1, strl).trim(); else return str;
            default:
                return str;
        }
    }

    /**
     * Default `PageBin' function for JavaScript.  This function is
     * dynamically configured via <tt>`SetPageBin'</tt> to 
     * 
     * @see PageBin
     * @see #SetPageBin */
    public static final Object PBJS(Object arg) {
        try {
            page pg = (page) arg;
            int lno = pg.page_line();
            file cached, binf;
            if (null == pg.jsp_cached) {
                cached = cachedir(pg);
                pg.jsp_cached = cached;
            } else cached = pg.jsp_cached;
            int type = pg.page_type();
            byte[] data = pg.page_data();
            if (BIN_SRC == (type & PAGETYPE_BIN_SRC)) {
                bbuf bbu = new bbuf();
                if (null == data) {
                    bbu.write2(zero);
                    bbu.write2(BIN_TGT);
                } else {
                    short len = (short) (data.length - 8);
                    bbu.write2(len);
                    short wrtyp = (short) (BIN_TGT | (type & PAGETYPE_BIN__MODMASK));
                    bbu.write2(wrtyp);
                    bbu.write(data, 6, len);
                    binf = file.NewFile(cached, chbuf.cat("rj_sect_", Integer.toString(pg.jsp_segment++)));
                    pg.jsp_segment_count += 1;
                    type &= PAGETYPE_BIN__MODMASK;
                    if (PAGETYPE_BIN__DIR == type) {
                        String srcstr = new String(data, 0, 7, len - 1);
                        pg.scriptDirective(srcstr, binf, lno);
                    } else {
                        if (PAGETYPE_BIN__REF == type || PAGETYPE_BIN__DEC == type) fwriteln(binf, data, 7, len - 1); else fwriteln(binf, data, 6, len);
                        pg.scriptCompile(null, null, lno, binf);
                    }
                }
                return bbu.toByteArray();
            } else if (BIN_TGT == (type & PAGETYPE_BIN_TGT)) {
                int segment = pg.jsp_segment++;
                binf = file.NewFile(cached, chbuf.cat("rj_sect_", Integer.toString(segment)));
                if (binf.exists()) {
                    Context cx = Context.enter();
                    Scriptable scope = pg.page_req().getSessionScope();
                    if (PAGETYPE_BIN__DEC == (type & PAGETYPE_BIN__DEC)) {
                        if (0 == pg.jsp_invocation) {
                            request req = pg.page_req();
                            bbuf obuf = req.out_bbu();
                            obuf.mark();
                            pg.scriptEvaluate(cx, scope, lno, binf);
                            byte[] segbuf = obuf.markedBits();
                            if (null != segbuf) {
                                byte[][] segment_buffers = pg.jsp_segment_buffers;
                                if (null == segment_buffers) {
                                    pg.jsp_segment_buffers = new byte[pg.jsp_segment_count][];
                                    segment_buffers = pg.jsp_segment_buffers;
                                } else if (segment >= segment_buffers.length) {
                                    throw new IllegalStateException("BBBUGGGG segment count problem.");
                                }
                                segment_buffers[segment] = segbuf;
                            }
                            return null;
                        } else {
                            byte[][] segment_buffers = pg.jsp_segment_buffers;
                            if (null != segment_buffers) return segment_buffers[segment]; else return null;
                        }
                    } else {
                        Object ret = pg.scriptEvaluate(cx, scope, lno, binf);
                        if (null == ret || Undefined.instance == ret || ScriptableObject.NOT_FOUND == ret) {
                            return null;
                        } else if (PAGETYPE_BIN__REF == (type & PAGETYPE_BIN__REF)) {
                            return utf8.encode(ScriptRuntime.toString(ret));
                        } else return null;
                    }
                } else return null;
            } else throw new IllegalStateException("PageBin Unknown Type=`" + typeString(type) + "'.");
        } catch (JavaScriptException jsx) {
            if (base.sysdebug) jsx.printStackTrace();
            throw new IllegalStateException(jsx.getMessage());
        }
    }

    /**
     * Used by "main" for visualizing page cache binary files.
     */
    private static final void print(InputStream in, PrintStream out) throws IOException {
        try {
            DataInputStream din = new DataInputStream(in);
            int iobuflen = 512, llen, lc, intmp, inrd, indx;
            byte iobuf[] = new byte[iobuflen], binbuf[];
            short olen, otyp, s_olen, s_otyp;
            while (true) {
                olen = din.readShort();
                otyp = din.readShort();
                if (olen > iobuflen) {
                    iobuflen = olen;
                    iobuf = new byte[iobuflen];
                }
                switch(otyp & PAGETYPE_MASK_TYPE) {
                    case PAGETYPE_TXT:
                    case PAGETYPE_SGML:
                    case PAGETYPE_BIN:
                        inrd = olen;
                        indx = 0;
                        while (0 < inrd && inrd <= olen) {
                            intmp = din.read(iobuf, indx, inrd);
                            if (0 < intmp) {
                                inrd -= intmp;
                                indx += intmp;
                            } else throw new IllegalStateException("Pagecache file incomplete.");
                        }
                        out.print(chbuf.cat("{{", typeString(otyp), "}}"));
                        out.write(iobuf, 0, olen);
                        out.println();
                        break;
                    case PAGETYPE_LLIN:
                        llen = (short) (otyp & PAGETYPE_MASK_LLIN_NUMLIN);
                        for (lc = 0; lc < llen; lc++) {
                            s_olen = din.readShort();
                            s_otyp = din.readShort();
                            if (s_olen > iobuflen) {
                                iobuflen = s_olen;
                                iobuf = new byte[iobuflen];
                            }
                            inrd = s_olen;
                            indx = 0;
                            while (0 < inrd && inrd <= s_olen) {
                                intmp = din.read(iobuf, indx, inrd);
                                if (0 < intmp) {
                                    inrd -= intmp;
                                    indx += intmp;
                                } else throw new IllegalStateException("Pagecache file incomplete.");
                            }
                            out.print(chbuf.cat("{{", typeString(s_otyp), "}}"));
                            out.write(iobuf, 0, s_olen);
                        }
                        out.println();
                        break;
                    case 0:
                        out.println();
                        break;
                    default:
                        throw new IllegalStateException("Format error TYPE `0x" + Integer.toHexString(otyp & 0xffff) + "'.");
                }
            }
        } catch (EOFException end) {
            return;
        } catch (Throwable t) {
            if (base.sysdebug) t.printStackTrace();
            return;
        } finally {
            try {
                in.close();
            } catch (Throwable t) {
            }
        }
    }

    private static final void usage(PrintStream out) {
        out.println();
        out.println(" Usage: page [-d 'filename.rjpg'] [-c dir]");
        out.println();
        out.println("\t-d    Describe binary.");
        out.println();
        out.println("\t-c    Compile directory.");
        out.println();
    }

    public static void main(String[] argv) {
        if (null == argv || 2 != argv.length) {
            usage(System.err);
            System.exit(1);
        } else {
            base.sysdebug = true;
            String opt = argv[0];
            String arg = argv[1];
            if ("-d".equalsIgnoreCase(opt)) {
                try {
                    PrintStream out = System.out;
                    InputStream in = new FileInputStream(arg);
                    print(in, out);
                    System.exit(0);
                } catch (Exception exc) {
                    exc.printStackTrace();
                    System.exit(1);
                }
            } else if ("-c".equalsIgnoreCase(opt)) {
                try {
                    shell sh = shell.getGlobal();
                    mimes.init(sh.getClass());
                    file dir = file.NewFile(arg);
                    if (dir.isDirectory()) {
                        int num = PC_dir(null, dir);
                        System.out.println("Compiled (" + num + ") files in `" + dir + "'.");
                    } else if (dir.isFile()) {
                        page pg = new page(null, dir);
                        System.out.println("Compiled `" + dir + "'.");
                    }
                    System.exit(0);
                } catch (Exception exc) {
                    exc.printStackTrace();
                    System.exit(1);
                }
            } else {
                usage(System.err);
                System.exit(1);
            }
        }
    }
}
