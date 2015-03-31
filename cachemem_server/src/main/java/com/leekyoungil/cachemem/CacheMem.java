package com.leekyoungil.cachemem;

import com.leekyoungil.cachemem.memcached.MemcachedResult;
import com.leekyoungil.cachemem.model.CacheMemLog;
import com.leekyoungil.cachemem.socket.ClientHandler;
import com.leekyoungil.cachemem.socket.WebHandler;
import com.leekyoungil.cachemem.util.CacheMemLogger;
import com.leekyoungil.cachemem.util.CacheMemUtil;
import io.netty.handler.codec.http.HttpHeaders;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.platform.Verticle;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CacheMem extends Verticle implements CacheMemInterface {

    /**
     * The constant LOG_QUEUE_MAP.
     * 로그를 저장하는 queue hashmap 를 생성한다.
     * (Create the log queue hashmap.)
     */
    public static AtomicInteger QUEUE_NO = new AtomicInteger(1);
    public static ConcurrentHashMap<Integer, LinkedBlockingQueue<CacheMemLog>> LOG_QUEUE_MAP = new ConcurrentHashMap<>();

    /**
     * Sets queue no.
     */
    public static void setQueueNo () {
        if (QUEUE_NO.get() == 1) {
            QUEUE_NO.set(2);
        } else {
            QUEUE_NO.set(1);
        }
    }

    @Override
    public void start() {
        /**
         * HashMap initialization.
         */
        LOG_QUEUE_MAP.put(1, new LinkedBlockingQueue<>());
        LOG_QUEUE_MAP.put(2, new LinkedBlockingQueue<>());

        // Create a Set and Get thread.
        makeThreads();

        /**
         * Create a Logging Thread.
         * 1분에 한번씩 돌면서 큐를 비윤고 로그를 저장한다.
         * Start a pop the queue at the once per minutes, and save the log.
         */
        new Thread(() -> {
            Integer popQueueNo = QUEUE_NO.get();
            setQueueNo();

            LOG_QUEUE_MAP.get(popQueueNo).stream().parallel().forEach(cacheMemLog -> CacheMemLogger.getInstance().insertLog(cacheMemLog));
            LOG_QUEUE_MAP.get(popQueueNo).clear();

            try {
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        /**
         * Create a add to set the TTL value Thread.
         * 5분에 한번씩 돌면서 추가 TTL 타입을 설정한다.
         * Start a thread at the 5 per minutes, and the TTL time.
         */
        new Thread(() -> {
            while (true) {
                CacheMemUtil.setAddTTLTime();

                try {
                    Thread.sleep(300000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        /**
         * Create a Vert.x instance
         */
        vertx.createHttpServer().setUsePooledBuffers(true).setReceiveBufferSize(BUFF_SIZE).setSendBufferSize(BUFF_SIZE).setTCPKeepAlive(false).setTCPNoDelay(true).setAcceptBacklog(10000).requestHandler((req) -> {
            MemcachedResult result = WebHandler.getInstance().actionData(req);

            String headerContentType = objectHtmlHeader;
            String resultMessage = null;

            int headerContentLength = 0;
            byte[] resultByte = null;

            if (result.isResult() && "success".equals(result.getResultText())) {
                try {
                    resultByte = (byte[]) result.getResultObject();

                    if (resultByte == null || resultByte.length == 0) {
                        resultMessage = ("/PURGE".equals(req.path().toUpperCase())) ? result.getResultText() : errorMsg002;
                        headerContentLength = resultMessage.length();
                    } else {
                        headerContentType = objectByteHeader;
                        headerContentLength = resultByte.length;
                        req.response().putHeader("Accept-Ranges", "bytes");
                    }
                } catch (Exception ex) {
                    resultMessage = errorMsg003;
                    headerContentLength = resultMessage.length();

                    ex.printStackTrace();
                }
            } else {
                resultMessage = errorMsg001;
                headerContentLength = resultMessage.length();
            }

            req.response().putHeader(HttpHeaders.Names.CONTENT_TYPE, headerContentType);
            req.response().putHeader(HttpHeaders.Names.CONTENT_LENGTH, String.valueOf(headerContentLength));

            if (objectByteHeader.equals(headerContentType) && resultByte != null) {
                req.response().write(new Buffer(resultByte));
            } else {
                req.response().end(resultMessage);
            }

            req.response().close();
        }).listen(vertxSocketPort);
    }

    /**
     * Make threads.
     * Set, Get 용 thread 를 만든다.
     * (Create a Set and Get thread.)
     */
    private void makeThreads () {
        /**
         * 이미 설정되어있는 서버의 소켓수대로 루프를 돈다.
         * (Execute the number of the socket that is already set.)
         *
         */
        Arrays.stream(serverSockets).parallel().forEach((portNum) ->
            // Data input output Socket Thread Get / Set 생성
            new Thread(() -> {
                /*
                 * sorry, non block io is not yet.
                 *
                if (nonblocking) {
                    ClientHandlerNio clientHandlerNio = new ClientHandlerNio(portNum);
                    clientHandlerNio.start();
                } else {
                */
                try {
                    /**
                     * 쓰레드 풀을 만든다.
                     * Create a thread pool.
                     */
                    ThreadPoolExecutor threadPool = new ThreadPoolExecutor(basicPoolSize, maximumPoolSize, keepPoolAliveTime, TimeUnit.MILLISECONDS, new SynchronousQueue<>());
                    threadPool.setRejectedExecutionHandler((r, exc) -> {
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }

                        exc.execute(r);
                    });

                    /**
                     * 소켓을 생성한다. (backLog 를 같이 설정해준다.)
                     * Create a server socket. (initialization with backLog.)
                     */
                    ServerSocket socketServer = new ServerSocket(portNum, threadSocketMaxConnectionQueue);

                    while (true) {
                        Socket socket = socketServer.accept();

                        if (socket != null) {
                            threadPool.execute(new ClientHandler(socket));
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //}
            }).start()
        );
    }
}
