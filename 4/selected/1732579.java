package geisler.projekt.game.util;

import geisler.projekt.game.constants.EnumObjectType;
import geisler.projekt.game.interfaces.DrawableObj;
import geisler.projekt.game.model.Ebene2D;
import geisler.projekt.game.model.Map2D;
import geisler.projekt.game.model.NonPlayingCharacter;
import geisler.projekt.game.model.Player;
import geisler.projekt.game.model.Ressources;
import geisler.projekt.game.model.Tile2D;
import geisler.projekt.game.model.TileAnimation2D;
import java.awt.Component;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.ByteChannel;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Helper Klasse die beim Lesen und Schreiben
 * von Dateien Methoden bereitstellt, um
 * alle ben�tigten Objekte des Spielablaufs 
 * erstellen und speichern zu k�nnen.
 * 
 * @author Geislern
 *
 */
public class ReadAndWriteHelper extends Component {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private static Log LOG = LogFactory.getLog(ReadAndWriteHelper.class);

    private static Ressources callerr;

    /**
	 * Liefert ein Map2D-Objekt zur�ck 
	 * welches aus einer Map-File gelesen wird.
	 * 
	 * @param mapFile {@link File}
	 * @param caller {@link Ressources}
	 * @return {@link Map2D} 
	 */
    public static Map2D readMap(File mapFile, Ressources caller) {
        if (callerr == null) {
            callerr = caller;
        }
        InputStream fis = null;
        Map2D map = null;
        try {
            fis = new FileInputStream(mapFile);
            ObjectInputStream o = new ObjectInputStream(fis);
            map = (Map2D) o.readObject();
            List<DrawableObj> toDelete = new ArrayList<DrawableObj>();
            for (EnumObjectType type : EnumObjectType.values()) {
                for (Ebene2D ebene : map.getObjs().get(type)) {
                    for (DrawableObj drawObj : ebene.getListeObjs()) {
                        drawObj.setSelected(false);
                        if (drawObj instanceof TileAnimation2D) {
                            TileAnimation2D ta2D = (TileAnimation2D) drawObj;
                            for (Tile2D taTile : ta2D.getPics()) {
                                taTile.setTileImage(caller.setTile(new Point((int) taTile.getTileSetX(), (int) taTile.getTileSetY()), taTile.getTileSet()));
                            }
                            caller.getListAnimationsMove().add(ta2D);
                        } else if (drawObj instanceof Tile2D) {
                            Tile2D tile = (Tile2D) drawObj;
                            tile.setTileImage(caller.setTile(new Point((int) tile.getTileSetX(), (int) tile.getTileSetY()), tile.getTileSet()));
                        } else if (drawObj instanceof NonPlayingCharacter) {
                            NonPlayingCharacter npc = (NonPlayingCharacter) drawObj;
                            npc.setHp(20);
                            npc.setStrength(10);
                            npc.setDeffence(5);
                            boolean npcExists = false;
                            for (NonPlayingCharacter listNpc : caller.getListNPCs()) {
                                if (npc.getId() == listNpc.getId() && npc.getName().equals(listNpc.getName())) {
                                    npcExists = true;
                                    break;
                                }
                            }
                            if (npcExists) {
                                npc.setHeads(loadPics(npc.getPicNumber(), 12));
                                npc.setPics(loadPics(npc.getPicNumber() + 32, 12));
                                caller.getListSpritesMove().add(npc);
                            } else {
                                toDelete.add(drawObj);
                            }
                        }
                    }
                    for (DrawableObj drawableObj : toDelete) {
                        ebene.getListeObjs().remove(drawableObj);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(99);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(99);
        }
        return map;
    }

    /**
	 * Schreibt ein
	 * {@link Map2D} - Objekt in eine Datei
	 * unter dem angegbenen {@link URL} - Pfad
	 * 
	 * @param map {@link Map2D}
	 * @param fileURI {@link URL}
	 */
    public static void writeMap(Map2D map, URL fileURI) {
        File outputMap = null;
        OutputStream fos = null;
        try {
            URL url = fileURI;
            outputMap = new File(url.toURI());
        } catch (URISyntaxException e) {
            try {
                outputMap = File.createTempFile("Map" + map.getMapId(), "ser", new File(fileURI.getPath()));
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        try {
            fos = new FileOutputStream(outputMap);
            ObjectOutputStream o = new ObjectOutputStream(fos);
            o.writeObject(map);
        } catch (Exception e2) {
            e2.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
	 * Liefert ein {@link Player} - Objekt zur�ck welches
	 * aus einer Player-File gelesen wird.
	 * 
	 * @param caller {@link Ressources}
	 * @return {@link Player} 
	 */
    public static Player readPlayerFile(Ressources caller) {
        if (callerr == null) {
            callerr = caller;
        }
        Player player = null;
        InputStream fis = null;
        try {
            File playerData = new File(caller.getLocalUSER_HOME_PATH().toURI().getPath() + "Player_Default.ser");
            fis = new FileInputStream(playerData);
            ObjectInputStream o = new ObjectInputStream(fis);
            player = (Player) o.readObject();
            player.setHeads(loadPics(player.getPicNumber(), 12));
            player.setPics(loadPics(player.getPicNumber() + 32, 12));
            player.setDelay(150);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return player;
    }

    /**
	 * Schreibt ein
	 * {@link Player} - Objekt in eine Datei
	 * mit Informationen aus dem {@link Ressources} - Objekt.
	 * 
	 * @param player {@link Player}
	 * @param caller {@link Ressources}
	 */
    public static void writePlayer(Player player, Ressources caller) {
        OutputStream fos = null;
        File outputPlayer = null;
        try {
            URL url = new URL("file", null, caller.getLocalUSER_HOME_PATH().getFile() + "Player_Default.ser");
            outputPlayer = new File(url.toURI());
            if (!outputPlayer.exists()) {
                LOG.info("Pfad der Player_Datei:" + url.getPath());
                outputPlayer.createNewFile();
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            fos = new FileOutputStream(outputPlayer);
            ObjectOutputStream o = new ObjectOutputStream(fos);
            o.writeObject(player);
        } catch (Exception e2) {
            e2.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
	 * Liefert ein {@link TileAnimation2D} - Objekt zur�ck welches
	 * aus einer Animation-File gelesen wird.
	 * 
	 * @param animFile {@link File}
	 * @param caller {@link Ressources}
	 * @return {@link TileAnimation2D} 
	 */
    public static TileAnimation2D readAnimationFile(File animFile, Ressources caller) {
        if (callerr == null) {
            callerr = caller;
        }
        InputStream fis = null;
        TileAnimation2D anim = null;
        try {
            File animData = animFile;
            fis = new FileInputStream(animData);
            ObjectInputStream o = new ObjectInputStream(fis);
            anim = (TileAnimation2D) o.readObject();
            for (Tile2D animTile : anim.getPics()) {
                animTile.setTileImage(caller.setTile(new Point((int) animTile.getTileSetX(), (int) animTile.getTileSetY()), animTile.getTileSet()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return anim;
    }

    /**
	 * Schreibt ein
	 * {@link TileAnimation2D} - Objekt in eine Datei
	 * mit Informationen aus dem {@link Ressources} - Objekt.
	 * 
	 * @param anim {@link TileAnimation2D}
	 * @param caller {@link Ressources}
	 */
    public static void writeAnimation(TileAnimation2D anim, Ressources caller) {
        if (callerr == null) {
            callerr = caller;
        }
        OutputStream fos = null;
        File outputAnim = null;
        try {
            URL url = callerr.getLocalUSER_HOME_PATH().toURI().toURL();
            url = new URL(url, "anims/");
            outputAnim = new File(url.toURI());
            outputAnim = new File(outputAnim.getPath() + "/Anim_" + anim.getName() + ".ser");
            if (!outputAnim.exists()) {
                outputAnim.createNewFile();
            }
            fos = new FileOutputStream(outputAnim);
            ObjectOutputStream o = new ObjectOutputStream(fos);
            o.writeObject(anim);
        } catch (Exception e2) {
            e2.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
	 * Liefert ein {@link NonPlayingCharacter} - Objekt zur�ck welches 
	 * aus einer NPC-File gelesen wird.
	 * 
	 * @param npcFile {@link File}
	 * @param caller {@link Ressources}
	 * @return {@link NonPlayingCharacter} 
	 */
    public static NonPlayingCharacter readNpcFile(File npcFile, Ressources caller) {
        if (callerr == null) {
            callerr = caller;
        }
        NonPlayingCharacter npc = null;
        InputStream fis = null;
        try {
            File npcData = npcFile;
            fis = new FileInputStream(npcData);
            ObjectInputStream o = new ObjectInputStream(fis);
            npc = (NonPlayingCharacter) o.readObject();
            npc.setHeads(loadPics(npc.getPicNumber(), 12));
            npc.setPics(loadPics(npc.getPicNumber() + 32, 12));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return npc;
    }

    /**
	 * Schreibt ein
	 * {@link NonPlayingCharacter} - Objekt in eine Datei
	 * mit Informationen aus dem {@link Ressources} - Objekt.
	 * 
	 * @param npc {@link NonPlayingCharacter}
	 * @param caller {@link Ressources}
	 */
    public static void writeNpcFile(NonPlayingCharacter npc, Ressources caller) {
        if (callerr == null) {
            callerr = caller;
        }
        OutputStream fos = null;
        File outputNpc = null;
        try {
            URL url = callerr.getLocalUSER_HOME_PATH().toURI().toURL();
            url = new URL(url, "npcs/");
            outputNpc = new File(url.toURI());
            outputNpc = new File(outputNpc.getPath() + "/Npc_" + npc.getName() + ".ser");
            if (!outputNpc.exists()) {
                outputNpc.createNewFile();
            }
            fos = new FileOutputStream(outputNpc);
            ObjectOutputStream o = new ObjectOutputStream(fos);
            o.writeObject(npc);
        } catch (Exception e2) {
            e2.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static BufferedImage[] loadPics(int spriteNr, int pics) {
        BufferedImage[] anim = new BufferedImage[pics];
        BufferedImage source = callerr.getSprites64();
        for (int x = 0; x < pics; x++) {
            anim[x] = source.getSubimage(x * source.getWidth() / pics, spriteNr, 32, 32);
        }
        return anim;
    }

    /**
	 * @param source
	 * @param destination
	 */
    public static void copy(File source, File destination) {
        try {
            FileInputStream fileInputStream = new FileInputStream(source);
            FileOutputStream fileOutputStream = new FileOutputStream(destination);
            FileChannel inputChannel = fileInputStream.getChannel();
            FileChannel outputChannel = fileOutputStream.getChannel();
            transfer(inputChannel, outputChannel, source.length(), 1024 * 1024 * 32, true, true);
            fileInputStream.close();
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param fileChannel
     * @param byteChannel
     * @param lengthInBytes
     * @param chunckSizeInBytes
     * @param verbose
     * @param fromFile
     * @throws IOException
     */
    public static void transfer(FileChannel fileChannel, ByteChannel byteChannel, long lengthInBytes, long chunckSizeInBytes, boolean verbose, boolean fromFile) throws IOException {
        long overallBytesTransfered = 0L;
        long time = -System.currentTimeMillis();
        while (overallBytesTransfered < lengthInBytes) {
            long bytesTransfered = 0L;
            if (fromFile) {
                bytesTransfered = fileChannel.transferTo(0, Math.min(chunckSizeInBytes, lengthInBytes - overallBytesTransfered), byteChannel);
            } else {
                bytesTransfered = fileChannel.transferFrom(byteChannel, overallBytesTransfered, Math.min(chunckSizeInBytes, lengthInBytes - overallBytesTransfered));
            }
            overallBytesTransfered += bytesTransfered;
            if (verbose) {
            }
        }
        time += System.currentTimeMillis();
        if (verbose) {
        }
    }
}
