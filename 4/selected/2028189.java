package org.kalypso.nofdpidss.core.common.utils.various;

import java.io.File;
import java.io.FileFilter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.namespace.QName;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.NotImplementedException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.kalypso.nofdp.idss.schema.schemata.gml.GmlConstants;
import org.kalypso.nofdpidss.core.NofdpCorePlugin;
import org.kalypso.nofdpidss.core.base.WorkspaceSync;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IDataStructureMember;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IGeodataCategories;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IGeodataCategory;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IGeodataModel;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IGeodataSet;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IGeodataSetTypes;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IMap;
import org.kalypso.nofdpidss.core.base.gml.model.geodata.IMaps;
import org.kalypso.nofdpidss.core.base.gml.pool.MyBasePool;
import org.kalypso.nofdpidss.core.base.gml.pool.PoolGeoData;
import org.kalypso.nofdpidss.core.common.NofdpIDSSConstants;
import org.kalypso.nofdpidss.core.i18n.Messages;
import org.kalypso.ogc.gml.FeatureUtils;
import org.kalypsodeegree.model.feature.Feature;

/**
 * @author Dirk Kuch
 */
public class BaseGeoUtils {

    public static boolean checkSubCategoryParent(final Feature fSelection) {
        if (fSelection == null) return false;
        final QName name = fSelection.getFeatureType().getSubstitutionGroupFT().getQName();
        if (IGeodataCategories.QN_CATEGORY_TYPE.equals(name) || IGeodataCategories.QN_ABSTRACT_CATEGORY.equals(name) || IGeodataCategories.QN_ABSTRACT_SUB_CATEGORY.equals(name) || IGeodataCategories.QN_SUB_CATEGORY.equals(name)) return true;
        return false;
    }

    public static void cloneMap(final IProject project, final MyBasePool pool, final Feature f) throws Exception {
        final Object objMap = f.getProperty(IMap.QN_FILE_NAME);
        if (!(objMap instanceof String)) throw new IllegalStateException();
        final String sMap = (String) objMap;
        final IFolder folder = project.getFolder(IMaps.FOLDER);
        final IFile iFile = folder.getFile(sMap);
        if (!iFile.exists()) throw new IllegalStateException();
        final String sDestName = BaseGeoUtils.getFileName(folder, sMap.split("\\.")[0]) + "." + sMap.split("\\.")[1];
        final File fSrc = iFile.getLocation().toFile();
        final File fDest = new File(folder.getLocation().toFile(), sDestName);
        FileUtils.copyFile(fSrc, fDest);
        FeatureUtils.updateProperty(pool.getWorkspace(), f, IMap.QN_FILE_NAME, sDestName);
        WorkspaceSync.sync(project, IResource.DEPTH_INFINITE);
    }

    public static IFolder createDir(final IFolder folder, final String dirName) throws CoreException {
        if (folder == null || dirName == null) return null;
        final String name = BaseGeoUtils.toFileName(dirName);
        final IFolder newFolder = folder.getFolder(name);
        newFolder.create(true, false, new NullProgressMonitor());
        newFolder.getWorkspace().save(true, new NullProgressMonitor());
        final IFolder styleFolder = folder.getProject().getFolder(NofdpIDSSConstants.NOFDP_PROJECT_STYLE_FOLDER_PATH);
        final IFolder nStyleFolder = styleFolder.getFolder(name);
        nStyleFolder.create(true, false, new NullProgressMonitor());
        nStyleFolder.getWorkspace().save(true, new NullProgressMonitor());
        return newFolder;
    }

    public static IFolder createSubDir(final IProject project, final Feature fSelection, final String name) throws CoreException {
        if (fSelection == null || name == null || "".equals(name) || "".equals(BaseGeoUtils.toFileName(name))) return null;
        final IFolder folder = BaseGeoUtils.getSubDirLocation(project, fSelection);
        if (folder == null) throw new IllegalStateException(Messages.BaseGeoUtils_0); else if (!folder.exists()) folder.create(true, true, new NullProgressMonitor());
        final IFolder nFolder = folder.getFolder(BaseGeoUtils.toFileName(name));
        nFolder.create(true, true, new NullProgressMonitor());
        nFolder.getWorkspace().save(true, new NullProgressMonitor());
        return nFolder;
    }

    public static void deleteFile(final IFolder folder, final String fileName) {
        BaseGeoUtils.deleteGeoDataFile(folder, fileName);
    }

    public static void deleteFolder(final IFolder folder) throws CoreException {
        if (folder == null) return;
        final String folderPath = folder.getProjectRelativePath().removeFirstSegments(1).toString();
        final IFolder styleFolder = folder.getProject().getFolder(NofdpIDSSConstants.NOFDP_PROJECT_STYLE_FOLDER_PATH + "/" + folderPath);
        styleFolder.delete(true, new NullProgressMonitor());
        folder.delete(true, new NullProgressMonitor());
    }

    public static void deleteGeoDataFile(final IFolder folder, final String fileName) {
        if (folder == null || fileName == null) return;
        final File dir = new File(folder.getLocation().toOSString());
        if (!dir.isDirectory()) return;
        final File fSld = new File(new File(dir, NofdpIDSSConstants.NOFDP_PROJECT_GEODATA_STYLES_FOLDER), fileName + ".sld");
        if (fSld.exists()) fSld.delete();
        final String[] parts = fileName.split("\\.");
        String fileRegEx = "";
        if (parts.length == 1) {
            fileRegEx += parts[0] + ".";
        } else {
            for (int i = 0; i < parts.length - 1; i++) fileRegEx += parts[i] + ".";
        }
        final File[] files = BaseGeoUtils.getFiles(dir, fileRegEx);
        if (files == null) return;
        for (final File file : files) file.delete();
        WorkspaceSync.sync(folder, IResource.DEPTH_INFINITE);
    }

    private static boolean existsInFiles(final File[] files, final String regex) {
        final Set<String> list = new HashSet<String>();
        for (final File file : files) {
            final String[] part = file.getName().split("\\.");
            list.add(part[0]);
        }
        for (final String name : list) if (name.equals(regex)) return true;
        return false;
    }

    public static String getFileName(final File dirDest, String regex) {
        if (dirDest != null && !dirDest.exists()) dirDest.mkdirs();
        if (!dirDest.isDirectory() || regex == null) throw new IllegalStateException(Messages.BaseGeoUtils_1);
        final String orig = regex;
        final File[] files = BaseGeoUtils.getFiles(dirDest, regex);
        int count = 0;
        while (BaseGeoUtils.existsInFiles(files, regex)) {
            regex = orig + Integer.toString(count);
            count++;
        }
        return regex;
    }

    public static String getFileName(final IFolder folder, final String regex) {
        final File file = folder.getLocation().toFile();
        return BaseGeoUtils.getFileName(file, regex);
    }

    public static File[] getFiles(final File fDir, final String regex) {
        if (fDir == null || regex == null || !fDir.isDirectory()) return new File[0];
        final String myRegEx = regex.toLowerCase();
        final File[] files = fDir.listFiles(new FileFilter() {

            public boolean accept(File pathname) {
                if (pathname.getName().toLowerCase().contains(myRegEx)) return true;
                return false;
            }
        });
        return files;
    }

    public static IFile getMap(final IProject project, final PoolGeoData pool, final String internalMapId) throws CoreException {
        final IGeodataModel model = pool.getModel();
        final IMap map = model.getMaps().getMap(internalMapId);
        return map.getIFile();
    }

    public static IFile getStyleTemplateForCategory(final Feature category) {
        final Object objAbstrStrMember = category.getProperty(IGeodataSet.QN_ABSTRACT_GEODATA_MEMBER);
        if (objAbstrStrMember == null || !(objAbstrStrMember instanceof Feature)) throw new IllegalStateException();
        final Feature fAbstrStrMember = (Feature) objAbstrStrMember;
        final Object objNameSld = fAbstrStrMember.getProperty(IDataStructureMember.QN_MAP_SYMBOLIZATION);
        if (objNameSld == null || !(objNameSld instanceof String) || "".equals(objNameSld)) throw new IllegalStateException();
        return BaseGeoUtils.getStyleTemplateForCategory((String) objNameSld);
    }

    public static IFile getStyleTemplateForCategory(final String templateSld) {
        final IProject global = NofdpCorePlugin.getProjectManager().getBaseProject();
        final IFolder templateFolder = global.getFolder(NofdpIDSSConstants.NOFDP_PROJECT_GLOBAL_STYLES_FOLDER);
        final IFile iTemplate = templateFolder.getFile(templateSld);
        if (!iTemplate.exists()) throw new IllegalStateException(Messages.BaseGeoUtils_3 + iTemplate.getLocation().toOSString());
        return iTemplate;
    }

    public static IFolder getSubDirLocation(final IProject project, final Feature fSelection) throws CoreException {
        if (fSelection == null || project == null) return null;
        final QName name = fSelection.getFeatureType().getSubstitutionGroupFT().getQName();
        if (IGeodataCategories.QN_ABSTRACT_CATEGORY.equals(name) || IGeodataCategories.QN_CATEGORY_TYPE.equals(name)) {
            final String foldername = (String) fSelection.getProperty(GmlConstants.QN_GEODATA_FOLDER_PATH);
            final IFolder folder = project.getFolder(NofdpIDSSConstants.NOFDP_GEODATA_BASE_FOLDER).getFolder(foldername.toLowerCase());
            if (!folder.exists()) folder.create(true, true, new NullProgressMonitor());
            return folder;
        } else if (IGeodataCategories.QN_SUB_CATEGORY.equals(name) || IGeodataCategories.QN_ABSTRACT_SUB_CATEGORY.equals(name)) {
            final IFolder parentFolder = BaseGeoUtils.getSubDirLocation(project, fSelection.getParent());
            final String foldername = (String) fSelection.getProperty(GmlConstants.QN_GEODATA_FOLDER_PATH);
            final IFolder folder = parentFolder.getFolder(foldername);
            if (!folder.exists()) folder.create(true, true, new NullProgressMonitor());
            return folder;
        } else if (IGeodataSetTypes.QN_ABSTRACT_GEODATA_SET.equals(name)) return BaseGeoUtils.getSubDirLocation(project, fSelection.getParent());
        throw new NotImplementedException(Messages.BaseGeoUtils_4 + name.toString() + Messages.BaseGeoUtils_5);
    }

    public static boolean selectionIsAddable(final Feature fSelection) {
        final QName qName = fSelection.getFeatureType().getSubstitutionGroupFT().getQName();
        if (IGeodataCategories.QN_ABSTRACT_CATEGORY.equals(qName) || IGeodataCategories.QN_CATEGORY_TYPE.equals(qName) || IGeodataCategories.QN_ABSTRACT_SUB_CATEGORY.equals(qName) || IGeodataCategories.QN_SUB_CATEGORY.equals(qName)) return true;
        return false;
    }

    public static boolean selectionIsDeleteable(final Feature fSelection) {
        final QName qName = fSelection.getFeatureType().getSubstitutionGroupFT().getQName();
        if (IGeodataSetTypes.QN_ABSTRACT_GEODATA_SET.equals(qName) || IGeodataSetTypes.QN_GEODATA_SET.equals(qName)) return true; else if (IGeodataCategories.QN_ABSTRACT_CATEGORY.equals(qName) || IGeodataCategories.QN_CATEGORY_TYPE.equals(qName) || IGeodataCategories.QN_ABSTRACT_SUB_CATEGORY.equals(qName) || IGeodataCategories.QN_SUB_CATEGORY.equals(qName)) {
            final Object objDefaultCat = fSelection.getProperty(IGeodataSet.QN_IS_NOFDP_TYPE);
            if (objDefaultCat instanceof Boolean && (Boolean) objDefaultCat == true) return false; else {
                final Object objLstChilds = fSelection.getProperty(IGeodataCategory.QN_SUBCATEGORY_MEMBER);
                if (objLstChilds == null || !(objLstChilds instanceof List)) throw new IllegalStateException();
                final List childs = (List) objLstChilds;
                for (final Object object : childs) {
                    if (!(object instanceof Feature)) continue;
                    final Feature feature = (Feature) object;
                    final QName type = feature.getFeatureType().getSubstitutionGroupFT().getQName();
                    if (IGeodataCategories.QN_ABSTRACT_CATEGORY.equals(type) || IGeodataCategories.QN_CATEGORY_TYPE.equals(type) || IGeodataCategories.QN_ABSTRACT_SUB_CATEGORY.equals(type) || IGeodataCategories.QN_SUB_CATEGORY.equals(type)) {
                        final boolean value = BaseGeoUtils.selectionIsDeleteable(feature);
                        if (value == false) return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    public static String toFileName(final String name) {
        final char[] arr = name.toLowerCase().toCharArray();
        final Pattern p = Pattern.compile("[a-z0-9]");
        String sName = "";
        for (final char c : arr) {
            final Matcher m = p.matcher("" + c);
            if (m.matches()) sName += c;
        }
        return sName;
    }
}
