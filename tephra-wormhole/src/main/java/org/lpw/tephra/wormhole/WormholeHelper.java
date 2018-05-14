package org.lpw.tephra.wormhole;

import java.io.File;
import java.io.InputStream;

/**
 * @author lpw
 */
public interface WormholeHelper {
    /**
     * 保存图片。
     *
     * @param path        目录。
     * @param name        名称。
     * @param suffix      文件名后缀。
     * @param sign        签名密钥名。
     * @param inputStream 输入流。
     * @return URI地址。
     */
    String image(String path, String name, String suffix, String sign, InputStream inputStream);

    /**
     * 保存图片。
     *
     * @param path 目录。
     * @param name 名称。
     * @param sign 签名密钥名。
     * @param file 文件。
     * @return URI地址。
     */
    String image(String path, String name, String sign, File file);

    /**
     * 保存文件。
     *
     * @param path        目录。
     * @param name        名称。
     * @param suffix      文件名后缀。
     * @param sign        签名密钥名。
     * @param inputStream 输入流。
     * @return URI地址。
     */
    String file(String path, String name, String suffix, String sign, InputStream inputStream);

    /**
     * 保存文件。
     *
     * @param path 目录。
     * @param name 名称。
     * @param sign 签名密钥名。
     * @param file 文件。
     * @return URI地址。
     */
    String file(String path, String name, String sign, File file);
}
