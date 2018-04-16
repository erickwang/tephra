package org.lpw.tephra.carousel;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * 本地服务。
 *
 * @author lpw
 */
public interface LocalService extends Callable<String> {
    /**
     * 创建本地服务。
     *
     * @param uri       URI地址。
     * @param ip        IP地址。
     * @param header    头信息集。
     * @param parameter 参数集。
     * @return 当前实例。
     */
    LocalService build(String uri, String ip, Map<String, String> header, Map<String, String> parameter);
}
