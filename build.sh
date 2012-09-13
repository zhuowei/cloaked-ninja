#!/bin/bash
rm -r bin
rm qptool.jar
mkdir bin
javac -d bin net/zhuoweizhang/qptool/Main.java
cd bin
jar -cvfe ../qptool.jar net.zhuoweizhang.qptool.Main net
cd ..
