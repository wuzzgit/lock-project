package com.wuzz.lock.zkLockDemo1.writeOrReadLock;

import com.wuzz.lock.zkLockDemo1.ChildrenBlockingLock;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * zookeeper 实现读锁
 * 读锁的前面没有任何写锁，就是获取锁成功
 *
 * @author wuzongzhao
 * @date 2020/9/15 15:41
 */
@Slf4j
public class ZkReadLock extends ChildrenBlockingLock {

    public static final String FLAg="r-lock-";

    public ZkReadLock(String address) throws IOException {
        super(address);
    }


}
