package jreader;

import java.util.Date;

/**
 * Dane potrzebne do wyświetlenia podglądu elementu lub kanału w GUI.
 */
public class Preview {

    /** Czy pokazuje element, w przeciwnym wypadku informacje o kanale. */
    private boolean showingItem;

    private String title;

    private String link;

    private Date date;

    private String description;

    private String author;

    private String source;

    /** Tytuł kanału, z którego pochodzi element. */
    private String channelTitle;

    /** Adres strony, z której pochodzi kanał bądź element. */
    private String baseURL;

    /** Obrazek będący częścią opisu kanału. */
    private String imageURL;

    private String imageTitle;

    private String imageLink;

    /**
	 * Tworzy nowy podgląd kanału.
	 */
    public Preview(Channel ch) {
        showingItem = false;
        this.title = ch.getTitle();
        this.link = ch.getLink();
        this.description = ch.getDescription();
        this.imageURL = ch.getImageURL();
        this.imageTitle = ch.getImageTitle();
        this.imageLink = ch.getImageLink();
        this.source = ch.getChannelURL();
        this.baseURL = ch.getLink();
    }

    /**
	 * Tworzy nowy podgląd elementu.
	 */
    public Preview(Item item) {
        showingItem = true;
        this.title = item.getTitle();
        this.link = item.getLink();
        this.date = item.getDate();
        this.description = item.getDescription();
        this.author = item.getAuthor();
        this.source = null;
        this.channelTitle = JReader.getChannel(item.getChannelId()).getTitle();
        this.baseURL = JReader.getChannel(item.getChannelId()).getLink();
    }

    /**
	 * Sprawdza czy jest to podgląd elementu kanału czy opisu kanału.
	 *
	 * @return <code>true</code>, jeśli ten obiekt stanowi podgląd elementu,
	 *         <code>false</code>, jeśli stanowi opis kanału.
	 */
    public boolean isShowingItem() {
        return showingItem;
    }

    public String getTitle() {
        return title;
    }

    public String getLink() {
        return link;
    }

    public String getAuthor() {
        return author;
    }

    /**
	 * Zwraca URL źródła XML kanału lub <code>null</code> dla wiadomości.
	 */
    public String getSource() {
        return source;
    }

    /**
	 * Zwraca tytuł kanału, z którego pochodzi dany element.
	 */
    public String getChannelTitle() {
        return channelTitle;
    }

    /**
	 * Zwraca datę publikacji elementu lub <code>null</code> dla kanału.
	 */
    public Date getDate() {
        return date;
    }

    /**
	 * Zwraca właściwą treść elementu lub opis kanału.
	 *
	 * @return Kod HTML zawierający treść elementu lub opis kanału.
	 */
    public String getHTML() {
        if (description == null) {
            return "<html><body><i>Brak opisu.</i></body></html>";
        }
        String HTML = "";
        if (!showingItem) {
            if (imageURL != null) {
                if (imageTitle != null) {
                    HTML += "<img alt=\"" + imageTitle + "\"" + " src=\"" + imageURL + "\">";
                } else {
                    HTML += "<img src=\"" + imageURL + "\">";
                }
                if (imageLink != null) {
                    HTML = "<br><br><a href=\"" + imageLink + "\">" + HTML + "</a>\n";
                } else {
                    HTML = "<br><br>" + HTML;
                }
            }
        }
        HTML = "<html><head><base href=\"" + baseURL + "\"></head><body>" + description + HTML + "</body></html>";
        return HTML;
    }
}
