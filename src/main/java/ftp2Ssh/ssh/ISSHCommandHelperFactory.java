package ftp2Ssh.ssh;

public interface ISSHCommandHelperFactory {
	
	ISSHCommandHelper getHelper();
	IB64Decoder getDecoder();

}
