package org.lpw.tephra.dao.jdbc;


/**
 * 搜集更新SQL，并在同一个事务中批量更新。
 *
 * @author lpw
 */
public interface BatchUpdate {
    /**
     * 开始收集。
     */
    void begin();

    /**
     * 收集。
     *
     * @param dataSource 数据源。
     * @param sql        SQL。
     * @param args       参数集。
     * @return 如果已收集则返回true；否则返回false。
     */
    boolean collect(String dataSource, String sql, Object[] args);

    /**
     * 提交更新。
     */
    void commit();
}
