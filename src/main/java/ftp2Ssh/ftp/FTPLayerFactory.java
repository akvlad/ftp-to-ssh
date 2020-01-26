package ftp2Ssh.ftp;

import org.springframework.stereotype.Component;

import ftp2Ssh.SpringContext;

@Component
public class FTPLayerFactory {
	
	public FTPLayer getFTPLayer() {
		return SpringContext.getContext().getBean(FTPLayer.class);
	}

}
