package org.obapanel.jedis.countdownlatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.util.concurrent.TimeUnit;

/**
 * Jedis implemetation of a CountDownLatch
 *
 * It makes some threads, in one or many different processes, wait until its counter reaches zero.
 * Then, all threads can resume the execution.
 * All the CountDownLatch on different threads and processes must share the same name to synchronize.
 *
 * The first created CountDownLatch assings the intial count to the shared value
 *
 * In this implementation, a thread that is waiting checks periodically the value of the counter in redis (polling)
 *
 *
 */
public class JedisCountDownLatch {

    private static final Logger LOG = LoggerFactory.getLogger(JedisCountDownLatch.class);
    private static final Long LONG_NULL_VALUE = -1L;


    private final Jedis jedis;
    private final String name;
    private int waitTimeMilis = 150;


    /**
     * Creates a new shared CountDownLatch
     * @param jedis Jedis connection
     * @param name Shared name
     * @param count Initial count
     */
    public JedisCountDownLatch(Jedis jedis, String name, long count) {
        this.jedis = jedis;
        this.name = name;
        init(count);
    }


    /**
     * Sets the waiting time between queries on Redis while waiting
     * @param waitTimeMilis time to wait in miliseconds
     * @return this
     */
    public JedisCountDownLatch withWaitingTimeMilis(int waitTimeMilis){
        this.waitTimeMilis = waitTimeMilis;
        return this;
    }

    /**
     * Checks if count is more than zero and creates the shared value if doesn't exists
     * @param count Initial count
     */
    private void init(long count){
        if (count <= 0) {
            throw new IllegalArgumentException("initial count on countdownlatch must be always more than zero");
        }
        jedis.set(name, String.valueOf(count), new SetParams().nx());
    }

    /**
     * Wait until interrupted or shared value reaches zero
     * @throws InterruptedException if interrupted
     */
    public void await() throws InterruptedException {
        while(getCount() > 0) {
            Thread.sleep(waitTimeMilis);
        }
    }

    /**
     * Wait until interrupted or shared value reaches zero or time passes
     * @param timeout Maximim time to wait
     * @param unit wait time unit
     * @return true if counter has reached zero, false if maximum wait time reached
     * @throws InterruptedException if interrupted
     */
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        long timeStampToWait = System.currentTimeMillis() + unit.toMillis(timeout);
        boolean reachedZero = false;
        while(timeStampToWait > System.currentTimeMillis()) {
            reachedZero = getCount() == 0;
            if (reachedZero){
                break;
            }
            Thread.sleep(waitTimeMilis);
        }
        return reachedZero;
    }


    /**
     * Decreases by one unit the share value
     * @return the current value, after operation
     */
    public long countDown() {
        Long value = jedis.decr(name);
        LOG.debug("countDown name {} value {}", name, value);
        return value != null ? value : -1;
    }


    /**
     * Get the current shared value, or -1 if it doen't exists
     * @return current value
     */
    public long getCount() {
        String value = jedis.get(name);
        LOG.debug("getCount name {} value {}", name, value);
        if (value != null && !value.isEmpty()) {
            return Long.parseLong(value);
        } else {
            return LONG_NULL_VALUE;
        }
    }

    /**
     * CAUTION !!
     * THIS METHOD DELETES THE REMOTE VALUE DESTROYING THIS COUNTDOWNLATCH AND OHTERS
     * USE AT YOUR OWN RISK WHEN ALL POSSIBLE OPERATIONS ARE FINISHED
     */
    public void destroy(){
        jedis.del(name);
    }


}
