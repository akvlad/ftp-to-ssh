package ftp2Ssh;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class SpringContext {
	
	private static AnnotationConfigApplicationContext ctx; 
	
	public static ApplicationContext getContext() {
		synchronized (SpringContext.class) {
			if (ctx == null) {
				ctx = new AnnotationConfigApplicationContext("ftp2Ssh", "ftp2Ssh.ssh.impl");
			}
		}
		return ctx;
		
	}
}
