package org.lpw.tephra.chrome;

import com.alibaba.fastjson.JSONObject;
import org.lpw.tephra.bean.BeanFactory;
import org.lpw.tephra.bean.ContextClosedListener;
import org.lpw.tephra.bean.ContextRefreshedListener;
import org.lpw.tephra.storage.StorageListener;
import org.lpw.tephra.storage.Storages;
import org.lpw.tephra.util.Converter;
import org.lpw.tephra.util.Generator;
import org.lpw.tephra.util.Http;
import org.lpw.tephra.util.Io;
import org.lpw.tephra.util.Json;
import org.lpw.tephra.util.Logger;
import org.lpw.tephra.util.Thread;
import org.lpw.tephra.util.Validator;
import org.lpw.tephra.ws.WsClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author lpw
 */
@Component("tephra.chrome")
public class ChromeImpl implements Chrome, StorageListener, ContextRefreshedListener, ContextClosedListener {
    @Inject
    private Validator validator;
    @Inject
    private Converter converter;
    @Inject
    private Io io;
    @Inject
    private Http http;
    @Inject
    private Json json;
    @Inject
    private Generator generator;
    @Inject
    private Thread thread;
    @Inject
    private Logger logger;
    @Inject
    private WsClients wsClients;
    @Value("${tephra.chrome.services:/WEB-INF/chrome}")
    private String services;
    @Value("${tephra.chrome.max-thread:5}")
    private int maxThread;
    private ExecutorService executorService;
    private List<String> list;

    @Override
    public byte[] pdf(String url, int wait, int width, int height, String range) {
        if (validator.isEmpty(list))
            return null;

        JSONObject message = new JSONObject();
        message.put("id", generator.random(1, 9999));
        message.put("method", "Page.printToPDF");
        JSONObject params = new JSONObject();
        params.put("printBackground", true);
        params.put("paperWidth", width / 96.0D);
        params.put("paperHeight", height / 96.0D);
        params.put("marginTop", 0.0D);
        params.put("marginBottom", 0.0D);
        params.put("marginLeft", 0.0D);
        params.put("marginRight", 0.0D);
        params.put("pageRanges", range);
        message.put("params", params);
        Future<byte[]> future = executorService.submit(BeanFactory.getBean(ChromeClient.class).set(
                list.get(generator.random(0, list.size() - 1)), url, wait, message));

        try {
            return future.get();
        } catch (Exception e) {
            logger.warn(e, "获取PDF数据[{}:{}:{}:{}]时发生异常！", url, wait, width, height);

            return null;
        }
    }

    @Override
    public byte[] png(String url, int wait, int x, int y, int width, int height) {
        return img(url, wait, "png", 0, x, y, width, height);
    }

    @Override
    public byte[] jpeg(String url, int wait, int x, int y, int width, int height) {
        return img(url, wait, "jpeg", 100, x, y, width, height);
    }

    private byte[] img(String url, int wait, String format, int quality, int x, int y, int width, int height) {
        if (validator.isEmpty(list))
            return null;

        JSONObject message = new JSONObject();
        message.put("id", generator.random(1, 9999));
        message.put("method", "Page.captureScreenshot");
        JSONObject params = new JSONObject();
        params.put("format", format);
        if (quality > 0)
            params.put("quality", quality);
        JSONObject clip = new JSONObject();
        clip.put("offsetX", x);
        clip.put("offsetY", y);
        clip.put("pageX", 0);
        clip.put("pageY", 0);
        clip.put("clientWidth", x + width);
        clip.put("clientHeight", y + height);
        params.put("clip", clip);
        message.put("params", params);
        Future<byte[]> future = executorService.submit(BeanFactory.getBean(ChromeClient.class).set(
                list.get(generator.random(0, list.size() - 1)), url, wait, message));

        try {
            return future.get();
        } catch (Exception e) {
            logger.warn(e, "获取PDF数据[{}:{}:{}:{}]时发生异常！", url, wait, width, height);

            return null;
        }
    }

    @Override
    public String getStorageType() {
        return Storages.TYPE_DISK;
    }

    @Override
    public String[] getScanPathes() {
        return new String[]{services};
    }

    @Override
    public void onStorageChanged(String path, String absolutePath) {
        Set<String> set = new HashSet<>();
        for (String string : converter.toArray(io.readAsString(absolutePath), "\n")) {
            string = string.trim();
            if (string.length() == 0 || string.charAt(0) == '#' || string.indexOf(':') == -1)
                continue;

            set.add(string);
        }
        list = new ArrayList<>(set);
        if (logger.isInfoEnable())
            logger.info("更新Chrome调试服务集[{}]。", converter.toString(set));
    }

    @Override
    public int getContextRefreshedSort() {
        return 9;
    }

    @Override
    public void onContextRefreshed() {
        executorService = Executors.newFixedThreadPool(maxThread);
    }

    @Override
    public int getContextClosedSort() {
        return 9;
    }

    @Override
    public void onContextClosed() {
        executorService.shutdown();
    }
}
