Made by and (C) Apoorv Kumar, 2012
IIT Guwahati

Released under GPL-V3

#UDP FS Query Client Server

In this utility , the client can send a file name to server and the server has to look it up , extract the last modification date and other details , and then report back to the client.


###Ideas used 
----------
* I have standard ports both for client and server , known to each other.

* Each connection has a queue in which the requests are queued.

* After this the problem is reduced to CONSUMER-PRODUCER problem. Here the main thread is consumer of packets. While the connection thread is consumer.

* I have implemented the "waiting" of threads using a wait() call that is available to any object in Java.

* Threads are woken up using the notify_all() call that wakes up all threads waiting on an object (in this case the queue).

* To ensure that the queue is not edited by both consumer and producer at once , it uses a method of serialization called "sychronize()".

* Incase file is not found , the server marks *DONE* with *status* = 1. Marking an error.



###instructions to run the program
	compile the codes - 
	--------------------------------
	$ javac Server.java Client.java
	--------------------------------

	1. run the server
	--------------------------------
	$ java Server
	--------------------------------

	2. run the client
	--------------------------------
	$ java Client localhost this_file
	--------------------------------





###CLIENT OUTPUT IN THE IDEAL CASE 
	===========================================================================
	starting the client ... 
	sending req to localhost
	request sent ...
	 now waiting for reply ...
	a new connection id has been allocated: 3
	------------------------------------------------------ 
	the file was located ... 
	the last modification date of file is : Tue Jan 17 23:19:55 IST 2012
	------------------------------------------------------ 
	sending DONE_ACK to localhost
	===========================================================================


###SERVER OUTPUT IN THE IDEAL CASE 
	===========================================================================
	creating new connection for the client - /127.0.0.1 - id: 3
	Producing this_file for: 3
	ACK datagram sent
	Processing request from 3
	found modification date of : this_file - Tue Jan 17 23:19:55 IST 2012
	closing connection from client - /127.0.0.1
	===========================================================================




###Known Issues:


	IN VERY VERY RARE CASES *DONE* PACKET REACHES BEFORE *ACK*
	THE ACK PACKET IS DROPPED , WHILE THE CLIENT IS BUSY RECEIVING *DONE* . 
	IT ISN'T HANDLED IN THIS PROGRAM THOUGH ... THE CLIENT PROGRAM WAITS FOREVER. 
	ALSO NO *DONE_ACK* IS SENT FROM CLIENT SINCE IT IS JAMMED
	CLIENT OUTPUT IN THIS CASE WOULD BE SOMETHING LIKE
	===========================================================================
	starting the client ... 
	sending req to localhost
	request sent ...
	now waiting for reply ...
	------------------------------------------------------ 
	the file was located ... 
	the last modification date of file is : Tue Jan 17 23:19:55 IST 2012
	------------------------------------------------------ 
	__blinking cursor__
	===========================================================================
	THE RESULT IS RECEIVED CORRECTLY ... HOWEVER ... 
	PRESS CTRL+C TO END THE WAITING CLIENT. 
