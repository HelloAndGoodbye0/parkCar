package com.parkcar.common;

import lombok.Data;

import java.util.List;

/**
 * 分页结果
 */
@Data
public class PageResult<T> {

    private long total;
    private long pages;
    private long current;
    private long size;
    private List<T> records;

    public static <T> PageResult<T> of(long total, long current, long size, List<T> records) {
        PageResult<T> p = new PageResult<>();
        p.total = total;
        p.current = current;
        p.size = size;
        p.pages = size == 0 ? 0 : (total + size - 1) / size;
        p.records = records;
        return p;
    }
}
