package com.sparta.logistics.presentation.common.constant;

public final class HeaderConstants {
    public static final String USER_ID = "X-User-Id";
    public static final String USER_ROLE = "X-User-Role";

    // 인스턴스화 방지 (유틸리티/상수 클래스 패턴)
    private HeaderConstants() {
    }
}