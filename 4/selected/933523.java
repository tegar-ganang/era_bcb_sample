package jp.eisbahn.eclipse.plugins.twitterclipse.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jp.eisbahn.eclipse.plugins.twitterclipse.HttpRequestTimeoutException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.io.IOUtils;

/**
 * {@link HttpClient}���g�p����ۂɕ֗��ȏ�����񋟂��郆�[�e�B���e�B�N���X�ł��B
 * @author Yoichiro Tanaka
 * @since 1.0.0
 */
class HttpClientUtils {

    /** Twitter�x�[�XURL */
    private static final String TWITTER_BASE_URL = "twitter.com";

    /**
	 * �w�肳�ꂽ���\�b�h�����s����HTTP���N�G�X�g�𔭍s���C���̌��ʂ�JSON�I�u�W�F�N�g��Ԃ��܂��B
	 * @param method ���s���郁�\�b�h�I�u�W�F�N�g
	 * @param timeout �\�P�b�g�̃^�C���A�E�g�l(s)
	 * @param array ���ʂ��z�񂩂ǂ���
	 * @return ���ʂ�JSON�I�u�W�F�N�g
	 * @throws HttpRequestFailureException HTTP���N�G�X�g�̌��ʁC�T�[�o����{@link HttpStatus#SC_OK}�ȊO�̒l������ꂽ�Ƃ�
	 * @throws IOException �Ȃ�炩�̓�o�̓G���[�����������Ƃ�
	 * @throws HttpException HTTP�v���g�R����ŉ��炩�̃G���[�����������Ƃ�
	 * @throws HttpRequestTimeoutException �T�[�o����̉�������莞�ԗ��Ȃ������Ƃ�
	 */
    static Object executeMethod(HttpMethod method, int timeout, boolean array) throws HttpRequestFailureException, HttpException, IOException, HttpRequestTimeoutException {
        try {
            method.getParams().setSoTimeout(timeout * 1000);
            int status = -1;
            Object result = null;
            System.out.println("Execute method: " + method.getPath() + " " + method.getQueryString());
            TwitterclipseConfig config = TwitterclipsePlugin.getDefault().getTwitterclipseConfiguration();
            HttpClient httpClient = HttpClientUtils.createHttpClient(TWITTER_BASE_URL, config.getUserId(), config.getPassword());
            status = httpClient.executeMethod(method);
            System.out.println("Received response. status = " + status);
            if (status == HttpStatus.SC_OK) {
                InputStream inputStream = method.getResponseBodyAsStream();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                IOUtils.copy(inputStream, baos);
                String response = new String(baos.toByteArray(), "UTF-8");
                System.out.println(response);
                if (array) result = JSONArray.fromString(response); else result = JSONObject.fromString(response);
            } else {
                throw new HttpRequestFailureException(status);
            }
            return result;
        } catch (SocketTimeoutException e) {
            throw new HttpRequestTimeoutException(e);
        } finally {
            method.releaseConnection();
        }
    }

    /**
	 * {@link PostMethod}�I�u�W�F�N�g�𐶐����ĕԂ��܂��B
	 * format�p�����[�^��"json"�ɃZ�b�g����܂��B
	 * @param uri URI�̑��΃p�X
	 * @param nameValuePairs �p�����[�^�̔z��
	 * @return {@link PostMethod}�I�u�W�F�N�g
	 */
    static PostMethod createPostMethod(String uri, NameValuePair[] nameValuePairs) {
        PostMethod method = new PostMethod(uri);
        method.addParameters(nameValuePairs);
        method.getParams().setContentCharset("UTF-8");
        return method;
    }

    /**
	 * {@link GetMethod}�I�u�W�F�N�g�𐶐����܂��B
	 * @param uri URI�̑��΃p�X
	 * @param nameValuePairs �p�����[�^�̔z��
	 * @return {@link GetMethod}�I�u�W�F�N�g
	 */
    static GetMethod createGetMethod(String uri, NameValuePair[] nameValuePairs) {
        GetMethod method = new GetMethod(uri);
        if (nameValuePairs.length > 0) {
            List<NameValuePair> list = new ArrayList<NameValuePair>();
            Collections.addAll(list, nameValuePairs);
            method.setQueryString(list.toArray(new NameValuePair[0]));
        }
        method.getParams().setContentCharset("UTF-8");
        return method;
    }

    /**
	 * {@link HttpClient}�I�u�W�F�N�g�𐶐����ĕԂ��܂��B
	 * �}���`�X���b�h�Ή��ɃZ�b�g�A�b�v����C�w�肳�ꂽ�x�[�XURL���Z�b�g����܂��B
	 * ����ɁC�ݒ���e���g�p����BASIC�F�؂��s����悤�ɂ��܂��B
	 * @param baseUrl �x�[�XURL
	 * @param userId ���[�UID
	 * @param password �p�X���[�h
	 * @return HTTP�N���C�A���g�I�u�W�F�N�g
	 */
    static HttpClient createHttpClient(String baseUrl, String userId, String password) {
        HttpClient httpClient = new HttpClient(new MultiThreadedHttpConnectionManager());
        httpClient.getHostConfiguration().setHost(baseUrl, 80, Protocol.getProtocol("http"));
        httpClient.getParams().setAuthenticationPreemptive(true);
        TwitterclipsePlugin plugin = TwitterclipsePlugin.getDefault();
        TwitterclipseConfig config = plugin.getTwitterclipseConfiguration();
        Credentials defaultCreds = new UsernamePasswordCredentials(config.getUserId(), config.getPassword());
        httpClient.getState().setCredentials(new AuthScope("twitter.com", 80, AuthScope.ANY_REALM), defaultCreds);
        return httpClient;
    }
}
