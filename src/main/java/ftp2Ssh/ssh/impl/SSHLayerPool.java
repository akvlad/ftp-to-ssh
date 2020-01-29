package ftp2Ssh.ssh.impl;

import java.beans.ConstructorProperties;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import expectj.TimeoutException;
import ftp2Ssh.ssh.ISSHLayerPool;
import ftp2Ssh.ssh.ISSHLayerPooled;
import ftp2Ssh.ssh.AsyncCmdRes;
import ftp2Ssh.ssh.ISSHCommandHelperFactory;

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class SSHLayerPool implements ISSHLayerPool {
	private String[] spawnCmd;
	private int max;
	private Executor asyncExecutor;
	private final BlockingQueue<ISSHLayerPooled> idle = new LinkedBlockingQueue<>();
	private final Set<ISSHLayerPooled> acquired = Collections.synchronizedSet(new HashSet<>());
	private int spawning = 0;
	private int timeoutMSec = 30000;
	private ISSHCommandHelperFactory sshCmdHelperFactory;

	public SSHLayerPool(String spawnCmd, int max) {
		setSpawnCmd(spawnCmd.split("\\s+"));
		setMax(max);
	}
	
	public SSHLayerPool(String[] spawnCmd, int max) {
		setSpawnCmd(spawnCmd);
		setMax(max);
	}
	
	public SSHLayerPool() {
	}
	
	private void spawn() throws IOException, TimeoutException {
		if (idle.size() + acquired.size() + spawning >= this.max) {
			return;
		}
		spawning++;
		asyncExecutor.execute(() -> {
			SSHLayerPooled newPooled;
			try {
				newPooled = new SSHLayerPooled(this.spawnCmd, sshCmdHelperFactory, this);
				newPooled.setSshCmdHelperFactory(sshCmdHelperFactory);
				newPooled.setTimeoutSec(timeoutMSec / 1000);
				idle.put(newPooled);
			} catch (IOException | TimeoutException | InterruptedException e) {
				e.printStackTrace();
			} finally {
				spawning--;
			}
		});
	}
	
	@Override
	public ISSHLayerPooled acquire() throws IOException, InterruptedException, TimeoutException {
		spawn();
		ISSHLayerPooled res = null;
		res = idle.poll(timeoutMSec, TimeUnit.MILLISECONDS);
		if (res == null) {
			throw new IllegalStateException("Timeout error: all workers are acquired");
		}
		synchronized (acquired) {
			acquired.add(res);
		}
		return res;
	}
	
	@Override
	public void release(ISSHLayerPooled pooled) {
		synchronized (acquired) {
			if (!acquired.contains(pooled)) {
				return;
			}
			acquired.remove(pooled);
		}
		try {
			idle.put(pooled);
		} catch (Exception e) {
			e.printStackTrace();
			pooled.close(sshCmdHelperFactory.getHelper().exit());
		}
	}
	
	@Override
	public Future<AsyncCmdRes> cmdA(String cmd) throws IOException, InterruptedException, TimeoutException {
		final ISSHLayerPooled executor = acquire();
		final FutureTask<AsyncCmdRes> res = new FutureTask<>(() -> {
			AsyncCmdRes asyncRes = null;
			try {
				asyncRes = new AsyncCmdRes(null, executor.cmd(cmd));
			} catch (Exception e) {
				asyncRes = new AsyncCmdRes(e, null);
			} finally {
				executor.release();
			}
			return asyncRes;
		});
		asyncExecutor.execute(() -> {
			res.run();
		});
		return res;
	}
	
	@Override
	public void close(String cmd) throws IllegalStateException {
		if (acquired.size() > 0) {
			throw new IllegalStateException("Not all executors are released");
		}
		for (ISSHLayerPooled l : idle) {
			l.close(cmd);
		}
		idle.clear();
		acquired.clear();
	}
	
	@Override
	public void forceClose(String cmd) {
		for (ISSHLayerPooled l : idle) {
			l.close(cmd);
		}
		for (ISSHLayerPooled l : acquired) {
			l.close(cmd);
		}
		idle.clear();
		acquired.clear();
	}

	@Override
	@Autowired
	@Qualifier("timeoutMSec")
	public void setTimeoutMSec(int timeoutMsec) {
		this.timeoutMSec = timeoutMsec;
	}

	public ISSHCommandHelperFactory getSshCmdHelperFactory() {
		return sshCmdHelperFactory;
	}

	@Autowired
	public void setSshCmdHelperFactory(ISSHCommandHelperFactory sshCmdHelperFactory) {
		this.sshCmdHelperFactory = sshCmdHelperFactory;
	}

	public int getMax() {
		return max;
	}

	@Autowired
	@Qualifier("maxLayers")
	public void setMax(int max) {
		if (asyncExecutor != null) {
			synchronized (this) {
				((ExecutorService)asyncExecutor).shutdown();
			}
		}
		this.max = max;
		this.asyncExecutor = Executors.newFixedThreadPool(max);
	}

	public String[] getSpawnCmd() {
		return spawnCmd;
	}

	public void setSpawnCmd(String[] spawnCmd) {
		this.spawnCmd = spawnCmd;
	}

}
