// ***************************************************************************
// *  Copyright 2015 Joseph Molnar
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
package com.talvish.tales.system.configuration.hierarchical;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.base.Preconditions;

/**
 * Simple helper tree structure which is currently only used for tracking
 * setting history (for potential conflict) in the config system.
 * @author jmolnar
 *
 * @param <T> the type of data being stored
 */
class SimpleTreeNode<T> {
	private T value;
	private final List<SimpleTreeNode<T>> children = new ArrayList<SimpleTreeNode<T>>( );
	
	/**
	 * Default empty constructor meaning nothing is set yet.
	 */
	public SimpleTreeNode( ) {
		value = null;
	}

	/**
	 * The constructor taking the current value to use, with no children.
	 * @param theValue the value to store
	 */
	public SimpleTreeNode( T theValue ) {
		value = theValue;
	}

	/**
	 * The constructor taking both a value and a starting child.
	 * @param theValue the starting value
	 * @param child the initial child
	 */
	public SimpleTreeNode( T theValue, SimpleTreeNode<T> child ) {
		value = theValue;
		children.add( child );
	}

	/**
	 * Gets the value associated with the node.
	 * @return the value associated with the node
	 */
	public T getValue( ) {
		return value;
	}
	
	/**
	 * Associates a value with the node.
	 * @param theValue the value to associate
	 */
	public void setValue( T theValue ) {
		value = theValue;
	}
	
	/**
	 * Adds a child node to the tree.
	 * @param theChild the child node to add
	 */
	public void addChild( SimpleTreeNode<T> theChild ) {
		Preconditions.checkArgument( theChild != null, "Attempting to add a null child node." );;
		children.add( theChild );
	}

	/**
	 * Indicates if the node has any children.
	 * @return true if the node has children, false otherwise
	 */
	public boolean hasChildren( ) {
		return children.size() > 0;
	}
	
	/**
	 * Returns all of the children nodes.
	 * @return the children of the node
	 */
	public Collection<SimpleTreeNode<T>> getChildren( ) {
		return children;
	}
}
