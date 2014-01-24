package com.tales.contracts.services.http;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.servlet.Servlet;

import com.tales.contracts.Subcontract;


/**
 * A contract representing a resource that is being exposed.
 * @author jmolnar
 *
 */
public class HttpResourceContract extends HttpContract {
	private final ResourceType resourceType;
	
	private final Collection<Subcontract> resourceSubcontracts; 
	/**
	 * The constructor for the contract.
	 * @param theName the name of the contract
	 * @param theDescription the optional description of the contract
	 * @param theVersions the versions supported by the contract
	 * @param theBoundObject the resource instance being bound
	 * @param theBoundServlet the servlet being bound within
	 * @param theBoundPath the path the resource is bound to
	 * @param theResourceType the type of resource being bound
	 */
	public HttpResourceContract( String theName, String theDescription, String[] theVersions, Object theBoundObject, Servlet theBoundServlet, String theBoundPath, ResourceType theResourceType ) {
		super( theName, theDescription, theVersions, theBoundObject, theBoundServlet, theBoundPath );
	
		Collection<Subcontract> subcontracts = new ArrayList<Subcontract>( );
		
		resourceType = theResourceType;
		for( ResourceMethod method : theResourceType.getMethods() ) {
			this.getStatusManager().register( method.getName(), method.getStatus() );
			int changeThis;
			// NOTE: this is a hack job since this isn't the proper parent
			//       I should see about using ResourceType / Resource Method directly
			//       and not have this class
			subcontracts.add( method );
		}
		resourceSubcontracts = Collections.unmodifiableCollection( subcontracts );
	}
	
	/**
	 * The resource type being exposed for this contract.
	 * @return the resource type
	 */
	public ResourceType getResourceType( ) {
		return resourceType;
	}
	

	/**
	 * Returns the subcontracts, which are resource methods
	 * of this resource contract. 
	 */
	@Override
	public Collection<Subcontract> getSubcontracts() {
		return resourceSubcontracts;
	}
}
