// ***************************************************************************
// *  Copyright 2011 Joseph Molnar
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
// ***************************************************************************
package com.tales.storage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import com.google.common.base.Preconditions;

public class Query<T> {
	private final Class<T> type;
	private final Collection<Filter> filters;
	
	public Query( Class<T> theType, Filter ... theFilters ) {
		Preconditions.checkNotNull( theType, "need a type" );
		type = theType;
		filters = Collections.unmodifiableCollection( Arrays.asList( theFilters ) );
	}

	public Query( Class<T> theType, Collection<Filter> theFilters ) {
		Preconditions.checkNotNull( theType, "need a type" );
		type = theType;
		filters = Collections.unmodifiableCollection( new ArrayList<Filter>( theFilters ) );
	}

	public Class<T> getType( ) {
		return type;
	}
	
	public Collection<Filter> getFilters( ) {
		return filters;
	}
}
