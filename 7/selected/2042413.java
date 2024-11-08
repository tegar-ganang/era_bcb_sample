package client.client;

import java.util.Arrays;
import protocol.Contact;

/**
 * Trieda na uskladnenie kontaktov a ich osobných informácií tj. prezývka, email a status
 * @author Guldan
 */
public class ContactArray {

    private Contact[] list;

    /**
     * Inicializácia kontaktov
     */
    public ContactArray() {
        list = new Contact[0];
    }

    /**
     * Metóda na získanie prezývky pre emailovú adresu
     * @param str email hľadaného kontaktu
     * @return vracia prezývku kontaktu alebo ak sa tam kontakt nenachádza vracia null
     */
    public String getContactName(String str) {
        if (list.length != 0) {
            for (int i = 0; i < list.length; i++) {
                if ((str.equals(list[i].getBuddyMail()))) {
                    return list[i].getBuddyName();
                }
            }
        }
        return null;
    }

    /**
     * Metóda na získanie čísla záznamu v kontaktov. V prípade že uživateľ nie je v kotaktoch
     * vytvoríme ďalší záznam a vrátime jeho aktuálnu pozíciu
     * @param str email hľadaného kontaktu
     * @return poradie uživateľa v kontaktoch
     */
    public int getRecordNr(String str) {
        if (list.length != 0) {
            for (int i = 0; i < list.length; i++) {
                if ((str.equals(list[i].getBuddyMail()))) {
                    return i;
                }
            }
        }
        addContact(new Contact(null, str, str, true));
        return list.length - 1;
    }

    /**
     * Metóda na získanie skutočného mena uživateľa podľa čísla záznamu
     * @param i poradové číslo kontaktu
     * @return vracia emailovú adresu uživateľa
     */
    public String getEmailAdrress(int i) {
        return list[i].getBuddyMail();
    }

    /**
     * Metóda na získanie statusu uživateľa
     * @param i poradové číslo kontaktu
     * @return true ak je online, false ak nie je online
     */
    public boolean getStatus(int i) {
        return list[i].isOnline();
    }

    /**
     * Nastaví kontakty na nové kontakty
     * @param con pole nových kontaktov
     */
    public void setContacts(Contact[] con) {
        list = con;
    }

    /**
     * Pridá nový kontakt do kontaktov
     * @param con nový kontakt
     */
    public void addContact(Contact con) {
        Contact[] old = list;
        list = Arrays.copyOf(old, old.length + 1);
        list[old.length] = con;
        old = null;
    }

    /**
     * metóda na získanie vštkých kontaktov
     * @return vracia pole kontaktov
     */
    public Contact[] getContacts() {
        return list;
    }

    public Contact getContact(int i) {
        Contact con = new Contact(list[i].getSenderMail(), list[i].getBuddyMail(), list[i].getBuddyName(), false);
        return con;
    }

    /**
     * Kontrola ci uživateľ je v liste kontaktov
     * @param str email hľadaného kontaktu
     * @return true, ak je v liste false aksa tam nenachádza
     */
    public boolean isInBuddyList(String str) {
        if (list.length != 0) {
            for (int i = 0; i < list.length; i++) {
                if ((str.equals(list[i].getBuddyMail()))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     *  Mení status pripojeného uživateľa
     * @param kontakt celý kontakt, ktorý chceme zmeniť
     */
    public void changeStatus(Contact kontakt) {
        if (list.length != 0) {
            for (int i = 0; i < list.length; i++) {
                if (((kontakt.getBuddyMail()).equals(list[i].getBuddyMail()))) {
                    list[i] = kontakt;
                }
            }
        }
    }

    public void removeContact(Contact kontakt) {
        int i = getRecordNr(kontakt.getBuddyMail());
        for (int g = i; g < (list.length - 1); g++) {
            list[g] = list[g + 1];
        }
    }
}
