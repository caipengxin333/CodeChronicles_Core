package com.codechronicles.core.dto;

import java.util.List;

/**
 * 简单分页响应，total 是符合查询条件的总数，list 是当前页数据。
 */
public record PageResponse<T>(long total, List<T> list) {
}
