package ftp2Ssh.configuration;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class ConfigurationProvider {
	
	public static class Configuration {
		@JsonProperty(defaultValue = "10000")
		public int chunkSize = 10000;
		public String prepareFileToRetr = null;
		public String retrPiece = null;
		public String storePiece = null;
		public String joinStoredPieces = null;
		public String echo = null;
		public String cd = null;
		public String pwd = null;
		public String lsla = null;
		public String lsw1 = null;
		public String size = null;
		public String mkdir = null;
		public String rmrf = null;
		public String mv = null;
		public String exit = null;
		public String DS = null;
		public String tmpFolder = null;
		@JsonProperty(defaultValue = "30")
		public int timeoutSec = 30;
		@JsonProperty(defaultValue = "10")
		public int maxLayers = 10;
		@JsonProperty(defaultValue = "8888")
		public int port = 8888;
		@JsonProperty(defaultValue = "0.0.0.0")
		public String host = "0.0.0.0";
		@JsonProperty(defaultValue = "bash")
		public String spawn = "bash";
		@JsonProperty(defaultValue = "false")
		public boolean gzip = false;
		public Configuration() {
		}
	}
	
	private Configuration configuration;
	
	@PostConstruct
	public void createConfig() throws JsonParseException, JsonMappingException, IOException, URISyntaxException {
		ObjectMapper om = new ObjectMapper();
		String pathToConf = new File(ConfigurationProvider.class.getProtectionDomain().getCodeSource().getLocation()
			    .toURI()).getParent() + "/configuration.json";
		
		System.out.println("Searching for " + pathToConf);
		if (new File(pathToConf).exists()) {
			configuration = om.readValue(new File(pathToConf), Configuration.class);
			return;
		}
		configuration = new Configuration();
	}
	
	@Bean
	@Scope(BeanDefinition.SCOPE_PROTOTYPE)
	@Qualifier("chunkSize")
	public int getChunkSize() {
		return configuration.chunkSize;
	}

	@Bean
	@Scope(BeanDefinition.SCOPE_PROTOTYPE)
	@Qualifier("timeoutMSec")
	public int getTimeoutMSec() {
		return configuration.timeoutSec * 1000;
	}

	@Bean
	@Scope(BeanDefinition.SCOPE_PROTOTYPE)
	@Qualifier("prepareFileToRetr")
	public String getPrepareFileToRetr() {
		return configuration.prepareFileToRetr;
	}

	@Bean
	@Scope(BeanDefinition.SCOPE_PROTOTYPE)
	@Qualifier("retrPiece")
	public String getRetrPiece() {
		return configuration.retrPiece;
	}

	@Bean
	@Scope(BeanDefinition.SCOPE_PROTOTYPE)
	@Qualifier("storePiece")
	public String getStorePiece() {
		return configuration.storePiece;
	}

	@Bean
	@Scope(BeanDefinition.SCOPE_PROTOTYPE)
	@Qualifier("joinStoredPieces")
	public String getJoinStoredPieces() {
		return configuration.joinStoredPieces;
	}

	@Bean
	@Scope(BeanDefinition.SCOPE_PROTOTYPE)
	@Qualifier("echo")
	public String getEcho() {
		return configuration.echo;
	}

	@Bean
	@Scope(BeanDefinition.SCOPE_PROTOTYPE)
	@Qualifier("cd")
	public String getCd() {
		return configuration.cd;
	}

	@Bean
	@Scope(BeanDefinition.SCOPE_PROTOTYPE)
	@Qualifier("pwd")
	public String getPwd() {
		return configuration.pwd;
	}

	@Bean
	@Scope(BeanDefinition.SCOPE_PROTOTYPE)
	@Qualifier("lsla")
	public String getLsla() {
		return configuration.lsla;
	}

	@Bean
	@Scope(BeanDefinition.SCOPE_PROTOTYPE)
	@Qualifier("lsw1")
	public String getLsw1() {
		return configuration.lsw1;
	}

	@Bean
	@Scope(BeanDefinition.SCOPE_PROTOTYPE)
	@Qualifier("size")
	public String getSize() {
		return configuration.size;
	}

	@Bean
	@Scope(BeanDefinition.SCOPE_PROTOTYPE)
	@Qualifier("mkdir")
	public String getMkdir() {
		return configuration.mkdir;
	}

	@Bean
	@Scope(BeanDefinition.SCOPE_PROTOTYPE)
	@Qualifier("rmrf")
	public String getRmrf() {
		return configuration.rmrf;
	}

	@Bean
	@Scope(BeanDefinition.SCOPE_PROTOTYPE)
	@Qualifier("mv")
	public String getMv() {
		return configuration.mv;
	}

	@Bean
	@Scope(BeanDefinition.SCOPE_PROTOTYPE)
	@Qualifier("exit")
	public String getExit() {
		return configuration.exit;
	}

	@Bean
	@Scope(BeanDefinition.SCOPE_PROTOTYPE)
	@Qualifier("DS")
	public String getDS() {
		return configuration.DS;
	}

	@Bean
	@Scope(BeanDefinition.SCOPE_PROTOTYPE)
	@Qualifier("tmpFolder")
	public String getTmpFolder() {
		return configuration.tmpFolder;
	}
	
	@Bean
	@Scope(BeanDefinition.SCOPE_PROTOTYPE)
	@Qualifier("timeoutSec")
	public int getTimeoutSec() {
		return configuration.timeoutSec;
	}

	@Bean
	@Scope(BeanDefinition.SCOPE_PROTOTYPE)
	@Qualifier("maxLayers")
	public int getMaxLayers() {
		return configuration.maxLayers;
	}

	@Bean
	@Scope(BeanDefinition.SCOPE_PROTOTYPE)
	@Qualifier("port")
	public int getPort() {
		return configuration.port;
	}

	@Bean
	@Scope(BeanDefinition.SCOPE_PROTOTYPE)
	@Qualifier("host")
	public String getHost() {
		return configuration.host;
	}
	
	@Bean
	@Scope(BeanDefinition.SCOPE_PROTOTYPE)
	@Qualifier("spawn")
	public String getSpawn() {
		return configuration.spawn;
	}
	
	@Bean
	@Scope(BeanDefinition.SCOPE_PROTOTYPE)
	@Qualifier("gzip")
	public boolean getGzip() {
		return configuration.gzip;
	}

	//-------------------------------------------------------------
	
	public void setPrepareFileToRetr(String prepareFileToRetr) {
		configuration.prepareFileToRetr = prepareFileToRetr;
	}

	public void setRetrPiece(String retrPiece) {
		configuration.retrPiece = retrPiece;
	}

	public void setStorePiece(String storePiece) {
		configuration.storePiece = storePiece;
	}

	public void setJoinStoredPieces(String joinStoredPieces) {
		configuration.joinStoredPieces = joinStoredPieces;
	}

	public void setEcho(String echo) {
		configuration.echo = echo;
	}

	public void setCd(String cd) {
		configuration.cd = cd;
	}

	public void setPwd(String pwd) {
		configuration.pwd = pwd;
	}

	public void setLsla(String lsla) {
		configuration.lsla = lsla;
	}

	public void setLsw1(String lsw1) {
		configuration.lsw1 = lsw1;
	}

	public void setSize(String size) {
		configuration.size = size;
	}

	public void setMkdir(String mkdir) {
		configuration.mkdir = mkdir;
	}

	public void setRmrf(String rmrf) {
		configuration.rmrf = rmrf;
	}

	public void setMv(String mv) {
		configuration.mv = mv;
	}

	public void setExit(String exit) {
		configuration.exit = exit;
	}

	public void setDS(String dS) {
		configuration.DS = dS;
	}

	public void setTmpFolder(String tmpFolder) {
		configuration.tmpFolder = tmpFolder;
	}
	
	public void setChunkSize(int chunkSize) {
		configuration.chunkSize = chunkSize;
	}

	public void setTimeoutSec(int timeoutSec) {
		configuration.timeoutSec = timeoutSec;
	}
	
	public void setMaxLayers(int maxLayers) {
		configuration.maxLayers = maxLayers;
	}

	public void setPort(int port) {
		configuration.port = port;
	}

	public void setHost(String host) {
		configuration.host = host;
	}
	
	public void setSpawn(String spawn) {
		configuration.spawn = spawn;
	}
	
	public void setGzip(boolean gzip) {
		configuration.gzip = gzip;
	}

}
