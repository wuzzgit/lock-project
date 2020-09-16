package com.wuzz.lock.zkLockDemo1;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.common.StringUtils;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 基于子节点实现分布式锁
 *
 * @author wuzongzhao
 * @date 2020/9/14 16:44
 */
public abstract class ChildrenNodeLock extends ZooKeeperBase implements ZooKeeperLock {

    private Logger log;

    /**
     * 用于加锁的唯一节点名
     */
    protected String guidNodeName;
    /**
     * 子节点的前缀
     */
    protected String childPrefix = "element";

    /**
     * 用于记录所创建子节点的路径
     */
    protected String elementNodeName;

    /**
     * 用于记录所创建子节点的完整路径
     */
    protected String elementNodeFullName;

    public ChildrenNodeLock(String address) throws IOException {
        super(address);
        log = LoggerFactory.getLogger(getClass().getName());
    }


    /**
     * 获取排序好的子节点列表
     *
     * @param path
     * @param watch
     * @return
     * @throws KeeperException
     * @throws InterruptedException
     */
    final public List<String> getOrderedChildren(String path, boolean watch)
            throws KeeperException, InterruptedException {
        List<String> children = getZooKeeper().getChildren(path, watch);
        Collections.sort(children, new StringCompare());
        return children;
    }

    /**
     * 在日志中输出子节点
     *
     * @param path
     * @param children
     */
    final public void traceOrderedChildren(String path, List<String> children) {
        if (log.isTraceEnabled()) {
            log.trace("children : {}", StringUtils.joinStrings(children, ","));
        }
    }

    /**
     * 获取当前节点的前一个节点，如果为空表示自己是第一个
     * 不为空返回
     *
     * @return
     * @throws KeeperException
     * @throws InterruptedException
     */
    protected String getPreElementName() throws KeeperException, InterruptedException {
        List<String> elementNames = getOrderedChildren(this.guidNodeName, false);
        traceOrderedChildren(this.guidNodeName, elementNames);

        String preElementName = null;
        for (String oneElementName : elementNames) {
            //判断节点名是否按照子节点的完整路径结尾
            if (this.elementNodeFullName.endsWith(oneElementName)) {
                //已到当前节点
                break;
            }
            preElementName = oneElementName;
        }
        return preElementName;
    }

    /**
     * 在日志中输出子节点
     *
     * @param path
     * @param children
     */
    final public void infoOrderedChildren(String path, List<String> children) {
        if (log.isInfoEnabled()) {
            log.info("children : {}", StringUtils.joinStrings(children, ","));
        }
    }

    /**
     * 子节点名称比较，取最后10位进行比较
     */
    private class StringCompare implements Comparator<String> {
        @Override
        public int compare(String string1, String string2) {
            return string1.substring(string1.length() - 10)
                    .compareTo(string2.substring(string2.length() - 10));
        }
    }

    /**
     * 尝试获得锁
     *
     * @param guidNodeName 加锁的唯一节点名
     * @param clientGuid   标识客户端唯一id
     * @return
     */
    @Override
    public boolean getLock(String guidNodeName, String clientGuid) {
        boolean result = false;
        this.guidNodeName = guidNodeName;
        //确保根节点存在，并且创建为容器节点
        super.createRootNode(this.guidNodeName, CreateMode.CONTAINER);
        try {
            String fullNodeName = this.guidNodeName + "/" + this.childPrefix;
            byte[] nodeValue = clientGuid == null ? new byte[0] : clientGuid.getBytes();
            elementNodeFullName=getZooKeeper().create(fullNodeName,nodeValue,
                    ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.EPHEMERAL_SEQUENTIAL);
            elementNodeName=elementNodeFullName.substring(guidNodeName.length()+1);
            log.trace("{} 尝试获取锁", elementNodeName);
            //是否加锁成功
            boolean lockSuccess=isLockSuccess();
            result=lockSuccess;
        } catch (KeeperException e) {
            log.info("获取分布式锁失败", e);
        } catch (InterruptedException e) {
            log.error("InterruptedException", e);
        }
        return result;
    }

    /**
     * 释放锁
     * @param guidNodeName 锁的唯一节点名
     * @param clientGuid 标识客户端唯一id
     * @return
     */
    @Override
    public boolean releaseLock(String guidNodeName, String clientGuid) {
        boolean result = true;
        try {
            // 删除子节点
            getZooKeeper().delete(elementNodeFullName, 0);
        } catch (KeeperException e) {
            result = false;
            log.error("KeeperException", e);
        } catch (InterruptedException e) {
            result = false;
            log.error("InterruptedException", e);
        }
        return result;
    }

    /**
     * 判断锁是否存在
     * @param guidNodeName
     * @return
     */
    @Override
    public boolean existsLock(String guidNodeName) {
        boolean exists = false;
        Stat stat = new Stat();
        try {
            getZooKeeper().getData(guidNodeName, false, stat);
            if (stat.getNumChildren() > 0) {
                exists = true;
            }
        } catch (KeeperException.NoNodeException e) {
            exists = false;
        } catch (KeeperException e) {
            log.error("KeeperException", e);
        } catch (InterruptedException e) {
            log.error("InterruptedException", e);
        }
        return exists;
    }
    /**
     * 是否加锁成功
     *
     * @return
     * @throws KeeperException
     * @throws InterruptedException
     */
    protected abstract boolean isLockSuccess() throws KeeperException, InterruptedException;
}
