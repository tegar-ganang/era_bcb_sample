package cz.vse.integrace.iag.google;

import com.google.gdata.client.Service.GDataRequest;
import com.google.gdata.client.contacts.ContactsService;
import com.google.gdata.data.contacts.ContactEntry;
import com.google.gdata.data.contacts.ContactFeed;
import com.google.gdata.data.Link;
import com.google.gdata.util.ContentType;
import com.google.gdata.util.PreconditionFailedException;
import com.google.gdata.util.ServiceException;
import cz.vse.integrace.iag.synchronizace.SynchronizaceBean;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;

/**
 * Statless bean pro přístup ke Google kontaktům. 
 * Pro získání/vkládání/aktualizaci kontaktů může být použito uživatelské jméno
 * a heslo nebo OAuth token.
 *
 * @author David Nejedly
 * @version 1.0
 */
@Stateless
public class KontaktyDAOBean implements KontaktyDAO {

    private static final String DEFAULT_FEED = "https://www.google.com/m8/feeds/";

    private static final String JMENO_APLIKACE = "SQL DB Google kontakty integrace";

    private static final Logger LOGGER = Logger.getLogger(SynchronizaceBean.class.getName());

    /**
     * Metoda, která načte všechny kontakty pro zadaného uživatele (jeho email)
     * buď pomocí OAuth tokenu, pokud se to nepodaří, pokusí se to načíst pomocí 
     * emailu a hesla k tomuto emailu. 
     * 
     * @param email email kontaktu pro který načítá kontakty
     * @param accessToken OAuth token pro přístup k kontaktům
     * @param heslo heslo pro přístup k kontaktům
     * @return seznam kontaktů pro zadaný email
     */
    @Override
    public List<ContactEntry> getKontatky(String email, String accessToken, String heslo) {
        List<ContactEntry> kontakty = null;
        kontakty = getKontaktyPomociTokenu(email, accessToken);
        if (kontakty == null) {
            kontakty = getKontaktyPomociHesla(email, heslo);
        }
        return kontakty;
    }

    /**
     * Metoda, která načte všechny kontakty pro zadaného uživatele (jeho email)
     * pomocí emailu a hesla k tomuto emailu, když se to nepodaří je vrácen null.
     * 
     * @param email email kontaktu pro který načítá kontakty
     * @param heslo heslo pro přístup k kontaktům
     * @return seznam kontaktů pro zadaný email
     */
    private List<ContactEntry> getKontaktyPomociHesla(String email, String heslo) {
        if (email == null || heslo == null) {
            return null;
        }
        ContactFeed resultFeed = null;
        try {
            ContactsService kontaktService = new ContactsService(JMENO_APLIKACE);
            kontaktService.setUserCredentials(email, heslo);
            URL feedUrl = new URL(DEFAULT_FEED + "contacts/" + email + "/full");
            resultFeed = kontaktService.getFeed(feedUrl, ContactFeed.class);
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return resultFeed.getEntries();
    }

    /**
     * Metoda, která načte všechny kontakty pro zadaného uživatele (jeho email)
     * pomocí OAuth tokenu, když se to nepodaří je vrácen null.
     * 
     * @param email email kontaktu pro který načítá kontakty
     * @param accessToken OAuth token pro přístup k kontaktům
     * @return seznam kontaktů pro zadaný email
     */
    private List<ContactEntry> getKontaktyPomociTokenu(String email, String accessToken) {
        if (email == null || accessToken == null) {
            return null;
        }
        ContactFeed resultFeed = null;
        try {
            ContactsService kontaktService = new ContactsService(JMENO_APLIKACE);
            kontaktService.setAuthSubToken(accessToken);
            URL feedUrl = new URL(DEFAULT_FEED + "contacts/" + email + "/full");
            resultFeed = kontaktService.getFeed(feedUrl, ContactFeed.class);
        } catch (ServiceException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return resultFeed.getEntries();
    }

    /**
     * Metoda, která k zaměstnaci nastaví všechny ostatní kontakty zaměstnanců.
     * 
     * @param email email kontaktu pro který načítá kontakty
     * @param accessToken OAuth token pro přístup k kontaktům
     * @param heslo heslo pro přístup k kontaktům
     * @param kontakty kontakty k uložení
     * @param fotky fotky uživatelů
     */
    @Override
    public void setKontakty(String email, String accessToken, String heslo, List<ContactEntry> kontakty, Map<String, byte[]> fotky) {
        for (ContactEntry kontakt : kontakty) {
            kontakt = ulozKontakt(kontakt, email, accessToken, heslo);
            if (kontakt != null && fotky.containsKey(email) && fotky.get(email) != null) {
                nastavFoto(kontakt, email, accessToken, heslo, fotky.get(email));
            }
        }
    }

    /**
     * Metoda, která načte kontakt pro zadaného uživatele (jeho email) podle jeho Id,
     * buď pomocí OAuth tokenu, pokud se to nepodaří, pokusí se to načíst pomocí 
     * emailu a hesla k tomuto emailu. 
     * 
     * @param email email kontaktu pro který načítá kontakty
     * @param accessToken OAuth token pro přístup k kontaktům
     * @param heslo heslo pro přístup k kontaktům
     * @param kontaktId id kontaktu, která má být načeten
     * @return kontakt se zadaným id, pokud není nalezen null
     */
    @Override
    public ContactEntry getKontakt(String email, String accessToken, String heslo, String kontaktId) {
        ContactEntry kontakt = null;
        kontakt = getKontaktPomociTokenu(email, accessToken, kontaktId);
        if (kontakt == null) {
            kontakt = getKontaktPomociHesla(email, heslo, kontaktId);
        }
        return kontakt;
    }

    /**
     * Metoda, která načte kontakt pro zadaného uživatele (jeho email) podle jeho Id,
     *  pomocí emailu a hesla k tomuto emailu, když se to nepodaří je vrácen null.
     * 
     * @param email email kontaktu pro který načítá kontakty
     * @param heslo heslo pro přístup k kontaktům
     * @param kontaktId id kontaktu, která má být načeten
     * @return kontakt se zadaným id, pokud není nalezen null
     */
    private ContactEntry getKontaktPomociHesla(String email, String heslo, String kontaktId) {
        if (email == null || heslo == null || kontaktId == null) {
            return null;
        }
        try {
            ContactsService kontaktService = new ContactsService(JMENO_APLIKACE);
            kontaktService.setUserCredentials(email, heslo);
            kontaktId = kontaktId.replaceFirst("http://", "https://");
            LOGGER.log(Level.WARNING, "Kontakt id: {0}", kontaktId);
            URL feedUrl = new URL(kontaktId);
            return kontaktService.getEntry(feedUrl, ContactEntry.class);
        } catch (ServiceException e) {
            LOGGER.log(Level.WARNING, "Chyba služby", e);
            return null;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Chyba na vstupu/výstupu", e);
            return null;
        }
    }

    /**
     * Metoda, která načte kontakt pro zadaného uživatele (jeho email) podle jeho Id,
     * pomocí OAuth tokenu, když se to nepodaří je vrácen null.
     * 
     * @param email email kontaktu pro který načítá kontakty
     * @param accessToken OAuth token pro přístup k kontaktům
     * @param kontaktId id kontaktu, která má být načeten
     * @return kontakt se zadaným id, pokud není nalezen null
     */
    private ContactEntry getKontaktPomociTokenu(String email, String accessToken, String kontaktId) {
        if (email == null || accessToken == null || kontaktId == null) {
            return null;
        }
        try {
            ContactsService kontaktService = new ContactsService(JMENO_APLIKACE);
            kontaktService.setAuthSubToken(accessToken);
            URL feedUrl = new URL(DEFAULT_FEED + "contacts/" + email + "/full/" + kontaktId);
            return kontaktService.getEntry(feedUrl, ContactEntry.class);
        } catch (ServiceException e) {
            LOGGER.log(Level.WARNING, "Chyba služby", e);
            return null;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Chyba na vstupu/výstupu", e);
            return null;
        }
    }

    /**
     * Metoda, která načte fotografii kontaktu, který je zadán, 
     * pomocí zadaného uživatele (jeho email), buď pomocí OAuth tokenu, 
     * pokud se to nepodaří, pokusí se to načíst pomocí emailu
     * a hesla k tomuto emailu. 
     * 
     * @param email email kontaktu pro který načítá kontakty
     * @param accessToken OAuth token pro přístup k kontaktům
     * @param heslo heslo pro přístup k kontaktům
     * @param kontakt  kontakt, ke kterému má být foto načteno
     * @return data fotografie nebo null, když není načteno
     */
    @Override
    public byte[] nactiFoto(String email, String accessToken, String heslo, ContactEntry kontakt) {
        byte[] fotka = null;
        fotka = nactiFotoPomociTokenu(accessToken, kontakt);
        if (fotka == null) {
            fotka = nactiFotoPomociHesla(email, heslo, kontakt);
        }
        return fotka;
    }

    /**
     * Metoda, která načte fotografii kontaktu, který je zadán, 
     * pomocí zadaného uživatele (jeho email), pomocí emailu
     * a hesla k tomuto emailu. 
     * 
     * @param email email kontaktu pro který načítá kontakty
     * @param heslo heslo pro přístup k kontaktům
     * @param kontakt  kontakt, ke kterému má být foto načteno
     * @return data fotografie nebo null, když není načteno
     */
    private byte[] nactiFotoPomociHesla(String email, String heslo, ContactEntry kontakt) {
        if (email == null || heslo == null || kontakt == null) {
            LOGGER.log(Level.WARNING, "Špatně zadené parametry email: {0}, heslo: {1}, kontakt: {2}", new Object[] { email, heslo, kontakt });
            return null;
        }
        Link photoLink = kontakt.getContactPhotoLink();
        int read;
        if (photoLink != null) {
            InputStream in = null;
            try {
                ContactsService kontaktService = new ContactsService(JMENO_APLIKACE);
                kontaktService.setUserCredentials(email, heslo);
                in = kontaktService.getStreamFromLink(photoLink);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                while (true) {
                    if ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    } else {
                        break;
                    }
                }
                LOGGER.log(Level.WARNING, "Foto načteno.");
                return out.toByteArray();
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "Chyba na vstupu/výstupu", ex);
            } catch (ServiceException ex) {
                LOGGER.log(Level.WARNING, "Chyba služby", ex);
            } finally {
                try {
                    in.close();
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, "Chyba na vstupu/výstupu", ex);
                }
            }
        } else {
            LOGGER.log(Level.WARNING, "Photolink je null, parametry email: {0}, heslo: {1}, kontakt: {2}", new Object[] { email, heslo, kontakt });
        }
        return null;
    }

    /**
     * Metoda, která načte fotografii kontaktu, který je zadán, 
     * pomocí zadaného uživatele (jeho email), pomocí OAuth tokenu. 
     * 
     * @param accessToken OAuth token pro přístup k kontaktům
     * @param kontakt  kontakt, ke kterému má být foto načteno
     * @return data fotografie nebo null, když není načteno
     */
    private byte[] nactiFotoPomociTokenu(String accessToken, ContactEntry kontakt) {
        if (accessToken == null || kontakt == null) {
            LOGGER.log(Level.WARNING, "Špatně zadené parametry accessToken: {0}, kontakt: {1}", new Object[] { accessToken, kontakt });
            return null;
        }
        Link photoLink = kontakt.getContactPhotoLink();
        int read;
        if (photoLink != null) {
            InputStream in = null;
            try {
                ContactsService kontaktService = new ContactsService(JMENO_APLIKACE);
                kontaktService.setAuthSubToken(accessToken);
                in = kontaktService.getStreamFromLink(photoLink);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                while (true) {
                    if ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    } else {
                        break;
                    }
                }
                LOGGER.log(Level.WARNING, "Foto načteno.");
                return out.toByteArray();
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "Chyba na vstupu/výstupu", ex);
            } catch (ServiceException ex) {
                LOGGER.log(Level.WARNING, "Chyba služby", ex);
            } finally {
                try {
                    in.close();
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, "Chyba na vstupu/výstupu", ex);
                }
            }
        }
        return null;
    }

    /**
     * Metoda, která uloží (pokud kontakt má edit link, tak jej aktualizuje, 
     * když ne, vytvoří nový) zadaný kontakt pomocí zadaného uživatele (jeho email),
     * buď pomocí OAuth tokenu, pokud se to nepodaří, pokusí se uložit pomocí
     * emailu a hesla k tomuto emailu. 
     * 
     * @param email email kontaktu pro který načítá kontakty
     * @param accessToken OAuth token pro přístup k kontaktům
     * @param heslo heslo pro přístup k kontaktům
     * @param kontakt kontakt, ke který má být uložen
     * @return uložený kontakt, nebo null v případě chyby
     */
    @Override
    public ContactEntry ulozKontakt(ContactEntry kontakt, String email, String accessToken, String heslo) {
        if (kontakt == null) {
            return null;
        }
        if (kontakt.getEditLink() != null) {
            return aktualizujKontakt(kontakt, email, accessToken, heslo);
        }
        return vytvorKontakt(kontakt, email, accessToken, heslo);
    }

    /**
     * Metoda, která auktualizuje kontakt pomocí zadaného uživatele (jeho email),
     * buď pomocí OAuth tokenu, pokud se to nepodaří, pokusí se aktualizovat pomocí
     * emailu a hesla k tomuto emailu. 
     * 
     * @param email email kontaktu pro který načítá kontakty
     * @param accessToken OAuth token pro přístup k kontaktům
     * @param heslo heslo pro přístup k kontaktům
     * @param kontakt kontakt, ke který má být aktualizován
     * @return uložený kontakt, nebo null v případě chyby
     */
    private ContactEntry aktualizujKontakt(ContactEntry kontakt, String email, String accessToken, String heslo) {
        if (accessToken != null && kontakt != null) {
            return aktualizujKontaktPomociTokenu(accessToken, kontakt);
        } else if (email != null && heslo != null && kontakt != null) {
            return akualizujKontaktPomociHesla(email, heslo, kontakt);
        } else {
            return null;
        }
    }

    /**
     * Metoda, která auktualizuje kontakt pomocí zadaného uživatele (jeho email),
     * pomocí OAuth tokenu.
     * 
     * @param accessToken OAuth token pro přístup k kontaktům
     * @param kontakt kontakt, ke který má být aktualizován
     * @return uložený kontakt, nebo null v případě chyby
     */
    private ContactEntry aktualizujKontaktPomociTokenu(String accessToken, ContactEntry kontakt) {
        try {
            ContactsService kontaktService = new ContactsService(JMENO_APLIKACE);
            URL editUrl = new URL(kontakt.getEditLink().getHref());
            kontaktService.setAuthSubToken(accessToken);
            ContactEntry contactEntry = kontaktService.update(editUrl, kontakt);
            System.out.println("Updated: " + contactEntry.getUpdated().toString());
            return contactEntry;
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Chyba na vstupu/výstupu", ex);
        } catch (PreconditionFailedException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        } catch (ServiceException ex) {
            LOGGER.log(Level.WARNING, "Chyba služby", ex);
        }
        return null;
    }

    /**
     * Metoda, která aukutalizuje kontakt pomocí zadaného uživatele (jeho email),
     * pomocí emailu a hesla k tomuto emailu. 
     * 
     * @param email email kontaktu pro který načítá kontakty
     * @param heslo heslo pro přístup k kontaktům
     * @param kontakt kontakt, ke který má být aktualizován
     * @return uložený kontakt, nebo null v případě chyby
     */
    private ContactEntry akualizujKontaktPomociHesla(String email, String heslo, ContactEntry kontakt) {
        try {
            ContactsService kontaktService = new ContactsService(JMENO_APLIKACE);
            URL editUrl = new URL(kontakt.getEditLink().getHref());
            kontaktService.setUserCredentials(email, heslo);
            ContactEntry contactEntry = kontaktService.update(editUrl, kontakt);
            System.out.println("Updated: " + contactEntry.getUpdated().toString());
            return contactEntry;
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Chyba na vstupu/výstupu", ex);
        } catch (PreconditionFailedException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        } catch (ServiceException ex) {
            LOGGER.log(Level.WARNING, "Chyba služby", ex);
        }
        return null;
    }

    /**
     * Metoda, která vytvoří kontakt k zadanému uživateli (jeho email),
     * buď pomocí OAuth tokenu, pokud se to nepodaří, pokusí se vytvořit pomocí
     * emailu a hesla k tomuto emailu. 
     * 
     * @param email email kontaktu pro který načítá kontakty
     * @param accessToken OAuth token pro přístup k kontaktům
     * @param heslo heslo pro přístup k kontaktům
     * @param kontakt kontakt, ke který má být vytvořen
     * @return uložený kontakt, nebo null v případě chyby
     */
    private ContactEntry vytvorKontakt(ContactEntry kontakt, String email, String accessToken, String heslo) {
        if (email != null && accessToken != null && !"".equals(accessToken) && kontakt != null) {
            return vytvorKontaktPomociTokenu(email, accessToken, kontakt);
        } else if (email != null && heslo != null && kontakt != null) {
            return vytvorKontaktPomociHesla(email, heslo, kontakt);
        }
        return null;
    }

    /**
     * Metoda, která vytvoří kontakt k zadanému uživateli (jeho email),
     * pomocí OAuth tokenu. 
     * 
     * @param email email kontaktu pro který načítá kontakty
     * @param accessToken OAuth token pro přístup k kontaktům
     * @param heslo heslo pro přístup k kontaktům
     * @param kontakt kontakt, ke který má být vytvořen
     * @return uložený kontakt, nebo null v případě chyby
     */
    private ContactEntry vytvorKontaktPomociTokenu(String email, String accessToken, ContactEntry kontakt) {
        try {
            ContactsService kontaktService = new ContactsService(JMENO_APLIKACE);
            kontaktService.setAuthSubToken(accessToken);
            URL postUrl = new URL(DEFAULT_FEED + "contacts/" + email + "/full");
            ContactEntry vytvorenyKontakt = kontaktService.insert(postUrl, kontakt);
            System.out.println("Contact's ID: " + vytvorenyKontakt.getId());
            return vytvorenyKontakt;
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Chyba na vstupu/výstupu", ex);
        } catch (PreconditionFailedException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        } catch (ServiceException ex) {
            LOGGER.log(Level.WARNING, "Chyba služby", ex);
        }
        return null;
    }

    /**
     * Metoda, která vytvoří kontakt k zadanému uživateli (jeho email),
     * pomocí emailu a hesla k tomuto emailu. 
     * 
     * @param email email kontaktu pro který načítá kontakty
     * @param heslo heslo pro přístup k kontaktům
     * @param kontakt kontakt, ke který má být vytvořen
     * @return uložený kontakt, nebo null v případě chyby
     */
    private ContactEntry vytvorKontaktPomociHesla(String email, String heslo, ContactEntry kontakt) {
        try {
            ContactsService kontaktService = new ContactsService(JMENO_APLIKACE);
            kontaktService.setUserCredentials(email, heslo);
            URL postUrl = new URL(DEFAULT_FEED + "contacts/" + email + "/full");
            ContactEntry vytvorenyKontakt = kontaktService.insert(postUrl, kontakt);
            LOGGER.log(Level.FINE, "Contact''s ID: {0}", vytvorenyKontakt.getId());
            return vytvorenyKontakt;
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Chyba na vstupu/výstupu", ex);
        } catch (PreconditionFailedException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        } catch (ServiceException ex) {
            LOGGER.log(Level.WARNING, "Chyba služby", ex);
        }
        return null;
    }

    /**
     * Metoda nastavující fotografii k zadanému kontaktu, 
     * buď pomocí OAuth tokenu, pokud se to nepodaří, pokusí se nastavit pomocí
     * emailu a hesla k tomuto emailu.
     * 
     * @param email email kontaktu pro který načítá kontakty
     * @param accessToken OAuth token pro přístup k kontaktům
     * @param heslo heslo pro přístup k kontaktům
     * @param kontakt kontakt pro uložení fotografie
     * @param foto fotografie k uložení
     * @return true, pokud se podaří foto nastavit, jinak false
     */
    private boolean nastavFoto(ContactEntry kontakt, String email, String accessToken, String heslo, byte[] foto) {
        boolean nastaveno = nastavFotoPomociTokenu(accessToken, kontakt, foto);
        if (!nastaveno) {
            nastaveno = nastavFotoPomociHesla(email, heslo, kontakt, foto);
        }
        return nastaveno;
    }

    /**
     * Metoda nastavující fotografii k zadanému kontaktu, 
     * pomocí emailu a hesla k tomuto emailu.
     * 
     * @param email email kontaktu pro který načítá kontakty
     * @param heslo heslo pro přístup k kontaktům
     * @param kontakt kontakt pro uložení fotografie
     * @param foto fotografie k uložení
     * @return true, pokud se podaří foto nastavit, jinak false
     */
    private boolean nastavFotoPomociHesla(String email, String heslo, ContactEntry kontakt, byte[] foto) {
        if (email == null || heslo == null || kontakt == null) {
            LOGGER.log(Level.WARNING, "Špatně zadené parametry email: {0}, heslo: {1}, kontakt: {2}", new Object[] { email, heslo, kontakt });
            return false;
        }
        if (foto == null) {
            LOGGER.log(Level.WARNING, "Zadané foto je null. Uložení nevykonáno");
            return false;
        }
        OutputStream requestStream = null;
        try {
            ContactsService kontaktService = new ContactsService(JMENO_APLIKACE);
            kontaktService.setUserCredentials(email, heslo);
            Link fotoLink = kontakt.getContactEditPhotoLink();
            URL fotoUrl = new URL(fotoLink.getHref());
            GDataRequest request = kontaktService.createRequest(GDataRequest.RequestType.UPDATE, fotoUrl, new ContentType("image/jpeg"));
            request.setEtag(fotoLink.getEtag());
            requestStream = request.getRequestStream();
            requestStream.write(foto);
            request.execute();
            return true;
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Chyba na vstupu/výstupu", ex);
        } catch (PreconditionFailedException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        } catch (ServiceException ex) {
            LOGGER.log(Level.SEVERE, "Chyba služby", ex);
        } finally {
            try {
                requestStream.close();
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "Chyba na vstupu/výstupu", ex);
            }
        }
        return false;
    }

    /**
     * Metoda nastavující fotografii k zadanému kontaktu, 
     * pomocí OAuth tokenu.
     * 
     * @param accessToken OAuth token pro přístup k kontaktům
     * @param kontakt kontakt pro uložení fotografie
     * @param foto fotografie k uložení
     * @return true, pokud se podaří foto nastavit, jinak false
     */
    private boolean nastavFotoPomociTokenu(String accessToken, ContactEntry kontakt, byte[] foto) {
        if (accessToken == null || kontakt == null) {
            return false;
        }
        OutputStream requestStream = null;
        try {
            ContactsService kontaktService = new ContactsService(JMENO_APLIKACE);
            kontaktService.setAuthSubToken(accessToken);
            Link fotoLink = kontakt.getContactEditPhotoLink();
            URL fotoUrl = new URL(fotoLink.getHref());
            GDataRequest request = kontaktService.createRequest(GDataRequest.RequestType.UPDATE, fotoUrl, new ContentType("image/jpeg"));
            request.setEtag(fotoLink.getEtag());
            requestStream = request.getRequestStream();
            requestStream.write(foto);
            request.execute();
            return true;
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Chyba na vstupu/výstupu", ex);
        } catch (PreconditionFailedException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        } catch (ServiceException ex) {
            LOGGER.log(Level.WARNING, "Chyba služby", ex);
        } finally {
            try {
                requestStream.close();
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "Chyba na vstupu/výstupu", ex);
            }
        }
        return false;
    }
}
