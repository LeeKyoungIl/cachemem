package com.leekyoungil.cachemem.memcached;

import com.google.code.yanf4j.core.impl.StandardSocketOption;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.command.BinaryCommandFactory;
import net.rubyeye.xmemcached.impl.ArrayMemcachedSessionLocator;
import net.rubyeye.xmemcached.transcoders.SerializingTranscoder;
import net.rubyeye.xmemcached.utils.AddrUtil;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by leekyoungil (leekyoungil@gmail.com) on 3/31/15.
 * github : https://github.com/LeeKyoungIl/cachemem
 */
public abstract class MemcachedInfo implements MemcachedInterface {

    /**
     * memcached 서버의 주소 정보를 저장한다.
     * save the server information. (memcached server ip address.)
     */
    protected ConcurrentHashMap<Integer, String> serverNoMap = new ConcurrentHashMap<>();
    /**
     * memcached 서버 접속 오브젝트를 저장한다.
     * save the memcached server connect object. (included meta server.)
     */
    protected CopyOnWriteArrayList<MemcachedClient> memcachedClient = new CopyOnWriteArrayList<>();
    protected MemcachedClient metaMemcachedClient = null;
    /**
     * memcached 서버중에 접속이 불가능한 서버 번호를 저장한다.
     * save the server number can't connected in the memcached servers.
     */
    protected CopyOnWriteArrayList<Integer> confuseServerNo = new CopyOnWriteArrayList<>();

    /**
     * Connect boolean.
     *
     * 정의한 서버의 수 만큼 memcached 서버 접속 object를 만든다.
     * defined as the number of servers to create the memcached server connection object.
     *
     * @return the boolean
     */
    public synchronized boolean connect () {
        /**
         * 메타서버 접속 객체를 만든다.
         * Create a meta server connect object.
         */
        if (this.metaMemcachedClient == null) {
            metaConnect();
        }

        if (this.memcachedClient.size() > 0) {
            return true;
        }

        Arrays.asList(serverAddress).stream().parallel().forEach(address -> {
            /**
             * memcached 서버에 접속시도를 한다.
             * try to connect to memcached server.
             */
            MemcachedClient tmpConnection = getMemcachedServerConnection(address);

            if (tmpConnection != null) {
                tmpConnection = setMemcachedClientOptions(tmpConnection);
                this.memcachedClient.add(tmpConnection);
                this.serverNoMap.put(this.memcachedClient.size()-1, address);
            }
        });

        return (this.memcachedClient.size() > 0) ? true : false;
    }

    /**
     * Meta connect.
     *
     * 메타서버 접속 object를 만든다.
     *
     * @return the boolean
     */
    public synchronized boolean metaConnect () {
        if (this.metaMemcachedClient != null) {
            return true;
        }

        /**
         * 메타서버에 접속시도를 한다.
         * try to connect to meta server.
         */
        this.metaMemcachedClient = getMemcachedServerConnection(metaServerAddress);

        if (this.metaMemcachedClient != null) {
            this.metaMemcachedClient = setMemcachedClientOptions(this.metaMemcachedClient);
        }

        return (this.metaMemcachedClient != null) ? true : false;
    }


    /**
     * Sets memcached client options.
     *
     * memcached 클라이언트 접속 옵션을 설정해 준다.
     * set the memcached client connection options.
     *
     * @param tmpConnection the tmp connection
     * @return the memcached client options
     */
    private MemcachedClient setMemcachedClientOptions (MemcachedClient tmpConnection) {
        tmpConnection.setOpTimeout(5000L);
        tmpConnection.setMergeFactor(50);
        tmpConnection.setOptimizeMergeBuffer(false);
        tmpConnection.setEnableHeartBeat(false);
        tmpConnection.setOptimizeGet(false);

        return tmpConnection;
    }

    /**
     * Gets memcached server connection.
     *
     * memcached 서버 접속 옵션으로 자신의 서버 상황에 맞추어 옵션을 변경 해줘야 한다.
     * this method is a memcached server connection options.
     * you should try to be changing the options on your server situation.
     *
     * @param serverAddress the server address
     * @return the memcached server connection
     */
    @Override
    public MemcachedClient getMemcachedServerConnection (String serverAddress) {
        if (serverAddress.isEmpty()) {
            return null;
        }

        try {
            MemcachedClientBuilder builder = new XMemcachedClientBuilder(AddrUtil.getAddresses(serverAddress));
            builder.setConnectionPoolSize(15);
            builder.setConnectTimeout(5000);
            builder.setCommandFactory(new BinaryCommandFactory());
            builder.setSessionLocator(new ArrayMemcachedSessionLocator(net.rubyeye.xmemcached.HashAlgorithm.ONE_AT_A_TIME));
            builder.getConfiguration().setSoTimeout(5000);
            builder.getConfiguration().setWriteThreadCount(2);
            builder.getConfiguration().setReadThreadCount(2);
            builder.getConfiguration().setSessionIdleTimeout(10000);
            builder.getConfiguration().setStatisticsServer(false);
            builder.setSocketOption(StandardSocketOption.SO_RCVBUF, 10 * 1024 * 1024);
            builder.setSocketOption(StandardSocketOption.SO_SNDBUF, 10 * 1024 * 1024);
            builder.setSocketOption(StandardSocketOption.TCP_NODELAY, false);
            builder.getConfiguration().setSessionReadBufferSize(10 * 1024 * 1024);
            builder.setTranscoder(new SerializingTranscoder(10 * 1024 * 1024));
            builder.getConfiguration().setStatisticsServer(false);

            return (builder.build() != null) ? builder.build() : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Connect check.
     *
     * memcached 접속 상태를 체크하며 접속에 문제가 있으면 1초 간격으로 3번 다시 시도한다.
     * check the memcached connection status. if it have problem, at one second intervals three times to retry.
     *
     * @param reTryCnt the re try cnt
     * @return the boolean
     */
    protected boolean connectCheck (int reTryCnt) {
        if (this.memcachedClient.size() > 0) {
            boolean connectionDestroy = false;

            for (MemcachedClient memcachedClientTmp : this.memcachedClient) {
                if (memcachedClientTmp.getAvailableServers().size() == 0) {
                    connectionDestroy = true;
                }
            }

            if (connectionDestroy) {
                this.memcachedClient.parallelStream().forEach((memcachedClient) -> {
                    try {
                        memcachedClient.shutdown();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                this.memcachedClient.clear();
            }

            return (!connectionDestroy) ? true : connectCheck(reTryCnt);
        } else if (this.memcachedClient.size() == 0 && reTryCnt < 3) {
            if (!connect()) {
                reTryCnt++;

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return connectCheck(reTryCnt);
            }
        } else  {
            return false;
        }

        return true;
    }

    /**
     * Gets server no.
     *
     * @param inputNo the input no
     * @param oLoopCnt the o loop cnt
     * @return the server no
     */
    protected int getServerNo (int inputNo, int oLoopCnt) {
        if ((this.confuseServerNo.size() == this.memcachedClient.size()) || oLoopCnt >= 10) {
            return -1;
        }

        /**
         * 접속 서버 번호 랜덤 생성
         * create server number by random.
         */
        Random random = new Random(System.nanoTime());

        int serverCount = this.memcachedClient.size();
        int serverNo = (serverCount == 1) ? 0 : random.nextInt((serverCount));

        if (inputNo >= 0 && serverCount > 1) {
            int loopCnt = 0;

            while (serverNo == inputNo || loopCnt < 30) {
                serverNo = random.nextInt((serverCount));
                loopCnt++;
            }
        } else if (inputNo >= 0 && serverCount <= 1) {
            serverNo = -1;
        }

        boolean serverStatusCheck = true;

        if (serverNo >= 0 && this.memcachedClient.get(serverNo).getAvailableServers().size() == 0) {
            if (!this.confuseServerNo.contains(serverNo)) {
                this.confuseServerNo.add(serverNo);
            }

            serverStatusCheck = false;
            oLoopCnt++;
        }

        return (serverStatusCheck) ? serverNo : getServerNo(serverNo, oLoopCnt);
    }
}
