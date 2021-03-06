package org.obapanel.jedis.semaphore.functional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.obapanel.jedis.semaphore.JedisSemaphore;
import redis.clients.jedis.Jedis;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.obapanel.jedis.semaphore.functional.JedisTestFactory.functionalTestEnabled;


public class FunctionalSemaphoreNumberPermitsTest {


    private Jedis jedis;
    private String semaphoreName;


    @Before
    public void before() throws IOException {
        org.junit.Assume.assumeTrue(functionalTestEnabled());
        if (!functionalTestEnabled()) return;
        jedis = JedisTestFactory.createJedisClient();
        semaphoreName = "semaphore:" + this.getClass().getName() + ":" + System.currentTimeMillis();
    }

    @After
    public void after() throws IOException {
        if (!functionalTestEnabled()) return;
        if (jedis != null) {
            jedis.del(semaphoreName);
            jedis.close();
        }
    }



    @Test
    public void testNumOfPermits(){
        JedisSemaphore jedisSemaphore = new JedisSemaphore(jedis,semaphoreName,3);
        assertEquals(3, jedisSemaphore.availablePermits());
        assertFalse( jedisSemaphore.tryAcquire(5));
        assertEquals(3, jedisSemaphore.availablePermits());
        assertTrue( jedisSemaphore.tryAcquire(1));
        assertEquals(2, jedisSemaphore.availablePermits());
        assertTrue( jedisSemaphore.tryAcquire(2));
        assertEquals(0, jedisSemaphore.availablePermits());
        assertFalse( jedisSemaphore.tryAcquire(1));
        assertEquals(0, jedisSemaphore.availablePermits());
        jedisSemaphore.release(2);
        assertEquals(2, jedisSemaphore.availablePermits());
        assertTrue( jedisSemaphore.tryAcquire(1));
        assertEquals(1, jedisSemaphore.availablePermits());
        assertTrue( jedisSemaphore.tryAcquire());
        assertEquals(0, jedisSemaphore.availablePermits());
    }

}
