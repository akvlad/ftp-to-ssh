package ftp2Ssh.ssh;

public interface ISSHPoolFactory {
	ISSHLayerPool createPool(String spawnCmd);
}
