package divxtek;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.net.*;
import java.io.*;
import javax.imageio.*;
import java.awt.image.*;
import java.util.*;

public class MovieModificationPanel extends JPanel {

    private static String[] SEEN;

    private static String[] GENRE;

    private static String[] MARK;

    private static String[] QUALITY;

    private JComboBox seenInput;

    private JTextField titleInput = new JTextField(40);

    private JTextField directorInput = new JTextField(40);

    private JComboBox genreInput;

    private JTextField storageInput = new JTextField(40);

    private JTextField yearInput = new JTextField(4);

    private JComboBox markInput;

    private JComboBox qualityInput;

    private JTextArea actorsInput = new JTextArea(4, 40);

    private PosterPanel poster = new PosterPanel();

    private JPanel panel = new JPanel(new BorderLayout(5, 5));

    private String posterName = "";

    ResourceBundle bundle;

    private ImageIcon moviePoster = null;

    public MovieModificationPanel(Movie modif, String arg1, String arg2) {
        bundle = ResourceBundle.getBundle("language.general", new Locale(arg1, arg2));
        SEEN = new String[] { bundle.getString("notSeen"), bundle.getString("seen") };
        GENRE = new String[] { bundle.getString("PasDeGenre"), bundle.getString("Aventure"), bundle.getString("Action"), bundle.getString("Comedie"), bundle.getString("Thriller"), bundle.getString("Policier"), bundle.getString("Horreur"), bundle.getString("Fantastique"), bundle.getString("Romantique"), bundle.getString("Suspense"), bundle.getString("Animation"), bundle.getString("ScienceFiction"), bundle.getString("Guerre"), bundle.getString("EpouvanteHorreur"), bundle.getString("Comique"), bundle.getString("Peplum"), bundle.getString("Espionnage"), bundle.getString("Drame"), bundle.getString("TeenMovie"), bundle.getString("Documentaire"), bundle.getString("Romance"), bundle.getString("ComedieMusicale"), bundle.getString("Western"), bundle.getString("ComedieDramatique"), bundle.getString("DessinAnime"), bundle.getString("Historique"), bundle.getString("Biographie") };
        MARK = new String[] { bundle.getString("PasDeNote"), "1", "2", "3", "4" };
        QUALITY = new String[] { bundle.getString("Inconnu"), "1", "2", "3", "4" };
        seenInput = new JComboBox(SEEN);
        genreInput = new JComboBox(GENRE);
        markInput = new JComboBox(MARK);
        qualityInput = new JComboBox(QUALITY);
        titleInput.setText(modif.getTitle());
        directorInput.setText(modif.getDirector());
        storageInput.setText(modif.getStorage());
        if (modif.getYear() != 0) {
            Integer year = new Integer(modif.getYear());
            yearInput.setText(year.toString());
        }
        markInput.setSelectedIndex(modif.getMark());
        qualityInput.setSelectedIndex(modif.getQuality());
        if (modif.getSeen() == true) seenInput.setSelectedIndex(0); else seenInput.setSelectedIndex(1);
        genreInput.setSelectedItem(modif.getGenre());
        String actors = "";
        if (modif.getActors().length != 0) {
            for (int i = 0; i < modif.getActors().length; i++) {
                actors = actors + modif.getActors()[i] + System.getProperty("line.separator");
            }
        }
        actorsInput.setText(actors);
        poster.setPoster(modif.getPoster());
        moviePoster = modif.getPoster();
        JPanel labelsPanel = new JPanel(new GridLayout(9, 1, 5, 5));
        labelsPanel.add(new JLabel(bundle.getString("Titre")));
        labelsPanel.add(new JLabel(bundle.getString("Realisateur")));
        labelsPanel.add(new JLabel(bundle.getString("Genre")));
        labelsPanel.add(new JLabel(bundle.getString("Rangement")));
        labelsPanel.add(new JLabel(bundle.getString("Annee")));
        labelsPanel.add(new JLabel(bundle.getString("Note")));
        labelsPanel.add(new JLabel(bundle.getString("Qualite")));
        labelsPanel.add(new JLabel(bundle.getString("Vu")));
        labelsPanel.add(new JLabel(bundle.getString("Acteurs")));
        JPanel inputPannel = new JPanel(new GridLayout(9, 1, 5, 5));
        inputPannel.add(this.titleInput);
        inputPannel.add(this.directorInput);
        inputPannel.add(this.genreInput);
        inputPannel.add(this.storageInput);
        inputPannel.add(this.yearInput);
        inputPannel.add(this.markInput);
        inputPannel.add(this.qualityInput);
        inputPannel.add(this.seenInput);
        JPanel posterPanel = new JPanel(new BorderLayout(5, 5));
        JButton chooseButton = new JButton(bundle.getString("Parcourir"));
        chooseButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ev) {
                JFileChooser fc = new JFileChooser();
                String[] extensions = { "gif", "jpg", "png" };
                FileMasque filter = new FileMasque(extensions, bundle.getString("supportedImages"));
                fc.addChoosableFileFilter(filter);
                fc.setAcceptAllFileFilterUsed(false);
                int returnVal = fc.showDialog(panel, bundle.getString("Choisir"));
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();
                    moviePoster = new ImageIcon(file.getAbsolutePath());
                    poster.setPoster(moviePoster);
                }
            }
        });
        posterPanel.add(this.poster, BorderLayout.CENTER);
        posterPanel.add(chooseButton, BorderLayout.SOUTH);
        setLayout(new BorderLayout(5, 5));
        panel.add(labelsPanel, BorderLayout.WEST);
        panel.add(inputPannel, BorderLayout.CENTER);
        panel.add(new JScrollPane(actorsInput), BorderLayout.SOUTH);
        panel.add(posterPanel, BorderLayout.EAST);
        add(panel);
    }

    public String getTitle() {
        return this.titleInput.getText();
    }

    public String getDirector() {
        return this.directorInput.getText();
    }

    public String getGenre() {
        String genre = (String) this.genreInput.getSelectedItem();
        if (genre.equals(bundle.getString("PasDeGenre"))) genre = "";
        return genre;
    }

    public String getStorage() {
        return this.storageInput.getText();
    }

    public int getYear() {
        int year;
        if (this.yearInput.getText().equals("")) year = 0; else year = Integer.parseInt(this.yearInput.getText());
        return year;
    }

    public int getMark() {
        String mark = (String) this.markInput.getSelectedItem();
        if (mark.equals(bundle.getString("PasDeNote"))) mark = "0";
        int marks = Integer.parseInt(mark);
        return marks;
    }

    public int getQuality() {
        String quality = (String) this.qualityInput.getSelectedItem();
        if (quality.equals(bundle.getString("Inconnu"))) quality = "0";
        int qualities = Integer.parseInt(quality);
        return qualities;
    }

    public boolean getSeen() {
        boolean seen = true;
        String seenString = (String) this.seenInput.getSelectedItem();
        if (seenString.equals(bundle.getString("notSeen"))) seen = false;
        return seen;
    }

    public ImageIcon getPoster() {
        return this.moviePoster;
    }

    public String[] getActors() {
        String content = actorsInput.getText();
        String[] actors = new String[0];
        int separator;
        System.out.println(content);
        while (content.lastIndexOf("\n") >= 0) {
            separator = content.indexOf("\n");
            String actor = content.substring(0, separator);
            System.out.println(actor);
            content = content.substring(separator + 1);
            int nbactors = actors.length;
            System.out.println(nbactors);
            String[] transit = new String[nbactors];
            if (nbactors != 0) for (int j = 0; j < nbactors; j++) transit[j] = actors[j];
            actors = new String[nbactors + 1];
            if (nbactors != 0) for (int j = 0; j < nbactors; j++) actors[j] = transit[j];
            actors[nbactors] = actor;
        }
        int nbactors = actors.length;
        String[] transit = new String[nbactors];
        if (nbactors != 0) for (int j = 0; j < nbactors; j++) transit[j] = actors[j];
        actors = new String[nbactors + 1];
        if (nbactors != 0) for (int j = 0; j < nbactors; j++) actors[j] = transit[j];
        actors[nbactors] = content;
        return actors;
    }

    private URL getURL() {
        String path = getClass().getName() + ".class";
        System.out.println(path);
        return getClass().getResource(path);
    }

    /**
   *@author scifire
   */
    public static final void copy(String source, String destination) {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(source);
            fos = new FileOutputStream(destination);
            java.nio.channels.FileChannel channelSrc = fis.getChannel();
            java.nio.channels.FileChannel channelDest = fos.getChannel();
            channelSrc.transferTo(0, channelSrc.size(), channelDest);
            fis.close();
            fos.close();
        } catch (FileNotFoundException e2) {
            e2.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }
}
