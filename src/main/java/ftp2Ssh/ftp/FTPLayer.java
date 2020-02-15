package ftp2Ssh.ftp;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import expectj.TimeoutException;
import ftp2Ssh.ssh.AsyncCmdRes;
import ftp2Ssh.ssh.IB64Decoder;
import ftp2Ssh.ssh.ISSHCommandHelper;
import ftp2Ssh.ssh.ISSHCommandHelperFactory;
import ftp2Ssh.ssh.ISSHLayerPool;
import ftp2Ssh.ssh.ISSHLayerPooled;
import ftp2Ssh.ssh.ISSHPoolFactory;

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class FTPLayer extends Observable {
	
	private ISSHLayerPool sshPool;
	private ISSHLayerPooled mainExec;
	private ISSHPoolFactory sshPoolFactory;
	private IFTPDataSock dataSock;
	private ISSHCommandHelperFactory cmdHelperFactory;
	private Executor helperExecutor = Executors.newFixedThreadPool(1);
	private List<FTPResponseListener> listeners = new ArrayList<>();
	
	
	private String login;
	private String password;
	private String renameFile;
	private String tmpFolder;
	private String DS;
	private int chunkSize = 10000;
	private int timeoutMSec = 30000;
	
	private IFTPRequestContext ctx;
	
	public IB64Decoder getDecoder() {
		return cmdHelperFactory.getDecoder();
	}


	
	public ISSHCommandHelperFactory getCmdHelperFactory() {
		return cmdHelperFactory;
	}

	@Autowired
	public void setCmdHelperFactory(ISSHCommandHelperFactory cmdHelperFactory) {
		this.cmdHelperFactory = cmdHelperFactory;
		DS = cmdHelperFactory.getHelper().DS();
		tmpFolder = cmdHelperFactory.getHelper().tmpFolder();
	}

	public ISSHPoolFactory getSSHPoolFactory() {
		return sshPoolFactory;
	}

	@Autowired
	public void setSSHPoolFactory(ISSHPoolFactory factory) {
		this.sshPoolFactory = factory;
	}

	public void addResponseListener(FTPResponseListener listener) {
		listeners.add(listener);
	}
	
	public void removeResponseListener(FTPResponseListener listener) {
		listeners.remove(listener);
	}
	
	public IFTPDataSock getDataSock() {
		return dataSock;
	}
	
	@Autowired
	public void setDataSock(IFTPDataSock dataSock) {
		this.dataSock = dataSock;
	}
	
	public void CMD(String command, IFTPRequestContext ctx) {
		try {
			this.ctx = ctx;
			String cmd = command.split(" ")[0].toUpperCase().trim();
			Method method = this.getClass().getDeclaredMethod("ftp"+cmd, String.class);
			method.invoke(this, command.trim());
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			fireResponse("502 Command not implemented.");
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			Throwable c = e;
			while(c.getCause() != null) {
				if (!(c instanceof FTPException)) {
					c = c.getCause();
					continue;
				}
				FTPException _e = (FTPException) (e.getCause());
				fireResponse(String.format("%d %s", _e.getCode(), _e.getMessage()));
				return;
			}
			fireResponse("451 " + c.getMessage());
		}  catch (Throwable e) {
			e.printStackTrace();
			fireResponse("451 " + e.getMessage());
		}
		
	}
	
	public int getChunkSize() {
		return chunkSize;
	}
	
	@Autowired
	@Qualifier("chunkSize")
	public void setChunkSize(int chunkSize) {
		this.chunkSize = chunkSize;
	}

	public int getTimeoutMSec() {
		return timeoutMSec;
	}
	
	@Autowired
	@Qualifier("timeoutMSec")
	public void setTimeoutMSec(int timeoutMSec) {
		this.timeoutMSec = timeoutMSec;
	}
	
	public void close() {
		if (dataSock == null) return;
		dataSock.close();
	}
	
	public void quit() {
		try {
			close();
			mainExec.release();
			sshPool.close(cmdHelper().exit());
		} catch (IllegalStateException e) {
			e.printStackTrace();
			sshPool.forceClose(cmdHelper().exit());
		}
	}
	
	private String getTmpFolder() {
		return String.format("%s%sFTP%d", tmpFolder, DS,  (int)(Math.random() * 65535)); 
	}

	
	private void fireResponse(String response) {
		for (FTPResponseListener l : listeners) {
			l.onResponse(response+"\r\n");
		}
	}
	
	private boolean createPool() throws Exception {
		if (sshPool != null) {
			return true;
		}
		if (login == null || password == null) {
			return false;
		}
		sshPool = sshPoolFactory.createPool(cmdHelper().getSpawnCmd(login, password));
		mainExec = sshPool.acquire();
		return true;
	}
	
	private ISSHCommandHelper cmdHelper() {
		return getCmdHelperFactory().getHelper();
	}
	
	private void ftpQUIT(String cmd) {
		quit();
	}
	
	private void ftpBYE(String cmd) {
		this.ftpQUIT(cmd);
	}
	
	private void ftpSYST(String cmd) {
		fireResponse("215 UNIX Type: L8");
	}
	
    private void ftpOPTS(String cmd) throws FTPException {
    	if (cmd.substring(5).toUpperCase().equals("UTF8 ON")) {
    		fireResponse("200 OK.");
    	} else {
    		throw new FTPException("Sorry", 451);
    	}
    }
    
    public void ftpUSER(String cmd) throws Exception {
    	login = cmd.substring(5);
    	boolean created = createPool();
    	fireResponse(created ? "230 OK" : "331 OK");
    }
    private void ftpPASS(String cmd) throws Exception {
    	password = cmd.substring(5);
   		boolean created = createPool();
   		fireResponse(created ? "230 OK" : "331 OK");
    }
    
    private void ftpNOOP(String cmd) throws IOException, InterruptedException, TimeoutException {
   		mainExec.cmd(cmdHelper().echo("HI"));
   		fireResponse("200 OK.");
    }
    
    private void ftpTYPE(String cmd) throws FTPException {
        String type = cmd.substring(5,6);
        fireResponse("200 Type set to " + type.toUpperCase());
    }
    
    private void ftpCDUP(String cmd) throws IOException, InterruptedException, TimeoutException {
    	mainExec.cmd(cmdHelper().cd(".."));
    	fireResponse("200 OK.");
    }
    
    private void ftpPWD(String cmd) throws IOException, InterruptedException, TimeoutException {
    	String pwd = mainExec.cmd(cmdHelper().pwd());
    	fireResponse("257 " + pwd.trim());
    }
    
    private void ftpCWD(String cmd) throws IOException, InterruptedException, TimeoutException {
        String chwd = cmd.substring(4);
        mainExec.cmd(cmdHelper().cd(chwd));
        fireResponse("250 OK");
    }
    private void ftpPORT(String cmd) {
    	String[] l=cmd.substring(5).split(",");
        String dataAddr=String.join(".", Arrays.copyOfRange(l, 0, 4));
        int dataPort=(Integer.parseInt(l[4])<<8)+Integer.parseInt(l[5]);
    	getDataSock().activeMode(dataAddr, dataPort);
        fireResponse("200 Get port.");
    }
    
    private void ftpPASV(String cmd) throws IOException {
        getDataSock().passiveMode(ctx.getDest());
        String ip = getDataSock().getHost();
        int port = getDataSock().getPort();
        fireResponse(String.format("227 Entering Passive Mode (%s,%d,%d).", String.join(",", ip.split("\\.")), port>>8&0xFF, port&0xFF));
    }
    
    private void ftpLIST(String cmd) throws IOException, InterruptedException, TimeoutException {
    	
    	try {
	    	fireResponse("150 Here comes the directory listing.");
	    	String dir = cmd.length() <= 5 ? "." : cmd.substring(5);
	    	getDataSock().startDataSock(ctx.getDest());
	        String out = mainExec.cmd(cmdHelper().lsla(dir));
	        out = out.replaceAll("([dlrwx\\-t]{10})\\.", "$1");
	        getDataSock().write(out + "\r\n");
    	} catch (IOException | InterruptedException | TimeoutException e) {
    		throw e;
    	} finally {
    		getDataSock().stopDataSock();
    	}
    	fireResponse("226 Directory send OK.");
    }
        
    
    
    private void ftpSIZE(String cmd) throws IOException, InterruptedException, TimeoutException {
        String name = cmd.substring(5);
        long out = fileSize(name);
        fireResponse("213 " + Long.toString(out));
    }
    
    private void ftpMKD(String cmd) throws IOException, InterruptedException, TimeoutException {
        String dirName = cmd.substring(4);
        String out = mainExec.cmd(cmdHelper().mkdir(dirName));
        fireResponse("257 Directory created.");
    }
    
    private void ftpRMD(String cmd) throws IOException, InterruptedException, TimeoutException {
        String dirName=cmd.substring(4);
        mainExec.cmd(cmdHelper().rmrf(dirName));
        fireResponse("250 Deleted");
    }
    
    private void ftpDELE(String cmd) throws IOException, InterruptedException, TimeoutException {
    	String dirName=cmd.substring(5);
        mainExec.cmd(cmdHelper().rmrf(dirName));
        fireResponse("250 Deleted");
    }
    
    
    private void ftpRNFR(String cmd) {
        renameFile=cmd.substring(5);
        fireResponse("350 Ready.");
    }

    private void ftpRNTO(String cmd) throws IOException, InterruptedException, TimeoutException {
        String name =cmd.substring(5);
        mainExec.cmd(cmdHelper().mv(renameFile, name));
        fireResponse("250 File renamed.");
    }
    
    private void ftpRETR(String cmd) throws Exception {
    	FolderTransmitter transmitter = null;
    	try {
    		ISSHCommandHelper helper = getCmdHelperFactory().getHelper();
	        String name = cmd.substring(5);
	        long size = fileSize(name);
	        fireResponse("150 Opening data connection.");
	        dataSock.startDataSock(ctx.getDest());
	        BlockingQueue<Future<AsyncCmdRes>> results = new LinkedBlockingQueue<>();
	        transmitter = new FolderTransmitter(helper, sshPool);
	        final FolderTransmitter _transmitter = transmitter;
	        FileSlicer slicer = new FileSlicer(name, size, 100 * chunkSize, mainExec, helper, transmitter);
	        
	        slicer.slice();
	        slicer.slice();
	        
	        helperExecutor.execute(() -> {
	        	_transmitter.process(results);
	        });
	        
	        while (true) {
	        	AsyncCmdRes chunk = results.poll(30, TimeUnit.SECONDS).get();
	        	if (chunk.e != null) {
	        		if (chunk.e instanceof EOFException) {
	        			break;
	        		}
	        		throw chunk.e;
	        	}
	        	dataSock.write(getDecoder().decode(chunk.res.trim()));
	        }
	        
    	} catch (IOException | InterruptedException | TimeoutException e) {
    		throw e;
    	} finally {
    		dataSock.stopDataSock();
    		if (transmitter != null) {
    			transmitter.close();
    		}
    	}
    	fireResponse("226 Transfer complete.");
    }
    
    private void ftpSTOR(String cmd) throws Exception {
        String fileName=cmd.substring(5);
        ISSHCommandHelper helper = cmdHelper();
        String tmpFolder = getTmpFolder();
        mainExec.cmd(helper.mkdir(tmpFolder));
        fireResponse("150 Opening data connection.");
        dataSock.startDataSock(ctx.getDest());
        long readBytes = 0;
        mainExec.cmd(helper.rmrf(fileName));
        List<Future<AsyncCmdRes>> results = new LinkedList<>();
        List<Future<Object>> joinResults = new LinkedList<>();
        while(true) {
        	byte[] ibuff = dataSock.read(chunkSize);
        	if (ibuff == null) {
            	break;
            }
        	results.add(sshPool.cmdA(helper.storePiece(ibuff, tmpFolder)));
        	readBytes += ibuff.length;
        	if (readBytes > 100 * chunkSize) {
        		FutureTask<Object> joinFuture = waitAndJoinA(results, tmpFolder, fileName, helper);
        		helper = cmdHelper();
        		tmpFolder = getTmpFolder();
                mainExec.cmd(helper.mkdir(tmpFolder));
                results = new LinkedList<>();
                readBytes = 0;
                joinResults.add(joinFuture);
                helperExecutor.execute(() -> {
                	joinFuture.run();
                });
        	}
        }
        for (Future<Object> joinResult : joinResults) {
        	joinResult.get();
        }
        waitAndJoin(results, tmpFolder, fileName, helper);
        fireResponse("226 Transfer complete.");
    }
    
    private FutureTask<Object> waitAndJoinA(List<Future<AsyncCmdRes>> results, String tmpFolder, String fileName, ISSHCommandHelper helper) {
    	return new FutureTask<Object>((Callable<Object>) () -> {
			waitAndJoin(results, tmpFolder, fileName, helper);
			return null;
		});
    } 
    
    private void waitAndJoin(List<Future<AsyncCmdRes>> results, String tmpFolder, String fileName, ISSHCommandHelper helper) 
    		throws Exception {
    	for (Future<AsyncCmdRes> res : results) {
        	AsyncCmdRes _res = res.get();
        	if (_res.e != null) {
        		throw _res.e;
        	}
        }
		mainExec.cmd(helper.joinStoredPieces(tmpFolder, fileName));
        mainExec.cmd(helper.rmrf(tmpFolder));
    }
    
    private long fileSize(String name) throws IOException, InterruptedException, TimeoutException {
    	String out = mainExec.cmd(cmdHelper().size(name));
    	return out.isEmpty() ? 0 : Long.parseLong(out.trim()); 
    }
    
    private BlockingQueue<Future<AsyncCmdRes>> transferSlice(
    			String tmpFolder, 
    			BlockingQueue<Boolean> interruptingQueue,
    			Future<Object> futureToWait
    		) 
    		throws IOException, InterruptedException, TimeoutException {
    	final BlockingQueue<Future<AsyncCmdRes>> results = new LinkedBlockingQueue<>();
    	helperExecutor.execute(() -> {
    		Future<AsyncCmdRes> res = null;
    		try {
	        	if (futureToWait != null) {
	        		futureToWait.get();
	        	}
	        	ISSHCommandHelper helper = getCmdHelperFactory().getHelper();
	        	final String files = mainExec.cmd(helper.lsw1(tmpFolder));
	            
	        	for (String file : files.split("\n")) {
					if (interruptingQueue.size() != 0 && interruptingQueue.peek()) {
						break;
					}
					res = sshPool.cmdA(helper.retrPiece(tmpFolder + "/" + file));
					results.put(res);
	        	}
	        	AsyncCmdRes cmdRes = new AsyncCmdRes(new EOFException(), null);
				results.put(ConcurrentUtils.constantFuture(cmdRes));
    		} catch (Throwable e) {
				res = ConcurrentUtils.constantFuture(new AsyncCmdRes(new Exception(e), null));
				try { results.put(res); } catch (InterruptedException e1) {}
			}
        	
        });
        return results;
    }
}

