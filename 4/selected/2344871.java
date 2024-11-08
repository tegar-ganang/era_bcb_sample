package jreader;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * Kanał RSS lub Atom.
 */
public class Channel implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
	 * Lista identyfikatorów elementów należących do kanału.
	 */
    private List<String> items;

    /**
	 * Tagi; jeśli kanał nie jest oznaczony tagami, lista jest pusta.
	 */
    private List<String> tags;

    /**
	 * URL pliku XML z treścią kanału.
	 */
    private String channelURL;

    /**
	 * Unikalny identyfikator kanału.
	 */
    private String id;

    /** 
	 * Informacja o nieudanej aktualizacji kanału.
	 */
    private boolean isFail;

    /**
	 * Ilość nieprzeczytanych elementów.
	 */
    private int unreadItemsCount;

    /**
	 * Tytuł kanału.
	 */
    private String title;

    /**
	 * Odnośnik do strony z której został pobrany kanał.
	 */
    private String link;

    /**
	 * Opis kanału.
	 */
    private String description;

    /**
	 * Położenie obrazka będącego częścią opisu kanału.
	 */
    private String imageURL;

    /**
	 * Tytuł obrazka będącego częścią opisu kanału.
	 */
    private String imageTitle;

    /**
	 * Link do strony, która powinna się otworzyć po kliknięciu obrazka będącego
	 * częścią opisu kanału.
	 */
    private String imageLink;

    /**
	 * Ścieżka do ikony kanału, pobranej ze strony z której pochodzi kanał.
	 */
    private String iconPath;

    /**
	 * Inicjuje podstawowe wartości kanału.
	 *
	 * @param channelURL Adres URL źródła kanału.
	 */
    public Channel(String channelURL) {
        this.channelURL = channelURL;
        this.id = channelURL;
        this.items = new LinkedList<String>();
        this.tags = new LinkedList<String>();
        this.isFail = false;
        this.unreadItemsCount = 0;
    }

    /**
	 * Dodaje identyfikator elementu do kanału.
	 *
	 * @param itemId Idetyfikator elementu.
	 */
    public void addItem(String itemId) {
        this.items.add(itemId);
    }

    /**
	 * Usuwa idetyfikator elementu z listy.
	 *
	 * @param itemId Identyfikator elementu.
	 */
    public void removeItem(String itemId) {
        this.items.remove(itemId);
    }

    /**
	 * Zwraca listę wszystkich identyfikatorów elementów w kanale.
	 *
	 * @return Zwraca listę identyfikatorów elementów.
	 */
    public List<String> getItems() {
        return this.items;
    }

    /**
	 * Zwraca adres źródła kanału.
	 *
	 * @return Adres kanału.
	 */
    public String getChannelURL() {
        return this.channelURL;
    }

    /**
	 * Zwraca identyfikator kanału.
	 *
	 * @return Identyfikator kanału.
	 */
    public String getId() {
        return this.id;
    }

    /**
	 * Sprawdza, czy ostatnia aktualizacja kanału się nie powiodła.
	 *
	 * @return <code>true</code>, gdy ostatnia aktualizacja się nie powidła,
	 *         <code>false</code> w przeciwnym wypadku.
	 */
    public boolean isFail() {
        return this.isFail;
    }

    /**
	 * Ustawia informację o nieudanej aktualizacji.
	 *
	 * @param isFail <code>true</code> gdy się nie powiodło,
	 *               <code>false</code> w przeciwnym wypadku.
	 */
    public void setFail(boolean isFail) {
        this.isFail = isFail;
    }

    /**
	 * Zwraca tytuł kanału.
	 *
	 * @return Tytuł kanału lub <code>null</code>, gdy tytuł nie jest ustawiony.
	 */
    public String getTitle() {
        return this.title;
    }

    /**
	 * Ustawia tytuł kanału.
	 *
	 * @param title Tytuł kanału na który chcemy zmienić.
	 */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
	 * Zwraca odnośnik do strony z której został pobrany kanał.
	 *
	 * @return Odnośnik do strony lub <code>null</code>, gdy odnośnik nie jest
	 *         ustawiony.
	 */
    public String getLink() {
        return this.link;
    }

    /**
	 * Ustawia odnośnik do strony z której został pobrany kanał.
	 *
	 * @param link Odnośnik do strony na który chcemy zmienić.
	 */
    public void setLink(String link) {
        this.link = link;
    }

    /**
	 * Zwraca opis kanału.
	 *
	 * @return Opis kanału lub <code>null</code>, gdy opis nie jest ustawiony.
	 */
    public String getDescription() {
        return this.description;
    }

    /**
	 * Ustawia opis kanału.
	 *
	 * @param description Opis kanału na który chcemy zmienić.
	 */
    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageURL() {
        return this.imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public String getImageTitle() {
        return this.imageTitle;
    }

    public void setImageTitle(String imageTitle) {
        this.imageTitle = imageTitle;
    }

    public String getImageLink() {
        return this.imageLink;
    }

    public void setImageLink(String imageLink) {
        this.imageLink = imageLink;
    }

    /**
	 * Zwraca ścieżkę do ikony kanału.
	 *
	 * @return Ścieżka do pliku graficznego na dysku, zawierającego ikonę kanału.
	 */
    public String getIconPath() {
        return this.iconPath;
    }

    /**
	 * Ustawia ścieżkę do ikony kanału.
	 *
	 * @param iconPath Ścieżka do pliku, w którym jest zapisana ikona kanału.
	 */
    public void setIconPath(String iconPath) {
        this.iconPath = iconPath;
    }

    /**
	 * Zwraca ilość nieprzeczytanych elementów w kanale.
	 *
	 * @return Ilość nieprzeczytanych elementów.
	 */
    public int getUnreadItemsCount() {
        return this.unreadItemsCount;
    }

    /**
	 * Ustawia ilość nieprzeczytanych elementów kanału.
	 *
	 * @param unreadItemsCount Ilość nieprzeczytanych elementów.
	 */
    public void setUnreadItemsCount(int unreadItemsCount) {
        this.unreadItemsCount = unreadItemsCount;
    }

    /**
	 * Zwraca listę wszystkich tagów.
	 *
	 * @return Lista tagów.
	 */
    public List<String> getTags() {
        return tags;
    }

    /**
	 * @return Lista tagów jako string w formacie 'tag1 tag2 tag3 [...]';
	 *         jeśli nie ma żadnych tagów, zwraca pusty string ""
	 */
    public String getTagsAsString() {
        String tagsList = "";
        for (String tag : tags) {
            tagsList += tag.concat(" ");
        }
        return tagsList;
    }

    /**
	 * Ustawia listę tagów kanału na podaną.
	 *
	 * @param tags Nowa lista tagów.
	 */
    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    /**
	 * Dodaje podane tagi do już istniejących.
	 *
	 * @param newTags Lista tagów.
	 */
    public void addTags(List<String> newTags) {
        this.tags.addAll(newTags);
    }

    public boolean containsTag(String tag) {
        for (String tagInThisChannel : tags) {
            if (tag.toLowerCase().equals(tagInThisChannel.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
	 * Sprawdza czy kanały są identyczne.
	 */
    public boolean equals(Object obj) {
        Channel channel = (Channel) obj;
        if (this.channelURL.equals(channel.channelURL)) {
            return true;
        }
        return false;
    }
}
