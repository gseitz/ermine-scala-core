package ermine.syntax

import bound._
import org.scalacheck.{Prop, Arbitrary}
import org.scalacheck.Prop._
import f0._
import f0.Readers._
import f0.Writers._
import f0.DynamicF
import scalaz._
import Scalaz._
import CoreArbitraryInstances._
import CoreSerialization._

object CoreSerializationTests extends ErmineProperties("CoreSerializationTests") {

  test("core == put/get core")(clone(coreW(intW), coreR(intR)))

  def clone[A,F](w: f0.Writer[A,F], r: f0.Reader[A,F])(implicit eql: Equal[A], arb: Arbitrary[A]): Prop =
    forAll((a: A) => { r(w.toByteArray(a)) === a })
}

object RoundTripTest extends ErmineProperties("RoundTripTest") {

  lazy val coreEchoExists = new java.io.File("../ermine/dist/build/core-echo/core-echo.exe").exists

  test("roundtrip")(forAll{(a: Core[Int]) => echo(a) })
  test("floats")(forAll{(f: Float) => echo(LitFloat(f))})
  test("doubles")(forAll{(d: Double) => echo(LitDouble(d))})

  def echo(c:Core[Int]) = coreEchoExists ==> (c === callHaskellEcho(coreW(intW), coreR(intR))(c))

  def callHaskellEcho[A, F](w: f0.Writer[A,F], r: f0.Reader[A,F])(aOut: A)(implicit eql: Equal[A], arb: Arbitrary[A]): A = {
    import java.io._
    List(new File("core.in.toString"), new File("core.in"), new File("core.out")).foreach(_.delete)
    // write the core to a file so that if it fails we have it around to paste back into code
    val pw  = new PrintWriter("core.in.toString")
    pw.println(aOut)
    pw.close()
    // serialize the Core[Int] out to a file.
    Sinks.toFile("core.in").using{ s => w.bind(s)(aOut) }
    // call the haskell process that reads it in from core.in and spits it back out into core.out
    val p = Runtime.getRuntime.exec("../ermine/dist/build/core-echo/core-echo.exe")
    // read all the output from the process or else it can get deadlocked due to laziness
    scala.io.Source.fromInputStream(p.getInputStream).mkString
    p.waitFor
    // read the Core[Int] back in from the file.
    Sources.fromFile("core.out")(r)
  }
}


//test("roundtrip A")(coreEchoExists ==> (callHaskellEcho(hardcoreW, hardcoreR)(LitChar('A')) === LitChar('A')))
//  def roundtrip[A,F](w: f0.Writer[A,F], r: f0.Reader[A,F])(implicit eql: Equal[A], arb: Arbitrary[A]): Prop = Prop(prms => {
//    def callHaskellEcho(aOut: A) = {
//      println(aOut)
//      Sinks.toFile("../ermine/core.in").using{ s => w.bind(s)(aOut) }
//      val p = Runtime.getRuntime.exec("../ermine/dist/build/core-echo/core-echo.exe")
//      p.waitFor
//      Sources.fromFile("../ermine/core.in")(r)
//    }
//    val aOut: A = arb.arbitrary(prms.genPrms).get
//    Prop.all(callHaskellEcho(aOut) === aOut)(prms)
//  })

//      Sinks.toProcess(p).using{s =>
//        w.bind(s)(aOut)
//        println("done writing!")
//        val res = Sources.fromProcess(p)(r)
//        println("done reading!")
//        res
//      }