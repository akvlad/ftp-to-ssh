package ftp2Ssh.ssh.impl;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import ftp2Ssh.SpringContext;
import ftp2Ssh.ssh.ISSHLayerPool;
import ftp2Ssh.ssh.ISSHPoolFactory;

@Component
public class SSHPoolFactory implements ISSHPoolFactory {
	
	@Override
	public ISSHLayerPool createPool(String spawnCmd) {
		SSHLayerPool pool = SpringContext.getContext().getBean(SSHLayerPool.class);
		pool.setSpawnCmd(spawnCmd);
		return pool;
	}


}
