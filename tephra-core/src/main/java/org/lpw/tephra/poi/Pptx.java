package org.lpw.tephra.poi;

import com.alibaba.fastjson.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * PPTx处理器。
 *
 * @author lpw
 */
public interface Pptx {
    /**
     * 输出PPTx。
     *
     * @param object       数据。
     * @param outputStream 输出流。
     */
    void write(JSONObject object, OutputStream outputStream);

    /**
     * 读取并解析PPTx数据。
     *
     * @param inputStream 输入流。
     * @return 数据。
     */
    JSONObject read(InputStream inputStream);
}
