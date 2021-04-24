package redis.lock.redlock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RedLock {

    private static final Logger log = LoggerFactory.getLogger(RedLock.class);

    private final String[] servers;
    private final List<Jedis> instances = new ArrayList<>();

    private final String redLockReleaseLuaScript = "if redis.call(\"get\",KEYS[1]) == ARGV[1] then " +
            "return redis.call(\"del\",KEYS[1]) " +
            "else " +
            "return 0 " +
            "end";

    private final int quorum;

    private final double clockDriftFactor = 0.01;

    private final int retryDelay = 200;
    private final int retryCount = 3;

    public RedLock() {
        this("127.0.0.1");
    }

    public RedLock(String host) {
        this(host, 6379);
    }

    public RedLock(String host, int port) {
        this(host, port, "");
    }

    public RedLock(String host, int port, String password) {
        this(host, port, password, 0);
    }

    public RedLock(String host, int port, int database) {
        this(host, port, "", database);
    }

    public RedLock(String host, int port, String password, int database) {
        if (password != null && password.trim().length() != 0)
            this.servers = new String[]{"redis://:" + password + "@" + host + ":" + port + "/" + database};
        else
            this.servers = new String[]{"redis://" + host + ":" + port + "/" + database};

        this.quorum = 1;
    }

    public RedLock(String[] servers) {
        this.servers = servers;

        this.quorum = Math.min(servers.length, servers.length / 2 + 1);
    }

    private synchronized void init() {
        if (this.instances.size() == 0) {
            try {
                for (String server : servers) {
                    this.instances.add(new Jedis(new URI(server)));
                }
            } catch (URISyntaxException e) {
                log.error("RedLock - Error initializing Jedis / JedisCluster client. Exception = {}", e.getMessage());
                this.instances.clear();
            }
        }
    }

    /**
     * Try to acquire a lock for this resource
     * @param resource Resource
     * @return LockResult containing resource, value, validityTime if lock is acquired successfully or else null
     */
    public LockResult lock(String resource) {
        return lock(resource, 60000);
    }

    /**
     * Try to acquire a lock for this resource with specified ttl
     * @param resource Resource
     * @return LockResult containing resource, value, validityTime if lock is acquired successfully or else null
     */
    public LockResult lock(String resource, int ttl) {
        return lock0(resource, ttl);
    }

    private synchronized LockResult lock0(String resource, int ttl) {
        init();
        if (instances.size() == 0) {
            log.error("RedLock - Error. Jedis / JedisCluster client not initialized");
            return null;
        }

        String value = getRandomUUID().toString();
        int retry = this.retryCount;

        do {
            int n = 0;

            long start = System.currentTimeMillis();

            for (Jedis jedis : instances) {
                String reply = jedis.set(resource, value, new SetParams().nx().px(ttl));
                if (reply != null && reply.equals("OK")) {
                    n++;
                }
            }

            double drift = (ttl * clockDriftFactor) + 2;
            double validityTime = ttl - (System.currentTimeMillis() - start) - drift;

            if (n >= quorum && validityTime > 0) {
                return new LockResult(resource, value, validityTime);
            } else {
                release0(resource, value);
            }

            int randomDelay = (int) ((Math.random() * (retryDelay - retryDelay / 2)) + retryDelay / 2);
            try {
                Thread.sleep(randomDelay);
            } catch (InterruptedException e) {
                log.warn("RedLock - Warning. Sleep of {} ms interrupted in between", randomDelay);
            }

            retry--;
        } while (retry > 0);

        return null;
    }

    private synchronized UUID getRandomUUID() {
        return UUID.randomUUID();
    }

    /**
     * Release the lock for this resource from all instances
     * @param resource Resource
     * @param value Value
     */
    public void release(String resource, String value) {
        release0(resource, value);
    }

    private synchronized void release0(String resource, String value) {
        init();
        if (instances.size() == 0) {
            log.error("RedLock - Error. Jedis / JedisCluster client not initialized");
            return;
        }

        for (Jedis jedis : instances) {
            jedis.eval(redLockReleaseLuaScript, 1, resource, value);
        }
    }

}