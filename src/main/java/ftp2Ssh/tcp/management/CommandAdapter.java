package ftp2Ssh.tcp.management;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ftp2Ssh.ftp.FTPLayer;
import ftp2Ssh.ftp.FTPLayerFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.channel.ChannelHandler;

@Component
@ChannelHandler.Sharable
public class CommandAdapter extends ChannelInboundHandlerAdapter{

	private FTPLayerFactory ftpLayerFactory;
	private Executor cmdExec = Executors.newSingleThreadExecutor();
	
	private FTPLayer getLayer(ChannelHandlerContext ctx) {
		Attribute<FTPLayer> attr = ctx.channel().attr(AttributeKey.valueOf(FTPLayer.class, "FTP"));
		FTPLayer layer = attr.get(); 
		if (layer == null) {
			layer = ftpLayerFactory.getFTPLayer();
			attr.set(layer);
			layer.addResponseListener((String response) -> {
				ByteBuf bResp = ctx.alloc().buffer(response.getBytes().length);
				bResp.writeBytes(response.getBytes(StandardCharsets.UTF_8));
				ctx.writeAndFlush(bResp);
				//bResp.release();
			});
		}
		return layer;
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
		String response = "220 Welcome\r\n";
		ByteBuf bResp = ctx.alloc().buffer(response.getBytes().length);
		bResp.writeBytes(response.getBytes(StandardCharsets.UTF_8));
		ctx.writeAndFlush(bResp);
		//bResp.release();
	}
	
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		FTPLayer layer = getLayer(ctx);
		ByteBuf in = (ByteBuf) msg;
		byte[] buff = new byte[in.readableBytes()];
		in.readBytes(buff);
		String localAddress = ((InetSocketAddress)ctx.channel().localAddress()).getAddress().getHostAddress();
		String strBuff = new String(buff);
		//in.release();
		cmdExec.execute(() -> {
			for (String s : strBuff.split("\n")) {
				if (s.isEmpty()) {
					continue;
				}
				layer.CMD(s, () -> localAddress );
			}
		});
		
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		System.out.println("CHANNEL INACTIVE HANDLER !!!!!!");
		Attribute<FTPLayer> attr = ctx.channel().attr(AttributeKey.valueOf(FTPLayer.class, "FTP"));
		FTPLayer layer = attr.get(); 
		if (layer == null) {
			System.out.println("LAYER == null");
			return;
			
		}
		System.out.println("LAYER close");
		layer.quit();
	}
	
	public FTPLayerFactory getFtpLayerFactory() {
		return ftpLayerFactory;
	}
	
	@Autowired
	public void setFtpLayerFactory(FTPLayerFactory ftpLayerFactory) {
		this.ftpLayerFactory = ftpLayerFactory;
	}
	

}
