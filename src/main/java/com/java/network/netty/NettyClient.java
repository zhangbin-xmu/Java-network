package com.java.network.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;

/**
 * <p>
 *     通过Netty实现的Socket客户端
 * </p>
 * @author zhangbin
 * @date 2020-05-28
 */
public class NettyClient {

    private Bootstrap bootstrap;

    public NettyClient() {
        bootstrap = new Bootstrap()
                .group(new NioEventLoopGroup())
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_LINGER, 0)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel channel) {
                        System.out.println("Channel Initialize.");
                        channel.pipeline().addLast(new SimpleChannelInboundHandler<Object>() {

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
                                ctx.writeAndFlush(Unpooled.copiedBuffer(getRequest(inetSocketAddress.getHostName()), CharsetUtil.UTF_8));
                            }
                        });
                    }
                });
    }

    public void request(String host, int port) {
        ChannelFuture channelFuture = bootstrap.connect(host, port);
        channelFuture.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                System.out.println("Complete.");
            } else {
                future.cause().printStackTrace();
                future.channel().close();
            }
        });
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
        NettyClient client = new NettyClient();
        for (final String host : hosts) {
            client.request(host, 9527);
        }
    }
}
