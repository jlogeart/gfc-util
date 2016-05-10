package com.gilt.gfc.util

import scala.annotation.tailrec

object Throwables {
  def unwind(t: Throwable): List[Throwable] = {
    @tailrec def seq(t: Throwable, acc: List[Throwable]): List[Throwable] = {
      val deduped = Option(t).filterNot(acc.contains)
      if (deduped.isDefined) {
        seq(t.getCause, t :: acc)
      } else {
        acc
      }
    }
    seq(t, Nil).reverse
  }

  def rootCause(t: Throwable): Throwable = {
    if (t == null) throw new NullPointerException else unwind(t).last
  }

  def messages(t: Throwable): List[String] = {
    unwind(t).map(_.getMessage).filterNot(_ == null).distinct
  }

  def isA[T <: Throwable](clazz: Class[T])(t: Throwable): Option[T] = {
    unwind(t).find(t => clazz.isAssignableFrom(t.getClass)).map(_.asInstanceOf[T])
  }

  def apply[X](pf: PartialFunction[Throwable, X]): PartialFunction[Throwable, X] = {
    new PartialFunction[Throwable, X] {
      override def isDefinedAt(t: Throwable): Boolean = unwind(t).exists(pf.isDefinedAt)
      override def apply(t: Throwable): X = unwind(t).collectFirst {
        case t if pf.isDefinedAt(t) => pf.apply(t)
      } match {
        case Some(x) => x
      }
    }
  }

  object Matcher {
    def apply[T <: Throwable](clz: Class[T]): { def unapply(t: Throwable): Option[T] } = {
      new {
        def unapply(t: Throwable): Option[T] = isA(clz)(t)
      }
    }
  }
}
