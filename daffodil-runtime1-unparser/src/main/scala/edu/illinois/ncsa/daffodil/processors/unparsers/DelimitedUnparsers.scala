package edu.illinois.ncsa.daffodil.processors.unparsers

import edu.illinois.ncsa.daffodil.processors.ElementRuntimeData
import edu.illinois.ncsa.daffodil.processors.TextJustificationType
import edu.illinois.ncsa.daffodil.processors.TextJustificationType
import java.nio.charset.MalformedInputException
import edu.illinois.ncsa.daffodil.util.LogLevel
import edu.illinois.ncsa.daffodil.processors.ElementRuntimeData
import edu.illinois.ncsa.daffodil.processors.dfa.CreateFieldDFA
import edu.illinois.ncsa.daffodil.processors.dfa.TextDelimitedUnparser
import edu.illinois.ncsa.daffodil.schema.annotation.props.gen.GenerateEscape
import edu.illinois.ncsa.daffodil.processors.EscapeSchemeCharUnparserHelper
import edu.illinois.ncsa.daffodil.processors.EscapeSchemeBlockUnparserHelper
import edu.illinois.ncsa.daffodil.processors.EscapeSchemeUnparseEv
import edu.illinois.ncsa.daffodil.equality._
import edu.illinois.ncsa.daffodil.util.MaybeChar
import edu.illinois.ncsa.daffodil.util.Maybe
import edu.illinois.ncsa.daffodil.util.Maybe._

class StringDelimitedUnparser(erd: ElementRuntimeData,
  justificationPad: TextJustificationType.Type,
  override val pad: MaybeChar,
  escapeScheme: Maybe[EscapeSchemeUnparseEv],
  isDelimRequired: Boolean
  )
  extends PrimUnparserObject(erd) with PaddingRuntimeMixin with TextUnparserRuntimeMixin {

  val fieldDFA = CreateFieldDFA()
  val textUnparser = new TextDelimitedUnparser(erd)

  protected def theString(state: UState) =
    state.currentInfosetNode.asSimple.dataValueAsString

  override val padToLength: Int = {
    if (erd.minLength.isDefined) { erd.minLength.get.intValue() } else { 0 }
  }

  def padOrTruncateByJustification(str: String): String = {
    val result = justificationPad match {
      case TextJustificationType.None => str
      case TextJustificationType.Right => addLeftPadding(str)
      case TextJustificationType.Left => addRightPadding(str)
      case TextJustificationType.Center => addPadding(str)
    }
    result
  }

  def unparse(state: UState): Unit = {

    setupEncoding(state, erd)

    val schemeOpt = if (escapeScheme.isDefined) One(escapeScheme.get.evaluate(state)) else Nope

    try {
      val valueString = theString(state)

      val escapedValue: String =
        if (schemeOpt.isDefined) {
          state.withUnparserDataInputStream { dis =>
            val terminatingMarkup = state.allTerminatingMarkup

            dis.reset(valueString)

            val scheme = schemeOpt.get

            val (result, _) = {
              if (scheme.isInstanceOf[EscapeSchemeCharUnparserHelper]) {
                val theScheme = scheme.asInstanceOf[EscapeSchemeCharUnparserHelper]
                val hasEscCharAsDelimiter = terminatingMarkup.exists(d => d.lookingFor.length == 1 && d.lookingFor(0) =#= theScheme.ec)
                val thingsToEscape = (terminatingMarkup ++ scheme.lookingFor).toArray

                textUnparser.escapeCharacter(dis, fieldDFA, thingsToEscape, hasEscCharAsDelimiter, theScheme.ec, theScheme.eec, state)
              } else {
                val theScheme = scheme.asInstanceOf[EscapeSchemeBlockUnparserHelper]

                def hasInscopeTerminatingDelimiters(): Boolean = {
                  // Need to do this so we can 'break' the loop early
                  //
                  for (d <- terminatingMarkup) {
                    if (valueString.contains(d.lookingFor)) return true
                  }
                  false
                }

                val generateEscapeBlock = (theScheme.generateEscapeBlock == GenerateEscape.Always) ||
                  valueString.startsWith(theScheme.blockStart) || hasInscopeTerminatingDelimiters()

                val thingsToEscape = theScheme.lookingFor // blockEnd and extraEscapedCharacters

                textUnparser.escape(dis, fieldDFA, thingsToEscape, theScheme.blockEndDFA,
                  theScheme.eec, theScheme.blockStart, theScheme.blockEnd,
                  generateEscapeBlock, state)
              }
            }

            result
          }
        } else valueString // No EscapeScheme

      val paddedValue = padOrTruncateByJustification(escapedValue)

      val outStream = state.dataOutputStream
      val nCharsWritten = outStream.putString(paddedValue)
      if (nCharsWritten != paddedValue.length) UE(state, "%s - Too many bits in field: IndexOutOfBounds. Insufficient space to write %s characters.", nom, paddedValue.length)
      log(LogLevel.Debug, "Ended at bit position " + outStream.relBitPos0b)
    } catch {
      // Characters in infoset element cannot be encoded without error.
      //
      // This won't actually be thrown until encodingErrorPolicy='error' is
      // implemented.
      //
      case m: MalformedInputException => { UE(state, "%s - MalformedInputException: \n%s", nom, m.getMessage()) }
    }
  }

}

class LiteralNilDelimitedEndOfDataUnparser(
  erd: ElementRuntimeData,
  outputNilValue: StringLiteralForUnparser,
  justPad: TextJustificationType.Type,
  padChar: MaybeChar,
  isDelimRequired: Boolean)
  extends StringDelimitedUnparser(erd, justPad, padChar, Nope, isDelimRequired) {

  final override def theString(ustate: UState) = outputNilValue.evaluate(ustate)

}
