package com.tales.samples.userservice;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class UserEngine {	
//	public class Status {
//		private AtomicInteger queuedItems = new AtomicInteger( 0 );
//		private RatedLong enqueueRate = new RatedLong( );
//		private AverageLong itemsPerFacebookRequest = new AverageLong( );
//
//	
//	//	private AtomicLong successfulItems;
//	//	private AtomicLong unsuccessfulItems;
//	
//		/**
//		 * Returns the current execution state of the engine.
//		 * @return the execution state
//		 */
//		@MonitorableStatusValue( name="state" )
//		public ExecutionLifecycleState getState( ) {
//			return FacebookBatchEngine.this.lifecycleState;
//		}
//	}
	
	private Map<UUID, StorageUser> storage = new HashMap<UUID, StorageUser>( );
	
	public UserEngine( ) {
		// since we aren't building a real storage system
		// we are faking a storage system by using a map
		// and adding a few existing users
		StorageUser user;
		
		user = new StorageUser( UUID.fromString( "00000000-0000-0000-0000-000000000001" ) );
		user.setFirstName( "John" );
		user.setLastName( "Doe" );		
		storage.put( user.getId(), user );

		user = new StorageUser( UUID.fromString( "00000000-0000-0000-0000-000000000002" ) );
		user.setFirstName( "Jane" );
		user.setLastName( "Smith" );		
		storage.put( user.getId(), user );
	}
	
	public StorageUser createUser( String theFirstName, String theLastName ) {
		StorageUser user = new StorageUser( UUID.randomUUID() );
		
		user.setFirstName( theFirstName );
		user.setLastName( theLastName );
		storage.put( user.getId(), user);
		return user;
	}
	
	public StorageUser getUser( UUID theId ) {
		return storage.get( theId );
	}
	
	public Collection<StorageUser> getUsers( ) {
		return storage.values();
	}
	
	public boolean updateUser( StorageUser theUser ) {
		boolean updated = false;
		StorageUser user = getUser( theUser.getId() );
		
		if( user != null ) {
			user.setFirstName( theUser.getFirstName( ) );
			user.setLastName(  theUser.getLastName( ) );
			user.indicateModified();
			storage.put( user.getId( ),  user ); // yes not needed, but pretend we are storing back into persistence
			updated = true;
		}
		return updated;
	}
	
	public boolean deleteUser( UUID theId ) {
		return !( storage.remove( theId )==null );
	}

}
