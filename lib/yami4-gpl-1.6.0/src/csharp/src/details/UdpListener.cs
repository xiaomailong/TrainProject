// Copyright Paweł Kierski 2010, 2011.
// This file is part of YAMI4.
//
// YAMI4 is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// YAMI4 is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with YAMI4.  If not, see <http://www.gnu.org/licenses/>.

using System.Net.Sockets;
using System.IO;
using System.Net;

namespace Inspirel.YAMI.details
{
    internal class UdpListener : Listener
    {
        private readonly Socket channel;
        private readonly Options options;

        // Parameter incomingMessageDispatchCallback not used here 
        internal UdpListener(
            Socket channel, string resolvedTarget, 
            IncomingMessageDispatchCallback incomingMessageDispatchCallback, 
            Options options, 
            LogCallback logCallback, LogEventArgs.LogLevel logLevel) 
            : base(channel, resolvedTarget, logCallback, logLevel)
        {
            this.channel = channel;
            this.options = options;
        }

        internal override Socket registerForSelection(Selector selector)
        {
            selector.Add(channel, Selector.Direction.READ);
            return channel;
        }

        internal override ListeningResult accept()
        {
        // the concept of "accepting" from a UDP listener is a bit artificial
        // and relates to the fact of receiving any datagram
        // this datagram needs to be injected into proper channel
            MemoryStream buffer = new MemoryStream(options.udpFrameSize);
            EndPoint address = new IPEndPoint(IPAddress.Any, 0);
            int dataRead = 
                channel.ReceiveFrom(buffer.GetBuffer(), ref address);
            if(dataRead < buffer.Length)
            {
                MemoryStream newBuffer = new MemoryStream(dataRead);
                newBuffer.Write(buffer.GetBuffer(), 0, dataRead);
                buffer = newBuffer;
            }

            if (address is IPEndPoint)
            {
                IPEndPoint inetAddress = (IPEndPoint)address;

                string clientTarget = string.Format("{0}{1}:{2}",
                    NetworkUtils.udpPrefix,
                    Dns.GetHostEntry(inetAddress.Address).HostName,
                    inetAddress.Port);

                return new ListeningResult(clientTarget, buffer);

            }
            else
            {
            // ignore messages from non-inet addresses
            // (in practical terms they should never arrive)

                return null;
            }
        }
    }

}
