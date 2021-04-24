# redlock-java
RedLock in Java

Implementation based on [Redlock-rb](https://github.com/antirez/redlock-rb)

This Java library implements the Redis-based distributed lock manager algorithm [described in this blog post](http://antirez.com/news/77).

Include the dependency into your project:

```java
<dependency>
  <groupId>io.github.s-sathish</groupId>
  <artifactId>redlock-java</artifactId>
  <version>1.0.1</version>
</dependency>
```

To create a lock manager:

```java
RedLock redLock = new RedLock();
```

To acquire a lock:

```java
String lockResult = redLock.lock("redlock");
```

To release a lock:

```java
List<Object> releaseLockResult = redLock.release("redlock", lockResult);
```