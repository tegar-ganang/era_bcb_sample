package org.wsmostudio.integration.ssb.swsrepository;

import java.util.Set;
import org.apache.axis2.databinding.types.URI;
import org.apache.axis2.databinding.types.URI.MalformedURIException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.ip_super.api.ssb.swsrepository.SWSRepositoryStub;
import org.ip_super.api.ssb.swsrepository.SWSRepositoryStub.*;
import org.wsmo.common.IRI;
import org.wsmo.common.TopEntity;
import org.wsmo.common.exception.SynchronisationException;
import org.wsmo.wsml.Parser;
import org.wsmo.wsml.Serializer;
import org.wsmostudio.runtime.LogManager;
import org.wsmostudio.runtime.WSMORuntime;

public class Helper {

    public static void storeEntity(TopEntity entity, SWSRepositoryStub _repository, Set<IRI> localCache) {
        if (localCache.contains(entity.getIdentifier()) && false == MessageDialog.openConfirm(Display.getCurrent().getActiveShell(), "Entity Overwrite", "Entity '" + entity.getIdentifier().toString() + "' already exists in the repository." + "\n\nPlease confirm overwriting it?")) {
            return;
        }
        Serializer wsmlPrinter = WSMORuntime.getRuntime().getWsmlSerializer();
        StringBuffer wsmoStringContent = new StringBuffer();
        wsmlPrinter.serialize(new TopEntity[] { entity }, wsmoStringContent);
        WSMLDocument strWrapper = new SWSRepositoryStub.WSMLDocument();
        strWrapper.setDocumentContent(wsmoStringContent.toString());
        StoreEntity sendParam = new SWSRepositoryStub.StoreEntity();
        sendParam.setTheEntity(strWrapper);
        try {
            StoreEntityResponse response = _repository.storeEntity(sendParam);
            if (false == response.getOut()) {
                throw new Exception("Storage failure of " + entity.getIdentifier());
            }
            localCache.add((IRI) entity.getIdentifier());
        } catch (Exception ex) {
            LogManager.logError(ex);
            throw new SynchronisationException(ex.getMessage());
        }
    }

    public static void deleteEntity(TopEntity entity, SWSRepositoryStub _repository, Set<IRI> localCache) {
        Serializer wsmlPrinter = WSMORuntime.getRuntime().getWsmlSerializer();
        StringBuffer wsmoStringContent = new StringBuffer();
        wsmlPrinter.serialize(new TopEntity[] { entity }, wsmoStringContent);
        WSMLDocument strWrapper = new SWSRepositoryStub.WSMLDocument();
        strWrapper.setDocumentContent(wsmoStringContent.toString());
        RemoveEntity removeParam = new SWSRepositoryStub.RemoveEntity();
        removeParam.setTheEntity(strWrapper);
        try {
            RemoveEntityResponse response = _repository.removeEntity(removeParam);
            if (false == response.getOut()) {
                throw new Exception("Removal failure of " + entity.getIdentifier());
            }
            localCache.remove((IRI) entity.getIdentifier());
        } catch (Exception ex) {
            LogManager.logError(ex);
            throw new SynchronisationException(ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static TopEntity retrieveEntity(IRI entity, SWSRepositoryStub _repository, Class type) throws SynchronisationException {
        RetrieveByIdentifier getEntityParam = new SWSRepositoryStub.RetrieveByIdentifier();
        try {
            getEntityParam.setIdentifier(new URI(entity.toString()));
        } catch (MalformedURIException mue) {
            LogManager.logError(mue);
            throw new SynchronisationException(mue);
        }
        try {
            RetrieveByIdentifierResponse response = _repository.retrieveByIdentifier(getEntityParam);
            if (response == null || response.getOut() == null || response.getOut().getDocumentContent() == null) {
                throw new Exception("No content received from repository for identifier: " + entity.toString());
            }
            Parser wsmlParser = WSMORuntime.getRuntime().getWsmlParser();
            TopEntity[] entities = wsmlParser.parse(new StringBuffer(response.getOut().getDocumentContent()));
            if (entities == null || entities.length == 0) {
                throw new Exception("No content received from repository for identifier: " + entity.toString());
            }
            if (false == type.isInstance(entities[0])) {
                throw new Exception("Invalid content received from repository for identifier: " + entity.toString() + " expected " + type.getSimpleName());
            }
            return entities[0];
        } catch (Exception ex) {
            LogManager.logError(ex);
            throw new SynchronisationException(ex.getMessage());
        }
    }

    public static byte detectContentType(IRI ref, SWSRepositoryStub repository) throws Exception {
        RetrieveByIdentifier idParam = new SWSRepositoryStub.RetrieveByIdentifier();
        idParam.setIdentifier(new URI(ref.toString()));
        RetrieveByIdentifierResponse result = repository.retrieveByIdentifier(idParam);
        if (result == null || result.getOut() == null) {
            throw new Exception("Unable to fetch content of : " + ref.toString());
        }
        String content = result.getOut().getDocumentContent();
        byte resultType = -1;
        int pos = indexOfKeyword(content, "ontology");
        if (pos != -1) {
            resultType = SWSRepository.TYPE_ONTO;
        }
        int pos2 = indexOfKeyword(content, "goal");
        if (pos2 != -1 && pos != -1 && pos2 < pos) {
            resultType = SWSRepository.TYPE_GOAL;
            pos = pos2;
        }
        pos2 = indexOfKeyword(content, "webservice");
        if (pos2 != -1 && pos != -1 && pos2 < pos) {
            resultType = SWSRepository.TYPE_WS;
            pos = pos2;
        }
        pos2 = indexOfKeyword(content, "ooMediator");
        if (pos2 != -1 && pos != -1 && pos2 < pos) {
            resultType = SWSRepository.TYPE_MED;
            pos = pos2;
        }
        pos2 = indexOfKeyword(content, "ggMediator");
        if (pos2 != -1 && pos != -1 && pos2 < pos) {
            resultType = SWSRepository.TYPE_MED;
            pos = pos2;
        }
        pos2 = indexOfKeyword(content, "wgMediator");
        if (pos2 != -1 && pos != -1 && pos2 < pos) {
            resultType = SWSRepository.TYPE_MED;
            pos = pos2;
        }
        pos2 = indexOfKeyword(content, "wwMediator");
        if (pos2 != -1 && pos != -1 && pos2 < pos) {
            resultType = SWSRepository.TYPE_MED;
            pos = pos2;
        }
        return resultType;
    }

    public static int indexOfKeyword(String data, String keyword) {
        int i = data.indexOf(keyword);
        while (i != -1 && i > 0 && false == Character.isWhitespace(data.charAt(i - 1)) && i + keyword.length() < data.length() && false == Character.isWhitespace(data.charAt(i + keyword.length()))) {
            i = data.indexOf(keyword, i + 1);
        }
        return i;
    }
}
