package com.wuzz.lock.zkLockDemo1;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.profiler.Profiler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * 为zk测试代码创建一个基类，封装建立连接的过程
 *
 * @author wuzongzhao
 * @date 2020/9/14 15:05
 */
public class ZooKeeperBase implements Watcher {

    Logger logger = null;

    //等待连接建立成功的信号
    private CountDownLatch countDownLatch = new CountDownLatch(1);

    //zk客户端
    private ZooKeeper zooKeeper = null;

    //避免重复根节点
    static Integer rootNodeInitial = Integer.valueOf(1);
    //收到所有事件
    List<WatchedEvent> watchedEvents = new ArrayList<>();

    //构造函数
    public ZooKeeperBase(String address) throws IOException {
        logger = LoggerFactory.getLogger(getClass());

        Profiler profiler = new Profiler(this.getClass().getName() + " 连接到ZooKeeper");
        profiler.start("开始链接");
        zooKeeper = new ZooKeeper(address, 30000, this);
        try {
            profiler.start("等待连接成功的Event");
            countDownLatch.await();
        } catch (InterruptedException e) {
            logger.error("InterruptedException", e);
        }
        profiler.stop();
        profiler.setLogger(logger);
        profiler.log();
    }


    /**
     * 创建根节点
     *
     * @return
     */
    public String createRootNode(String rootNodeName) {
        CreateMode createMode = CreateMode.PERSISTENT;
        return createRootNode(rootNodeName, createMode);
    }

    public String createRootNode(String rootNodeName, CreateMode createMode) {
        synchronized (rootNodeInitial) {
            //创建tableSerial的zNode
            try {
                Stat existsStat = getZooKeeper().exists(rootNodeName, false);
                if (existsStat == null) {
                    rootNodeName = getZooKeeper().create(rootNodeName, new byte[0],
                            ZooDefs.Ids.OPEN_ACL_UNSAFE, createMode);
                }
            } catch (KeeperException e) {
                logger.info("创建节点失败，可能是其他客户端已经创建", e);
            } catch (InterruptedException e) {
                logger.error("InterruptedException", e);
            }
            return rootNodeName;
        }
    }

    //读取zk对象，供子类调用
    public ZooKeeper getZooKeeper() {
        return zooKeeper;
    }


    //监听节点变化
    @Override
    public void process(WatchedEvent watchedEvent) {
        if(Event.EventType.None.equals(watchedEvent.getType())){
            // 连接状态发生变化
            if (Event.KeeperState.SyncConnected.equals(watchedEvent.getState())) {
                // 连接建立成功
                countDownLatch.countDown();
            }
        }else {
            watchedEvents.add(watchedEvent);

            if (Event.EventType.NodeCreated.equals(watchedEvent.getType())) {
                processNodeCreated(watchedEvent);
            } else if (Event.EventType.NodeDeleted.equals(watchedEvent.getType())) {
                processNodeDeleted(watchedEvent);
            } else if (Event.EventType.NodeDataChanged.equals(watchedEvent.getType())) {
                processNodeDataChanged(watchedEvent);
            } else if (Event.EventType.NodeChildrenChanged.equals(watchedEvent.getType())) {
                processNodeChildrenChanged(watchedEvent);
            }
        }

    }

    /**
     * 处理事件: NodeCreated
     *
     * @param event
     */
    protected void processNodeCreated(WatchedEvent event) {}

    /**
     * 处理事件: NodeDeleted
     *
     * @param event
     */
    protected void processNodeDeleted(WatchedEvent event) {}

    /**
     * 处理事件: NodeDataChanged
     *
     * @param event
     */
    protected void processNodeDataChanged(WatchedEvent event) {}

    /**
     * 处理事件: NodeChildrenChanged
     *
     * @param event
     */
    protected void processNodeChildrenChanged(WatchedEvent event) {}

    /** 收到的所有 Event 列表 */
    public List<WatchedEvent> getWatchedEventList() {
        return watchedEvents;
    }

}
