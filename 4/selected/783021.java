package net.sourceforge.dita4publishers.tools.dxp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import net.sourceforge.dita4publishers.api.bos.BosException;
import net.sourceforge.dita4publishers.api.bos.BosMember;
import net.sourceforge.dita4publishers.api.bos.BoundedObjectSet;
import net.sourceforge.dita4publishers.impl.dita.AddressingUtil;
import net.sourceforge.dita4publishers.impl.ditabos.DitaBosVisitorBase;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * DITA Bos visitor that blindly copies the members to a new location.
 * Does not do any pointer rewriting.
 */
public class MapCopyingBosVisitor extends DitaBosVisitorBase {

    private static Log log = LogFactory.getLog(MapCopyingBosVisitor.class);

    private URI baseUri;

    private URL outputUrl;

    /**
	 * @param log
	 */
    public MapCopyingBosVisitor(Log log) {
        super(log);
    }

    /**
	 * @param rootUrl URL of the root map 
	 * @param outputDir
	 * @throws MalformedURLException 
	 */
    public MapCopyingBosVisitor(File outputDir) throws MalformedURLException {
        super(log);
        this.outputUrl = outputDir.toURI().toURL();
    }

    public void visit(BoundedObjectSet bos) throws Exception {
        this.bos = bos;
        this.rootMember = bos.getRoot();
        if (this.rootMember == null) {
            throw new RuntimeException("No root member for BOS. This visitor requires a single root BOS member");
        }
        try {
            this.baseUri = AddressingUtil.getParent(rootMember.getDataSourceUri());
        } catch (URISyntaxException e) {
            throw new BosException(e);
        } catch (MalformedURLException e) {
            throw new BosException(e);
        } catch (IOException e) {
            throw new BosException(e);
        }
        for (BosMember member : bos.getMembers()) {
            member.accept(this);
        }
    }

    public void visit(BosMember member) throws BosException {
        String relative = AddressingUtil.getRelativePath(member.getDataSourceUri(), baseUri);
        URL resultUrl;
        try {
            resultUrl = new URL(outputUrl, relative);
            File resultFile = new File(resultUrl.toURI());
            resultFile.getParentFile().mkdirs();
            log.info("Creating result file \"" + resultFile.getAbsolutePath() + "\"...");
            IOUtils.copy(member.getInputStream(), new FileOutputStream(resultFile));
        } catch (Exception e) {
            throw new BosException(e);
        }
    }
}
