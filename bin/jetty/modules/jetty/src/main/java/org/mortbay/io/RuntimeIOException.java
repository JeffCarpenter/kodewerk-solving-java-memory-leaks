// ========================================================================
// Copyright (c) 2009-2009 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// All rights reserved. This program and the accompanying materials
// are made available under the terms of the Eclipse Public License v1.0
// and Apache License v2.0 which accompanies this distribution.
// The Eclipse Public License is available at 
// http://www.eclipse.org/legal/epl-v10.html
// The Apache License v2.0 is available at
// http://www.opensource.org/licenses/apache2.0.php
// You may elect to redistribute this code under either of these licenses. 
// ========================================================================


package org.mortbay.io;

/* ------------------------------------------------------------ */
/**
 * Subclass of {@link java.lang.RuntimeException} used to signal that there
 * was an {@link java.io.IOException} thrown by underlying {@link java.io.Writer}
 */
public class RuntimeIOException extends RuntimeException
{
    public RuntimeIOException()
    {
        super();
    }

    public RuntimeIOException(String message)
    {
        super(message);
    }

    public RuntimeIOException(Throwable cause)
    {
        super(cause);
    }

    public RuntimeIOException(String message, Throwable cause)
    {
        super(message,cause);
    }
}
