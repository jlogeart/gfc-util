package com.gilt.gfc.util

/**
 * Church boolean types for use in type-level programming
 *
 * Inspired by:
 * https://apocalisp.wordpress.com/2010/06/08/type-level-programming-in-scala/
 * https://en.wikipedia.org/wiki/Church_encoding#Church_Booleans
 * http://downloads.typesafe.com/website/presentations/ScalaDaysSF2015/T4_Barnes_Typelevel_Prog.pdf
 *
 * @author Gregor Heine
 * @since 20/Aug/2015 22:50
 */
package object types {
  sealed trait TBool {
    type If[T <: TBool, F <: TBool] <: TBool
  }

  trait TTrue extends TBool {
    type If[T <: TBool, F <: TBool] = T
  }

  trait TFalse extends TBool {
    type If[T <: TBool, F <: TBool] = F
  }

  type && [A <: TBool, B <: TBool] = A#If[B, TFalse]
  type || [A <: TBool, B <: TBool] = A#If[TTrue, B]
  type Not[A <: TBool] = A#If[TFalse, TTrue]
}
