/*
 * Copyright 2004-2008 H2 Group. Licensed under the H2 License, Version 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.test;

import java.sql.SQLException;
import java.util.Properties;

import org.h2.server.TcpServer;
import org.h2.store.fs.FileSystemDisk;
import org.h2.test.db.TestAutoRecompile;
import org.h2.test.db.TestBackup;
import org.h2.test.db.TestBigDb;
import org.h2.test.db.TestBigResult;
import org.h2.test.db.TestCases;
import org.h2.test.db.TestCheckpoint;
import org.h2.test.db.TestCluster;
import org.h2.test.db.TestCompatibility;
import org.h2.test.db.TestCsv;
import org.h2.test.db.TestExclusive;
import org.h2.test.db.TestFullText;
import org.h2.test.db.TestFunctions;
import org.h2.test.db.TestIndex;
import org.h2.test.db.TestLinkedTable;
import org.h2.test.db.TestListener;
import org.h2.test.db.TestLob;
import org.h2.test.db.TestLogFile;
import org.h2.test.db.TestMemoryUsage;
import org.h2.test.db.TestMultiConn;
import org.h2.test.db.TestMultiDimension;
import org.h2.test.db.TestMultiThread;
import org.h2.test.db.TestOpenClose;
import org.h2.test.db.TestOptimizations;
import org.h2.test.db.TestPowerOff;
import org.h2.test.db.TestReadOnly;
import org.h2.test.db.TestRights;
import org.h2.test.db.TestRunscript;
import org.h2.test.db.TestSQLInjection;
import org.h2.test.db.TestScript;
import org.h2.test.db.TestScriptSimple;
import org.h2.test.db.TestSequence;
import org.h2.test.db.TestSessionsLocks;
import org.h2.test.db.TestSpaceReuse;
import org.h2.test.db.TestSpeed;
import org.h2.test.db.TestTempTables;
import org.h2.test.db.TestTransaction;
import org.h2.test.db.TestTriggersConstraints;
import org.h2.test.db.TestTwoPhaseCommit;
import org.h2.test.db.TestView;
import org.h2.test.jdbc.TestBatchUpdates;
import org.h2.test.jdbc.TestCallableStatement;
import org.h2.test.jdbc.TestCancel;
import org.h2.test.jdbc.TestDatabaseEventListener;
import org.h2.test.jdbc.TestManyJdbcObjects;
import org.h2.test.jdbc.TestMetaData;
import org.h2.test.jdbc.TestNativeSQL;
import org.h2.test.jdbc.TestPreparedStatement;
import org.h2.test.jdbc.TestResultSet;
import org.h2.test.jdbc.TestStatement;
import org.h2.test.jdbc.TestTransactionIsolation;
import org.h2.test.jdbc.TestUpdatableResultSet;
import org.h2.test.jdbc.TestZloty;
import org.h2.test.jdbcx.TestDataSource;
import org.h2.test.jdbcx.TestXA;
import org.h2.test.jdbcx.TestXASimple;
import org.h2.test.mvcc.TestMvcc1;
import org.h2.test.mvcc.TestMvcc2;
import org.h2.test.server.TestNestedLoop;
import org.h2.test.server.TestPgServer;
import org.h2.test.server.TestWeb;
import org.h2.test.synth.TestBtreeIndex;
import org.h2.test.synth.TestKillRestart;
import org.h2.test.synth.TestCrashAPI;
import org.h2.test.synth.TestHaltApp;
import org.h2.test.synth.TestJoin;
import org.h2.test.synth.TestKill;
import org.h2.test.synth.TestRandomSQL;
import org.h2.test.synth.TestTimer;
import org.h2.test.synth.sql.TestSynth;
import org.h2.test.synth.thread.TestMulti;
import org.h2.test.unit.TestBitField;
import org.h2.test.unit.TestCache;
import org.h2.test.unit.TestCompress;
import org.h2.test.unit.TestDataPage;
import org.h2.test.unit.TestDate;
import org.h2.test.unit.TestExit;
import org.h2.test.unit.TestFile;
import org.h2.test.unit.TestFileLock;
import org.h2.test.unit.TestFileSystem;
import org.h2.test.unit.TestFtp;
import org.h2.test.unit.TestIntArray;
import org.h2.test.unit.TestIntIntHashMap;
import org.h2.test.unit.TestMultiThreadedKernel;
import org.h2.test.unit.TestOverflow;
import org.h2.test.unit.TestPattern;
import org.h2.test.unit.TestReader;
import org.h2.test.unit.TestSampleApps;
import org.h2.test.unit.TestScriptReader;
import org.h2.test.unit.TestSecurity;
import org.h2.test.unit.TestStreams;
import org.h2.test.unit.TestStringCache;
import org.h2.test.unit.TestStringUtils;
import org.h2.test.unit.TestTools;
import org.h2.test.unit.TestValue;
import org.h2.test.unit.TestValueHashMap;
import org.h2.test.unit.TestValueMemory;
import org.h2.tools.DeleteDbFiles;
import org.h2.tools.Server;
import org.h2.util.StringUtils;

/**
 * The main test application. JUnit is not used because loops are easier to write in
 * regular java applications (most tests are ran multiple times using different settings).
 */
public class TestAll {

/*

Random test:

cd bin
del *.db
start cmd /k "java -cp .;%H2DRIVERS% org.h2.test.TestAll join >testJoin.txt"
start cmd /k "java -cp . org.h2.test.TestAll synth >testSynth.txt"
start cmd /k "java -cp . org.h2.test.TestAll all >testAll.txt"
start cmd /k "java -cp . org.h2.test.TestAll random >testRandom.txt"
start cmd /k "java -cp . org.h2.test.TestAll btree >testBtree.txt"
start cmd /k "java -cp . org.h2.test.TestAll halt >testHalt.txt"
java -cp . org.h2.test.TestAll crash >testCrash.txt

java org.h2.test.TestAll timer

*/

    public boolean smallLog, big, networked, memory, ssl, textStorage, diskUndo, diskResult, deleteIndex, traceSystemOut;
    public boolean codeCoverage, mvcc, endless;
    public int logMode = 1, traceLevelFile, throttle;
    public String cipher;

    public boolean traceTest, stopOnError;
    public boolean jdk14 = true;

    private Server server;
    public boolean cache2Q;

    public static void main(String[] args) throws Exception {
        long time = System.currentTimeMillis();
        TestAll test = new TestAll();
        test.printSystem();

/*

implement max_query_time and use it for TestCrashAPI

link from h2 console to sourceError
add sourceError as an official link
Adjust cache memory usage
At startup, when corrupted, say if LOG=0 was used before
Extend tests that simulate power off
Automate real power off tests
timer test
// test with garbage at the end of the log file (must be consistently detected as such)
// TestRandomSQL is too random; most statements fails
// extend the random join test that compared the result against PostgreSQL
// long running test with the same database
// repeatable test with a very big database (making backups of the database files)
Test Recovery with MAX_LOG_FILE_SIZE=1; test with various log file sizes

ODBC must include a password; I cannot use the default 'sa' with no password for it.
Test with many columns (180), create index
add a 'kill process while altering tables' test case

Roadmap:


History:


*/

        if (args.length > 0) {
            if ("crash".equals(args[0])) {
                test.endless = true;
                new TestCrashAPI().runTest(test);
            } else if ("synth".equals(args[0])) {
                new TestSynth().runTest(test);
            } else if ("kill".equals(args[0])) {
                new TestKill().runTest(test);
            } else if ("random".equals(args[0])) {
                test.endless = true;
                new TestRandomSQL().runTest(test);
            } else if ("join".equals(args[0])) {
                new TestJoin().runTest(test);
            } else if ("btree".equals(args[0])) {
                new TestBtreeIndex().runTest(test);
            } else if ("all".equals(args[0])) {
                test.testEverything();
            } else if ("codeCoverage".equals(args[0])) {
                test.codeCoverage = true;
                test.runTests();
            } else if ("multiThread".equals(args[0])) {
                new TestMulti().runTest(test);
            } else if ("halt".equals(args[0])) {
                new TestHaltApp().runTest(test);
            } else if ("timer".equals(args[0])) {
                new TestTimer().runTest(test);
            }
        } else {
            test.runTests();
        }
        System.out.println("done (" + (System.currentTimeMillis() - time) + " ms)");
    }

    /**
     * Run all tests in all possible combinations.
     */
    private void testEverything() throws Exception {
        for (int c = 0; c < 3; c++) {
            if (c == 0) {
                cipher = null;
            } else if (c == 1) {
                cipher = "XTEA";
            } else {
                cipher = "AES";
            }
            for (int a = 0; a < 256; a++) {
                smallLog = (a & 1) != 0;
                big = (a & 2) != 0;
                networked = (a & 4) != 0;
                memory = (a & 8) != 0;
                ssl = (a & 16) != 0;
                textStorage = (a & 32) != 0;
                diskResult = (a & 64) != 0;
                deleteIndex = (a & 128) != 0;
                for (logMode = 0; logMode < 3; logMode++) {
                    traceLevelFile = logMode;
                    test();
                }
            }
        }
    }

    /**
     * Run the tests with a number of different settings.
     */
    private void runTests() throws Exception {

        smallLog = big = networked = memory = ssl = textStorage = diskResult = deleteIndex = traceSystemOut = diskUndo = false;
        traceLevelFile = throttle = 0;
        logMode = 1;
        cipher = null;
        test();

        smallLog = big = networked = memory = ssl = textStorage = diskResult = deleteIndex = traceSystemOut = false;
        traceLevelFile = throttle = 0;
        logMode = 1;
        cipher = null;
        mvcc = false;
        cache2Q = false;
        test();

        diskUndo = false;
        smallLog = false;
        big = false;
        networked = true;
        memory = true;
        ssl = false;
        textStorage = true;
        diskResult = deleteIndex = traceSystemOut = false;
        traceLevelFile = throttle = 0;
        logMode = 1;
        cipher = null;
        mvcc = false;
        cache2Q = false;
        test();

        big = false;
        smallLog = false;
        networked = false;
        memory = false;
        ssl = false;
        textStorage = false;
        diskResult = false;
        deleteIndex = false;
        traceSystemOut = false;
        logMode = 2;
        traceLevelFile = 0;
        throttle = 0;
        cipher = null;
        mvcc = false;
        cache2Q = false;
        test();

        diskUndo = true;
        smallLog = false;
        big = networked = memory = ssl = false;
        textStorage = true;
        diskResult = true;
        deleteIndex = true;
        traceSystemOut = false;
        logMode = 1;
        traceLevelFile = 3;
        throttle = 1;
        cipher = "XTEA";
        mvcc = false;
        cache2Q = false;
        test();

        diskUndo = false;
        big = true;
        smallLog = false;
        networked = false;
        memory = false;
        ssl = false;
        textStorage = false;
        diskResult = false;
        deleteIndex = false;
        traceSystemOut = false;
        logMode = 1;
        traceLevelFile = 1;
        throttle = 0;
        cipher = null;
        mvcc = false;
        cache2Q = false;
        test();

        big = true;
        smallLog = true;
        networked = true;
        memory = false;
        ssl = true;
        textStorage = false;
        diskResult = false;
        deleteIndex = false;
        traceSystemOut = false;
        logMode = 2;
        traceLevelFile = 2;
        throttle = 0;
        cipher = null;
        mvcc = false;
        cache2Q = true;
        test();

        big = true;
        smallLog = false;
        networked = true;
        memory = false;
        ssl = false;
        textStorage = false;
        diskResult = false;
        deleteIndex = false;
        traceSystemOut = false;
        logMode = 0;
        traceLevelFile = 0;
        throttle = 0;
        cipher = "AES";
        mvcc = false;
        cache2Q = false;
        test();

        smallLog = big = networked = memory = ssl = textStorage = diskResult = deleteIndex = traceSystemOut = false;
        traceLevelFile = throttle = 0;
        logMode = 1;
        cipher = null;
        mvcc = true;
        cache2Q = false;
        test();

        memory = true;
        test();
    }

    /**
     * Run all tests with the current settings.
     */
    private void test() throws Exception {
        System.out.println();
        System.out.println("Test big:"+big+" net:"+networked+" cipher:"+cipher+" memory:"+memory+" log:"+logMode+" diskResult:"+diskResult + " mvcc:" + mvcc);
        beforeTest();

        // db
        new TestScriptSimple().runTest(this);
        new TestScript().runTest(this);
        new TestAutoRecompile().runTest(this);
        new TestBackup().runTest(this);
        new TestBigDb().runTest(this);
        new TestBigResult().runTest(this);
        new TestCases().runTest(this);
        new TestCheckpoint().runTest(this);
        new TestCluster().runTest(this);
        new TestCompatibility().runTest(this);
        new TestCsv().runTest(this);
        new TestExclusive().runTest(this);
        new TestFullText().runTest(this);
        new TestFunctions().runTest(this);
        new TestIndex().runTest(this);
        new TestLinkedTable().runTest(this);
        new TestListener().runTest(this);
        new TestLob().runTest(this);
        new TestLogFile().runTest(this);
        new TestMemoryUsage().runTest(this);
        new TestMultiConn().runTest(this);
        new TestMultiDimension().runTest(this);
        new TestMultiThread().runTest(this);
        new TestOpenClose().runTest(this);
        new TestOptimizations().runTest(this);
        new TestPowerOff().runTest(this);
        new TestReadOnly().runTest(this);
        new TestRights().runTest(this);
        new TestRunscript().runTest(this);
        new TestSQLInjection().runTest(this);
        new TestSessionsLocks().runTest(this);
        new TestSequence().runTest(this);
        new TestSpaceReuse().runTest(this);
        new TestSpeed().runTest(this);
        new TestTempTables().runTest(this);
        new TestTransaction().runTest(this);
        new TestTriggersConstraints().runTest(this);
        new TestTwoPhaseCommit().runTest(this);
        new TestView().runTest(this);

        // jdbc
        new TestBatchUpdates().runTest(this);
        new TestCallableStatement().runTest(this);
        new TestCancel().runTest(this);
        new TestDatabaseEventListener().runTest(this);
        new TestManyJdbcObjects().runTest(this);
        new TestMetaData().runTest(this);
        new TestNativeSQL().runTest(this);
        new TestPreparedStatement().runTest(this);
        new TestResultSet().runTest(this);
        new TestStatement().runTest(this);
        new TestTransactionIsolation().runTest(this);
        new TestUpdatableResultSet().runTest(this);
        new TestZloty().runTest(this);

        // jdbcx
        new TestDataSource().runTest(this);
        new TestXA().runTest(this);
        new TestXASimple().runTest(this);

        // server
        new TestNestedLoop().runTest(this);
        new TestWeb().runTest(this);
        new TestPgServer().runTest(this);

        // mvcc
        new TestMvcc1().runTest(this);
        new TestMvcc2().runTest(this);

        // synth
        new TestCrashAPI().runTest(this);
        new TestRandomSQL().runTest(this);
        new TestKillRestart().runTest(this);

        // unit
        new TestBitField().runTest(this);
        new TestCache().runTest(this);
        new TestCompress().runTest(this);
        new TestDataPage().runTest(this);
        new TestDate().runTest(this);
        new TestExit().runTest(this);
        new TestFile().runTest(this);
        new TestFileLock().runTest(this);
        new TestFtp().runTest(this);
        new TestFileSystem().runTest(this);
        new TestIntArray().runTest(this);
        new TestIntIntHashMap().runTest(this);
        new TestMultiThreadedKernel().runTest(this);
        new TestOverflow().runTest(this);
        new TestPattern().runTest(this);
        new TestReader().runTest(this);
        new TestSampleApps().runTest(this);
        new TestScriptReader().runTest(this);
        new TestSecurity().runTest(this);
        new TestStreams().runTest(this);
        new TestStringCache().runTest(this);
        new TestStringUtils().runTest(this);
        new TestTools().runTest(this);
        new TestValue().runTest(this);
        new TestValueHashMap().runTest(this);
        new TestValueMemory().runTest(this);

        afterTest();
    }

    public void beforeTest() throws SQLException {
        DeleteDbFiles.execute(TestBase.baseDir, null, true);
        FileSystemDisk.getInstance().deleteRecursive("trace.db");
        if (networked) {
            TcpServer.logInternalErrors = true;
            String[] args = ssl ? new String[] { "-tcpSSL", "true" } : new String[0];
            server = Server.createTcpServer(args);
            try {
                server.start();
            } catch (SQLException e) {
                System.out.println("FAIL: can not start server (may already be running)");
                server = null;
            }
        }
    }

    public void afterTest() throws SQLException {
        FileSystemDisk.getInstance().deleteRecursive("trace.db");
        if (networked && server != null) {
            server.stop();
        }
        DeleteDbFiles.execute(TestBase.baseDir, null, true);
    }

    private void printSystem() {
        Properties prop = System.getProperties();
        System.out.println("Java: " +
                prop.getProperty("java.runtime.version") + ", " +
                prop.getProperty("java.vm.name")+", " +
                prop.getProperty("java.vendor"));
        System.out.println("Env: " +
                prop.getProperty("os.name") + ", " +
                prop.getProperty("os.arch")+", "+
                prop.getProperty("os.version")+", "+
                prop.getProperty("sun.os.patch.level")+", "+
                prop.getProperty("file.separator")+" "+
                prop.getProperty("path.separator")+" "+
                StringUtils.javaEncode(prop.getProperty("line.separator")) + " " +
                prop.getProperty("user.country") + " " +
                prop.getProperty("user.language") + " " +
                prop.getProperty("user.variant")+" "+
                prop.getProperty("file.encoding"));
    }
}