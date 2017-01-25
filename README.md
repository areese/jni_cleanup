JNI Cleanup Example
=======
 
Overview
-----------
This is a small chunk of code that provides an example for automatically cleaning up JNI Objects in Java without using a Finalizer.
It also includes "Leak" detection, where you can enable a flag to dump all of the places where someone created an Object and didn't close it.

JDK9 adds Cleaner.cleanable: http://download.java.net/java/jdk9/docs/api/java/lang/ref/Cleaner.Cleanable.html

FWIW, this is a terrible idea, and I've encountered lots of problems when running this under load.  If the free method grabs some sort of lock you can end with some really weird crashes.

You are far better off implementing Closeable, and just leaking the jni memory than fighting with the oddness I've seen by abusing this api.

Running
-----------

There are 2 sample tests that come the parameters to harness.TestContextLeaks are:
<close | leak>
<threads>
<loops to run>

close will simply close the context when it's done using it.
leak is the interesting one, it emulates the typical java user who never reads the docs and doesn't call close even after you yell at them and they've had many outages related to this. 

java -XX:+HeapDumpOnOutOfMemoryError -verbose:gc -XX:+UseG1GC -XX:+ParallelRefProcEnabled -Xmx512m  -cp target/classes/ -Djava.library.path=. harness.TestContextLeaks close 10 1000


Internals
-----------
This was all spawned by a discussion with Charlie Hunt who mentioned that finalizers could be avoided using sun.misc.Cleaner as the DirectByteBuffer class does.

It turns out this is not a trivial procedure.



References
-----------
http://docs.oracle.com/javase/7/docs/api/java/nio/ByteBuffer.html

https://www.linkedin.com/profile/view?id=1541345&authType=NAME_SEARCH&authToken=oafT&locale=en_US&srchid=80459361437146014650&srchindex=1&srchtotal=334&trk=vsrp_people_res_name&trkInfo=VSRPsearchId%3A80459361437146014650%2CVSRPtargetId%3A1541345%2CVSRPcmpt%3Aprimary%2CVSRPnm%3Atrue

