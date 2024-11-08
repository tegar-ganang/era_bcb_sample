package br.com.javaplanet.util;

import br.com.javaplanet.data.FeedContentVo;
import de.nava.informa.core.ItemIF;

/**
 * Created by IntelliJ IDEA.
 * User: Rodrigo
 * Date: 10/10/2006
 * Time: 15:40:47
 * To change this template use File | Settings | File Templates.
 */
public class ContentHelper {

    public static FeedContentVo buildContent(final ItemIF it) {
        final FeedContentVo fc = new FeedContentVo();
        fc.setDateTime(it.getDate() == null ? it.getFound() : it.getDate());
        fc.setAuthor(it.getChannel().getCopyright());
        fc.setContent(it.getDescription());
        fc.setLink(it.getLink().toString());
        fc.setSiteDescription(it.getChannel().getDescription());
        if (it.getChannel().getImage() != null) {
            fc.setSiteImage(it.getChannel().getImage().getLink().toString());
        }
        fc.setSiteLink(it.getChannel().getSite().toString());
        fc.setSiteName(it.getChannel().getTitle());
        fc.setTitle(it.getTitle());
        fc.setLink(it.getLink().toString());
        return fc;
    }
}
