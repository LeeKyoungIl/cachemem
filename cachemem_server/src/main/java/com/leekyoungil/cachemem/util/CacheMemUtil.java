package com.leekyoungil.cachemem.util;

import com.leekyoungil.cachemem.CacheMemInterface;

import java.io.*;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Scanner;

/**
 * Created by kyoungil_lee on 2014. 8. 5..
 */
public class CacheMemUtil {

    /**
     * To byte array.
     *
     * Object 를 byte array 로 변환한다. 단 해당 object 는 반드시 Serializable 을 구현 해야한다.
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

    /**
     * To object.
     *
     * byte array 를 Object 로 변환한다.
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
        } catch (IOException ex) {
            ex.printStackTrace();
            obj = new String(bytes);
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
            obj = null;
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
     * Sets add tTL time.
     */
    public static void setAddTTLTime () {
        String fileName = "./flashdb_ttl.txt";

        FileChannel channel = null;
        MappedByteBuffer byteByffer = null;
        Scanner scanner = null;

        try {
            channel = new FileInputStream(fileName).getChannel();
            byteByffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            CharsetDecoder decoder = Charset.forName("UTF-8").newDecoder();
            CharBuffer charBuffer = decoder.decode(byteByffer);
            scanner = new Scanner(charBuffer).useDelimiter("\n");

            while (scanner.hasNext()) {
                String tmpData = scanner.next();

                if (tmpData != null && !"".equals(tmpData) && tmpData.length() > 0) {
                    String arrayTmpData[] = tmpData.split(":");

                    CacheMemInterface.addTTLTime.put(arrayTmpData[0], Integer.parseInt(arrayTmpData[1]));
                }
            }
        } catch (IOException iex) {
            iex.printStackTrace();
        } finally {
            try {
                if (scanner != null) {
                    scanner.close();
                }
                if (channel != null) {
                    channel.close();
                }
            } catch (IOException iex2) {
                iex2.printStackTrace();
            }
        }
    }
}
