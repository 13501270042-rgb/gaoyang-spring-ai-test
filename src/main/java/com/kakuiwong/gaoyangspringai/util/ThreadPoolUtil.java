package com.kakuiwong.gaoyangspringai.util;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;

import java.util.concurrent.*;

/**
 * @author: gaoyang
 * @Description:
 */
public final class ThreadPoolUtil {

    private ThreadPoolUtil() {
    }

    private static volatile ThreadPoolExecutor pool;
    private static BlockingQueue<Runnable> bqueue = new ArrayBlockingQueue<Runnable>(2000);
    private static final int SIZE_CORE_POOL = Runtime.getRuntime().availableProcessors();
    private static final int SIZE_MAX_POOL = 100;
    private static final long ALIVE_TIME = 2000;


    static class NameTreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "ThreadPoolUtil-" + IdWorker.getTimeId());
        }
    }

    public static ThreadPoolExecutor getPool() {
        if (pool == null) {
            synchronized (ThreadPoolUtil.class) {
                if (pool == null) {
                    pool = new ThreadPoolExecutor(SIZE_CORE_POOL, SIZE_MAX_POOL, ALIVE_TIME
                            , TimeUnit.MILLISECONDS, bqueue, new NameTreadFactory(), new ThreadPoolExecutor.CallerRunsPolicy());
                }
            }
        }
        return pool;
    }
}
