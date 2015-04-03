package com.leekyoungil.cachemem.client.socket;

import com.leekyoungil.cachemem.client.define.CacheMemDefine;
import com.leekyoungil.cachemem.client.socket.model.CacheMemResult;

import java.io.*;
import java.net.*;
import java.util.Random;

/**
 * Created by Kyoungil_Lee on 4/1/15.
 */
public class CacheMemSocketClient {

    /**
     * Get cache mem result.
     *
     * @param serverNo the server no
     * @param key the key
     * @param reConnect the re connect
     * @return the cache mem result
     */
    public static CacheMemResult get (int serverNo, String key, int reConnect) {
        CacheMemResult cacheMemResult = new CacheMemResult();
        cacheMemResult.setResult(false);
        cacheMemResult.setErrorText("none");
        cacheMemResult.setConnectServerNo(serverNo);

        Socket socket = null;

        DataOutputStream write = null;
        DataInputStream read = null;

        boolean reConnectCheck = false;

        try {
            Random rand = new Random(System.nanoTime());

            /*
             * 소켓에 접속할 객체를 생성한다.
             */
            SocketAddress socketAddress = new InetSocketAddress(CacheMemDefine.SERVER_ADDRESS[cacheMemResult.getConnectServerNo()], CacheMemDefine.SERVER_PORT_GET[Math.abs(rand.nextInt(CacheMemDefine.SERVER_PORT_GET.length))]);

            socket = new Socket();
            socket.connect(socketAddress, 5000);

            write = new DataOutputStream(socket.getOutputStream());

            StringBuilder writeData = new StringBuilder();
            writeData.append("BEGIN&GET&0&");
            writeData.append(key);

            // Socket 데이타 send 시도 GET 메서드 키값과 같이 전달한다.
            write.writeUTF(writeData.toString());
            write.flush();

            // Object 데이타를 읽어오는 소켓 스트림 생성
            read = new DataInputStream(socket.getInputStream());

            byte[] readDataResult = new byte[1024];

            read.read(readDataResult);

            String readValue = new String(readDataResult).trim();

            if (readValue != null && !readValue.isEmpty() && readValue.contains("SUCCESS")) {
                String byteSize[] = readValue.split("&");

                // 결과값이 있으면 데이타를 읽어와서 오브젝트로 변환후 저장
                if ("SUCCESS".equals(byteSize[0])) {
                    // Socket 데이타 send 시도 GET 메서드 키값과 같이 전달한다.
                    write.write("READY".getBytes());
                    write.flush();

                    read = null;
                    read = new DataInputStream(socket.getInputStream());

                    byte[] responseByte = new byte[Integer.parseInt(byteSize[1])];
                    read.readFully(responseByte);

                    cacheMemResult.setResult(true);
                    cacheMemResult.setResultData(toObject(responseByte));
                } else {
                    cacheMemResult.setResultData(new String("empty"));
                }
            }
        } catch (IOException cex) {
            cex.printStackTrace();
            cacheMemResult.setResult(false);
            cacheMemResult.setErrorText(cex.toString());

            reConnectCheck = true;
        } finally {
            if (reConnectCheck && CacheMemDefine.SERVER_ADDRESS.length > 1 && reConnect < 1) {
                System.out.println("reConnectCheck GET : " + reConnect);
                return get(((serverNo == 0) ? 1 : 0), key, (++reConnect));
            }

            if (write != null) {
                try {
                    write.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    cacheMemResult.setErrorText("DataOutputStream close io exception");
                }
            }

            if (read != null) {
                try {
                    read.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    cacheMemResult.setErrorText("DataInputStream close io exception");
                }
            }

            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    cacheMemResult.setErrorText("Socket close io exception");
                }
            }
        }

        return cacheMemResult;
    }

    /**
     * Set cache mem result.
     *
     * @param serverNo the server no
     * @param siteName the site name
     * @param originKey the origin key
     * @param itemName the item name
     * @param key the key
     * @param data the data
     * @param ttl the ttl
     * @param ipAddress the ip address
     * @param reConnect the re connect
     * @return the cache mem result
     */
    public static CacheMemResult set (int serverNo, String siteName, String originKey, String itemName, String key, byte[] data, int ttl, String ipAddress, int reConnect) {
        CacheMemResult cacheMemResult = new CacheMemResult();
        cacheMemResult.setResult(false);
        cacheMemResult.setErrorText("none");
        cacheMemResult.setConnectServerNo(serverNo);

        Socket socket = null;

        DataOutputStream write = null;
        DataInputStream read = null;

        boolean reConnectCheck = false;

        try {
            Random rand = new Random(System.currentTimeMillis());
            // 서버 소켓에 접속 한다.
            SocketAddress socketAddress = new InetSocketAddress(CacheMemDefine.SERVER_ADDRESS[cacheMemResult.getConnectServerNo()], CacheMemDefine.SERVER_PORT_SET[Math.abs(rand.nextInt(CacheMemDefine.SERVER_PORT_SET.length))]);

            socket = new Socket();
            socket.connect(socketAddress, 5000);

            write = new DataOutputStream(socket.getOutputStream());

            int sendByteSize = data.length;

            // Socket 데이타 send 시도 SET 메서드 키값과 send byte size 와 ttl 값 사이트명, 클라이언트 아이피 주소, 아이템명, 오리진키를 같이 전달한다.
            write.writeUTF("BEGIN&SET&"+sendByteSize+"&"+ key + "&" + ttl + "&" + siteName + "&" + ipAddress + "&" + itemName + "&" + originKey);
            write.flush();

            // data byte array send
            write.write(data);
            write.flush();

            read = new DataInputStream(socket.getInputStream());

            String readValue = read.readUTF();

            if (readValue != null || !readValue.isEmpty()) {
                String byteSize[] = readValue.split("&");

                byte[] responseByte = new byte[Integer.parseInt(byteSize[1])];
                read.readFully(responseByte);

                if ("SUCCESS".equals(byteSize[0])) {
                    cacheMemResult.setResult(true);
                }
            }
        } catch (IOException cex) {
            cex.printStackTrace();
            cacheMemResult.setResult(false);
            cacheMemResult.setErrorText(cex.toString());

            reConnectCheck = true;
        } finally {
            if (reConnectCheck && CacheMemDefine.SERVER_ADDRESS.length > 1 && reConnect < 1) {
                System.out.println("reConnectCheck : " + reConnect);
                return set(((serverNo == 0) ? 1 : 0), siteName, originKey, itemName, key, data, ttl, ipAddress, (++reConnect));
            }

            if (write != null) {
                try {
                    write.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    cacheMemResult.setErrorText("DataOutputStream close io exception");
                }
            }

            if (read != null) {
                try {
                    read.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    cacheMemResult.setErrorText("DataInputStream close io exception");
                }
            }

            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    cacheMemResult.setErrorText("Socket close io exception");
                }
            }
        }

        return cacheMemResult;
    }

    /**
     * To object.
     *
     * byte array 를 Object 로 변환한다.
     * convert to byte array to Object.
     *
     * @param bytes the bytes
     * @return the object
     */
    public static Object toObject(byte[] bytes) {
        Object obj = null;

        ByteArrayInputStream bis = null;
        ObjectInputStream ois = null;

        try {
            bis = new ByteArrayInputStream (bytes);
            ois = new ObjectInputStream (bis);
            obj = ois.readObject();
        } catch (IOException|ClassNotFoundException ex) {
            ex.printStackTrace();

            if (ex.toString().toLowerCase().indexOf("ioexception") > -1) {
                obj = new String(bytes);
            } else {
                obj = null;
            }
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return obj;
    }

    /**
     * To byte array.
     *
     * Object 를 byte array 로 변환한다. 단 해당 object 는 반드시 Serializable 을 구현 해야한다.
     * convert object to byte array. and that object must implements Serializable.
     *
     * @param obj the obj
     * @return the byte [ ]
     */
    public static byte[] toByteArray(Object obj) {
        byte[] bytes = null;

        ByteArrayOutputStream bos = null;
        ObjectOutputStream oos = null;

        try {
            bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.flush();
            bytes = bos.toByteArray ();
        }
        catch (IOException ex) {
            ex.printStackTrace();
            bytes = null;
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return bytes;
    }
}
