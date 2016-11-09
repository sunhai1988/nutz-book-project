package net.wendal.nutzbook.shiro.cache;

import java.util.HashMap;
import java.util.Map;

import org.apache.shiro.ShiroException;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.util.Destroyable;
import org.apache.shiro.util.Initializable;
import org.nutz.lang.random.R;

import redis.clients.jedis.JedisPool;

@SuppressWarnings({"unchecked", "rawtypes"})
public class LCacheManager implements CacheManager, Initializable, Destroyable {

    public static String PREFIX = "LCache:";
    
    protected String id = R.UU32();

    protected CacheManager level1;
    protected CacheManager level2;

    protected JedisPool pool;
    protected CachePubSub pubSub = new CachePubSub();
    protected Map<String, LCache> caches = new HashMap<>();

    protected static LCacheManager me;

    public static LCacheManager me() {
        return me;
    }

    public LCacheManager() {
        me = this;
    }

    public void setupJedisPool(JedisPool pool) {
        this.pool = pool;
        new Thread(() -> pool.getResource().psubscribe(pubSub, PREFIX + "*")).start();
    }

    public void depose() {
        pubSub.punsubscribe(PREFIX + "*");
    }

    @Override
    public void destroy() throws Exception {
        if (level2 != null && level2 instanceof Destroyable)
            ((Destroyable) level2).destroy();
        if (level1 != null && level1 instanceof Destroyable)
            ((Destroyable) level1).destroy();
    }

    @Override
    public void init() throws ShiroException {
        if (level2 != null && level2 instanceof Initializable)
            ((Initializable) level2).init();
        if (level1 != null && level1 instanceof Initializable)
            ((Initializable) level1).init();
    }

    @Override
    public <K, V> Cache<K, V> getCache(String name) throws CacheException {
        LCache<K, V> combo = caches.get(name);
        if (combo != null)
            return combo;
        combo = new LCache<>(name);
        if (level1 != null)
            combo.add(level1.getCache(name));
        if (level2 != null)
            combo.add(level2.getCache(name));
        caches.put(name, combo);
        return combo;
    }

    public void setLevel1(CacheManager level1) {
        this.level1 = level1;
    }

    public void setLevel2(CacheManager level2) {
        this.level2 = level2;
    }
}
