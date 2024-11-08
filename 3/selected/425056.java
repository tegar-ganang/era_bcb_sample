package org.zend.webapi.internal.core.connection;

import java.util.ArrayList;
import java.util.List;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.Uniform;
import org.restlet.data.ClientInfo;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Parameter;
import org.restlet.data.Preference;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.engine.http.header.HeaderConstants;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import org.restlet.util.Series;
import org.zend.webapi.core.WebApiException;
import org.zend.webapi.core.connection.data.DataDigster;
import org.zend.webapi.core.connection.dispatch.IServiceDispatcher;
import org.zend.webapi.core.connection.request.IRequest;
import org.zend.webapi.core.connection.request.RequestParameter;
import org.zend.webapi.core.connection.response.IResponse;
import org.zend.webapi.core.connection.response.ResponseFactory;
import org.zend.webapi.internal.core.connection.auth.signature.SignatureException;
import org.zend.webapi.internal.core.connection.exception.InternalWebApiException;
import org.zend.webapi.internal.core.connection.exception.InvalidResponseException;
import org.zend.webapi.internal.core.connection.exception.UnexpectedResponseCode;
import org.zend.webapi.internal.core.connection.exception.WebApiCommunicationError;
import org.zend.webapi.internal.core.connection.request.HeaderParameters;

/**
 * WebApi Service Dispatcher
 * 
 * @author Roy, 2011
 * 
 */
public class ServiceDispatcher implements IServiceDispatcher {

    private Context context;

    public ServiceDispatcher() {
        this(null);
    }

    public ServiceDispatcher(Context context) {
        this.context = context;
    }

    public IResponse dispatch(IRequest request) throws WebApiException {
        if (request == null) {
            throw new IllegalArgumentException("Error request == null");
        }
        IResponse response = null;
        try {
            ClientResource resource = getResource(request);
            final Representation handle = resource.handle();
            if (handle == null) {
                throw new WebApiCommunicationError();
            }
            final DataDigster dataDigster = new DataDigster(request, handle);
            final Status status = resource.getStatus();
            int responseCode = status.getCode();
            if (!dataDigster.validateResponse()) {
                throw new InvalidResponseException(new Exception("Invalid response media type for XML response"));
            }
            if (!request.isExpectedResponseCode(responseCode)) {
                throw new UnexpectedResponseCode(responseCode, handle);
            }
            dataDigster.digest();
            response = ResponseFactory.createResponse(request, responseCode, dataDigster.getResponseData());
        } catch (ResourceException e) {
            throw new InternalWebApiException(e);
        }
        return response;
    }

    protected ClientResource getResource(IRequest webApiRequest) throws SignatureException {
        final Request request = createRequest(webApiRequest);
        final ClientResource clientResource = new ClientResource(request, new Response(null));
        if (context != null) {
            Uniform uniform = clientResource.getNext();
            if (uniform instanceof Restlet) {
                Restlet restlet = (Restlet) uniform;
                restlet.setContext(context);
            }
        }
        return clientResource;
    }

    /**
	 * Creates the low level request to the Reslet framework
	 * 
	 * @param webApiRequest
	 * @throws SignatureException
	 */
    private Request createRequest(IRequest webApiRequest) throws SignatureException {
        Request request = new Request();
        request.setProtocol(getProtocol(webApiRequest.getHost()));
        final Reference baseRef = new Reference(webApiRequest.getHost());
        final Reference reference = new Reference(baseRef, webApiRequest.getUri());
        request.setResourceRef(reference);
        final Method method = webApiRequest.getMethod();
        request.setMethod(method);
        final ClientInfo clientInfo = new ClientInfo();
        clientInfo.setAgent(webApiRequest.getUserAgent());
        final ArrayList<Preference<MediaType>> arrayList = new ArrayList<Preference<MediaType>>(1);
        final String name = "application/vnd.zend.serverapi+xml;version=" + webApiRequest.getVersion().getVersionName();
        arrayList.add(new Preference<MediaType>(new MediaType(name)));
        clientInfo.setAcceptedMediaTypes(arrayList);
        request.setClientInfo(clientInfo);
        request.setDate(webApiRequest.getDate());
        Series<Parameter> s = new HeaderParameters();
        StringBuilder b = new StringBuilder(webApiRequest.getKeyName());
        b.append("; ");
        b.append(webApiRequest.getSignature());
        s.add("X-Zend-Signature", b.toString());
        request.getAttributes().put(HeaderConstants.ATTRIBUTE_HEADERS, s);
        request.setHostRef(baseRef.getAuthority());
        final List<RequestParameter<?>> parameters = webApiRequest.getParameters();
        if (parameters != null) {
            webApiRequest.applyParameters(request);
        }
        return request;
    }

    private Protocol getProtocol(String host) {
        if (host.startsWith("https")) {
            return Protocol.HTTPS;
        }
        return Protocol.HTTP;
    }
}
