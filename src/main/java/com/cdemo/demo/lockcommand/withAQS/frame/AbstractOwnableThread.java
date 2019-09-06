package com.cdemo.demo.lockcommand.withAQS.frame;

public abstract class AbstractOwnableThread {

    private Thread exclusiveOwnableThread;

    protected Thread getOwnableThread() {
        return exclusiveOwnableThread;
    }

    protected void setOwnableThread(Thread exclusiveOwnableThread) {
        this.exclusiveOwnableThread = exclusiveOwnableThread;
    }
}
