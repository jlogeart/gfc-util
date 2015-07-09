# gfc-util [![Build Status](https://travis-ci.org/gilt/gfc-util.svg?branch=master)](https://travis-ci.org/gilt/gfc-util) [![Coverage Status](https://coveralls.io/repos/gilt/gfc-util/badge.svg?branch=master&service=github)](https://coveralls.io/github/gilt/gfc-util?branch=master)

A library that contains a few scala utility classes. Part of the gilt foundation classes.

## Example Usage


### com.gilt.gfc.util.Retry

Allows a retry of a potentially failing function with or without an exponentially growing wait period:
```
    import scala.concurrent.duration._
    import com.gilt.gfc.concurrent.ScalaFutures._
    def remoteCall: Future[Response] = ???
    // Retry the remote call up to 10 times until it succeeds
    val response: Future[Response] = retry(10)(remoteCall)
```
```
    import scala.concurrent.duration._
    import com.gilt.gfc.concurrent.ScalaFutures._
    def remoteCall: Future[Response] = ???
    // Retry the remote call up to 10 times until it succeeds, with an exponential backoff,
    // starting at 10 ms and doubling each iteration until it reaches 1 second, i.e.
    // 10ms, 20ms, 40ms, 80ms, 160ms, 320ms, 640ms, 1s, 1s, 1s
    val response: Future[Response] = retryWithExponentialDelay(maxRetryTimes = 10,
                                                               maxRetryTimeout = 5 minutes fromNow,
                                                               initialDelay = 10 millis,
                                                               maxDelay = 1 second,
                                                               exponentFactor = 2) {
      remoteCall
     }
```

### com.gilt.gfc.util.RateLimiter and com.gilt.gfc.util.ThreadSafeRateLimiter

RateLimiter can be used to rate-limit calls to a work function, e.g. a function that writes to a db.
ThreadSafeRateLimiter is a thread safe version of RateLimiter, that synchronizes calls to the limit function.


    val rateLimiter = new ThreadSafeRateLimiter(100) // Limit to 100 calls/second

    def writeObject(obj: DBObject) = rateLimiter.limit {
      db.insert(obj)
    }


### com.gilt.gfc.util.SingletonCache

A cache for objects that need their lifecycle to be managed and can't be just throw-away,
e.g. for when ConcurrentHashMap.putIfAbsent(new Something()) may result in a Something
instance that need to be closed if another thread's putIfAbsent was successful.


    val inputStreamCache = new SingletonCache[File]

    val file = new File("foo")

    val is: InputStream = inputStreamCache(file) {
      new FileInputStream(file)
    }

### com.gilt.gfc.util.Throwables

Utility to unwind nested Throwable stacks

    val e1 = new Exception()
    val e2 = new Exception(e1)
    val e3 = new Exception(e3)

    val t: Throwable = Throwables.rootCause(e3)

    t should be theSameInstanceAs e1

## License
Copyright 2014 Gilt Groupe, Inc.

Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
