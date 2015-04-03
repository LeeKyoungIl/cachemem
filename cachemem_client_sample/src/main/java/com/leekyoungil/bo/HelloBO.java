package com.leekyoungil.bo;

import com.leekyoungil.cachemem.client.simple.CacheMem;
import com.leekyoungil.cachemem.client.socket.CacheMemSocketClient;
import com.leekyoungil.cachemem.client.socket.model.CacheMemResult;
import com.leekyoungil.model.Hello;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

/**
 * Created by Kyoungil_Lee on 4/3/15.
 */
@Service
public class HelloBO {

    /**
     * Gets text.
     *
     * @param key the key
     * @param i the i
     * @return the text
     */
    @CacheMem(itemName = "getText_METHOD", siteName = "CacheMem_Sample", ttl = 30)
    public String getText (String key, int i) {
        System.out.println("aop");
        return "temp1";
    }

    /**
     * Gets model.
     *
     * @param name the name
     * @param age the age
     * @param cellPhone the cell phone
     * @return the model
     */
    @CacheMem(itemName = "getModel_METHOD", siteName = "CacheMem_Sample", ttl = 30)
    public Hello getModel (String name, int age, String cellPhone) {
        Hello hello = new Hello();
        hello.setName(name);
        hello.setAge(age);
        hello.setCellPhone(cellPhone);

        return hello;
    }

    /**
     * Gets model using socket.
     *
     * @param name the name
     * @param age the age
     * @param cellPhone the cell phone
     * @return the model using socket
     */
    public Hello getModelUsingSocket (String name, int age, String cellPhone) {
        String siteName = "CacheMem_Sample";

        StringBuilder parameter = new StringBuilder();
        parameter.append(siteName);
        parameter.append("_");
        parameter.append("getModelUsingSocket");
        parameter.append("_");
        parameter.append(name);
        parameter.append("_");
        parameter.append(age);
        parameter.append("_");
        parameter.append(cellPhone);

        String key = DigestUtils.md5Hex(parameter.toString());

        CacheMemResult cacheMemResultGet = CacheMemSocketClient.get(0, key, 0);

        Hello hello = null;

        if (!cacheMemResultGet.isResult()) {
            hello = new Hello();
            hello.setName(name);
            hello.setAge(age);
            hello.setCellPhone(cellPhone);

            byte[] bytes = CacheMemSocketClient.toByteArray(hello);

            new Thread(() -> {
                CacheMemSocketClient.set(0, siteName, parameter.toString(), "getModelUsingSocket_METHOD", key, bytes, 300, "0.0.0.1", 0);
            }).start();
        } else {
            hello = (Hello) cacheMemResultGet.getResultData();
        }

        return hello;
    }
}
