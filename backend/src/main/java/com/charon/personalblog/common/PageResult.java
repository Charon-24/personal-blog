package com.charon.personalblog.common;

import java.util.List;

public record PageResult<T>(List<T> items, long total, int page, int size) {
    public static int offset(int page, int size) {
        return Math.max(0, page - 1) * Math.min(Math.max(size, 1), 100);
    }

    public static int safeSize(int size) {
        return Math.min(Math.max(size, 1), 100);
    }
}
