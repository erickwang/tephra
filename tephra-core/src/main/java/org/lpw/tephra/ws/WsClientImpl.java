package org.lpw.tephra.ws;

import org.lpw.tephra.util.Converter;
import org.lpw.tephra.util.Logger;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.websocket.ClientEndpoint;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.io.IOException;
import java.net.URI;

/**
 * @author lpw
 */
@Component("tephra.ws.client")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@ClientEndpoint
public class WsClientImpl implements WsClient {
    @Inject
    private Converter converter;
    @Inject
    private Logger logger;
    private WebSocketContainer container;
    private WsClientListener listener;
    private Session session;

    @Override
    public WsClient setContainer(WebSocketContainer container) {
        this.container = container;

        return this;
    }

    @Override
    public void connect(WsClientListener listener, String url) {
        this.listener = listener;
        try {
            container.connectToServer(this, new URI(url));
        } catch (Exception e) {
            logger.warn(e, "连接远程WebSocket服务[{}]时发生异常！", url);
            close();
        }
    }

    @OnOpen
    public void open(Session session) {
        this.session = session;
        listener.connect();
        if (logger.isDebugEnable())
            logger.debug("连接到远程WebSocket服务[{}]。", session);
    }

    @OnMessage
    public void message(String message) {
        listener.receive(message);
        if (logger.isDebugEnable() || logger.isDebugEnable())
            logger.debug("接收到远程WebSocket发送的数据[{}]。", converter.toBitSize(message.length()));
    }

    @OnError
    public void error(Session session, Throwable throwable) {
        logger.warn(throwable, "与远程WebSocket服务[{}]交互时发生异常！", session);
        close();
    }

    @Override
    public void send(String message) {
        session.getAsyncRemote().sendText(message);
        if (logger.isDebugEnable())
            logger.debug("发送数据[{}]到远程WebSocket服务[{}]。", message, session);
    }

    @Override
    public void close() {
        if (session == null)
            return;

        if (logger.isDebugEnable())
            logger.debug("关闭远程WebSocket连接[{}]。", session);

        try {
            if (session.isOpen())
                session.close();
            session = null;
        } catch (IOException e) {
            logger.warn(e, "关闭远程WebSocket连接[{}]时发生异常！", session);
        }
    }
}
