package org.lpw.tephra.ctrl.upload;

import org.lpw.tephra.bean.BeanFactory;
import org.lpw.tephra.storage.Storage;
import org.lpw.tephra.util.Coder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author lpw
 */
public class SimpleUploadReader implements UploadReader {
    private String name;
    private String fileName;
    private String contentType;
    private String base64;
    private String string;
    private Map<String, String> map;
    private byte[] bytes;
    private InputStream inputStream;

    SimpleUploadReader(Map<String, String> map) {
        name = map.get("name");
        fileName = map.get("fileName");
        contentType = map.get("contentType");
        base64 = map.get("base64");
        string = map.get("string");
        Set<String> set = new HashSet<>();
        set.add("name");
        set.add("fileName");
        set.add("contentType");
        set.add("base64");
        set.add("string");
        this.map = new HashMap<>();
        map.keySet().stream().filter(key -> !set.contains(key)).forEach(key -> this.map.put(key, map.get(key)));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public String getParameter(String name) {
        return map.get(name);
    }

    @Override
    public long getSize() {
        return getBytes().length;
    }

    @Override
    public void write(Storage storage, String path) throws IOException {
        storage.write(path, getBytes());
    }

    @Override
    public InputStream getInputStream() {
        if (inputStream == null)
            inputStream = new ByteArrayInputStream(getBytes());

        return inputStream;
    }

    @Override
    public byte[] getBytes() {
        if (bytes == null)
            bytes = base64 == null ? string.getBytes() : BeanFactory.getBean(Coder.class).decodeBase64(base64);

        return bytes;
    }

    @Override
    public void delete() throws IOException {
    }
}