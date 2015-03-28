package com.leekyoungil.cachemem.memcached;

import com.google.code.yanf4j.core.impl.StandardSocketOption;
import com.leekyoungil.cachemem.CacheMem;
import com.leekyoungil.cachemem.CacheMemInterface;
import com.leekyoungil.cachemem.model.CacheMemLog;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.command.BinaryCommandFactory;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.impl.ArrayMemcachedSessionLocator;
import net.rubyeye.xmemcached.transcoders.SerializingTranscoder;
import net.rubyeye.xmemcached.utils.AddrUtil;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeoutException;
import java.util.Map.Entry;

/**
 * Created by kyoungil_lee on 2014. 7. 24..
 */
public class MemcachedControl implements MemcachedInfo {
    private volatile static MemcachedControl instance = null;

    private ConcurrentHashMap<Integer, String> serverNoMap = new ConcurrentHashMap<Integer, String>();

    private CopyOnWriteArrayList<MemcachedClient> memcachedClient = new CopyOnWriteArrayList<MemcachedClient>();
    private MemcachedClient metaMemcachedClient = null;
    private CopyOnWriteArrayList<Integer> confuseServerNo = new CopyOnWriteArrayList<Integer>();

    public MemcachedControl () { }

    public static MemcachedControl getInstance () {
        if (instance == null) {
            synchronized (MemcachedControl.class) {
                if (instance == null) {
                    instance = new MemcachedControl();
                }
            }
        }

        return instance;
    }

    /**
     * Connect boolean.
     *
     * memcached 와 접속 한다.
     *
     * @return the boolean
     */
    @Override
    public synchronized boolean connect () {
        if (this.metaMemcachedClient == null) {
            metaConnect();
        }

        if (this.memcachedClient.size() > 0) {
            return true;
        }

        for (String address : serverAddress) {
            MemcachedClient tmpConnection = getMemcachedServerConnection(address);

            if (tmpConnection != null) {
                tmpConnection.setOpTimeout(5000L);
                tmpConnection.setMergeFactor(50);
                tmpConnection.setOptimizeMergeBuffer(false);
                tmpConnection.setEnableHeartBeat(false);
                tmpConnection.setOptimizeGet(false);

                this.memcachedClient.add(tmpConnection);

                this.serverNoMap.put(this.memcachedClient.size()-1, address);
            }
        }

        return (this.memcachedClient.size() > 0) ? true : false;
    }

    private synchronized boolean metaConnect () {
        if (this.metaMemcachedClient != null) {
            return true;
        }

        this.metaMemcachedClient = getMemcachedServerConnection(metaServerAddress);

        if (this.metaMemcachedClient != null) {
            this.metaMemcachedClient.setOpTimeout(5000L);
            this.metaMemcachedClient.setMergeFactor(50);
            this.metaMemcachedClient.setOptimizeMergeBuffer(false);
            this.metaMemcachedClient.setEnableHeartBeat(false);
            this.metaMemcachedClient.setOptimizeGet(false);
        }

        return (this.metaMemcachedClient != null) ? true : false;
    }

    /**
     * Gets memcached server connection.
     *
     * 서버댓수 만큼 접속 한다.
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

    @Override
    public boolean close() {
        return true;
    }

    @Override
    public MemcachedResult set (String key, Object value, int ttl, String siteName, String ipAddress, String itemName, String originKey) {
        MemcachedResult memcachedResult = new MemcachedResult();

        memcachedResult.setResult(true);

        if (!connectCheck(0)) {
            memcachedResult.setResultText(ERROR_001);
            memcachedResult.setResult(false);

            return memcachedResult;
        }

        boolean result = false;

        try {
            int addTTLTime = 30;

            try {
                addTTLTime = (CacheMemInterface.addTTLTime.containsKey(siteName) ? CacheMemInterface.addTTLTime.get(siteName) : 30);
            } catch (Exception e) {
                e.printStackTrace();
                addTTLTime = 30;
            }

            int serverNo = 0;

            // 처음은 무조건 localhost 로 set 시도
            result = this.memcachedClient.get(serverNo).set(key, ((ttl > 0) ? ttl+addTTLTime : this.timeout), value);

            CacheMemLog log = new CacheMemLog();

            if (result == false) {
                serverNo = getServerNo(-1, 0);

                this.confuseServerNo.clear();

                if (serverNo < 0) {
                    memcachedResult.setResult(false);
                    memcachedResult.setResultText(ERROR_006);

                    value = null;
                    ttl = serverNo;
                    result = false;
                } else {
                    result = this.memcachedClient.get(serverNo).set(key, ((ttl > 0) ? ttl+addTTLTime : this.timeout), value);
                }
            }

            if (result) {
                memcachedResult.setResultText(SUCCESS_001);
                memcachedResult.setResult(result);

                try {
                    if (this.metaMemcachedClient != null && this.metaMemcachedClient.getAvailableServers().size() > 0) {
                        this.metaMemcachedClient.set(key, ((ttl > 0) ? ttl+1+addTTLTime : this.timeout), serverNoMap.get(serverNo));
                    } else {
                        this.metaMemcachedClient = null;
                        metaConnect();
                    }
                } catch (Exception me) {
                    me.printStackTrace();
                }
            }

            log.setSiteName(siteName);
            log.setKey(key);
            log.setObjectData(value);
            log.setTtl(ttl);
            log.setTtlM(ttl+addTTLTime);
            log.setSetTime((int) (System.currentTimeMillis() / 1000));
            log.setIpAddress(ipAddress);
            log.setItemName(itemName);
            log.setOriginKey(originKey);

            CacheMem.logQueue.add(log);
        } catch (TimeoutException e) {
            e.printStackTrace();
            memcachedResult.setResult(false);
            memcachedResult.setResultText(ERROR_002);
        } catch (InterruptedException e) {
            e.printStackTrace();
            memcachedResult.setResult(false);
            memcachedResult.setResultText(ERROR_003);
        } catch (MemcachedException e) {
            e.printStackTrace();
            memcachedResult.setResult(false);
            memcachedResult.setResultText(ERROR_004);
        } catch (Exception e) {
            e.printStackTrace();
            memcachedResult.setResult(false);
            memcachedResult.setResultText(ERROR_005);
        }

        return memcachedResult;
    }

    @Override
    public MemcachedResult get (String key) {
        MemcachedResult memcachedResult = new MemcachedResult();

        memcachedResult.setResult(true);

        if (!connectCheck(0)) {
            memcachedResult.setResultText(ERROR_001);
            memcachedResult.setResult(false);

            return memcachedResult;
        }

        Object tmpResult = null;

        boolean checkMetaServer = false;

        // 일단 메타 서버 뒤진다.
        try {
            if (this.metaMemcachedClient != null && this.metaMemcachedClient.getAvailableServers().size() > 0) {
                Object serverMetaAddress = this.metaMemcachedClient.get(key);

                if (serverMetaAddress != null) {
                    checkMetaServer = true;
                    String flashDbAddress = (String) serverMetaAddress;

                    for(Entry<Integer, String> entry : this.serverNoMap.entrySet()) {
                        int keyNo = entry.getKey();
                        String value = entry.getValue();

                        if (value.equals(flashDbAddress)) {
                            tmpResult = this.memcachedClient.get(keyNo).get(key);
                            break;
                        }
                    }
                }
            } else {
                this.metaMemcachedClient = null;
                metaConnect();
            }
        } catch (TimeoutException e) {
            e.printStackTrace();
            memcachedResult.setResult(false);
            memcachedResult.setResultText(ERROR_002);
            tmpResult = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
            memcachedResult.setResult(false);
            memcachedResult.setResultText(ERROR_003);
            tmpResult = null;
        } catch (MemcachedException e) {
            e.printStackTrace();
            memcachedResult.setResult(false);
            memcachedResult.setResultText(ERROR_004);
            tmpResult = null;
        }

        if (tmpResult == null && !checkMetaServer) {
            // 무조건 localhost 부터 뒤진다
            for (MemcachedClient memcachedClientTmp : this.memcachedClient) {
                try {
                    if (tmpResult == null) {
                        tmpResult = memcachedClientTmp.get(key);

                        if (tmpResult != null) {
                            break;
                        }
                    }
                } catch (TimeoutException e) {
                    e.printStackTrace();
                    memcachedResult.setResult(false);
                    memcachedResult.setResultText(ERROR_002);
                    tmpResult = null;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    memcachedResult.setResult(false);
                    memcachedResult.setResultText(ERROR_003);
                    tmpResult = null;
                } catch (MemcachedException e) {
                    e.printStackTrace();
                    memcachedResult.setResult(false);
                    memcachedResult.setResultText(ERROR_004);
                    tmpResult = null;
                }
            }
        }

        memcachedResult.setResultText(SUCCESS_002);

        if (tmpResult != null) {
            memcachedResult.setResultObject(tmpResult);
            memcachedResult.setResultText(SUCCESS_001);
        }

        memcachedResult.setResult(true);


        return memcachedResult;
    }

    @Override
    public MemcachedResult purge (String key) {
        MemcachedResult memcachedResult = new MemcachedResult();

        memcachedResult.setResult(true);

        if (!connectCheck(0)) {
            memcachedResult.setResultText(ERROR_001);
            memcachedResult.setResult(false);

            return memcachedResult;
        }
        // 무조건 localhost 부터 뒤진다
        for (MemcachedClient memcachedClientTmp : this.memcachedClient) {
            try {
                memcachedClientTmp.delete(key);
                memcachedResult.setResultText(SUCCESS_001);
                memcachedResult.setResult(true);
            } catch (TimeoutException e) {
                e.printStackTrace();
                memcachedResult.setResult(false);
                memcachedResult.setResultText(ERROR_002);
            } catch (InterruptedException e) {
                e.printStackTrace();
                memcachedResult.setResult(false);
                memcachedResult.setResultText(ERROR_003);
            } catch (MemcachedException e) {
                e.printStackTrace();
                memcachedResult.setResult(false);
                memcachedResult.setResultText(ERROR_004);
            }
        }

        return memcachedResult;
    }

    /**
     * Connect check.
     *
     * memcached 접속 체크하며 접속에 문제가 있으면 1초 간격으로 3번 리트라잉을 시도한다.
     *
     * @param reTryCnt the re try cnt
     * @return the boolean
     */
    private boolean connectCheck (int reTryCnt) {
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
     * @return the server no
     */
    private int getServerNo (int inputNo, int oLoopCnt) {
        if ((this.confuseServerNo.size() == this.memcachedClient.size()) || oLoopCnt >= 10) {
            return -1;
        }

        // 접속 서버 번호 랜덤 생성
        Random random = new Random();

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
