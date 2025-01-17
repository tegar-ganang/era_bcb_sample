package jimo.osgi.modules.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import jimo.osgi.api.CommandContext;
import jimo.osgi.api.CommandHandler;
import jimo.osgi.api.JIMOConstants;
import jimo.osgi.api.util.PropertiesParser;
import jimo.osgi.impl.framework.FrameworkImpl;
import jimo.osgi.modules.xml.api.UtilXMLReader;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.w3c.dom.Document;

/**
 * Handle core command invocations.
 * <br/>
 */
public class CoreCommandHandler implements CommandHandler {

    public void onCommand(final String line, final CommandContext context) {
        doCommand(line, context);
    }

    protected void doCommand(String line, CommandContext context) {
        if (line.charAt(0) == '[') return;
        String l = Core.getConfig().resolve(line).trim();
        StringTokenizer tok = new StringTokenizer(l);
        String cmd = tok.nextToken();
        try {
            if (CoreConstants.CORECOMMAND_EVENT.equals(cmd)) {
                postEvent(l, context);
            } else if (CoreConstants.CORECOMMAND_FACTORYCONFIGURATION.equals(cmd)) {
                createFactoryConfiguration(line, context);
            } else if (CoreConstants.CORECOMMAND_GETCONFIGURATION.equals(cmd)) {
                getConfiguration(line, context);
            } else if (CoreConstants.CORECOMMAND_LISTCONFIGURATIONS.equals(cmd)) {
                listConfigurations(line, context);
            } else if (CoreConstants.CORECOMMAND_DELETECONFIGURATION.equals(cmd)) {
                deleteConfigurations(line, context);
            } else if (CoreConstants.CORECOMMAND_IMPORT.equals(cmd)) {
                importConfigurations(line, context);
            } else if (CoreConstants.CORECOMMAND_INSTALL.equals(cmd)) {
                installBundle(line, context);
            } else if (CoreConstants.CORECOMMAND_UNINSTALL.equals(cmd)) {
                uninstallBundle(line, context);
            } else if (CoreConstants.CORECOMMAND_STARTBUNDLE.equals(cmd)) {
                startBundle(line, context);
            } else if (CoreConstants.CORECOMMAND_STOPBUNDLE.equals(cmd)) {
                stopBundle(line, context);
            } else if (CoreConstants.CORECOMMAND_UPDATEBUNDLE.equals(cmd)) {
                updateBundle(line, context);
            } else if (CoreConstants.CORECOMMAND_STOP.equals(cmd)) {
                FrameworkImpl.INSTANCE.stop();
            } else if (CoreConstants.CORECOMMAND_PANIC.equals(cmd)) {
                FrameworkImpl.INSTANCE.panic();
            } else if (CoreConstants.CORECOMMAND_RESTART.equals(cmd)) {
                FrameworkImpl.INSTANCE.restart();
            } else if (CoreConstants.CORECOMMAND_REBUILD.equals(cmd)) {
                FrameworkImpl.INSTANCE.rebuild();
            } else if (CoreConstants.CORECOMMAND_INFO.equals(cmd)) {
                showInfo(l, context);
            } else if (CoreConstants.CORECOMMAND_STATUS.equals(cmd)) {
                showStatus(l, context);
            } else if (CoreConstants.CORECOMMAND_SERVICES.equals(cmd)) {
                showServices(l, context);
            } else if (CoreConstants.CORECOMMAND_SERVICE.equals(cmd)) {
                serviceCommand(l, context);
            } else if (CoreConstants.CORECOMMAND_REFRESH.equals(cmd)) {
                refresh();
            } else if (CoreConstants.CORECOMMAND_RC.equals(cmd)) {
                loadRc(l, context);
            } else if (CoreConstants.CORECOMMAND_PROPGET.equals(cmd)) {
                propGet(l, context);
            } else if (CoreConstants.CORECOMMAND_PROPSET.equals(cmd)) {
                propSet(l, context);
            } else if (CoreConstants.CORECOMMAND_HELP.equals(cmd)) {
                printHelp(l, context);
            } else {
                context.error(Core.getConfig().format("${" + CoreConstants.KEY_COMMANDNOTFOUND + "}", new Object[] { cmd }));
            }
        } catch (Throwable e) {
            context.error(e);
        }
    }

    /**
	 * @param line
	 * @param context
	 */
    private void importConfigurations(String line, CommandContext context) {
        StringTokenizer tokenizer = new StringTokenizer(Core.getConfig().resolve(line));
        String command = tokenizer.nextToken();
        while (tokenizer.hasMoreTokens()) {
            String location = tokenizer.nextToken();
            try {
                Document document = UtilXMLReader.loadXML(new URL(location));
            } catch (MalformedURLException e) {
                context.error(e);
            }
        }
    }

    private void deleteConfigurations(String line, CommandContext context) {
        context.error("NYI");
    }

    private void createFactoryConfiguration(String line, CommandContext context) {
        Dictionary properties = new Hashtable();
        ConfigurationAdmin configurationAdmin = (ConfigurationAdmin) getConfigurationAdmin();
        line = Core.getConfig().resolve(line);
        StringTokenizer tokenizer = new StringTokenizer(line);
        String command = tokenizer.nextToken();
        String factoryPid = tokenizer.nextToken();
        Configuration configuration;
        try {
            configuration = configurationAdmin.createFactoryConfiguration(factoryPid);
            context.println(configuration.getFactoryPid() + "," + configuration.getPid());
        } catch (IOException e) {
            context.error(e);
            return;
        }
        String propString = "";
        if (line.indexOf('[') != -1 && line.indexOf(']') != -1) {
            ;
            propString = line.substring(line.indexOf('[') + 1, line.indexOf(']'));
            PropertiesParser.setProperties(propString, properties);
            try {
                configuration.update(properties);
            } catch (IOException e) {
                context.error(e);
            }
        }
    }

    private void getConfiguration(String line, CommandContext context) {
        Dictionary properties = new Hashtable();
        ConfigurationAdmin configurationAdmin = (ConfigurationAdmin) getConfigurationAdmin();
        line = Core.getConfig().resolve(line);
        StringTokenizer tokenizer = new StringTokenizer(line);
        String command = tokenizer.nextToken();
        String pid = tokenizer.nextToken();
        Configuration configuration;
        try {
            configuration = configurationAdmin.getConfiguration(pid);
        } catch (IOException e) {
            context.error(e);
            return;
        }
        String propString = "";
        if (line.indexOf('[') != -1 && line.indexOf(']') != -1) {
            propString = line.substring(line.indexOf('[') + 1, line.indexOf(']'));
            PropertiesParser.setProperties(propString, properties);
            try {
                configuration.update(properties);
                context.println(configuration.getPid());
            } catch (IOException e) {
                context.error(e);
            }
        }
    }

    private void listConfigurations(String line, CommandContext context) {
        ConfigurationAdmin configurationAdmin = (ConfigurationAdmin) getConfigurationAdmin();
        line = Core.getConfig().resolve(line);
        StringTokenizer tokenizer = new StringTokenizer(line);
        String command = tokenizer.nextToken();
        String filter = "";
        if (line.indexOf('[') != -1 && line.indexOf(']') != -1) {
            filter = line.substring(line.indexOf('[') + 1, line.indexOf(']'));
        } else filter = "(" + Constants.SERVICE_PID + "=*)";
        try {
            Configuration[] configurations = configurationAdmin.listConfigurations(filter);
            for (int i = 0; i < configurations.length; i++) {
                Configuration configuration = configurations[i];
                showConfiguration(context, configuration);
            }
        } catch (IOException e) {
            context.error(e);
        } catch (InvalidSyntaxException e) {
            context.error(e);
        }
    }

    private EventAdmin getEventAdmin() {
        ServiceReference serviceReference = Core.INSTANCE.getBundleContext().getServiceReference(EventAdmin.class.getName());
        return (EventAdmin) Core.INSTANCE.getBundleContext().getService(serviceReference);
    }

    private ConfigurationAdmin getConfigurationAdmin() {
        ServiceReference serviceReference = Core.INSTANCE.getBundleContext().getServiceReference(ConfigurationAdmin.class.getName());
        return (ConfigurationAdmin) Core.INSTANCE.getBundleContext().getService(serviceReference);
    }

    private void showStatus(String line, CommandContext context) {
        FrameworkImpl.INSTANCE.showStatus(context.getOut());
    }

    private void propSet(String line, CommandContext context) {
        StringTokenizer tokenizer = new StringTokenizer(Core.getConfig().resolve(line));
        String command = tokenizer.nextToken();
        String key = tokenizer.nextToken();
        String value = tokenizer.nextToken();
        FrameworkImpl.INSTANCE.getConfig().setProperty(key, value);
    }

    private void propGet(String line, CommandContext context) {
        StringTokenizer tokenizer = new StringTokenizer(Core.getConfig().resolve(line));
        String command = tokenizer.nextToken();
        String key = tokenizer.nextToken();
        String value = FrameworkImpl.INSTANCE.getConfig().getProperty(key);
        context.println(value);
    }

    private void showServices(String line, CommandContext context) {
        String filter = null;
        String clazz = null;
        StringTokenizer tokenizer = new StringTokenizer(Core.getConfig().resolve(line));
        String command = tokenizer.nextToken();
        if (tokenizer.hasMoreTokens()) clazz = tokenizer.nextToken();
        if (tokenizer.hasMoreTokens()) filter = tokenizer.nextToken();
        ServiceReference[] serviceReferences = null;
        try {
            serviceReferences = Core.INSTANCE.getBundleContext().getAllServiceReferences(clazz, filter);
        } catch (InvalidSyntaxException e) {
            context.error(e);
            return;
        }
        if (serviceReferences == null) return;
        showServiceReferences(context, serviceReferences);
    }

    private void showServiceReferences(CommandContext context, ServiceReference[] serviceReferences) {
        for (int i = 0; i < serviceReferences.length; i++) {
            ServiceReference reference = serviceReferences[i];
            showService(context, reference);
        }
    }

    private void showService(CommandContext context, ServiceReference reference) {
        String bundleInfo = Core.getResourceString(CoreConstants.KEY_CORESERVICEINFO);
        context.println(MessageFormat.format(bundleInfo, new Object[] { new Long(reference.getBundle().getBundleId()), reference.getBundle().getSymbolicName() }));
        Bundle[] usingBundles = reference.getUsingBundles();
        for (int i = 0; i < usingBundles.length; i++) {
        }
        String[] propertyKeys = reference.getPropertyKeys();
        for (int i = 0; i < propertyKeys.length; i++) {
            context.print(propertyKeys[i] + " = ");
            Object value = reference.getProperty(propertyKeys[i]);
            if (value instanceof String[]) {
                String[] strings = (String[]) value;
                context.print("[");
                for (int j = 0; j < strings.length; j++) {
                    String string = strings[j];
                    context.print("'" + string + "' ");
                }
                context.print("]");
            } else if (value instanceof Integer[]) {
                Integer[] integers = (Integer[]) value;
                context.print("[");
                for (int j = 0; j < integers.length; j++) {
                    Integer integer = integers[j];
                    context.print("'" + integer + "' ");
                }
                context.print("]");
            } else context.print("" + value);
            context.newLine();
        }
    }

    private void serviceCommand(String line, CommandContext context) {
        StringTokenizer tokenizer = new StringTokenizer(Core.getConfig().resolve(line));
        String command = tokenizer.nextToken();
        String serviceCommand = tokenizer.nextToken();
        if (CoreConstants.SERVICECOMMAND_REGISTER.equals(serviceCommand)) {
            context.error("NYI");
        } else if (CoreConstants.SERVICECOMMAND_DEREGISTER.equals(serviceCommand)) {
            context.error("NYI");
        } else if (CoreConstants.SERVICECOMMAND_GET.equals(serviceCommand)) {
            context.error("NYI");
        } else if (CoreConstants.SERVICECOMMAND_UNGET.equals(serviceCommand)) {
            context.error("NYI");
        }
    }

    private void printHelp(String line, CommandContext context) {
        String help = Core.getResourceString(CoreConstants.KEY_COREHELP);
        context.println(help);
    }

    private void loadRc(String line, CommandContext context) {
        StringTokenizer tokenizer = new StringTokenizer(Core.getConfig().resolve(line));
        String command = tokenizer.nextToken();
        while (tokenizer.hasMoreTokens()) {
            String location = tokenizer.nextToken();
            try {
                URL url = new URL(location);
                InputStream in = url.openStream();
                CommandProcessor processor = new CommandProcessor(in, context.getOut(), Core.INSTANCE.getBundleContext(), location, false);
                processor.run();
            } catch (MalformedURLException e) {
                context.error(e);
            } catch (IOException e) {
                context.error(e);
            }
        }
    }

    private void stopBundle(String line, CommandContext context) {
        StringTokenizer tokenizer = new StringTokenizer(Core.getConfig().resolve(line));
        String command = tokenizer.nextToken();
        while (tokenizer.hasMoreTokens()) {
            Bundle bundle = null;
            String bundleId = tokenizer.nextToken();
            try {
                long id = Long.parseLong(bundleId);
                bundle = FrameworkImpl.INSTANCE.getBundleRegistry().getBundle(id);
            } catch (NumberFormatException e) {
            }
            if (bundle == null) {
                bundle = FrameworkImpl.INSTANCE.getBundleRegistry().getBundle(bundleId);
            }
            if (bundle == null) {
                context.error(Core.getConfig().getResourceString(CoreConstants.KEY_NOSUCHBUNDLE));
                return;
            }
            try {
                bundle.stop();
                String resourceString = Core.getConfig().getResourceString(CoreConstants.KEY_STOPPEDBUNDLE);
                context.println(MessageFormat.format(resourceString, new Object[] { new Long(bundle.getBundleId()), bundle.getSymbolicName() }));
            } catch (BundleException e) {
                context.error(e);
            }
        }
    }

    private void startBundle(String line, CommandContext context) {
        StringTokenizer tokenizer = new StringTokenizer(Core.getConfig().resolve(line));
        String command = tokenizer.nextToken();
        while (tokenizer.hasMoreTokens()) {
            Bundle bundle = null;
            String bundleId = tokenizer.nextToken();
            try {
                long id = Long.parseLong(bundleId);
                bundle = FrameworkImpl.INSTANCE.getBundleRegistry().getBundle(id);
            } catch (NumberFormatException e) {
            }
            if (bundle == null) {
                bundle = FrameworkImpl.INSTANCE.getBundleRegistry().getBundle(bundleId);
            }
            if (bundle == null) {
                context.error(Core.getConfig().getResourceString(CoreConstants.KEY_NOSUCHBUNDLE));
            } else {
                FrameworkImpl.INSTANCE.getBundleRegistry().setStarted(bundle);
                String resourceString = Core.getConfig().getResourceString(CoreConstants.KEY_STARTEDBUNDLE);
                context.println(MessageFormat.format(resourceString, new Object[] { new Long(bundle.getBundleId()), bundle.getSymbolicName() }));
            }
        }
    }

    private void updateBundle(String line, CommandContext context) {
        StringTokenizer tokenizer = new StringTokenizer(Core.getConfig().resolve(line));
        String command = tokenizer.nextToken();
        while (tokenizer.hasMoreTokens()) {
            long id = Long.parseLong(tokenizer.nextToken());
            Bundle bundle = FrameworkImpl.INSTANCE.getBundleRegistry().getBundle(id);
            if (bundle == null) {
                context.error(Core.getConfig().getResourceString(CoreConstants.KEY_NOSUCHBUNDLE));
                return;
            }
            try {
                bundle.update();
                String resourceString = Core.getConfig().getResourceString(CoreConstants.KEY_UPDATEDBUNDLE);
                context.println(MessageFormat.format(resourceString, new Object[] { new Long(bundle.getBundleId()), bundle.getSymbolicName() }));
            } catch (BundleException e) {
                e.printStackTrace(System.err);
                context.error(e);
            }
        }
        refresh();
    }

    private void uninstallBundle(String line, CommandContext context) {
        StringTokenizer tokenizer = new StringTokenizer(Core.getConfig().resolve(line));
        String command = tokenizer.nextToken();
        while (tokenizer.hasMoreTokens()) {
            long id = Long.parseLong(tokenizer.nextToken());
            try {
                Bundle bundle = FrameworkImpl.INSTANCE.getBundleRegistry().getBundle(id);
                if (bundle == null) {
                    context.error(Core.getConfig().getResourceString(CoreConstants.KEY_NOSUCHBUNDLE));
                    return;
                }
                bundle.uninstall();
                String resourceString = Core.getConfig().getResourceString(CoreConstants.KEY_UNINSTALLEDBUNDLE);
                context.println(MessageFormat.format(resourceString, new Object[] { new Long(bundle.getBundleId()), bundle.getSymbolicName() }));
            } catch (BundleException e) {
                context.error(e);
            }
        }
    }

    private void installBundle(String line, CommandContext context) {
        StringTokenizer tokenizer = new StringTokenizer(Core.getConfig().resolve(line));
        String command = tokenizer.nextToken();
        while (tokenizer.hasMoreTokens()) {
            String location = tokenizer.nextToken();
            try {
                Bundle bundle = Core.INSTANCE.getBundleContext().installBundle(location);
                String resourceString = Core.getConfig().getResourceString(CoreConstants.KEY_INSTALLEDBUNDLE);
                context.println(MessageFormat.format(resourceString, new Object[] { new Long(bundle.getBundleId()), bundle.getSymbolicName() }));
            } catch (BundleException e) {
                context.error(e);
            }
        }
    }

    private void refresh() {
        FrameworkImpl.INSTANCE.getBundleRegistry().removeUninstalledBundles();
        FrameworkImpl.INSTANCE.getBundleRegistry().resolvePendingBundles();
        FrameworkImpl.INSTANCE.getBundleRegistry().startPendingBundles();
    }

    private void postEvent(String line, CommandContext context) {
        Dictionary properties = new Hashtable();
        EventAdmin eventAdmin = getEventAdmin();
        StringTokenizer tokenizer = new StringTokenizer(Core.getConfig().resolve(line));
        String command = tokenizer.nextToken();
        String topic = tokenizer.nextToken();
        String propString = "";
        if (line.indexOf('[') != -1 && line.indexOf(']') != -1) {
            ;
            propString = line.substring(line.indexOf('[') + 1, line.indexOf(']'));
            PropertiesParser.setProperties(propString, properties);
        }
        properties.put(JIMOConstants.EVENT_COMMANDLINE, line);
        properties.put(JIMOConstants.EVENT_COMMAND, command);
        properties.put(JIMOConstants.EVENT_COMMANDCONTEXT, context);
        Event event = new Event(topic, properties);
        eventAdmin.postEvent(event);
    }

    private void showInfo(String line, CommandContext context) {
        StringTokenizer tokenizer = new StringTokenizer(Core.getConfig().resolve(line));
        String command = tokenizer.nextToken();
        if (tokenizer.hasMoreTokens()) {
            while (tokenizer.hasMoreTokens()) {
                long id = Long.parseLong(tokenizer.nextToken());
                Bundle bundle = FrameworkImpl.INSTANCE.getBundleRegistry().getBundle(id);
                showBundle(bundle, context, true);
            }
            return;
        }
        Bundle[] bundles = FrameworkImpl.INSTANCE.getBundleRegistry().getBundles();
        for (int i = 0; i < bundles.length; i++) {
            Bundle bundle = bundles[i];
            showBundle(bundle, context, false);
        }
    }

    private void showBundle(Bundle bundle, CommandContext context, boolean verbose) {
        String bundleInfo = Core.getConfig().getResourceString(CoreConstants.KEY_BUNDLEINFO);
        context.println(MessageFormat.format(bundleInfo, new Object[] { new Long(bundle.getBundleId()), bundle.getSymbolicName(), new Integer(bundle.getState()), bundle.getLocation() }));
        if (verbose) {
            context.println(Core.getConfig().getResourceString(CoreConstants.KEY_BUNDLEREGSERVICES));
            ServiceReference[] registeredServices = bundle.getRegisteredServices();
            showServiceReferences(context, registeredServices);
            context.println(Core.getConfig().getResourceString(CoreConstants.KEY_BUNDLESERVICESINUSE));
            ServiceReference[] servicesInUse = bundle.getServicesInUse();
            showServiceReferences(context, servicesInUse);
        }
    }

    private void showConfiguration(CommandContext context, Configuration configuration) {
        String configInfo = Core.getResourceString(CoreConstants.KEY_CORECONFIGINFO);
        context.println(MessageFormat.format(configInfo, new Object[] { configuration.getPid(), configuration.getFactoryPid(), configuration.getBundleLocation() }));
        Dictionary properties = configuration.getProperties();
        Enumeration enumeration = properties.keys();
        while (enumeration.hasMoreElements()) {
            String key = (String) enumeration.nextElement();
            Object value = properties.get(key);
            context.print(key + " = ");
            if (value instanceof String[]) {
                String[] strings = (String[]) value;
                for (int j = 0; j < strings.length; j++) {
                    String string = strings[j];
                    context.print("'" + string + "' ");
                }
            } else if (value instanceof Integer[]) {
                Integer[] integers = (Integer[]) value;
                context.print("[");
                for (int j = 0; j < integers.length; j++) {
                    Integer integer = integers[j];
                    context.print("'" + integer + "' ");
                }
                context.print("]");
            } else context.print("" + value);
            context.newLine();
        }
    }
}
