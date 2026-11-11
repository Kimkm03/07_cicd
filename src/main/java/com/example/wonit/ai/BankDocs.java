package com.example.wonit.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

// 📚 (선택) RAG: resources/rag/*.md 은행 안내문을 벡터 저장소에 넣고, AI 저축 도우미에게 "안내문 검색 도구" 로 건넨다
//   ① 읽기(TextReader) → ② 자르기(TokenTextSplitter) → ③ 임베딩 + 저장(vectorStore.add)
//   처음 쓸 때 한 번만 넣는다 → AI 키가 없어도 앱은 뜬다 (넣다가 실패하면 다음 요청 때 다시 시도)
//   Tool Calling(BankTools)은 DB 의 "숫자" 를, 이 도구는 글로 된 "규정" 을 AI 에게 가져다준다. 어느 쪽을 부를지는 AI 가 고른다
@Slf4j
@Component
public class BankDocs {

    private final VectorStore vectorStore;
    private final Resource[] files;
    private boolean loaded = false;

    public BankDocs(VectorStore vectorStore, @Value("classpath:rag/*.md") Resource[] files) {
        this.vectorStore = vectorStore;
        this.files = files;
    }

    public synchronized VectorStore store() {
        if (!loaded) {
            List<Document> docs = new ArrayList<>();
            for (Resource file : files) {
                docs.addAll(new TextReader(file).get());                       // ① 읽기 (metadata 의 source = 파일 이름)
            }
            List<Document> chunks = new TokenTextSplitter().apply(docs);      // ② 긴 글은 조각으로 자른다
            vectorStore.add(chunks);                                          // ③ 조각마다 임베딩해서 저장
            loaded = true;
            log.info("RAG 문서 {}개 → 조각 {}개 저장", files.length, chunks.size());
            saveToFile();
        }
        return vectorStore;
    }

    // 🔧 도구: 규정 · 상품 안내가 필요한 질문이면 AI 가 이 메서드를 부른다 (질문과 가장 비슷한 안내문 조각 2개)
    @Tool(description = "우리 은행의 규정 · 상품 안내문 검색 (적금 금리, 휴면 계좌 해제, 지급정지, 이체 규칙 등)")
    public List<String> searchBankRules(@ToolParam(description = "찾을 내용, 예: 휴면 계좌 해제 방법") String question) {
        log.info("AI 가 도구 호출: searchBankRules({})", question);
        return store().similaritySearch(SearchRequest.builder().query(question).topK(2).build()).stream()
                .map(d -> "[" + d.getMetadata().get(TextReader.SOURCE_METADATA) + "] " + d.getText())   // [dormant.md] 안내문 내용
                .toList();
    }

    // 🔍 저장소 안을 들여다보기: 메모리에만 있는 내용을 build/vector-store.json 으로 꺼내 둔다 (조각 글 · 출처 파일 · 숫자 벡터)
    //    실패해도 RAG 는 그대로 쓴다 (보기용일 뿐)
    private void saveToFile() {
        if (vectorStore instanceof SimpleVectorStore simple) {
            File file = new File("build/vector-store.json");
            try {
                simple.save(file);
                log.info("벡터 저장소 내용을 파일로 저장: {}", file.getAbsolutePath());
            } catch (Exception e) {
                log.warn("벡터 저장소 파일 저장 실패 (RAG 는 계속 쓴다): {}", e.getMessage());
            }
        }
    }
}
