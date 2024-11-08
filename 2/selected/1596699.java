package org.encuestame.oauth2.support;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import org.encuestame.utils.oauth.OAuth2Version;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;

/**
 * OAuth2RequestFactory
 * Request factory that signs RestTemplate requests with an OAuth 2 Authorization header.
 * @author Picado, Juan juanATencuestame.org
 * @since
 */
class OAuth2RequestFactory implements ClientHttpRequestFactory {

    private final ClientHttpRequestFactory delegate;

    private final String accessToken;

    private final OAuth2Version oauth2Version;

    public OAuth2RequestFactory(ClientHttpRequestFactory delegate, String accessToken, OAuth2Version oauth2Version) {
        this.delegate = delegate;
        this.accessToken = accessToken;
        this.oauth2Version = oauth2Version;
    }

    public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
        return new OAuth2SigningRequest(delegate.createRequest(uri, httpMethod), accessToken, oauth2Version);
    }

    private static class OAuth2SigningRequest implements ClientHttpRequest {

        private final ClientHttpRequest delegate;

        private ByteArrayOutputStream bodyOutputStream;

        private final String accessToken;

        private final OAuth2Version oauth2Version;

        public OAuth2SigningRequest(ClientHttpRequest delegate, String accessToken, OAuth2Version oauth2Version) {
            this.delegate = delegate;
            this.accessToken = accessToken;
            this.oauth2Version = oauth2Version;
            this.bodyOutputStream = new ByteArrayOutputStream();
        }

        public ClientHttpResponse execute() throws IOException {
            byte[] bufferedOutput = bodyOutputStream.toByteArray();
            delegate.getBody().write(bufferedOutput);
            delegate.getHeaders().set("Authorization", oauth2Version.getAuthorizationHeaderValue(accessToken));
            return delegate.execute();
        }

        public URI getURI() {
            return delegate.getURI();
        }

        public HttpMethod getMethod() {
            return delegate.getMethod();
        }

        public HttpHeaders getHeaders() {
            return delegate.getHeaders();
        }

        public OutputStream getBody() throws IOException {
            return bodyOutputStream;
        }
    }
}
