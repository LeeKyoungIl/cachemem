package com.leekyoungil.cachemem.socket;

import com.leekyoungil.cachemem.memcached.MemcachedControl;
import com.leekyoungil.cachemem.memcached.MemcachedResult;
import com.leekyoungil.cachemem.util.CacheMemUtil;
import org.vertx.java.core.http.HttpServerRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by kyoungil_lee on 2014. 8. 5..
 */
public class WebHandler {
    private volatile static WebHandler instance;

    private HttpServerRequest req;
    private final String saveFilePath = "/dev/shm/flashdb/";

    private WebHandler () { }

    public static WebHandler getInstance () {
        if (instance == null) {
            synchronized (WebHandler.class) {
                instance = new WebHandler();
            }
        }

        return instance;
    }

    /**
     * Action data.
     *
     * Memcached Control interface
     *
     * @param req the req
     * @return the memcached result
     */
    public MemcachedResult actionData (HttpServerRequest req) {
        MemcachedResult memcachedResult = null;

        if (!validationCheck(req)) {
            memcachedResult = new MemcachedResult();
            memcachedResult.setResult(false);

            return memcachedResult;
        }

        switch (req.path().toUpperCase()) {
            case "/GET" :
                memcachedResult = MemcachedControl.getInstance().get(req.params().get("key"));
                break;

            case "/SET" :
                switch (req.method().toUpperCase()) {
                    case "GET" :
                        memcachedResult = MemcachedControl.getInstance().set(req.params().get("key"), CacheMemUtil.toByteArray(req.params().get("value")), Integer.parseInt(req.params().get("ttl")), null, null, null, null);
                        break;

                    case "POST" :
                        String filePath = this.saveFilePath+req.params().get("key");

                        req.expectMultiPart(true);
                        req.uploadHandler((upload) -> {
                            upload.streamToFileSystem(filePath);
                        });

                        try {
                            byte[] data = Files.readAllBytes(Paths.get(filePath));

                            if (data.length > 0) {
                                memcachedResult = MemcachedControl.getInstance().set(req.params().get("key"), data, Integer.parseInt(req.params().get("ttl")), req.params().get("siteName"), req.params().get("ipAddress"), req.params().get("itemName"), req.params().get("originKey"));
                            } else {
                                memcachedResult.setResult(false);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        Path fileLocation = Paths.get(filePath);

                        if (Files.exists(fileLocation)) {
                            try {
                                Files.delete(fileLocation);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        break;

                    default:
                        memcachedResult = new MemcachedResult();
                        memcachedResult.setResult(false);
                        break;
                }
                break;

            case "/PURGE" :
                memcachedResult = MemcachedControl.getInstance().purge(req.params().get("key"));
                break;

            default:
                memcachedResult = new MemcachedResult();
                memcachedResult.setResult(false);
                break;
        }

        return memcachedResult;
    }

    /**
     * Validation check.
     *
     * Request validation check
     *
     * @param req the req
     * @return the boolean
     */
    private boolean validationCheck (HttpServerRequest req) {
        if (req == null || req.path() == null || req.path().length() < 2 || req.params().get("key") == null || req.params().get("key").isEmpty()) {
            return false;
        }

        return true;
    }
}
