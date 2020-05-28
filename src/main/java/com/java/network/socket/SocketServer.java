package com.java.network.socket;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *     Socket服务
 * </p>
 * @author zhangbin
 * @date 2020-05-28
 */
public class SocketServer {

    public void start(int port) throws IOException {
        ExecutorService executorService = new ThreadPoolExecutor(2, 4, 0L,
                TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new ThreadFactoryBuilder().setNameFormat("thread-pool-%d").build());

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started.");

            while (true) {
                try {
                    final Socket socket = serverSocket.accept();
                    System.out.println(String.format("Accepted connection from %s.", socket));

                    executorService.submit(() -> {
                        try (PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()))) {
                            printWriter.write(getResponse());
                            printWriter.flush();
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }

    @NotNull
    @Contract(pure = true)
    private String getResponse() {
        return "GET / HTTP/1.1\r\n"
                + "User-Agent: curl/7.43.0\r\n"
                + "Accept: */*\r\n\r\n";
    }

    public static void main(String[] args) throws IOException {
        new SocketServer().start(9527);
    }
}
