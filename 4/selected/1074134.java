package ch.tarnet.jagged;

import static org.junit.Assert.*;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.Test;
import ch.tarnet.jagged.entity.CreatureTpl;
import ch.tarnet.jagged.entity.Entity;
import ch.tarnet.jagged.entity.TileTpl;
import ch.tarnet.library.Utilities;

/**
 * @author Damien Plumettaz
 * @since 5 avr. 2012
 *
 */
public class ResManTest {

    private static Logger logger = Logger.getLogger(ResManTest.class.getName());

    @org.junit.BeforeClass
    public static void initialize() {
        Utilities.configureLogger();
    }

    @Test
    public void managerInitialization() {
        logger.entering("ResManTest", "managerInitialization");
        ResMan resMan = new ResMan("res/global", "res/explorer");
        logger.exiting("ResManTest", "managerInitialization");
        logger.info("EMPTY");
    }

    @Test
    public void openResourceStream() throws Exception {
        logger.entering("ResManTest", "openResourceStream");
        ResMan resMan = new ResMan("res/global", "res/explorer");
        InputStream is = resMan.openResource("test.txt");
        BufferedInputStream bis = new BufferedInputStream(is);
        int b;
        while ((b = bis.read()) != -1) System.out.write(b);
        System.out.println();
        is = resMan.openResource("error.txt");
        logger.exiting("ResManTest", "openResourceStream");
        logger.info("EMPTY");
    }

    @Test
    public void loadTemplate() {
        logger.entering("ResManTest", "loadTemplate");
        ResMan resMan = new ResMan("res/global", "res/explorer");
        resMan.inspect();
        resMan.loadTemplates("templates/tiles.def", TileTpl.class);
        resMan.loadTemplates("templates/creatures.def", CreatureTpl.class);
        resMan.inspect();
        logger.exiting("ResManTest", "loadTemplate");
        logger.info("EMPTY");
    }

    @Test
    public void createCreature() {
        logger.entering("ResManTest", "createCreature");
        ResMan resMan = new ResMan("res/global", "res/explorer");
        resMan.loadTemplates("templates/creatures.def", CreatureTpl.class);
        Entity<CreatureTpl> warrior = resMan.createCreature("creature/warrior");
        logger.exiting("ResManTest", "createCreature");
        logger.info("EMPTY");
    }
}
