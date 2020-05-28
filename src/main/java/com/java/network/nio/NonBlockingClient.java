package com.java.network.nio;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;

/**
 * <p>
 *     通过NIO的非阻塞方式实现的Socket客户端
 * </p>
 * @author zhangbin
 * @date 2020-05-28
 */
public class NonBlockingClient {

    private static Selector selector;

    static {
        try {
            selector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void request(String host, int port) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        SocketAddress socketAddress = new InetSocketAddress(host, port);
        socketChannel.configureBlocking(false);
        socketChannel.socket().setSoTimeout(10000);
        socketChannel.connect(socketAddress);
        socketChannel.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }

    public void select() throws IOException {
        while (selector.select() > 0) {
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while (iterator.hasNext()) {
                SelectionKey selectionKey = iterator.next();
                iterator.remove();

                if (selectionKey.isConnectable()) {
                    connect(selectionKey);
                } else if (selectionKey.isReadable()) {
                    read(selectionKey);
                } else if (selectionKey.isWritable()) {
                    write(selectionKey);
                }
            }
        }
    }

    private void connect(@NotNull SelectionKey selectionKey) throws IOException {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        socketChannel.finishConnect();
        InetSocketAddress inetSocketAddress = (InetSocketAddress) socketChannel.socket().getRemoteSocketAddress();
        System.out.println(String.format("Connected to %s:%s.", inetSocketAddress.getHostName(), inetSocketAddress.getPort()));
    }

    private void read(@NotNull SelectionKey selectionKey) throws IOException {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        socketChannel.read(byteBuffer);
        byteBuffer.flip();

        String data = Charset.defaultCharset().decode(byteBuffer).toString();
        if ("".equals(data)) {
            selectionKey.cancel();
        } else {
            InetSocketAddress inetSocketAddress = (InetSocketAddress) socketChannel.socket().getRemoteSocketAddress();
            System.out.println(String.format("Read from %s:%s.", inetSocketAddress.getHostName(), inetSocketAddress.getPort()));
            System.out.println(data);
        }
    }

    private void write(@NotNull SelectionKey selectionKey) throws IOException {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        InetSocketAddress inetSocketAddress = (InetSocketAddress) socketChannel.socket().getRemoteSocketAddress();
        System.out.println(String.format("Write to %s:%s.", inetSocketAddress.getHostName(), inetSocketAddress.getPort()));
        socketChannel.write(Charset.defaultCharset().encode(getRequest(inetSocketAddress.getHostName())));
        selectionKey.interestOps(SelectionKey.OP_READ);
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
        final String[] hosts = { "localhost", "127.0.0.1", "10.26.3.55" };
        NonBlockingClient client = new NonBlockingClient();
        for (final String host : hosts) {
            client.request(host, 9527);
        }
        client.select();
    }
}
