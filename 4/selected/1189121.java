package org.telscenter.sail.webapp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import net.sf.sail.webapp.service.curnit.CurnitService;
import net.sf.sail.webapp.spring.SpringConfiguration;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.telscenter.sail.webapp.dao.module.impl.RooloLOROtmlModuleDao;
import org.telscenter.sail.webapp.domain.impl.CreateRooloLOROtmlModuleParameters;
import roolo.elo.BasicELO;
import roolo.elo.ELOMetadataKeys;
import roolo.elo.api.IContent;
import roolo.elo.api.IELO;
import roolo.elo.api.IMetadata;
import roolo.elo.api.IMetadataValueContainer;
import roolo.elo.api.IRepository;

/**
 * Adds default otml-curnits into Roolo repository.
 * 
 * @author Hiroki Terashima
 * @version $Id: CreateDefaultRooloOtmlCurnits.java 2102 2008-09-17 23:26:15Z hiroki $
 */
public class CreateDefaultRooloLOROtmlCurnits {

    private IRepository rep;

    private static final String WS_URL = "http://tels-web.soe.berkeley.edu:8080/lor/services/LORService";

    private static final String WS_USER = "admin";

    private static final String WS_PASS = "admin";

    private CurnitService curnitService;

    public CreateDefaultRooloLOROtmlCurnits(ConfigurableApplicationContext applicationContext) {
        this.setCurnitService((CurnitService) applicationContext.getBean("curnitService"));
    }

    private IELO createAirbagsCurnit() {
        IELO curnit = createCurnit("hydrogencarsweb.otml");
        return curnit;
    }

    private IELO createChemicalReactionsCurnit() {
        IELO curnit = createCurnit("ChemicalReactions.otml");
        return curnit;
    }

    private IELO createCurnit(String filename) {
        IELO curnit = new BasicELO();
        IContent content = curnit.getContent();
        URL url = CreateDefaultRooloLOROtmlCurnits.class.getResource(filename);
        try {
            FileInputStream fis;
            fis = new FileInputStream(new File(url.toURI()));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
            fis.close();
            content.setBytes(baos.toByteArray());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        IMetadata metadata1 = curnit.getMetadata();
        IMetadataValueContainer container;
        container = metadata1.getMetadataValueContainer(ELOMetadataKeys.TITLE.getKey());
        container.setValue(filename);
        container = metadata1.getMetadataValueContainer(ELOMetadataKeys.TYPE.getKey());
        container.setValue("Curnit");
        container = metadata1.getMetadataValueContainer(ELOMetadataKeys.DESCRIPTION.getKey());
        container.setValue("This is a test curnit based on a curnit");
        container = metadata1.getMetadataValueContainer(ELOMetadataKeys.AUTHOR.getKey());
        container.setValue("tony p");
        container = metadata1.getMetadataValueContainer(ELOMetadataKeys.FAMILYTAG.getKey());
        container.setValue("TELS");
        container = metadata1.getMetadataValueContainer(ELOMetadataKeys.ISCURRENT.getKey());
        container.setValue("yes");
        return curnit;
    }

    public void createDefaultCurnits(ConfigurableApplicationContext applicationContext) {
        IELO airbagsCurnit = createAirbagsCurnit();
        rep.addELO(airbagsCurnit);
        saveToLocalDb(applicationContext, airbagsCurnit);
    }

    /**
	 * @param applicationContext 
	 * @param lo
	 */
    private void saveToLocalDb(ConfigurableApplicationContext applicationContext, IELO lo) {
        CreateRooloLOROtmlModuleParameters params = (CreateRooloLOROtmlModuleParameters) applicationContext.getBean("createRooloLOROtmlModuleParameters");
        params.setName(lo.getMetadata().getMetadataValueContainer(ELOMetadataKeys.TITLE.getKey()).getValue().toString());
        params.setUrl(RooloLOROtmlModuleDao.defaultOtrunkCurnitUrl);
        params.setRoolouri(lo.getUri().toString());
        params.setRooloRepositoryUrl(RooloLOROtmlModuleDao.rooloRepositoryUrl);
        params.setLearningObject(lo);
        curnitService.createCurnit(params);
    }

    /**
	 * Creates Default Curnits
	 * @param args
	 */
    public static void main(String[] args) {
        try {
            ConfigurableApplicationContext applicationContext = null;
            SpringConfiguration springConfig;
            springConfig = (SpringConfiguration) BeanUtils.instantiateClass(Class.forName("org.telscenter.sail.webapp.spring.impl.SpringConfigurationImpl"));
            applicationContext = new ClassPathXmlApplicationContext(springConfig.getRootApplicationContextConfigLocations());
            CreateDefaultRooloLOROtmlCurnits cdc = new CreateDefaultRooloLOROtmlCurnits(applicationContext);
            cdc.createDefaultCurnits(applicationContext);
            applicationContext.close();
        } catch (BeanInstantiationException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
	 * @param curnitService the curnitService to set
	 */
    public void setCurnitService(CurnitService curnitService) {
        this.curnitService = curnitService;
    }
}
