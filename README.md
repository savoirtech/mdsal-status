MDSAL Status
===

OpenDaylight Helium command for displaying MDSAL status information.


Description:

 The status command displays key Config Registry, DOMDataBroker, and 
 InMemory DataStore metrics. 

Building from source:
===

To build, invoke:
 
 mvn install


To install in Helium, invoke from console:

 install -s mvn:com.savoirtech.karaf.commands/mdsal-status/<version>


To execute command on Helium, invoke:

 mdsal:status


To exit status: q or Control + C


Runtime Options:
===

 
 -u  --updates   information update interval in milliseconds.
