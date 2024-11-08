package com.luzan.app.map.tool;

import com.sun.xfile.XFile;
import com.sun.xfile.XFileOutputStream;
import com.sun.xfile.XFileInputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.beans.Introspector;
import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;

public class GlobalMapperTileTranslator {

    private static final Logger logger = Logger.getLogger(GlobalMapperTileTranslator.class);

    protected XFile srcDir;

    protected XFile dstParentDir = new XFile("./");

    protected String dstGuid;

    protected boolean override = false;

    protected boolean overrideIfDiffLength = false;

    protected boolean overrideIfDiffTime = false;

    public void setSrcDir(String srcDir) {
        this.srcDir = new XFile(srcDir);
    }

    public void setDstParentDir(String dstParentDir) {
        this.dstParentDir = new XFile(dstParentDir);
    }

    public void setDstGuid(String dstGuid) {
        this.dstGuid = dstGuid;
    }

    public void setOverride(String override) {
        this.override = Boolean.parseBoolean(override);
    }

    public void setOverrideIfDiffLength(String override) {
        this.overrideIfDiffLength = Boolean.parseBoolean(override);
    }

    public void setOverrideIfDiffTime(String override) {
        this.overrideIfDiffTime = Boolean.parseBoolean(override);
    }

    public void doIt() throws GlobalMapperTileTranslatorException {
        if (StringUtils.isEmpty(dstGuid)) throw new GlobalMapperTileTranslatorException("GUID of destination map is empty");
        if (srcDir == null || !srcDir.isDirectory() || !srcDir.exists()) throw new GlobalMapperTileTranslatorException("Source directory is invalid");
        try {
            int z;
            final XFile dstDir = new XFile(dstParentDir, dstGuid);
            dstDir.mkdir();
            int n = 1;
            if (srcDir.isDirectory() && srcDir.exists()) {
                for (int i = 0; i < 18; i++) {
                    XFile zDir = new XFile(srcDir, "z" + i);
                    if (!zDir.isDirectory() || !zDir.exists()) zDir = new XFile(srcDir, "Z" + i);
                    if (zDir.isDirectory() && zDir.exists()) {
                        for (String fileName : zDir.list()) {
                            XFile file = new XFile(zDir, fileName);
                            if (file.isFile() && file.exists() && file.canRead()) {
                                final String[] yx;
                                if (fileName.indexOf('.') > 0) {
                                    String[] fileExt = fileName.split("\\.");
                                    yx = fileExt[0].split("_");
                                } else yx = fileName.split("_");
                                if (yx.length > 1) {
                                    final int x = Integer.valueOf(yx[1]);
                                    final int y = Integer.valueOf(yx[0]);
                                    z = 17 - i;
                                    XFileOutputStream out = null;
                                    XFileInputStream in = null;
                                    try {
                                        final XFile outFile = new XFile(dstDir, x + "_" + y + "_" + z);
                                        if (override || !(isExist(outFile, file))) {
                                            out = new XFileOutputStream(outFile);
                                            in = new XFileInputStream(file);
                                            IOUtils.copy(in, out);
                                        }
                                        if (n % 999 == 0) {
                                            logger.info(i + " tiles were copied from 'incoming'");
                                            synchronized (GlobalMapperTileTranslator.class) {
                                                GlobalMapperTileTranslator.class.wait(300);
                                            }
                                        }
                                        n++;
                                    } finally {
                                        if (out != null) {
                                            out.flush();
                                            out.close();
                                        }
                                        if (in != null) {
                                            in.close();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable e) {
            logger.error("map tile importing has failed: ", e);
            throw new GlobalMapperTileTranslatorException(e);
        }
    }

    public boolean isExist(final XFile outFile, final XFile file) {
        return (outFile.exists() && outFile.isFile() && !(overrideIfDiffLength && outFile.length() != file.length()) && !(overrideIfDiffTime && outFile.lastModified() != file.lastModified()));
    }

    public static class GlobalMapperTileTranslatorException extends Exception {

        public GlobalMapperTileTranslatorException() {
        }

        public GlobalMapperTileTranslatorException(String message) {
            super(message);
        }

        public GlobalMapperTileTranslatorException(String message, Throwable cause) {
            super(message, cause);
        }

        public GlobalMapperTileTranslatorException(Throwable cause) {
            super(cause);
        }
    }

    public static void main(String args[]) {
        GlobalMapperTileTranslator translator = new GlobalMapperTileTranslator();
        String allArgs = StringUtils.join(args, ' ');
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(GlobalMapperTileTranslator.class, Object.class);
            for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
                Pattern p = Pattern.compile("-" + pd.getName() + "\\s*([\\S]*)", Pattern.CASE_INSENSITIVE);
                final Matcher m = p.matcher(allArgs);
                if (m.find()) {
                    pd.getWriteMethod().invoke(translator, m.group(1));
                }
            }
            translator.doIt();
        } catch (Throwable e) {
            logger.error("error", e);
            System.out.println(e.getMessage());
            try {
                BeanInfo beanInfo = Introspector.getBeanInfo(GlobalMapperTileTranslator.class);
                System.out.println("Options:");
                for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
                    System.out.println("-" + pd.getName());
                }
            } catch (Throwable t) {
                System.out.print("Internal error");
            }
        }
    }
}
