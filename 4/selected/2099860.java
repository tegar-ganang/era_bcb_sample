package org.is;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.URL;
import org.apache.log4j.Logger;
import org.is.web.CGIParameters;
import org.is.web.HttpRequest;

/**
 * Remote Index implementation. 
 * For local implementation - use IndexLocal class
 */
public class IndexRemote extends IndexBase {

    private static Logger logger = Logger.getLogger(IndexRemote.class);

    private String remoteIndexUrl;

    public IndexRemote() {
        remoteIndexUrl = Parameters.getString("index.remote.url");
    }

    public Result get(String keys, int from, int num) throws Exception {
        HttpRequest httpReq = new HttpRequest(new URL(remoteIndexUrl));
        CGIParameters params = new CGIParameters();
        params.put("cmd", Constants.CGI_PARAM_CMD_GET);
        params.put(Constants.CGI_PARAM_KEYS, keys);
        params.put(Constants.CGI_PARAM_FROM, Integer.toString(from));
        params.put(Constants.CGI_PARAM_NUM, Integer.toString(num));
        Result result = new Result(from);
        BufferedReader bis = null;
        try {
            bis = new BufferedReader(new InputStreamReader(httpReq.post(params)));
            String line = bis.readLine();
            result.setTotal(Integer.parseInt(line));
            while ((line = bis.readLine()) != null) {
                result.add(Utils.line2Entry(line));
            }
        } finally {
            if (bis != null) bis.close();
        }
        return result;
    }

    public String put(String keys, String uri, boolean tokenize) throws Exception {
        HttpRequest httpReq = new HttpRequest(new URL(remoteIndexUrl));
        CGIParameters params = new CGIParameters();
        params.put("cmd", Constants.CGI_PARAM_CMD_PUT);
        params.put(Constants.CGI_PARAM_KEYS, keys);
        params.put(Constants.CGI_PARAM_URI, uri);
        StringBuffer sb = new StringBuffer();
        InputStream is = null;
        is = httpReq.post(params);
        try {
            int read = 0;
            while ((read = is.read()) != -1) {
                sb.append((char) read);
            }
        } finally {
            if (is != null) is.close();
        }
        return sb.toString();
    }

    public void delete(String uri) throws Exception {
        HttpRequest httpReq = new HttpRequest(new URL(remoteIndexUrl));
        CGIParameters params = new CGIParameters();
        params.put("cmd", Constants.CGI_PARAM_CMD_DELETE);
        params.put(Constants.CGI_PARAM_URI, uri);
        InputStream is = null;
        is = httpReq.post(params);
        try {
            int read = 0;
            while ((read = is.read()) != -1) {
            }
        } finally {
            if (is != null) is.close();
        }
    }

    public void deleteByKey(String keys) throws Exception {
        HttpRequest httpReq = new HttpRequest(new URL(remoteIndexUrl));
        CGIParameters params = new CGIParameters();
        params.put("cmd", Constants.CGI_PARAM_CMD_DELETE);
        params.put(Constants.CGI_PARAM_KEYS, keys);
        InputStream is = null;
        is = httpReq.post(params);
        try {
            int read = 0;
            while ((read = is.read()) != -1) {
            }
        } finally {
            if (is != null) is.close();
        }
    }

    public void dumpIndex(Writer out) throws Exception {
        HttpRequest httpReq = new HttpRequest(new URL(remoteIndexUrl));
        CGIParameters params = new CGIParameters();
        params.put("cmd", Constants.CGI_PARAM_CMD_DUMP_INDEX);
        InputStream is = null;
        try {
            is = httpReq.get(params);
            BufferedInputStream bis = new BufferedInputStream(is);
            int read = 0;
            while ((read = bis.read()) != -1) {
                out.write((char) read);
            }
        } finally {
            if (is != null) is.close();
        }
    }

    public void optimize() throws Exception {
        HttpRequest httpReq = new HttpRequest(new URL(remoteIndexUrl));
        CGIParameters params = new CGIParameters();
        params.put("cmd", Constants.CGI_PARAM_CMD_OPTIMIZE);
        InputStream is = null;
        try {
            is = httpReq.get(params);
            byte b[] = new byte[1024];
            BufferedInputStream bis = new BufferedInputStream(is);
            int read = 0;
            while ((read = bis.read()) != -1) {
            }
        } finally {
            if (is != null) is.close();
        }
    }
}
