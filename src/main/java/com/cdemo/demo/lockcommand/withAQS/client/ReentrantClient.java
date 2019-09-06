package com.cdemo.demo.lockcommand.withAQS.client;

import com.cdemo.demo.lockcommand.withAQS.LockClient;
import com.cdemo.demo.lockcommand.withAQS.frame.AbstractSyncFrame;

public class ReentrantClient extends AbstractSyncFrame implements LockClient{

    @Override
    public void lock() {
        /*if(compareAndSetState(0,1)){
            return;
        }else{
            acquire(1);
        }*/
        acquire(1);
    }

    @Override
    public void unLock() {
        int stateCount = getState();
        release(1);
    }

    @Override
    public boolean tryAcquire(int count){
       return compareAndSetState(0,getState() + count);
    }

    @Override
    public boolean tryRelease(int count){
        return compareAndSetState(0,getState() - count);
    }
}
