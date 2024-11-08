package photobook.data.model;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Map;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.util.List;
import photobook.data.util.CryptSHA1;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

/**
 * <p>Cette classe fournit une repr�sentation Objet d'une photo.</p>
 * @author Tony Khosravi Dehkourdi
 *
 */
public class Photo implements Serializable {

    /**
	 * 
	 */
    private static final long serialVersionUID = 5468L;

    /**
	 * <p>L'identifiant de la photo.</p>
	 */
    private String id;

    /**
	 * <p>Le titre de la photo.</p>
	 */
    private String title;

    /**
	 * <p>Le texte de description de la photo.</p>
	 */
    private String description;

    /**
	 * R�pertoire o� aller chercher l'image
	 */
    public String basePathImage;

    /**
	 * <p>Les permissions de chaque groupe sur la photo. La <em>key</em> correspond � 
	 * l'identifiant du groupe et la <em>value</em> correspond � la valeur de la 
	 * permission.</p>
	 * @see Permission
	 */
    private Map<Integer, Integer> groupsPermissions;

    /**
	 * <p>Les permissions particuli�res pour certains contacts sur la photo.La 
	 * <em>key</em> correspond � l'identifiant du contact et la <em>value</em> 
	 * correspond � la valeur de la permission.</p>
	 */
    private Map<String, Integer> contactsPermissions;

    /**
	 * <p>Les notes des contacts pour la photo. La <em>key</em> correspond � 
	 * l'identifiant du contact et la <em>value</em> correspond � la valeur de la 
	 * note.</p>
	 */
    private List<Mark> marks;

    /**
	 * les commentaires de la photo
	 */
    private List<Comment> comments;

    /**
	 * Les mots cl�s de la photo
	 */
    private List<String> keywords;

    /**
	 * NE PAS UTILISER SI VOUS N'AVEZ PAS D'ID ! (Utiliser createPhoto)
	 * Constructeur, permet de d�finir l'ID de l'image
	 * @param id		identifiant de l'image
	 * @author Tony Devaux
	 * @version 0.1
	 */
    public Photo(String id) {
        this.id = id;
        List<Comment> comments = new ArrayList<Comment>();
        List<String> keywords = new ArrayList<String>();
        List<Mark> marks = new ArrayList<Mark>();
        this.comments = comments;
        this.keywords = keywords;
        this.marks = marks;
    }

    /**
	 * M�thode � appeler pour la cr�ation d'une photo, lorsqu'elle n'a jamais �t� ajout�e � l'application
	 * Permet de g�n�rer un identifiant, de la d�placer dans le r�pertoire par d�faut des images et de g�n�rer sa miniature
	 * @param title Titre de la photo
	 * @param userLogin Login de l'utilisateur (utilis� pour la g�n�ration de l'identifiant de la photo)
	 * @param pathToPhoto Chemin de la photo � r�cup�rer, sur le disque de l'utilisateur
	 * @param basePathImage Chemin o� stocker la photo
	 * @return Objet photo cr��
	 * @author Tony Devaux
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 * @throws URISyntaxException 
	 */
    public static Photo createPhoto(String title, String userLogin, String pathToPhoto, String basePathImage) throws NoSuchAlgorithmException, IOException {
        String id = CryptSHA1.genPhotoID(userLogin, title);
        String extension = pathToPhoto.substring(pathToPhoto.lastIndexOf("."));
        String destination = basePathImage + id + extension;
        FileInputStream fis = new FileInputStream(pathToPhoto);
        FileOutputStream fos = new FileOutputStream(destination);
        FileChannel fci = fis.getChannel();
        FileChannel fco = fos.getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        while (true) {
            int read = fci.read(buffer);
            if (read == -1) break;
            buffer.flip();
            fco.write(buffer);
            buffer.clear();
        }
        fci.close();
        fco.close();
        fos.close();
        fis.close();
        ImageIcon image;
        ImageIcon thumb;
        String destinationThumb = basePathImage + "thumb/" + id + extension;
        image = new ImageIcon(destination);
        int maxSize = 150;
        int origWidth = image.getIconWidth();
        int origHeight = image.getIconHeight();
        if (origWidth > origHeight) {
            thumb = new ImageIcon(image.getImage().getScaledInstance(maxSize, -1, Image.SCALE_SMOOTH));
        } else {
            thumb = new ImageIcon(image.getImage().getScaledInstance(-1, maxSize, Image.SCALE_SMOOTH));
        }
        BufferedImage bi = new BufferedImage(thumb.getIconWidth(), thumb.getIconHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics g = bi.getGraphics();
        g.drawImage(thumb.getImage(), 0, 0, null);
        try {
            ImageIO.write(bi, "JPG", new File(destinationThumb));
        } catch (IOException ioe) {
            System.out.println("Error occured saving thumbnail");
        }
        Photo photo = new Photo(id);
        photo.setTitle(title);
        photo.basePathImage = basePathImage;
        return photo;
    }

    /**
	 * R�cup�re le chemin de l'image originale
	 * @return Chemin de l'image originale
	 */
    public String getPathImage() {
        return this.basePathImage + this.getId() + ".jpg";
    }

    public String getPathImage(String newBasePath) {
        return newBasePath + this.getId() + ".jpg";
    }

    /**
	 * R�cup�re le chemin de l'image miniature
	 * @return Chemin de la miniature
	 */
    public String getPathThumb() {
        return this.basePathImage + "thumb/" + this.getId() + ".jpg";
    }

    public String getPathThumb(String newBasePath) {
        return newBasePath + "thumb/" + this.getId() + ".jpg";
    }

    /**
	 * Cette m�thode permet de r�cup�rer l'ID d'une photo
	 * @return 	Identifiant de la photo
	 * @author Tony Devaux
	 * @version 0.1
	 */
    public String getId() {
        return this.id;
    }

    /**
	 * Cette m�thode permet de r�cup�rer le titre d'une photo
	 * @return Le titre de la photo
	 * @author Tony Devaux
	 * @version 0.1
	 */
    public String getTitle() {
        return this.title;
    }

    /**
	 * Cette m�thode permet de d�finir le titre d'une photo
	 * @param title		nouveau titre
	 * @return 					True ou False, suivant le bon d�roulement
	 * @author Tony Devaux
	 * @version 0.1
	 */
    public boolean setTitle(String title) {
        this.title = title;
        if (this.title.equals(title)) return true;
        return false;
    }

    /**
	 * Cette m�thode permet de r�cup�rer la description d'une photo
	 * @return 	La description de la photo
	 * @author Tony Devaux
	 * @version 0.1
	 */
    public String getDescription() {
        return this.description;
    }

    /**
	 * Cette m�thode permet de d�finir la description d'une photo
	 * @param description		nouvelle description
	 * @return 					True ou False, suivant le bon d�roulement
	 * @author Tony Devaux
	 * @version 0.1
	 */
    public boolean setDescription(String description) {
        this.description = description;
        if (this.description.equals(description)) return true;
        return false;
    }

    /**
	 * Cette m�thode permet de r�cup�rer le r�pertoire de base des images de l'utilisateur
	 * @return basePathImage 	Chemin de base des images de l'utilisateur
	 * @author Tony Devaux
	 * @version 0.1
	 */
    public String getBasePathImage() {
        return this.basePathImage;
    }

    /**
	 * Cette m�thode permet de d�finir le r�pertoire de base des images de l'application
	 * @param basePathImage		nouveau r�pertoire de base pour les images � d�finir
	 * @return 					True ou False, suivant le bon d�roulement
	 * @author Tony Devaux
	 * @version 0.1
	 */
    public boolean setBasePathImage(String basePathImage) {
        this.basePathImage = basePathImage;
        if (this.basePathImage.equals(basePathImage)) return true;
        return false;
    }

    public boolean equals(Object object) {
        Photo user = (Photo) object;
        if (this.id.equals(user.getId())) return true;
        return false;
    }

    /**
	 * Surcharge de la m�thode toString
	 * @return Titre de la photo
	 * @author Tony Devaux
	 */
    public String toString() {
        return this.getTitle();
    }

    /**
	 * Cette m�thode permet de d�finir les commentaires de la photo
	 * @param comments
	 * @return
	 * @author JP
	 */
    public boolean setComments(List<Comment> comments) {
        this.comments = comments;
        return true;
    }

    /**
	 * Cette m�thode permet de r�cup�rer la liste des commentaires de la photo.
	 * @author JP
	 * @return Liste des commentaires
	 */
    public List<Comment> getComments() {
        return this.comments;
    }

    /**
	 * Ajoute un commentaire � la liste des commentaires de la photo.
	 * @param comment Objet commentaire � ajouter
	 * @return True ou False
	 * @author Tony Devaux
	 */
    public boolean addComment(Comment comment) {
        return this.comments.add(comment);
    }

    /**
	 * Supprime un commentaire de la liste des commentaires de la photo.
	 * @param comment
	 * @return True ou False
	 * @author Tony Devaux
	 */
    public boolean removeComment(Comment comment) {
        return this.comments.remove(comment);
    }

    /**
	 * Cette m�thode permet de r�cup�rer les notes d'une photo
	 * @return 	La liste des notes
	 * @author Tony Devaux
	 * @author JP Surget
	 * @version 0.1
	 */
    public List<Mark> getMarks() {
        return this.marks;
    }

    /**
	 * Cette m�thode permet de d�finir les notes d'une photo
	 * @author JP Surget
	 * @version 0.1
	 */
    public boolean setMarks(List<Mark> marks) {
        this.marks = marks;
        return true;
    }

    /**
	 * Ajoute une note pour la photo
	 * @param mark
	 * @return True ou False
	 * @author Tony Devaux
	 * @version 0.1
	 */
    public boolean addMark(Mark mark) {
        return this.marks.add(mark);
    }

    /**
	 * Supprime une note pour la photo
	 * @param mark
	 * @return True ou False
	 * @author Tony Devaux
	 * @version 0.1
	 */
    public boolean removeMark(Mark mark) {
        return this.marks.remove(mark);
    }

    /**
	 * Permet d'ajouter un mot cl� � la photo
	 * @param value (valeur du mot cl�)
	 * @return True ou False
	 * @author Tony Devaux
	 */
    public boolean addKeyword(String value) {
        if (value.length() < 2) return false;
        return this.keywords.add(value);
    }

    /**
	 * Permet de supprimer un mot cl� � partir de sa valeur
	 * @param value (valeur du mot cl� � supprimer)
	 * @return True ou False
	 * @author Tony Devaux
	 */
    public boolean removeKeyword(String value) {
        return this.keywords.remove(value);
    }

    /**
	 * Permet de chercher si le mot cl� sp�cifi� existe dans la liste des mots cl�s
	 * @param value Motcl� � chercher
	 * @return True ou False, suivant si le mot cl� est pr�sent ou non
	 * @author Tony Devaux
	 */
    public boolean existKeyword(String value) {
        return this.keywords.contains(value);
    }

    /**
	 * Retourne la liste des mots cl�s de la photo
	 * @return Liste de mots cl�s
	 * @author Tony Devaux
	 */
    public List<String> getKeywords() {
        return this.keywords;
    }

    /**
	 * ABANDONNEE POUR LE MOMENT !
	 * Cette m�thode permet de r�cup�rer les permissions des groupes
	 * @return 	La liste des permissions des groupes
	 * @author Tony Devaux
	 * @version 0.1
	 */
    public Map<Integer, Integer> getGroupsPermissions() {
        return this.groupsPermissions;
    }

    /**
	 * ABANDONNEE POUR LE MOMENT !
	 * Cette m�thode permet de d�finir les permissions des groupes
	 * @param groupsPermissions		nouvelle liste � d�finir
	 * @return 					True ou False, suivant le bon d�roulement
	 * @author Tony Devaux
	 * @version 0.1
	 */
    public boolean setGroupsPermissions(Map<Integer, Integer> groupsPermissions) {
        this.groupsPermissions = groupsPermissions;
        if (this.groupsPermissions.equals(groupsPermissions)) return true;
        return false;
    }

    /**
	 * ABANDONNEE POUR LE MOMENT !
	 * Cette m�thode permet de r�cup�rer les permissions des contacts
	 * @return 	La liste des permissions des contacts
	 * @author Tony Devaux
	 * @version 0.1
	 */
    public Map<String, Integer> getContactsPermissions() {
        return this.contactsPermissions;
    }

    /**
	 * ABANDONNEE POUR LE MOMENT !
	 * Cette m�thode permet de d�finir les permissions des contacts
	 * @param contactsPermissions		nouvelle liste � d�finir
	 * @return 					True ou False, suivant le bon d�roulement
	 * @author Tony Devaux
	 * @version 0.1
	 */
    public boolean setContactsPermissions(Map<String, Integer> contactsPermissions) {
        this.contactsPermissions = contactsPermissions;
        if (this.contactsPermissions.equals(contactsPermissions)) return true;
        return false;
    }

    /**
	 * Calcule la moyenne des notes recues par la photo
	 * @return Moyenne des notes
	 */
    public int getMean() {
        int mean;
        mean = 0;
        for (Mark mark : this.marks) {
            mean += mark.getValue();
        }
        mean = mean / this.marks.size();
        return mean;
    }
}
