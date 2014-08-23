<%@page
	import="com.tales.services.Service"
	import="com.tales.services.http.AttributeConstants"
	import="com.tales.system.configuration.ConfigurationManager"
	contentType="text/html;charset=UTF-8" language="java" session="false"%>
	
<!DOCTYPE html>
<html>
<head>
	<title>Hello World</title>
	<script src="//ajax.googleapis.com/ajax/libs/jquery/2.1.1/jquery.min.js"></script> 
</head>

<%
    // let's pull out the configuration manager so we can load some config
    Service service = ( Service )request.getServletContext().getAttribute( AttributeConstants.SERVICE_SERVLET_CONTEXT );
	ConfigurationManager configManager = service.getConfigurationManager( );
	
	// now let's get the config values and provide some defaults, just in they aren't found
	String contractBaseURL = configManager.getStringValue( "simple_contract.base_url", "http://localhost:8090/simple_contract" );
	String contractVersion = configManager.getStringValue( "simple_contract.version", "20140124" );
%>

<body>
    <div id="form">
        <span id="query_label">query: </span>
        <input id="query_value" />
        <span id="cookie_label">cookie: </span>
        <input id="cookie_value" />
        <button id="submit">submit</button>
    </div>
    <div id="process"></div>
    <div id="output"></div>
    <script>
    	$("#submit").click( function( ) {
    	    /** TODO: Not sure this encoding below is sufficient: http://stackoverflow.com/questions/6544564/url-encode-a-string-in-jquery-for-an-ajax-request **/
    	    var queryValue = encodeURIComponent( $("#query_value").val( ) );
    		var cookieValue = encodeURIComponent( $("#cookie_value").val( ) );
    		var requestUrl = "<%=contractBaseURL%>/echo?query_echo=" + queryValue + "&version=<%=contractVersion %>";

    		document.cookie="cookie_echo=" + cookieValue;
    		
    		$("#process").html( "request: " + requestUrl );
    		$("#output").html( "" );
    		$.ajax( {
				type:'GET',
				url:requestUrl,
				success:function(data) {
					$("#output").html( "result: " + data.return );
				},
				error:function(data) {
 					$("#output").html( "result: failure" );
				}
			})  
		});
    </script>
</body>
</html>