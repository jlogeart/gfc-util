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

  def messages(t: Throwable): Seq[String] = {
    @tailrec def messages(t: Throwable, acc1: Set[Throwable], acc2: List[String]): Seq[String] = {
      val msgs = Option(t.getMessage).fold(acc2)(acc2 ::)
      val cause = Option(t.getCause).filterNot(acc1.contains)
      if (cause.isDefined) {
        messages(cause.get, acc1 + t, msgs)
      } else {
        msgs
      }
    }
    messages(t, Set.empty, Nil).reverse.distinct
  }
}
