package com.wwfish.cms.model;

/**
 * Created by IntelliJ IDEA.
 * User: Tom
 * Date: 2010-8-11
 * Time: 14:16:46
 * To change this template use File | Settings | File Templates.
 */
public class NewsDto extends BaseContentDto {

    private String body;

    private String refer;

    private String digest;

    private String author;

    private String image;

    private String shortName;

    private String name;

    private String channelNameExt;

    public String getChannelNameExt() {
        return channelNameExt;
    }

    public void setChannelNameExt(String channelNameExt) {
        this.channelNameExt = channelNameExt;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getRefer() {
        return refer;
    }

    public void setRefer(String refer) {
        this.refer = refer;
    }

    public String getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }
}
