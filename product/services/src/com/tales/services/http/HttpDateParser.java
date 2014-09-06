// ***************************************************************************
// *  Copyright 2014 Joseph Molnar
// *
// *  Licensed under the Apache License, Version 2.0 (the "License");
// *  you may not use this file except in compliance with the License.
// *  You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// *  Unless required by applicable law or agreed to in writing, software
// *  distributed under the License is distributed on an "AS IS" BASIS,
// *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// *  See the License for the specific language governing permissions and
// *  limitations under the License.
// *
// *  This is base on the Jetty DateParser that can be found here ...
// *       http://download.eclipse.org/jetty/stable-9/xref/org/eclipse/jetty/http/DateParser.html
// *  ... as found on January 6, 2014. 
// *
// ***************************************************************************
package com.tales.services.http;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class HttpDateParser {
	private static final TimeZone __GMT = TimeZone.getTimeZone("GMT");
	static {
		__GMT.setID("GMT");
	}

	final static String __dateReceiveFmt[] = {
		"EEE, dd MMM yyyy HH:mm:ss zzz",
		"EEE, dd-MMM-yy HH:mm:ss",
		"EEE MMM dd HH:mm:ss yyyy",
		"EEE, dd MMM yyyy HH:mm:ss", "EEE dd MMM yyyy HH:mm:ss zzz",
		"EEE dd MMM yyyy HH:mm:ss", "EEE MMM dd yyyy HH:mm:ss zzz", "EEE MMM dd yyyy HH:mm:ss",
		"EEE MMM-dd-yyyy HH:mm:ss zzz", "EEE MMM-dd-yyyy HH:mm:ss", "dd MMM yyyy HH:mm:ss zzz",
		"dd MMM yyyy HH:mm:ss", "dd-MMM-yy HH:mm:ss zzz", "dd-MMM-yy HH:mm:ss", "MMM dd HH:mm:ss yyyy zzz",
		"MMM dd HH:mm:ss yyyy", "EEE MMM dd HH:mm:ss yyyy zzz",
		"EEE, MMM dd HH:mm:ss yyyy zzz", "EEE, MMM dd HH:mm:ss yyyy", "EEE, dd-MMM-yy HH:mm:ss zzz",
		"EEE dd-MMM-yy HH:mm:ss zzz", "EEE dd-MMM-yy HH:mm:ss",
	};

	public static long parseDate(String date) {
		return __dateParser.get().parse(date);
	}

	private static final ThreadLocal<HttpDateParser> __dateParser =new ThreadLocal<HttpDateParser>( ) {
		@Override
		protected HttpDateParser initialValue() {
			return new HttpDateParser();
		}
	};
	
	final SimpleDateFormat _dateReceive[]= new SimpleDateFormat[__dateReceiveFmt.length];
	private long parse(final String dateVal) {
		for (int i = 0; i < _dateReceive.length; i++) {
			if (_dateReceive[i] == null) {
				_dateReceive[i] = new SimpleDateFormat(__dateReceiveFmt[i], Locale.US);
				_dateReceive[i].setTimeZone(__GMT);
			}
			try {
				Date date = (Date) _dateReceive[i].parseObject(dateVal);
				return date.getTime();
			} catch (java.lang.Exception e) {
				// LOG.ignore(e);
			}
		}

		if (dateVal.endsWith(" GMT")) {
			final String val = dateVal.substring(0, dateVal.length() - 4);
			for (SimpleDateFormat element : _dateReceive) {
				try {
					Date date = (Date) element.parseObject(val);
					return date.getTime();
				} catch (java.lang.Exception e) {
					// LOG.ignore(e);
				}
			}
		}
		return -1;
	}
}
