package com.wuzz.lock;

import com.wuzz.lock.zkLockDemo1.ChildrenBlockingLock;
import com.wuzz.lock.zkLockDemo1.ChildrenNodeLock;
import com.wuzz.lock.zkLockDemo1.LockClientThread;
import com.wuzz.lock.zkLockSimpleDemo2.AbstractLock;
import com.wuzz.lock.zkLockSimpleDemo2.HighPerformanceZkLock;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * 基于zk实现分布式锁测试类 首选（比较好的）
 * Hello world!
 * 参考链接：https://www.cnblogs.com/codestory/p/11387116.html
 * https://www.cnblogs.com/jing99/p/11607094.html
 */
public class ZkLockTest {


    static String address="localhost:2181";

    public static void main(String[] args) throws IOException {

        String guidNodeName = "/MYLOCK-" + System.currentTimeMillis();
        LockClientThread.successLockSemaphore = new CountDownLatch(1);

        //模拟多个10个客户端
        for (int i = 0; i < 10; i++) {
            // ChildrenBlocklessLock nodeBlocklessLock = new ChildrenBlocklessLock(address);

            ChildrenBlockingLock childrenBlockingLock = new ChildrenBlockingLock(address);


            LockClientThread lockClientThread = new LockClientThread(guidNodeName, "thread-" + i, childrenBlockingLock);
            lockClientThread.start();
        }
    }

}
