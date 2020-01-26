# FTP2SSH - FTP Over a Plain Console 
FTP2SSH is the utility to convert a plain bash access to file system to an FTP server. 
In case of scp/sftp/smb absence. For example in case of kubernetes access or suppressing of scp/sftp by some reasons.

## Getting started
1. Download dist directory to a local folder.
2. Create "configuration.json" file :
`{"spawn": "<command to spawn a bash console>", "exit": "<command to exit the bash console>", "host": "127.0.0.1"}`
3. Run `./start.sh` or `java -jar ftp2Ssh-0.0.1-SNAPSHOT.jar`
4. Open Filezilla to localhost:8888 with a random login/password
5. Use an ftp access.

## Prerequisites
For system where utility will be started:
* JAVA 1.8+

For system to access over a bash-like console (ssh/kubectl/etc.)
* base64
* split
* ls
* cat
* echo
* stat
* mkdir
* rm
* mv
* gzip (optional)

## File transfer process

During the execution of STOR command files are read chunk by chunk, then each chunk gets base64 encoded and stored in temporary folder as files. After the last chunk is stored, the command to split all the chunks to the destination file is executed. 

RERT command encodes the remote file, then splits it on chunks, and stores each chunk to the temporary folder. Then the utility reads each chunk and transfers it according to the ftp protocol.

In gzip mode utility compresses each chunk before base64 encoding.

## Configurations

### General configurations
| Parameter 		| Type    | Default / Example                                                    | Description |
| ----------------- | ------- | -------------------------------------------------------------------- | ----------- |
| port 				| int     | 8888                                                                 | Port to listen |
| host 				| String  | 0.0.0.0                                                              | Hostname to listen |
| spawn 			| String  |  sshpass -p {{PASS}} ssh {{USER}}@127.0.0.1 <br/>(Default: `bash`) | Command to spawn new console. Available parameters <br/>`{{USER}}` - login of the ftp user, <br/>`{{PASS}}` - password of the ftp user |
| chunkSize 		| int 	  | 10000                                                                | Amount of data in bytes to transfer at once. Should not be too big as it is written by `echo "..." >>...` by default. |
| tmpFolder 		| String  | /tmp                                                                 | Folder to store chunks of the file to transfer. Has to be writable for the spawned bash session. |
| timeoutSec 		| int     | 30                                                                   | Timeout in seconds for a command to execute or for a chunk to transfer. |
| maxLayers 		| int     | 10                                                                   | Maximum of spawned bash sessions for one ftp session. Minimum is 2. |
| gzip 				| boolean | false                                                                | Use gzip to archive data before sending it over a console. |

### Configurations to overwrite commands 
| Parameter 		| Type    | Default / Example                                                | Description |
| ----------------- | ------- | ---------------------------------------------------------------- | ----------- |
| prepareFileToRetr | String  | base64 -w {{CHUNK_SIZE}} {{FILE}} &#124; split -l 1 - {{TMP}}{{DS}} | Command to split a remote file to retrieve. The command should create a set of files in the temporary folder to retrieve in alphabetical order.  Each file has to be base64 encoded. <br/> `{{CHUNK_SIZE}}` - size of a chunk in bytes, <br/> `{{FILE}}` - file to retrieve <br/> `{{TMP}}` - temporary folder to split a file <br /> `{{DS}}` - directory separator
| retrPiece 		| String  | cat {{FILE}}                                                   | Command to print a chunk to stdout. <br/> `{{FILE}}` - the name of the file to print
| storePiece 		| String  | echo {{PIECE}} >>{{TMPFILE}}                                   | Command to store a chunk of data. <br/> `{{PIECE}}` - a base64 encoded chunk <br/> `{{TMPFILE}}` - name of the file to store
| joinStoredPieces 	| String  | base64 -d {{TMPFILE}} >>{{FILE}}                               | Base64 decode a chunk and store it to the file. <br /> `{{TMPFILE}}` - name of the file storing the chunk <br/> `{{FILE}}` - name of the destination file
| echo 				| String  | echo                                                           | Print something to stdout. Used as `echo "Some escaped text"`
| cd 				| String  | cd                                                             | Change directory. Used as `cd "/escaped/path"`
| pwd 				| String  | pwd                                                            | Print current directory.
| lsla 				| String  | ls -la --time-style="+%Y-%m-%dT%H:%M"                          | Enumerate files in directory. Used as `ls -la --time-style="+%Y-%m-%dT%H:%M" "/escaped/path"`.
| lsw1 				| String  | ls -w1                                                         | Print files in directory one per line. Used as `ls -w1 "/escaped/path"`.
| size 				| String  | stat {{FILE}} &#124; grep -oEh \"Size: [0-9]+\" &#124; cut -b 7- | Print file size in bytes. <br/> `{{FILE}}` - filename to get size
| mkdir 			| String  | mkdir                                                          | Create directory. Used as `mkdir "/path/to/directory"`.
| mv 				| String  | mv {{FROM}} {{TO}}                                             | Move file. <br/> `{{FROM}}` - filename to move. <br/> `{{TO}}` - destination path
| rmrf 				| String  | rm -rf                                                         | Remove a file or a directory recursively. Used as `rm -rf "/path/to/remove"`
| exit 				| String  | exit                                                           | Gracefully close console. 
| DS 				| String  | /                                                              | Directory Separator.

## GZIP compression
If gzip configuration is set to true, the utility assumes that chunks are gzipped before transfer. Chunks stored in the temporary folder during STOR will be gzipped and base64 encoded. As well as each chunk has to be gzipped and base64 encoded before RETR.

Default commands in gzip mode:

| Command           | Default |
| ----------------- | ------- |
| joinStoredPieces  | base64 -d {{TMPFILE}} &#124; gzip -d -c - >>{{FILE}} |
| prepareFileToRetr | split -b {{CHUNK_SIZE}} --additional-suffix .split {{FILE}} {{TMP}}{{DS}} && gzip {{TMP}}{{DS}}* && for l in &#96;ls {{TMP}}&#96;; <br/> do base64 {{TMP}}{{DS}}$l >{{TMP}}{{DS}}$l.b64; done && rm {{TMP}}{{DS}}*.split.gz |

## Examples
Ftp over bash to the local computer. Just for example.

	{
		"spawn": "bash",
		"host": "0.0.0.0",
		"exit": "exit"
	}

------
Ftp over ssh to remote computer. In case of scp/sftp is disabled somehow.

	{
		"spawn": "sshpass -p {{PASS}} ssh {{USER}}@127.0.0.1",
		"host": "0.0.0.0",
		"exit": "exit"
	}

-----
Bash over kubectl. How I use it.
Create file sh-pod.sh:

	#!/bin/bash
	l=`kubectl get pods | grep $1 | cut --delimiter=' ' -f1`; 
	kubectl exec $l -it sh

Add execution mode to the file: `chmod +x sh-pod.sh` 

Configuration file: 

	{
		"spawn": "./sh-pod.sh {{USER}}",
		"host": "0.0.0.0",
		"exit": "exit"
	}

Don't forget to start the utility from its directory or specify full path to the sh-pod file.

## TODO:
* Bash injections. Some injections like ``mkdir "$(rm -rf /)"` or `rm "`whoami`"`` are escaped, but steel needs some investigation.
* Windows support. Not tested yet.
* Better way to stor/retr files. Work with big files is impossible. 
