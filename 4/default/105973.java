import java.util.logging.Level;
import java.util.logging.Logger;
import org.blinkenlights.jid3.MediaFile;
import org.blinkenlights.jid3.MP3File;
import org.blinkenlights.jid3.v2.*;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.blinkenlights.jid3.ID3Tag;

public class ImageTagger {

    public static void writeImage(File image, File mp3) {
        MediaFile oMediaFile = new MP3File(mp3);
        ID3V2_3_0Tag tag = null;
        try {
            tag = (ID3V2_3_0Tag) oMediaFile.getID3V2Tag();
        } catch (Exception e) {
        } finally {
            if (tag == null) {
                tag = new ID3V2_3_0Tag();
            }
        }
        try {
            InputStream s = new FileInputStream(image);
            FileInputStream in = new FileInputStream(image);
            FileChannel fc = in.getChannel();
            byte[] data = new byte[(int) fc.size()];
            ByteBuffer bb = ByteBuffer.wrap(data);
            fc.read(bb);
            String imageType = image.getName().substring(image.getName().lastIndexOf(".") + 1);
            String mimeType = imageType.equalsIgnoreCase("jpg") ? "image/jpeg" : "image/" + imageType;
            APICID3V2Frame picFrame = new APICID3V2Frame(mimeType, APICID3V2Frame.PictureType.FrontCover, "Frontcover", data);
            tag.addAPICFrame(picFrame);
            oMediaFile.setID3Tag(tag);
            oMediaFile.sync();
            System.out.println("Tag written");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static BufferedImage readImage(File mp3) {
        MediaFile oMediaFile = new MP3File(mp3);
        try {
            if (oMediaFile.getID3V2Tag() instanceof ID3V2_3_0Tag) {
                APICID3V2Frame picFrame = null;
                ID3V2_3_0Tag tag = (ID3V2_3_0Tag) oMediaFile.getID3V2Tag();
                APICID3V2Frame[] picFrames = tag.getAPICFrames();
                for (int i = 0; i < picFrames.length; i++) {
                    if (picFrames[i].getPictureType().equals(APICID3V2Frame.PictureType.FrontCover)) {
                        picFrame = picFrames[i];
                        break;
                    } else {
                        picFrame = picFrames[i];
                    }
                }
                if (picFrame != null) return ImageIO.read(new ByteArrayInputStream(picFrame.getPictureData()));
            } else System.out.println(oMediaFile.getTags().length);
        } catch (org.blinkenlights.jid3.ID3Exception e) {
            return null;
        } catch (IOException e) {
            System.out.println("ID3tag image data corrupt, or of unknown type");
            e.printStackTrace();
            return null;
        }
        return null;
    }

    public static void deleteImage(File mp3) {
        MediaFile oMediaFile = new MP3File(mp3);
        try {
            ID3Tag[] tags = oMediaFile.getTags();
            for (int j = 0; j < tags.length; j++) {
                if (tags[j] instanceof ID3V2_3_0Tag) {
                    ID3V2_3_0Tag tag = (ID3V2_3_0Tag) oMediaFile.getID3V2Tag();
                    APICID3V2Frame[] picFrames = tag.getAPICFrames();
                    for (int i = 0; i < picFrames.length; i++) {
                        if (picFrames[i].getPictureType().equals(APICID3V2Frame.PictureType.FrontCover)) {
                            tag.removeAPICFrame(picFrames[i].getDescription());
                            System.out.println("We got one!");
                        }
                    }
                    oMediaFile.removeID3V2Tag();
                    if (tag.containsAtLeastOneFrame()) oMediaFile.setID3Tag(tag);
                    oMediaFile.sync();
                    break;
                }
            }
        } catch (org.blinkenlights.jid3.ID3Exception e) {
            e.printStackTrace();
        }
    }

    public static File getImageFile(File mp3) {
        try {
            MediaFile oMediaFile = new MP3File(mp3);
            File tempFile = File.createTempFile(mp3.getName().substring(0, mp3.getName().lastIndexOf(".")), "png");
            BufferedImage image = null;
            try {
                if (oMediaFile.getID3V2Tag() instanceof ID3V2_3_0Tag) {
                    APICID3V2Frame picFrame = null;
                    ID3V2_3_0Tag tag = (ID3V2_3_0Tag) oMediaFile.getID3V2Tag();
                    APICID3V2Frame[] picFrames = tag.getAPICFrames();
                    for (int i = 0; i < picFrames.length; i++) {
                        if (picFrames[i].getPictureType().equals(APICID3V2Frame.PictureType.FrontCover)) {
                            picFrame = picFrames[i];
                            break;
                        } else {
                            picFrame = picFrames[i];
                        }
                    }
                    if (picFrame != null) {
                        image = ImageIO.read(new ByteArrayInputStream(picFrame.getPictureData()));
                    }
                    ImageIO.write(image, "png", tempFile);
                    return tempFile;
                } else {
                    System.out.println(oMediaFile.getTags().length);
                }
            } catch (org.blinkenlights.jid3.ID3Exception e) {
                return null;
            }
            return null;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
