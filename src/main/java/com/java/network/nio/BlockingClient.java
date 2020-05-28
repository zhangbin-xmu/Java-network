package com.java.network.nio;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

/**
 * <p>
 *     通过NIO的阻塞方式实现的Socket客户端
 * </p>
 * @author zhangbin
 * @date 2020-05-28
 */
public class BlockingClient {

    private void request(String host, int port) throws IOException {
        try (SocketChannel socketChannel = SocketChannel.open()) {
            SocketAddress socketAddress = new InetSocketAddress(host, port);
            socketChannel.connect(socketAddress);

             try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socketChannel.socket().getInputStream()));
                  PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(socketChannel.socket().getOutputStream()))) {
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

    public static void main(String[] args) throws IOException {
        final String[] hosts = { "www.baidu.com", "www.weibo.com", "www.sina.com" };
        for (final String host : hosts) {
            new BlockingClient().request(host, 80);
        }
    }
}
