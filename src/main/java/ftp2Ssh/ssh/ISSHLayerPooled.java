package ftp2Ssh.ssh;

import java.io.IOException;

import expectj.TimeoutException;

public interface ISSHLayerPooled {
	void release();
	String cmd(String cmd) throws IOException, InterruptedException, TimeoutException;
	void close(String cmd);
	boolean isClosed();
}