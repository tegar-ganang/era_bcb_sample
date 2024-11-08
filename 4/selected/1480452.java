package com.jonosoft.photocast.viewer.web.client.photocast;

import java.util.ArrayList;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;

/**
 * TODO Add description Photocast
 * (com.jonosoft.photocast.viewer.web.client.Photocast)
 * 
 * @author jon
 * 
 */
public class Photocast extends PhotocastObject {

    private String photocastURL = null;

    private static interface Keys {

        public static final String TITLE = "title";

        public static final String DESCRIPTION = "description";
    }

    /**
	 * 
	 */
    public Photocast() {
    }

    /**
	 * @param photocastURL
	 */
    public Photocast(String photocastURL) {
        setPhotocastURL(photocastURL);
    }

    /**
	 * TODO Add method description for getChannel
	 *
	 * @return
	 */
    private JSONObject getChannel() {
        JSONObject channel = (JSONObject) jsonRep.get("channel");
        if (channel == null) jsonRep.put("channel", channel = new JSONObject());
        return channel;
    }

    /**
	 * @return the photocastURL
	 */
    public String getPhotocastURL() {
        return this.photocastURL;
    }

    /**
	 * @param photocastURL
	 *            the photocastURL to set
	 */
    public void setPhotocastURL(String photocastURL) {
        this.photocastURL = photocastURL;
    }

    /**
	 * @return Photocast's title
	 */
    public String getTitle() {
        return JSONStringValueOrNull((JSONString) getChannel().get(Keys.TITLE));
    }

    /**
	 * @return Photocast's description
	 */
    public String getDescription() {
        return JSONStringValueOrNull((JSONString) getChannel().get(Keys.DESCRIPTION));
    }

    /**
	 * Sets the Photocast's title
	 * 
	 * @param title
	 */
    public void setTitle(String title) {
        getChannel().put(Keys.TITLE, new JSONString(title));
    }

    /**
	 * Sets the Photocast's description
	 *
	 * @param description
	 */
    public void setDescription(String description) {
        getChannel().put(Keys.DESCRIPTION, new JSONString(description));
    }

    private JSONArray getItems() {
        return (JSONArray) getChannel().get("item");
    }

    public ArrayList getPhotocastItems() {
        JSONArray items = getItems();
        ArrayList arrayList = new ArrayList();
        PhotocastItem photocastItem = null;
        for (int i = 0; i < items.size(); i++) {
            photocastItem = new PhotocastItem();
            photocastItem.setJSONRepresentation((JSONObject) items.get(i));
            arrayList.add(photocastItem);
        }
        return arrayList;
    }
}
