package com.cdemo.demo.lockcommand.withAQS.frame;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.LockSupport;

public class AbstractSyncFrame extends AbstractOwnableThread{

    //exclusive state
    private volatile int state;

    //common head node
    private volatile Node head;

    //common tail node
    private volatile Node tail;

    public void setHead(Node node) {
        node.prev = null;
        this.head = node;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public Node getHead(){
        return head;
    }

    static class Node{

        private Node mode;

        //exclusive mode
        static final Node NODE_MODE_EXCLUSIVE = null;
        //share mode
        static final Node NODE_MODE_SHARE = new Node();

        //prev node
        volatile Node prev;
        //next node
        volatile Node next;

        //waiter status
        volatile int waiterStatus;

        //single state while be wake up next node
        static final int NODE_STATE_SINGLE = -1;
        //this node is not deal with it
        static final int NODE_STATE_CANCEL = 1;

        private Thread thread;

        Node(){};

        Node (Thread t,Node mode){
            this.mode = mode;
            this.thread = t;
        }

        public Node getPredecessor() {
            Node prde = prev;
            if(prde == null){
                throw new NullPointerException();
            }
            return prde;
        }
    }

    protected void acquire(int count){
        if(!tryAcquire(count) &&
                !tryGetSource(addWaiter(Node.NODE_MODE_EXCLUSIVE),count)){
            selfInteruput();
        }
    }

    protected boolean release(int count){
        //get prev state
        if(tryRelease(count)){
            Node temp = head;
            if(temp != null && temp.waiterStatus != 0){
                wakeUp(temp);
            }
        }
        return true;
    }

    private void wakeUp(Node node) {
        int ws = node.waiterStatus;
        if (ws < 0)
            compareAndSetWaitStatus(node, ws, 0);
        Node w = node.next;
        if(w == null || w.waiterStatus > 0){
            w = null;
            for (Node t = tail;t != null && t != node && t.waiterStatus > 0;t =  t.prev) {
                w = t;
            }
        }
        if(w != null){
            Thread thread = w.thread;
            LockSupport.unpark(thread);
        }
    }

    private void selfInteruput() {
        Thread.currentThread().interrupt();
    }

    private boolean tryGetSource(Node node, int count) {
        boolean isInterrupt = true;
        for (;;) {
            //get prev
            Node prev = node.getPredecessor();
        	if(prev == head && tryAcquire(count)){
                setHead(prev);
                prev.next = null;
                isInterrupt = false;
                return isInterrupt;
        	}
            //if compete failed
            if(ifShouldPark(node) && ifIsInterrupt(node)){
                isInterrupt = true;
                return isInterrupt;
            }
        }
    }

    private boolean ifIsInterrupt(Node node) {
        LockSupport.park();
        return Thread.currentThread().isInterrupted();
    }

    private boolean ifShouldPark(Node node) {
        Node prev = node.prev;
        int ws = prev.waiterStatus;
        if(ws == Node.NODE_STATE_SINGLE){
            return true;
        }else if(ws > 0){
            do{
                prev = prev.prev;
            }while (ws > 0);
            prev.next = node;
        }else{
            //set single
            ws = Node.NODE_STATE_SINGLE;
        }
        return false;
    }

    private Node addWaiter(Node mode) {
        Node node = new Node(Thread.currentThread(),mode);
        Node pred = tail;
        //fast
        if(pred != null){
            node.prev = pred;
            if(compareAndSetTail(tail,node)){
                pred.next = node;
                return node;
            }
        }
        enq(node);
        return node;
    }

    private Node enq(Node node) {
        for (;;) {
            Node pred = tail;
            if(pred == null){
                //if tail is null so init a new list
                if(compareAndSetHead(head,new Node())){
                    tail = head;
                }
            }else{
                node.prev = pred;
                if(compareAndSetTail(pred,node)){
                    pred.next = node;
                    return node;
                }
            }
        }
    }

    protected boolean tryAcquire(int count) {
        throw new UnsupportedOperationException();
    }

    protected boolean tryRelease(int count) {
        throw new UnsupportedOperationException();
    }

    private static final Unsafe unsafe;
    private static final long stateOffset;
    private static final long headOffset;
    private static final long tailOffset;
    private static final long waitStatusOffset;

    static {
        try {
            //init unsafe
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            unsafe = (Unsafe) theUnsafe.get(null);

            stateOffset = unsafe.objectFieldOffset
                    (AbstractQueuedSynchronizer.class.getDeclaredField("state"));
            headOffset = unsafe.objectFieldOffset
                    (AbstractQueuedSynchronizer.class.getDeclaredField("head"));
            tailOffset = unsafe.objectFieldOffset
                    (AbstractQueuedSynchronizer.class.getDeclaredField("tail"));
            waitStatusOffset = unsafe.objectFieldOffset
                    (Node.class.getDeclaredField("waiterStatus"));
        } catch (Exception ex) { System.out.println("init unsafe failed");throw new Error(ex); }
    }

    protected boolean compareAndSetState(int expct,int update){
        return this.unsafe.compareAndSwapInt(this,stateOffset,expct,update);
    }

    private boolean compareAndSetHead(Node head,Node update) {
        return this.unsafe.compareAndSwapObject(this,headOffset,head,update);
    }

    protected boolean compareAndSetTail(Node tail,Node update){
        return this.unsafe.compareAndSwapObject(this,tailOffset,tail,update);
    }

    private boolean compareAndSetWaitStatus(Node node, int expect, int update) {
        return unsafe.compareAndSwapInt(node, waitStatusOffset,
                expect, update);
    }
}