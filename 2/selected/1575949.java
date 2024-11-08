package com.googlecode.batchfb.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import com.google.appengine.api.urlfetch.HTTPHeader;
import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;
import com.google.apphosting.api.ApiProxy.ApiDeadlineExceededException;
import com.googlecode.batchfb.err.IOFacebookException;
import com.googlecode.batchfb.util.RequestBuilder.HttpMethod;
import com.googlecode.batchfb.util.RequestBuilder.HttpResponse;

/**
 * <p>Uses GAE's URLFetch service.  Supports parallel fetching!</p>
 * 
 * @author Jeff Schnitzer
 */
public class AppengineRequestExecutor extends RequestExecutor {

    /** */
    private static final Logger log = Logger.getLogger(AppengineRequestExecutor.class.getName());

    /** */
    private class Request implements RequestDefinition {

        /** */
        private HTTPRequest gaeRequest;

        /** If oen of these exists at time of execution, use it as payload */
        private ByteArrayOutputStream payload;

        @Override
        public void init(HttpMethod meth, String url) throws IOException {
            this.gaeRequest = new HTTPRequest(new URL(url), HTTPMethod.valueOf(meth.name()));
        }

        @Override
        public void setHeader(String name, String value) {
            this.gaeRequest.setHeader(new HTTPHeader(name, value));
        }

        @Override
        public OutputStream getContentOutputStream() throws IOException {
            this.payload = new ByteArrayOutputStream(16384);
            return this.payload;
        }

        @Override
        public void setContent(byte[] content) throws IOException {
            this.gaeRequest.setPayload(content);
        }

        @Override
        public void setTimeout(int millis) {
            this.gaeRequest.getFetchOptions().setDeadline(millis / 1000.0);
        }

        public HTTPRequest getRequest() throws IOException {
            if (this.payload != null) this.setContent(this.payload.toByteArray());
            return this.gaeRequest;
        }
    }

    /**
	 * The appengine version of an HttpResponse, which hides the asynchrony and the retry mechanism.
	 */
    private class Response implements HttpResponse {

        /** Number of retries to execute */
        int retries;

        /** */
        HTTPRequest request;

        /** */
        Future<HTTPResponse> futureResponse;

        /** */
        public Response(int retries, HTTPRequest req) {
            this.retries = retries;
            this.request = req;
            this.futureResponse = URLFetchServiceFactory.getURLFetchService().fetchAsync(this.request);
        }

        @Override
        public int getResponseCode() throws IOException {
            return this.getResponse().getResponseCode();
        }

        @Override
        public InputStream getContentStream() throws IOException {
            return new ByteArrayInputStream(this.getResponse().getContent());
        }

        /** */
        private HTTPResponse getResponse() throws IOException {
            try {
                return this.futureResponse.get();
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            } catch (ExecutionException ex) {
                if (ex.getCause() instanceof IOException) throw (IOException) ex.getCause(); else if (ex.getCause() instanceof RuntimeException) throw (RuntimeException) ex.getCause(); else throw new UndeclaredThrowableException(ex);
            } catch (ApiDeadlineExceededException ex) {
                if (this.retries == 0) throw new IOFacebookException(ex); else {
                    log.warning("URLFetch timed out, retrying: " + ex.toString());
                    return new Response(this.retries - 1, this.request).getResponse();
                }
            }
        }
    }

    /** */
    @Override
    public HttpResponse execute(int retries, RequestSetup setup) throws IOException {
        Request req = new Request();
        setup.setup(req);
        HTTPRequest request = req.getRequest();
        return new Response(retries, request);
    }
}
