package com.wuzz.lock.zkLockDemo1;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Random;

/**
 *  * ZooKeeper中基于子节点功能加锁<br>
 *  * 用于 阻塞锁 ： 加锁失败会等待其他客户端释放锁
 *  *
 * @author wuzongzhao
 * @date 2020/9/15 10:26
 */
@Slf4j
public class ChildrenBlockingLock extends ChildrenNodeLock {

    /** 前一个节点被删除的信号 */
    Integer mutex;

    public ChildrenBlockingLock(String address) throws IOException {
        super(address);
        Random random = new SecureRandom();
        mutex = Integer.valueOf(random.nextInt());
    }

    @Override
    protected void processNodeDeleted(WatchedEvent event) {
        synchronized (mutex) {
            // 节点被删除，通知重新检查锁情况
            String deletedNodeName = event.getPath().substring(guidNodeName.length() + 1);
            log.trace("{} 节点被删除，当前监控的是 {} ", deletedNodeName, elementNodeName);
            mutex.notify();
        }
    }

    @Override
    protected void processNodeChildrenChanged(WatchedEvent event) {
        synchronized (mutex) {
            // 子节点有变化，通知重新检查锁情况。有羊群效应
            log.trace("{} 子节点有变化", this.guidNodeName);
            mutex.notify();
        }
    }

    /**
     * 是否加锁成功
     *羊群效应: 羊群效应就是 一个特定的znode 改变的时候ZooKeper 触发了所有watches 的事件。
     * 举个例子，如果有1000个客户端watch 一个znode的exists调用，当这个节点被创建的时候，将会有1000个通知被发送。
     * 这种由于一个被watch的znode变化，导致大量的通知需要被发送，将会导致在这个通知期间的其他操作提交的延迟。
     * 因此，只要可能，我们都强烈建议不要这么使用watch。仅仅有很少的客户端同时去watch一个znode比较好，理想的情况是只有1个。
     * @return
     * @throws KeeperException
     * @throws InterruptedException
     */
    @Override
    protected boolean isLockSuccess() {
        // 是否监控子节点变化，会有羊群效应
        boolean monitorChildrenEvent = false;

        boolean lockSuccess;
        try {
            while (true) {
                String prevElementName = getPreElementName();
                if (prevElementName == null) {
                    log.trace("{} 没有更靠前的子节点，加锁成功", elementNodeName);
                    lockSuccess = true;
                    break;
                } else {
                    // 有更小的节点，说明当前节点没抢到锁，注册前一个节点的监听。
                    if (monitorChildrenEvent) {
                        log.trace("{} 监控 {} 子节点变化事件", elementNodeName, guidNodeName);
                        getZooKeeper().getChildren(this.guidNodeName, true);
                    } else {
                        log.trace("{} 监控 {} 的事件", elementNodeName, prevElementName);
                        getZooKeeper().exists(this.guidNodeName + "/" + prevElementName, true);
                    }
                    synchronized (mutex) {
                        // 最多一秒
                        mutex.wait(1000);
                        if (monitorChildrenEvent) {
                            log.trace("{} 监控的 {} 有子节点变化", elementNodeName, guidNodeName);
                        } else {
                            log.trace("{} 监控的 {} 被删除", elementNodeName, prevElementName);
                        }
                    }
                }
            }
        } catch (KeeperException e) {
            lockSuccess = false;
        } catch (InterruptedException e) {
            lockSuccess = false;
        }
        return lockSuccess;
    }

}
