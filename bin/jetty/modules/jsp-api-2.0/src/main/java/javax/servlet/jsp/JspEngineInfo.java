/**
 *
 * Copyright 2003-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

//
// This source code implements specifications defined by the Java
// Community Process. In order to remain compliant with the specification
// DO NOT add / change / or delete method signatures!
//

package javax.servlet.jsp;

/**
 * The JspEngineInfo is an abstract class that provides information on the
 * current JSP engine.
 */

public abstract class JspEngineInfo {

    /**
     * Sole constructor. (For invocation by subclass constructors, 
     * typically implicit.)
     */
    public JspEngineInfo() {
    }
    
    /**
     * Return the version number of the JSP specification that is supported by
     * this JSP engine.
     * <p>
     * Specification version numbers that consists of positive decimal integers
     * separated by periods ".", for example, "2.0" or "1.2.3.4.5.6.7".
     * This allows an extensible number to be used to
     * represent major, minor, micro, etc versions.
     * The version number must begin with a number.
     * </p>
     *
     * @return the specification version, null is returned if it is not known
     */

    public abstract String getSpecificationVersion();
}
