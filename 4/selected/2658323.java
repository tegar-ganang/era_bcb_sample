package jp.eisbahn.eclipse.plugins.lingrclipse.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jp.eisbahn.eclipse.plugins.lingrclipse.HttpRequestTimeoutException;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NameValuePair;
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

    /** Lingr����status=fail������ꂽ�Ƃ��ɍėv�����J��Ԃ��� */
    private static int RETRY = 5;

    /**
	 * �w�肳�ꂽ���\�b�h���w�肳�ꂽ{@link HttpClient}�I�u�W�F�N�g�Ŏ��s����HTTP���N�G�X�g�𔭍s���C���̌��ʂ�JSON�I�u�W�F�N�g��Ԃ��܂��B
	 * Lingr����status�l�Ƃ���fail������ꂽ�Ƃ��́C����̉񐔂����ėv�����s���܂��B
	 * @param httpClient HTTP�N���C�A���g
	 * @param method ���s���郁�\�b�h�I�u�W�F�N�g
	 * @param timeout �\�P�b�g�̃^�C���A�E�g�l(s)
	 * @return ���ʂ�JSON�I�u�W�F�N�g
	 * @throws HttpRequestFailureException HTTP���N�G�X�g�̌��ʁC�T�[�o����{@link HttpStatus#SC_OK}�ȊO�̒l������ꂽ�Ƃ�
	 * @throws IOException �Ȃ�炩�̓�o�̓G���[�����������Ƃ�
	 * @throws HttpException HTTP�v���g�R����ŉ��炩�̃G���[�����������Ƃ�
	 * @throws HttpRequestTimeoutException �T�[�o����̉�������莞�ԗ��Ȃ������Ƃ�
	 */
    static JSONObject executeMethod(HttpClient httpClient, HttpMethod method, int timeout) throws HttpRequestFailureException, HttpException, IOException, HttpRequestTimeoutException {
        try {
            method.getParams().setSoTimeout(timeout * 1000);
            int status = -1;
            JSONObject result = null;
            for (int i = 0; i < RETRY; i++) {
                System.out.println("Execute method[" + method.getURI() + "](try " + (i + 1) + ")");
                status = httpClient.executeMethod(method);
                if (status == HttpStatus.SC_OK) {
                    InputStream inputStream = method.getResponseBodyAsStream();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    IOUtils.copy(inputStream, baos);
                    String response = new String(baos.toByteArray(), "UTF-8");
                    System.out.println(response);
                    result = JSONObject.fromString(response);
                    if (result.has("status")) {
                        String lingrStatus = result.getString("status");
                        if ("ok".equals(lingrStatus)) {
                            break;
                        } else {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                            }
                        }
                    }
                } else {
                    throw new HttpRequestFailureException(status);
                }
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
        method.addParameter(new NameValuePair("format", "json"));
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
        List<NameValuePair> list = new ArrayList<NameValuePair>();
        Collections.addAll(list, nameValuePairs);
        list.add(new NameValuePair("format", "json"));
        method.setQueryString(list.toArray(new NameValuePair[0]));
        method.getParams().setContentCharset("UTF-8");
        return method;
    }

    /**
	 * {@link HttpClient}�I�u�W�F�N�g�𐶐����ĕԂ��܂��B
	 * �}���`�X���b�h�Ή��ɃZ�b�g�A�b�v����C�w�肳�ꂽ�x�[�XURL���Z�b�g����܂��B
	 * @param baseUrl �x�[�XURL
	 * @return HTTP�N���C�A���g�I�u�W�F�N�g
	 */
    static HttpClient createHttpClient(String baseUrl) {
        HttpClient httpClient = new HttpClient(new MultiThreadedHttpConnectionManager());
        httpClient.getHostConfiguration().setHost(baseUrl, 80, Protocol.getProtocol("http"));
        return httpClient;
    }
}
