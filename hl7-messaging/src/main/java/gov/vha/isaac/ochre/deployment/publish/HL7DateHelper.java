/**
 * Copyright Notice
 *
 * This is a work of the U.S. Government and is not subject to copyright
 * protection in the United States. Foreign copyrights may apply.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.vha.isaac.ochre.deployment.publish;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * @author vhaislempeyd
 *
 * Formats date string
 */
public class HL7DateHelper
{
	private static SimpleDateFormat dateTimeFormat;

	/**
	 * The <code>getHL7DateFormat</code> method takes date and time input and returns
	 * a date string that matches the HL7 date standard, including the time
	 * offset from GMT.  If the incoming date string ends with '_NOW', this method
	 * returns the string '00000000000000.000-0000', which is interpreted by
	 * MFS as 'now' (current date and time).
	 *
	 * @param incomingDateString should be in the form 'MM/dd/yyyy@hh:mm' or in the form 'MM/dd/yyyy'.
	 * @return
	 */
	public static String getHL7DateFormat(String incomingDateString)
	{
		String hl7DateOutput = null;
		DateFormat incomingDateFormat;

		if(incomingDateString.indexOf("@") >= 0)
		{
			incomingDateFormat = new SimpleDateFormat("MM/dd/yyyy@hh:mm");
		}
		else
		{
			incomingDateFormat = new SimpleDateFormat("MM/dd/yyyy");
		}
		DateFormat outgoingDateFormat = new SimpleDateFormat("yyyyMMddhhmmss.SSSZ");



		if((incomingDateString.endsWith("_NOW")) | (incomingDateString.endsWith("_now")))
		{
			hl7DateOutput = "00000000000000.000-0000";
			return hl7DateOutput;
		}

		try
		{
			Date myDate = incomingDateFormat.parse(incomingDateString);

			hl7DateOutput = outgoingDateFormat.format(myDate);
		}
		catch(ParseException pe)
		{
			pe.printStackTrace();
		}
		return hl7DateOutput;
	}

	/**
	 * The <code>getNumericDateFormat</code> method takes date and time input and
	 * returns a date string in the form yyyyMMddhhmm.  The incoming date string may
	 * be in the form MM/dd/yyyy@hh:mm or MM/dd/yyyy@hh:mm_NOW.
	 *
	 * @param incomingDateString Date string
	 * @return Date string in the form yyyyMMddhhmmss
	 */
	public static String getNumericDateFormat(String incomingDateString)
	{
		String dateOutput = null;
		DateFormat incomingDateFormat;

		if(incomingDateString.indexOf("@") >= 0)
		{
			incomingDateFormat = new SimpleDateFormat("MM/dd/yyyy@hh:mm");
		}
		else
		{
			incomingDateFormat = new SimpleDateFormat("MMM/dd/yyyy");
		}

		DateFormat outgoingDateFormat = new SimpleDateFormat("yyyyMMddhhmmss");

		try
		{
			Date myDate = incomingDateFormat.parse(incomingDateString);

			dateOutput = outgoingDateFormat.format(myDate);
		}
		catch(ParseException pe)
		{
			pe.printStackTrace();
		}
		return dateOutput;
	}

	/**
	 * The <code>getCurrentDate</code> method returns a date string
	 * in the format MM/dd/yyyy.
	 * @return date string MM/dd/yyyy
	 */
	public static String getCurrentDate()
	{
		String currentDate = null;

		Calendar calendar = Calendar.getInstance(TimeZone.getDefault());

		String dateFormat = "MM/dd/yyyy";
		java.text.SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);

		simpleDateFormat.setTimeZone(TimeZone.getDefault());

		currentDate = simpleDateFormat.format(calendar.getTime());

		return currentDate;
	}

	/**
	 * The <code>getCurrentDateTime</code> method returns a date and time string
	 * in the format MM/dd/yyyy@hh:mm.
	 * @return date string MM/dd/yyyy@hh:mm
	 */
	public static String getCurrentDateTime()
	{
		String currentDateTime = null;

		Calendar calendar = Calendar.getInstance(TimeZone.getDefault());

		String dateFormat = "MM/dd/yyyy@HH:mm";
		java.text.SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);

		simpleDateFormat.setTimeZone(TimeZone.getDefault());

		currentDateTime = simpleDateFormat.format(calendar.getTime());

		return currentDateTime;
	}

	/**
	 * The <code>getCurrentYear</code> method returns the
	 * current year as 'yyyy'.
	 * @return String current year in the form 'yyyy'
	 */
	public static String getCurrentYear()
	{
		dateTimeFormat = new SimpleDateFormat("yyyy");
		String currentYear = dateTimeFormat.format(new Date());
		return currentYear;
	}
	/**
	 * The <code>getCurrentMonth</code> method returns the
	 * current month as 'MM'.
	 * @return String current month in the form 'MM'
	 */
	public static String getCurrentMonth()
	{
		dateTimeFormat = new SimpleDateFormat("MM");
		String currentMonth = dateTimeFormat.format(new Date());
		return currentMonth;
	}

	/**
	 * The <code>getCurrentDay</code> method returns the
	 * current day of the month as 'dd'.
	 * @return String current day in the form 'dd'
	 */
	public static String getCurrentDay()
	{
		dateTimeFormat = new SimpleDateFormat("dd");
		String currentDay = dateTimeFormat.format(new Date());
		return currentDay;
	}

	/**
	 * The <code>getCurrentHour</code> method returns the
	 * current hour of the day as 'hh' using a 24-hour clock.
	 * @return String current hour of the day in the form 'hh'
	 */
	public static String getCurrentHour()
	{
		dateTimeFormat = new SimpleDateFormat("hh");
		String currentHour = dateTimeFormat.format(new Date());
		return currentHour;
	}

	/**
	 * The <code>getCurrentMinute</code> method returns the
	 * current minute of the current hour as 'mm'.
	 * @return String current minute of the current hour in the form 'mm'
	 */
	public static String getCurrentMinute()
	{
		dateTimeFormat = new SimpleDateFormat("mm");
		String currentMinute = dateTimeFormat.format(new Date());
		return currentMinute;
	}

	/**
	 * The <code>getCurrentSecond</code> method returns the
	 * current second of the current minute as 'ss'.
	 * @return String current second of the current minute in the form 'ss'
	 */
	public static String getCurrentSecond()
	{
		dateTimeFormat = new SimpleDateFormat("ss");
		String currentSecond = dateTimeFormat.format(new Date());
		return currentSecond;
	}

	/**
	 * The <code>getCurrentMillisecond</code> method returns the
	 * current millisecond of the current second as 'SSS'.
	 * @return String current millisecond of the current second in the form 'SSS'
	 */
	public static String getCurrentMillisecond()
	{
		dateTimeFormat = new SimpleDateFormat("SSS");
		String currentMillisecond = dateTimeFormat.format(new Date());
		return currentMillisecond;
	}
}
