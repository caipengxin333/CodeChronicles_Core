package com.codechronicles.core.util;

/**
 * ThreadLocal 工具类，用于在一次请求内保存当前登录用户上下文。
 */
public class ThreadLocalUtil {

    private static final ThreadLocal<Object> THREAD_LOCAL = new ThreadLocal<>();

    private ThreadLocalUtil() {
    }

    /**
     * 获取当前线程保存的数据。
     */
    @SuppressWarnings("unchecked")
    public static <T> T get() {
        return (T) THREAD_LOCAL.get();
    }

    /**
     * 保存当前线程数据。
     */
    public static void set(Object value) {
        THREAD_LOCAL.set(value);
    }

    /**
     * 清除当前线程数据，避免线程复用时发生内存泄漏或用户串号。
     */
    public static void remove() {
        THREAD_LOCAL.remove();
    }
}
