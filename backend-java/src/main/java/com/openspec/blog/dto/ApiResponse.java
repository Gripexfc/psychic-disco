package com.openspec.blog.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private T data;
    private ApiMeta meta;
    private ApiError error;

    private ApiResponse(T data, ApiMeta meta, ApiError error) {
        this.data = data;
        this.meta = meta;
        this.error = error;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(data, null, null);
    }

    public static <T> ApiResponse<T> ok(T data, ApiMeta meta) {
        return new ApiResponse<>(data, meta, null);
    }

    public static <T> ApiResponse<T> fail(String code, String message) {
        return new ApiResponse<>(null, null, new ApiError(code, message, null));
    }

    public static <T> ApiResponse<T> fail(String code, String message, Object details) {
        return new ApiResponse<>(null, null, new ApiError(code, message, details));
    }

    public T getData() { return data; }
    public ApiMeta getMeta() { return meta; }
    public ApiError getError() { return error; }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ApiMeta {
        private Pagination pagination;

        public ApiMeta() {}
        public ApiMeta(Pagination pagination) { this.pagination = pagination; }

        public Pagination getPagination() { return pagination; }
        public void setPagination(Pagination pagination) { this.pagination = pagination; }
    }

    public static class Pagination {
        private Integer page;
        private Integer pageSize;
        private Long total;
        private Integer totalPages;

        public Pagination() {}
        public Pagination(Integer page, Integer pageSize, Long total) {
            this.page = page;
            this.pageSize = pageSize;
            this.total = total;
            this.totalPages = Math.max(1, (int) Math.ceil((double) total / pageSize));
        }

        public Integer getPage() { return page; }
        public Integer getPageSize() { return pageSize; }
        public Long getTotal() { return total; }
        public Integer getTotalPages() { return totalPages; }
    }

    public static class ApiError {
        private String code;
        private String message;
        private Object details;

        public ApiError() {}
        public ApiError(String code, String message, Object details) {
            this.code = code;
            this.message = message;
            this.details = details;
        }

        public String getCode() { return code; }
        public String getMessage() { return message; }
        public Object getDetails() { return details; }
    }
}
