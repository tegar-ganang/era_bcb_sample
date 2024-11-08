package com.doculibre.intelligid.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.doculibre.intelligid.entites.UtilisateurIFGD;

public class MailUtils {

    private static Log log = LogFactory.getLog(MailUtils.class);

    private static final String MAILER_VERSION = "Java";

    private static Properties getRealSMTPServerProperties() {
        MailConfiguration mailUtils = FGDSpringUtils.getMailConfiguration();
        Properties prop = System.getProperties();
        String smtpServerHost = mailUtils.getSmtpServerHost();
        Integer smtpServerPort = mailUtils.getSmtpServerPort();
        if (smtpServerHost == null || smtpServerHost.isEmpty()) {
            log.info("Aucun courriel n'est envoyé, car le serveur d'envoi n'est pas configuré");
            return null;
        }
        prop.put("mail.smtp.host", smtpServerHost);
        if (smtpServerPort != null) {
            prop.put("mail.smtp.port", smtpServerPort);
        }
        return prop;
    }

    public static boolean sendMail(String destinataire, String contenu, String sujet, String from) {
        Properties prop = getRealSMTPServerProperties();
        if (prop != null) {
            try {
                Session session = Session.getDefaultInstance(prop, null);
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(FGDSpringUtils.getExpediteurSupport()));
                InternetAddress[] internetAddresses = { new InternetAddress(destinataire) };
                message.setRecipients(Message.RecipientType.TO, internetAddresses);
                message.setContent(contenu, "text/html");
                message.setSubject(sujet);
                message.setHeader("X-Mailer", MAILER_VERSION);
                Transport.send(message);
                return true;
            } catch (AddressException e) {
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static void sendHTML(UtilisateurIFGD recipient, String object, MimeMultipart mime, String from) {
        sendHTML(Arrays.asList(new UtilisateurIFGD[] { recipient }), object, mime, from);
    }

    public static void sendHTML(List<UtilisateurIFGD> recipients, String object, MimeMultipart mime, String from) {
        sendHTML(toRecipientsMap(recipients), object, mime, from);
    }

    private static Map<String, String> toRecipientsMap(List<UtilisateurIFGD> users) {
        Map<String, String> recipientsMap = new TreeMap<String, String>();
        for (UtilisateurIFGD user : users) {
            recipientsMap.put(user.getCourriel(), user.getPrenom() + " " + user.getNomFamille());
        }
        return recipientsMap;
    }

    public static void sendSimpleHTMLMessage(UtilisateurIFGD recipient, String object, String htmlContent, String from) {
        sendSimpleHTMLMessage(Arrays.asList(recipient), object, htmlContent, from);
    }

    public static void sendSimpleHTMLMessage(List<UtilisateurIFGD> recipients, String object, String htmlContent, String from) {
        sendSimpleHTMLMessage(toRecipientsMap(recipients), object, htmlContent, from);
    }

    public static void sendSimpleHTMLMessage(Map<String, String> recipients, String object, String htmlContent, String from) {
        String message;
        try {
            File webinfDir = ClasspathUtils.getClassesDir().getParentFile();
            File mailDir = new File(webinfDir, "mail");
            File templateFile = new File(mailDir, "HtmlMessageTemplate.html");
            StringWriter sw = new StringWriter();
            Reader r = new BufferedReader(new FileReader(templateFile));
            IOUtils.copy(r, sw);
            sw.close();
            message = sw.getBuffer().toString();
            message = message.replaceAll("%MESSAGE_HTML%", htmlContent).replaceAll("%APPLICATION_URL%", FGDSpringUtils.getExternalServerURL());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Properties prop = getRealSMTPServerProperties();
        if (prop != null) {
            try {
                MimeMultipart multipart = new MimeMultipart("related");
                BodyPart messageBodyPart = new MimeBodyPart();
                messageBodyPart.setContent(message, "text/html");
                multipart.addBodyPart(messageBodyPart);
                sendHTML(recipients, object, multipart, from);
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }
        } else {
            StringBuffer contenuCourriel = new StringBuffer();
            for (Entry<String, String> recipient : recipients.entrySet()) {
                if (recipient.getValue() == null) {
                    contenuCourriel.append("À : " + recipient.getKey());
                } else {
                    contenuCourriel.append("À : " + recipient.getValue() + "<" + recipient.getKey() + ">");
                }
                contenuCourriel.append("\n");
            }
            contenuCourriel.append("Sujet : " + object);
            contenuCourriel.append("\n");
            contenuCourriel.append("Message : ");
            contenuCourriel.append("\n");
            contenuCourriel.append(message);
        }
    }

    public static void sendHTML(Map<String, String> recipients, String object, MimeMultipart mime, String from) {
        Properties props = getRealSMTPServerProperties();
        if (props != null) {
            props.put("mail.from", from);
            Session session = Session.getInstance(props, null);
            try {
                MimeMessage msg = new MimeMessage(session);
                msg.setFrom();
                List<Address> adresses = new ArrayList<Address>();
                for (Entry<String, String> recipient : recipients.entrySet()) {
                    if (recipient.getValue() == null) {
                        adresses.add(new InternetAddress(recipient.getKey()));
                    } else {
                        adresses.add(new InternetAddress(recipient.getKey(), recipient.getValue()));
                    }
                }
                msg.setRecipients(Message.RecipientType.TO, adresses.toArray(new Address[0]));
                msg.setSubject(object);
                msg.setSentDate(new Date());
                msg.setContent(mime);
                Transport.send(msg);
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void main(String[] args) {
        String expediteurSupport = FGDSpringUtils.getExpediteurAlertes();
        String sujet = "Message test sujet";
        String contenu = "<p>Message test contenu</p>";
        String destinataireCourriel = args[0];
        String destinataireNom;
        if (args.length > 1) {
            StringBuffer sb = new StringBuffer();
            for (int i = 1; i < args.length; i++) {
                String arg = args[i];
                sb.append(arg);
                if (i < args.length - 1) {
                    sb.append(" ");
                }
            }
            destinataireNom = sb.toString();
        } else {
            destinataireNom = destinataireCourriel;
        }
        Map<String, String> destinataireMap = new TreeMap<String, String>();
        destinataireMap.put(destinataireCourriel, destinataireNom);
        sendSimpleHTMLMessage(destinataireMap, sujet, contenu, expediteurSupport);
    }
}
