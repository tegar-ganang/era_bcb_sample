package com.taobao.top.util;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;
import com.taobao.top.Constants;

/**
 * 系统工具类。
 * 
 * @author carver.gu
 * @since 1.0, Sep 12, 2009
 */
public abstract class TopUtils {

    private TopUtils() {
    }

    /**
	 * 给TOP请求签名。
	 * 
	 * @param params 所有字符型的TOP请求参数
	 * @param secret 签名密钥
	 * @return 签名
	 * @throws IOException
	 */
    public static String signTopRequest(Map<String, String> params, String secret) throws IOException {
        Map<String, String> sortedParams = new TreeMap<String, String>(params);
        Set<Entry<String, String>> paramSet = sortedParams.entrySet();
        StringBuilder query = new StringBuilder(secret);
        for (Entry<String, String> param : paramSet) {
            if (StrUtils.areNotEmpty(param.getKey(), param.getValue())) {
                query.append(param.getKey()).append(param.getValue());
            }
        }
        MessageDigest md5 = getMd5MessageDigest();
        byte[] bytes = md5.digest(query.toString().getBytes(Constants.CHARSET_UTF8));
        StringBuilder sign = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(bytes[i] & 0xFF);
            if (hex.length() == 1) {
                sign.append("0");
            }
            sign.append(hex.toUpperCase());
        }
        return sign.toString();
    }

    /**
	 * 验证TOP回调地址的签名是否合法。要求所有参数均为已URL反编码的。
	 * 
	 * @param topParams TOP私有参数（未经BASE64解密）
	 * @param topSession TOP私有会话码
	 * @param topSign TOP回调签名
	 * @param appKey 应用公钥
	 * @param appSecret 应用密钥
	 * @return 验证成功返回true，否则返回false
	 * @throws IOException
	 */
    public static boolean verifyTopResponse(String topParams, String topSession, String topSign, String appKey, String appSecret) throws IOException {
        StringBuilder result = new StringBuilder();
        MessageDigest md5 = getMd5MessageDigest();
        result.append(appKey).append(topParams).append(topSession).append(appSecret);
        byte[] bytes = md5.digest(result.toString().getBytes(Constants.CHARSET_UTF8));
        BASE64Encoder encoder = new BASE64Encoder();
        return encoder.encode(bytes).equals(topSign);
    }

    private static MessageDigest getMd5MessageDigest() throws IOException {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
	 * 获取TOP容器回调上下文，仅用于正式环境下的客户端应用。
	 * 
	 * @param authCode 授权码
	 * @return TOP容器上下文
	 * @throws IOException 如果授权码已经过期或者容器不可访问
	 */
    public static TopContext getTopContext(String authCode) throws IOException {
        String url = getProductClientSessionUrl(authCode);
        String rsp = WebUtils.doGet(url, null, Constants.CHARSET_UTF8);
        if (StrUtils.isEmpty(rsp)) {
            return null;
        }
        TopContext context = new TopContext();
        Set<Entry<String, String>> paramSet = WebUtils.splitUrlQuery(rsp).entrySet();
        for (Entry<String, String> param : paramSet) {
            if (TopContext.PARAMETERS.equals(param.getKey())) {
                context.addParameters(decodeTopParams(param.getValue()));
            } else {
                context.addParameter(param.getKey(), param.getValue());
            }
        }
        return context;
    }

    /**
	 * 使用UTF-8字符集解释TOP回调参数为键值对。
	 * 
	 * @param params 经过BASE64编码的字符串
	 * @return 键值对
	 * @throws IOException
	 */
    public static Map<String, String> decodeTopParams(String params) throws IOException {
        return decodeTopParams(params, Constants.CHARSET_UTF8);
    }

    /**
	 * 解释TOP回调参数为键值对。
	 * 
	 * @param params 经过BASE64编码的字符串
	 * @param charset 字符集，如GBK, UTF-8
	 * @return 键值对
	 * @throws IOException
	 */
    public static Map<String, String> decodeTopParams(String params, String charset) throws IOException {
        if (StrUtils.isEmpty(params)) {
            return null;
        }
        BASE64Decoder decoder = new BASE64Decoder();
        byte[] buffer = decoder.decodeBuffer(params);
        String decodedParams = new String(buffer, charset);
        return WebUtils.splitUrlQuery(decodedParams);
    }

    /**
	 * 获取沙箱环境下客户端应用的上下文。
	 * 
	 * @param appKey 应用编码
	 * @param nick 用户昵称
	 * @return 应用的上下文
	 * @throws IOException
	 */
    public static TopContext getSandboxContext(String appKey, String nick) throws IOException {
        Map<String, String> authParams = new HashMap<String, String>();
        authParams.put("appkey", appKey);
        authParams.put("nick", nick);
        authParams.put("zhxz", "1");
        String response = WebUtils.doPost(Constants.SANDBOX_AUTHORIZE_URL, authParams);
        String authRegex = "<input type=\"text\" id=\"autoInput\" value=\"(.+?)\" style=\".+?\">";
        Pattern pattern = Pattern.compile(authRegex);
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            String authCode = matcher.group(1);
            String sessionUrl = getSandboxSessionUrl(authCode);
            String rsp = WebUtils.doGet(sessionUrl, null);
            TopContext context = new TopContext();
            context.setCallbackUrl(rsp);
            Set<Entry<String, String>> paramSet = WebUtils.splitUrlQuery(rsp).entrySet();
            for (Entry<String, String> param : paramSet) {
                if (TopContext.PARAMETERS.equals(param.getKey())) {
                    context.addParameters(decodeTopParams(param.getValue()));
                } else {
                    context.addParameter(param.getKey(), param.getValue());
                }
            }
            return context;
        } else {
            return null;
        }
    }

    /**
	 * 根据授权码获取沙箱环境SessionKey的地址。
	 * 
	 * @param authCode 授权码
	 * @return 地址
	 */
    public static String getSandboxSessionUrl(String authCode) {
        StringBuilder url = new StringBuilder(Constants.SANDBOX_CONTAINER_URL);
        url.append("?authcode=").append(authCode);
        return url.toString();
    }

    /**
	 * 根据应用编号获取正式环境WEB应用SessionKey的地址。
	 * 
	 * @param appKey 应用编号
	 * @return 地址
	 */
    public static String getProductWebSessionUrl(String appKey) {
        StringBuilder url = new StringBuilder(Constants.PRODUCT_CONTAINER_URL);
        url.append("?appkey=").append(appKey);
        return url.toString();
    }

    /**
	 * 根据应用编号获取正式环境客户端应用SessionKey的地址。
	 * 
	 * @param authCode 授权码
	 * @return 地址
	 */
    public static String getProductClientSessionUrl(String authCode) {
        StringBuilder url = new StringBuilder(Constants.PRODUCT_CONTAINER_URL);
        url.append("?authcode=").append(authCode);
        return url.toString();
    }

    /**
	 * 获取文件的真实后缀名。目前只支持JPG, GIF, PNG, BMP四种图片文件。
	 * 
	 * @param bytes 文件字节流
	 * @return JPG, GIF, PNG or null
	 */
    public static String getFileSuffix(byte[] bytes) {
        if (bytes == null || bytes.length < 10) {
            return null;
        }
        if (bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') {
            return "GIF";
        } else if (bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
            return "PNG";
        } else if (bytes[6] == 'J' && bytes[7] == 'F' && bytes[8] == 'I' && bytes[9] == 'F') {
            return "JPG";
        } else if (bytes[0] == 'B' && bytes[1] == 'M') {
            return "BMP";
        } else {
            return null;
        }
    }

    /**
	 * 获取文件的真实媒体类型。目前只支持JPG, GIF, PNG, BMP四种图片文件。
	 * 
	 * @param bytes 文件字节流
	 * @return 媒体类型(MEME-TYPE)
	 */
    public static String getMimeType(byte[] bytes) {
        String suffix = getFileSuffix(bytes);
        String mimeType;
        if ("JPG".equals(suffix)) {
            mimeType = "image/jpeg";
        } else if ("GIF".equals(suffix)) {
            mimeType = "image/gif";
        } else if ("PNG".equals(suffix)) {
            mimeType = "image/png";
        } else if ("BMP".equals(suffix)) {
            mimeType = "image/bmp";
        } else {
            mimeType = "application/octet-stream";
        }
        return mimeType;
    }

    /**
	 * 清除字典中值为空的项。
	 * 
	 * @param <V> 泛型
	 * @param map 待清除的字典
	 * @return 清除后的字典
	 */
    public static <V> Map<String, V> cleanupMap(Map<String, V> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        Map<String, V> result = new HashMap<String, V>(map.size());
        Set<Entry<String, V>> entries = map.entrySet();
        for (Entry<String, V> entry : entries) {
            if (entry.getValue() != null) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }
}
