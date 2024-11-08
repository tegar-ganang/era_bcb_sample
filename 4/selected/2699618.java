package espider;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import espider.contact.Contact;
import espider.contact.ContactList;
import espider.favorite.Favorite;
import espider.indexation.SearchSpiderFile;
import espider.libs.com.inzyme.jtrm.Mp3Trm;
import espider.libs.file.AudioSpider;
import espider.libs.file.SpiderFile;
import espider.libs.helliker.id3.ID3v2FormatException;
import espider.libs.helliker.id3.MP3File;
import espider.tri.ConfigSortException;
import espider.tri.FileSort;
import espider.webservices.amazon.Amazon;
import espider.webservices.amazon.Cover;
import espider.webservices.musicbrainz.MusicBrainz;

class Console {

    public static void main(String[] argv) {
        String command = "";
        ArrayList Mp3Files = null;
        Espider instance = new Espider();
        Config configuration = Config.getInstance();
        String directory = "";
        configuration.getProperty("shared_directory");
        System.out.println("");
        System.out.println("****************************");
        System.out.println("   Espider - Console Mode   ");
        System.out.println("****************************");
        System.out.println("");
        System.out.println("> ? for command list ");
        System.out.println("");
        while (!command.equals("quit")) {
            System.out.print("> ");
            try {
                BufferedReader entree = new BufferedReader(new InputStreamReader(System.in));
                command = entree.readLine();
            } catch (java.io.IOException e) {
                System.out.println("Erreur de lecture");
                System.exit(0);
            }
            if (command.equals("?")) {
                PrintCommand();
            } else if (command.equals("scan")) {
            } else if (command.startsWith("search ")) {
                Mp3Files = SearchSpiderFile.searchAudio(command.substring(7));
                for (int i = 0; i < Mp3Files.size(); i++) {
                    AudioSpider result = (AudioSpider) Mp3Files.get(i);
                    System.out.println("[" + i + "]" + result.getArtist() + " - " + result.getSong() + " -- " + result.getAlbum());
                }
                System.out.println("> Done");
                System.out.println("");
            } else if (command.startsWith("CountFiles")) {
                System.out.println("");
            } else if (command.startsWith("searchArtist ")) {
                ArrayList<SpiderFile> array = SearchSpiderFile.searchByArtist(command.substring(13));
                for (int i = 0; i < array.size(); i++) {
                    System.out.println(array.get(i).getPath());
                }
                System.out.println("> Done");
                System.out.println("");
            } else if (command.startsWith("searchAlbum ")) {
                for (int i = 0; i < Mp3Files.size(); i++) {
                    MP3File result = (MP3File) Mp3Files.get(i);
                    try {
                        System.out.println(result.getArtist() + " - " + result.getTitle() + " -- " + result.getAlbum());
                    } catch (ID3v2FormatException ie) {
                    }
                }
                System.out.println("> Done");
                System.out.println("");
            } else if (command.startsWith("SearchAllAlbumFor ")) {
                for (int i = 0; i < Mp3Files.size(); i++) {
                    System.out.println(Mp3Files.get(i));
                }
                System.out.println("> Done");
                System.out.println("");
            } else if (command.startsWith("searchTitle ")) {
                for (int i = 0; i < Mp3Files.size(); i++) {
                    MP3File result = (MP3File) Mp3Files.get(i);
                    try {
                        System.out.println(result.getArtist() + " - " + result.getTitle() + " -- " + result.getAlbum());
                    } catch (ID3v2FormatException ie) {
                    }
                }
                System.out.println("> Done");
                System.out.println("");
            } else if (command.startsWith("getInfo ")) {
                MP3File result = (MP3File) Mp3Files.get(Integer.parseInt(command.substring(8)));
                try {
                    System.out.println(result.getArtist() + " - " + result.getTitle() + " -- " + result.getAlbum() + " (" + result.getPath() + ")");
                    ArrayList cover = Amazon.searchAmazonItem(result.getArtist(), result.getAlbum());
                    for (int i = 0; i < cover.size(); i++) {
                        System.out.println("Cover : " + cover.get(i));
                    }
                } catch (ID3v2FormatException ie) {
                }
                System.out.println("");
                System.out.println("> Done");
                System.out.println("");
            } else if (command.startsWith("AutoCover")) {
                Cover.MassCovering(new File(directory), 1);
                System.out.println("");
                System.out.println("> Done");
                System.out.println("");
            } else if (command.startsWith("AutoTag ")) {
                MP3File result = (MP3File) Mp3Files.get(Integer.parseInt(command.substring(8)));
                System.out.println("Auto tagging : " + result.getPath());
                try {
                    File f = new File(result.getPath());
                    String trm = Mp3Trm.generateTRM(f, null, 0);
                    MusicBrainz.SearchMusicBrainz(trm);
                    MusicBrainz.SearchMBResult(result.getPath());
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                System.out.println("");
                System.out.println("> Done");
                System.out.println("");
            } else if (command.startsWith("AutoTagResult")) {
                for (int i = 0; i < Mp3Files.size(); i++) {
                    MP3File result = (MP3File) Mp3Files.get(i);
                    System.out.println("Auto tagging : " + result.getPath());
                    try {
                        File f = new File(result.getPath());
                        String trm = Mp3Trm.generateTRM(f, null, 0);
                        MusicBrainz.SearchMusicBrainz(trm);
                        MusicBrainz.SearchMBResult(result.getPath());
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
                System.out.println("");
                System.out.println("> Done");
                System.out.println("");
            } else if (command.startsWith("AmazonCover")) {
                try {
                    BufferedReader amazon = new BufferedReader(new InputStreamReader(System.in));
                    System.out.print("<Artist : > ");
                    String artist = amazon.readLine();
                    System.out.print("<Album : > ");
                    String album = amazon.readLine();
                    System.out.println("");
                    ArrayList cover = Amazon.searchAmazonItem(artist, album);
                    for (int i = 0; i < cover.size(); i++) {
                        System.out.println(cover.get(i));
                    }
                    System.out.println("> Done");
                    System.out.println("");
                } catch (java.io.IOException e) {
                    System.out.println("Erreur de lecture sur amazon");
                    System.exit(0);
                }
            } else if (command.equals("AmazonDebbug")) {
                try {
                    BufferedReader amazon = new BufferedReader(new InputStreamReader(System.in));
                    System.out.print("<Artist : > ");
                    String artist = amazon.readLine();
                    System.out.print("<Album : > ");
                    String album = amazon.readLine();
                    System.out.println("");
                    System.out.println("> " + Amazon.DebbugUrl(artist, album));
                    System.out.println("");
                } catch (java.io.IOException e) {
                    System.out.println("Erreur de lecture sur amazon");
                    System.exit(0);
                }
            } else if (command.equals("Contact")) {
                ContactList.load();
                ArrayList<Contact> contacts = ContactList.getContacts();
                Contact buddie = new Contact();
                for (int i = 0; i < contacts.size(); i++) {
                    buddie = contacts.get(i);
                    System.out.println(buddie.getIP() + " --> " + buddie.getName());
                }
            } else if (command.equals("AddContact")) {
            } else if (command.equals("DelContact")) {
                ContactList.deleteContact("1234");
            } else if (command.equals("random")) {
                SearchSpiderFile.randomAudio();
            } else if (command.equals("playlist")) {
                try {
                    System.out.println("A refaire");
                } catch (Exception e) {
                    System.out.println("Load playlist: " + e);
                }
            } else if (command.equals("rescan")) {
                File rep = new File("D:\\Perso\\mp3");
                System.out.println("Dossier " + rep.getName() + " last modified : " + rep.lastModified());
            } else if (command.equals("radio")) {
                try {
                    URL file = new URL("http://cypress.man.poznan.pl:8000/kissfm.ogg");
                    AudioInputStream in = AudioSystem.getAudioInputStream(file);
                    AudioInputStream din = null;
                    if (in != null) {
                        AudioFormat baseFormat = in.getFormat();
                        AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
                        din = AudioSystem.getAudioInputStream(decodedFormat, in);
                        rawplay(decodedFormat, din);
                        in.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (command.equals("trier")) {
                FileSort fs = new FileSort();
                try {
                    fs.sort();
                } catch (ConfigSortException e1) {
                    e1.printStackTrace();
                }
            } else if (command.equals("update")) {
            } else if (command.equals("addF")) {
                Favorite.add("test");
            } else if (command.equals("delF")) {
                Favorite.remove("test");
            } else if (command.equals("isF")) {
                if (Favorite.isFavorite("test")) {
                    System.out.println("is f");
                } else {
                    System.out.println("is not f");
                }
            }
        }
    }

    private static void rawplay(AudioFormat targetFormat, AudioInputStream din) throws IOException, LineUnavailableException {
        byte[] data = new byte[4096];
        SourceDataLine line = getLine(targetFormat);
        if (line != null) {
            line.start();
            int nBytesRead = 0;
            while (nBytesRead != -1) {
                nBytesRead = din.read(data, 0, data.length);
            }
            line.drain();
            line.stop();
            line.close();
            din.close();
        }
    }

    private static SourceDataLine getLine(AudioFormat audioFormat) throws LineUnavailableException {
        SourceDataLine res = null;
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        res = (SourceDataLine) AudioSystem.getLine(info);
        res.open(audioFormat);
        return res;
    }

    public static void PrintCommand() {
        System.out.println("");
        System.out.println("--------------");
        System.out.println("Command list :");
        System.out.println("--------------");
        System.out.println("");
        System.out.println("> quit");
        System.out.println("> scan");
        System.out.println("> CountFiles");
        System.out.println("> search <item>");
        System.out.println("> searchAlbum <item>");
        System.out.println("> searchTitle <item>");
        System.out.println("> searchArtist <item>");
        System.out.println("");
        System.out.println("> AutoTag <index>");
        System.out.println("> getInfo <index>");
        System.out.println("");
        System.out.println("> AmazonCover");
        System.out.println("> AmazonTracks");
        System.out.println("> AmazonDebbug");
        System.out.println("--------------");
        System.out.println("");
    }
}
