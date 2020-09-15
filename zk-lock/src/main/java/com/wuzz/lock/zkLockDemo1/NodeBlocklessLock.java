package com.wuzz.lock.zkLockDemo1;


import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.security.KeyException;

/**
 * 分布式基于节点实现非阻塞锁,思想：判断节点是否存在，加锁添加节点，释放锁删除节点
 * 获取不到锁就快速返回，不等待锁的释放
 * @author wuzongzhao
 * @date 2020/9/14 11:30
 */
public class NodeBlocklessLock extends ZooKeeperBase implements ZooKeeperLock {

    /** 用于加锁的唯一节点名 */
    String guidNodeName;
    /** 用于唯一标识当前客户端的ID */
    String clientGuid;

    public NodeBlocklessLock(String address) throws IOException {
        super(address);
    }


    /**
     * 获得锁
     * @param guidNodeName 加锁的唯一节点名
     * @param clientGuid 标识客户端唯一id
     * @return true代表成功,false代表失败
     */
    @Override
    public boolean getLock(String guidNodeName, String clientGuid) {
       boolean result=false;
       this.guidNodeName=guidNodeName;
       this.clientGuid=clientGuid;
       try {
           //判断节点是否存在
           if(getZooKeeper().exists(guidNodeName,false)==null) {
               //创建节点
               getZooKeeper().create(guidNodeName, clientGuid.getBytes(),
                       ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
               byte[] data = getZooKeeper().getData(guidNodeName, false, null);
               //判断节点名是否一致
               if (data != null && clientGuid.equals(new String(data))) {
                   result = true;
               } else {
                   logger.info("创建node成功，但值不是自己添加的，理论上不应该出现这种情况");
               }
           }
       }catch (KeeperException.NodeExistsException e){
           // 节点已经存在，说明自己失败，什么都不做，直接返回 false
       }catch (KeeperException e){
           logger.error("获取分布式锁失败",e);
       }catch (InterruptedException e){
           logger.error("InterruptedException",e);
       }
        return result;
    }

    /**
     * 释放锁（删除节点）
     * @param guidNodeName 锁的唯一节点名
     * @param clientGuid 标识客户端唯一id
     * @return true代表成功,false代表失败
     */
    @Override
    public boolean releaseLock(String guidNodeName, String clientGuid) {
        boolean result=false;
        Stat stat=new Stat();
        try {
            byte[] data=getZooKeeper().getData(guidNodeName,false,stat);
            if(data!=null && clientGuid.equals(new String(data))){
                getZooKeeper().delete(guidNodeName, stat.getVersion());
                result = true;
            }

        }catch (KeeperException e){
            logger.error("keeperException",e);
        }catch (InterruptedException e){
            logger.error("InterruptiedException",e);
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
        boolean result = false;
        try {
            Stat stat = getZooKeeper().exists(guidNodeName, false);
            result = stat != null;
        } catch (KeeperException e) {
            logger.error("KeeperException", e);
        } catch (InterruptedException e) {
            logger.error("InterruptedException", e);
        }
        return result;
    }
}
