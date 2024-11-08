package net.sf.webwarp.modules.partner.ui.trinidad;

import java.io.Serializable;
import java.util.Locale;
import org.joda.time.DateTime;
import org.springframework.context.MessageSource;
import net.sf.webwarp.modules.partner.address.ContactChannel;
import net.sf.webwarp.modules.partner.address.ContactChannelType;

public class ContactChannelDisplayBean implements Serializable {

    private static final long serialVersionUID = -1443917857433418485L;

    public static final Class[] constructorSignature = new Class[] { ContactChannel.class, ContactChannelType.class, MessageSource.class, Locale.class };

    private ContactChannel contactChannel;

    private ContactChannelType channelType;

    protected MessageSource messageSource;

    protected Locale locale;

    public ContactChannelDisplayBean(ContactChannel contactChannel, ContactChannelType channelType, MessageSource messageSource, Locale locale) {
        this.contactChannel = contactChannel;
        this.channelType = channelType;
        this.messageSource = messageSource;
        this.locale = locale;
    }

    public String getChannel() {
        return contactChannel.getChannel();
    }

    public DateTime getCreatedAt() {
        return contactChannel.getCreatedAt();
    }

    public String getCreatedFrom() {
        return contactChannel.getCreatedBy();
    }

    public Long getId() {
        return contactChannel.getId();
    }

    public String getType() {
        return channelType.getName();
    }

    public String getTypeName() {
        return messageSource.getMessage(channelType.getName(), null, channelType.getName(), locale);
    }

    public DateTime getUpdatedAt() {
        return contactChannel.getUpdatedAt();
    }

    public String getUpdatedFrom() {
        return contactChannel.getUpdatedBy();
    }

    public boolean isDeleted() {
        return contactChannel.isDeleted();
    }

    public boolean isPreferred() {
        return contactChannel.isPreferred();
    }

    public void setChannel(String channel) {
        contactChannel.setChannel(channel);
    }

    public void setPreferred(boolean preferred) {
        contactChannel.setPreferred(preferred);
    }
}
