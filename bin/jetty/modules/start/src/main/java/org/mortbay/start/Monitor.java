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

package org.mortbay.start;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/*-------------------------------------------*/
/** Monitor thread.
 * This thread listens on the port specified by the STOP.PORT system parameter
 * (defaults to -1 for not listening) for request authenticated with the key given by the STOP.KEY
 * system parameter (defaults to "mortbay") for admin requests. 
 * <p>
 * If the stop port is set to zero, then a random port is assigned and the port number
 * is printed to stdout.
 * <p>
 * Commands "stop" and * "status" are currently supported.
 *
 */
public class Monitor extends Thread
{
    private int _port = Integer.getInteger("STOP.PORT", -1).intValue();
    private String _key = System.getProperty("STOP.KEY", null);

    ServerSocket _socket;
    
    Monitor()
    {
        try
        {
            if(_port<0)
                return;
            setDaemon(true);
	    setName("StopMonitor");
            _socket=new ServerSocket(_port,1,InetAddress.getByName("127.0.0.1"));
            if (_port==0)
            {
                _port=_socket.getLocalPort();
                System.out.println(_port);
            }
            
            if (_key==null)
            {
                _key=Long.toString((long)(Long.MAX_VALUE*Math.random()+this.hashCode()+System.currentTimeMillis()),36);
                System.out.println("-DSTOP.KEY="+_key);
            }
        }
        catch(Exception e)
        {
            if (Main._debug)
                e.printStackTrace();
            else
                System.err.println(e.toString());
        }
        if (_socket!=null)
            this.start();
        else
            System.err.println("WARN: Not listening on monitor port: "+_port);
    }
    
    public void run()
    {
        while (true)
        {
            Socket socket=null;
            try{
                socket=_socket.accept();
                
                LineNumberReader lin=
                    new LineNumberReader(new InputStreamReader(socket.getInputStream()));
                String key=lin.readLine();
                if (!_key.equals(key))
                    continue;
                
                String cmd=lin.readLine();
                if (Main._debug) System.err.println("command="+cmd);
                if ("stop".equals(cmd))
                {
                    try {socket.close();}catch(Exception e){e.printStackTrace();}
                    try {_socket.close();}catch(Exception e){e.printStackTrace();}
                    System.exit(0);
                }
                else if ("status".equals(cmd))
                {
                    socket.getOutputStream().write("OK\r\n".getBytes());
                    socket.getOutputStream().flush();
                }
            }
            catch(Exception e)
            {
                if (Main._debug)
                    e.printStackTrace();
                else
                    System.err.println(e.toString());
            }
            finally
            {
                if (socket!=null)
                {
                    try{socket.close();}catch(Exception e){}
                }
                socket=null;
            }
        }
    }

    /** Start a Monitor.
     * This static method starts a monitor that listens for admin requests.
     */
    public static void monitor()
    {
        new Monitor();
    }
 
}
