package org.targol.warfocdamanager.core.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.osgi.framework.Bundle;
import org.targol.warfocdamanager.core.CdaCoreActivator;
import org.targol.warfocdamanager.core.io.internal.IoToModelMapper;
import org.targol.warfocdamanager.core.io.internal.ModelToIoMapper;
import org.targol.warfocdamanager.core.io.internal.armyList.IoArmyList;
import org.targol.warfocdamanager.core.io.internal.armyList.IoGroup;
import org.targol.warfocdamanager.core.io.internal.armyList.IoSlot;
import org.targol.warfocdamanager.core.io.internal.armyList.IoUnit;
import org.targol.warfocdamanager.core.io.internal.cda.IoCda;
import org.targol.warfocdamanager.core.io.internal.cda.IoCdaProgress;
import org.targol.warfocdamanager.core.io.internal.cda.IoCdaSession;
import org.targol.warfocdamanager.core.io.internal.cda.IoCdaUnit;
import org.targol.warfocdamanager.core.io.out.armylist.IArmyListWriter;
import org.targol.warfocdamanager.core.model.GameType;
import org.targol.warfocdamanager.core.model.SlotType;
import org.targol.warfocdamanager.core.model.armylist.ArmyList;
import org.targol.warfocdamanager.core.model.cda.Cda;
import org.targol.warfocdamanager.core.nl.Messages;
import org.targol.warfocdamanager.core.utils.GamesTypesFactory;

/**
 * @author mhardy
 */
public final class FilesHelper {

    public static final String ICON_EMPTY = "/icons/empty.png";

    public static final String FILE_EXT_PROJECT = ".tgl";

    public static final String FILE_EXT_CDA = ".wca";

    public static final String FILE_EXT_BBCODE = ".bbc";

    public static final String FILE_EXT_GAMETYPE = ".gty";

    public static final String FILE_EXT_GAME_EXPORT = ".gte";

    public static final String FILE_EXT_PNG = ".png";

    public static final String GAME_TYPES_FOLDER = "GameTypes";

    public static final String ARMYLISTS_FOLDER = "listes";

    public static final String CDA_FOLDER = "Cda";

    public static final String OUT_FOLDER = "sorties";

    public static final String ZIP_FILE_SEP = "/";

    public static final String FILE_SEP;

    public static final String FOLDERPATH_GAME_TYPES;

    public static final String FOLDERPATH_ARMY_LISTS;

    public static final String FOLDERPATH_CDAS;

    public static final String FOLDERPATH_OUT;

    public static final String FILE_LABEL_PNG;

    public static final String FILE_LABEL_GAME_EXPORT;

    private static final int NB_BITE = 8192;

    private static final Map<String, List<IDirectoryWatcher>> WATCHERS;

    static {
        FILE_SEP = System.getProperty("file.separator");
        FOLDERPATH_ARMY_LISTS = Platform.getLocation().toOSString().concat(FILE_SEP).concat(ARMYLISTS_FOLDER);
        FOLDERPATH_GAME_TYPES = Platform.getLocation().toOSString().concat(FILE_SEP).concat(GAME_TYPES_FOLDER);
        FOLDERPATH_CDAS = Platform.getLocation().toOSString().concat(FILE_SEP).concat(CDA_FOLDER);
        FOLDERPATH_OUT = Platform.getLocation().toOSString().concat(FILE_SEP).concat(OUT_FOLDER);
        FILE_LABEL_PNG = Messages.labelIcon.concat("(*").concat(FILE_EXT_PNG).concat(")");
        FILE_LABEL_GAME_EXPORT = Messages.labelGameTypeExport.concat("(*").concat(FILE_EXT_GAME_EXPORT).concat(")");
        WATCHERS = new HashMap<String, List<IDirectoryWatcher>>();
    }

    /**
	 * Adds a watcher to watch given folder.
	 * 
	 * @param path the path to watch.
	 * @param watcher the watcher.
	 */
    public static void addWatcher(final String path, final IDirectoryWatcher watcher) {
        List<IDirectoryWatcher> list = WATCHERS.get(path);
        if (list == null) {
            list = new ArrayList<IDirectoryWatcher>();
        }
        list.add(watcher);
        WATCHERS.put(path, list);
    }

    /**
	 * Removes a watcher that watched given folder.
	 * 
	 * @param path the path to watch.
	 * @param watcher the watcher.
	 */
    public static void removeWatcher(final String path, final IDirectoryWatcher watcher) {
        final List<IDirectoryWatcher> list = WATCHERS.get(path);
        if (list == null) {
            return;
        }
        list.remove(watcher);
        if (list.size() == 0) {
            WATCHERS.remove(path);
        }
    }

    /**
	 * Notifies all watchers that watches give path.
	 * 
	 * @param path watched path to notify listeners.
	 */
    public static void refreshDir(final String path) {
        final List<IDirectoryWatcher> list = WATCHERS.get(path);
        if (list == null) {
            return;
        }
        for (final IDirectoryWatcher watcher : list) {
            watcher.directoryChanged(createDirectory(path));
        }
    }

    private FilesHelper() {
    }

    /**
	 * Saves given project as a file in workspace.
	 * 
	 * @param al the project to save.
	 * @throws JAXBException in case of error.
	 */
    public static void saveArmyList(final ArmyList al) throws JAXBException {
        final IoArmyList out = ModelToIoMapper.getInstance().mapArmyListFromModeltoIo(al);
        final String path = FOLDERPATH_ARMY_LISTS.concat(FILE_SEP).concat(out.getName()).concat(FILE_EXT_PROJECT);
        final JAXBContext j = JAXBContext.newInstance(IoArmyList.class, IoSlot.class, IoGroup.class, IoUnit.class);
        final Marshaller m = j.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(out, new File(path));
        refreshDir(FOLDERPATH_ARMY_LISTS);
    }

    /**
	 * Loads given project as a file in workspace.
	 * 
	 * @param path the path of the file containing the project to load.
	 * @return loaded project.
	 * @throws JAXBException in case of error.
	 */
    public static ArmyList loadArmyList(final String path) throws JAXBException {
        final JAXBContext j = JAXBContext.newInstance(IoArmyList.class, IoSlot.class, IoGroup.class, IoUnit.class);
        final Unmarshaller m = j.createUnmarshaller();
        final IoArmyList project = (IoArmyList) m.unmarshal(new File(path));
        final ArmyList out = IoToModelMapper.getInstance().mapProjectfromIoToModel(project);
        return out;
    }

    /**
	 * Saves a CDA as xml on the disk.
	 * 
	 * @param cda the Cda to save.
	 * @throws JAXBException in case of error.
	 */
    public static void saveCda(final Cda cda) throws JAXBException {
        final IoCda out = ModelToIoMapper.getInstance().mapCdaFromModeltoIo(cda);
        final String path = FOLDERPATH_CDAS.concat(FILE_SEP).concat(out.getName()).concat(FILE_EXT_CDA);
        createDirectory(FOLDERPATH_CDAS);
        final JAXBContext j = JAXBContext.newInstance(IoCda.class, IoCdaSession.class, IoCdaUnit.class, IoCdaProgress.class);
        final Marshaller m = j.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(out, new File(path));
        refreshDir(FOLDERPATH_CDAS);
    }

    /**
	 * Loads a Cda from a file in workspace.
	 * 
	 * @param path the path of the file containing the Cda to load.
	 * @return loaded Cda.
	 * @throws JAXBException in case of error.
	 */
    public static Cda loadCda(final String path) throws JAXBException {
        final JAXBContext j = JAXBContext.newInstance(IoCda.class, IoCdaSession.class, IoCdaUnit.class, IoCdaProgress.class);
        final Unmarshaller m = j.createUnmarshaller();
        final IoCda ioCda = (IoCda) m.unmarshal(new File(path));
        final Cda out = IoToModelMapper.getInstance().mapCdafromIoToModel(ioCda);
        return out;
    }

    /**
	 * Creates given project's army list using given writer as a file in workspace.
	 * 
	 * @param project the project to save.
	 * @param writer the writer responsible of the output.
	 * @return Full path to generated file.
	 * @throws IOException in case of error.
	 */
    public static TextFile exportArmyList(final ArmyList project, final IArmyListWriter writer) throws IOException {
        createDirectory(FOLDERPATH_OUT);
        final File out = new File(FOLDERPATH_OUT.concat(FILE_SEP).concat(project.getName().replaceAll("\"", "")).concat(writer.getWriterOutFileExtension()));
        final String result = writer.writeArmyListToFile(project, out);
        final TextFile ret = new TextFile(out.getAbsolutePath(), result);
        refreshDir(FOLDERPATH_OUT);
        return ret;
    }

    /**
	 * Saves given image as a png file in workspace.
	 * 
	 * @param image Image to save.
	 * @param name image name (without path or extension).
	 * @return the path to the image.
	 */
    public static String saveImageAsPng(final Image image, final String name) {
        createDirectory(FOLDERPATH_OUT);
        final String path = FOLDERPATH_OUT.concat(FILE_SEP).concat(name).concat(FILE_EXT_PNG);
        final ImageLoader loader = new ImageLoader();
        loader.data = new ImageData[] { image.getImageData() };
        loader.save(path, SWT.IMAGE_PNG);
        refreshDir(FOLDERPATH_OUT);
        return path;
    }

    /**
	 * @param directoryPath : The path of the directory what we want to create
	 * @return The directory created.
	 */
    public static File createDirectory(final String directoryPath) {
        if (directoryPath == null || directoryPath.length() == 0) {
            throw new IllegalArgumentException(Messages.exceptionPathNull);
        }
        final File dir = new File(directoryPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
	 * Allows to copy a file into a specific location.
	 * 
	 * @param source File : the file what we want to copy
	 * @param dest File : the new file
	 * @throws IOException in case of error.
	 */
    public static void copyFile(final File source, final File dest) throws IOException {
        dest.createNewFile();
        final java.io.FileInputStream sourceFile = new java.io.FileInputStream(source);
        try {
            final java.io.FileOutputStream destinationFile = new java.io.FileOutputStream(dest);
            try {
                final byte[] buffer = new byte[NB_BITE];
                int nbLecture = 0;
                while ((nbLecture = sourceFile.read(buffer)) != -1) {
                    destinationFile.write(buffer, 0, nbLecture);
                }
            } finally {
                destinationFile.close();
            }
        } finally {
            sourceFile.close();
        }
    }

    /**
	 * This method copies the content of a directory to another one.
	 * 
	 * @param source source folder.
	 * @param dest destination folder.
	 * @param recursive set to <code>true</code> to copy all files and sub-folders recursively, set to
	 *            <code>false</code> to copy only direct files.
	 * @throws Exception in case of error
	 */
    public static void copyFolderWithContent(final File source, final File dest, final boolean recursive) throws Exception {
        createDirectory(dest.getAbsolutePath());
        for (final File child : source.listFiles()) {
            if (child.isDirectory() && recursive) {
                final File destSubFolder = new File(new StringBuilder(dest.getAbsolutePath()).append(FILE_SEP).append(child.getName()).toString());
                copyFolderWithContent(child, destSubFolder, true);
            } else if (child.isFile()) {
                final File destFile = new File(new StringBuilder(dest.getAbsolutePath()).append(FILE_SEP).append(child.getName()).toString());
                copyFile(child, destFile);
            }
        }
    }

    /**
	 * @return all read GameTypes.
	 * @throws Exception in case of error.
	 */
    public static List<GameType> readGameTypes() throws Exception {
        final File typesFolder = new File(FOLDERPATH_GAME_TYPES);
        final List<GameType> ret = new ArrayList<GameType>();
        for (final File subFolder : typesFolder.listFiles()) {
            if (subFolder.isDirectory()) {
                GameType gt = null;
                gt = loadGameTypeFromFolder(subFolder);
                if (gt != null && !ret.contains(gt)) {
                    ret.add(gt);
                }
            }
        }
        return ret;
    }

    /**
	 * Fills the {@link #FOLDERPATH_ARMY_LISTS} folder with default one(s) stored in src/data.
	 * 
	 * @throws Exception in case of error.
	 */
    public static void fillGameTypesFromData() throws Exception {
        final Bundle b = CdaCoreActivator.getDefault().getBundle();
        final URL source = b.getResource("/src/data");
        final URL u2 = FileLocator.toFileURL(source);
        final String pluginPath = u2.getPath();
        final File f = new File(pluginPath);
        copyFolderWithContent(f, new File(FOLDERPATH_GAME_TYPES), true);
    }

    /**
	 * @param gameTypeFolder a folder that contains gametype files.
	 * @return created GameType.
	 * @throws Exception in case of error.
	 */
    private static GameType loadGameTypeFromFolder(final File gameTypeFolder) throws Exception {
        final File[] gtFiles = gameTypeFolder.listFiles(new ExtensionFilter(FILE_EXT_GAMETYPE, false));
        if (gtFiles == null || gtFiles.length != 1) {
            final String msg = NLS.bind(Messages.exceptionFolderCanContainOnlyOneFile, gameTypeFolder.getAbsolutePath(), FILE_EXT_GAMETYPE);
            throw new Exception(msg);
        }
        final GameType ret = loadGameTypeFromFile(gtFiles[0]);
        GamesTypesFactory.fillAllRootPathes(ret, gameTypeFolder.getAbsolutePath());
        return ret;
    }

    private static GameType loadGameTypeFromFile(final File gtFile) throws JAXBException {
        final JAXBContext j = JAXBContext.newInstance(GameType.class, SlotType.class);
        final Unmarshaller m = j.createUnmarshaller();
        final GameType ret = (GameType) m.unmarshal(gtFile);
        replaceNullsByEmptyIcon(ret);
        return ret;
    }

    /**
	 * Saves given GameType as a file in workspace.
	 * 
	 * @param gt game type to save.
	 * @throws JAXBException in case of error.
	 */
    public static void saveGameType(final GameType gt) throws JAXBException {
        replaceEmptyIconsByNulls(gt);
        final String path = FOLDERPATH_GAME_TYPES.concat(FILE_SEP).concat(gt.getName()).concat(FILE_SEP).concat(gt.getName()).concat(FILE_EXT_GAMETYPE);
        final JAXBContext j = JAXBContext.newInstance(GameType.class, SlotType.class);
        final Marshaller m = j.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(gt, new File(path));
        replaceNullsByEmptyIcon(gt);
        refreshDir(FOLDERPATH_GAME_TYPES);
    }

    private static void replaceEmptyIconsByNulls(final GameType gt) {
        if (ICON_EMPTY.equals(gt.getRelativeIconPath())) {
            gt.setRelativeIconPath(null);
        }
        for (final SlotType t : gt.getSlots()) {
            if (ICON_EMPTY.equals(t.getRelativeIconPath())) {
                t.setRelativeIconPath(null);
            }
        }
    }

    private static void replaceNullsByEmptyIcon(final GameType gt) {
        final String icon = ICON_EMPTY;
        if (gt.getRelativeIconPath() == null) {
            gt.setRelativeIconPath(ICON_EMPTY);
        }
        for (final SlotType t : gt.getSlots()) {
            if (t.getRelativeIconPath() == null) {
                t.setRelativeIconPath(ICON_EMPTY);
            }
        }
    }

    /**
	 * File filter depending on extension.
	 * 
	 * @author mhardy
	 */
    public static class ExtensionFilter implements FileFilter {

        private final String choosenExtension;

        private final boolean isCaseSensitive;

        /**
		 * Constructor.
		 * 
		 * @param extension extension that files must have to pass filter.
		 * @param caseSensitive set to <code>true</code> for a case sensitive extension comparator.
		 */
        public ExtensionFilter(final String extension, final boolean caseSensitive) {
            if (caseSensitive) {
                this.choosenExtension = extension;
            } else {
                this.choosenExtension = extension.toUpperCase();
            }
            this.isCaseSensitive = caseSensitive;
        }

        /**
		 * {@inheritDoc}
		 */
        @Override
        public final boolean accept(final File fileToTest) {
            if (fileToTest.isDirectory()) {
                return false;
            }
            String name = fileToTest.getName();
            if (!this.isCaseSensitive) {
                name = name.toUpperCase();
            }
            if (name != null && name.endsWith(this.choosenExtension)) {
                return true;
            }
            return false;
        }
    }

    /**
	 * This void allows to zip the directory and is child.
	 * 
	 * @param zipDir => File : The directory that we want to zip.
	 * @param parentRelativePath relative to archive root parent path.
	 * @param zos => ZipOutputStream : The ZipOutputStream of the current zip.
	 * @throws IOException in case of IO error.
	 */
    private static void zipDir(final File zipDir, final String parentRelativePath, final ZipOutputStream zos) throws IOException {
        if (zipDir == null || zos == null) {
            throw new IllegalArgumentException("directory and ZipOutputStream shouldn't be null");
        }
        final File[] dirChildren = zipDir.listFiles();
        String entry = zipDir.getName();
        if (parentRelativePath.length() > 0) {
            entry = parentRelativePath + ZIP_FILE_SEP + zipDir.getName();
        }
        final ZipEntry anDirectoryEntry = new ZipEntry(entry + ZIP_FILE_SEP);
        zos.putNextEntry(anDirectoryEntry);
        final byte[] readBuffer = new byte[NB_BITE];
        int bytesIn = 0;
        for (final File element : dirChildren) {
            if (element.isDirectory()) {
                zipDir(element, entry, zos);
            } else {
                final FileInputStream fis = new FileInputStream(element);
                final ZipEntry anFileEntry = new ZipEntry(entry + ZIP_FILE_SEP + element.getName());
                zos.putNextEntry(anFileEntry);
                while ((bytesIn = fis.read(readBuffer)) != -1) {
                    zos.write(readBuffer, 0, bytesIn);
                }
                fis.close();
            }
        }
    }

    /**
	 * This void allows to delete a folder or a file.
	 * 
	 * @param path Path to the folder/file to delete
	 * @throws IOException if the file or the directory isn't delete.
	 */
    public static void recursifDelete(final String path, final boolean errorIfDoesntExist) throws IOException {
        if (path == null || path.length() == 0) {
            throw new IllegalArgumentException("path shouldn't be null");
        }
        File base;
        base = new File(path);
        if (!base.exists()) {
            if (errorIfDoesntExist) {
                throw new IOException("File not found '" + base.getAbsolutePath() + "'");
            }
            return;
        }
        if (base.isDirectory()) {
            final File[] children = base.listFiles();
            for (int i = 0; children != null && i < children.length; i++) {
                recursifDelete(path + File.separatorChar + children[i].getName(), false);
            }
            if (!base.delete()) {
                throw new IOException("No delete directory '" + base.getAbsolutePath() + "'");
            }
        } else if (!base.delete()) {
            throw new IOException("No delete file '" + base.getAbsolutePath() + "'");
        }
    }

    /**
	 * This method creates a zip file from the directory passed in parameter .
	 * 
	 * @param zipFileName The name of the created zip file.
	 * @param pathDirectoryToZip The directory that we want to put in zip format.
	 * @param pathZipFile The path of created zip file .
	 * @throws Exception in case of error
	 * 
	 */
    public static void createZipFile(final String zipFileName, final String pathDirectoryToZip, final String pathZipFile) throws Exception {
        if (zipFileName == null || zipFileName.length() == 0) {
            throw new IllegalArgumentException("zipFileName shouldn't be null");
        }
        if (pathDirectoryToZip == null || pathDirectoryToZip.length() == 0) {
            throw new IllegalArgumentException("pathDirectoryToZip shouldn't be null");
        }
        final File di = new File(pathDirectoryToZip);
        final File[] files1 = di.listFiles();
        final FileOutputStream dest = new FileOutputStream(pathZipFile + ZIP_FILE_SEP + zipFileName);
        final CheckedOutputStream checksum = new CheckedOutputStream(dest, new Adler32());
        final BufferedOutputStream buff = new BufferedOutputStream(checksum);
        final ZipOutputStream out = new ZipOutputStream(buff);
        out.setMethod(ZipOutputStream.DEFLATED);
        out.setLevel(Deflater.BEST_COMPRESSION);
        final byte[] data = new byte[NB_BITE];
        for (final File f : files1) {
            if (f.isDirectory()) {
                zipDir(f, "", out);
            } else {
                final FileInputStream fi = new FileInputStream(pathDirectoryToZip + ZIP_FILE_SEP + f.getName());
                final BufferedInputStream buffi = new BufferedInputStream(fi, NB_BITE);
                final ZipEntry entry = new ZipEntry(f.getName());
                out.putNextEntry(entry);
                int count;
                while ((count = buffi.read(data, 0, NB_BITE)) != -1) {
                    out.write(data, 0, count);
                }
                out.closeEntry();
                buffi.close();
            }
        }
        out.close();
        buff.close();
        checksum.close();
        dest.close();
    }

    /**
	 * Unzips given zip file to given directory.
	 * 
	 * @param zipFullPath full path of zip file.
	 * @param destPath Destination folder.
	 * @throws IOException in case of error.
	 */
    public static void unziptoDir(final String zipFullPath, final String destPath) throws IOException {
        String out = destPath;
        if (!out.endsWith(FILE_SEP)) {
            out = destPath.concat(FILE_SEP);
        }
        final FileInputStream fis = new FileInputStream(zipFullPath);
        final ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
        BufferedOutputStream dest = null;
        ZipEntry entry = null;
        while ((entry = zis.getNextEntry()) != null) {
            int count;
            final byte[] data = new byte[NB_BITE];
            final FileOutputStream fos = new FileOutputStream(out.concat(entry.getName()));
            dest = new BufferedOutputStream(fos, NB_BITE);
            while ((count = zis.read(data, 0, NB_BITE)) != -1) {
                dest.write(data, 0, count);
            }
            dest.flush();
            dest.close();
        }
        zis.close();
    }
}
