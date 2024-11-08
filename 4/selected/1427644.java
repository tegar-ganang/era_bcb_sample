package fr.bibiche.tvixieAddOn.utils;

import org.apache.log4j.Logger;
import fr.bibiche.mediaInfos.bean.Infos;
import fr.bibiche.tvixieAddOn.bean.MovieInfo;

/**
 * <b>Auteur : </b> FT/NCPI/DPS/DDP <br>
 * <b>Fichier : </b> MediaInfosTvixieInfosTransferer.java <b>du projet</b>
 * TvixieAddOn <br>
 * <b>Date de création :</b> 23 avr. 10 <br>
 * <b>Description : </b>Passage des infos de MediaInfos à TvixieInfos<br>
 */
public class MediaInfosTvixieInfosTransferer {

    private static final Logger LOG = Logger.getLogger(MediaInfosTvixieInfosTransferer.class);

    /**
     * Passage des infos de MediaInfos à TvixieInfos
     * @param mediaInfos
     * @param tvixieInfos
     * @param overwrite
     */
    public static final void transfert(Infos mediaInfos, MovieInfo tvixieInfos, boolean overwrite) {
        if ((tvixieInfos.getResolutions() == null || "".equals(tvixieInfos.getResolutions())) || overwrite) {
            tvixieInfos.setResolutions(getResolution(mediaInfos));
        }
        if ((tvixieInfos.getSoundFormats() == null || "".equals(tvixieInfos.getSoundFormats())) || overwrite) {
            tvixieInfos.setSoundFormats(getSoundFormat(mediaInfos));
        }
        if ((tvixieInfos.getRuntime() == null || "".equals(tvixieInfos.getRuntime())) || overwrite) {
            tvixieInfos.setRuntime(getDuration(mediaInfos));
        }
        if ((tvixieInfos.getMediaFormats() == null || "".equals(tvixieInfos.getMediaFormats()) || overwrite) && mediaInfos.isBlurayFormat()) {
            tvixieInfos.setMediaFormats("BLURAY");
        }
    }

    private static final String getResolution(Infos mediaInfos) {
        LOG.debug("recuperation de la resolution");
        String res = null;
        int width = mediaInfos.getVideo().getWidth();
        if ((width == 1080 * 16 / 9 || width == 720 * 16 / 9 || width == 576 * 16 / 9) && ("Progressive".equals(mediaInfos.getVideo().getScanType()) || "Interlaced".equals(mediaInfos.getVideo().getScanType()))) {
            res = Integer.toString(width * 9 / 16) + mediaInfos.getVideo().getScanType().substring(0, 1);
            LOG.debug("resolution : " + res);
        } else {
            LOG.info("pas de resolution : largeur : " + width + " | Scan : " + mediaInfos.getVideo().getScanType());
        }
        return res;
    }

    private static final String getSoundFormat(Infos mediaInfos) {
        LOG.debug("recuperation du format de son");
        String res = null;
        String sound = mediaInfos.getAudio().getFormat();
        int channel = mediaInfos.getAudio().getChannel();
        if ("AC-3".equals(sound)) {
            res = "DD";
        } else if ("DTS".equals(sound)) {
            res = "DTS";
        }
        if (res != null) {
            if (channel == 2 && !"DTS".equals(res)) {
                res += "20";
            } else if (channel == 6) {
                res += "51";
            } else if (channel == 8) {
                res += "71";
            } else {
                res = null;
            }
            LOG.debug("format de son : " + res);
        } else {
            LOG.info("format de son non recupere : " + sound + " | " + channel);
        }
        return res;
    }

    private static final String getDuration(Infos mediaInfos) {
        LOG.debug("recuperation de la durée");
        String ret = null;
        float duree = mediaInfos.getVideo().getDuration();
        if (duree > 0) {
            LOG.debug(duree);
            LOG.debug(duree / 60000);
            ret = Integer.toString(Math.round(duree / 60000));
            LOG.debug("duree : " + ret);
        }
        return ret;
    }
}
