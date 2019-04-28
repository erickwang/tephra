package org.lpw.tephra.util;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;

/**
 * @author lpw
 */
@Component("tephra.util.ssh")
public class SshImpl implements Ssh {
    @Inject
    private Validator validator;
    @Inject
    private Io io;
    @Inject
    private Logger logger;

    @Override
    public String shell(String host, int port, String user, String password, String... commands) {
        if (validator.isEmpty(host) || port < 1 || validator.isEmpty(user) || validator.isEmpty(commands))
            return null;

        try {
            Session session = getSession(host, port, user, password);
            ChannelShell channelShell = (ChannelShell) session.openChannel("shell");
            channelShell.setPty(true);
            InputStream inputStream = channelShell.getInputStream();
            PrintWriter writer = new PrintWriter(channelShell.getOutputStream());
            channelShell.connect();
            for (String command : commands) {
                writer.println(command);
                writer.flush();
            }
            String string = null;
            if (inputStream.available() > 0)
                string = io.readAsString(inputStream);
            writer.close();
            inputStream.close();
            channelShell.disconnect();
            session.disconnect();
            if (logger.isInfoEnable())
                logger.info("执行SSH[{}:{}:{}:{}:{}]完成[{}]。", host, port, user, password, Arrays.toString(commands), string);

            return string;
        } catch (Throwable throwable) {
            logger.warn(throwable, "执行SSH[{}:{}:{}:{}:{}]时发生异常！", host, port, user, password, Arrays.toString(commands));

            return null;
        }
    }

    @Override
    public String exec(String host, int port, String user, String password, String command) {
        if (validator.isEmpty(host) || port < 1 || validator.isEmpty(user) || validator.isEmpty(command))
            return null;

        try {
            Session session = getSession(host, port, user, password);
            ChannelExec channelExec = (ChannelExec) session.openChannel("exec");
            channelExec.setPty(true);
            channelExec.setCommand(command);
            InputStream inputStream = channelExec.getInputStream();
            channelExec.connect();
            String string = null;
            if (inputStream.available() > 0)
                string = io.readAsString(inputStream);
            inputStream.close();
            channelExec.disconnect();
            session.disconnect();
            if (logger.isInfoEnable())
                logger.info("执行SSH[{}:{}:{}:{}:{}]完成[{}]。", host, port, user, password, command, string);

            return string;
        } catch (Throwable throwable) {
            logger.warn(throwable, "执行SSH[{}:{}:{}:{}:{}]时发生异常！", host, port, user, password, command);

            return null;
        }
    }

    @Override
    public boolean get(String host, int port, String user, String password, String path, OutputStream outputStream) {
        if (validator.isEmpty(host) || port < 1 || validator.isEmpty(user) || validator.isEmpty(path))
            return false;

        try {
            Session session = getSession(host, port, user, password);
            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
            channelSftp.get(path, outputStream);
            outputStream.close();
            channelSftp.disconnect();
            session.disconnect();
            if (logger.isInfoEnable())
                logger.info("下载[{}:{}:{}:{}]文件[{}]完成。", host, port, user, password, path);

            return true;
        } catch (Throwable throwable) {
            logger.warn(throwable, "下载[{}:{}:{}:{}]文件[{}]时发生异常！", host, port, user, password, path);

            return false;
        }
    }

    @Override
    public boolean put(String host, int port, String user, String password, String path, int mode, InputStream inputStream) {
        if (validator.isEmpty(host) || port < 1 || validator.isEmpty(user) || validator.isEmpty(path))
            return false;

        try {
            Session session = getSession(host, port, user, password);
            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
            channelSftp.put(inputStream, path, mode);
            inputStream.close();
            channelSftp.disconnect();
            session.disconnect();
            if (logger.isInfoEnable())
                logger.info("上传[{}:{}:{}:{}]文件[{}]完成。", host, port, user, password, path);

            return true;
        } catch (Throwable throwable) {
            logger.warn(throwable, "上传[{}:{}:{}:{}]文件[{}]时发生异常！", host, port, user, password, path);

            return false;
        }
    }

    private Session getSession(String host, int port, String user, String password) throws JSchException {
        Session session = new JSch().getSession(user, host, port);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect(60 * 60 * 1000);

        return session;
    }
}
