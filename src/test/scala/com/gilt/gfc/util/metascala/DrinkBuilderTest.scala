package com.gilt.gfc.util.metascala

import org.scalatest.{FunSuite, Matchers}

class DrinkBuilderTest extends FunSuite with Matchers {
  test("can build a cuba libre (whisky & coke)") {
    val drink = DrinkBuilder().withWhisky().withCoke().withGlass(Tall).asDouble().build()

    drink.glass shouldBe Tall
    drink.spirit shouldBe Whisky
    drink.mixer shouldBe Some(Coke)
    drink.isDouble shouldBe true
  }

  test("can build a neat single whisky") {
    val drink = DrinkBuilder().withWhisky().withGlass(Short).build()

    drink.glass shouldBe Short
    drink.spirit shouldBe Whisky
    drink.mixer shouldBe None
    drink.isDouble shouldBe false
  }

  test("can build a G & T") {
    val drink = DrinkBuilder().withGin().withTonic().withGlass(Tulip).build()

    drink.glass shouldBe Tulip
    drink.spirit shouldBe Gin
    drink.mixer shouldBe Some(Tonic)
    drink.isDouble shouldBe false
  }

  test("can't build from nothing") {
    assertTypeError("DrinkBuilder().build()")
  }

  test("can't build missing glass") {
    assertTypeError("DrinkBuilder().withWhisky().withCoke().build()")
  }

  test("can't build missing spirit") {
    assertTypeError("DrinkBuilder().withGlass(Tall).build()")
  }

  test("can't set glass twice") {
    assertTypeError("DrinkBuilder().withGlass(Tall).withGlass(Tall)")
  }

  test("can't set spirit twice") {
    assertTypeError("DrinkBuilder().withWhisky().withWhisky()")
    assertTypeError("DrinkBuilder().withGin().withGin()")
    assertTypeError("DrinkBuilder().withWhisky().withGin()")
    assertTypeError("DrinkBuilder().withGin().withWhisky()")
  }

  test("can't add mixer twice") {
    assertTypeError("DrinkBuilder().withWhisky().withCoke().withCoke()")
  }

  test("can't add mixer before spirit") {
    assertTypeError("DrinkBuilder().withCoke().withWhisky()")
  }

  test("can't add wrong mixer") {
    assertTypeError("DrinkBuilder().withWhisky().withTonic()")
    assertTypeError("DrinkBuilder().withGin().withCoke()")
  }

}
