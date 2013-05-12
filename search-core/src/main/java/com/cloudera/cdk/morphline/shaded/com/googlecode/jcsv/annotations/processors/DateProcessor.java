/**
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
package com.cloudera.cdk.morphline.shaded.com.googlecode.jcsv.annotations.processors;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import com.cloudera.cdk.morphline.shaded.com.googlecode.jcsv.annotations.ValueProcessor;

/**
 * Processes Date values, using a specified DateFormat.
 * 
 * By default, the ValueProcessorProvider uses {@link DateFormat#getDateInstance()}
 * as the DateFormat.
 * 
 * @author Eike Bergmann
 */
public class DateProcessor implements ValueProcessor<Date> {

	private final DateFormat sdf;

	public DateProcessor(DateFormat sdf) {
		this.sdf = sdf;
		
	}

	/**
	 * Parses the value as a date, using the given DateFormat instance.
	 * 
	 * @param value
	 *            the input string
	 * @return Date the date
	 * @throws IllegalArgumentException
	 *             if the input String can not be parsed by the given
	 *             DateFormat instance
	 */
	@Override
	public Date processValue(String value) {
		Date result = null;
		try {
			result = sdf.parse(value);
		} catch (ParseException pe) {
			throw new IllegalArgumentException(value + " can not be parsed as a date", pe);
		}

		return result;
	}
}
