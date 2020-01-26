package ftp2Ssh.ssh;

public class AsyncCmdRes {
	public Exception e; 
	public String res;
	public AsyncCmdRes(Exception e, String res) {
		this.e = e;
		this.res = res;
	}
}