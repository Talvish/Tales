package com.tales.samples.simpleservice;

import com.google.common.base.Strings;
import com.tales.services.http.HttpInterface;
import com.tales.services.http.HttpService;
import com.tales.system.configuration.PropertySource;

/**
 * A simple http service that is designed to be public facing.
 * @author Joseph Molnar
 *
 */
public class SimpleService extends HttpService {

	protected SimpleService( ) {
		super( "simple_service", "Simple Service", "A very simple public tales service." );
	}
	
	@Override
	protected void onInitializeConfiguration() {
		String filename = this.getConfigurationManager( ).getStringValue( "settings.file", null ); // get a config filename	 from command-line, if available
		
		if( !Strings.isNullOrEmpty( filename ) ) {
			this.getConfigurationManager( ).addSource( new PropertySource( filename) );
		}
	};
	
	@Override
	protected void onStart() {
		super.onStart();
		
		HttpInterface httpInterface = new HttpInterface( "public", this );
		
		this.interfaceManager.register( httpInterface );
		httpInterface.bind( new SimpleResource( ), "/simple_resource" );
	}
	
    public static void main( String[ ] args ) throws Exception {
    	SimpleService service = new SimpleService( );
    	
    	service.start( args );
    	service.run( );
    	service.stop( );
	}
}
