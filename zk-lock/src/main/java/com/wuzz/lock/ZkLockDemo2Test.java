package com.wuzz.lock;

import com.wuzz.lock.zkLockSimpleDemo2.AbstractLock;
import com.wuzz.lock.zkLockSimpleDemo2.HighPerformanceZkLock;

/**
 * 分布式锁实现2测试类
 * @author wuzongzhao
 * @date 2020/9/16 15:24
 */
public class ZkLockDemo2Test {

    public static void main(String[] args) {
        //模拟多个10个客户端
        for (int i=0;i<10;i++) {
            Thread thread = new Thread(new LockRunnable());
            thread.start();
        }

    }

    static class LockRunnable implements Runnable{

        @Override
        public void run() {
            AbstractLock zkLock = new HighPerformanceZkLock();
            zkLock.getLock();
            //模拟业务操作
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            zkLock.releaseLock();
        }

    }



}
