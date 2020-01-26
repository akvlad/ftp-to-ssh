package ftp2Ssh.ftp;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;

public interface IFTPDataSock {
	
	void activeMode(String host, int port);
	
	//void startDataSock() throws UnknownHostException, IOException;
	void stopDataSock();
	
	void write(byte[] buff) throws IOException;
	void write(String buff) throws UnsupportedEncodingException, IOException;
	byte[] read(int maxRead) throws Exception;
	
	String getHost();
	int getPort();
	void passiveMode(String host);

	void close();

	void startDataSock(String host) throws UnknownHostException, IOException;
	

}
