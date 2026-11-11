package com.example.wonit;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MyTest {

    @Test
    public void givenInitialState_whenActionIsPerformed_thenExpectedOutcome() {
        // Given
        // 초기 상태 설정
        int initialValue = 1;

        // When
        // 동작 수행
        int result = addOne(initialValue);

        // Then
        // 예상 결과 확인
        // assertTrue(true);
        assertEquals(2, result, "1 + 1 should equal 2");

    }

    private int addOne(int value) {
        return value + 1;
    }
}
