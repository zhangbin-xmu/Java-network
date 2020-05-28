package com.java.network.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.CharsetUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;

/**
 * <p>
 *     通过Netty实现的Socket服务端
 * </p>
 * @author zhangbin
 * @date 2020-05-28
 */
public class NettyServer {

    public void start(int port) throws InterruptedException {
        ServerBootstrap serverBootstrap = new ServerBootstrap()
                .group(new NioEventLoopGroup())
                .localAddress(new InetSocketAddress(port))
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) {
                        socketChannel.pipeline().addLast(new SimpleChannelInboundHandler<Object>() {

                            private StringBuilder stringBuilder = new StringBuilder();

                            @Override
                            protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) {
                                ByteBuf byteBuf = (ByteBuf) o;
                                while (byteBuf.isReadable()) {
                                    stringBuilder.append((char) byteBuf.readByte());
                                }
                            }

                            @Override
                            public void channelReadComplete(ChannelHandlerContext ctx) {
                                InetSocketAddress inetSocketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
                                System.out.println(String.format("Read from %s:%s.", inetSocketAddress.getHostName(), inetSocketAddress.getPort()));
                                System.out.println(stringBuilder.toString());
                                ctx.channel().close();
                                ctx.close();
                            }

                            @Override
                            public void channelActive(ChannelHandlerContext ctx) {
                                InetSocketAddress inetSocketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
                                System.out.println(String.format("Write to %s:%s.", inetSocketAddress.getHostName(), inetSocketAddress.getPort()));
                                ctx.writeAndFlush(Unpooled.copiedBuffer(getResponse(), CharsetUtil.UTF_8));
                            }
                        });
                    }
                });

        ChannelFuture channelFuture = serverBootstrap.bind().sync();
        channelFuture.addListener((ChannelFutureListener) future -> {
            if (! future.isSuccess()) {
                future.cause().printStackTrace();
                future.channel().close();
            }
        });

        System.out.println("Server started.");
        channelFuture.channel().closeFuture().sync();
    }

    @NotNull
    @Contract(pure = true)
    private String getResponse() {
        return "GET / HTTP/1.1\r\n"
                + "User-Agent: curl/7.43.0\r\n"
                + "Accept: */*\r\n\r\n";
    }

    public static void main(String[] args) throws InterruptedException {
        new NettyServer().start(9527);
    }
}
