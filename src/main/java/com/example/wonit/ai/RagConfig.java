package com.example.wonit.ai;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 📚 (선택) RAG: 문서 조각을 담아 두는 벡터 저장소
//   - EmbeddingModel(글 → 숫자 벡터)은 Spring AI 가 자동으로 만들어 준다 (프로필의 임베딩 모델 사용)
//   - 학습용이라 메모리에만 둔다 → 앱을 다시 켜면 비어 있다. 실무는 PGVector · Redis 같은 DB 에 둔다
@Configuration
public class RagConfig {

    @Bean
    VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
