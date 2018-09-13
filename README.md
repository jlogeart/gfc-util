# gfc-util [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.gilt/gfc-util_2.12/badge.svg?style=plastic)](https://maven-badges.herokuapp.com/maven-central/com.gilt/gfc-util_2.12) [![Build Status](https://travis-ci.org/gilt/gfc-util.svg?branch=master)](https://travis-ci.org/gilt/gfc-util) [![Coverage Status](https://coveralls.io/repos/gilt/gfc-util/badge.svg?branch=master&service=github)](https://coveralls.io/github/gilt/gfc-util?branch=master) [![Join the chat at https://gitter.im/gilt/gfc](https://badges.gitter.im/gilt/gfc.svg)](https://gitter.im/gilt/gfc?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

A library that contains a few scala utility classes. Part of the [Gilt Foundation Classes](https://github.com/gilt?q=gfc).

## Getting gfc-util

The latest version is 0.2.2, which is cross-built against Scala 2.10.x, 2.11.x and 2.12.x.

If you're using SBT, add the following line to your build file:

```scala
libraryDependencies += "com.gilt" %% "gfc-util" % "0.2.2"
```

For Maven and other build tools, you can visit [search.maven.org](http://search.maven.org/#search%7Cga%7C1%7Ccom.gilt%20gfc).
(This search will also list other available libraries from the gilt fundation classes.)

## Contents and Example Usage

### com.gilt.gfc.util.Retry

Allows a retry of a potentially failing function with or without an exponentially growing wait period:
```scala
// some potentially failing function
def inputNumber: Int = ???

// Retry the function up to 10 times or until it succeeds
val number: Int = retry(10)(inputNumber)
```
```scala
// some potentially failing function
def readFile: Seq[String] = ???

// Retry the function up to 10 times until it succeeds, with an exponential backoff,
// starting at 10 ms and doubling each iteration until it reaches 1 second, i.e.
// 10ms, 20ms, 40ms, 80ms, 160ms, 320ms, 640ms, 1s, 1s, 1s
val contents: Seq[String] = retryWithExponentialDelay(maxRetryTimes = 10,
                                                      maxRetryTimeout = 5 minutes fromNow,
                                                      initialDelay = 10 millis,
                                                      maxDelay = 1 second,
                                                      exponentFactor = 2)
                                                     (readFile)
```

Allows a retry of a function `I => O` via a function `I => Either[I, O]`:
```scala
val arr: Array[Boolean] = Array.fill(5)(false)

// Up to 10 times, set a random array index to 'true' and 
// return "success" when all array elements are 'true', or throw 'TooManyRetries'
Retry.retryFold(maxRetryTimes = 10)(arr){ a: Array[Boolean] =>
  if (a.forall(identity)) {
    Right("success")
  } else {
    a(scala.util.Random.nextInt(a.length)) = true
    Left(a)
  }
}
```
```scala
// Set a random array index to 'true' and return "success" when all array elements are 'true'.
// Retry this up to 10 times until it succeeds, with an exponential backoff,
// starting at 10 ms and doubling each iteration until it reaches 1 second, i.e.
// 10ms, 20ms, 40ms, 80ms, 160ms, 320ms, 640ms, 1s, 1s, 1s, or throw 'TooManyRetries'
def func(a: Array[Boolean]): Either[Array[Boolean], String] = {
  if (a.forall(identity)) {
    Right("success")
  } else {
    a(scala.util.Random.nextInt(a.length)) = true
    Left(a)
  }
}

val result: String = retryFoldWithExponentialDelay(maxRetryTimes = 10,
                                                   maxRetryTimeout = 5 minutes fromNow,
                                                   initialDelay = 10 millis,
                                                   maxDelay = 1 second,
                                                   exponentFactor = 2)
                                                  (arr)
                                                  (func)
```


### com.gilt.gfc.util.RateLimiter and com.gilt.gfc.util.ThreadSafeRateLimiter

RateLimiter can be used to rate-limit calls to a work function, e.g. a function that writes to a db.
ThreadSafeRateLimiter is a thread safe version of RateLimiter, that synchronizes calls to the limit function.
```scala
val rateLimiter = new ThreadSafeRateLimiter(100) // Limit to 100 calls/second

def writeObject(obj: DBObject) = rateLimiter.limit {
  db.insert(obj)
}
```

### com.gilt.gfc.util.SingletonCache

A cache for objects that need their lifecycle to be managed and can't be just throw-away,
e.g. for when ConcurrentHashMap.putIfAbsent(new Something()) may result in a Something
instance that need to be closed if another thread's putIfAbsent was successful.
```scala
val inputStreamCache = new SingletonCache[File]

val file = new File("foo")

val is: InputStream = inputStreamCache(file) {
  new FileInputStream(file)
}
```

### com.gilt.gfc.util.Throwables

Utility to unwind nested Throwable stacks
```scala
val e1 = new Exception()
val e2 = new Exception(e1)
val e3 = new Exception(e3)

val t: Throwable = Throwables.rootCause(e3)

t should be theSameInstanceAs e1
```

## License
Copyright 2018 Gilt Groupe, Inc.

Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
