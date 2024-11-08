import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.Vector;
import java.nio.channels.FileChannel;

public class UnType extends JPanel implements ActionListener {

    private String sonNom;

    private String sonImage;

    private int sonFranchissement;

    private String sonPourquoi;

    private JTextField sonChampNom;

    private JTextField sonChampImage;

    private JRadioButton sonOptionRadio1;

    private JRadioButton sonOptionRadio2;

    private JRadioButton sonOptionRadio3;

    private JFileChooser sonSelectionneurDeFichier;

    private File sonFichier;

    UnType(String telNom) {
        try {
            BufferedReader leLecteur = new BufferedReader(new FileReader(new File("bundll/types.jay")));
            String laLigne = leLecteur.readLine();
            while (!(telNom.equalsIgnoreCase(laLigne)) && (laLigne != null)) {
                laLigne = leLecteur.readLine();
            }
            if (laLigne == null) {
                sonImage = "blank.jpg";
                sonFranchissement = 0;
            } else {
                sonNom = telNom;
                sonImage = leLecteur.readLine();
                laLigne = leLecteur.readLine();
                if (laLigne.split(":")[0].equals("1")) sonPourquoi = laLigne.split(":")[1]; else sonPourquoi = null;
                sonFranchissement = Integer.parseInt(laLigne.split(":")[0]);
            }
            leLecteur.close();
        } catch (Exception lException) {
            JOptionPane.showMessageDialog(null, "une erreur de chargement");
        }
    }

    UnType() {
        super(new GridLayout(0, 1));
        creerLesPanneaux();
    }

    private void creerLesPanneaux() {
        sonSelectionneurDeFichier = new JFileChooser();
        sonSelectionneurDeFichier.setAcceptAllFileFilterUsed(false);
        sonSelectionneurDeFichier.addChoosableFileFilter(new javax.swing.filechooser.FileFilter() {

            public boolean accept(File telFic) {
                String lExtension = telFic.getName().substring((telFic.getName().lastIndexOf(".") + 1));
                if (((lExtension.equals("jpg") || lExtension.equals("JPG") || lExtension.equals("jpeg")) && (new ImageIcon(telFic.getAbsolutePath()).getIconWidth() == 40) && (new ImageIcon(telFic.getAbsolutePath()).getIconHeight() == 40)) || lExtension.equals(telFic.getName())) return true; else return false;
            }

            public String getDescription() {
                return "Images JPEG 40x40(*.jpg, *.jpeg, *.JPG)";
            }
        });
        sonSelectionneurDeFichier.addChoosableFileFilter(new javax.swing.filechooser.FileFilter() {

            public boolean accept(File telFic) {
                String lExtension = telFic.getName().substring((telFic.getName().lastIndexOf(".") + 1));
                if (((lExtension.equals("gif") || lExtension.equals("GIF")) && (new ImageIcon(telFic.getAbsolutePath()).getIconWidth() == 40) && (new ImageIcon(telFic.getAbsolutePath()).getIconHeight() == 40)) || lExtension.equals(telFic.getName())) return true; else return false;
            }

            public String getDescription() {
                return "Images CompuServe GIF 40x40(*.gif, *.GIF)";
            }
        });
        sonSelectionneurDeFichier.addChoosableFileFilter(new javax.swing.filechooser.FileFilter() {

            public boolean accept(File telFic) {
                String lExtension = telFic.getName().substring((telFic.getName().lastIndexOf(".") + 1));
                if (((lExtension.equals("bmp") || lExtension.equals("BMP")) && (new ImageIcon(telFic.getAbsolutePath()).getIconWidth() == 40) && (new ImageIcon(telFic.getAbsolutePath()).getIconHeight() == 40)) || lExtension.equals(telFic.getName())) return true; else return false;
            }

            public String getDescription() {
                return "Images Bitmap BMP 40x40(*.bmp,*.BMP)";
            }
        });
        sonSelectionneurDeFichier.addChoosableFileFilter(new javax.swing.filechooser.FileFilter() {

            public boolean accept(File telFic) {
                String lExtension = telFic.getName().substring((telFic.getName().lastIndexOf(".") + 1));
                if (((lExtension.equals("png") || lExtension.equals("PNG")) && (new ImageIcon(telFic.getAbsolutePath()).getIconWidth() == 40) && (new ImageIcon(telFic.getAbsolutePath()).getIconHeight() == 40)) || lExtension.equals(telFic.getName())) return true; else return false;
            }

            public String getDescription() {
                return "Images PNG 40x40(*.png, *.PNG)";
            }
        });
        sonSelectionneurDeFichier.addChoosableFileFilter(new javax.swing.filechooser.FileFilter() {

            public boolean accept(File telFic) {
                String lExtension = telFic.getName().substring((telFic.getName().lastIndexOf(".") + 1));
                if (((lExtension.equals("jpg") || lExtension.equals("JPG") || lExtension.equals("jpeg") || lExtension.equals("png") || lExtension.equals("PNG") || lExtension.equals("bmp") || lExtension.equals("BMP") || lExtension.equals("gif") || lExtension.equals("GIF")) && (new ImageIcon(telFic.getAbsolutePath()).getIconWidth() == 40) && (new ImageIcon(telFic.getAbsolutePath()).getIconHeight() == 40)) || lExtension.equals(telFic.getName())) return true; else return false;
            }

            public String getDescription() {
                return "Tous les formats support�s 40x40";
            }
        });
        JPanel lePanneau = new JPanel();
        sonChampNom = new JTextField(13);
        JLabel leLabel = new JLabel("Nom du type :  ");
        leLabel.setLabelFor(sonChampNom);
        lePanneau.add(leLabel);
        lePanneau.add(sonChampNom);
        add(lePanneau);
        lePanneau = new JPanel();
        sonChampImage = new JTextField(10);
        sonChampImage.setEditable(false);
        JButton leBouton = new JButton("Charger l'image");
        leBouton.addActionListener(this);
        lePanneau.add(sonChampImage);
        lePanneau.add(leBouton);
        add(lePanneau);
        lePanneau = new JPanel(new GridLayout(0, 1));
        ButtonGroup leGroupeDeBouton = new ButtonGroup();
        sonOptionRadio1 = new JRadioButton("Infranchissable", true);
        leGroupeDeBouton.add(sonOptionRadio1);
        lePanneau.add(sonOptionRadio1);
        sonOptionRadio2 = new JRadioButton("Difficile � franchir");
        leGroupeDeBouton.add(sonOptionRadio2);
        lePanneau.add(sonOptionRadio2);
        sonOptionRadio3 = new JRadioButton("Franchissable");
        leGroupeDeBouton.add(sonOptionRadio3);
        lePanneau.add(sonOptionRadio3);
        add(lePanneau);
        leBouton = new JButton("Enregistrer");
        leBouton.addActionListener(this);
        lePanneau.add(leBouton);
        add(lePanneau);
    }

    public void actionPerformed(ActionEvent telleAction) {
        String laSource = telleAction.getActionCommand();
        if (laSource == "Charger l'image") {
            int leRetour = sonSelectionneurDeFichier.showOpenDialog(getParent());
            if (leRetour == JFileChooser.APPROVE_OPTION) {
                sonFichier = sonSelectionneurDeFichier.getSelectedFile();
                sonChampImage.setText(sonFichier.getName());
            }
        }
        if (laSource == "Enregistrer") {
            sonNom = sonChampNom.getText();
            sonImage = sonChampImage.getText();
            if (sonNom.equals("") || sonImage.equals("")) {
                JOptionPane.showMessageDialog(null, "Un des champs est mal remplis, v�rifiez votre saisie", "Erreur de saisie", JOptionPane.WARNING_MESSAGE);
            } else {
                if (!enregistreToi()) {
                    JOptionPane.showMessageDialog(null, "Une erreur est survenue lors de l'enregistrement, v�rifiez si vous avez les droits d'�criture sur ce disque.", "Erreur d'enregistrement", JOptionPane.ERROR_MESSAGE);
                } else {
                    removeAll();
                    updateUI();
                    JOptionPane.showMessageDialog(null, "Le type de terrain a �t� cr�� avec succ�s!\nVous pouvez ajouter les fichiers : \nperso_" + sonImage + "\ncoffre_" + sonImage + "\nperso_coffre_" + sonImage + "\nperso_evt_" + sonImage + "\ncoffre_evt_" + sonImage + "\nperso_coffre_evt_" + sonImage + "\nevt_" + sonImage + "\n dans le dossier \"img_type\" si vous le d�sirez.\nAidez vous du mod�le dans les ressources si besoin est.", "Enregistrement termin�", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }
    }

    public String getsonImage() {
        return sonImage;
    }

    public String getsonNom() {
        return sonNom;
    }

    public int getsonFranchissement() {
        return sonFranchissement;
    }

    public String getsonPourquoi() {
        return sonPourquoi;
    }

    public String toString() {
        String laChaine = sonNom;
        if (sonFranchissement == 0) laChaine += "/ infranchissable"; else if (sonFranchissement == 1) laChaine += "/ difficile"; else if (sonFranchissement == 2) laChaine += "/ franchissable";
        return laChaine;
    }

    private boolean enregistreToi() {
        PrintWriter lEcrivain;
        String laDest = "./img_types/" + sonImage;
        if (!new File("./img_types").exists()) {
            new File("./img_types").mkdirs();
        }
        try {
            FileChannel leFicSource = new FileInputStream(sonFichier).getChannel();
            FileChannel leFicDest = new FileOutputStream(laDest).getChannel();
            leFicSource.transferTo(0, leFicSource.size(), leFicDest);
            leFicSource.close();
            leFicDest.close();
            lEcrivain = new PrintWriter(new FileWriter(new File("bundll/types.jay"), true));
            lEcrivain.println(sonNom);
            lEcrivain.println(sonImage);
            if (sonOptionRadio1.isSelected()) {
                lEcrivain.println("0:?");
            }
            if (sonOptionRadio2.isSelected()) {
                lEcrivain.println("1:" + JOptionPane.showInputDialog(null, "Vous avez choisis de rendre ce terrain difficile � franchir.\nVeuillez en indiquer la raison.", "Demande de pr�cision", JOptionPane.INFORMATION_MESSAGE));
            }
            if (sonOptionRadio3.isSelected()) {
                lEcrivain.println("2:?");
            }
            lEcrivain.close();
            return true;
        } catch (Exception lException) {
            return false;
        }
    }

    public static Vector donneTesTypes() {
        Vector<String> laListe = new Vector<String>();
        try {
            BufferedReader leLecteur = new BufferedReader(new FileReader(new File("bundll/types.jay")));
            String laLigne = leLecteur.readLine();
            int lIndice = 0;
            while (laLigne != null) {
                if (lIndice % 3 == 0 || lIndice == 0) {
                    laListe.add(laLigne);
                }
                laLigne = leLecteur.readLine();
                lIndice++;
            }
            leLecteur.close();
        } catch (Exception lException) {
            laListe.add("Pas de Terrains existant");
        }
        return laListe;
    }
}
