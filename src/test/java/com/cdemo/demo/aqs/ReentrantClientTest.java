package com.cdemo.demo.aqs;

import com.cdemo.demo.lockcommand.withAQS.LockClient;
import com.cdemo.demo.lockcommand.withAQS.client.ReentrantClient;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ReentrantClientTest {

    @Test
    public void test(){
        LockClient lock = new ReentrantClient();
        lock.lock();
        lock.unLock();
        lock.lock();
    }
}
