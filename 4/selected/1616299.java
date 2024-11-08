package com.acv.service.emailEngine;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.mail.MessagingException;
import org.apache.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.springframework.mail.javamail.MimeMessageHelper;
import com.acv.dao.catalog.Media;
import com.acv.dao.catalog.categories.promotions.model.Promotion;
import com.acv.dao.catalog.categories.promotions.model.PromotionI18n;
import com.acv.dao.catalog.categories.promotions.model.PromotionRules;
import com.acv.dao.common.Constants;
import com.acv.dao.emailEngine.model.EmailAttachment;
import com.acv.dao.emailEngine.model.EmailField;
import com.acv.dao.emailEngine.model.EmailQueue;
import com.acv.dao.templates.model.VelocityTemplate;
import com.acv.service.catalog.CategoryManager;
import com.acv.service.common.exception.ObjectNotFoundException;
import com.acv.service.configuration.exception.ConfigurationException;
import com.acv.service.emailEngine.exception.EmailEngineException;
import com.acv.service.templates.VelocityTemplateManager;
import com.acv.service.urlrewriter.EncodeFriendlyUrl;
import com.acv.service.urlrewriter.UrlMapperManager;

/**
 * Class in charge of verifying templates availability and populating emails
 * using the velocity engine.
 *
 * @author Mickael Guesnon
 */
public class EmailTemplater {

    private VelocityEngine engine = null;

    private UrlMapperManager urlMapperManager;

    private CategoryManager categoryManager;

    private VelocityTemplateManager velocityTemplateManager;

    public static final Integer MAX_PROMOTION_IN_EMAIL_RIGHT_COLUMN = 2;

    public static final String PROMOTION_MARKETING_SPOT_FOR_EMAILS = "MS_EMAIL";

    public static final String MEDIA_URI = "MEDIA_URI";

    private static final Logger log = Logger.getLogger(EmailTemplater.class);

    /**
	 * Sets the velocity engine.
	 *
	 * @param engine
	 *            the new velocity engine
	 */
    public void setVelocityEngine(VelocityEngine engine) {
        this.engine = engine;
    }

    private static final String[] SPECIAL_ENTITIES = { "&quot;", "&amp;", "&lt;", "&gt;", "&nbsp;", "&iexcl;", "&cent;", "&pound;", "&curren;", "&yen;", "&brvbar;", "&sect;", "&uml;", "&copy;", "&ordf;", "&laquo;", "&not;", "&shy;", "&reg;", "&macr;", "&deg;", "&plusmn;", "&sup2;", "&sup3;", "&acute;", "&micro;", "&para;", "&middot;", "&cedil;", "&sup1;", "&ordm;", "&raquo;", "&frac14;", "&frac12;", "&frac34;", "&iquest;", "&Agrave;", "&Aacute;", "&Acirc;", "&Atilde;", "&Auml;", "&Aring;", "&AElig;", "&Ccedil;", "&Egrave;", "&Eacute;", "&Ecirc;", "&Euml;", "&Igrave;", "&Iacute;", "&Icirc;", "&Iuml;", "&ETH;", "&Ntilde;", "&Ograve;", "&Oacute;", "&Ocirc;", "&Otilde;", "&Ouml;", "&times;", "&Oslash;", "&Ugrave;", "&Uacute;", "&Ucirc;", "&Uuml;", "&Yacute;", "&THORN;", "&szlig;", "&agrave;", "&aacute;", "&acirc;", "&atilde;", "&auml;", "&aring;", "&aelig;", "&ccedil;", "&egrave;", "&eacute;", "&ecirc;", "&euml;", "&igrave;", "&iacute;", "&icirc;", "&iuml;", "&eth;", "&ntilde;", "&ograve;", "&oacute;", "&ocirc;", "&otilde;", "&ouml;", "&divide;", "&oslash;", "&ugrave;", "&uacute;", "&ucirc;", "&uuml;", "&yacute;", "&thorn;", "&yuml;" };

    /** Normal chars corresponding to the defined special entities */
    private static final String[] ENTITY_STRINGS = { "\"", "&", "<", ">", " ", "¡", "¢", "£", "¤", "¥", "¦", "§", "¨", "©", "ª", "«", "¬", "­", "®", "¯", "°", "±", "²", "³", "´", "µ", "¶", "·", "¸", "¹", "º", "»", "¼", "½", "¾", "¿", "À", "Á", "Â", "Ã", "Ä", "Å", "Æ", "Ç", "È", "É", "Ê", "Ë", "Ì", "Í", "Î", "Ï", "Ð", "Ñ", "Ò", "Ó", "Ô", "Õ", "Ö", "×", "Ø", "Ù", "Ú", "Û", "Ü", "Ý", "Þ", "ß", "à", "á", "â", "ã", "ä", "å", "æ", "ç", "è", "é", "ê", "ë", "ì", "í", "î", "ï", "ð", "ñ", "ò", "ó", "ô", "õ", "ö", "÷", "ø", "ù", "ú", "û", "ü", "ý", "þ", "ÿ" };

    /**
	 * remove special entities from html string.
	 *
	 * @param source
	 * @return String
	 *
	 */
    private String removeHtmlSpecialEntities(String source) {
        String result = source;
        for (int i = 0; i < SPECIAL_ENTITIES.length; i++) {
            result = result.replaceAll(SPECIAL_ENTITIES[i], ENTITY_STRINGS[i]);
        }
        return result;
    }

    /**
	 * Create a simple mail message object based on an email queue data.
	 *
	 * @param emailQueue
	 *            The emailQueue object to transform.
	 * @param helper
	 *            The helper to populate with emailQueue information.
	 *
	 * @return {@link MimeMessageHelper}
	 *
	 * @throws EmailEngineException
	 *             If fields are missing or if template and model don't match
	 *             each other.
	 */
    public MimeMessageHelper populateEmailTemplate(EmailQueue emailQueue, MimeMessageHelper helper) throws EmailEngineException {
        Map<String, String> model = new HashMap<String, String>();
        Set<EmailField> emailFields = emailQueue.getEmailFields();
        for (EmailField field : emailFields) {
            model.put(field.getFieldName(), field.getFieldValue());
        }
        VelocityContext context = new VelocityContext(model);
        Template template = null;
        StringWriter contentSw = null;
        String templateType = emailQueue.getTemplateType();
        VelocityTemplate velocityTemplate = null;
        try {
            velocityTemplate = velocityTemplateManager.getTemplate(templateType);
        } catch (ObjectNotFoundException e1) {
            log.warn("Missing template : " + templateType);
        }
        try {
            log.debug("Loading template: " + templateType);
            template = engine.getTemplate(templateType);
            contentSw = new StringWriter();
            log.debug("Merging template.");
            template.merge(context, contentSw);
        } catch (ResourceNotFoundException rnfe) {
            log.warn("couldn't find the template: " + templateType, rnfe);
            throw new EmailEngineException("Unable to find specified template : " + templateType);
        } catch (ParseErrorException pee) {
            log.warn("Template and model don't match", pee);
            throw new EmailEngineException("Template and model don't match :" + pee.getMessage());
        } catch (MethodInvocationException mie) {
            log.warn("Method invocation error populating template.", mie);
            throw new EmailEngineException(mie.getMessage());
        } catch (Exception e) {
            log.warn("Generic error loading and populating email template", e);
            throw new EmailEngineException(e.getMessage());
        }
        try {
            helper.setTo(emailQueue.getTo());
            helper.setSubject(emailQueue.getSubject());
            try {
                if (model.containsKey("html") && model.get("html").equalsIgnoreCase("true")) {
                    Map<String, String> htmlModel = new HashMap<String, String>();
                    htmlModel.put("title", velocityTemplate.getTitle() != null ? velocityTemplate.getTitle() : "");
                    htmlModel.put("content", contentSw.toString());
                    if (velocityTemplate != null) {
                        String langCode = velocityTemplate.getTemplateType().substring(velocityTemplate.getTemplateType().length() - 2);
                        try {
                            htmlModel.put("promotion", generateHtmlForPromotions(langCode, velocityTemplate));
                        } catch (Exception e) {
                            log.error("Exception occured while generating promotion html for email", e);
                            htmlModel.put("promotion", "");
                        }
                    } else {
                        htmlModel.put("promotion", "");
                    }
                    htmlModel.put("footer", "");
                    VelocityContext htmlContext = new VelocityContext(htmlModel);
                    Template htmlTemplate = null;
                    StringWriter htmlSw = null;
                    try {
                        htmlTemplate = engine.getTemplate(Constants.BASE_EMAIL_HTML_TEMPLATE);
                        htmlSw = new StringWriter();
                        htmlTemplate.merge(htmlContext, htmlSw);
                        helper.setText(htmlSw.toString(), true);
                    } catch (ResourceNotFoundException rnfe) {
                        log.warn("couldn't find the template: " + Constants.BASE_EMAIL_HTML_TEMPLATE, rnfe);
                        throw new EmailEngineException("Unable to find specified template : " + Constants.BASE_EMAIL_HTML_TEMPLATE);
                    } catch (ParseErrorException pee) {
                        log.warn("Template and model don't match", pee);
                        throw new EmailEngineException("Template and model don't match :" + pee.getMessage());
                    } catch (MethodInvocationException mie) {
                        log.warn("Method invocation error populating template.", mie);
                        throw new EmailEngineException(mie.getMessage());
                    } catch (Exception e) {
                        log.warn("Generic error loading and populating email template", e);
                        throw new EmailEngineException(e.getMessage());
                    }
                } else {
                    String noHTMLString = contentSw.toString().replaceAll(config.get("regex1"), config.get("regex1token"));
                    noHTMLString = noHTMLString.replaceAll(config.get("regex2"), config.get("regex2token"));
                    noHTMLString = noHTMLString.replaceAll(config.get("regex3"), config.get("regex3token"));
                    noHTMLString = noHTMLString.replaceAll(config.get("regex4"), config.get("regex4token"));
                    noHTMLString = noHTMLString.replaceAll(config.get("regex5"), config.get("regex5token"));
                    int regexNumber = 5;
                    String regEX = null;
                    while (++regexNumber < 99 && (regEX = config.get("regex" + regexNumber)) != null) {
                        noHTMLString = noHTMLString.replaceAll(regEX, config.get("regex" + regexNumber + "token"));
                    }
                    noHTMLString = removeHtmlSpecialEntities(noHTMLString);
                    helper.setText(noHTMLString);
                    log.debug(noHTMLString);
                }
                if (model.containsKey("FORM")) helper.setBcc(EmailEngineConfiguration.getInstance().getConfigurationParameter(EmailEngineConfiguration.SMTP_EFORMS));
                if (model.containsKey("from")) {
                    helper.setFrom(model.get("from"));
                } else {
                    helper.setFrom(EmailEngineConfiguration.getInstance().getConfigurationParameter(EmailEngineConfiguration.EMAIL_SRC_ADR));
                }
            } catch (ConfigurationException e) {
                log.warn("Unable to retrieve configuration for email templater", e);
                throw new EmailEngineException("Unable to set From field due to ConfigurationException : " + e.getMessage());
            }
            log.debug("Begin processing attachments.");
            if (!emailQueue.getAttachments().isEmpty()) {
                Set<EmailAttachment> attachments = emailQueue.getAttachments();
                FileOutputStream fos = null;
                InputStream is;
                for (EmailAttachment att : attachments) {
                    log.debug("Processing attachment: " + att);
                    try {
                        is = att.getAttachmentContent().getBinaryStream();
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        int read;
                        while ((read = is.read()) != (-1)) {
                            out.write(read);
                        }
                        is.close();
                        byte[] b = out.toByteArray();
                        File file = new File(att.getAttachmentName());
                        fos = new FileOutputStream(file);
                        fos.write(b);
                        fos.flush();
                        fos.close();
                        helper.addAttachment(att.getAttachmentName(), file);
                    } catch (SQLException e) {
                        log.error("Unable to retrieve binary stream for attachment " + att, e);
                        throw new EmailEngineException("Unable to retrieve binary stream for attachment");
                    } catch (IOException ioe) {
                        log.error("Unable to read/write the attachment file.", ioe);
                        throw new EmailEngineException("Unable to read/write the attachment file");
                    }
                }
            }
            helper.setSentDate(new Timestamp(new Date().getTime()));
        } catch (MessagingException e) {
            throw new EmailEngineException("Unable to populate email due to MessagingException : " + e.getMessage());
        }
        return helper;
    }

    /**
	 * Check template availability.
	 *
	 * @param templateType
	 *            the template type
	 *
	 * @return {@link Boolean}
	 */
    public boolean templateExists(String templateType) {
        boolean found = false;
        try {
            found = engine.getTemplate(templateType) != null;
        } catch (Exception e) {
            log.error(e);
        }
        return found;
    }

    private final String TYPE_TXT = "Text";

    private final String TYPE_URL = "URL";

    private final String TYPE_IMG = "Large Image";

    private final String TYPE_PDF = "PDF File";

    /**
	 * Generate html for promotions.
	 *
	 * @param lang
	 *            the lang
	 * @param template
	 *            the template
	 *
	 * @return the string
	 */
    public String generateHtmlForPromotions(String lang, VelocityTemplate template) {
        StringBuilder sb = new StringBuilder();
        List<Promotion> promotions = categoryManager.getPromotionsByMarketingSpotCode(PROMOTION_MARKETING_SPOT_FOR_EMAILS);
        Set<Promotion> specificPromotions = new HashSet<Promotion>();
        for (Promotion promotion : promotions) {
            for (PromotionRules rule : promotion.getPromotionRules()) {
                if (rule != null && template.getId().equals(rule.getRelatedId())) {
                    specificPromotions.add(promotion);
                }
            }
        }
        while (specificPromotions.size() < MAX_PROMOTION_IN_EMAIL_RIGHT_COLUMN || promotions.size() == specificPromotions.size()) {
            Promotion promo = getHighestPriorityPromotion(promotions, specificPromotions);
            if (promo != null) {
                specificPromotions.add(promo);
            } else break;
        }
        boolean isFirst = true;
        for (Promotion currentPromotion : specificPromotions) {
            PromotionI18n promotionI18n = currentPromotion.getContent().get(lang);
            if (promotionI18n == null) {
                log.warn("PROMOTION WITHOUT CONTENT " + currentPromotion.getId());
                promotions.remove(currentPromotion);
                currentPromotion = null;
                continue;
            }
            String contentType = promotionI18n.getType();
            String rootLink = null;
            try {
                rootLink = EmailEngineConfiguration.getInstance().getConfigurationParameter(MEDIA_URI);
            } catch (ConfigurationException e) {
                log.warn("Configuration not found. Key : " + MEDIA_URI);
                rootLink = "";
            }
            String link = "#";
            if (TYPE_IMG.equals(contentType)) {
                if (currentPromotion.getMediasWithType().get(lang) != null) {
                    if (currentPromotion.getMediasWithType().get(lang).get(Constants.MEDIA_PROMOTIONLINK) != null) {
                        for (Media media : currentPromotion.getMediasWithType().get(lang).get(Constants.MEDIA_PROMOTIONLINK)) {
                            link = rootLink + media.getPathNormalSizeImage();
                        }
                    }
                }
            } else if (TYPE_TXT.equals(contentType)) {
                StringBuilder builder = new StringBuilder();
                String promotionTitle = null;
                promotionTitle = currentPromotion.getContent().get(lang.toUpperCase()).get("title");
                if (promotionTitle != null) {
                    promotionTitle = EncodeFriendlyUrl.encode(promotionTitle);
                    builder.append(urlMapperManager.getUrlMapperByUrlKey("mayWeSuggest.promotion", lang.toLowerCase()).getFriendlyUrl());
                    builder.append("/");
                    builder.append(promotionTitle);
                    builder.append("/");
                    builder.append(currentPromotion.getId());
                }
                link = rootLink + builder.toString();
            } else if (TYPE_PDF.equals(contentType)) {
                link = rootLink + promotionI18n.getPDFValue();
            } else if (TYPE_URL.equals(contentType)) {
                link = rootLink + promotionI18n.getURLValue();
            }
            Media icon = null;
            Set<Media> mediaType = null;
            if (currentPromotion.getMediasWithType().get(lang) != null) {
                mediaType = currentPromotion.getMediasWithType().get(lang).get(Constants.MEDIA_PROMOTION);
            } else if (currentPromotion.getMediasWithType().get(Constants.DEFAULT_CONTENT_LANGUAGE_CODE) != null) {
                mediaType = currentPromotion.getMediasWithType().get(Constants.DEFAULT_CONTENT_LANGUAGE_CODE).get(Constants.MEDIA_PROMOTION);
            }
            if (mediaType != null) {
                for (Media mediaWithType : mediaType) {
                    icon = mediaWithType;
                }
            }
            String title = promotionI18n.getTitle();
            String description = promotionI18n.getLabel();
            String imageSrc = "";
            if (icon != null) imageSrc = rootLink + icon.getPathSmallSizeImage();
            if (isFirst) {
                sb.append("<table class='offreEmail' border='0' cellpadding='0' cellspacing='0'><tbody>");
                isFirst = false;
            } else {
                sb.append("<tr><td colspan='2'><img src='" + rootLink + "/media/images/email/stroke_sidebar.gif' alt='image' height='31' width='180'></td></tr>");
            }
            sb.append("<tr><td colspan='2'><a href='" + link + "' target='_blank'><img src='" + imageSrc);
            sb.append("' alt='image' height='61' width='180'></a></td></tr><tr><td height='10'>&nbsp;</td></tr>");
            sb.append("<tr><td colspan='2' style='font-family: Tahoma,Arial,Sans-Serif; font-style: normal; font-variant: normal; font-weight: normal; font-size: 11px; line-height: normal; font-size-adjust: none; font-stretch: normal; color: rgb(41, 65, 80);'>" + description);
            sb.append("</td></tr>");
            sb.append("<tr><td height='5'></td></tr>");
            sb.append("<tr><td style='font-family: Tahoma,Arial,Sans-Serif; font-style: normal; font-variant: normal; font-weight: normal; font-size: 11px; line-height: normal; font-size-adjust: none; font-stretch: normal; color: rgb(41, 65, 80);' valign='top'>" + title);
            sb.append("</td></tr>");
        }
        if (!isFirst) {
            sb.append("</tbody></table>");
        }
        return sb.toString();
    }

    /**
	 * Sets the category manager.
	 *
	 * @param categoryManager
	 *            the new category manager
	 */
    public void setCategoryManager(CategoryManager categoryManager) {
        this.categoryManager = categoryManager;
    }

    /**
	 * Sets the url mapper manager.
	 *
	 * @param urlMapperManager
	 *            the new url mapper manager
	 */
    public void setUrlMapperManager(UrlMapperManager urlMapperManager) {
        this.urlMapperManager = urlMapperManager;
    }

    /**
	 * Sets the velocity template manager.
	 *
	 * @param velocityTemplateManager
	 *            the new velocity template manager
	 */
    public void setVelocityTemplateManager(VelocityTemplateManager velocityTemplateManager) {
        this.velocityTemplateManager = velocityTemplateManager;
    }

    /**
	 * Gets the highest priority promotion.
	 *
	 * @param from
	 *            the from
	 * @param exclude
	 *            the exclude
	 *
	 * @return the highest priority promotion
	 */
    private Promotion getHighestPriorityPromotion(List<Promotion> from, Set<Promotion> exclude) {
        Promotion p1 = null;
        float maxPriority = -1;
        for (Promotion promotion : from) {
            boolean skip = false;
            for (Promotion excludePromo : exclude) {
                if (promotion.equals(excludePromo)) {
                    skip = true;
                    break;
                }
            }
            if (!skip) {
                for (PromotionRules rule : promotion.getPromotionRules()) {
                    float priority = 0;
                    if (rule.getPriority() != null) {
                        priority = rule.getPriority();
                    }
                    if (priority > maxPriority) {
                        p1 = promotion;
                        maxPriority = priority;
                    }
                }
            }
        }
        return p1;
    }

    /** The Constant config. */
    private static final Map<String, String> config = new HashMap<String, String>();

    static {
        InputStream is = EmailTemplater.class.getClassLoader().getResourceAsStream("EmailTemplater.properties");
        if (is != null) {
            try {
                Properties properties = new Properties();
                properties.load(is);
                properties.keySet();
                for (Object key : properties.keySet()) {
                    String skey = (String) key;
                    String value = (String) properties.getProperty(skey);
                    if (value.equalsIgnoreCase("space")) {
                        value = " ";
                    }
                    config.put(skey, value);
                    if (log.isDebugEnabled()) log.debug("{key=" + skey + " value=" + value + "}");
                }
            } catch (IOException e) {
                log.warn("Error Loading EmailTemplater.properties", e);
            }
        }
    }
}
