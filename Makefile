# Copyright 2016 Yahoo Inc.
# Licensed under the terms of the 3-Clause BSD license. See LICENSE file in the project root for details.
UNAME_S := $(shell uname -s)



ifeq ($(UNAME_S),Darwin)
# this is from /usr/libexec/java_home -v 1.8
#JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_72.jdk/Contents/Home
JAVA_HOME=`/usr/libexec/java_home -v 1.8`
JAVA_OS=darwin
EXT=dylib
CC=CLANG++
endif

ifeq ($(UNAME_S),Linux)
JAVA_HOME=/usr/java/default
JAVA_OS=linux
LINUX_ADD=amd64/
EXT=so
CC=gcc
endif

# include rules that can change the locations
-include custom.rules

JAVA_LIBRARY_PATH=$(JAVA_HOME)/jre/lib/$(LINUX_ADD)server/
JAVA_INCLUDES=-I$(JAVA_HOME)/include/ -I$(JAVA_HOME)/include/$(JAVA_OS)/ -L$(JAVA_LIBRARY_PATH)

CXXFLAGS=-I$(INCLUDE)  -g -O0  -shared -fPIC
LFLAGS = -lstdc++ -Wall -lpthread 

#all: 
#	g++ -I$(INCLUDE) jni_JniContextAccess.cpp -g -O0  -shared -fPIC -o libtest.$(EXT)

LIB_OBJS=PointerHelper.o jni_JniContextAccess.o 
LIBNAME=libtest.$(EXT)

$(LIBNAME): $(LIB_OBJS)
	$(CC) $(LFLAGS) $(LIB_OBJS) -shared -o $@

%.o: %.cpp
	@echo $< 
	$(CC) $(CXXFLAGS) -c $< -o $@

clean:
	-rm *.o *.$(EXT)