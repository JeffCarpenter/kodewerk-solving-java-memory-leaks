// ========================================================================
// Copyright 2003-2005 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

package org.mortbay.jetty.security;

import java.io.IOException;
import java.io.Serializable;
import java.security.Principal;

import org.mortbay.jetty.Request;
import org.mortbay.jetty.Response;

/** Authenticator Interface.
 * This is the interface that must be implemented to provide authentication implementations to the HttpContext.
 */
public interface Authenticator extends Serializable
{
    /** Authenticate.
     * @param realm an <code>UserRealm</code> value
     * @param pathInContext a <code>String</code> value
     * @param request a <code>Request</code> value
     * @param response a <code>Response</code> value. If non-null response is passed, 
     *              then a failed authentication will result in a challenge response being 
     *              set in the response.
     * @return User <code>Principal</code> if authenticated. Null if Authentication
     * failed. If the SecurityConstraint.__NOBODY instance is returned,
     * the request is considered as part of the authentication process.
     * @exception IOException if an error occurs
     */
    public Principal authenticate(
        UserRealm realm,
        String pathInContext,
        Request request,
        Response response)
        throws IOException;

    /* ------------------------------------------------------------ */
    public String getAuthMethod();
}
