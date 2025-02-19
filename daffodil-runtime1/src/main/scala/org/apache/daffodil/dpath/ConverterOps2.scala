/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.daffodil.dpath

import com.ibm.icu.text.SimpleDateFormat
import com.ibm.icu.util.Calendar

import org.apache.daffodil.calendar.DFDLCalendar
import org.apache.daffodil.calendar.DFDLDate
import org.apache.daffodil.calendar.DFDLDateConversion
import org.apache.daffodil.calendar.DFDLDateTime
import org.apache.daffodil.calendar.DFDLDateTimeConversion
import org.apache.daffodil.calendar.DFDLTime
import org.apache.daffodil.calendar.DFDLTimeConversion

case object AnyAtomicToString extends Converter {
  override def computeValue(a: AnyRef, dstate: DState): AnyRef = {
    a match {
      case c: DFDLCalendar => c.toString
      case _ => a.asInstanceOf[String]
    }
  }
}

case object StringToDate extends Converter {
  val name = "StringToDate"

  override def computeValue(a: AnyRef, dstate: DState): AnyRef = {
    val result = a match {
      case cal: DFDLDateTime => cal.toDate
      case cal: DFDLDate => cal
      case str: String => DFDLDateConversion.fromXMLString(str)
      case _ => throw new NumberFormatException("xs:date only accepts String, Date or DateTime objects.")
    }
    result
  }
}

case object StringToDateTime extends Converter {
  val name = "StringToDateTime"

  override def computeValue(a: AnyRef, dstate: DState): AnyRef = {
    val result = a match {
      case cal: DFDLDateTime => cal
      case cal: DFDLDate => cal.toDateTime
      case str: String => DFDLDateTimeConversion.fromXMLString(str)
      case _ => throw new NumberFormatException("xs:dateTime only accepts String, Date or DateTime objects.")
    }
    result
  }
}

case object StringToTime extends Converter {
  val name = "StringToTime"

  override def computeValue(a: AnyRef, dstate: DState): AnyRef = {
    val result = a match {
      case cal: DFDLDateTime => cal.toTime
      case cal: DFDLTime => cal
      case str: String => DFDLTimeConversion.fromXMLString(str)
      case _ => throw new NumberFormatException("xs:time only accepts String, DateTime or Time objects.")
    }
    result
  }
}

case object StringToHexBinary extends Converter with HexBinaryKind{
  val name = "StringToHexBinary"

  override def computeValue(a: AnyRef, dstate: DState): AnyRef = {
    val result = a match {
      case s: String => hexStringToByteArray(s)
      case hb: Array[Byte] => hb
      case x => throw new NumberFormatException("%s cannot be cast to dfdl:hexBinary\ndfdl:hexBinary received an unrecognized type! Must be String or HexBinary.".format(x.toString))
    }
    result
  }
}
