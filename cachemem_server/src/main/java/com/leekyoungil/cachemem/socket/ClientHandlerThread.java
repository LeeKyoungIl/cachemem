package com.leekyoungil.cachemem.socket;

import com.leekyoungil.cachemem.memcached.MemcachedControl;
import com.leekyoungil.cachemem.memcached.MemcachedResult;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by leekyoungil (leekyoungil@gmail.com) on 3/31/15.
 * github : https://github.com/LeeKyoungIl/cachemem
 */
public class ClientHandlerThread extends Thread {
    private final Charset cs = Charset.forName("UTF-8");
    private final int socketOps = SelectionKey.OP_CONNECT | SelectionKey.OP_WRITE  | SelectionKey.OP_READ;

    private SocketChannel conn = null;
    private Selector selector = null;
    private int readByteSize = 1024, writeByteSize = 1024;

    public ClientHandlerThread(SocketChannel conn) {
        this.conn = conn;

        try {
            this.conn.configureBlocking(false);
            this.conn.socket().setKeepAlive(false);
            this.conn.socket().setTcpNoDelay(true);
            this.conn.socket().setSoTimeout(10000);
            this.conn.socket().setPerformancePreferences(0, 2, 1);
        } catch (IOException e) {
            e.printStackTrace();

            if (this.conn != null) {
                try {
                    this.conn.socket().close();
                    this.conn.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    @Override
    public void start() {
        workMemcachedHandling();
    }

    /**
     * Work memcached handling.
     *
     * client 와 memcached 가 통신하여 작업을 진행할수 있도록 Handling 을 해준다.
     *
     */
    private void workMemcachedHandling () {
        boolean done = false, metadata = false;
        String line[] = null;
        byte[] resultByte = null;
        HashMap<Integer, Object> param = null;
        MemcachedResult resultData = null;

        try {
            while (!Thread.interrupted() && !done) {
                this.selector = Selector.open();
                this.conn.register(this.selector, socketOps);
                System.out.println("this.selector.isOpen() : " + this.selector.isOpen());


                this.selector.select(6000);

                System.out.println("@@@@@@@@@@@@@@@ Thread Start @@@@@@@@@@@@@@@");
                Iterator<SelectionKey> iter = this.selector.selectedKeys().iterator();

                while (iter.hasNext() && !done) {
                    SelectionKey key = iter.next();
                    iter.remove();

                    if (key.isConnectable()) {
                        key.cancel();
                        done = true;
                        break;
                    } else {
                        this.conn.write(ByteBuffer.wrap(new String("CONNECTED").getBytes()));

                        while ((key.isReadable() || key.isWritable()) && !done) {
                            ByteBuffer readBuffer = ByteBuffer.allocate(this.readByteSize);
                            readBuffer.clear();

                            ByteBuffer writeBuffer = ByteBuffer.allocate(this.writeByteSize);
                            writeBuffer.clear();

                            int len = this.conn.read(readBuffer);

                            if (len < 0) {
                                key.cancel();
                                done = true;
                                break;
                            } else if (len >  0) {
                                readBuffer.flip();

                                if (!metadata) {
                                    String keyData = cs.decode(readBuffer).toString();

                                    System.out.println("keyData : " + keyData);

                                    line = (keyData != null && !keyData.isEmpty() && keyData.length() > 0) ? keyData.split("&") : null;

                                    for (String tmp : line) {
                                        System.out.println("line : " + tmp);
                                    }
                                }

                                if (line != null && line.length > 2 && line[0] != null && line[1] != null && line[0].indexOf("BEGIN") == 0) {
                                    if (param == null) {
                                        param = new HashMap<>();

                                        for (int i=3; i<line.length; i++) {
                                            if (line.length > i && line[i] != null && !line[i].isEmpty()) {
                                                if (i != 4) {
                                                    param.put(i, line[i]);
                                                } else {
                                                    param.put(i, line[i]);
                                                }
                                            } else {
                                                if (i == 4) {
                                                    param.put(i, 30);
                                                } else {
                                                    param.put(i, null);
                                                }
                                            }

                                            System.out.println("param : " + i + " / " + param.get(i));
                                        }
                                    }

                                    System.out.println("Flashdb Action : " + line[1]);

                                    switch (line[1]) {
                                        case "GET":
                                            String tmp = cs.decode(readBuffer).toString().trim();
                                            boolean endChecker = false;

                                            System.out.println("get receive tmp : " + tmp);

                                            if (!metadata) {
                                                metadata = true;

                                                System.out.println("key : " + line[3]);

                                                resultData = actionData("GET", line[3], null, 0, null, null, null, null);

                                                System.out.println("resultData.isResult() : " + resultData.isResult());


                                                if (resultData.isResult() && resultData.getResultObject() != null) {
                                                    resultByte = (byte[]) resultData.getResultObject();
                                                    this.readByteSize = 10;

                                                    writeBuffer.put(new String("SUCCESS&"+resultByte.length).getBytes());
                                                    writeBuffer.position(0);

                                                    this.conn.write(writeBuffer);
                                                    writeBuffer.flip();
                                                    System.out.println("resultData.getResultObject() : " + resultData.getResultObject());

                                                } else {
                                                    writeBuffer.put(new String("FAILED&" + ("empty".getBytes().length)).getBytes());
                                                    writeBuffer.position(0);

                                                    this.conn.write(writeBuffer);
                                                    writeBuffer.flip();
                                                    endChecker = true;
                                                }
                                            } else if (resultByte != null && resultByte.length > 0 && "GETDATA".equals(tmp)) {
                                                writeBuffer.put(resultByte);
                                                writeBuffer.position(0);

                                                this.conn.write(writeBuffer);
                                                writeBuffer.flip();
                                                endChecker = true;
                                                System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@ get send data ok @@@@@@@@@@@@@@@@@@@@@@@@@@@@");
                                            } else {
                                                writeBuffer.put(new String("FAILED&" + ("empty".getBytes().length)).getBytes());
                                                writeBuffer.position(0);

                                                this.conn.write(writeBuffer);
                                                writeBuffer.flip();
                                                endChecker = true;
                                            }

                                            if (endChecker) {
                                                System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@ get ok @@@@@@@@@@@@@@@@@@@@@@@@@@@@");
                                                done = true;
                                            }

                                            break;

                                        case "SET":
                                            System.out.println("resultData : " + resultData);
                                            System.out.println("readBuffer : " + readBuffer);
                                            if (!metadata) {
                                                metadata = true;

                                                this.readByteSize = Integer.parseInt(line[2]);

                                                writeBuffer.put(new String("SUCCESS").getBytes());
                                                writeBuffer.position(0);

                                                this.conn.write(writeBuffer);
                                                writeBuffer.flip();
                                                System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@ set 0 @@@@@@@@@@@@@@@@@@@@@@@@@@@@");
                                            } else if (resultData == null && readBuffer != null && readBuffer.hasRemaining()) {
                                                System.out.println("this.sendByteSize ; " + this.readByteSize);
                                                System.out.println("buffer.position() ; " + readBuffer.position());
                                                System.out.println("buffer.capacity() ; " + readBuffer.capacity());
                                                System.out.println("buffer.limit() ; " + readBuffer.limit());
                                                System.out.println("buffer.remaining() ; " + readBuffer.remaining());
                                                System.out.println("buffer.hasRemaining() ; " + readBuffer.hasRemaining());

                                                byte[] bytes = new byte[this.readByteSize];
                                                readBuffer.get(bytes, 0, bytes.length);
                                                System.out.println("bytes size ; " + bytes.length);

                                                resultData = actionData("SET", (String) param.get(3), bytes, Integer.valueOf((String) param.get(4)), (String) param.get(5), (String) param.get(6), (String) param.get(7), (String) param.get(8));
                                                System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@ set ok @@@@@@@@@@@@@@@@@@@@@@@@@@@@");

                                                writeBuffer.put(((!resultData.isResult()) ? "FAILED" : "SUCCESS").getBytes());
                                                writeBuffer.position(0);

                                                this.conn.write(writeBuffer);
                                                writeBuffer.flip();

                                                done = true;
                                            }

                                            break;

                                        case "PURGE":
                                            resultData = actionData("PURGE", line[3], null, 0, null, null, null, null);

                                            writeBuffer.put(((!resultData.isResult()) ? "FAILED" : "SUCCESS").getBytes());
                                            writeBuffer.position(0);

                                            this.conn.write(writeBuffer);
                                            writeBuffer.flip();

                                            done = true;
                                            break;

                                        default:
                                            writeBuffer.put("ERROR".getBytes());
                                            writeBuffer.position(0);

                                            this.conn.write(writeBuffer);
                                            writeBuffer.flip();
                                            done = true;
                                            break;
                                    }
                                }

                                writeBuffer.compact();
                                readBuffer.compact();
                            }
                        }
                    }

                    done = true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();

            if (this.conn != null) {
                try {
                    this.conn.socket().close();
                    this.conn.close();
                    this.conn = null;
                    System.out.println("Exception Client :: server closed");
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        } finally {
            /*
            if (this.conn != null) {
                try {
                    this.conn.socket().close();
                    this.conn.close();
                    this.conn = null;
                    System.out.println("Client :: server closed");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            */
        }
    }

    /**
     * Action data.
     *
     * memcached 와 통신 하는 부분
     *
     * @param action the action
     * @param key the key
     * @param value the value
     * @param ttl the ttl
     * @return the memcached result
     */
    private MemcachedResult actionData (String action, String key, Object value, Integer ttl, String siteName, String ipAddress, String itemName, String originKey) {
        MemcachedResult returnValue = null;

        switch (action) {
            case "GET" :
                returnValue = MemcachedControl.getInstance().get(key);
                break;

            case "SET" :
                returnValue = MemcachedControl.getInstance().set(key, value, Integer.parseInt(String.valueOf(ttl)), siteName, ipAddress, itemName, originKey);
                break;

            case "PURGE" :
                returnValue = MemcachedControl.getInstance().purge(key);
                break;
        }

        return returnValue;
    }
}
