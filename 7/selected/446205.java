package net.ontopia.topicmaps.cmdlineutils;

import java.util.*;
import java.io.*;
import java.net.URL;
import net.ontopia.topicmaps.xml.XTMTopicMapReader;
import net.ontopia.topicmaps.core.*;
import net.ontopia.infoset.core.*;
import net.ontopia.topicmaps.cmdlineutils.statistics.*;
import net.ontopia.topicmaps.cmdlineutils.utils.TopicMapReader;
import net.ontopia.utils.*;
import net.ontopia.topicmaps.utils.*;

/**
 * PUBLIC: Prints various kinds of statistics for topic maps.</p>
 */
public class StatisticsPrinter {

    protected static BufferedReader stdInn = new BufferedReader(new InputStreamReader(System.in));

    protected TopicMapIF tm;

    protected StringifierIF ts = TopicStringifiers.getDefaultStringifier();

    /**
   * Constructor that accepts a topicmap as argument.
   */
    public StatisticsPrinter(TopicMapIF tm) {
        this.tm = tm;
    }

    /**
   * Used to request a filename when none is given.
   */
    protected static String requestFile() {
        String name = "";
        System.out.print("Please enter TopicMap file name: ");
        System.out.flush();
        try {
            name = stdInn.readLine().trim();
        } catch (IOException e) {
            System.out.println("Error : " + e);
        }
        return name;
    }

    /**
   * INTERNAL: Method that counts the number of TAO's, counts the
   * number of occurrences of each combination of association roles,
   * and counts the number topics, associations and occurrences 
   * that have no types.
   */
    void topicStats() {
        countTopics();
        countAssociationDep();
    }

    /**
   * Handles all the counting of different topics.
   */
    protected void countTopics() {
        TopicCounter topiccounter = new TopicCounter(tm);
        topiccounter.count();
        int numberOfTopics = topiccounter.getNumberOfTopics();
        int numberOfAssociations = topiccounter.getNumberOfAssociations();
        int numberOfOccurrences = topiccounter.getNumberOfOccurrences();
        HashMap topicTypes = topiccounter.getTopicTypes();
        HashMap assocTypes = topiccounter.getAssociationTypes();
        HashMap ocursTypes = topiccounter.getOccurrenceTypes();
        print("        Topic Map Count Result:          \n\n\n");
        print("Number of Topics:       " + numberOfTopics + "\n");
        print("Number of Associations: " + numberOfAssociations + "\n");
        print("Number of Occurrences:  " + numberOfOccurrences + "\n");
        print("---------------------------------------\n");
        print("Number of Taos:         " + (numberOfTopics + numberOfAssociations + numberOfOccurrences) + "\n");
        print("=======================================\n\n\n");
        print("                 Types:           \n");
        print("\n\n     Topics:\n\n");
        if (topicTypes.size() > 0) {
            print("\n" + format("Number of different topic types") + ": " + topicTypes.keySet().size() + "\n\n");
            String[] templist = sortAlpha(topicTypes.keySet());
            int i = 0;
            while (i < templist.length) {
                String t = templist[i];
                print(format(t) + ": " + ((Integer) topicTypes.get(t)).intValue() + "\n");
                i++;
            }
        } else {
            print("There are no topics with type in this topic map.\n");
        }
        print("\n\n     Associations:     \n\n");
        if (assocTypes.size() > 0) {
            print("\n" + format("Number of different association types") + ": " + assocTypes.keySet().size() + "\n\n");
            String[] templist = sortAlpha(assocTypes.keySet());
            int i = 0;
            while (i < templist.length) {
                String t = templist[i];
                print(format(t) + ": " + ((Integer) assocTypes.get(t)).intValue() + "\n");
                i++;
            }
        } else {
            print("There are no assocations with type in this topicmap.\n");
        }
        print("\n\n     Occurrences:\n\n");
        if (ocursTypes.size() > 0) {
            print("\n" + format("Number of different occurrence types") + ": " + ocursTypes.keySet().size() + "\n\n");
            String[] templist = sortAlpha(ocursTypes.keySet());
            int i = 0;
            while (i < templist.length) {
                String t = templist[i];
                print(format(t) + ": " + ((Integer) ocursTypes.get(t)).intValue() + "\n");
                i++;
            }
        } else {
            print("There are no occurrences with type in this topic map.\n");
        }
    }

    /**
   * Handles all the assciation dependecies.
   */
    protected void countAssociationDep() {
        TopicAssocDep topicassocdep = new TopicAssocDep(tm);
        print("\n\n\n         Association Dependencies:    \n\n\n");
        Iterator it = topicassocdep.getAssociations().iterator();
        while (it.hasNext()) {
            String a = (String) it.next();
            StringTokenizer st = new StringTokenizer(a, "$");
            String string = st.nextToken();
            print("\n\nThe association \"" + string + "\" has roles:\n");
            while (st.hasMoreTokens()) {
                string = st.nextToken();
                print("\"" + string + "\"\n");
            }
            print("and occurs " + topicassocdep.getNumberOfOccurrences(a) + " times\n");
        }
    }

    /**
   * Handles all the topics without type.
   */
    protected void getNoTypeTopics() {
        NoTypeCount notypes = new NoTypeCount(tm);
        notypes.traverse();
        Collection notypetopics = notypes.getNoTypeTopics();
        Collection notypeoccrs = notypes.getNoTypeOccurrences();
        Collection notypeassocs = notypes.getNoTypeAssociations();
        if (!notypetopics.isEmpty()) {
            Iterator it = notypetopics.iterator();
            print("\n\n\n       Topics without type:\n\n\n");
            while (it.hasNext()) {
                TopicIF t = (TopicIF) it.next();
                print("Topic : " + getTopicId(t) + "\n");
            }
        }
        if (!notypeassocs.isEmpty()) {
            print("\n\n\n       Associations without type:\n\n\n");
            Iterator it = notypeassocs.iterator();
            while (it.hasNext()) {
                AssociationIF a = (AssociationIF) it.next();
                print("Association : " + a.getObjectId() + "\n");
                Collection roles = a.getRoles();
                Iterator itr = roles.iterator();
                while (itr.hasNext()) {
                    AssociationRoleIF arif = (AssociationRoleIF) itr.next();
                    print("Role : " + ts.toString(arif.getPlayer()) + "\n");
                }
            }
        }
        if (!notypeoccrs.isEmpty()) {
            print("\n\n\n       Occurrences without type:\n\n\n");
            Iterator it = notypeoccrs.iterator();
            while (it.hasNext()) {
                OccurrenceIF o = (OccurrenceIF) it.next();
                LocatorIF l = o.getLocator();
                print("Occurrence : " + l.getAddress() + "\n");
            }
        }
    }

    /**
   * Method used for pretty print.
   */
    protected String format(String t) {
        int numberOfBlanks = 42 - t.length();
        for (int i = 0; i < numberOfBlanks; i++) {
            t = t + " ";
        }
        return t;
    }

    /**
   * Lazy print, used internaly.
   */
    protected static void print(String s) {
        System.out.print(s);
    }

    protected String getTopicId(TopicIF topic) {
        String id = null;
        if (topic.getTopicMap().getStore().getBaseAddress() != null) {
            String base = topic.getTopicMap().getStore().getBaseAddress().getAddress();
            Iterator it = topic.getItemIdentifiers().iterator();
            while (it.hasNext()) {
                LocatorIF sloc = (LocatorIF) it.next();
                if (sloc.getAddress().startsWith(base)) {
                    String addr = sloc.getAddress();
                    id = addr.substring(addr.indexOf('#') + 1);
                    break;
                }
            }
        }
        if (id == null) id = "id" + topic.getObjectId();
        return id;
    }

    public static void main(String[] argv) {
        CmdlineUtils.initializeLogging();
        CmdlineOptions options = new CmdlineOptions("StatisticsPrinter", argv);
        CmdlineUtils.registerLoggingOptions(options);
        try {
            options.parse();
        } catch (CmdlineOptions.OptionsException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
        String[] args = options.getArguments();
        if (args.length < 1) {
            System.err.println("Error: Illegal number of arguments.");
            usage();
            System.exit(1);
        }
        try {
            TopicMapIF tm = ImportExportUtils.getReader(args[0]).read();
            if (tm == null) throw new OntopiaRuntimeException("No topic maps found.");
            StatisticsPrinter statsprinter = new StatisticsPrinter(tm);
            statsprinter.topicStats();
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(1);
        }
    }

    private String[] sortAlpha(Collection collection) {
        String[] retur = new String[collection.size()];
        Iterator it = collection.iterator();
        int k = 0;
        while (it.hasNext()) {
            String temp = (String) it.next();
            retur[k] = temp;
            k++;
        }
        for (int i = 0; i + 1 < retur.length; i++) {
            if (retur[i].compareTo(retur[i + 1]) > 0) {
                String temp = retur[i];
                retur[i] = retur[i + 1];
                retur[i + 1] = temp;
                int j = i;
                boolean done = false;
                while (j != 0 && !done) {
                    if (retur[j].compareTo(retur[j - 1]) < 0) {
                        temp = retur[j];
                        retur[j] = retur[j - 1];
                        retur[j - 1] = temp;
                    } else done = true;
                    j--;
                }
            }
        }
        return retur;
    }

    protected static void usage() {
        System.out.println("java net.ontopia.topicmaps.cmdlineutils.StatisticsPrinter [options] <url> [parser]");
        System.out.println("");
        System.out.println("  Reads a topic map and outputs statistics about it.");
        System.out.println("");
        System.out.println("  Options:");
        CmdlineUtils.printLoggingOptionsUsage(System.out);
        System.out.println("");
        System.out.println("  <url>: url of topic map to output statistics for");
        System.out.println("  [parser]: (optional) lets you specify which xml parser to use.");
    }
}
