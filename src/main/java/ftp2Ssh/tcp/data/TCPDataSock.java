package ftp2Ssh.tcp.data;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import ftp2Ssh.ftp.IFTPDataSock;

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class TCPDataSock implements IFTPDataSock {
	
	private ServerSocket serverSocket;
	private Socket socket;
	private String activeHost; 
	private int activePort;
	private boolean activeMode;
	private String localIP;
	
	private void closeServerSocket() {
		if (serverSocket == null) {
			return;
		}
		try {
			serverSocket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void closeSocket() {
		if (socket == null) {
			return;
		}
		try {
			socket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void passiveMode(String host) {
		closeSocket();
		closeServerSocket();
		activeMode = false;
		try {
			localIP = InetAddress.getByName(host).getHostAddress();
			serverSocket = new ServerSocket(0, 0, InetAddress.getByName(host));
		} catch (Exception e) {
			throw new RuntimeException(e);
		} 
		
	}

	@Override
	public void activeMode(String host, int port) {
		closeSocket();
		closeServerSocket();
		activeMode = true;
		activeHost = host;
		activePort = port;
	}

	@Override
	public void startDataSock(String host) throws UnknownHostException, IOException {
		if (activeMode) {
			socket = new Socket(activeHost, activePort);
			return;
		}
		if (serverSocket == null) {
			passiveMode(host);
		}
		socket = serverSocket.accept();
	}

	@Override
	public void stopDataSock() {
		closeSocket();
		
	}

	@Override
	public void write(byte[] buff) throws IOException {
		socket.getOutputStream().write(buff);
		
	}

	@Override
	public void write(String buff) throws UnsupportedEncodingException, IOException {
		this.write(buff.getBytes("UTF-8"));
	}

	@Override
	public byte[] read(int maxRead) throws Exception {
		byte[] res = new byte[maxRead];
		int read = socket.getInputStream().read(res);
		if (read == -1) {
			return null;
		}
		return Arrays.copyOfRange(res, 0, read) ;
	}

	@Override
	public String getHost() {
		return localIP;
	}

	@Override
	public int getPort() {
		return serverSocket.getLocalPort();
	}
	
	@Override
	public void close() {
		closeServerSocket();
		closeSocket();
	}

}
