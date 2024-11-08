package ru.ksu.niimm.cll.mocassin.crawl.arxiv;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;
import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.ksu.niimm.cll.mocassin.crawl.arxiv.ArxivDAOFacade;
import ru.ksu.niimm.cll.mocassin.crawl.arxiv.ArxivModule;
import ru.ksu.niimm.cll.mocassin.util.model.ArticleMetadata;
import ru.ksu.niimm.cll.mocassin.util.model.Link;
import com.google.inject.Inject;
import com.mycila.testing.junit.MycilaJunitRunner;
import com.mycila.testing.plugin.guice.GuiceContext;

@RunWith(MycilaJunitRunner.class)
@GuiceContext({ ArxivModule.class })
@Ignore("get an arxiv id for the test at random")
public class ArxivDAOFacadeTest {

    @Inject
    ArxivDAOFacade arxivDAOFacade;

    @Test
    public void testRetrieve() {
        ArticleMetadata metadata = arxivDAOFacade.retrieve("math/0205003");
        Assert.assertEquals("http://arxiv.org/abs/math/0205003v1", metadata.getId());
        Assert.assertEquals("A strengthening of the Nyman-Beurling criterion for the Riemann\n  hypothesis, 2", metadata.getTitle());
        List<Link> links = metadata.getLinks();
        String pdfLink = null;
        for (Link link : links) {
            if (link.getType().equals("application/pdf")) {
                pdfLink = link.getHref();
                break;
            }
        }
        Assert.assertNotNull(pdfLink);
        Assert.assertEquals("http://arxiv.org/pdf/math/0205003v1", pdfLink);
    }

    @Test
    public void testLoadSource() throws IOException {
        ArticleMetadata metadata = new ArticleMetadata();
        metadata.setId("http://arxiv.org/abs/math/0205003v1");
        InputStream inputStream = arxivDAOFacade.loadSource(metadata);
        Assert.assertNotNull(inputStream);
        StringWriter writer = new StringWriter();
        IOUtils.copy(inputStream, writer, "utf8");
        String contents = writer.toString();
        Assert.assertTrue(contents.contains("A strengthening of the Nyman"));
        inputStream.close();
    }
}
