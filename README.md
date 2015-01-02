# gfc-util

A library that contains a few scala utility classes. Part of the gilt foundation classes.

## Example Usage

### com.gilt.gfc.util.ExponentialBackoff

Allows periodic retry of a potentially failing function with an exponentially growing wait period:

    class SomeResource extends ExponentialBackoff {
      // Grow backoff exponentially from 1ms until it hits 1s, then remain at that rate,
      // i.e. backoff will be: 1ms, 2ms, 4ms, 8ms, 16ms, 32ms, 64ms, 128ms, 256ms, 512ms, 1s, 1s, ...
      override val backoffMaxTimeMs = 1000L

      private def maybeFailing: Long = {
        val time = System.currentTimeMillis
        if(time % 2 == 0) time else throw new RuntimeException("bang!")
      }

      /**
       * Retry (potentially infinitely) until the function succeeds
       */
      def getValue: String = retry(maybeFailing).toString

      /**
       * Retry up to maxAttempts for the function to succeeds
       */
      def getValue(maxAttempts: Long): String = retryUpTo(maxAttempts)(maybeFailing).toString
    }

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
