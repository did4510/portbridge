package com.darsh.portbridge.tunnel;

import org.slf4j.Logger;
import com.darsh.portbridge.PortBridge;
import java.io.*;
import java.net.Socket;

public class TunnelForwarder {
    private static final Logger LOGGER = PortBridge.LOGGER;
    private static final int BUFFER_SIZE = 8192;

    private final String connectionId;
    private final Socket relaySocket;
    private final Socket localSocket;
    private final TunnelClient tunnelClient;

    public TunnelForwarder(String connectionId, Socket relaySocket, Socket localSocket, TunnelClient tunnelClient) {
        this.connectionId = connectionId;
        this.relaySocket = relaySocket;
        this.localSocket = localSocket;
        this.tunnelClient = tunnelClient;
    }

    public void start() {
        new Thread(this::forwardRelayToLocal, "TunnelFwd-" + connectionId + "-R2L").start();
        new Thread(this::forwardLocalToRelay, "TunnelFwd-" + connectionId + "-L2R").start();
    }

    private void forwardRelayToLocal() {
        byte[] buffer = new byte[BUFFER_SIZE];
        try (InputStream relayIn = relaySocket.getInputStream();
             OutputStream localOut = localSocket.getOutputStream()) {

            int bytesRead;
            while ((bytesRead = relayIn.read(buffer)) != -1) {
                localOut.write(buffer, 0, bytesRead);
                localOut.flush();
                tunnelClient.recordTransfer(bytesRead);
            }
        } catch (IOException e) {
            LOGGER.debug("[PortBridge] Error forwarding relay to local", e);
        }
    }

    private void forwardLocalToRelay() {
        byte[] buffer = new byte[BUFFER_SIZE];
        try (InputStream localIn = localSocket.getInputStream();
             OutputStream relayOut = relaySocket.getOutputStream()) {

            int bytesRead;
            while ((bytesRead = localIn.read(buffer)) != -1) {
                relayOut.write(buffer, 0, bytesRead);
                relayOut.flush();
                tunnelClient.recordTransfer(bytesRead);
            }
        } catch (IOException e) {
            LOGGER.debug("[PortBridge] Error forwarding local to relay", e);
        }
    }
}
