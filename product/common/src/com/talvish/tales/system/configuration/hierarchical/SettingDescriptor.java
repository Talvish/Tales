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
package com.talvish.tales.system.configuration.hierarchical;

import com.talvish.tales.contracts.data.DataContract;
import com.talvish.tales.contracts.data.DataMember;

@DataContract( name="talvish.tales.configuration.hierarchical.setting_descriptor")
public class SettingDescriptor {
	@DataMember( name="name" )
	private String name;
	@DataMember( name="description" )
	private String description;
	
	@DataMember( name="value" )
	private String value;
	@DataMember( name="type" )
	private String type;
	
	@DataMember( name="override" )
	private Boolean override;
	@DataMember( name="sensitive" )
	private Boolean sensitive;

	
	public String getName( ) {
		return name;
	}
	
	public String getDescription( ) {
		return description;
	}
	
	public String getValue( ) {
		return value;
	}
	
	public String getType( ) {
		return type;
	}
	
	public Boolean isOverride( ) {
		return override;
	}
	
	public Boolean isSensitive( ) {
		return sensitive;
	}
}
