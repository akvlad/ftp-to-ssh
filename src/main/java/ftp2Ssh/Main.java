package ftp2Ssh;

import java.io.IOException;

import expectj.Executor;
import expectj.ExpectJ;
import expectj.ExpectJException;
import expectj.Spawn;
import expectj.TimeoutException;
import ftp2Ssh.tcp.management.ManagementServerInit;

public class Main {

	public static void main(String[] args) {
		try {
			ManagementServerInit init = SpringContext.getContext().getBean(ManagementServerInit.class);
			init.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
