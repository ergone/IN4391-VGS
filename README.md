# The Virtual Grid System (VGS) : <br> Distributed Simulation of Distributed Systems 

This software was developed and released by P.S. Tekieli and I.V. Ivanov as a part of IN4391 (Distributed Computing Systems (2015-2016 Q3)) course's final assignment. The learning objectives associated with this laboratory task were aimed to show students how complex the operations performed by cloud infrastructures are, and what tradeoffs are usually involved in the design of distrubted platforms' applications. In order to do so, the team was asked to create, implement and test a Virtual Grid Simulator that would demonstrate the feasibility of emuluted multi-cluster system, and reveal the most recent problems with providing decent levels of fault-tolerance, scalability and replication inside those structures. This particular application is capable of utlizing DAS-4 supercomputer resources for achieving the aforementioned goals and can automatically be deployed within this cluster with the use of included Testing Environment Controller (TEC).

<b>Disclaimer!</b> This application uses some external, publically-available libraries for providing auxiliary functionalities. We thank their authors for providing us with an opportuninty to use them for free and maintain this fashion by sharing our work with the same spirit. However, the re-use of such is condition by their individual licences, therefore each person interested in developing our project utilizing the same libraries, should refer to the terms stated there. 

TEC - Testing Environment Controller :
+ JSch - Java Secure Channel - JCraft,
+ Client (Wrapper) - Java OpenNebula Cloud API - OpenNebula,
+ Apache Webservices Common Utilities,
+ The Apache XML-RPC Client.

RMI Application
+ Apache Commons IO.

-------------------------------------------------------------------------------------------------------------------------------------
MIT License (excluding the aforementioned libraries)

Copyright (c) 2016 - P.S. Tekieli, V.I.Ivanov - https://github.com/ergone/IN4391-VGS

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
