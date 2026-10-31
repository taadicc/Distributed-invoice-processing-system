package net.unibl.etf.mdp.parser.net;

import net.unibl.etf.mdp.common.config.AppConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TcpServer {

    private static final Logger log = LoggerFactory.getLogger(TcpServer.class);

    private final int port;
    private final ExecutorService pool;

    public TcpServer(int port, int brojNiti) {
        this.port = port;
        this.pool = Executors.newFixedThreadPool(brojNiti);
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {

            log.info("Parser TCP server slusa na portu {}", port);

            while (true) {
                Socket socket = serverSocket.accept();
                pool.submit(new ClientHandler(socket));
            }

        } catch (IOException e) {
            log.error("Greska u radu TCP servera", e);
        } finally {
            pool.shutdown();
        }
    }
}