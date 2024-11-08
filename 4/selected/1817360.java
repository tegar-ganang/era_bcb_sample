package jreader;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.xml.sax.SAXException;
import jreader.gui.GUI;

/**
 * Główna klasa programu. Przechowuje zbiór wszystkich subskrypcji, ustawienia
 * programu oraz obiekty przeznaczone do wyświetlania w graficznym interfejsie
 * użytkownika. Posiada też metody obsługujące podstawowe funkcjonalności
 * programu, używane przy pomocy interfejsu użytkownika. 
 */
public class JReader {

    /** Ilość podglądów kanałów lub wiadomości o jaką można się cofnąć. */
    public static final int HISTORY_SIZE = 25;

    /**
	 * Ustawienia programu wybrane przez użytkownika.
	 */
    private static Config config = new Config();

    /**
	 * Zbiór wszystkich kanałów.
	 */
    private static Channels channels = new Channels();

    /**
	 * Lista subskrypcji do wyświetlenia w GUI.
	 */
    private static List<Channel> visibleChannels = new ArrayList<Channel>();

    /**
	 * Lista wiadomości do wyświetlenia w GUI.
	 */
    private static List<Item> items = new ArrayList<Item>();

    /**
	 * Lista zawierająca historię przeglądanych elementów dla każdej zakładki.
	 * Przy pomocy przycisków Wstecz i Dalej można nawigować po historii.
	 */
    private static List<HistoryList<Preview>> previewTabs = new ArrayList<HistoryList<Preview>>(HISTORY_SIZE);

    /**
	 * Lista tagów do wyświetlenia w GUI.
	 */
    private static List<String> tags = new LinkedList<String>();

    private static ChannelComparator channelComparator = new ChannelComparator();

    private static ItemComparator itemComparator = new ItemComparator();

    /**
	 * Aktualnie wybrany przez użytkownika filtr elementów.<br>
	 * Jeśli wybrano kanał, jest równy id tego kanału.<br>
	 * Jeśli wybrano nieprzeczytane kanały, jest równy "unread".<br>
	 * Jeśli wybrano wszystkie kanały, jest równy "all".
	 */
    private static String currentFilter = "unread";

    /** Nie można tworzyć obiektów tej klasy. */
    private JReader() {
    }

    /**
	 * Uruchamia program. Opcja "-t" włącza tryb tekstowy.
	 */
    public static void main(String[] args) {
        addNewPreviewTab();
        channels.removeItems();
        updateTagsList();
        selectTag("all");
        selectUnread();
        if (args.length > 0 && args[0].equals("-t")) {
            TextUI.run();
        } else {
            GUI.run();
        }
        channels.write();
    }

    /**
	 * Zwraca ustawienia programu.
	 */
    public static Config getConfig() {
        return config;
    }

    /**
	 * Zwraca kanał o wskazanym identyfikatorze z listy wszystkich kanałów.
	 */
    public static Channel getChannel(String channelId) {
        return channels.getChannel(channelId);
    }

    /**
	 * Zwraca kanał o wskazanym indeksie z listy kanałów do wyświetlenia w GUI.
	 */
    public static Channel getChannel(int index) {
        return visibleChannels.get(index);
    }

    /**
	 * Zwraca listę kanałów do wyświetlenia w GUI.
	 */
    public static List<Channel> getVisibleChannels() {
        return visibleChannels;
    }

    /**
	 * Zwraca listę wiadomości do wyświetlenia w GUI.
	 */
    public static List<Item> getItems() {
        return items;
    }

    /**
	 * Zwraca historię wyświetlanych wiadomości dla zakładki o podanym indeksie.
	 */
    public static HistoryList<Preview> getPreview(int tabIndex) {
        return previewTabs.get(tabIndex);
    }

    /**
	 * Zwraca listę tagów do wyświetlenia w GUI.
	 */
    public static List<String> getTags() {
        return tags;
    }

    /**
	 * Dodaje nowy kanał na podstawie URLa oraz listy tagów podanej przez
	 * użytkownika. URL może odnosić się do strony www lub konkretnego
	 * pliku XML z treścią kanału. Lista tagów może być pustym Stringiem.
	 *
	 * @throws LinkNotFoundException jeśli na podanej stronie nie znaleziono
	 *         żadnych odnośników do kanałów.
	 * @throws MalformedURLException jeśli podany przez użytkownika adres
	 *         nie jest prawidłowym adresem URL.
	 * @throws SAXParseException jeśli parsowanie źródła XML kanału nie powiodło
	 *         się.
	 * @throws SAXException jeśli wystąpił błąd parsera XML.
	 * @throws IOException jeśli pobieranie pliku nie powiodło się.
	 */
    public static void addChannel(String siteURL, String channelTags) throws LinkNotFoundException, MalformedURLException, SAXException, IOException {
        Channel newChannel = ChannelFactory.getChannelFromSite(siteURL);
        newChannel.setTags(parseTags(channelTags));
        channels.add(newChannel, ChannelFactory.getDownloadedItems());
        visibleChannels.add(newChannel);
        Collections.sort(visibleChannels, channelComparator);
        for (String tag : newChannel.getTags()) {
            if (!tags.contains(tag)) {
                tags.add(tag);
            }
        }
        Collections.sort(tags);
        channels.write();
    }

    /**
	 * Ustawia poprzedni element z historii jako bieżący.
	 *
	 * @param  tabIndex Indeks zakładki której historię przeglądamy.
	 * @return <code>null</code>, jeśli nie można wrócić, w przeciwnym
	 *         wypadku podgląd elementu.
	 */
    public static Preview previousItem(int tabIndex) {
        return previewTabs.get(tabIndex).previous();
    }

    /**
	 * Ustawia następny element z historii jako bieżący.
	 *
	 * @return <code>null</code>, jeśli nie można przejść dalej, w przeciwnym
	 *         wypadku podgląd elementu.
	 */
    public static Preview nextItem(int tabIndex) {
        return previewTabs.get(tabIndex).next();
    }

    /**
	 * Ustawia następny pod względem daty nieprzeczytany element jako bieżący.
	 * Jeśli ustawione jest sortowanie od najnowszych, szuka najnowszego,
	 * w przeciwnym wypadku najstarszego.
	 *
	 * @return <code>false</code>, jeśli nie ma więcej nieprzeczytanych
	 *         wiadomości, <code>true</code> w przeciwnym wypadku.
	 */
    public static boolean nextUnread(int tabIndex) {
        Item nextUnreadItem = new Item("", "");
        Date beginningOfTime = new Date(0);
        Date endOfTime = new Date();
        try {
            endOfTime = new SimpleDateFormat("yyyy").parse("9999");
        } catch (ParseException pe) {
        }
        if (config.getSortByNewest()) {
            nextUnreadItem.setDate(beginningOfTime);
            for (Channel channel : visibleChannels) {
                if (channel.getUnreadItemsCount() > 0) {
                    for (Item item : channels.getItems(channel.getId())) {
                        if (!item.isRead()) {
                            if (item.getDate().after(nextUnreadItem.getDate())) {
                                nextUnreadItem = item;
                            }
                        }
                    }
                }
            }
        } else {
            nextUnreadItem.setDate(endOfTime);
            for (Channel channel : visibleChannels) {
                if (channel.getUnreadItemsCount() > 0) {
                    for (Item item : channels.getItems(channel.getId())) {
                        if (!item.isRead()) {
                            if (item.getDate().before(nextUnreadItem.getDate())) {
                                nextUnreadItem = item;
                            }
                        }
                    }
                }
            }
        }
        if (nextUnreadItem.getDate().equals(beginningOfTime) || nextUnreadItem.getDate().equals(endOfTime)) {
            return false;
        }
        nextUnreadItem.markAsRead();
        updateUnreadItemsCount(channels.getChannel(nextUnreadItem.getChannelId()));
        previewTabs.get(tabIndex).setCurrent(new Preview(nextUnreadItem));
        channels.write();
        return true;
    }

    /**
	 * Efekt kliknięcia na wybrany element. Zostaje on pokazany w podglądzie,
	 * oznaczony jako przeczytany, a ilość elementów nieprzeczytanych kanału
	 * z którego pochodzi zostaje zaktualizowana.
	 */
    public static void selectItem(Item item, int tabIndex) {
        item.markAsRead();
        previewTabs.get(tabIndex).setCurrent(new Preview(item));
        if (updateUnreadItemsCount(channels.getChannel(item.getChannelId()))) {
            channels.write();
        }
    }

    /**
	 * Efekt kliknięcia na wybrany kanał. Jego opis zostaje pokazany
	 * w podglądzie, a na listę elementów zostają wpisane jego elementy.
	 *
	 * @param index Indeks kanału na liście kanałów do wyświetlenia.
	 */
    public static void selectChannel(int index, int tabIndex) {
        items = channels.getItems(visibleChannels.get(index).getId());
        Collections.sort(items, itemComparator);
        previewTabs.get(tabIndex).setCurrent(new Preview(visibleChannels.get(index)));
        currentFilter = visibleChannels.get(index).getId();
    }

    /**
	 * Oznacza wszystkie wiadomości w kanale jako przeczytane.
	 */
    public static void markChannelAsRead(Channel channel) {
        for (Item item : channels.getItems(channel.getId())) {
            item.markAsRead();
        }
        if (updateUnreadItemsCount(channel)) {
            channels.write();
        }
    }

    /**
	 * Wybiera wszystkie elementy z listy kanałów.
	 */
    public static void selectAll() {
        items = new ArrayList<Item>();
        for (Channel channel : visibleChannels) {
            for (Item item : channels.getItems(channel.getId())) {
                items.add(item);
            }
        }
        Collections.sort(items, itemComparator);
        currentFilter = "all";
    }

    /**
	 * Wybiera nieprzeczytane elementy z listy kanałów.
	 */
    public static void selectUnread() {
        items = new ArrayList<Item>();
        for (Channel channel : visibleChannels) {
            if (channel.getUnreadItemsCount() > 0) {
                for (Item item : channels.getItems(channel.getId())) {
                    if (!item.isRead()) {
                        items.add(item);
                    }
                }
            }
        }
        Collections.sort(items, itemComparator);
        currentFilter = "unread";
    }

    /**
	 * Ogranicza listę kanałów do kanałów oznaczonych wybranym tagiem.
	 *
	 * @param tag Tag, według którego filtrujemy kanały. Jeśli jest równy "all",
	 *            wyświetla wszystkie kanały. Jeśli jest równy "" (pusty napis)
	 *            lub <code>null</code>, wyświetla kanały które nie są oznaczone
	 *            żadnym tagiem.
	 */
    public static void selectTag(String tag) {
        tag = tag.trim();
        visibleChannels = new ArrayList<Channel>();
        if (tag.equals("all")) {
            visibleChannels = channels.getChannels();
        } else if (tag.equals("") || tag == null) {
            for (Channel channel : channels.getChannels()) {
                if (channel.getTags().size() == 0) {
                    visibleChannels.add(channel);
                }
            }
        } else {
            for (Channel channel : channels.getChannels()) {
                if (channel.containsTag(tag)) {
                    visibleChannels.add(channel);
                }
            }
        }
        Collections.sort(visibleChannels, channelComparator);
    }

    /**
	 * Sprawdza, czy w danym kanale pojawiły się nowe wiadomości i jeśli tak,
	 * to dodaje je do listy wiadomości kanału.
	 *
	 * @throws SAXParseException jeśli parsowanie źródła XML kanału nie powiodło
	 *         się.
	 * @throws SAXException jeśli wystąpił błąd parsera XML.
	 * @throws IOException jeśli pobieranie pliku nie powiodło się.
	 * @return Ilość nowych wiadomości dodanych do kanału.
	 */
    public static int updateChannel(Channel channel) throws SAXException, IOException {
        Channel newChannel = ChannelFactory.getChannelFromXML(channel.getChannelURL());
        if (channel.getIconPath() == null) {
            String iconPath;
            try {
                iconPath = ChannelFactory.extractIconPath(channel.getLink());
                channel.setIconPath(iconPath);
            } catch (IOException ioe) {
            }
        }
        channel.setTitle(newChannel.getTitle());
        channel.setLink(newChannel.getLink());
        channel.setDescription(newChannel.getDescription());
        channel.setImageURL(newChannel.getImageURL());
        channel.setImageTitle(newChannel.getImageTitle());
        channel.setImageLink(newChannel.getImageLink());
        int newItemsCount = 0;
        for (Item updatedItem : ChannelFactory.getDownloadedItems()) {
            boolean itemAlreadyExists = false;
            for (Item item : channels.getItems(channel.getId())) {
                if (updatedItem.equals(item)) {
                    itemAlreadyExists = true;
                    break;
                }
            }
            if (!itemAlreadyExists) {
                channels.addItem(updatedItem);
                channel.addItem(updatedItem.getId());
                updateItemsList(updatedItem);
                newItemsCount++;
            }
        }
        if (updateUnreadItemsCount(channel)) {
            channels.write();
        }
        return newItemsCount;
    }

    /**
	 * Zmienia tagi kanału na podane przez użytkownika.
	 */
    public static void editTags(Channel channel, String channelTags) {
        channel.setTags(parseTags(channelTags));
        updateTagsList();
        Collections.sort(tags);
        channels.write();
    }

    /**
	 * Usuwa kanał na stałe.
	 *
	 * @param index Indeks kanału na liście kanałów do wyświetlenia.
	 */
    public static void removeChannel(int index) {
        List<Integer> indToRemove = new ArrayList<Integer>();
        for (int i = 0; i < items.size(); i++) {
            for (Item channelItem : channels.getItems(visibleChannels.get(index).getId())) {
                if (items.get(i).equals(channelItem)) {
                    indToRemove.add(i);
                }
            }
        }
        for (int i = 0; i < indToRemove.size(); i++) {
            items.remove((int) indToRemove.get(i));
            for (int j = i; j < indToRemove.size(); j++) {
                indToRemove.set(j, indToRemove.get(j) - 1);
            }
        }
        for (String itemId : visibleChannels.get(index).getItems()) {
            channels.removeItem(itemId);
        }
        channels.removeChannel(visibleChannels.get(index).getId());
        visibleChannels.remove(index);
        updateTagsList();
        channels.write();
    }

    /**
	 * Usuwa pojedynczy element. Usuwany jest też jego id z kanału, do którego
	 * należy.
	 * 
	 * @param item Element, który chcemy usunąć.
	 */
    public static void removeItem(Item item) {
        items.remove(item);
        getChannel(item.getChannelId()).getItems().remove(item.getId());
        channels.removeItem(item.getId());
    }

    /**
	 * Importuje listę kanałów z pliku. Kanały są dodawane na koniec listy
	 * wyświetlanych subskrypcji.
	 *
	 * @return Ilość zaimportowanych kanałów.
	 * @throws FileNotFoundException jeśli podany plik nie istnieje.
	 * @throws IOException jeśli nie można odczytać pliku.
	 * @throws SAXParseException jeśli parsowanie podanego pliku OPML
	 *         nie powiodło się.
	 * @throws SAXException jeśli wystąpił błąd parsera XML.
	 */
    public static int importChannelList(String fileLocation) throws IOException, SAXException {
        List<Channel> importedChannels = ImportExport.getChannelsFromFile(fileLocation);
        for (Channel channel : importedChannels) {
            if (!channels.containsChannel(channel.getId())) {
                channels.add(channel);
                visibleChannels.add(channel);
            }
            for (String tag : channel.getTags()) {
                if (!tags.contains(tag)) {
                    tags.add(tag);
                }
            }
        }
        Collections.sort(tags);
        channels.write();
        return importedChannels.size();
    }

    /**
	 * Eksportuje wszystkie kanały (nie tylko z listy do wyświetlenia) do pliku.
	 *
	 * @throws IOException jeśli zapisanie pliku jest niemożliwe.
	 */
    public static void exportChannelList(String fileLocation) throws IOException {
        ImportExport.writeChannelsToFile(new LinkedList<Channel>(channels.getChannels()), fileLocation);
    }

    /**
	 * Szuka podanego napisu w opisach i tytułach (lub tylko tytułach) elementów.
	 * Jeśli zostanie znaleziony, na liście elementów pojawią się elementy
	 * go zawierające.
	 *
	 * @param  titlesOnly Jeśli <code>true</code>, przeszukiwane będą same tytuły
	 *                    elementów; jeśli <code>false</code>, również ich opisy.
	 * @param  scope      Zasięg szukania:<br>
	 *                    0 - tylko w elementach z listy elementów.<br>
	 *                    1 - tylko w elementach kanałów z listy kanałów.<br>
	 *                    2 - we wszystkich elementach zapamiętanych w programie.
	 * @param  needle     Szukany napis.
	 * @return            <code>true</code>, jeśli znaleziono co najmniej jedno
	 *                    wystąpienie podanego napisu, <code>false</code>
	 *                    w przeciwnym wypadku.
	 */
    public static boolean search(boolean caseSensitive, boolean titlesOnly, int scope, String needle) {
        List<Item> haystack = new LinkedList<Item>();
        List<Item> matchingItems = new LinkedList<Item>();
        String itemContents = "";
        if (scope == 0) {
            haystack.addAll(items);
        } else if (scope == 1) {
            for (Channel channel : visibleChannels) {
                haystack.addAll(channels.getItems(channel.getId()));
            }
        } else if (scope == 2) {
            for (Channel channel : channels.getChannels()) {
                haystack.addAll(channels.getItems(channel.getId()));
            }
        }
        for (Item item : haystack) {
            itemContents = item.getTitle();
            if (!titlesOnly) {
                if (item.getDescription() != null) {
                    itemContents = itemContents.concat(item.getDescription());
                }
            }
            if (!caseSensitive) {
                itemContents = itemContents.toLowerCase();
            }
            if (itemContents.contains(needle)) {
                matchingItems.add(item);
            }
        }
        if (matchingItems.size() > 0) {
            items = matchingItems;
            return true;
        }
        return false;
    }

    /**
	 * Zwraca ilość nieprzeczytanych elementów na liście kanałów.
	 */
    public static int getUnreadItemsCount() {
        int unreadItemsCount = 0;
        for (Channel channel : visibleChannels) {
            unreadItemsCount += channel.getUnreadItemsCount();
        }
        return unreadItemsCount;
    }

    /**
	 * Zwraca ilość wszystkich elementów na liście kanałów.
	 */
    public static int getItemsCount() {
        int itemsCount = 0;
        for (Channel channel : visibleChannels) {
            itemsCount += channel.getItems().size();
        }
        return itemsCount;
    }

    public static void addNewPreviewTab() {
        previewTabs.add(new HistoryList<Preview>(HISTORY_SIZE));
    }

    public static void removePreviewTab(int tabIndex) {
        previewTabs.remove(tabIndex);
    }

    /**
	 * Zwraca listę tagów utworzoną na podstawie napisu wprowadzonego przez
	 * użytkownika.
	 *
	 * @param tagsAsString Tagi, które wprowadził użytkownik.
	 * @return Lista tagów utworzona na podstawie podanego napisu.
	 */
    public static List<String> parseTags(String tagsAsString) {
        List<String> tagsAsList = new LinkedList<String>();
        if (tagsAsString != null) {
            tagsAsString = tagsAsString.trim().toLowerCase();
            if (!"".equals(tagsAsString)) {
                tagsAsString = tagsAsString.replace(", ", ",");
                tagsAsString = tagsAsString.replace(",", " ");
                String[] tagsAsStringArray = tagsAsString.split(" ");
                for (String tag : tagsAsStringArray) {
                    tagsAsList.add(tag);
                }
            }
        }
        return tagsAsList;
    }

    /**
	 * Liczy na nowo i aktualizuje ilość nieprzeczytanych elementów kanału.
	 *
	 * @return <code>true</code>, jeśli ilość nieprzeczytanych elementów
	 *         się zmieniła; <code>false</code> w przeciwnym wypadku.
	 */
    private static boolean updateUnreadItemsCount(Channel channel) {
        int oldCount = channel.getUnreadItemsCount();
        channel.setUnreadItemsCount(0);
        for (Item item : channels.getItems(channel.getId())) {
            if (!item.isRead()) {
                channel.setUnreadItemsCount(channel.getUnreadItemsCount() + 1);
            }
        }
        if (oldCount == channel.getUnreadItemsCount()) {
            return false;
        } else {
            return true;
        }
    }

    /**
	 * Aktualizuje listę tagów do wyświetlenia w GUI.
	 */
    private static void updateTagsList() {
        tags.clear();
        for (Channel channel : channels.getChannels()) {
            for (String tag : channel.getTags()) {
                if (!tags.contains(tag)) {
                    tags.add(tag);
                }
            }
        }
        Collections.sort(tags);
    }

    /**
	 * Jeśli podany element pasuje do aktualnie wybranego filtra (czyli kanału,
	 * listy nieprzeczytanych lub wszystkich elementów), zostaje dodany do listy
	 * elementów do wyświetlenia. Chodzi o to, żeby lista elementów do
	 * wyświetlenia była uaktualniana na bieżąco podczas aktualizacji kanałów.
	 *
	 * @param item Element, który dopiero co został ściągnięty i jest nowy.
	 */
    private static void updateItemsList(Item item) {
        if (currentFilter.equals("unread")) {
            if (!item.isRead()) {
                items.add(item);
            }
        } else if (currentFilter.equals("all")) {
            items.add(item);
        } else {
            if (currentFilter.equals(item.getChannelId())) {
                items.add(item);
            }
        }
        Collections.sort(items, itemComparator);
    }
}
