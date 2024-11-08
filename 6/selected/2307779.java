package com.scholardesk.email;

import com.scholardesk.config.Config;
import com.scholardesk.template.EmailTemplate;

/**
 * Handles all configuration and initialization of classes to support
 * then sending of email messages using Velocity templates or just
 * sending a message without a template.
 * 
 * @author Christopher M. Dunavant
 *
 */
public class Email {

    private final Config m_config = Config.getInstance();

    private final String m_template_path = m_config.getString("config_path") + m_config.getString("email.config_path");

    private EmailMessage m_message = new EmailMessage();

    private EmailTemplate m_template;

    private String m_template_file;

    /**
	 * Constructor.  Use for email's that don't require a template.
	 */
    public Email() {
    }

    /**
	 * Constructor. Use for email's that use a Velocity template.
	 * 
	 * NOTE: Template file location is determined by configuration
	 * options in the Config object.
	 * 
	 * @param template_file name of template file for this email.
	 * @throws Exception
	 */
    public Email(String template_file) throws Exception {
        m_template = new EmailTemplate(m_template_path);
        m_template_file = template_file;
    }

    /**
	 * Configures the {@link Smtp} object and performs the three 
	 * methods required for sending an email.
	 * 
	 * connect, login (optional) and send.
	 * 
	 * NOTE: All connection properties are found in a Config object.
	 * 
	 * @throws Exception
	 */
    public void send() throws Exception {
        initTemplate();
        Smtp smtp = new Smtp();
        smtp.connect(m_config.getString("smtp.server"), Integer.parseInt(m_config.getString("smtp.port")), Boolean.parseBoolean(m_config.getString("smtp.ssl")), Boolean.parseBoolean(m_config.getString("smtp.auth")));
        if (m_config.getString("smtp.username") != null) {
            smtp.login(m_config.getString("smtp.username"), m_config.getString("smtp.password"));
        }
        smtp.send(m_message);
    }

    /**
	 * Allows caller to add objects to the context of the Velocity
	 * template. Useful for adding data objects to the context so that
	 * the email can be customized with variable data.
	 * 
	 * @param _key name of attribute to add to template context.
	 * @param _object
	 */
    public void addContext(String _key, Object _object) {
        m_template.addContext(_key, _object);
    }

    /**
	 * <p>
	 * Sets receiver's "to" address(es) for email message. Must be RFC822.
	 * </p>
	 * - multiple addresses must be comma-separated <br/>
	 * - you can use the two different personal name formats </br>
	 *   - <code>address "(" display-name ")"</code> <br/>
	 *   - <code>display-name "&lt;" address "&gt;"</code> <br/>
	 * - also the group form <code>display-name ":" address, address, etc.</code><br/>
	 * 
	 * @param _to email address to send message to.
	 */
    public void setTo(String _to) {
        if (_to != null) m_message.setTo(_to);
    }

    /**
	 * <p>
	 * Returns an RFC822 compliant "to" address for a full name.
	 * </p>
	 * @param _to email address.
	 * @param _name full name or to address recipient.
	 * 
	 * @return rfc822 compliant to address.
	 */
    public static String buildToWithName(String _to, String _name) {
        if (_name != null) return "\"" + _name + "\" <" + _to + ">";
        return _to;
    }

    /**
	 * Sets blind carbon-copy address(es) for email message.
	 * 
	 * NOTE: multiple addresses must be comma-separated.
	 * 
	 * @param _bcc email address to blind copy.
	 */
    public void setBcc(String _bcc) {
        if (_bcc != null) m_message.setBcc(_bcc);
    }

    /**
	 * Sets the from address for email message.
	 * 
	 * @param _from the sender's email address.
	 */
    public void setFrom(String _from) {
        if (_from != null) m_message.setFrom(_from);
    }

    /**
	 * Sets the email sender's name.
	 * 
	 * @param _from_name the email sender's name.
	 */
    public void setFromName(String _from_name) {
        if (_from_name != null) m_message.setFromName(_from_name);
    }

    /**
	 * Sets the subject of the email.
	 * 
	 * @param _subject subject of the email.
	 */
    public void setSubject(String _subject) {
        if (_subject != null) m_message.setSubject(_subject);
    }

    /**
	 * Sets the address for email message replies.
	 * 
	 * @param _reply_to the sender's email address.
	 */
    public void setReplyTo(String _reply_to) {
        if (_reply_to != null) m_message.setReplyTo(_reply_to);
    }

    /**
	 * Sets the main "body" content of the email message.
	 * 
	 * @param _content a string containing the body of the message.
	 */
    public void setContent(String _content) {
        if (_content != null) m_message.setContent(_content);
    }

    /**
	 * Sets the MIME Content-type of the message.  NOTE: The default
	 * in the {@link EmailMessage} class is "text/plain".
	 * 
	 * @param _content_type
	 */
    public void setContentType(String _content_type) {
        if (_content_type != null) m_message.setContentType(_content_type);
    }

    /**
	 * Initializes the velocity template if a template is being used.
	 *
	 * It will also set some of the Email specific attributes by
	 * checking the template for "pre-defined" or "hard-coded" 
	 * email attributes.  For example: you can set a hard-coded
	 * subject for an email in the template with the following:
	 * 
	 * #set( $subject = "Notification: $program.shortTitle" )
	 */
    private void initTemplate() {
        if (m_template_file == null) return;
        setContent(m_template.merge(m_template_file));
        setTo(m_template.getEmailTo());
        setBcc(m_template.getEmailBcc());
        setFrom(m_template.getEmailFrom());
        setFromName(m_template.getEmailFromName());
        setSubject(m_template.getEmailSubject());
    }
}
