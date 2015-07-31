# Copyright 2015 Yahoo Inc.
# Licensed under the terms of the 3-Clause BSD license. See LICENSE file in the project root for details.
UNAME_S := $(shell uname -s)



ifeq ($(UNAME_S),Darwin)
INCLUDE=/System/Library/Frameworks/JavaVM.framework/Versions/Current/Headers/
EXT=dylib
CC=CLANG++
endif


###############################################################################
# Ylinux Support
###############################################################################
ifeq ($(UNAME_S),Linux)
INCLUDE=/usr/share/java/latest
EXT=so
CC=gcc
endif

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
	-rm *.o *.dylib *.so