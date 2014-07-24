package com.gilt.gfc.util

import scala.annotation.tailrec

object Throwables {
  def rootCause(t: Throwable): Throwable = {
    @tailrec def rootCause(t: Throwable, acc: Set[Throwable]): Throwable = {
      val cause = Option(t.getCause).filterNot(acc.contains)
      if (cause.isDefined) {
        rootCause(cause.get, acc + t)
      } else {
        t
      }
    }
    rootCause(t, Set.empty)
  }
}
