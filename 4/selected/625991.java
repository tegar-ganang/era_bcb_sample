package net.sourceforge.dita4publishers.tools.dxp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import net.sourceforge.dita4publishers.api.bos.BosException;
import net.sourceforge.dita4publishers.api.bos.BosMember;
import net.sourceforge.dita4publishers.api.bos.BosVisitor;
import net.sourceforge.dita4publishers.api.ditabos.DitaBoundedObjectSet;
import net.sourceforge.dita4publishers.impl.bos.BosConstructionOptions;
import net.sourceforge.dita4publishers.impl.dita.AddressingUtil;
import net.sourceforge.dita4publishers.impl.ditabos.DitaBosHelper;
import net.sourceforge.dita4publishers.tools.common.MapBosProcessorOptions;
import net.sourceforge.dita4publishers.util.DomUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Helper class for working with DITA DXP packages.
 */
public class DitaDxpHelper {

    private static Log log = LogFactory.getLog(DitaDxpHelper.class);

    /**
	 * Given a DITA map bounded object set, zips it up into a DXP Zip package.
	 * @param mapBos
	 * @param outputZipFile
	 * @throws Exception 
	 */
    public static void zipMapBos(DitaBoundedObjectSet mapBos, File outputZipFile, MapBosProcessorOptions options) throws Exception {
        log.debug("Determining zip file organization...");
        BosVisitor visitor = new DxpFileOrganizingBosVisitor();
        visitor.visit(mapBos);
        if (!options.isQuiet()) log.info("Creating DXP package \"" + outputZipFile.getAbsolutePath() + "\"...");
        OutputStream outStream = new FileOutputStream(outputZipFile);
        ZipOutputStream zipOutStream = new ZipOutputStream(outStream);
        ZipEntry entry = null;
        URI rootMapUri = mapBos.getRoot().getEffectiveUri();
        URI baseUri = null;
        try {
            baseUri = AddressingUtil.getParent(rootMapUri);
        } catch (URISyntaxException e) {
            throw new BosException("URI syntax exception getting parent URI: " + e.getMessage());
        }
        Set<String> dirs = new HashSet<String>();
        if (!options.isQuiet()) log.info("Constructing DXP package...");
        for (BosMember member : mapBos.getMembers()) {
            if (!options.isQuiet()) log.info("Adding member " + member + " to zip...");
            URI relativeUri = baseUri.relativize(member.getEffectiveUri());
            File temp = new File(relativeUri.getPath());
            String parentPath = temp.getParent();
            if (parentPath != null && !"".equals(parentPath) && !parentPath.endsWith("/")) {
                parentPath += "/";
            }
            log.debug("parentPath=\"" + parentPath + "\"");
            if (!"".equals(parentPath) && parentPath != null && !dirs.contains(parentPath)) {
                entry = new ZipEntry(parentPath);
                zipOutStream.putNextEntry(entry);
                zipOutStream.closeEntry();
                dirs.add(parentPath);
            }
            entry = new ZipEntry(relativeUri.getPath());
            zipOutStream.putNextEntry(entry);
            IOUtils.copy(member.getInputStream(), zipOutStream);
            zipOutStream.closeEntry();
        }
        zipOutStream.close();
        if (!options.isQuiet()) log.info("DXP package \"" + outputZipFile.getAbsolutePath() + "\" created.");
    }

    /**
	 * @param zipFile
	 * @param dxpOptions 
	 * @return
	 * @throws DitaDxpException 
	 */
    public static ZipEntry getDxpPackageRootMap(ZipFile zipFile, MapBosProcessorOptions dxpOptions) throws DitaDxpException {
        List<ZipEntry> candidateRootEntries = new ArrayList<ZipEntry>();
        List<ZipEntry> candidateDirs = new ArrayList<ZipEntry>();
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            File temp = new File(entry.getName());
            String parentPath = temp.getParent();
            if (entry.isDirectory()) {
                if (parentPath == null || "".equals(parentPath)) {
                    candidateDirs.add(entry);
                }
            } else {
                if (entry.getName().equals("dita_dxp_manifest.ditamap")) {
                    return entry;
                }
                if (entry.getName().endsWith(".ditamap")) {
                    if (parentPath == null || "".equals(parentPath)) {
                        candidateRootEntries.add(entry);
                    }
                }
            }
        }
        if (candidateRootEntries.size() == 1) {
            if (!dxpOptions.isQuiet()) log.info("Using root map " + candidateRootEntries.get(0).getName());
            return candidateRootEntries.get(0);
        }
        if (candidateRootEntries.size() == 0 & candidateDirs.size() > 1) {
            throw new DitaDxpException("No manifest map, no map in root of package, and more than one top-level directory in package.");
        }
        if (candidateDirs.size() == 1) {
            String parentPath = candidateDirs.get(0).getName();
            entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                File temp = new File(entry.getName());
                String entryParent = temp.getParent();
                if (entryParent == null) entryParent = "/"; else entryParent += "/";
                if (parentPath.equals(entryParent) && entry.getName().endsWith(".ditamap")) {
                    candidateRootEntries.add(entry);
                }
            }
            if (candidateRootEntries.size() == 1) {
                if (!dxpOptions.isQuiet()) log.info("Using root map " + candidateRootEntries.get(0).getName());
                return candidateRootEntries.get(0);
            }
            if (candidateRootEntries.size() > 1) {
                throw new DitaDxpException("No manifest map and found more than one map in the root directory of the package.");
            }
        }
        throw new DitaDxpException("Unable to find package manifest map or single root map in DXP package.");
    }

    /**
	 * @param dxpFile
	 * @param outputDir
	 * @param dxpOptions
	 * @throws Exception 
	 */
    public static void unpackDxpPackage(File dxpFile, File outputDir, DitaDxpOptions dxpOptions) throws Exception {
        unpackDxpPackage(dxpFile, outputDir, dxpOptions, log);
    }

    /**
	 * @param dxpFile
	 * @param outputDir
	 * @param dxpOptions
	 * @throws Exception 
	 */
    public static void unpackDxpPackage(File dxpFile, File outputDir, DitaDxpOptions dxpOptions, Log log) throws Exception {
        ZipFile zipFile = new ZipFile(dxpFile);
        ZipEntry rootMapEntry = getDxpPackageRootMap(zipFile, dxpOptions);
        if (dxpOptions.isUnzipAll()) {
            MultithreadedUnzippingController controller = new MultithreadedUnzippingController(dxpOptions);
            if (!dxpOptions.isQuiet()) log.info("Unzipping entire DXP package \"" + dxpFile.getAbsolutePath() + "\" to output directory \"" + outputDir + "\"...");
            controller.unzip(dxpFile, outputDir, true);
            if (!dxpOptions.isQuiet()) log.info("Unzip complete");
        } else {
            List<String> mapIds = dxpOptions.getRootMaps();
            List<ZipEntry> mapEntries = new ArrayList<ZipEntry>();
            if (mapIds.size() == 0) {
                mapEntries.add(rootMapEntry);
            } else {
                mapEntries = getMapEntries(zipFile, mapIds);
            }
            for (ZipEntry mapEntry : mapEntries) {
                extractMap(zipFile, mapEntry, outputDir, dxpOptions);
            }
        }
    }

    /**
	 * Extracts only the local dependencies used from a map from a DXP package.
	 * @param zipFile
	 * @param mapEntry
	 * @param outputDir
	 * @param dxpOptions
	 * @throws Exception 
	 */
    private static void extractMap(ZipFile zipFile, ZipEntry mapEntry, File outputDir, MapBosProcessorOptions dxpOptions) throws Exception {
        Map<URI, Document> domCache = new HashMap<URI, Document>();
        if (!dxpOptions.isQuiet()) log.info("Extracting map " + mapEntry.getName() + "...");
        BosConstructionOptions bosOptions = new BosConstructionOptions(log, domCache);
        InputSource source = new InputSource(zipFile.getInputStream(mapEntry));
        File dxpFile = new File(zipFile.getName());
        URL baseUri = new URL("jar:" + dxpFile.toURI().toURL().toExternalForm() + "!/");
        URL mapUrl = new URL(baseUri, mapEntry.getName());
        source.setSystemId(mapUrl.toExternalForm());
        Document rootMap = DomUtil.getDomForSource(source, bosOptions, false);
        DitaBoundedObjectSet mapBos = DitaBosHelper.calculateMapBos(bosOptions, log, rootMap);
        MapCopyingBosVisitor visitor = new MapCopyingBosVisitor(outputDir);
        visitor.visit(mapBos);
        if (!dxpOptions.isQuiet()) log.info("Map extracted.");
    }

    /**
	 * Uses the map IDs and the DXP manifest map to get the
	 * Zip entries for the specified maps.
	 * @param zipFile DXP package file.
	 * @param mapIds List of IDs, as specified in a DXP manifest, of maps to process.
	 * @return
	 */
    public static List<ZipEntry> getMapEntries(ZipFile zipFile, List<String> mapIds) throws DitaDxpException {
        throw new NotImplementedException();
    }
}
