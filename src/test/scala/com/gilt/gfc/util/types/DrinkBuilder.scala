package com.gilt.gfc.util.types

/**
 * Sample Typesafe Builder, using metascala phantom types.
 *
 * Inspired by
 * http://blog.rafaelferreira.net/2008/07/type-safe-builder-pattern-in-scala.html and
 * http://stackoverflow.com/questions/14283938/type-safe-builder-how-to-combine-phantom-types
 */
sealed abstract class Glass
case object Short extends Glass
case object Tall extends Glass
case object Tulip extends Glass

sealed abstract class Spirit
case object Whisky extends Spirit
case object Gin extends Spirit

sealed abstract class Mixer
case object Coke extends Mixer
case object Tonic extends Mixer

sealed trait BuilderMethods {
  type GlassCalled <: TBool
  type WhiskyCalled <: TBool
  type GinCalled <: TBool
  type MixerCalled <: TBool
}

case class OrderOfDrink(glass: Glass, spirit: Spirit, mixer: Option[Mixer], isDouble: Boolean)

class DrinkBuilder[M <: BuilderMethods] private (glass: Option[Glass],
                                                 spirit: Option[Spirit],
                                                 mixer: Option[Mixer],
                                                 isDouble: Boolean) {
  def withGlass(g: Glass)
               (implicit ev: M#GlassCalled =:= TFalse) = {
    new DrinkBuilder[M {type GlassCalled = TTrue}](Some(g), spirit, mixer, isDouble)
  }
  
  def withWhisky()
                (implicit ev1: M#WhiskyCalled =:= TFalse,
                          ev2: M#GinCalled =:= TFalse) = {
    new DrinkBuilder[M {type WhiskyCalled = TTrue}](glass, Some(Whisky), mixer, isDouble)
  }

  def withGin()
             (implicit ev1: M#WhiskyCalled =:= TFalse,
                       ev2: M#GinCalled =:= TFalse) = {
    new DrinkBuilder[M {type GinCalled = TTrue}](glass, Some(Gin), mixer, isDouble)
  }

  def withCoke()
              (implicit ev1: M#WhiskyCalled =:= TTrue,
                        ev2: M#MixerCalled =:= TFalse) = {
    new DrinkBuilder[M {type MixerCalled = TTrue}](glass, spirit, Some(Coke), isDouble)
  }

  def withTonic()
               (implicit ev1: M#GinCalled =:= TTrue,
                         ev2: M#MixerCalled =:= TFalse) = {
    new DrinkBuilder[M {type MixerCalled = TTrue}](glass, spirit, Some(Tonic), isDouble)
  }

  def asDouble() = {
    new DrinkBuilder[M](glass, spirit, mixer, true)
  }
  
  def build()(implicit ev1: M#GlassCalled =:= TTrue,
                       ev2: M#WhiskyCalled || M#GinCalled =:= TTrue) = {
    OrderOfDrink(glass.get, spirit.get, mixer, isDouble)
  }
}

object DrinkBuilder {
  def apply() = new DrinkBuilder[BuilderMethods { type GlassCalled = TFalse;
                                                  type WhiskyCalled = TFalse;
                                                  type GinCalled = TFalse;
                                                  type MixerCalled = TFalse }
    ](None, None, None, false)
}
