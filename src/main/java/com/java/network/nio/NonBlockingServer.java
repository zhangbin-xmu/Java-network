package com.java.network.nio;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;

/**
 * <p>
 *     通过NIO的非阻塞方式实现的Socket服务
 * </p>
 * @author zhangbin
 * @date 2020-05-28
 */
public class NonBlockingServer {

    private static Selector selector;

    static {
        try {
            selector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start(int port) throws IOException {
        try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) {
            InetSocketAddress inetSocketAddress = new InetSocketAddress(port);
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.socket().bind(inetSocketAddress);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("Server started.");

            while (true) {
                try {
                    selector.select();
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        SelectionKey selectionKey = iterator.next();
                        iterator.remove();

                        if (selectionKey.isAcceptable()) {
                            accept(selectionKey);
                        } else if (selectionKey.isReadable()) {
                            read(selectionKey);
                        } else if (selectionKey.isWritable()) {
                            write(selectionKey);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }

    private void accept(@NotNull SelectionKey selectionKey) throws IOException {
        try {
            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
            SocketChannel socketChannel = serverSocketChannel.accept();
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

            InetSocketAddress inetSocketAddress = (InetSocketAddress) socketChannel.socket().getRemoteSocketAddress();
            System.out.println(String.format("Accepted from %s:%s.", inetSocketAddress.getHostName(), inetSocketAddress.getPort()));
        } catch (Exception e) {
            e.printStackTrace();
            selectionKey.channel().close();
        }
    }

    private void read(@NotNull SelectionKey selectionKey) throws IOException {
        try {
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
        } catch (Exception e) {
            e.printStackTrace();
            selectionKey.channel().close();
        }
    }

    private void write(@NotNull SelectionKey selectionKey) throws IOException {
        try {
            SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
            InetSocketAddress inetSocketAddress = (InetSocketAddress) socketChannel.socket().getRemoteSocketAddress();
            System.out.println(String.format("Write to %s:%s.", inetSocketAddress.getHostName(), inetSocketAddress.getPort()));
            socketChannel.write(Charset.defaultCharset().encode(getResponse()));
            selectionKey.interestOps(SelectionKey.OP_READ);
        } catch (Exception e) {
            e.printStackTrace();
            selectionKey.channel().close();
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
        new NonBlockingServer().start(9527);
    }
}
