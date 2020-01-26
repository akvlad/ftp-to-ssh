package ftp2Ssh.tcp.management;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

@Component
public class ManagementServerInit {
	private int port;
	private String host = "0.0.0.0";
	private CommandAdapter commandAdapter;
	private ServerBootstrap bootstrap;
	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;
	
	public void run() throws Exception {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        try {
            bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
            	.channel(NioServerSocketChannel.class)
            	.childHandler(new ChannelInitializer<SocketChannel>() {

					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast(commandAdapter);
					}
					@Override
					public void channelInactive(ChannelHandlerContext ctx) throws Exception {
						System.out.println("CHANNEL INACTIVE INITER !!!!!!");
						super.channelInactive(ctx);
						
					}
            	});
            ChannelFuture f = bootstrap.bind(host, port).sync();
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
	
	public void close() {
		workerGroup.shutdownGracefully();
		bossGroup.shutdownGracefully();
	}
	
	public int getPort() {
		return port;
	}
	
	@Autowired
	@Qualifier("port")
	public void setPort(int port) {
		this.port = port;
	}
	
	public CommandAdapter getCommandAdapter() {
		return commandAdapter;
	}

	@Autowired
	public void setCommandAdapter(CommandAdapter adapter) {
		this.commandAdapter = adapter;
	}
	
	public String getHost() {
		return host;
	}
	
	@Autowired
	@Qualifier("host")
	public void setHost(String host) {
		this.host = host;
	}
}
