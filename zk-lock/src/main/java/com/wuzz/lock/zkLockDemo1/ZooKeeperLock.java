package com.wuzz.lock.zkLockDemo1;

/**
 * 基于zk实现分布式锁,思想：根据zk节点来判断是否是最小节点，最小节点则获得锁，处理完玩释放锁
 * @author wuzongzhao
 * @date 2020/9/14 11:23
 */
public interface ZooKeeperLock {

    /**
     * 获得锁
     * @param guidNodeName 加锁的唯一节点名
     * @param clientGuid 标识客户端唯一id
     * @return
     */
    boolean getLock(String guidNodeName, String clientGuid);


    /**
     * 释放锁
     * @param guidNodeName 锁的唯一节点名
     * @param clientGuid 标识客户端唯一id
     * @return
     */
    boolean releaseLock(String guidNodeName, String clientGuid);

    /**
     * 判断锁是否存在
     * @param guidNodeName
     * @return
     */
    boolean existsLock(String guidNodeName);
}
