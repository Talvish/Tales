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
package com.talvish.tales.client.http;

import com.talvish.tales.system.configuration.annotated.Settings;

/**
 * A client configuration class that can be used by clients that do not have additional configuration.
 * To ensure no accidental use, this class cannot be inherited from. If there is a desire to 
 * have additional client configuration, the <code>ResourceConfigurationBase</code> class should be 
 * inherited from instead.
 * @author jmolnar
 *
 */
@Settings
final public class ResourceConfiguration extends ResourceConfigurationBase<ResourceConfiguration>{

}
