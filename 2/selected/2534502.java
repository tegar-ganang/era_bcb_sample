package net.yama.android.managers.connection;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;
import net.oauth.OAuthServiceProvider;
import net.oauth.client.OAuthClient;
import net.oauth.client.OAuthClient.ParameterStyle;
import net.oauth.client.httpclient4.HttpClient4;
import net.yama.android.managers.config.ConfigurationManager;
import net.yama.android.requests.AbstractRequest;
import net.yama.android.requests.write.WritePhotoRequest;
import net.yama.android.util.Constants;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.RJSONTokener;
import android.util.Log;

/**
 * Make requests using OAuth parameters
 * 
 * @author Rohit Kumbhar
 */
public class OAuthConnectionManager implements ConnectionManager {

    public static final OAuthServiceProvider provider = new OAuthServiceProvider(Constants.OAUTH_REQUEST_URL, Constants.OAUTH_AUTHORIZE_URL, Constants.OAUTH_ACCESS_URL);

    public static final OAuthConsumer consumer = new OAuthConsumer(Constants.CALLBACK_URL, Constants.OAUTH_CONSUMER_KEY, Constants.OAUTH_CONSUMER_SECRET, provider);

    /**
	 * Makes a generic request.
	 */
    public RJSONTokener makeRequest(AbstractRequest request) throws ApplicationException {
        RJSONTokener tokener = null;
        OAuthAccessor accessor = new OAuthAccessor(OAuthConnectionManager.consumer);
        accessor.accessToken = ConfigurationManager.instance.getAccessToken();
        accessor.tokenSecret = ConfigurationManager.instance.getAccessTokenSecret();
        accessor.consumer.setProperty(OAuthClient.PARAMETER_STYLE, ParameterStyle.QUERY_STRING);
        ArrayList<Map.Entry<String, String>> params = new ArrayList<Map.Entry<String, String>>();
        convertRequestParamsToOAuth(params, request.getParameterMap());
        OAuthClient oAuthClient = new OAuthClient(new HttpClient4());
        OAuthMessage authMessage;
        try {
            authMessage = oAuthClient.invoke(accessor, request.getMethod(), request.getRequestURL(), params);
            InputStream bodyStream = authMessage.getBodyAsStream();
            tokener = new RJSONTokener(bodyStream);
        } catch (Exception e) {
            Log.e("OAuthConnectionManager", "Exception in makeRequest()", e);
            throw new ApplicationException(e);
        }
        return tokener;
    }

    /**
	 * Converts all the parameters in the request to OAuth.Parameter.
	 * 
	 * @param params
	 * @param parameterMap
	 */
    private void convertRequestParamsToOAuth(ArrayList<Entry<String, String>> params, Map<String, String> parameterMap) {
        Set<Entry<String, String>> entries = parameterMap.entrySet();
        Iterator<Entry<String, String>> entryIter = entries.iterator();
        while (entryIter.hasNext()) {
            Map.Entry<java.lang.String, java.lang.String> entry = (Map.Entry<java.lang.String, java.lang.String>) entryIter.next();
            params.add(new OAuth.Parameter(entry.getKey(), entry.getValue()));
        }
    }

    /**
	 * Special request for photo upload since OAuth doesn't handle
	 * multipart/form-data
	 */
    public String uploadPhoto(WritePhotoRequest request) throws ApplicationException {
        String responseString = null;
        try {
            OAuthAccessor accessor = new OAuthAccessor(OAuthConnectionManager.consumer);
            accessor.accessToken = ConfigurationManager.instance.getAccessToken();
            accessor.tokenSecret = ConfigurationManager.instance.getAccessTokenSecret();
            String tempImagePath = request.getParameterMap().remove(Constants.TEMP_IMAGE_FILE_PATH);
            String eventId = request.getParameterMap().remove(Constants.EVENT_ID_KEY);
            ArrayList<Map.Entry<String, String>> params = new ArrayList<Map.Entry<String, String>>();
            convertRequestParamsToOAuth(params, request.getParameterMap());
            OAuthMessage message = new OAuthMessage(request.getMethod(), request.getRequestURL(), params);
            message.addRequiredParameters(accessor);
            List<Map.Entry<String, String>> oAuthParams = message.getParameters();
            String url = OAuth.addParameters(request.getRequestURL(), oAuthParams);
            HttpPost post = new HttpPost(url);
            File photoFile = new File(tempImagePath);
            FileBody photoContentBody = new FileBody(photoFile);
            StringBody eventIdBody = new StringBody(eventId);
            HttpClient client = new DefaultHttpClient();
            MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.STRICT);
            reqEntity.addPart(Constants.PHOTO, photoContentBody);
            reqEntity.addPart(Constants.EVENT_ID_KEY, eventIdBody);
            post.setEntity(reqEntity);
            HttpResponse response = client.execute(post);
            HttpEntity resEntity = response.getEntity();
            responseString = EntityUtils.toString(resEntity);
        } catch (Exception e) {
            Log.e("OAuthConnectionManager", "Exception in uploadPhoto()", e);
            throw new ApplicationException(e);
        }
        return responseString;
    }
}
