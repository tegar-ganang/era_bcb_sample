package com.software416.jsimplebrowser.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.io.IOUtils;
import com.google.inject.Singleton;
import com.software416.jsimplebrowser.HttpResponse;
import com.software416.jsimplebrowser.RequestService;
import com.software416.jsimplebrowser.event.RequestEvent;
import com.software416.jsimplebrowser.event.RequestEventListener;

/**
 * @author Rob Di Marco
 */
@Singleton
public class RequestServiceImpl implements RequestService {

    private static final int REQUEST_POOL_THREAD_COUNT = 5;

    HttpClient _client = new HttpClient();

    private List<RequestEventListener> _eventListeners = new ArrayList<RequestEventListener>();

    private AtomicInteger _requestIdCounter = new AtomicInteger(1);

    private ExecutorService _requestPool = new ThreadPoolExecutor(1, REQUEST_POOL_THREAD_COUNT, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());

    public String makeAsyncRequest(final HttpMethod m) {
        final String requestId = String.valueOf(_requestIdCounter.addAndGet(1));
        _requestPool.execute(new Runnable() {

            public void run() {
                makeRequest(m, requestId);
            }
        });
        return requestId;
    }

    public HttpResponse makeSynchronousRequest(HttpMethod hm) {
        final String requestId = String.valueOf(_requestIdCounter.addAndGet(1));
        return makeRequest(hm, requestId);
    }

    protected HttpResponseImpl makeRequest(final HttpMethod m, final String requestId) {
        try {
            HttpResponseImpl ri = new HttpResponseImpl();
            ri.setRequestMethod(m);
            ri.setResponseCode(_client.executeMethod(m));
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            IOUtils.copy(m.getResponseBodyAsStream(), bos);
            ri.setResponseBody(bos.toByteArray());
            notifyOfRequestSuccess(requestId, m, ri);
            return ri;
        } catch (HttpException ex) {
            notifyOfRequestFailure(requestId, m, ex);
        } catch (IOException ex) {
            notifyOfRequestFailure(requestId, m, ex);
        }
        return null;
    }

    protected void notifyOfRequestSuccess(String requestId, HttpMethod request, HttpResponseImpl response) {
        RequestSuccessEvent e = new RequestSuccessEvent();
        e.setRequestId(requestId);
        e.setRequest(request);
        e.setResponse(response);
        notifyListenersOfRequestEvent(e);
    }

    protected void notifyOfRequestFailure(String requestId, HttpMethod method, Throwable t) {
        RequestFailureEvent e = new RequestFailureEvent();
        e.setRequestId(requestId);
        e.setRequest(method);
        e.setCause(t);
        notifyListenersOfRequestEvent(e);
    }

    protected void notifyListenersOfRequestEvent(RequestEvent e) {
        for (RequestEventListener listener : _eventListeners) {
            listener.handleRequestEvent(e);
        }
    }

    public void addRequestEventListener(RequestEventListener listener) {
        _eventListeners.add(listener);
    }

    public void removeRequestEventListener(RequestEventListener listener) {
        _eventListeners.remove(listener);
    }
}
