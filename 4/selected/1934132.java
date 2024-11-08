package com.sin.server.cronservlet;

import java.util.List;
import java.util.logging.Logger;
import com.google.appengine.api.channel.ChannelMessage;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.inject.Inject;
import com.google.web.bindery.autobean.vm.AutoBeanFactorySource;
import com.sin.shared.autobean.ABeanFactory;
import com.sin.shared.autobean.AutoBeanHandlerImpl;
import com.sin.shared.domains.MessageAutoBean;
import de.beimax.janag.NameGenerator;

public class CronServletServiceImpl implements CronServletService {

    private static final Logger log = Logger.getLogger(CronServletServiceImpl.class.getName());

    private CronServletDao dao;

    private NameGenerator ng;

    @Inject
    public void setDao(CronServletDao dao) {
        this.dao = dao;
    }

    @Override
    public void doService() {
        List<String> list = dao.getList();
        ng = new NameGenerator("languages.txt", "semantics.txt");
        String[] namegn = ng.getRandomName("Finnish", "Female", 1);
        ABeanFactory factory = AutoBeanFactorySource.create(ABeanFactory.class);
        AutoBeanHandlerImpl autoBeanHandlerImpl = new AutoBeanHandlerImpl();
        ChannelService channelService = ChannelServiceFactory.getChannelService();
        for (String data : list) {
            log.info(data);
            MessageAutoBean messageAutoBean = autoBeanHandlerImpl.makeMessageAutoBean(factory);
            messageAutoBean.setAbout("girlconnected");
            messageAutoBean.setContext("Connected: " + namegn[0]);
            String jsonstr = autoBeanHandlerImpl.serializeToJsonMessageAutoBean(messageAutoBean);
            ChannelMessage message = new ChannelMessage(data, jsonstr);
            channelService.sendMessage(message);
        }
    }
}
