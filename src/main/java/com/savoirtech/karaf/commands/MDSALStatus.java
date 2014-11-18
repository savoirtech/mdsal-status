/*
 * MDSALStatus
 *
 * Copyright (c) 2014, Savoir Technologies, Inc., All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */

package com.savoirtech.karaf.commands;

import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import javax.management.ObjectName;
import javax.management.MBeanServer;
import javax.management.MBeanInfo;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.ReflectionException; 

import java.lang.management.ThreadInfo;
import java.lang.Integer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.util.LinkedList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Formatter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.AbstractAction;


@Command(scope = "mdsal", name = "status", description = "MDSAL Status Command")
public class MDSALStatus extends AbstractAction {

    private int DEFAULT_REFRESH_INTERVAL = 1000;
    private int DEFAULT_KEYBOARD_INTERVAL = 100;
    private ObjectName configRegistry =  null;
    private ObjectName commitExecutorStats = null;
    private ObjectName commitFutureExecutorStats = null;
    private ObjectName commitStats = null;
    private ObjectName imcds = null;
    private ObjectName imods = null;


    @Option(name = "-u", aliases = { "--updates" }, description = "Update interval in milliseconds", required = false, multiValued = false)
    private String updates;


    protected Object doExecute() throws Exception {
        if (updates != null) {
             DEFAULT_REFRESH_INTERVAL = Integer.parseInt(updates);
        } 
        try {
            configRegistry =  new ObjectName("org.opendaylight.controller:type=ConfigRegistry");
            commitExecutorStats = new ObjectName("org.opendaylight.controller:type=DOMDataBroker,name=CommitExecutorStats");
            commitFutureExecutorStats = new ObjectName("org.opendaylight.controller:type=DOMDataBroker,name=CommitFutureExecutorStats");
            commitStats  = new ObjectName("org.opendaylight.controller:type=DOMDataBroker,name=CommitStats");
            imcds = new ObjectName("org.opendaylight.controller:type=InMemoryConfigDataStore,name=notification-executor");
            imods = new ObjectName("org.opendaylight.controller:type=InMemoryOperationalDataStore,name=notification-executor");
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            MDSALStatus(server);
        } catch (IOException e) {
            //Ignore
        }
        return null;
    }

    private void MDSALStatus(MBeanServer server) throws InterruptedException, IOException, MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException {

        boolean run = true;

        // Continously update stats to console.
        while (run) {
            clearScreen();
            printControllerInfo(server);
            System.out.println("\u001B[36m==========================================================================================\u001B[0m");
            // Display notifications
            System.out.println("\u001B[36mDOMDataBroker Commit Stats:\u001B[0m");
            printCS(server);
            System.out.println("------------------------------------------------------------------------------------------");
            System.out.println("           ATC     CTC     CQS    CTPS     LQS    LTPS     MQS    MTPS     RTC     TTC");
            System.out.print("\u001B[36m   CE:\u001B[0m");
            printCES(server);
            System.out.print("\u001B[36m  CFE:\u001B[0m");
            printCFES(server);
            System.out.print("\u001B[36mIMCDS:\u001B[0m");
            printIMCDS(server);
            System.out.print("\u001B[36mIMODS:\u001B[0m");
            printIMODS(server);
            System.out.printf(" Note: MDSAL stats updated at  %d ms intervals", DEFAULT_REFRESH_INTERVAL);
            System.out.println();
            System.out.println(" To exit MDSAL Status: q");
            System.out.println();
            System.out.println("\u001B[36m==========================================================================================\u001B[0m");

            run = waitOnKeyboard();
        }
    }

    private void printControllerInfo(MBeanServer server) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException {
        Long version = (Long) server.getAttribute(configRegistry, "Version");
        boolean health = (Boolean) server.getAttribute(configRegistry, "Healthy");

        System.out.printf("MDSAL Controller Status:: ConfigRegistry Version: %d Healthy: %b  %n", version, health);
    } 

    private void printCS(MBeanServer server) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException {
        String AverageCommitTime  = (String) server.getAttribute(commitStats, "AverageCommitTime");
        String LongestCommitTime  = (String) server.getAttribute(commitStats, "LongestCommitTime");
        String ShortestCommitTime = (String) server.getAttribute(commitStats, "ShortestCommitTime");
        Long TotalCommits         = (Long) server.getAttribute(commitStats, "TotalCommits");
 
        System.out.printf("Average Commit Time: \u001B[36m%s\u001B[0m Total Commits: \u001B[36m%d\u001B[0m%n", AverageCommitTime,  TotalCommits);
        System.out.printf("Longest: \u001B[36m%s\u001B[0m Shortest: \u001B[36m%s\u001B[0m%n", LongestCommitTime, ShortestCommitTime);
    }

    private void printTabularData(MBeanServer server, ObjectName obj) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException {
        Long ActiveThreadCount     = (Long) server.getAttribute(obj, "ActiveThreadCount");
        Long CompletedTaskCount    = (Long) server.getAttribute(obj, "CompletedTaskCount");
        Long CurrentQueueSize      = (Long) server.getAttribute(obj, "CurrentQueueSize");
        Long CurrentThreadPoolSize = (Long) server.getAttribute(obj, "CurrentThreadPoolSize");
        Long LargestQueueSize      = (Long) server.getAttribute(obj, "LargestQueueSize");
        Long LargestThreadPoolSize = (Long) server.getAttribute(obj, "LargestThreadPoolSize");
        Long MaxQueueSize          = (Long) server.getAttribute(obj, "MaxQueueSize");
        Long MaxThreadPoolSize     = (Long) server.getAttribute(obj, "MaxThreadPoolSize");
        Long RejectedTaskCount     = (Long) server.getAttribute(obj, "RejectedTaskCount");
        Long TotalTaskCount        = (Long) server.getAttribute(obj, "TotalTaskCount");
        System.out.printf(" %7d %7d %7d %7d %7d %7d %7d %7d %7d %7d%n", ActiveThreadCount, CompletedTaskCount, CurrentQueueSize,
                                                                        CurrentThreadPoolSize, LargestQueueSize, LargestThreadPoolSize, MaxQueueSize,
                                                                        MaxThreadPoolSize, RejectedTaskCount, TotalTaskCount);
    }

    private void printCES(MBeanServer server) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException {
        printTabularData(server, commitExecutorStats);
    }

    private void printCFES(MBeanServer server) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException {
        printTabularData(server, commitFutureExecutorStats);
    }

    private void printIMCDS(MBeanServer server) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException {
        printTabularData(server, imcds);
    }

    private void printIMODS(MBeanServer server) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException {
        printTabularData(server, imods);
    }

    private boolean waitOnKeyboard() throws InterruptedException {
        InputStreamReader reader = new InputStreamReader(session.getKeyboard());
        for (int i = 0; i < DEFAULT_REFRESH_INTERVAL / DEFAULT_KEYBOARD_INTERVAL; i++) {
            Thread.sleep(DEFAULT_KEYBOARD_INTERVAL);
            try {
                if (reader.ready()) {
                    int value = reader.read();
                    switch (value) {
                        case 'q':
                            return false;
                        // Add more cases here for more interactive status display
                    }
                }
            } catch (IOException e) {

            }
        }

        return true;
    }

    private void clearScreen() {
        System.out.print("\33[2J");
        System.out.flush();
        System.out.print("\33[1;1H");
        System.out.flush();
    }

}
