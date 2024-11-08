package fr.bibiche.test.mediaInfos;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import java.io.File;
import org.junit.Test;
import fr.bibiche.mediaInfos.bean.Infos;
import fr.bibiche.mediaInfos.exceptions.MediaInfosException;

/**
 * <b>Auteur : </b> FT/NCPI/DPS/DDP <br>
 * <b>Fichier : </b> Execution.java <b>du projet</b> MediaInfo <br>
 * <b>Date de création :</b> 22 avr. 10 <br>
 * <b>Description : </b>Test total<br>
 */
public class MediaInfos {

    /**
     * 
     */
    @Test
    public void mediaInfos() {
        try {
            Infos infos = fr.bibiche.mediaInfos.MediaInfos.getMediaInfos(new File(getClass().getResource("/test.avi").getPath()));
            assertEquals("320", infos.getVideo().getWidth());
            assertEquals("240", infos.getVideo().getHeight());
            assertEquals("Progressive", infos.getVideo().getScanType());
            assertEquals("MPEG Audio", infos.getAudio().getFormat());
            assertEquals("2", infos.getAudio().getChannel());
        } catch (MediaInfosException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
}
