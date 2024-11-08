package com.germinus.xpression.groupware;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.RepositoryException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.germinus.xpression.cms.jcr.JCRUtil;
import com.germinus.xpression.cms.web.TemporaryFilesHandler;
import com.germinus.xpression.groupware.CommunityManager.ImportCommunityResult;
import com.germinus.xpression.groupware.communities.Community;
import com.germinus.xpression.groupware.communities.CommunityNotActiveException;
import com.germinus.xpression.groupware.communities.CommunityNotFoundException;
import com.germinus.xpression.groupware.communities.CommunityPersister;
import com.germinus.xpression.i18n.I18NUtils;

public class CommunityMigrator {

    private CommunityPersister communityPersister;

    private static final Log log = LogFactory.getLog(CommunityMigrator.class);

    private static final String MANUAL_EXPORTED_COMMUNITY_PREFIX = "COMMUNITY_";

    public CommunityMigrator(CommunityPersister communityPersister) {
        super();
        this.communityPersister = communityPersister;
    }

    File exportCommunityData(Community community) throws CommunityNotActiveException, FileNotFoundException, IOException, CommunityNotFoundException {
        try {
            String communityId = community.getId();
            if (!community.isActive()) {
                log.error("The community with id " + communityId + " is inactive");
                throw new CommunityNotActiveException("The community with id " + communityId + " is inactive");
            }
            new File(CommunityManagerImpl.EXPORTED_COMMUNITIES_PATH).mkdirs();
            String communityName = community.getName();
            String communityType = community.getType();
            String communityTitle = I18NUtils.localize(community.getTitle());
            File zipOutFilename;
            if (community.isPersonalCommunity()) {
                zipOutFilename = new File(CommunityManagerImpl.EXPORTED_COMMUNITIES_PATH + communityName + ".zip");
            } else {
                zipOutFilename = new File(CommunityManagerImpl.EXPORTED_COMMUNITIES_PATH + MANUAL_EXPORTED_COMMUNITY_PREFIX + communityTitle + ".zip");
            }
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipOutFilename));
            File file = File.createTempFile("exported-community", null);
            TemporaryFilesHandler.register(null, file);
            FileOutputStream fos = new FileOutputStream(file);
            String contentPath = JCRUtil.getNodeById(communityId).getPath();
            JCRUtil.currentSession().exportSystemView(contentPath, fos, false, false);
            fos.close();
            File propertiesFile = File.createTempFile("exported-community-properties", null);
            TemporaryFilesHandler.register(null, propertiesFile);
            FileOutputStream fosProperties = new FileOutputStream(propertiesFile);
            fosProperties.write(("communityId=" + communityId).getBytes());
            fosProperties.write(";".getBytes());
            fosProperties.write(("externalId=" + community.getExternalId()).getBytes());
            fosProperties.write(";".getBytes());
            fosProperties.write(("title=" + communityTitle).getBytes());
            fosProperties.write(";".getBytes());
            fosProperties.write(("communityType=" + communityType).getBytes());
            fosProperties.write(";".getBytes());
            fosProperties.write(("communityName=" + communityName).getBytes());
            fosProperties.close();
            FileInputStream finProperties = new FileInputStream(propertiesFile);
            byte[] bufferProperties = new byte[4096];
            out.putNextEntry(new ZipEntry("properties"));
            int readProperties = 0;
            while ((readProperties = finProperties.read(bufferProperties)) > 0) {
                out.write(bufferProperties, 0, readProperties);
            }
            finProperties.close();
            FileInputStream fin = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            out.putNextEntry(new ZipEntry("xmlData"));
            int read = 0;
            while ((read = fin.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            fin.close();
            out.close();
            community.setActive(Boolean.FALSE);
            communityPersister.saveCommunity(community);
            Collection<Community> duplicatedPersonalCommunities = communityPersister.searchCommunitiesByName(communityName);
            if (CommunityManager.PERSONAL_COMMUNITY_TYPE.equals(communityType)) {
                for (Community currentCommunity : duplicatedPersonalCommunities) {
                    if (currentCommunity.isActive()) {
                        currentCommunity.setActive(Boolean.FALSE);
                        communityPersister.saveCommunity(currentCommunity);
                    }
                }
            }
            return zipOutFilename;
        } catch (RepositoryException e) {
            log.error("Error getting community with id " + community.getId());
            throw new GroupwareRuntimeException("Error getting community with id " + community.getId(), e.getCause());
        }
    }

    byte[] exportCommunityDataAndDownloadZip(Community community) throws CommunityNotFoundException, IOException, CommunityNotActiveException {
        File zipOutFilename = exportCommunityData(community);
        return serveGeneratedFile(zipOutFilename);
    }

    byte[] serveGeneratedFile(File zipOutFilename) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FileInputStream fisZipped = new FileInputStream(zipOutFilename);
        byte[] bufferOut = new byte[4096];
        int readOut = 0;
        while ((readOut = fisZipped.read(bufferOut)) > 0) {
            baos.write(bufferOut, 0, readOut);
        }
        return baos.toByteArray();
    }

    void rollbackCommunityMigration(Community community) {
        community.setActive(Boolean.TRUE);
        communityPersister.saveCommunity(community);
    }

    ImportCommunityResult doImportCommunityResult(String parentNodePath, ZipInputStream zis) throws IOException {
        ZipEntry entry;
        String propertiesString = "";
        ByteArrayOutputStream xmlStream = new ByteArrayOutputStream();
        while ((entry = zis.getNextEntry()) != null) {
            if (entry.getName().equals("xmlData")) {
                int byteCount;
                byte[] data = new byte[512];
                while ((byteCount = zis.read(data, 0, 512)) != -1) {
                    xmlStream.write(data, 0, byteCount);
                }
                xmlStream.close();
            } else if (entry.getName().equals("properties")) {
                ByteArrayOutputStream targetStream = new ByteArrayOutputStream();
                int byteCount;
                byte[] data = new byte[512];
                while ((byteCount = zis.read(data, 0, 512)) != -1) {
                    targetStream.write(data, 0, byteCount);
                }
                targetStream.close();
                propertiesString = new String(targetStream.toByteArray());
            }
        }
        try {
            JCRUtil.currentSession().importXML(parentNodePath, new ByteArrayInputStream(xmlStream.toByteArray()), ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
            JCRUtil.currentSession().save();
        } catch (RepositoryException e) {
            throw new GroupwareRuntimeException("Error importing xml", e.getCause());
        }
        return ImportCommunityResult.noError;
    }
}
