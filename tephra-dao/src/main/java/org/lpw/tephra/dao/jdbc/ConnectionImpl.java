package org.lpw.tephra.dao.jdbc;

import org.lpw.tephra.dao.ConnectionSupport;
import org.lpw.tephra.dao.Mode;
import org.lpw.tephra.util.Converter;
import org.lpw.tephra.util.Logger;
import org.lpw.tephra.util.Validator;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.HashMap;
import java.util.Map;

/**
 * @author lpw
 */
@Repository("tephra.dao.jdbc.connection")
public class ConnectionImpl extends ConnectionSupport<Connection> implements org.lpw.tephra.dao.jdbc.Connection {
    @Inject
    private Validator validator;
    @Inject
    private Converter converter;
    @Inject
    private Logger logger;
    @Inject
    private org.lpw.tephra.dao.jdbc.DataSource dataSource;
    private ThreadLocal<Map<String, Connection>> connections = new ThreadLocal<>();
    private ThreadLocal<Map<String, Savepoint>> savepoints = new ThreadLocal<>();
    private ThreadLocal<Boolean> transactional = new ThreadLocal<>();

    @Override
    public void beginTransaction() {
        transactional.set(true);
    }

    @Override
    public Connection get(String dataSource, Mode mode) {
        if (dataSource == null)
            dataSource = this.dataSource.getDefaultKey();
        if ((transactional.get() != null && transactional.get()) || !this.dataSource.hasReadonly(dataSource))
            mode = Mode.Write;
        Map<String, Connection> connections = this.connections.get();
        if (connections == null) {
            connections = new HashMap<>();
            savepoints.set(new HashMap<>());
        }
        String key = dataSource + mode.ordinal();
        Connection connection = connections.get(key);
        try {
            if (connection != null) {
                if (isOpen(connection))
                    return connection;

                connections.remove(key);
                savepoints.get().remove(key);
            }

            DataSource ds = null;
            if (mode == Mode.Read)
                ds = this.dataSource.getReadonly(dataSource);
            if (ds == null)
                ds = this.dataSource.getWriteable(dataSource);
            connection = ds.getConnection();
            connection.setAutoCommit(mode == Mode.Read);
            savepoints.get().put(key, connection.setSavepoint());
            connections.put(key, connection);
            this.connections.set(connections);

            return connection;
        } catch (Exception e) {
            logger.warn(e, "获取数据库[{}:{}]连接时发生异常！", dataSource, mode);

            throw new NullPointerException("获取数据库[" + dataSource + "," + mode + "]连接时发生异常！");
        }
    }

    @Override
    public void fail(Throwable throwable) {
        Map<String, Connection> connections = this.connections.get();
        if (connections != null) {
            connections.forEach((key, connection) -> {
                try {
                    if (isOpen(connection) && !connection.getAutoCommit())
                        connection.rollback(savepoints.get().get(key));
                } catch (SQLException e) {
                    logger.warn(e, "回滚数据库连接时发生异常！");
                }
            });
            close(connections);

            if (logger.isDebugEnable())
                logger.debug("回滚[{}]个数据库连接！", connections.size());
        }
        remove();
    }

    @Override
    public void close() {
        Map<String, Connection> connections = this.connections.get();
        if (connections != null) {
            commit(connections);
            close(connections);

            if (logger.isDebugEnable())
                logger.debug("关闭[{}]个数据库连接！", connections.size());
        }
        remove();
    }

    private void commit(Map<String, Connection> connections) {
        try {
            for (Connection connection : connections.values())
                if (isOpen(connection) && !connection.getAutoCommit())
                    connection.commit();
        } catch (SQLException e) {
            logger.warn(e, "提交数据库[{}]事务时发生异常！", converter.toString(connections));

            fail(e);
        }
    }

    private boolean isOpen(Connection connection) throws SQLException {
        return !connection.isClosed();
    }

    private void close(Map<String, Connection> connections) {
        connections.forEach((key, connection) -> {
            try {
                connection.releaseSavepoint(savepoints.get().get(key));
                connection.close();
            } catch (SQLException e) {
                logger.warn(e, "关闭数据库[{}]事务时发生异常！", key);

                fail(e);
            }
        });
    }

    private void remove() {
        connections.remove();
        savepoints.remove();
        transactional.remove();
    }
}
