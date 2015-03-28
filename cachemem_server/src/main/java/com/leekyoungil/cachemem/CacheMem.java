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

public class CacheMem extends Verticle implements CacheMemInterface {
    public static LinkedBlockingQueue<CacheMemLog> logQueue = new LinkedBlockingQueue<>();

    @Override
    public void start() {
        // Set, Get thread 생성
        makeThreads();

        // Logging Thread 생성 1분에 한번씩 돌면서 큐를 비윤다
        new Thread(() -> {
            while (true) {
                if (CacheMem.logQueue.size() > 0) {
                    CacheMemLog cacheMemLog = null;

                    while ((cacheMemLog = CacheMem.logQueue.poll()) != null) {
                        CacheMemLogger.getInstance().insertLog(cacheMemLog);
                    }
                }

                try {
                    Thread.sleep(60000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        // 추가 TTL 타임 설정
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

        // Vert.x instance 생성
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
     */
    private void makeThreads () {
        Arrays.stream(serverSockets).parallel().forEach((portNum) ->
            // Data input output Socket Thread Get / Set 생성
            new Thread(() -> {
                /*
                if (nonblocking) {
                    ClientHandlerNio clientHandlerNio = new ClientHandlerNio(portNum);
                    clientHandlerNio.start();
                } else {
                */
                    try {
                        ServerSocket socketServer = new ServerSocket(portNum, threadSocketMaxConnectionQueue);
                        SynchronousQueue<Runnable> synchronousQueue = new SynchronousQueue<>();
                        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(basicPoolSize, maximumPoolSize, keepPoolAliveTime, TimeUnit.MILLISECONDS, synchronousQueue);

                           threadPool.setRejectedExecutionHandler((r, exc) -> {
                            try {
                                Thread.sleep(300);
                            } catch (InterruptedException ex) {
                                ex.printStackTrace();
                            }

                            exc.execute(r);
                        });

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
