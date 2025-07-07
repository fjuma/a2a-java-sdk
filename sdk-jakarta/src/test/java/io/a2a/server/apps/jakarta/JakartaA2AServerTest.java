package io.a2a.server.apps.jakarta;



import io.a2a.server.apps.common.AbstractA2AServerTest;
import io.a2a.server.apps.common.AgentCardProducer;
import io.a2a.server.apps.common.AgentExecutorProducer;
import io.a2a.server.events.InMemoryQueueManager;
import io.a2a.server.tasks.TaskStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.container.annotation.ArquillianTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;

@ArquillianTest
@ApplicationScoped
public class JakartaA2AServerTest extends AbstractA2AServerTest {

    public JakartaA2AServerTest() {
        super(8080);
    }

    @Deployment
    public static WebArchive createTestArchive() throws IOException {
        final List<String> prefixes = List.of(
                    "a2a-java-sdk-client",
                    "a2a-java-sdk-common",
                    "a2a-java-sdk-server-common",
                    "a2a-java-sdk-spec",
                    "jackson",
                    "mutiny",
                    "slf4j",
                    "rest-assured",
                    "groovy",
                    "http",
                    "commons",
                    "xml-path",
                    "json-path",
                    "hamcrest"
            );
        List<File> libraries = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get("target").resolve("lib"))) {
            for (Path file : stream) {
                String fileName = file.getFileName().toString();
                if (prefixes.stream().anyMatch(fileName::startsWith)) {
                    libraries.add(file.toFile());
                }
            }
        }
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "ROOT.war")
                .addAsLibraries(libraries.toArray(new File[libraries.size()]))
                .addClass(AbstractA2AServerTest.class)
                .addClass(AgentCardProducer.class)
                .addClass(AgentExecutorProducer.class)
                .addClass(JakartaA2AServerTest.class)
                .addClass(A2ARequestFilter.class)
                .addClass(A2AServerResource.class)
                .addClass(RestApplication.class)
                .addAsManifestResource("META-INF/beans.xml", "beans.xml")
                .addAsWebInfResource("META-INF/beans.xml", "beans.xml")
                .addAsWebInfResource("WEB-INF/web.xml", "web.xml");
        return archive;
    }
    
    @Inject
    TaskStore taskStore;

    @Inject
    InMemoryQueueManager queueManager;

    @Override
    protected TaskStore getTaskStore() {
        return taskStore;
    }

    @Override
    protected InMemoryQueueManager getQueueManager() {
        return queueManager;
    }

    @Override
    protected void setStreamingSubscribedRunnable(Runnable runnable) {
        A2AServerResource.setStreamingIsSubscribedRunnable(runnable);
    }
}
