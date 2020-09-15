package com.wuzz.lock.zkLockDemo1.queue;

import com.wuzz.lock.zkLockDemo1.ZooKeeperBase;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * zookeeper实现queue
 * @author wuzongzhao
 * @date 2020/9/15 15:48
 */
@Slf4j
public class ZookeeperQueue extends ZooKeeperBase {
    /** 队列名称 */
    private String queueName;

    /** 队列的同步信号 */
    private static Integer queueMutex = Integer.valueOf(1);

    public ZookeeperQueue(String address,String queueName) throws IOException {
        super(address);
        this.queueName=createRootNode(queueName);
    }

    @Override
    protected void processNodeChildrenChanged(WatchedEvent event) {
        synchronized (queueMutex){
            queueMutex.notify();
        }
    }

    /**
     * 将对象添加到队列中
     * @param i
     * @return
     * @throws KeeperException
     * @throws InterruptedException
     */
    private  boolean produce(int i) throws KeeperException,InterruptedException{
        ByteBuffer b = ByteBuffer.allocate(4);
        byte[] value;

        // Add child with value i
        b.putInt(i);
        value = b.array();
        String elementName = queueName + "/element";
        ArrayList<ACL> ids = ZooDefs.Ids.OPEN_ACL_UNSAFE;
        CreateMode createMode = CreateMode.PERSISTENT_SEQUENTIAL;
        getZooKeeper().create(elementName, value, ids, createMode);

        return true;
    }

    /**
     * 从队列中删除第一个对象
     *
     * @return
     * @throws KeeperException
     * @throws InterruptedException
     */
    private int consume() throws KeeperException,InterruptedException{
        while (true) {
            synchronized (queueMutex) {
                List<String> list = getZooKeeper().getChildren(queueName, true);
                if (list.size() == 0) {
                    queueMutex.wait();
                } else {
                    // 获取第一个子节点的名称
                    String firstNodeName = getFirstElementName(list);
                    // 删除节点，并返回节点的值
                    return deleteNodeAndReturnValue(firstNodeName);
                }
            }
        }
    }

    /**
     * 获取第一个子节点的名称
     *
     * @param list
     * @return
     */
    private String getFirstElementName(List<String> list) {
        Integer min = Integer.MAX_VALUE;
        String minNode = null;
        for (String s : list) {
            Integer tempValue = Integer.valueOf(s.substring(7));
            if (tempValue < min) {
                min = tempValue;
                minNode = s;
            }
        }
        return minNode;
    }


    /**
     * 删除节点，并返回节点的值
     *
     * @param minNode
     * @return
     * @throws KeeperException
     * @throws InterruptedException
     */
    private int deleteNodeAndReturnValue(String minNode)
            throws KeeperException, InterruptedException {
        String fullNodeName = queueName + "/" + minNode;
        Stat stat = new Stat();
        byte[] b = getZooKeeper().getData(fullNodeName, false, stat);
        getZooKeeper().delete(fullNodeName, stat.getVersion());
        ByteBuffer buffer = ByteBuffer.wrap(b);
        return buffer.getInt();
    }
}
