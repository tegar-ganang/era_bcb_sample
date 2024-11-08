package de.schwarzrot.dvd.theme.support;

import java.awt.Font;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import de.schwarzrot.app.errors.ApplicationException;
import de.schwarzrot.app.support.ApplicationServiceProvider;
import de.schwarzrot.data.access.Repository;
import de.schwarzrot.data.transaction.TOSave;
import de.schwarzrot.data.transaction.Transaction;
import de.schwarzrot.data.transaction.TransactionStatus;
import de.schwarzrot.data.transaction.support.TransactionFactory;
import de.schwarzrot.dvd.theme.MenueDefinition;
import de.schwarzrot.dvd.theme.Theme;
import de.schwarzrot.dvd.theme.domain.ElementSkin;
import de.schwarzrot.dvd.theme.domain.PageSkin;
import de.schwarzrot.dvd.theme.domain.ThemeElement;
import de.schwarzrot.dvd.theme.domain.data.MenueElementCategory;
import de.schwarzrot.dvd.theme.domain.data.MenuePageType;
import de.schwarzrot.system.SysInfo;
import de.schwarzrot.system.support.FileUtils;

public class ExchangeHandler {

    private static final int BUF_SIZE = 48 * 1024;

    private static final String ARCHIVE_EXTENSION = ".zip";

    private static TransactionFactory taFactory;

    private static Repository repository;

    private static SysInfo sysInfo;

    private static MenueDefinition menueDef;

    private static Map<String, List<File>> fontCache;

    private File fontBaseDir;

    private File workingDir;

    public ExchangeHandler() {
    }

    public ExchangeHandler(File workingDir) {
        this.workingDir = workingDir;
    }

    protected String genArchiveName(Theme theme) {
        StringBuffer sb = new StringBuffer(theme.getName());
        sb.append("-");
        sb.append(theme.getAspect().toString());
        sb.append("-");
        sb.append(new Long(new Date().getTime() / 1000).toString());
        sb.append(ARCHIVE_EXTENSION);
        return sb.toString();
    }

    public File exportTemplateArchive() {
        if (menueDef == null) setup();
        Theme theme2Export = menueDef.getTheme();
        Map<MenuePageType, PageSkin> skin2Export = menueDef.getSkin();
        File exportDir = new File(workingDir, theme2Export.getName());
        File archive = new File(workingDir, genArchiveName(theme2Export));
        File themeExportFile = new File(exportDir, String.format("%s-%s-Theme.xml", theme2Export.getName(), theme2Export.getAspect()));
        File skinExportFile = new File(exportDir, String.format("%s-Skin.xml", menueDef.getSkinName()));
        File targetFile;
        File tmp;
        try {
            exportDir.mkdirs();
            repository.performExport(theme2Export, themeExportFile);
            repository.performExport(skin2Export, menueDef.getSkinName(), skinExportFile);
        } catch (Throwable t) {
            throw new ApplicationException("failed to export menue template", t);
        }
        for (MenuePageType mpt : MenuePageType.values()) {
            List<ThemeElement<?>> elems = theme2Export.getThemeElements(mpt);
            for (ThemeElement<?> te : elems) {
                if (te.getImageName() != null) {
                    getLogger().info("check image [ " + te.getImageName().getAbsolutePath() + " ]");
                    targetFile = new File(exportDir, te.getImageName().getName());
                    if (!targetFile.exists()) FileUtils.copyFile(targetFile, te.getImageName());
                }
            }
        }
        for (MenuePageType mpt : skin2Export.keySet()) {
            PageSkin skin = skin2Export.get(mpt);
            for (MenueElementCategory cat : skin.getElements().keySet()) {
                ElementSkin eSkin = skin.getElementSkin(cat);
                if (eSkin.getFont() != null) {
                    getLogger().info("check font [" + eSkin.getFont() + "]");
                    if (fontCache.containsKey(eSkin.getFont())) {
                        for (File fontFile : fontCache.get(eSkin.getFont())) {
                            targetFile = new File(exportDir, fontFile.getName());
                            if (!targetFile.exists()) FileUtils.copyFile(targetFile, fontFile);
                        }
                    }
                }
                if (eSkin.getImage() != null) {
                    tmp = eSkin.getImage();
                    if (tmp.isFile() && tmp.canRead()) {
                        getLogger().info("check image " + tmp.getAbsolutePath());
                        targetFile = new File(exportDir, tmp.getName());
                        if (tmp.isFile() && tmp.canRead() && !targetFile.exists()) {
                            FileUtils.copyFile(targetFile, tmp);
                        }
                    }
                }
            }
        }
        createArchive(exportDir, archive);
        if (archive.exists()) FileUtils.removeDirectory(exportDir);
        return archive;
    }

    public final File getWorkingDirectory() {
        return workingDir;
    }

    @SuppressWarnings("unchecked")
    public void importTemplateArchive(File importArchive, String sudoPassword) {
        if (repository == null) setup();
        Theme theme2Import = null;
        Map<MenuePageType, PageSkin> skin2Import = null;
        File fontTargetDir = new File(fontBaseDir, "vatemplates");
        File tmp;
        File targetFile;
        if (sysInfo.isSuse()) fontTargetDir = fontBaseDir;
        if (importArchive.exists() && importArchive.isFile() && importArchive.canRead()) {
            getLogger().info("found archive " + importArchive + " to extract");
            File importDir = extractArchive(importArchive);
            if (importDir == null || !(importDir.exists() && importDir.isDirectory() && importDir.canRead())) {
                throw new ApplicationException("failed to extract archive " + importArchive);
            }
            for (File archiveEntry : importDir.listFiles()) {
                if (archiveEntry.getName().toUpperCase().endsWith(".TTF")) {
                    getLogger().info(archiveEntry.getName() + " should be copied to fonts directory");
                    targetFile = new File(fontTargetDir, archiveEntry.getName());
                    FileUtils.copyFile(targetFile, archiveEntry, sudoPassword);
                    continue;
                }
                if (archiveEntry.getName().endsWith(".xml")) {
                    try {
                        Object any = repository.performImport(archiveEntry);
                        if (any instanceof Theme) theme2Import = (Theme) any; else if (any instanceof Map<?, ?>) skin2Import = (Map<MenuePageType, PageSkin>) any;
                    } catch (Throwable t) {
                        throw new ApplicationException("failed to import " + archiveEntry, t);
                    }
                }
            }
            if (theme2Import != null) {
                if (menueDef.isKnownTheme(theme2Import)) {
                    String originalName = theme2Import.getName();
                    for (int i = 0; i < 10; i++) {
                        theme2Import.setThemeName(originalName + i);
                        if (!menueDef.isKnownTheme(theme2Import)) break;
                    }
                    if (menueDef.isKnownTheme(theme2Import)) {
                        throw new ApplicationException("theme does already exists and I could not guess an unused name");
                    }
                }
                try {
                    Transaction ta = taFactory.createTransaction();
                    ta.add(new TOSave<Theme>(theme2Import));
                    ta.execute();
                    if (ta.getStatus().equals(TransactionStatus.STATUS_COMMITTED)) {
                        for (MenuePageType mpt : MenuePageType.values()) {
                            List<ThemeElement<?>> elems = theme2Import.getThemeElements(mpt);
                            for (ThemeElement<?> te : elems) {
                                if (te.getImageName() != null) {
                                    targetFile = te.getImageName();
                                    if (targetFile == null) continue;
                                    if (targetFile.getName().length() < 2) continue;
                                    tmp = new File(importDir, targetFile.getName());
                                    if (tmp.exists()) {
                                        getLogger().info("should copy " + targetFile.getName() + " to " + targetFile);
                                        FileUtils.copyFile(targetFile, tmp, sudoPassword);
                                    }
                                }
                            }
                        }
                    }
                } catch (Throwable t) {
                    throw new ApplicationException("failed to import theme", t);
                }
            }
            if (skin2Import != null) {
                PageSkin any = skin2Import.values().iterator().next();
                if (menueDef.isKnownSkin(any.getName())) {
                    String tempName = any.getName();
                    for (int i = 0; i < 10; i++) {
                        tempName = any.getName() + i;
                        if (!menueDef.isKnownSkin(tempName)) break;
                    }
                    if (menueDef.isKnownSkin(tempName)) {
                        throw new ApplicationException("skin does already exists and I could not guess an unused name");
                    }
                    if (tempName.compareTo(any.getName()) != 0) {
                        for (MenuePageType mpt : skin2Import.keySet()) skin2Import.get(mpt).setName(tempName);
                    }
                }
                try {
                    Transaction ta = taFactory.createTransaction();
                    for (MenuePageType mpt : skin2Import.keySet()) ta.add(new TOSave<PageSkin>(skin2Import.get(mpt)));
                    ta.execute();
                    if (ta.getStatus().equals(TransactionStatus.STATUS_COMMITTED)) {
                        for (MenuePageType mpt : skin2Import.keySet()) {
                            PageSkin skin = skin2Import.get(mpt);
                            for (MenueElementCategory cat : skin.getElements().keySet()) {
                                ElementSkin eSkin = skin.getElementSkin(cat);
                                targetFile = eSkin.getImage();
                                if (targetFile == null) continue;
                                if (targetFile.getName().length() < 2) continue;
                                tmp = new File(importDir, targetFile.getName());
                                if (tmp.exists()) {
                                    getLogger().info("should copy " + targetFile.getName() + " to " + targetFile);
                                    FileUtils.copyFile(targetFile, tmp, sudoPassword);
                                }
                            }
                        }
                    }
                } catch (Throwable t) {
                    throw new ApplicationException("failed to import skin", t);
                }
            }
        }
    }

    public final void setWorkingDirectory(File workingDir) {
        this.workingDir = workingDir;
    }

    protected File createArchive(File dir2Archive, File newArchive) {
        byte buf[] = new byte[BUF_SIZE];
        if (dir2Archive.getParentFile().canWrite()) {
            ZipOutputStream out = null;
            String prefix = dir2Archive.getName() + "/";
            ZipEntry entry;
            int cRead;
            try {
                FileOutputStream stream = new FileOutputStream(newArchive);
                out = new ZipOutputStream(stream);
                entry = new ZipEntry(prefix);
                entry.setTime(dir2Archive.lastModified());
                out.putNextEntry(entry);
                for (File cur : dir2Archive.listFiles()) {
                    if (cur.isFile() && cur.canRead()) {
                        entry = new ZipEntry(prefix + cur.getName());
                        entry.setTime(cur.lastModified());
                        entry.setSize(cur.length());
                        out.putNextEntry(entry);
                        FileInputStream in = new FileInputStream(cur);
                        while ((cRead = in.read(buf, 0, buf.length)) > 0) {
                            out.write(buf, 0, cRead);
                        }
                        in.close();
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                try {
                    if (out != null) out.close();
                } catch (Throwable t) {
                }
            }
        }
        return newArchive;
    }

    protected File extractArchive(File archive) {
        byte buf[] = new byte[BUF_SIZE];
        ZipInputStream zis = null;
        File archiveDirectory = null;
        FileOutputStream fos;
        ZipEntry entry;
        File curEntry;
        int n;
        try {
            zis = new ZipInputStream(new FileInputStream(archive));
            while ((entry = zis.getNextEntry()) != null) {
                curEntry = new File(workingDir, entry.getName());
                if (entry.isDirectory()) {
                    getLogger().info("skip directory: " + entry.getName());
                    if (archiveDirectory == null) archiveDirectory = curEntry;
                    continue;
                }
                getLogger().info("zip-entry (file): " + entry.getName() + " ==> real path: " + curEntry.getAbsolutePath());
                if (!curEntry.getParentFile().exists()) curEntry.getParentFile().mkdirs();
                fos = new FileOutputStream(curEntry);
                while ((n = zis.read(buf, 0, buf.length)) > -1) fos.write(buf, 0, n);
                fos.close();
                zis.closeEntry();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            try {
                if (zis != null) zis.close();
            } catch (Throwable t) {
            }
        }
        return archiveDirectory;
    }

    protected final Log getLogger() {
        return LogFactory.getLog(getClass());
    }

    protected void setup() {
        sysInfo = (SysInfo) ApplicationServiceProvider.getService(SysInfo.class);
        repository = (Repository) ApplicationServiceProvider.getService(Repository.class);
        taFactory = (TransactionFactory) ApplicationServiceProvider.getService(TransactionFactory.class);
        menueDef = (MenueDefinition) ApplicationServiceProvider.getService(MenueDefinition.class);
        fontBaseDir = sysInfo.getFontBaseDir();
        if (workingDir == null) workingDir = sysInfo.getTempDirectory();
        fontCache = new HashMap<String, List<File>>();
        setupFontCache(fontCache, fontBaseDir);
    }

    protected void setupFontCache(Map<String, List<File>> cache, File baseDir) {
        for (File cur : baseDir.listFiles()) {
            if (cur.isFile() && cur.getName().toUpperCase().endsWith("TTF")) {
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(cur);
                    Font font = Font.createFont(Font.TRUETYPE_FONT, fis);
                    getLogger().info("found font: " + font.getFontName());
                    if (!cache.containsKey(font.getFamily())) cache.put(font.getFamily(), new ArrayList<File>());
                    cache.get(font.getFamily()).add(cur);
                } catch (Throwable t) {
                    t.printStackTrace();
                } finally {
                    try {
                        if (fis != null) fis.close();
                    } catch (Throwable t) {
                    }
                }
            } else if (cur.isDirectory()) {
                setupFontCache(cache, cur);
            }
        }
    }
}
