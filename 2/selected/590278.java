package hci.gnomex.httpclient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * @author Tony Di Sera
 * This is a java main that will be called by a batch script to automatically
 * create the FDT staging directory to upload files to an experiment
 * or an analysis in GNomEx.
 *
 */
public class FastDataTransferUploadStart {

    private String userName;

    private String password;

    private String propertiesFileName = "/properties/gnomex_httpclient.properties";

    private boolean debug = false;

    private String server;

    private String analysisNumber;

    private String requestNumber;

    /**
   * @param args
   */
    public static void main(String[] args) {
        FastDataTransferUploadStart createAnalysis = new FastDataTransferUploadStart(args);
        createAnalysis.callServlet();
    }

    private FastDataTransferUploadStart(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-h")) {
                printUsage();
                return;
            } else if (args[i].equals("-debug")) {
                debug = true;
            } else if (args[i].equals("-properties")) {
                propertiesFileName = args[++i];
            } else if (args[i].equals("-server")) {
                server = args[++i];
            } else if (args[i].equals("-experimentNumber")) {
                requestNumber = args[++i];
            } else if (args[i].equals("-analysisNumber")) {
                analysisNumber = args[++i];
            }
        }
    }

    private void loadProperties() throws FileNotFoundException, IOException {
        File file = new File(propertiesFileName);
        FileInputStream fis = new FileInputStream(file);
        Properties p = new Properties();
        p.load(fis);
        userName = (String) p.get("userName");
        password = (String) p.get("password");
    }

    private void printUsage() {
        System.out.println("java hci.gnomex.utility.FastDataTransferUploadStart " + "\n" + "[-debug] " + "\n" + "-properties <propertiesFileName> " + "\n" + "-server <serverName>" + "\n" + "-experimentNumber <experimentNumber>" + "\n" + "-analysisNumber <analysisNumber>" + "\n");
    }

    private void callServlet() {
        BufferedReader in = null;
        String inputLine;
        StringBuffer outputXML = new StringBuffer();
        boolean success = false;
        try {
            loadProperties();
            if ((requestNumber == null || requestNumber.equals("")) && (analysisNumber == null || analysisNumber.equals(""))) {
                this.printUsage();
                throw new Exception("Please specify all mandatory arguments.  See command line usage.");
            }
            if (server == null || server.equals("")) {
                this.printUsage();
                throw new Exception("Please specify all mandatory arguments.  See command line usage.");
            }
            trustCerts();
            URL url = new URL((server.equals("localhost") ? "http://" : "https://") + server + "/gnomex/login_verify.jsp?j_username=" + userName + "&j_password=" + password);
            URLConnection conn = url.openConnection();
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            success = false;
            while ((inputLine = in.readLine()) != null) {
                System.out.println(inputLine);
                if (inputLine.indexOf("<SUCCESS") >= 0) {
                    success = true;
                    break;
                }
            }
            if (!success) {
                System.err.print(outputXML.toString());
                throw new Exception("Unable to login");
            }
            List<String> cookies = conn.getHeaderFields().get("Set-Cookie");
            url = new URL((server.equals("localhost") ? "http://" : "https://") + server + "/gnomex/CreateSecurityAdvisor.gx");
            conn = url.openConnection();
            for (String cookie : cookies) {
                conn.addRequestProperty("Cookie", cookie.split(";", 2)[0]);
            }
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            success = false;
            outputXML = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                outputXML.append(inputLine);
                if (inputLine.indexOf("<SecurityAdvisor") >= 0) {
                    success = true;
                    break;
                }
            }
            if (!success) {
                System.err.print(outputXML.toString());
                throw new Exception("Unable to create security advisor");
            }
            in.close();
            String parms = "";
            if (requestNumber != null) {
                parms = URLEncoder.encode("requestNumber", "UTF-8") + "=" + URLEncoder.encode(requestNumber, "UTF-8");
            } else if (analysisNumber != null) {
                parms = URLEncoder.encode("analysisNumber", "UTF-8") + "=" + URLEncoder.encode(analysisNumber, "UTF-8");
            }
            success = false;
            outputXML = new StringBuffer();
            url = new URL((server.equals("localhost") ? "http://" : "https://") + server + "/gnomex/FastDataTransferUploadStart.gx");
            conn = url.openConnection();
            conn.setDoOutput(true);
            for (String cookie : cookies) {
                conn.addRequestProperty("Cookie", cookie.split(";", 2)[0]);
            }
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(parms);
            wr.flush();
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while ((inputLine = in.readLine()) != null) {
                System.out.print(inputLine);
                if (inputLine.indexOf("<FDTUploadUuid") >= 0) {
                    success = true;
                }
            }
            System.out.println();
            if (!success) {
                throw new Exception("Unable to create upload staging directory");
            }
        } catch (MalformedURLException e) {
            printUsage();
            e.printStackTrace();
            System.err.println(e.toString());
        } catch (IOException e) {
            printUsage();
            e.printStackTrace();
            System.err.println(e.toString());
        } catch (Exception e) {
            printUsage();
            e.printStackTrace();
            System.err.println(e.toString());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private void trustCerts() {
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                if (debug) {
                    System.out.println("authType is " + authType);
                    System.out.println("cert issuers");
                }
                for (int i = 0; i < certs.length; i++) {
                    if (debug) {
                        System.out.println("\t" + certs[i].getIssuerX500Principal().getName());
                        System.out.println("\t" + certs[i].getIssuerDN().getName());
                    }
                }
            }
        } };
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static class MyAuthenticator extends Authenticator {

        private String userName;

        private String password;

        private MyAuthenticator(String userName, String password) {
            this.userName = userName;
            this.password = password;
        }

        protected PasswordAuthentication getPasswordAuthentication() {
            String promptString = getRequestingPrompt();
            String hostname = getRequestingHost();
            InetAddress ipaddr = getRequestingSite();
            int port = getRequestingPort();
            return new PasswordAuthentication(userName, password.toCharArray());
        }
    }
}
