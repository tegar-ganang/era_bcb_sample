package org.jcvi.elvira;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.hibernate.Session;
import org.jcvi.common.command.CommandLineOptionBuilder;
import org.jcvi.common.command.CommandLineUtils;
import org.jcvi.common.core.util.JoinedStringBuilder;
import org.jcvi.common.io.idReader.DefaultFileIdReader;
import org.jcvi.common.io.idReader.StringIdParser;
import org.jcvi.commonx.auth.tigr.ProjectDbAuthorizer;
import org.jcvi.commonx.auth.tigr.ProjectDbAuthorizerUtils;
import org.jcvi.glk.Extent;
import org.jcvi.glk.ExtentType;
import org.jcvi.glk.elvira.ElviraGLKSessionBuilder;
import org.jcvi.glk.elvira.ExtentTypeName;
import org.jcvi.glk.elvira.nextgen.DefaultElviraTuple;
import org.jcvi.glk.elvira.nextgen.ElviraTuple;
import org.jcvi.glk.helpers.GLKHelper;
import org.jcvi.glk.helpers.HibernateGLKHelper;

public class CreateTupleFile {

    /**
     * @param args
     * @throws FileNotFoundException 
     */
    public static void main(String[] args) throws FileNotFoundException {
        Options options = new Options();
        options.addOption(CommandLineUtils.createHelpOption());
        OptionGroup group = new OptionGroup();
        group.setRequired(true);
        group.addOption(new CommandLineOptionBuilder("b", "file of bac ids").build());
        group.addOption(new CommandLineOptionBuilder("c", "file collection codes of collections to get tuple file for").build());
        group.addOption(new CommandLineOptionBuilder("l", "file lot codes of lots to get tuple file for").build());
        options.addOptionGroup(group);
        options.addOption(new CommandLineOptionBuilder("o", "path to file to write tuples to").isRequired(true).build());
        ProjectDbAuthorizerUtils.addProjectDbLoginOptionsTo(options, true);
        if (CommandLineUtils.helpRequested(args)) {
            printHelp(options);
            System.exit(0);
        }
        try {
            CommandLine commandLine = CommandLineUtils.parseCommandLine(options, args);
            ProjectDbAuthorizer auth = ProjectDbAuthorizerUtils.getProjectDbAuthorizerFrom(commandLine);
            File outputFile = new File(commandLine.getOptionValue("o"));
            PrintWriter writer = new PrintWriter(outputFile);
            Session session = new ElviraGLKSessionBuilder(auth).build();
            GLKHelper helper = new HibernateGLKHelper(session);
            final File inputFile;
            final List<String> list;
            if (commandLine.hasOption("b")) {
                inputFile = new File(commandLine.getOptionValue("b"));
                list = getTuplesFromBacs(auth, helper, inputFile);
            } else if (commandLine.hasOption("c")) {
                inputFile = new File(commandLine.getOptionValue("c"));
                list = getTuplesFromCollections(auth, helper, inputFile);
            } else {
                inputFile = new File(commandLine.getOptionValue("l"));
                list = getTuplesFromLots(auth, helper, inputFile);
            }
            writer.write(new JoinedStringBuilder(list).glue("\n").build());
            session.close();
            writer.close();
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            printHelp(options);
            System.exit(1);
        }
    }

    private static List<String> getTuplesFromBacs(ProjectDbAuthorizer auth, GLKHelper helper, final File inputFile) {
        DefaultFileIdReader<String> reader = new DefaultFileIdReader<String>(inputFile, new StringIdParser());
        Iterator<String> ids = reader.iterator();
        List<String> list = new ArrayList<String>();
        ExtentType sampleType = helper.getExtentType(ExtentTypeName.SAMPLE);
        while (ids.hasNext()) {
            String bacId = ids.next();
            Extent sample = helper.getExtent(sampleType, bacId);
            if (sample == null) {
                throw new IllegalArgumentException(String.format("could not find sample with bac id %s in project %s", bacId, auth.getProject()));
            }
            ElviraTuple tuple = DefaultElviraTuple.create(auth.getProject(), sample);
            list.add(tuple.toString());
        }
        return list;
    }

    private static List<String> getTuplesFromCollections(ProjectDbAuthorizer auth, GLKHelper helper, final File inputFile) {
        DefaultFileIdReader<String> reader = new DefaultFileIdReader<String>(inputFile, new StringIdParser());
        Iterator<String> ids = reader.iterator();
        List<String> list = new ArrayList<String>();
        ExtentType collectionType = helper.getExtentType(ExtentTypeName.COLLECTION);
        ExtentType sampleType = helper.getExtentType(ExtentTypeName.SAMPLE);
        while (ids.hasNext()) {
            String collectionCode = ids.next();
            Extent collection = helper.getExtent(collectionType, collectionCode);
            if (collection == null) {
                throw new IllegalArgumentException(String.format("could not find collection with code %s in project %s", collectionCode, auth.getProject()));
            }
            for (Extent sample : collection.getDescendantsByType(sampleType)) {
                ElviraTuple tuple = new DefaultElviraTuple(auth.getProject(), collectionCode, sample.getReference());
                list.add(tuple.toString());
            }
        }
        return list;
    }

    private static List<String> getTuplesFromLots(ProjectDbAuthorizer auth, GLKHelper helper, final File inputFile) {
        DefaultFileIdReader<String> reader = new DefaultFileIdReader<String>(inputFile, new StringIdParser());
        Iterator<String> ids = reader.iterator();
        List<String> list = new ArrayList<String>();
        ExtentType lotType = helper.getExtentType(ExtentTypeName.LOT);
        ExtentType sampleType = helper.getExtentType(ExtentTypeName.SAMPLE);
        while (ids.hasNext()) {
            String lotCode = ids.next();
            Extent lot = helper.getExtent(lotType, lotCode);
            if (lot == null) {
                throw new IllegalArgumentException(String.format("could not find lot with code %s in project %s", lotCode, auth.getProject()));
            }
            String collectionCode = lot.getParent().getReference();
            for (Extent sample : lot.getDescendantsByType(sampleType)) {
                ElviraTuple tuple = new DefaultElviraTuple(auth.getProject(), collectionCode, sample.getReference());
                list.add(tuple.toString());
            }
        }
        return list;
    }

    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("createTupleFile -D <db> -o <output tuple file> -b <bac id file>", "read the bac id file file, and write out the equivalent tuple file", options, "Created by Danny Katzel");
    }
}
