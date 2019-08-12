/*
 * Copyright 2006 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.user.server.rpc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.SerializationException;

/**
 * RemoteServiceServlet changes to allow extensions required for Jetty Continuatution support.
 *
 * Changes:
 *
 * readPayloadAsUtf8 now protected non-static
 *
 * @author Craig Day (craig@alderaan.com.au)
 *
 */
public class OpenRemoteServiceServlet extends HttpServlet implements SerializationPolicyProvider {
    /*
     * These members are used to get and set the different HttpServletResponse and
     * HttpServletRequest headers.
     */
    private static final String ACCEPT_ENCODING = "Accept-Encoding";
    private static final String CHARSET_UTF8 = "UTF-8";
    private static final String CONTENT_ENCODING = "Content-Encoding";
    private static final String CONTENT_ENCODING_GZIP = "gzip";
    private static final String CONTENT_TYPE_TEXT_PLAIN_UTF8 = "text/plain; charset=utf-8";
    private static final String GENERIC_FAILURE_MSG = "The call failed on the server; see server log for details";
    private static final String EXPECTED_CONTENT_TYPE = "text/x-gwt-rpc";
    private static final String EXPECTED_CHARSET = "charset=utf-8";

    /**
     * Controls the compression threshold at and below which no compression will
     * take place.
     */
    private static final int UNCOMPRESSED_BYTE_SIZE_LIMIT = 256;

    /**
     * Return true if the response object accepts Gzip encoding. This is done by
     * checking that the accept-encoding header specifies gzip as a supported
     * encoding.
     */
    private static boolean acceptsGzipEncoding(HttpServletRequest request) {
        assert (request != null);

        String acceptEncoding = request.getHeader(ACCEPT_ENCODING);
        if (null == acceptEncoding) {
            return false;
        }

        return (acceptEncoding.indexOf(CONTENT_ENCODING_GZIP) != -1);
    }

    /**
     * This method attempts to estimate the number of bytes that a string will
     * consume when it is sent out as part of an HttpServletResponse. This really
     * a hack since we are assuming that every character will consume two bytes
     * upon transmission. This is definitely not true since some characters
     * actually consume more than two bytes and some consume less. This is even
     * less accurate if the string is converted to UTF8. However, it does save us
     * from converting every string that we plan on sending back to UTF8 just to
     * determine that we should not compress it.
     */
    private static int estimateByteSize(final String buffer) {
        return (buffer.length() * 2);
    }

    /**
     * Read the payload as UTF-8 from the request stream.
     */
    protected String readPayloadAsUtf8(HttpServletRequest request)
            throws IOException, ServletException {
        int contentLength = request.getContentLength();
        if (contentLength == -1) {
            // Content length must be known.
            throw new ServletException("Content-Length must be specified");
        }

        String contentType = request.getContentType();
        boolean contentTypeIsOkay = false;
        // Content-Type must be specified.
        /*if (contentType != null) {
            // The type must be plain text.
            if (contentType.startsWith("text/plain")) {
                // And it must be UTF-8 encoded (or unspecified, in which case we assume
                // that it's either UTF-8 or ASCII).
                if (contentType.indexOf("charset=") == -1) {
                    contentTypeIsOkay = true;
                } else if (contentType.indexOf("charset=utf-8") != -1) {
                    contentTypeIsOkay = true;
                }
            }
        }
        if (!contentTypeIsOkay) {
            throw new ServletException(
                    "Content-Type must be 'text/plain' with 'charset=utf-8' (or unspecified charset)");
        }*/
        if (contentType != null) {
            contentType = contentType.toLowerCase();
            /*
             * The Content-Type must be text/x-gwt-rpc.
             * 
             * NOTE:We use startsWith because some servlet engines, i.e. Tomcat, do
             * not remove the charset component but others do.
             */
            if (contentType.startsWith(EXPECTED_CONTENT_TYPE)) {
                String characterEncoding = request.getCharacterEncoding();
                if (characterEncoding != null) {
                    /*
                     * TODO: It would seem that we should be able to use equalsIgnoreCase
                     * here instead of indexOf. Need to be sure that servlet engines
                     * return a properly parsed character encoding string if we decide to
                     * make this change.
                     */
                    if (characterEncoding.toLowerCase().indexOf(CHARSET_UTF8.toLowerCase()) != -1)
                        contentTypeIsOkay = true;                    
                }
            }
        }
        if (!contentTypeIsOkay) {
            throw new ServletException("Content-Type must be '"
                    + EXPECTED_CONTENT_TYPE + "' with '" + EXPECTED_CHARSET + "'.");
        }
        InputStream in = request.getInputStream();
        try {
            byte[] payload = new byte[contentLength];
            int offset = 0;
            int len = contentLength;
            int byteCount;
            while (offset < contentLength) {
                byteCount = in.read(payload, offset, len);
                if (byteCount == -1) {
                    throw new ServletException("Client did not send " + contentLength
                            + " bytes as expected");
                }
                offset += byteCount;
                len -= byteCount;
            }
            return new String(payload, "UTF-8");
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    private final ThreadLocal perThreadRequest = new ThreadLocal();

    private final ThreadLocal perThreadResponse = new ThreadLocal();
    
    /**
     * A cache of moduleBaseURL and serialization policy strong name to
     * {@link SerializationPolicy}.
     */
    private final Map<String, SerializationPolicy> serializationPolicyCache = new HashMap<String, SerializationPolicy>();

    /**
     * The default constructor.
     */
    public OpenRemoteServiceServlet() {
    }

    /**
     * Standard HttpServlet method: handle the POST.
     * <p/>
     * This doPost method swallows ALL exceptions, logs them in the
     * ServletContext, and returns a GENERIC_FAILURE_MSG response with status code
     * 500.
     */
    public final void doPost(HttpServletRequest request,
                             HttpServletResponse response) {
        try {
            // Store the request & response objects in thread-local storage.
            //
            perThreadRequest.set(request);
            perThreadResponse.set(response);

            // Read the request fully.
            //
            String requestPayload = readPayloadAsUtf8(request);

            // Let subclasses see the serialized request.
            //
            onBeforeRequestDeserialized(requestPayload);

            // Invoke the core dispatching logic, which returns the serialized
            // result.
            //
            String responsePayload = processCall(requestPayload);

            // Let subclasses see the serialized response.
            //
            onAfterResponseSerialized(responsePayload);

            // Write the response.
            //
            writeResponse(request, response, responsePayload);
            return;
        } catch (Throwable e) {
            // Give a subclass a chance to either handle the exception or rethrow it
            //
            doUnexpectedFailure(e);
        } finally {
            // null the thread-locals to avoid holding request/response
            //
            perThreadRequest.set(null);
            perThreadResponse.set(null);
        }
    }

    /**
     * Process a call originating from the given request. Uses the
     * {@link RPC#invokeAndEncodeResponse(Object,java.lang.reflect.Method,Object[])}
     * method to do the actual work.
     * <p/>
     * Subclasses may optionally override this method to handle the payload in any
     * way they desire (by routing the request to a framework component, for
     * instance). The {@link HttpServletRequest} and {@link HttpServletResponse}
     * can be accessed via the {@link #getThreadLocalRequest()} and
     * {@link #getThreadLocalResponse()} methods.
     * </p>
     * This is public so that it can be unit tested easily without HTTP.
     *
     * @param payload the UTF-8 request payload
     * @return a string which encodes either the method's return, a checked
     *         exception thrown by the method, or an
     *         {@link IncompatibleRemoteServiceException}
     * @throws SerializationException if we cannot serialize the response
     * @throws UnexpectedException    if the invocation throws a checked exception
     *                                that is not declared in the service method's signature
     * @throws RuntimeException       if the service method throws an unchecked
     *                                exception (the exception will be the one thrown by the service)
     */
    public String processCall(String payload) throws SerializationException {
        try {
            RPCRequest rpcRequest = RPC.decodeRequest(payload, this.getClass(), this);
            return RPC.invokeAndEncodeResponse(this, rpcRequest.getMethod(),
                    rpcRequest.getParameters(), rpcRequest.getSerializationPolicy());
        } catch (IncompatibleRemoteServiceException ex) {
            return RPC.encodeResponseForFailure(null, ex);
        }
    }

    /**
     * Override this method to control what should happen when an exception
     * escapes the {@link #processCall(String)} method. The default implementation
     * will log the failure and send a generic failure response to the client.<p/>
     * <p/>
     * An "expected failure" is an exception thrown by a service method that is
     * declared in the signature of the service method. These exceptions are
     * serialized back to the client, and are not passed to this method. This
     * method is called only for exceptions or errors that are not part of the
     * service method's signature, or that result from SecurityExceptions,
     * SerializationExceptions, or other failures within the RPC framework.<p/>
     * <p/>
     * Note that if the desired behavior is to both send the GENERIC_FAILURE_MSG
     * response AND to rethrow the exception, then this method should first send
     * the GENERIC_FAILURE_MSG response itself (using getThreadLocalResponse), and
     * then rethrow the exception. Rethrowing the exception will cause it to
     * escape into the servlet container.
     *
     * @param e the exception which was thrown
     */
    protected void doUnexpectedFailure(Throwable e) {
        ServletContext servletContext = getServletContext();
        servletContext.log("Exception while dispatching incoming RPC call", e);

        // Send GENERIC_FAILURE_MSG with 500 status.
        //
        respondWithFailure(getThreadLocalResponse());
    }

    /**
     * Gets the <code>HttpServletRequest</code> object for the current call. It
     * is stored thread-locally so that simultaneous invocations can have
     * different request objects.
     */
    protected final HttpServletRequest getThreadLocalRequest() {
        return (HttpServletRequest) perThreadRequest.get();
    }

    /**
     * Gets the <code>HttpServletResponse</code> object for the current call. It
     * is stored thread-locally so that simultaneous invocations can have
     * different response objects.
     */
    protected final HttpServletResponse getThreadLocalResponse() {
        return (HttpServletResponse) perThreadResponse.get();
    }

    /**
     * Override this method to examine the serialized response that will be
     * returned to the client. The default implementation does nothing and need
     * not be called by subclasses.
     */
    protected void onAfterResponseSerialized(String serializedResponse) {
    }

    /**
     * Override this method to examine the serialized version of the request
     * payload before it is deserialized into objects. The default implementation
     * does nothing and need not be called by subclasses.
     */
    protected void onBeforeRequestDeserialized(String serializedRequest) {
    }

    /**
     * Determines whether the response to a given servlet request should or should
     * not be GZIP compressed. This method is only called in cases where the
     * requestor accepts GZIP encoding.
     * <p/>
     * This implementation currently returns <code>true</code> if the response
     * string's estimated byte length is longer than 256 bytes. Subclasses can
     * override this logic.
     * </p>
     *
     * @param request         the request being served
     * @param response        the response that will be written into
     * @param responsePayload the payload that is about to be sent to the client
     * @return <code>true</code> if responsePayload should be GZIP compressed,
     *         otherwise <code>false</code>.
     */
    protected boolean shouldCompressResponse(HttpServletRequest request,
                                             HttpServletResponse response, String responsePayload) {
        return estimateByteSize(responsePayload) > UNCOMPRESSED_BYTE_SIZE_LIMIT;
    }

    /**
     * Called when the machinery of this class itself has a problem, rather than
     * the invoked third-party method. It writes a simple 500 message back to the
     * client.
     */
    private void respondWithFailure(HttpServletResponse response) {
        try {
            response.setContentType("text/plain");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getOutputStream().write(GENERIC_FAILURE_MSG.getBytes());
        } catch (IOException e) {
            getServletContext().log(
                    "respondWithFailure failed while sending the previous failure to the client",
                    e);
        }
    }

    /**
     * Write the response payload to the response stream.
     */
    private void writeResponse(HttpServletRequest request,
                               HttpServletResponse response, String responsePayload) throws IOException {

        byte[] reply = responsePayload.getBytes(CHARSET_UTF8);
        String contentType = CONTENT_TYPE_TEXT_PLAIN_UTF8;

        if (acceptsGzipEncoding(request)
                && shouldCompressResponse(request, response, responsePayload)) {
            // Compress the reply and adjust headers.
            //
            ByteArrayOutputStream output = null;
            GZIPOutputStream gzipOutputStream = null;
            Throwable caught = null;
            try {
                output = new ByteArrayOutputStream(reply.length);
                gzipOutputStream = new GZIPOutputStream(output);
                gzipOutputStream.write(reply);
                gzipOutputStream.finish();
                gzipOutputStream.flush();
                response.setHeader(CONTENT_ENCODING, CONTENT_ENCODING_GZIP);
                reply = output.toByteArray();
            } catch (IOException e) {
                caught = e;
            } finally {
                if (null != gzipOutputStream) {
                    gzipOutputStream.close();
                }
                if (null != output) {
                    output.close();
                }
            }

            if (caught != null) {
                getServletContext().log("Unable to compress response", caught);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }
        }

        // Send the reply.
        //
        response.setContentLength(reply.length);
        response.setContentType(contentType);
        response.setStatus(HttpServletResponse.SC_OK);
        response.getOutputStream().write(reply);
    }

    public final SerializationPolicy getSerializationPolicy(String moduleBaseURL, String strongName)
    {
        SerializationPolicy serializationPolicy = getCachedSerializationPolicy(moduleBaseURL, 
                strongName);
        if (serializationPolicy != null)
            return serializationPolicy;


        serializationPolicy = doGetSerializationPolicy(getThreadLocalRequest(),
                moduleBaseURL, strongName);

        if (serializationPolicy == null) 
        {
            // Failed to get the requested serialization policy; use the default
            getServletContext().log(
                "WARNING: Failed to get the SerializationPolicy '"
                    + strongName
                    + "' for module '"
                    + moduleBaseURL
                    + "'; a legacy, 1.3.3 compatible, serialization policy will be used.  You may experience SerializationExceptions as a result.");
            serializationPolicy = RPC.getDefaultSerializationPolicy();
        }

        // This could cache null or an actual instance. Either way we will not
        // attempt to lookup the policy again.
        putCachedSerializationPolicy(moduleBaseURL, strongName, serializationPolicy);

        return serializationPolicy;
    }
    
    private SerializationPolicy getCachedSerializationPolicy(String moduleBaseURL, 
            String strongName) 
    {
        synchronized (serializationPolicyCache) 
        {
            return serializationPolicyCache.get(moduleBaseURL + strongName);
        }
    }
    
    private void putCachedSerializationPolicy(String moduleBaseURL, String strongName, 
            SerializationPolicy serializationPolicy) 
    {
        synchronized (serializationPolicyCache) 
        {
            serializationPolicyCache.put(moduleBaseURL + strongName, serializationPolicy);
        }
    }
    
    /**
     * Gets the {@link SerializationPolicy} for given module base URL and strong
     * name if there is one.
     * 
     * Override this method to provide a {@link SerializationPolicy} using an
     * alternative approach.
     * 
     * @param request the HTTP request being serviced
     * @param moduleBaseURL as specified in the incoming payload
     * @param strongName a strong name that uniquely identifies a serialization
     *          policy file
     * @return a {@link SerializationPolicy} for the given module base URL and
     *         strong name, or <code>null</code> if there is none
     */    
    protected SerializationPolicy doGetSerializationPolicy(HttpServletRequest request, 
            String moduleBaseURL, String strongName) 
    {
        // The request can tell you the path of the web app relative to the
        // container root.
        String contextPath = request.getContextPath();

        String modulePath = null;
        if (moduleBaseURL != null) 
        {
            try 
            {
                modulePath = new URL(moduleBaseURL).getPath();
            } 
            catch (MalformedURLException ex) 
            {
                // log the information, we will default
                getServletContext().log("Malformed moduleBaseURL: " + moduleBaseURL, ex);
            }
        }

        SerializationPolicy serializationPolicy = null;

        /*
         * Check that the module path must be in the same web app as the servlet
         * itself. If you need to implement a scheme different than this, override
         * this method.
         */
        if (modulePath == null || !modulePath.startsWith(contextPath)) 
        {
            String message = "ERROR: The module path requested, "
                + modulePath
                + ", is not in the same web application as this servlet, "
                + contextPath
                + ".  Your module may not be properly configured or your client and server code maybe out of date.";
            getServletContext().log(message);
        } 
        else 
        {
            // Strip off the context path from the module base URL. It should be a
            // strict prefix.
            String contextRelativePath = modulePath.substring(contextPath.length());

            String serializationPolicyFilePath = SerializationPolicyLoader.getSerializationPolicyFileName(contextRelativePath
                + strongName);

            // Open the RPC resource file read its contents.
            InputStream is = getServletContext().getResourceAsStream(
                serializationPolicyFilePath);
            try 
            {
                if (is != null) 
                {
                    try 
                    {
                        serializationPolicy = SerializationPolicyLoader.loadFromStream(is, null);
                    } 
                    catch (ParseException e) 
                    {
                        getServletContext().log(
                                "ERROR: Failed to parse the policy file '"
                                    + serializationPolicyFilePath + "'", e);
                    } 
                    catch (IOException e) 
                    {
                        getServletContext().log(
                                "ERROR: Could not read the policy file '"
                                    + serializationPolicyFilePath + "'", e);
                    }
                } 
                else 
                {
                    String message = "ERROR: The serialization policy file '"
                        + serializationPolicyFilePath
                        + "' was not found; did you forget to include it in this deployment?";
                    getServletContext().log(message);
                }
            } 
            finally 
            {
                if (is != null) 
                {
                    try 
                    {
                        is.close();
                    } 
                    catch (IOException e) 
                    {
                        // Ignore this error
                    }
                }
            }
        }

        return serializationPolicy;
    }
    

}
