package ftp2Ssh.ssh.commands;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import ftp2Ssh.SpringContext;
import ftp2Ssh.ssh.IB64Decoder;
import ftp2Ssh.ssh.ISSHCommandHelper;
import ftp2Ssh.ssh.ISSHCommandHelperFactory;
import java.util.Base64;

@Component
public class SSHCommandHelperFactory implements ISSHCommandHelperFactory {
	
	private boolean gzip = false;

	@Override
	public ISSHCommandHelper getHelper() {
		return gzip ? SpringContext.getContext().getBean("GzippedCommandHelper", SSHGzippedCommandHelper.class) : 
			SpringContext.getContext().getBean("PlainCommandHelper", SSHPlainCommandHelper.class);
	}
	
	@Override
	public IB64Decoder getDecoder() {
		return gzip ? new SSHGzippedDecoder() : Base64.getDecoder()::decode;
	}

	public boolean isGzip() {
		return gzip;
	}
	
	@Autowired(required = false)
	@Qualifier("gzip")
	public void setGzip(boolean gzip) {
		this.gzip = gzip;
	}

}
