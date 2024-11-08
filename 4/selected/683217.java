package index;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.util.Properties;
import javax.annotation.Resource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import parse.LuceneConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:maven-lucene-context.xml")
public class IndexBuilderSpringContextTest {

    Log log = LogFactory.getLog(IndexBuilderSpringContextTest.class);

    @Resource
    IndexBuilder builder;

    @Resource
    LuceneConfiguration configuration;

    @Test
    public void shouldLoadContext() {
        assertNotNull(builder);
    }

    @Test
    public void shouldBuildIndexContext() throws Exception {
        File indexLocation = configuration.getIndexLocation();
        long initalSize = 0;
        if (indexLocation.exists()) {
            FileUtils.cleanDirectory(indexLocation);
            initalSize = FileUtils.sizeOfDirectory(indexLocation);
        }
        builder.buildIndex();
        long finalSize = FileUtils.sizeOfDirectory(indexLocation);
        assertTrue(finalSize > initalSize);
        log.debug("Size of index directory before and after index build were " + initalSize + " and " + finalSize + " resp.");
    }
}

@Component
class MavenLuceneContextPropertiesLoader {

    private Properties properties;

    @Bean
    PropertyPlaceholderConfigurer propsLoader() throws Exception {
        copyXmlAndInitializeProps();
        PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();
        configurer.setProperties(properties);
        return configurer;
    }

    void copyXmlAndInitializeProps() throws Exception {
        File tempFile = File.createTempFile("sample-lucene", ".xml");
        ClassPathResource classPathResource = new ClassPathResource("lucene.xml");
        FileUtils.copyFile(classPathResource.getFile(), tempFile);
        properties = new Properties();
        properties.setProperty("mvn.lucene.configFileLocation", tempFile.getAbsolutePath());
    }
}
