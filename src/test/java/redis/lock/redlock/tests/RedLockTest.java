package redis.lock.redlock.tests;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import redis.clients.jedis.Jedis;
import redis.lock.redlock.LockResult;
import redis.lock.redlock.RedLock;

import static org.junit.Assert.assertNull;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RedLockTest {

    protected Jedis jedis;

    @Before
    public void setUp() {
        jedis = new Jedis("127.0.0.1", 6379, 500);
        jedis.connect();
    }

    @After
    public void tearDown() {
        jedis.disconnect();
    }

    @Test
    public void aLock() {
        RedLock redLock = new RedLock();
        redLock.lock("redlock");
        assertNull(redLock.lock("redlock"));
    }

    @Test
    public void bRelease() {
        RedLock redLock = new RedLock();
        LockResult lockResult = redLock.lock("redlock_release");
        redLock.release("redlock_release", lockResult.getValue());
        assertNull(jedis.get("redlock_release"));
    }

    @Test
    public void cLockInCluster() {
        RedLock redLock = new RedLock(new String[]{"redis://127.0.0.1:6379/0"});
        redLock.lock("redlock");
        assertNull(redLock.lock("redlock"));
    }

    @Test
    public void dReleaseInCluster() {
        RedLock redLock = new RedLock(new String[]{"redis://127.0.0.1:6379/0"});
        LockResult lockResult = redLock.lock("redlock_release");
        redLock.release("redlock_release", lockResult.getValue());
        assertNull(jedis.get("redlock_release"));
    }

}
