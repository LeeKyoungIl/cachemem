package com.leekyoungil.cachemem.socket;

import com.leekyoungil.cachemem.CacheMem;
import com.leekyoungil.cachemem.memcached.MemcachedControl;
import com.leekyoungil.cachemem.memcached.MemcachedResult;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;

/**
 * Created by leekyoungil (leekyoungil@gmail.com) on 3/31/15.
 * github : https://github.com/LeeKyoungIl/cachemem
 */
public class ClientHandler implements Runnable {
    private Socket conn = null;

    public ClientHandler (Socket conn) {
        this.conn = conn;
        try {
            this.conn.setSendBufferSize(CacheMem.BUFF_SIZE);
            this.conn.setReceiveBufferSize(CacheMem.BUFF_SIZE);
            this.conn.setKeepAlive(true);
            this.conn.setTcpNoDelay(true);
            this.conn.setSoTimeout(5000);
            this.conn.setPerformancePreferences(2, 1, 0);
        } catch (SocketException e) {
            e.printStackTrace();

            try {
                if (this.conn != null) this.conn.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        workMemcachedHandling();
    }

    /**
     * Work memcached handling.
     *
     * client 와 memcached 가 통신하여 작업을 진행할수 있도록 Handling 을 해준다.
     *
     */
    private void workMemcachedHandling () {
        BufferedInputStream bis = null;
        DataInputStream in = null;
        DataOutputStream out = null;

        try {
            bis = new BufferedInputStream(this.conn.getInputStream());
            in = new DataInputStream(bis);
            out = new DataOutputStream(this.conn.getOutputStream());

            String data = null;

            try {
                data = in.readUTF();
            } catch (Exception ex) {
                ex.printStackTrace();
                data = null;
            }

            if (data != null && !data.isEmpty()) {
                String[] line = data.split("&");

                MemcachedResult resultData = null;
                String printResult = null;

                if (line.length > 2 && "BEGIN".equals(line[0])) {
                    switch (line[1]) {
                        case "GET" :
                            resultData = actionData("GET", line[3], null, 0, null, null, null, null);

                            if (resultData.isResult() && resultData.getResultObject() != null) {
                                byte[] resultByte = (byte[]) resultData.getResultObject();
                                out.writeUTF("SUCCESS&"+resultByte.length);
                                out.flush();
                                out.write(resultByte);
                            } else {
                                out.writeUTF("FAILED&" + ("empty".getBytes().length));
                            }
                            break;

                        case "SET" :
                            int size = Integer.parseInt(line[2]);
                            byte[] sendByte = new byte[size];
                            in.readFully(sendByte, 0, size);

                            int ttl = 0;

                            if (line.length > 4 && line[4] != null && !line[4].isEmpty()) {
                                ttl = Integer.parseInt(line[4]);
                            }

                            HashMap<Integer, String> param = new HashMap<Integer, String>();

                            for (int i=5; i<9; i++) {
                                if (line.length > i && line[i] != null && !line[i].isEmpty()) {
                                    param.put(i, line[i]);
                                } else {
                                    param.put(i, null);
                                }
                            }

                            resultData = actionData("SET", line[3], sendByte, ttl, param.get(5), param.get(6), param.get(7), param.get(8));

                            printResult = "SUCCESS";

                            if (!resultData.isResult()) {
                                printResult = "FAILED";
                            }

                            out.writeUTF(printResult+"&"+(printResult.getBytes().length));
                            out.flush();
                            out.write(printResult.getBytes());
                            break;

                        case "PURGE" :
                            resultData = actionData("PURGE", line[3], null, 0, null, null, null, null);

                            printResult = "SUCCESS";

                            if (!resultData.isResult()) {
                                printResult = "FAILED";
                            }

                            out.writeUTF(printResult+"&"+(printResult.getBytes().length));
                            out.flush();
                            out.write(printResult.getBytes());
                            break;

                        default:
                            out.writeUTF("ERROR");
                            break;
                    }

                    out.flush();
                }
            } else {
                out.writeUTF("ERROR_DATA_INPUT");
                out.flush();
            }
        } catch (IOException ioex) {
            ioex.printStackTrace();
        } finally {
            try {
                if (out != null) out.close();
                if (in != null) in.close();
                if (bis != null) bis.close();
                if (this.conn != null) this.conn.close();
            } catch (IOException ioex) {
                ioex.printStackTrace();
            }
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
    private MemcachedResult actionData (String action, String key, Object value, int ttl, String siteName, String ipAddress, String itemName, String originKey) {
        MemcachedResult returnValue = null;

        switch (action) {
            case "GET" :
                returnValue = MemcachedControl.getInstance().get(key);
                break;

            case "SET" :
                returnValue = MemcachedControl.getInstance().set(key, value, ttl, siteName, ipAddress, itemName, originKey);
                break;

            case "PURGE" :
                returnValue = MemcachedControl.getInstance().purge(key);
                break;
        }

        return returnValue;
    }
}
