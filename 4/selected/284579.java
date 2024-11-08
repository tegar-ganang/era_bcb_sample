package es.rvp.java.simpletag.core.metadata;

import org.apache.log4j.Logger;
import es.rvp.java.simpletag.core.exceptions.SettingValueTagException;
import es.rvp.java.simpletag.core.internacionalization.CoreMessages;
import es.rvp.java.simpletag.core.types.IDMetadataTypes;

/**
 * Representa el numero de canales de la cancion (stero, dolby ..etc).
 *
 * @author Rodrigo Villamil Perez
 */
public class MetadataChannels extends AbstractJaudioTaggerMetadata<String> {

    protected static final Logger LOGGER = Logger.getLogger(MetadataChannels.class);

    public MetadataChannels() {
        super(IDMetadataTypes.Channels);
        this.readOnly = true;
    }

    /**
	 * Retorna el valor asociado o null si no existe el campo.
	 */
    @Override
    public String getValue() {
        try {
            return this.getAudioHeader().getChannels();
        } catch (final Exception ex) {
            MetadataChannels.LOGGER.warn("[MetadataChannels - getValue] cannot get Channels info");
        }
        return null;
    }

    @Override
    public void setValue(final String value) throws SettingValueTagException {
        throw new SettingValueTagException(CoreMessages.getInstance().getMessage("metadata.readonly") + value);
    }
}
