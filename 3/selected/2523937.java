package com.googlecode.sarasvati.xml;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import com.googlecode.sarasvati.SarasvatiException;
import com.googlecode.sarasvati.load.SarasvatiLoadException;
import com.googlecode.sarasvati.load.definition.ExternalDefinition;
import com.googlecode.sarasvati.load.definition.NodeDefinition;
import com.googlecode.sarasvati.load.definition.ProcessDefinition;
import com.googlecode.sarasvati.util.SvUtil;

@XmlRootElement(name = "process-definition")
@XmlAccessorType(XmlAccessType.FIELD)
public class XmlProcessDefinition implements ProcessDefinition {

    @XmlAttribute(name = "name", required = true)
    protected String name;

    @XmlElement(name = "node")
    protected List<XmlNode> nodes = new ArrayList<XmlNode>();

    @XmlElement(name = "external")
    protected List<XmlExternal> externals = new ArrayList<XmlExternal>();

    @XmlTransient
    protected String messageDigest = null;

    @Override
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public List<XmlNode> getNodes() {
        return nodes;
    }

    public void setNodes(final List<XmlNode> nodes) {
        this.nodes = nodes;
    }

    @Override
    public List<XmlExternal> getExternals() {
        return externals;
    }

    public void setExternals(final List<XmlExternal> externals) {
        this.externals = externals;
    }

    @Override
    public String getMessageDigest() throws SarasvatiLoadException {
        if (messageDigest == null) {
            Collections.sort(nodes);
            Collections.sort(externals);
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA1");
                digest.update(name.getBytes());
                for (XmlNode node : nodes) {
                    node.addToDigest(digest);
                }
                for (XmlExternal external : externals) {
                    external.addToDigest(digest);
                }
                messageDigest = SvUtil.getHexString(digest.digest());
            } catch (NoSuchAlgorithmException nsae) {
                throw new SarasvatiException("Unable to load SHA1 algorithm", nsae);
            }
        }
        return messageDigest;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("<workflow name=\"");
        buf.append(getName());
        buf.append("\">\n");
        for (NodeDefinition node : nodes) {
            buf.append(node);
            buf.append("\n");
        }
        for (ExternalDefinition external : externals) {
            buf.append(external);
            buf.append("\n");
        }
        buf.append("</workflow>");
        return buf.toString();
    }
}
