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
package object metascala {
  sealed trait TBool {
    type If[T <: Up, F <: Up, Up] <: Up
  }

  sealed trait TTrue extends TBool {
    type If[T <: Up, F <: Up, Up] = T
  }

  sealed trait TFalse extends TBool {
    type If[T <: Up, F <: Up, Up] = F
  }

  type &&[A <: TBool, B <: TBool] = A#If[B, TFalse, TBool]
  type || [A <: TBool, B <: TBool] = A#If[TTrue, B, TBool]
  type Not[A <: TBool] = A#If[TFalse, TTrue, TBool]
}
