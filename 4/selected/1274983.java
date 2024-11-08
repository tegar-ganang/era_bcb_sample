package com.google.code.guidatv.server.service.rest;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import org.restlet.resource.ServerResource;
import com.google.code.guidatv.client.service.ChannelService;
import com.google.code.guidatv.client.service.impl.ChannelServiceImpl;
import com.google.code.guidatv.model.Schedule;
import com.google.code.guidatv.rest.ChannelScheduleResource;
import com.google.code.guidatv.server.service.ScheduleService;
import com.google.code.guidatv.server.service.impl.ScheduleServiceImpl;

public class ChannelScheduleServerResource extends ServerResource implements ChannelScheduleResource {

    private ChannelService channelService = new ChannelServiceImpl();

    private ScheduleService service = new ScheduleServiceImpl();

    @Override
    public Schedule retrieve() {
        Map<String, Object> attributes = getRequestAttributes();
        String channel = (String) attributes.get("channel");
        String dateString = (String) attributes.get("date");
        DateFormat format = new SimpleDateFormat("yyyyMMddZ");
        Date date;
        try {
            date = format.parse(dateString);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return service.getSchedule(channelService.getChannelByCode(channel), date);
    }
}
