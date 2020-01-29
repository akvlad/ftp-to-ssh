package ftp2Ssh.ssh.impl;

import java.io.IOException;

import expectj.TimeoutException;
import ftp2Ssh.ssh.ISSHCommandHelperFactory;
import ftp2Ssh.ssh.ISSHLayerPool;
import ftp2Ssh.ssh.ISSHLayerPooled;

public class SSHLayerPooled extends SSHLayer implements ISSHLayerPooled {
	
	private ISSHLayerPool pool;

	public SSHLayerPooled(String[] spawnCmd, ISSHCommandHelperFactory sshCommandHelperFactory, ISSHLayerPool pool) throws IOException, TimeoutException, InterruptedException {
		super(spawnCmd, sshCommandHelperFactory);
		this.pool = pool;
	}
	
	public void release() {
		this.pool.release(this);
	}
	
}
