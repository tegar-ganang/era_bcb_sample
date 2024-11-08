package jreader;

import java.io.Serializable;
import java.util.Date;

/**
 * Element (wiadomość) kanału.
 */
public class Item implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
	 * Data utworzenia obiektu reprezentującego ten element.
	 */
    private Date creationDate;

    /**
	 * Unikalny identyfikator elementu.
	 */
    private String id;

    /**
	 * Unikalny identyfikator kanału do którego należy element.
	 */
    private String channelId;

    /**
	 * Informacja o tym czy element został przeczytany.
	 */
    private boolean isRead;

    /**
	 * Tytuł elementu.
	 */
    private String title;

    /**
	 * Odnośnik do źródła skąd pochodzi element.
	 */
    private String link;

    /**
	 * Opis elementu.
	 */
    private String description;

    /**
	 * Autor elementu.
	 */
    private String author;

    /**
	 * Data publikacji elementu.
	 */
    private Date date;

    /**
	 * Inicjuje podstawowe wartości elementu.
	 *
	 * @param itemId Identykikator elementu.
	 * @param channelId Identykikator kanału do którego należy element.
	 */
    public Item(String itemId, String channelId) {
        this.creationDate = new Date();
        this.id = itemId;
        this.channelId = channelId;
    }

    /**
	 * Zwraca czas utworzenia obiektu.
	 *
	 * @return Czas utworzenia elementu.
	 */
    public Date getCreationDate() {
        return this.creationDate;
    }

    /**
	 * Zwraca identyfikator elementu.
	 *
	 * @return Identyfikator elementu.
	 */
    public String getId() {
        return this.id;
    }

    /**
	 * Zwraca identyfikator kanału do którego nalezy element.
	 *
	 * @return Identyfikator kanału.
	 */
    public String getChannelId() {
        return this.channelId;
    }

    /**
	 * Sprawdza czy element został przeczytany.
	 *
	 * @return <code>true</code>, gdy element został przeczytany lub
	 *         <code>false</code> w przeciwnym wypadku.
	 */
    public boolean isRead() {
        return this.isRead;
    }

    /**
	 * Ustawia status elementu na przeczytany.
	 */
    public void markAsRead() {
        isRead = true;
    }

    /**
	 * Ustawia status elementu na nieprzeczytany.
	 */
    public void markAsUnread() {
        isRead = false;
    }

    /**
	 * Zwraca tytuł elementu.
	 *
	 * @return Tytuł elementu lub <code>null</code> gdy tytuł nie jest ustawiony.
	 */
    public String getTitle() {
        return this.title;
    }

    /**
	 * Ustawia tytuł elementu.
	 *
	 * @param title Tytuł elementu na który chcemy zmienić.
	 */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
	 * Zwraca odnośnik.
	 *
	 * @return Odnośnik lub <code>null</code>, gdy odnośnik nie jest ustawiony.
	 */
    public String getLink() {
        return this.link;
    }

    /**
	 * Ustawia odnośnik.
	 *
	 * @param link Odnośnik na który chcemy zmienić.
	 */
    public void setLink(String link) {
        this.link = link;
    }

    /**
	 * Zwraca opis elementu.
	 *
	 * @return Opis elementu lub <code>null</code>, gdy opis nie jest ustawiony.
	 */
    public String getDescription() {
        return this.description;
    }

    /**
	 * Ustawia opis elementu.
	 *
	 * @param description Opis na który chcemy zmienić.
	 */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
	 * Zwraca autora elementu.
	 *
	 * @return Autor elementu lub <code>null</code> gdy autor nie jest ustawiony.
	 */
    public String getAuthor() {
        return this.author;
    }

    /**
	 * Ustawia autora elementu.
	 *
	 * @param author Autor na który chcemy zmienić.
	 */
    public void setAuthor(String author) {
        this.author = author;
    }

    /**
	 * Zwraca datę napisania elementu.
	 *
	 * @return Data napisania elementu lub <code>null</code>, gdy data nie jest
	 *         ustawiona.
	 */
    public Date getDate() {
        return this.date;
    }

    /**
	 * Ustawia datę napisania elementu.
	 *
	 * @param date Data na którą chcemy zmienić.
	 */
    public void setDate(Date date) {
        this.date = date;
    }

    /**
	 * Sprawdza czy elementy są identyczne (do sprawdzania, czy dany element
	 * jest nowy).
	 */
    public boolean equals(Object obj) {
        Item item = (Item) obj;
        if (this.id.equals(item.id)) {
            return true;
        }
        return false;
    }
}
