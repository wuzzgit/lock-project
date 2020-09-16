package com.wuzz.lock.zkLockDemo1;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;

/**
 * 用于模拟客户端
 * @author wuzongzhao
 * @date 2020/9/16 16:42
 */
@Slf4j
public class LockClientThread extends Thread {

    /**
     * 用于加锁的唯一节点名
     */
    private String guidNodeName;

    private String clientGuid;

    private  ZooKeeperLock zooKeeperLock;

    //计数器
    public static CountDownLatch successLockSemaphore = new CountDownLatch(1);

    public LockClientThread(String guidNodeName,String clientGuid,ZooKeeperLock zooKeeperLock){
              this.clientGuid=clientGuid;
              this.guidNodeName=guidNodeName;
              this.zooKeeperLock=zooKeeperLock;
    }

    @Override
    public void run() {
        log.info("{} lock() ...", clientGuid);
        boolean isLock=zooKeeperLock.getLock(guidNodeName,clientGuid);
        String threadName = Thread.currentThread().getName();
        if(isLock){
            log.info("{} lock() ...获得锁成功",threadName+"-"+clientGuid);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                log.error("InterruptedException", e);
            }
            zooKeeperLock.releaseLock(guidNodeName,clientGuid);
            successLockSemaphore.countDown();
        }else {
            log.info("{} lock() ...获得锁失败", threadName);
        }
    }
}
