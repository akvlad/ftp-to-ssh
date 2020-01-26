package ftp2Ssh.ssh;

import java.io.IOException;
import java.util.concurrent.Future;

import expectj.TimeoutException;

public interface ISSHLayerPool {

	ISSHLayerPooled acquire() throws IOException, InterruptedException, TimeoutException;

	Future<AsyncCmdRes> cmdA(String cmd) throws IOException, InterruptedException, TimeoutException;

	void close(String cmd) throws IllegalStateException;

	void forceClose(String cmd);

	void release(ISSHLayerPooled pooled);

	void setTimeoutMSec(int timeoutMsec);

}