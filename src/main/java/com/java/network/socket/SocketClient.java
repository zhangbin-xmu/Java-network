package com.java.network.socket;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.*;

/**
 * <p>
 *     Socket客户端
 * </p>
 * @author zhangbin
 * @date 2020-05-28
 */
public class SocketClient {

    public void request(String host, int port) throws IOException {
        try (Socket socket = new Socket()) {
            SocketAddress socketAddress = new InetSocketAddress(host, port);
            socket.connect(socketAddress);

            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()))) {
                // 发起请求。
                printWriter.write(getRequest(host));
                printWriter.flush();

                // 读取响应。
                String message;
                while (null != (message = bufferedReader.readLine())) {
                    System.out.println(message);
                }
            }
        }
    }

    @NotNull
    @Contract(pure = true)
    private String getRequest(String host) {
        return "GET / HTTP/1.1\r\n"
                + String.format("Host: %s\r\n", host)
                + "User-Agent: curl/7.43.0\r\n"
                + "Accept: */*\r\n\r\n";
    }

    public static void main(String[] args) {
        final String[] hosts = { "localhost", "127.0.0.1", "10.26.3.55" };
        ExecutorService executorService = new ThreadPoolExecutor(2, 4, 0L,
                TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new ThreadFactoryBuilder().setNameFormat("thread-pool-%d").build());

        for (final String host : hosts) {
            executorService.submit(() -> {
                try {
                    new SocketClient().request(host, 9527);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
