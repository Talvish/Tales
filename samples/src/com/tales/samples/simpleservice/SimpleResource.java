package com.tales.samples.simpleservice;

import com.tales.contracts.services.http.RequestParam;
import com.tales.contracts.services.http.ResourceContract;
import com.tales.contracts.services.http.ResourceOperation;

@ResourceContract( name="com.tales.simple_resource", versions={ "20140124" } )
public class SimpleResource {
	@ResourceOperation( name="hello_world", path="GET : hello" )
	public String hello( ) {
		return "hello world";
	}
	
	@ResourceOperation( name="echo", path="GET | POST : echo")
	public String echo( @RequestParam( name="value" )String theValue ) {
		return theValue;
	}
}
