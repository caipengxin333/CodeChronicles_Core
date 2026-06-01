package com.codechronicles.core.dto;

import java.util.List;

public record PageResponse<T>(long total, List<T> list) {
}
