package com.example.wonit;

import com.example.wonit.ai.BankDocs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// (선택) RAG: 진짜 AI 대신 "가짜 임베딩" 으로, 안내문 검색 도구가 맞는 문서를 찾는지 확인한다 (키 · 네트워크 없이)
@SpringBootTest
class RagFlowTest {

    @TestConfiguration
    static class FakeEmbedding {
        // 단어가 몇 번 나오는지 세어 벡터로 만든다. 같은 단어가 많이 겹치면 가깝다
        static final List<String> WORDS = List.of("휴면", "지급정지", "적금", "금리", "이체", "입금", "출금", "해제");

        @Bean
        @Primary
        EmbeddingModel fakeEmbeddingModel() {
            return new EmbeddingModel() {
                @Override
                public EmbeddingResponse call(EmbeddingRequest request) {
                    List<Embedding> list = new ArrayList<>();
                    for (int i = 0; i < request.getInstructions().size(); i++) {
                        list.add(new Embedding(vector(request.getInstructions().get(i)), i));
                    }
                    return new EmbeddingResponse(list);
                }

                @Override
                public float[] embed(Document document) {
                    return vector(document.getText());
                }
            };
        }

        static float[] vector(String text) {
            float[] v = new float[WORDS.size() + 1];
            for (int i = 0; i < WORDS.size(); i++) {
                v[i] = text.split(WORDS.get(i), -1).length - 1;
            }
            v[WORDS.size()] = 0.1f;     // 모든 칸이 0 이면 비교할 수 없어서 작은 값 하나
            return v;
        }
    }

    @Autowired
    BankDocs bankDocs;

    @Test
    @DisplayName("'휴면 계좌 해제' 를 검색하면 휴면 안내문이 1등")
    void searchDormant() {
        List<String> found = bankDocs.searchBankRules("휴면 계좌 해제는 어떻게 해요?");
        assertThat(found.get(0)).startsWith("[dormant.md]");
    }

    @Test
    @DisplayName("'적금 금리' 를 검색하면 적금 안내문이 1등")
    void searchSavings() {
        List<String> found = bankDocs.searchBankRules("적금 금리가 얼마예요?");
        assertThat(found.get(0)).startsWith("[savings.md]");
    }
}
