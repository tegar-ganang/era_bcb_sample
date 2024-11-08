package com.tcs.hrr.action;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.interceptor.validation.SkipValidation;
import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.validator.annotations.Validations;
import com.tcs.hrr.domain.Candidate;
import com.tcs.hrr.domain.Channel;
import com.tcs.hrr.domain.ChannelSource;
import com.tcs.hrr.domain.Datadictionary;
import com.tcs.hrr.domain.Skill;
import com.tcs.hrr.domain.Technology;
import com.tcs.hrr.domain.candidateCompetency;
import com.tcs.hrr.service.CandidateCompetencyManager;
import com.tcs.hrr.service.CandidateManager;
import com.tcs.hrr.service.ChannelManager;
import com.tcs.hrr.service.ChannelSourceManager;
import com.tcs.hrr.service.DataDictionaryManager;
import com.tcs.hrr.service.SkillManager;
import com.tcs.hrr.service.TechnologyManager;
import com.tcs.hrr.util.Constant;
import com.tcs.hrr.util.FileAccessUtil;

public class CandidateAction extends ActionSupport {

    private Candidate candidate;

    private DataDictionaryManager datadictionaryManager;

    private SkillManager skillManager;

    private CandidateCompetencyManager candidateCompetencyManager;

    private CandidateManager candidateManager;

    private TechnologyManager technologyManager;

    private ChannelManager channelManager;

    private ChannelSourceManager channelSourceManager;

    private java.util.List<File> uploads;

    private java.util.List<String> uploadsFileName;

    private java.util.List<String> uploadsContentType;

    private String savePath;

    private String province;

    private String city;

    private String channel;

    private String channelSource;

    private String technology;

    private String skill;

    private String test;

    public String getTest() {
        return test;
    }

    public void setTest(String test) {
        this.test = test;
    }

    private ArrayList<Datadictionary> provinces;

    private Map<String, ArrayList<Datadictionary>> area;

    private ArrayList<Technology> technologys;

    private Map<Integer, ArrayList<Skill>> skillMap;

    private ArrayList<Channel> channels;

    private Map<Integer, ArrayList<ChannelSource>> channelsourceMap;

    public String getSavePath() {
        return ServletActionContext.getServletContext().getRealPath(savePath);
    }

    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    public java.util.List<String> getUploadsFileName() {
        return uploadsFileName;
    }

    public void setUploadsFileName(java.util.List<String> uploadsFileName) {
        this.uploadsFileName = uploadsFileName;
    }

    public java.util.List<String> getUploadsContentType() {
        return uploadsContentType;
    }

    public void setUploadsContentType(java.util.List<String> uploadsContentType) {
        this.uploadsContentType = uploadsContentType;
    }

    public ArrayList<Channel> getChannels() {
        return channels;
    }

    public void setChannels(ArrayList<Channel> channels) {
        this.channels = channels;
    }

    public Map<Integer, ArrayList<ChannelSource>> getChannelsourceMap() {
        return channelsourceMap;
    }

    public void setChannelsourceMap(Map<Integer, ArrayList<ChannelSource>> channelsourceMap) {
        this.channelsourceMap = channelsourceMap;
    }

    public ChannelManager getChannelManager() {
        return channelManager;
    }

    public void setChannelManager(ChannelManager channelManager) {
        this.channelManager = channelManager;
    }

    public TechnologyManager getTechnologyManager() {
        return technologyManager;
    }

    public void setTechnologyManager(TechnologyManager technologyManager) {
        this.technologyManager = technologyManager;
    }

    public ArrayList<Technology> getTechnologys() {
        return technologys;
    }

    public void setTechnologys(ArrayList<Technology> technologys) {
        this.technologys = technologys;
    }

    public Map<Integer, ArrayList<Skill>> getSkillMap() {
        return skillMap;
    }

    public void setSkillMap(Map<Integer, ArrayList<Skill>> skillMap) {
        this.skillMap = skillMap;
    }

    public ArrayList<Datadictionary> getProvinces() {
        return provinces;
    }

    public void setProvinces(ArrayList<Datadictionary> provinces) {
        this.provinces = provinces;
    }

    public Map<String, ArrayList<Datadictionary>> getArea() {
        return area;
    }

    public void setArea(Map<String, ArrayList<Datadictionary>> area) {
        this.area = area;
    }

    public SkillManager getSkillManager() {
        return skillManager;
    }

    public void setSkillManager(SkillManager skillManager) {
        this.skillManager = skillManager;
    }

    public CandidateCompetencyManager getCandidateCompetencyManager() {
        return candidateCompetencyManager;
    }

    public void setCandidateCompetencyManager(CandidateCompetencyManager candidateCompetencyManager) {
        this.candidateCompetencyManager = candidateCompetencyManager;
    }

    public CandidateManager getCandidateManager() {
        return candidateManager;
    }

    public void setCandidateManager(CandidateManager candidateManager) {
        this.candidateManager = candidateManager;
    }

    public ChannelSourceManager getChannelSourceManager() {
        return channelSourceManager;
    }

    public void setChannelSourceManager(ChannelSourceManager channelSourceManager) {
        this.channelSourceManager = channelSourceManager;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getChannelSource() {
        return channelSource;
    }

    public void setChannelSource(String channelSource) {
        this.channelSource = channelSource;
    }

    public String getTechnology() {
        return technology;
    }

    public void setTechnology(String technology) {
        this.technology = technology;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public DataDictionaryManager getDatadictionaryManager() {
        return datadictionaryManager;
    }

    public void setDatadictionaryManager(DataDictionaryManager datadictionaryManager) {
        this.datadictionaryManager = datadictionaryManager;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public Candidate getCandidate() {
        return candidate;
    }

    public void setCandidate(Candidate candidate) {
        System.out.println("set candidate" + candidate);
        this.candidate = candidate;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public java.util.List<File> getUploads() {
        return uploads;
    }

    public void setUploads(java.util.List<File> uploads) {
        this.uploads = uploads;
    }

    public String addCandidate() throws Exception {
        String curLocation = this.province + "/" + this.city;
        this.candidate.setCurLocation(curLocation);
        ArrayList<ChannelSource> channelSources = (ArrayList<ChannelSource>) this.channelSourceManager.findByProperty("channelSourceName", channelSource);
        if (channelSources != null && channelSources.size() != 0) this.candidate.setChannelSource(channelSources.get(0));
        candidateCompetency ac = new candidateCompetency();
        ArrayList<Skill> skills = (ArrayList<Skill>) this.skillManager.findSkillByProperty("skillName", this.skill);
        if (skills != null && skills.size() != 0) {
            ac.setSkill(skills.get(0));
            ac.setCandidate(this.candidate);
        }
        Set<candidateCompetency> candidateCompetencies = new HashSet<candidateCompetency>();
        candidateCompetencies.add(ac);
        uploadCV();
        this.candidate.setCreateBy("TAG owner");
        this.candidate.setcandidateCompetencies(candidateCompetencies);
        this.candidate.setCreateDate(new Date());
        this.candidateManager.saveCandidate(this.candidate);
        this.candidateCompetencyManager.save(ac);
        return SUCCESS;
    }

    @SkipValidation
    public String start() throws Exception {
        area = new HashMap<String, ArrayList<Datadictionary>>();
        provinces = new ArrayList<Datadictionary>();
        ArrayList<String> provinceStrList = (ArrayList<String>) this.datadictionaryManager.getDataDictionarybySQL("select distinct dataValue from Datadictionary where dataType='location'");
        for (int i = 0; i < provinceStrList.size(); i++) {
            Datadictionary datadictionary = new Datadictionary();
            datadictionary.setDataValue(provinceStrList.get(i));
            datadictionary.setDataType("location");
            provinces.add(datadictionary);
        }
        for (int i = 0; i < provinces.size(); i++) {
            ArrayList<Datadictionary> citys = (ArrayList<Datadictionary>) this.datadictionaryManager.getCitysBySQL(provinces.get(i).getDataValue());
            area.put(provinces.get(i).getDataValue(), citys);
        }
        this.skillMap = new HashMap<Integer, ArrayList<Skill>>();
        this.technologys = (ArrayList<Technology>) this.technologyManager.findTechnologyAll();
        for (int i = 0; i < this.technologys.size(); i++) {
            ArrayList<Skill> skills = (ArrayList<Skill>) this.skillManager.findSkillByProperty("technology", this.technologys.get(i));
            this.skillMap.put(this.technologys.get(i).getTechnologyId(), skills);
        }
        this.channelsourceMap = new HashMap<Integer, ArrayList<ChannelSource>>();
        this.channels = (ArrayList<Channel>) this.channelManager.findChannelAll();
        for (int i = 0; i < channels.size(); i++) {
            ArrayList<ChannelSource> sources = (ArrayList<ChannelSource>) this.channelSourceManager.findByProperty("channel", channels.get(i));
            this.channelsourceMap.put(channels.get(i).getChannelId(), sources);
        }
        return SUCCESS;
    }

    @Override
    public String execute() throws Exception {
        return "start";
    }

    public void uploadCV() {
        String uploadType = Constant.FILE_TYPE_CV;
        StringBuffer[] cvs = new StringBuffer[2];
        System.out.println("fileNames:" + uploadsFileName);
        System.out.println("uploadContentTypes:" + uploadsContentType);
        if (uploads != null) {
            if (uploads.get(0) != null) {
                cvs[0] = new StringBuffer("");
                cvs[0].append("en_" + FileAccessUtil.generateUploadFileName(uploadsFileName.get(0), uploadType));
            }
            if (uploads.get(1) != null) {
                cvs[1] = new StringBuffer("");
                cvs[1].append("cn_" + FileAccessUtil.generateUploadFileName(uploadsFileName.get(1), uploadType));
            }
            System.out.println(cvs[0].toString());
            System.out.println(cvs[1].toString());
            File dir = new File(this.savePath);
            if (!dir.isDirectory()) dir.mkdirs();
            File target_cn = new File(this.savePath, cvs[0].toString());
            File target_en = new File(this.savePath, cvs[1].toString());
            try {
                FileUtils.copyFile(uploads.get(0), target_cn);
                FileUtils.copyFile(uploads.get(1), target_en);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.candidate.setCv_path(savePath + "/" + cvs[0] + ";" + savePath + "/" + cvs[1]);
    }
}
