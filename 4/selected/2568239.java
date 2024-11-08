package org.opencms.newsletter;

/**
 * Content for newsletters.<p>
 * 
 * @author Alexander Kandzior
 * @author Jan Baudisch
 */
public class CmsNewsletterContent implements I_CmsNewsletterContent {

    /** The output channel for this content. */
    String m_channel;

    /** The content String. */
    String m_content;

    /** The order of this content. */
    int m_order;

    /** The type of this content. */
    CmsNewsletterContentType m_type;

    /**
     * Creates a new CmsNewsletterContent instance.<p>
     * 
     * @param order the order of the newsletter content
     * @param content the content 
     * @param type the newsletter contents' type
     */
    public CmsNewsletterContent(int order, String content, CmsNewsletterContentType type) {
        m_order = order;
        m_content = content;
        m_type = type;
        m_channel = "";
    }

    /**
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object o) {
        return new Integer(m_order).compareTo(new Integer(((CmsNewsletterContent) o).getOrder()));
    }

    /**
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof CmsNewsletterContent)) {
            return false;
        }
        CmsNewsletterContent newsletterContent = (CmsNewsletterContent) obj;
        if (getOrder() != newsletterContent.getOrder()) {
            return false;
        }
        if (!getContent().equals(newsletterContent.getContent())) {
            return false;
        }
        if (!getChannel().equals(newsletterContent.getChannel())) {
            return false;
        }
        if (!getType().equals(newsletterContent.getType())) {
            return false;
        }
        return true;
    }

    /**
     * 
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return m_channel.hashCode() + m_content.hashCode() + m_order + m_type.hashCode();
    }

    /**
     * @see org.opencms.newsletter.I_CmsNewsletterContent#getChannel()
     */
    public String getChannel() {
        return m_channel;
    }

    /**
     * @see org.opencms.newsletter.I_CmsNewsletterContent#getContent()
     */
    public String getContent() {
        return m_content;
    }

    /**
     * @see org.opencms.newsletter.I_CmsNewsletterContent#getOrder()
     */
    public int getOrder() {
        return m_order;
    }

    /**
     * @see org.opencms.newsletter.I_CmsNewsletterContent#getType()
     */
    public CmsNewsletterContentType getType() {
        return m_type;
    }
}
