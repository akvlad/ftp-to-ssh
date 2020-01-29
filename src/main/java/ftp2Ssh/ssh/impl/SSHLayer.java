package ftp2Ssh.ssh.impl;

import java.io.IOException;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import expectj.ExpectJ;
import expectj.Spawn;
import expectj.TimeoutException;
import ftp2Ssh.ssh.ISSHCommandHelperFactory;

public class SSHLayer {
	
	private Spawn spawned;
	private int index = 0;
	private int timeoutSec = 30;
	
	private ISSHCommandHelperFactory sshCmdHelperFactory;
	
	public SSHLayer(String spawnCmd, ISSHCommandHelperFactory sshCommandHelperFactory) throws IOException, TimeoutException, InterruptedException {
		ExpectJ spawner = new ExpectJ();
		spawned = spawner.spawn(spawnCmd);
		this.sshCmdHelperFactory = sshCommandHelperFactory;
		cmd(sshCmdHelperFactory.getHelper().echo("HI"));
	}
	
	public String cmd(String cmd) throws IOException, TimeoutException, InterruptedException {
		synchronized (this) {
			String cnts = "";
			int index = this.index;
			String delimiter = String.format("---CUT HERE---%d-%d", new Date().getTime(), (int)(Math.random()*65535));
			int reps = 0;
			try {
				this.spawned.send(cmd + "\n" + sshCmdHelperFactory.getHelper().echo(delimiter)+"\n");
				int len = index;
				long start = (new Date()).getTime();
				while (len == index) {
					reps++;
					try {
						this.spawned.expect(delimiter, Math.max(timeoutSec / 10, 1));
						Thread.sleep(100);
						cnts = this.spawned.getCurrentStandardOutContents();
						len = cnts.length();
					} catch (TimeoutException e) {
						cnts = this.spawned.getCurrentStandardOutContents();
						if (cnts.endsWith(delimiter + "\n")) {
							cnts = this.spawned.getCurrentStandardOutContents();
							len = cnts.length();
						} else if (((new Date()).getTime() - start) > (timeoutSec * 1000)) {
							throw e;
						} else {
							len = index;
						}
					} 
				}
				this.index = cnts.length();
			return cnts.substring(index, cnts.length() - delimiter.length() - 1);
			} catch (IOException | TimeoutException e) {
				throw new RuntimeException(
						"ERR: " + this.spawned.getCurrentStandardErrContents() +
						"\nOUT: " + this.spawned.getCurrentStandardOutContents() + Integer.toString(this.spawned.getCurrentStandardOutContents().length())+ 
						"\nCommand: " + cmd + 
						"\nDELIM: " + delimiter +
						String.format("\nindex: %d\nreps: %d", this.index, reps), e);
			} catch (StringIndexOutOfBoundsException e) {
				System.out.printf("index: %d - %d, CONTENTS!!!! %s\n", index, this.index, cnts);
				throw e;
			}
		}
	}
	
	public void close(String cmd) {
		if (this.spawned.isClosed()) {
			this.spawned.stop();
			return;
		}
		if (cmd != null) {
			try {
				this.spawned.send(cmd + "\n");
				this.spawned.expectClose(1);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		this.spawned.stop();
	}
	
	public boolean isClosed() {
		return this.spawned.isClosed();
	}

	public ISSHCommandHelperFactory getSshCmdHelperFactory() {
		return sshCmdHelperFactory;
	}

	public void setSshCmdHelperFactory(ISSHCommandHelperFactory sshCmdHelperFactory) {
		this.sshCmdHelperFactory = sshCmdHelperFactory;
	}

	@Autowired
	@Qualifier("timeoutSec")
	public void setTimeoutSec(int timeoutSec) {
		this.timeoutSec = timeoutSec;
	}
}
