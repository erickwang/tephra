package org.lpw.tephra.ctrl.upload;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.lpw.tephra.bean.BeanFactory;
import org.lpw.tephra.bean.ContextRefreshedListener;
import org.lpw.tephra.storage.Storage;
import org.lpw.tephra.storage.Storages;
import org.lpw.tephra.util.DateTime;
import org.lpw.tephra.util.Generator;
import org.lpw.tephra.util.Image;
import org.lpw.tephra.util.Json;
import org.lpw.tephra.util.Logger;
import org.lpw.tephra.util.Message;
import org.lpw.tephra.util.Validator;
import org.lpw.tephra.wormhole.WormholeHelper;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lpw
 */
@Service(UploadService.PREFIX + "service")
public class UploadServiceImpl implements UploadService, ContextRefreshedListener {
    @Inject
    private Validator validator;
    @Inject
    private Json json;
    @Inject
    private Message message;
    @Inject
    private DateTime dateTime;
    @Inject
    private Generator generator;
    @Inject
    private Image image;
    @Inject
    private Logger logger;
    @Inject
    private Storages storages;
    @Inject
    private WormholeHelper wormholeHelper;
    @Inject
    private JsonConfigs jsonConfigs;
    private Map<String, UploadListener> listeners;

    @Override
    public JSONArray uploads(String content) {
        if (validator.isEmpty(content))
            return new JSONArray();

        JSONArray uploads = json.toArray(content);
        if (validator.isEmpty(uploads))
            return new JSONArray();

        List<UploadReader> uploadReaders = new ArrayList<>();
        for (int i = 0, size = uploads.size(); i < size; i++)
            uploadReaders.add(new SimpleUploadReader(json.toMap(uploads.getJSONObject(i))));

        try {
            return uploads(uploadReaders);
        } catch (IOException e) {
            logger.warn(e, "处理JSON方式上传文件时发生异常！");

            return new JSONArray();
        }
    }

    @Override
    public JSONArray uploads(List<UploadReader> uploadReaders) throws IOException {
        JSONArray array = new JSONArray();
        for (UploadReader uploadReader : uploadReaders)
            array.add(upload(uploadReader));

        return array;
    }

    @Override
    public JSONObject upload(Map<String, String> map) {
        try {
            return upload(new SimpleUploadReader(map));
        } catch (IOException e) {
            logger.warn(e, "处理文件[{}:{}:{}]上传时发生异常！", map.get("name"), map.get("fileName"), map.get("contentType"));

            return new JSONObject();
        }
    }

    @Override
    public JSONObject upload(UploadReader uploadReader) throws IOException {
        String name = uploadReader.getName();
        UploadListener listener = getListener(name);
        if (listener == null)
            return failure(uploadReader, message.get(PREFIX + "listener.not-exists", name));

        String contentType = listener.getContentType(name, uploadReader.getContentType(), uploadReader.getFileName());
        if (!listener.isUploadEnable(name, uploadReader)) {
            logger.warn(null, "无法处理文件上传请求[key={}&content-type={}&file-name={}]！",
                    name, contentType, uploadReader.getFileName());

            return failure(uploadReader, message.get(PREFIX + "disable", name, contentType, uploadReader.getFileName()));
        }

        JSONObject object = listener.settle(name, uploadReader);
        if (object == null)
            object = save(name, listener, uploadReader, contentType);
        uploadReader.delete();
        listener.complete(uploadReader, object);

        return object;
    }

    private UploadListener getListener(String key) {
        if (listeners.containsKey(key))
            return listeners.get(key);

        for (String k : listeners.keySet())
            if (validator.isMatchRegex(k, key))
                return listeners.get(k);

        UploadListener listener = jsonConfigs.get(key);
        if (listener == null)
            logger.warn(null, "无法获得上传监听器[{}]，文件上传失败！", key);

        return listener;
    }

    private JSONObject save(String key, UploadListener listener, UploadReader reader, String contentType) throws IOException {
        Storage storage = storages.get(listener.getStorage());
        if (storage == null) {
            logger.warn(null, "无法获得存储处理器[{}]，文件上传失败！", listener.getStorage());

            return failure(reader, message.get(PREFIX + "storage.not-exists", listener.getStorage()));
        }

        JSONObject object = new JSONObject();
        object.put("success", true);
        object.put("name", reader.getName());
        object.put("fileName", reader.getFileName());
        object.put("fileSize", reader.getSize());
        String suffix = getSuffix(listener, reader);

        if (storage.getType().equals(Storages.TYPE_DISK)) {
            String path = listener.getPath(reader.getName(), contentType, reader.getFileName());
            String whPath = image.is(contentType, reader.getFileName()) ?
                    wormholeHelper.image(path, null, suffix, null, reader.getInputStream()) :
                    wormholeHelper.file(path, null, suffix, null, reader.getInputStream());
            if (whPath != null) {
                object.put("path", whPath);
                if (logger.isDebugEnable())
                    logger.debug("保存上传文件[{}]。", object);

                return object;
            }
        }

        String path = (ROOT + contentType + "/" + listener.getPath(reader.getName(), contentType, reader.getFileName()) + "/"
                + dateTime.toString(dateTime.today(), "yyyyMMdd") + "/" + generator.random(32)
                + suffix).replaceAll("[/]+", "/");
        object.put("path", path);
        reader.write(storage, path);
        String thumbnail = thumbnail(listener.getImageSize(key), storage, contentType, path);
        if (thumbnail != null)
            object.put("thumbnail", thumbnail);
        if (logger.isDebugEnable())
            logger.debug("保存上传文件[{}]。", object);

        return object;
    }

    private String getSuffix(UploadListener listener, UploadReader reader) {
        String suffix = listener.getSuffix(listener.getKey(), reader.getContentType(), reader.getFileName());
        if (!validator.isEmpty(suffix))
            return suffix;

        int indexOf = reader.getFileName().lastIndexOf('.');

        return indexOf == -1 ? "" : reader.getFileName().substring(indexOf);
    }

    private String thumbnail(int[] size, Storage storage, String contentType, String path) {
        if (size == null || size.length != 2 || (size[0] <= 0 && size[1] <= 0))
            return null;

        try {
            BufferedImage image = this.image.read(storage.getInputStream(path));
            if (image == null)
                return null;

            image = this.image.thumbnail(image, size[0], size[1]);
            if (image == null)
                return null;

            int indexOf = path.lastIndexOf('.');
            String thumbnail = path.substring(0, indexOf) + ".thumbnail" + path.substring(indexOf);
            this.image.write(image, this.image.formatFromContentType(contentType), storage.getOutputStream(thumbnail));

            return thumbnail;
        } catch (Exception e) {
            logger.warn(e, "生成压缩图片时发生异常！");

            return null;
        }
    }

    private JSONObject failure(UploadReader uploadReader, String message) {
        JSONObject object = new JSONObject();
        object.put("success", false);
        object.put("name", uploadReader.getName());
        object.put("fileName", uploadReader.getFileName());
        object.put("message", message);

        return object;
    }

    @Override
    public void remove(String key, String uri) {
        UploadListener listener = getListener(key);
        if (listener == null) {
            logger.warn(null, "无法获得上传监听key[{}]，删除失败！", key);

            return;
        }

        storages.get(listener.getStorage()).delete(uri);

        if (logger.isDebugEnable())
            logger.debug("删除上传的文件[{}:{}]。", listener.getStorage(), uri);
    }

    @Override
    public int getContextRefreshedSort() {
        return 19;
    }

    @Override
    public void onContextRefreshed() {
        if (listeners != null)
            return;

        listeners = new HashMap<>();
        BeanFactory.getBeans(UploadListener.class).forEach(listener -> listeners.put(listener.getKey(), listener));
    }
}
