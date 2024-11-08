package org.jaffa.modules.messaging.tools;

import java.io.Writer;
import org.apache.log4j.Logger;
import org.jaffa.modules.messaging.services.ConfigurationService;
import org.jaffa.modules.messaging.services.configdomain.QueueInfo;
import org.jaffa.modules.messaging.services.configdomain.TopicInfo;

/** This is a utility class to generate a JBossMessaging destinations-service.xml file listing the required destinations.
 * The following parameters need to be provided before invoking the generate() method
 * <ul>
 *   <li> configurationFileName: the name (with path) of the configuration file (eg. "C:/a-deploy-folder/an-app.sar/resources/jaffa-messaging-config.xml") </li>
 *   <li> outputFileName: the application specific JBossMessaging destinations-service.xml file to generate (eg. "C:/a-deploy-folder/an-app.sar/jaffa-messaging-destinations-service.xml") </li>
 *   <li> roles: (optional) a comma-separated list of roles which have access to all the Destinations (eg. "guest") </li>
 * </ul>
 */
public class JBMDestinationsGenerator extends JBossMQDestinationsGenerator {

    private static final Logger log = Logger.getLogger(JBMDestinationsGenerator.class);

    /** Add jboss.xml to the jar.
     * @param config the configuration service.
     * @param out the jar file to write to.
     * @throws Exception if any error occurs.
     */
    @Override
    protected void addDestinations(ConfigurationService config, Writer out) throws Exception {
        StringBuilder secBuf = new StringBuilder();
        if (getRoles() != null && getRoles().length() > 0) {
            secBuf.append("    <attribute name='SecurityConfig'>\n").append("      <security>\n");
            for (String role : getRoles().split(",")) secBuf.append("        <role name='").append(role).append("' read='true' write='true'/>\n");
            secBuf.append("      </security>\n").append("    </attribute>\n");
        }
        StringBuilder mbeanBuf = new StringBuilder();
        String[] queueNames = config.getQueueNames();
        if (queueNames != null) {
            for (String queueName : queueNames) {
                QueueInfo queueInfo = config.getQueueInfo(queueName);
                mbeanBuf.append("  <mbean code='org.jboss.jms.server.destination.QueueService' name='jboss.messaging.destination:service=Queue,name=").append(queueInfo.getName()).append("' xmbean-dd='xmdesc/Queue-xmbean.xml'>\n").append("    <depends optional-attribute-name='ServerPeer'>jboss.messaging:service=ServerPeer</depends>\n").append("    <depends>jboss.messaging:service=PostOffice</depends>\n").append(secBuf.toString()).append("  </mbean>\n");
            }
        }
        String[] topicNames = config.getTopicNames();
        if (topicNames != null) {
            for (String topicName : topicNames) {
                TopicInfo topicInfo = config.getTopicInfo(topicName);
                mbeanBuf.append("  <mbean code='org.jboss.jms.server.destination.TopicService' name='jboss.messaging.destination:service=Topic,name=").append(topicInfo.getName()).append("' xmbean-dd='xmdesc/Queue-xmbean.xml'>\n").append("    <depends optional-attribute-name='ServerPeer'>jboss.messaging:service=ServerPeer</depends>\n").append("    <depends>jboss.messaging:service=PostOffice</depends>\n").append(secBuf.toString()).append("  </mbean>\n");
            }
        }
        StringBuilder buf = new StringBuilder("<?xml version='1.0' encoding='UTF-8'?>\n").append(createManifest()).append("<server>\n").append(mbeanBuf.toString()).append("</server>\n");
        out.write(buf.toString());
    }

    /** This will create an instance of the JBossMessagingDestinationsGenerator and set the various properties using the supplied arguments
     * It will then invoke the generate() method.
     * @param args The following arguments need to be passed: configurationFileName, outputFileName, roles.
     */
    public static void main(String... args) {
        if (args.length < 2) throw new IllegalArgumentException("Usage: JBossMessagingDestinationsGenerator <configurationFileName> <outputFileName> {<roles>}");
        try {
            JBMDestinationsGenerator obj = new JBMDestinationsGenerator();
            obj.setConfigurationFileName(args[0]);
            obj.setOutputFileName(args[1]);
            if (args.length > 2) obj.setRoles(args[2]);
            obj.generate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
