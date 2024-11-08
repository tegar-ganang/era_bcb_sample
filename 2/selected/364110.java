package com.google.checkout;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * The parent for all Checkout requests.
 * 
 * @author simonjsmith@google.com
 */
public abstract class AbstractCheckoutRequest {

    protected MerchantConstants merchantConstants;

    /**
	 * Constructor which takes an instance of MerchantConstants.
	 * 
	 * @param merchantConstants
	 *            The MerchantConstants.
	 * 
	 * @see MerchantConstants
	 */
    public AbstractCheckoutRequest(MerchantConstants merchantConstants) {
        this.merchantConstants = merchantConstants;
    }

    /**
	 * Return the URL to POST the request to.
	 * 
	 * @return The POST URL.
	 */
    public abstract String getPostUrl();

    /**
	 * Return the XML request String.
	 * 
	 * @return The XML request String.
	 */
    public abstract String getXml();

    /**
	 * Return the nicely formatted XML request String.
	 * 
	 * @return The nicely formatted XML request String.
	 */
    public abstract String getXmlPretty();

    /**
	 * Submit the request to the POST URL and return a CheckoutResonse.
	 * 
	 * @return The CheckoutResponse object.
	 * 
	 * @see CheckoutResponse
	 */
    public CheckoutResponse send() {
        try {
            URL url = new URL(getPostUrl());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Basic " + merchantConstants.getHttpAuth());
            connection.setRequestProperty("Host", connection.getURL().getHost());
            connection.setRequestProperty("content-type", "application/xml; charset=UTF-8");
            connection.setRequestProperty("accept", "application/xml");
            PrintWriter output = new PrintWriter(new OutputStreamWriter(connection.getOutputStream(), "UTF8"));
            output.print(getXml());
            output.flush();
            output.close();
            int responseCode = ((HttpURLConnection) connection).getResponseCode();
            InputStream inputStream;
            if (responseCode == HttpURLConnection.HTTP_OK) {
                inputStream = ((HttpURLConnection) connection).getInputStream();
            } else {
                inputStream = ((HttpURLConnection) connection).getErrorStream();
            }
            return new CheckoutResponse(inputStream);
        } catch (MalformedURLException murle) {
            System.err.println("MalformedURLException encountered.");
        } catch (IOException ioe) {
            System.err.println("IOException encountered.");
        }
        return null;
    }
}
