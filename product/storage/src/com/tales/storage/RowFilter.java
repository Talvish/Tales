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

/**
 * The row filter is used to manage the number of rows, and the
 * starting position of the rows, to return from the data repository.
 * @author jmolnar
 *
 * @param <K> the key type
 */
public class RowFilter<K> implements Filter {
	private final int limit;
	private final K startKey;
	private final K endKey;
	
	/**
	 * Constructor taking only a limit, which is the total
	 * number of results being requested.
	 * @param theLimit the number of rows to return, if that many are available
	 */
	public RowFilter( int theLimit ) {
		this( null, null, theLimit );
	}
	
	/**
	 * Constructor taking starting key to use in the query.
	 * This is useful for paging.
	 * @param theStartKey the starting key to use
	 */
	public RowFilter( K theStartKey ) {
		this( theStartKey, null, 0 );
	}

	/**
	 * Constructor taking the start key and limit.
	 * This indicates where to start, and from there, the 
	 * total number to return.
	 * @param theStartKey the start key to use
	 * @param theLimit the number of rows to return, if that many are available
	 */
	public RowFilter( K theStartKey, int theLimit ) {
		this( theStartKey, null, theLimit );
	}

	/**
	 * Constructor taking the start and end key to use in the query.
	 * This is useful for repositories with ordered keys. Typically
	 * the end key doesn't have to exist, instead acting as a boundary
	 * so it cannot be greater than the specified value.
	 * @param theStartKey the start key to use
	 * @param theEndKey the end key to use
	 */
	public RowFilter( K theStartKey, K theEndKey ) {
		this( theStartKey, theEndKey, 0 );
	}

	/**
	 * Constructor taking the start and end key, along with the limit, to use in the query.
	 * This is useful for repositories with ordered keys. Typically
	 * the end key doesn't have to exist, instead acting as a boundary
	 * so it cannot be greater than the specified value. The limit
	 * will force the total number possible to return.
	 * @param theStartKey the start key to use
	 * @param theEndKey the end key to use
	 * @param theLimit the number of rows to return, if that many are available
	 */
	public RowFilter( K theStartKey, K theEndKey, int theLimit ) {
		startKey = theStartKey;
		endKey = theEndKey;
		limit = theLimit;
	}

	/**
	 * The start key being used by the filter.
	 * This indicates which key the request should
	 * start with.
	 * @return the start key, which may be null
	 */
	public K getStartKey( ) {
		return startKey;
	}
	
	/**
	 * The end key being used by the filter.
	 * This indicates which key the request should
	 * end with. Typically the key doesn't have to 
	 * exist but acts as a boundary
	 * @return the end key, which may be null
	 */
	public K getEndKey( ) {
		return endKey;
	}
	
	/**
	 * The limit being used by the filter. The
	 * limit forces the total number of possible
	 * rows to return.
	 * @return the limit, which may be 0 meaning no limit
	 */
	public int getLimit( ) {
		return limit;
	}
}
