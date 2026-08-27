package zerozero.opensource.service;

import org.springframework.stereotype.Service;

@Service
public class AnswerGenerationService {

  private final OllamaChatService chatService;

  public AnswerGenerationService(OllamaChatService chatService) {
    this.chatService = chatService;
  }

  public String generate(String question, String toolName, String toolResult) {
    return chatService.chat(
        """
                        너는 Company-X 데이터 분석 도우미다.
                        반드시 제공된 도구 결과만 근거로 한국어로 답변한다.
                        근거가 부족하면 부족하다고 말한다.
                        답변은 짧고 명확하게 작성한다.
                        """,
        """
                        [사용자 질문]
                        %s

                        [사용한 도구]
                        %s

                        [도구 결과]
                        %s
                        """
            .formatted(question, toolName, toolResult));
  }
}
