package com.wuzz.lock.zkLockSimpleDemo2;

import org.I0Itec.zkclient.IZkDataListener;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * 高性能分布式锁实现(简单的方式)
 * 参考链接：https://blog.csdn.net/hongtaolong/article/details/88898875
 * @author wuzongzhao
 * @date 2020/9/15 16:21
 */
public class HighPerformanceZkLock extends AbstractLock {

    private static final String PATH = "/my_lock";
    //当前节点路径
    private String currentPath;
    //前一个节点的路径
    private String beforePath;

    private CountDownLatch countDownLatch = null;

    public HighPerformanceZkLock() {
        //如果不存在这个节点，则创建持久节点
        if (!zkClient.exists(PATH)) {
            zkClient.createPersistent(PATH);
        }
    }

    public boolean Lock() {
        String threadName = Thread.currentThread().getName();
        if (tryLock()) {
            System.out.println(threadName+"-获取锁成功");
            return true;
        }else {
            System.out.println(threadName+"-获取锁失败，进行等待...");
            waitLock();
            //递归重新获取锁
            getLock();
            return false;
        }
    }

    /**
     * 释放锁
     */
    @Override
    public void releaseLock() {
        if (null != zkClient) {
            zkClient.delete(currentPath);
            zkClient.close();
        }

    }

    /**
     * 获取锁
     * @return
     */
    @Override
    public boolean tryLock() {
        //如果currentPath为空则为第一次尝试加锁，第一次加锁赋值currentPath
        if (null == currentPath || "".equals(currentPath)) {
            //在path下创建一个临时的顺序节点
            currentPath = zkClient.createEphemeralSequential(PATH+"/", "lock");
        }
        //获取所有的临时节点，并排序
        List<String> childrens = zkClient.getChildren(PATH);
        Collections.sort(childrens);
        if (currentPath.equals(PATH+"/"+childrens.get(0))) {
            return true;
        }else {//如果当前节点不是排名第一，则获取它前面的节点名称，并赋值给beforePath
            int pathLength = PATH.length();
            int wz = Collections.binarySearch(childrens, currentPath.substring(pathLength+1));
            beforePath = PATH+"/"+childrens.get(wz-1);
        }
        return false;
    }

    /**
     * 锁监听
     */
    @Override
    public void waitLock() {
        IZkDataListener lIZkDataListener = new IZkDataListener() {

            @Override
            public void handleDataDeleted(String dataPath) throws Exception {
                if (null != countDownLatch){
                    countDownLatch.countDown();
                }
            }

            @Override
            public void handleDataChange(String dataPath, Object data) throws Exception {

            }
        };
        //监听前一个节点的变化
        zkClient.subscribeDataChanges(beforePath, lIZkDataListener);
        if (zkClient.exists(beforePath)) {
            countDownLatch = new CountDownLatch(1);
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        zkClient.unsubscribeDataChanges(beforePath, lIZkDataListener);
    }

}
