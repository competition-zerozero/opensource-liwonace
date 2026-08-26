package zerozero.opensource.tool;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import zerozero.opensource.dto.SqlQueryResult;
import zerozero.opensource.service.Nl2SqlService;

@Component
public class Nl2SqlTool {

    private final Nl2SqlService nl2SqlService;

    public Nl2SqlTool(Nl2SqlService nl2SqlService) {
        this.nl2SqlService = nl2SqlService;
    }

    @McpTool(name = "nl2sql", description = "자연어 질문을 안전한 SELECT SQL로 변환해 Company-X 정형 데이터를 조회합니다.")
    public SqlQueryResult nl2sql(
            @McpToolParam(description = "SQL로 변환할 자연어 질문입니다.", required = true) String question
    ) {
        return nl2SqlService.ask(question);
    }
}
