package com.tenline.pinecone.platform.sdk;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.mapped.Configuration;
import org.codehaus.jettison.mapped.MappedNamespaceConvention;
import org.codehaus.jettison.mapped.MappedXMLStreamReader;
import org.codehaus.jettison.mapped.MappedXMLStreamWriter;
import com.tenline.pinecone.platform.model.Mail;
import com.tenline.pinecone.platform.sdk.development.APIResponse;
import com.tenline.pinecone.platform.sdk.development.JaxbAPI;

/**
 * @author Bill
 *
 */
public class MailAPI extends JaxbAPI {

    /**
	 * @param host
	 * @param port
	 * @param context
	 */
    public MailAPI(String host, String port, String context) {
        super(host, port, context);
        try {
            jaxbContext = JAXBContext.newInstance(Mail.class);
            marshaller = jaxbContext.createMarshaller();
            unmarshaller = jaxbContext.createUnmarshaller();
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    /**
	 * 
	 * @param mail
	 * @return
	 * @throws Exception
	 */
    public APIResponse create(Mail mail) throws Exception {
        APIResponse response = new APIResponse();
        connection = (HttpURLConnection) new URL(url + "/api/mail/create").openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setUseCaches(false);
        connection.setConnectTimeout(TIMEOUT);
        connection.connect();
        marshaller.marshal(mail, new MappedXMLStreamWriter(new MappedNamespaceConvention(new Configuration()), new OutputStreamWriter(connection.getOutputStream(), "utf-8")));
        connection.getOutputStream().flush();
        connection.getOutputStream().close();
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            JSONObject obj = new JSONObject(new String(new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8")).readLine()));
            response.setDone(true);
            response.setMessage(unmarshaller.unmarshal(new MappedXMLStreamReader(obj, new MappedNamespaceConvention(new Configuration()))));
            connection.getInputStream().close();
        } else {
            response.setDone(false);
            response.setMessage("Create Mail Error Code: Http (" + connection.getResponseCode() + ")");
        }
        connection.disconnect();
        return response;
    }

    /**
	 * 
	 * @param id
	 * @return
	 * @throws Exception
	 */
    public APIResponse delete(String id) throws Exception {
        APIResponse response = new APIResponse();
        connection = (HttpURLConnection) new URL(url + "/api/mail/delete/" + id).openConnection();
        connection.setRequestMethod("DELETE");
        connection.setConnectTimeout(TIMEOUT);
        connection.connect();
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            response.setDone(true);
            response.setMessage("Mail Deleted!");
        } else {
            response.setDone(false);
            response.setMessage("Delete Mail Error Code: Http (" + connection.getResponseCode() + ")");
        }
        connection.disconnect();
        return response;
    }

    /**
	 * 
	 * @param mail
	 * @return
	 * @throws Exception
	 */
    public APIResponse update(Mail mail) throws Exception {
        APIResponse response = new APIResponse();
        connection = (HttpURLConnection) new URL(url + "/api/mail/update").openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("PUT");
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setUseCaches(false);
        connection.setConnectTimeout(TIMEOUT);
        connection.connect();
        marshaller.marshal(mail, new MappedXMLStreamWriter(new MappedNamespaceConvention(new Configuration()), new OutputStreamWriter(connection.getOutputStream(), "utf-8")));
        connection.getOutputStream().flush();
        connection.getOutputStream().close();
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            JSONObject obj = new JSONObject(new String(new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8")).readLine()));
            response.setDone(true);
            response.setMessage(unmarshaller.unmarshal(new MappedXMLStreamReader(obj, new MappedNamespaceConvention(new Configuration()))));
            connection.getInputStream().close();
        } else {
            response.setDone(false);
            response.setMessage("Update Mail Error Code: Http (" + connection.getResponseCode() + ")");
        }
        connection.disconnect();
        return response;
    }

    /**
	 * 
	 * @param filter
	 * @return
	 * @throws Exception
	 */
    public APIResponse show(String filter) throws Exception {
        APIResponse response = new APIResponse();
        String requestUrl = url + "/api/mail/show/" + filter;
        connection = (HttpURLConnection) new URL(requestUrl).openConnection();
        connection.setConnectTimeout(TIMEOUT);
        connection.connect();
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            JSONArray array = new JSONArray(new String(new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8")).readLine()));
            Collection<Mail> message = new ArrayList<Mail>();
            for (int i = 0; i < array.length(); i++) {
                message.add((Mail) unmarshaller.unmarshal(new MappedXMLStreamReader(array.getJSONObject(i), new MappedNamespaceConvention(new Configuration()))));
            }
            response.setDone(true);
            response.setMessage(message);
            connection.getInputStream().close();
        } else {
            response.setDone(false);
            response.setMessage("Show Mail Error Code: Http (" + connection.getResponseCode() + ")");
        }
        connection.disconnect();
        return response;
    }

    /**
	 * 
	 * @param filter
	 * @return
	 * @throws Exception
	 */
    public APIResponse showBySender(String filter) throws Exception {
        APIResponse response = new APIResponse();
        String requestUrl = url + "/api/mail/show/@Sender/" + filter;
        connection = (HttpURLConnection) new URL(requestUrl).openConnection();
        connection.setConnectTimeout(TIMEOUT);
        connection.connect();
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            JSONArray array = new JSONArray(new String(new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8")).readLine()));
            Collection<Mail> message = new ArrayList<Mail>();
            for (int i = 0; i < array.length(); i++) {
                message.add((Mail) unmarshaller.unmarshal(new MappedXMLStreamReader(array.getJSONObject(i), new MappedNamespaceConvention(new Configuration()))));
            }
            response.setDone(true);
            response.setMessage(message);
            connection.getInputStream().close();
        } else {
            response.setDone(false);
            response.setMessage("Show Mail By Sender Error Code: Http (" + connection.getResponseCode() + ")");
        }
        connection.disconnect();
        return response;
    }

    /**
	 * 
	 * @param filter
	 * @return
	 * @throws Exception
	 */
    public APIResponse showByReceiver(String filter) throws Exception {
        APIResponse response = new APIResponse();
        String requestUrl = url + "/api/mail/show/@Receiver/" + filter;
        connection = (HttpURLConnection) new URL(requestUrl).openConnection();
        connection.setConnectTimeout(TIMEOUT);
        connection.connect();
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            JSONArray array = new JSONArray(new String(new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8")).readLine()));
            Collection<Mail> message = new ArrayList<Mail>();
            for (int i = 0; i < array.length(); i++) {
                message.add((Mail) unmarshaller.unmarshal(new MappedXMLStreamReader(array.getJSONObject(i), new MappedNamespaceConvention(new Configuration()))));
            }
            response.setDone(true);
            response.setMessage(message);
            connection.getInputStream().close();
        } else {
            response.setDone(false);
            response.setMessage("Show Mail By Receiver Error Code: Http (" + connection.getResponseCode() + ")");
        }
        connection.disconnect();
        return response;
    }
}
