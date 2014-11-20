package com.gilt.gfc.util

import com.gilt.gfc.logging.Loggable

import scala.concurrent.ExecutionContext

/**
 * For small code blocks that don't need to be run on a separate thread.
 */
object SameThreadExecutionContext extends ExecutionContext with Loggable {
  override def execute(runnable: Runnable): Unit = runnable.run
  override def reportFailure(t: Throwable): Unit = error(t.getMessage, t)
}
