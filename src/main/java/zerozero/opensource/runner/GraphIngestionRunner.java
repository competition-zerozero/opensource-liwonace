package zerozero.opensource.runner;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import zerozero.opensource.config.GraphIngestionProperties;
import zerozero.opensource.service.GraphIngestionService;

@Component
public class GraphIngestionRunner implements ApplicationRunner {

    private final GraphIngestionProperties properties;
    private final GraphIngestionService ingestionService;

    public GraphIngestionRunner(GraphIngestionProperties properties, GraphIngestionService ingestionService) {
        this.properties = properties;
        this.ingestionService = ingestionService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!properties.enabled()) {
            return;
        }

        GraphIngestionService.GraphIngestionResult result = ingestionService.ingest(properties.path());
        System.out.println("그래프 데이터 적재 완료: "
                + result.nodeCount() + "개 node, "
                + result.edgeCount() + "개 edge");
    }
}
