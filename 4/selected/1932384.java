package org.translationcomponent.service.notranslation;

import org.apache.commons.io.IOUtils;
import org.translationcomponent.api.Document;
import org.translationcomponent.api.TranslationRequest;
import org.translationcomponent.api.TranslationResponse;
import org.translationcomponent.api.impl.response.ResponseStateException;
import org.translationcomponent.api.impl.response.ResponseStateOk;
import org.translationcomponent.api.impl.translator.TranslatorServiceAbstract;

/**
 * 
 * @author ROB
 * 
 */
public class ForwardService extends TranslatorServiceAbstract {

    public void serviceDocument(final TranslationRequest request, final TranslationResponse response, final Document document) throws Exception {
        response.addHeaders(document.getResponseHeaders());
        try {
            IOUtils.copy(document.getInputStream(), response.getOutputStream());
            response.setEndState(ResponseStateOk.getInstance());
        } catch (Exception e) {
            response.setEndState(new ResponseStateException(e));
            log.warn("Error parsing XML of " + document, e);
        }
    }
}
