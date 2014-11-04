package com.talvish.tales.contracts.services.http;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import com.talvish.tales.contracts.Subcontract;


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
	 * @param theBoundPath the path the resource is bound to
	 * @param theResourceType the type of resource being bound
	 */
	public HttpResourceContract( String theName, String theDescription, String[] theVersions, Object theBoundObject, String theBoundPath, ResourceType theResourceType ) {
		super( theName, theDescription, theVersions, theBoundObject, theBoundPath );
	
		Collection<Subcontract> subcontracts = new ArrayList<Subcontract>( );
		
		resourceType = theResourceType;
		for( ResourceMethod method : theResourceType.getMethods() ) {
			this.getStatusManager().register( method.getName(), method.getStatus() );
			int changeThis;
			// NOTE: this is a hack job since this isn't the proper parent
			//       I should see about using ResourceType / Resource Method directly
			//       and not have this class
			// NOTE: I tried one pass to get rid of this but the big difference is that
			//       ResourceType/Method do not expect to have an instance object since they
			//		 inherit off of Contact, but HttpContract inherits off of ServiceContract
			//		 which expects a slew of things including an instance object and status
			//		 I did do the first pass, however, which was to disassociate the servlet
			//		 from the contract, instead relying on the Jetty-dervied ContractServletHolder
			//		 to be the binding point between a contract, a servlet and and interface
			//		 maybe what we need is a contract instance which binds these things together
			//		 and that is really what this HttpContract 'stuff' is about
			// NOTE: if we keep this, we need to not have more than one of the same subcontract
			
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
