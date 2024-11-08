package org.translationcomponent.api.impl.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.io.IOUtils;
import org.translationcomponent.api.ResponseCode;
import org.translationcomponent.api.TranslationRequest;
import org.translationcomponent.api.TranslationResponse;
import org.translationcomponent.api.TranslatorService;
import org.translationcomponent.api.impl.response.ResponseStateBean;
import org.translationcomponent.api.impl.response.ResponseStateException;

public class MockTranslator implements TranslatorService {

    private Collection<String> ignoreParameters;

    private Map<String, RequestResponse> cache = new HashMap<String, RequestResponse>();

    private AtomicLong hits = new AtomicLong();

    public void service(TranslationRequest request, TranslationResponse response) {
        try {
            Thread.sleep((long) Math.random() * 250);
        } catch (InterruptedException e1) {
        }
        hits.incrementAndGet();
        String key = getKey(request);
        RequestResponse cachedResponse = cache.get(key);
        if (cachedResponse == null) {
            response.setEndState(new ResponseStateBean(ResponseCode.ERROR, "response not found for " + key));
            return;
        }
        response.addHeaders(cachedResponse.getExpectedResponse().getHeaders());
        response.setTranslationCount(cachedResponse.getExpectedResponse().getTranslationCount());
        response.setFailCount(cachedResponse.getExpectedResponse().getFailCount());
        if (cachedResponse.getExpectedResponse().getLastModified() != -1) {
            response.setLastModified(cachedResponse.getExpectedResponse().getLastModified());
        }
        try {
            OutputStream output = response.getOutputStream();
            InputStream input = cachedResponse.getExpectedResponse().getInputStream();
            try {
                IOUtils.copy(input, output);
            } finally {
                IOUtils.closeQuietly(input);
                IOUtils.closeQuietly(output);
            }
        } catch (IOException e) {
            response.setEndState(new ResponseStateException(e));
            return;
        }
        response.setEndState(cachedResponse.getExpectedResponse().getEndState());
    }

    public String getKey(TranslationRequest request) {
        StringBuilder key = new StringBuilder();
        key.append(request.getRelativeUrl());
        key.append("_");
        key.append(request.getQueryString(ignoreParameters));
        key.append("_");
        key.append(request.getClientName());
        key.append("_");
        key.append(request.getHostLanguageCode());
        key.append("_");
        key.append(request.getTargetLanguageCode());
        String s = request.getText();
        if (s != null) {
            key.append("_");
            key.append(s.hashCode() + s.substring(0, Math.min(100, s.length())));
        }
        return key.toString();
    }

    public Collection<String> getIgnoreParameters() {
        return ignoreParameters;
    }

    public void setIgnoreParameters(Collection<String> ignoreParameters) {
        this.ignoreParameters = ignoreParameters;
    }

    public void addRequestResponse(RequestResponse reqresp) {
        if (!cache.containsKey(getKey(reqresp.getRequest()))) {
            cache.put(getKey(reqresp.getRequest()), reqresp);
        }
    }

    public int getHits() {
        return hits.intValue();
    }

    public TranslatorService getChainedTranslator() {
        return null;
    }
}
