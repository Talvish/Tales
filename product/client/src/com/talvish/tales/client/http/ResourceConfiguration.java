package com.talvish.tales.client.http;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class ResourceConfiguration {
	private final String endpoint;
	private final boolean allowUntrustedSsl;

	public ResourceConfiguration( String theEndpoint ) {
		this( theEndpoint, false );
	}

	public ResourceConfiguration( String theEndpoint, boolean allowUntrustedSsl ) {
		Preconditions.checkArgument( !Strings.isNullOrEmpty( theEndpoint ), "need an endpoint" );
		
		endpoint = theEndpoint;
		this.allowUntrustedSsl = allowUntrustedSsl;
	}
	
	public String getEndpoint( ) {
		return endpoint;
	}
	
	public boolean getAllowUntrustedSsl( ) {
		return allowUntrustedSsl;
	}
}
