//***************************************************************************
//*  Copyright 2016 Joseph Molnar
//*
//*  Licensed under the Apache License, Version 2.0 (the "License");
//*  you may not use this file except in compliance with the License.
//*  You may obtain a copy of the License at
//*
//*      http://www.apache.org/licenses/LICENSE-2.0
//*
//*  Unless required by applicable law or agreed to in writing, software
//*  distributed under the License is distributed on an "AS IS" BASIS,
//*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//*  See the License for the specific language governing permissions and
//*  limitations under the License.
//***************************************************************************
package com.talvish.tales.system.configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
* This annotation is for use on ConfigurationSources so that they can be 
* configured automatically when loaded during the configuration source
* setup.
* @author jmolnar
*/
@Retention( RetentionPolicy.RUNTIME)
@Target( ElementType.TYPE )
public @interface  SourceConfiguration {
	 /**
	  * The class that contains the settings to use with the 
	  * associated configuration source.
	  * @return the settings
	  */
	Class<?> settingsClass( );
}