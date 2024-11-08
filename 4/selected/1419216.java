package org.telscenter.sail.webapp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.telscenter.sail.webapp.dao.module.impl.RooloOtmlModuleDao;
import org.telscenter.sail.webapp.domain.impl.CreateOtmlModuleParameters;
import net.sf.sail.webapp.service.curnit.CurnitService;
import net.sf.sail.webapp.spring.SpringConfiguration;

/**
 * Adds default otml curnits to local database.
 * 
 * @author Hiroki Terashima
 * @version $Id: CreateDefaultOtmlCurnits.java 2330 2009-01-14 19:04:37Z honchikun $
 */
public class CreateDefaultOtmlCurnits {

    private CurnitService curnitService;

    public CreateDefaultOtmlCurnits(ConfigurableApplicationContext applicationContext) {
        this.setCurnitService((CurnitService) applicationContext.getBean("curnitService"));
    }

    public void createDefaultCurnits(ConfigurableApplicationContext applicationContext) {
        createCurnit("airbags", CreateDefaultOtmlCurnits.class.getResource("airbags.otml"));
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
            CreateDefaultOtmlCurnits cdoc = new CreateDefaultOtmlCurnits(applicationContext);
            cdoc.createDefaultCurnits(applicationContext);
            applicationContext.close();
        } catch (BeanInstantiationException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void createCurnit(String name, URL otmlUrl) {
        CreateOtmlModuleParameters params = new CreateOtmlModuleParameters();
        try {
            FileInputStream fis;
            fis = new FileInputStream(new File(otmlUrl.toURI()));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
            fis.close();
            params.setOtml(baos.toByteArray());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        params.setName(name);
        params.setUrl(RooloOtmlModuleDao.defaultOtrunkCurnitUrl);
        curnitService.createCurnit(params);
    }

    /**
	 * @param curnitService the curnitService to set
	 */
    public void setCurnitService(CurnitService curnitService) {
        this.curnitService = curnitService;
    }
}
