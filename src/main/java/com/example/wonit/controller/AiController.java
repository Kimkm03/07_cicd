package com.example.wonit.controller;

import com.example.wonit.ai.BankDocs;
import com.example.wonit.ai.BankTools;
import com.example.wonit.dto.ChatRequest;
import com.example.wonit.dto.ChatResponse;
import com.example.wonit.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

// 🤖 거래 메모를 AI 에게 보내 카테고리 하나로 분류한다. AI 는 실패할 수 있으므로 항상 "기타" 로 대체할 길을 둔다
@Slf4j
@RestController
@RequestMapping("/api/ai")
public class AiController {

    record CategoryRequest(String memo) {}
    record CategoryResponse(String memo, String category) {}

    private static final List<String> CATEGORIES = List.of("식비", "쇼핑", "교통", "급여", "저축", "이체", "기타");

    private final ChatClient chatClient;
    private final ReportService reportService;
    private final ChatMemory chatMemory;        // Spring AI 가 자동으로 만들어 준다 (서버 메모리, 최근 20개 메시지)
    private final Resource savingsPrompt;       // 시스템 프롬프트 파일
    private final BankDocs bankDocs;            // (선택) RAG: 은행 안내문 검색 도구

    // 프로필(gemini · openrouter · openai)에 따라 어느 회사 AI 가 붙을지 정해진다. 코드는 그대로다
    // 프롬프트를 코드 밖 파일로 → 코드 수정 · 재컴파일 없이 문구만 고친다. {today} 같은 자리는 실행할 때 채운다
    public AiController(ChatClient.Builder builder, ReportService reportService, ChatMemory chatMemory,
                        @Value("classpath:prompts/savings-assistant.st") Resource savingsPrompt, BankDocs bankDocs) {
        this.chatClient = builder.defaultAdvisors(new SimpleLoggerAdvisor()).build();
        this.reportService = reportService;
        this.chatMemory = chatMemory;
        this.savingsPrompt = savingsPrompt;
        this.bankDocs = bankDocs;
    }

    @PostMapping("/category")
    public CategoryResponse category(@RequestBody CategoryRequest request) {
        String category;
        try {
            category = chatClient.prompt()
                    .system("은행 거래 메모를 분류한다. " + CATEGORIES + " 중 정확히 하나만 고르고, 그 단어만 답한다.")
                    .user(request.memo())
                    .call().content().strip();
        } catch (Exception e) {      // 키 없음 · 시간 초과 등 → 분류 실패가 거래 자체를 막으면 안 된다
            log.warn("AI 분류 실패 → 기타: {}", e.getMessage());
            category = "기타";
        }
        if (!CATEGORIES.contains(category)) {      // 🚨 AI 의 답은 믿지 말고 목록에 있는지 확인한다
            log.warn("AI 가 목록에 없는 답을 함: {} → 기타", category);
            category = "기타";
        }
        return new CategoryResponse(request.memo(), category);
    }

    // 💬 AI 저축 도우미 (Tool Calling)
    //   - 숫자는 DB 가 계산하고(리포트), 문장은 AI 가 만든다
    //   - AI 에게 BankTools 의 메서드를 "도구" 로 건넨다. 어떤 도구를 부를지는 AI 가 고르고, 실제 실행은 우리 서버가 한다
    //   - 🔐 BankTools 는 토큰 주인의 customerId 로 만든다 → AI 가 다른 고객 번호를 넣을 수 없다
    //   - 📚 (선택) RAG: 규정 · 상품 안내는 BankDocs 의 검색 도구로 → 숫자 질문이면 BankTools, 규정 질문이면 BankDocs, 둘 다면 둘 다 AI 가 부른다
    //   - 대화를 서버 메모리(ChatMemory)에 저장해서 "그럼 그 중에 식비는?" 같은 이어지는 질문을 이해한다
    //     대화 번호 = customerId → 🔐 고객마다 대화가 따로 저장된다. 서버를 다시 켜면 사라진다 (DB 에 저장하려면 JDBC ChatMemoryRepository — 설명만)
    @Operation(summary = "AI 저축 도우미 (Tool Calling + RAG)")
    @PostMapping("/chat")
    public ChatResponse chat(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChatRequest request) {
        Integer customerId = Integer.valueOf(jwt.getSubject());
        try {
            String reply = chatClient.prompt()
                    .system(sys -> sys.text(savingsPrompt).param("today", LocalDate.now()))
                    .advisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, String.valueOf(customerId)))
                    .user(request.message())
                    .tools(new BankTools(customerId, reportService), bankDocs)   // 숫자 도구 + 안내문 검색 도구
                    .call().content();
            return new ChatResponse(reply);
        } catch (Exception e) {      // 키 없음 · 시간 초과 등 → category 와 같은 생각: AI 가 실패해도 서버는 멀쩡히 답한다
            log.warn("AI 도우미 실패: {}", e.getMessage());
            return new ChatResponse("지금은 AI 도우미를 쓸 수 없습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    // 🧹 "새 대화": 내 대화 기억만 지운다
    @Operation(summary = "AI 저축 도우미 대화 지우기")
    @DeleteMapping("/chat")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearChat(@AuthenticationPrincipal Jwt jwt) {
        chatMemory.clear(jwt.getSubject());
    }
}
