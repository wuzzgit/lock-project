package com.wuzz.lock;

import com.wuzz.lock.zkLockDemo3.DistributedLock;
import org.apache.logging.log4j.core.config.plugins.util.ResolverUtil;

import java.util.concurrent.locks.Lock;

/**
 * 分布式锁实现方式3测试
 * 参考地址：https://www.cnblogs.com/jing99/p/11607094.html
 * @author wuzongzhao
 * @date 2020/9/18 9:51
 */
public class ZKLockDemo3Test {

    //100张票
    private Integer n = 100;
//    private Lock lock = new ReentrantLock();

    public void printInfo() {
        System.out.println(Thread.currentThread().getName() +
                "正在运行,剩余余票:" + --n);
    }

    public class TicketThread implements Runnable {
        public void run() {
           //集群模式下： Lock lock = new DistributedLock("127.0.0.1:2181,****:2181", "zk");
            Lock lock = new DistributedLock("127.0.0.1:2181", "zk");
            lock.lock();
            try {
                if (n > 0) {
                    printInfo();
                }
            }finally{
                lock.unlock();
            }
        }
    }

    public void ticketStart() {
        TicketThread thread = new TicketThread();
        for (int i = 0; i < 30; i++) {
            Thread t = new Thread(thread, "mem" + i);
            t.start();
        }
    }

    public static void main(String[] args) {
        new ZKLockDemo3Test().ticketStart();
    }
}
