package com.grad.venue.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

/** 分页返回结构：总数 / 页码 / 每页条数 / 数据 */
@Data
public class PageResult<T> {
    private long total;
    private long page;
    private long size;
    private List<T> list;

    public static <T> PageResult<T> of(IPage<T> p) {
        PageResult<T> r = new PageResult<>();
        r.total = p.getTotal();
        r.page = p.getCurrent();
        r.size = p.getSize();
        r.list = p.getRecords();
        return r;
    }

    public static <T> PageResult<T> of(long total, long page, long size, List<T> list) {
        PageResult<T> r = new PageResult<>();
        r.total = total;
        r.page = page;
        r.size = size;
        r.list = list;
        return r;
    }
}
