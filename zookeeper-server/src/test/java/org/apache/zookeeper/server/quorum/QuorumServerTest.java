/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership.  The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.zookeeper.server.quorum;

import static org.junit.Assert.assertEquals;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZKTestCase;
import org.apache.zookeeper.server.quorum.QuorumPeer.QuorumServer;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig.ConfigException;
import org.junit.Assert;
import org.junit.Test;

import java.net.InetSocketAddress;

public class QuorumServerTest extends ZKTestCase {
    private String ipv6n1 = "[2500:0:0:0:0:0:1:0]";
    private String ipv6n2 = "[2600:0:0:0:0:0:1:0]";
    private String ipv4config = "127.0.0.1:1234:1236";

    @Test
    public void testToString() throws ConfigException {
        String provided = ipv4config + ":participant;0.0.0.0:1237";
        String expected = ipv4config + ":participant;0.0.0.0:1237";
        QuorumServer qs = new QuorumServer(0, provided);
        Assert.assertEquals("Use IP address", expected, qs.toString());

        provided = ipv4config + ";0.0.0.0:1237";
        expected = ipv4config + ":participant;0.0.0.0:1237";
        qs = new QuorumServer(0, provided);
        Assert.assertEquals("Type unspecified", expected, qs.toString());

        provided = ipv4config + ":observer;0.0.0.0:1237";
        expected = ipv4config + ":observer;0.0.0.0:1237";
        qs = new QuorumServer(0, provided);
        Assert.assertEquals("Observer type", expected, qs.toString());

        provided = ipv4config + ":participant;1237";
        expected = ipv4config + ":participant;0.0.0.0:1237";
        qs = new QuorumServer(0, provided);
        Assert.assertEquals("Client address unspecified",
                            expected, qs.toString());

        provided = ipv4config + ":participant;1.2.3.4:1237";
        expected = ipv4config + ":participant;1.2.3.4:1237";
        qs = new QuorumServer(0, provided);
        Assert.assertEquals("Client address specified",
                            expected, qs.toString());

        provided = "example.com:1234:1236:participant;1237";
        expected = "example.com:1234:1236:participant;0.0.0.0:1237";
        qs = new QuorumServer(0, provided);
        Assert.assertEquals("Use hostname", expected, qs.toString());
    }

    @Test
    public void constructionUnderstandsIpv6LiteralsInServerConfig() throws ConfigException {
        String config = "[::1]:1234:1236:participant";
        QuorumServer qs = new QuorumServer(0, config);
        assertEquals("[0:0:0:0:0:0:0:1]:1234:1236:participant", qs.toString());
    }

    @Test
    public void constructionUnderstandsIpv6LiteralsInClientConfig() throws ConfigException {
        String config = ipv4config + ":participant;[::1]:1237";
        QuorumServer qs = new QuorumServer(0, config);
        assertEquals(ipv4config + ":participant;[0:0:0:0:0:0:0:1]:1237", qs.toString());
    }

    @Test(expected = ConfigException.class)
    public void unbalancedIpv6LiteralsInServerConfigFailToBeParsed() throws ConfigException {
        new QuorumServer(0, "[::1:1234:1236:participant");
    }

    @Test(expected = ConfigException.class)
    public void unbalancedIpv6LiteralsInClientConfigFailToBeParsed() throws ConfigException {
        new QuorumServer(0, ipv4config + ":participant;[::1:1237");
    }

    @Test
    public void testWildcard() throws KeeperException.BadArgumentsException {
        String[] addrs = new String[]{"127.0.0.1", "[0:0:0:0:0:0:0:1]", "0.0.0.0", "[::]"};
        for (int i = 0; i < addrs.length; i++) {
            for (int j = i; j < addrs.length; j++) {
                QuorumPeer.QuorumServer server1 =
                        new QuorumPeer.QuorumServer(1,
                                new InetSocketAddress(ipv6n1, 1234), // peer
                                new InetSocketAddress(ipv6n1, 1236), // election
                                new InetSocketAddress(addrs[i], 1237)  // client
                        );
                QuorumPeer.QuorumServer server2 =
                        new QuorumPeer.QuorumServer(2,
                                new InetSocketAddress(ipv6n2, 1234), // peer
                                new InetSocketAddress(ipv6n2, 1236), // election
                                new InetSocketAddress(addrs[j], 1237)  // client
                        );
                server1.checkAddressDuplicate(server2);
            }
        }
    }

    @Test(expected = KeeperException.BadArgumentsException.class)
    public void testDuplicate() throws KeeperException.BadArgumentsException {
        QuorumPeer.QuorumServer server1 =
                new QuorumPeer.QuorumServer(1,
                        new InetSocketAddress(ipv6n1, 1234), // peer
                        new InetSocketAddress(ipv6n1, 1236), // election
                        new InetSocketAddress(ipv6n1, 1237)  // client
                );
        QuorumPeer.QuorumServer server2 =
                new QuorumPeer.QuorumServer(2,
                        new InetSocketAddress(ipv6n2, 1234), // peer
                        new InetSocketAddress(ipv6n2, 1236), // election
                        new InetSocketAddress(ipv6n1, 1237)  // client
                );
        server1.checkAddressDuplicate(server2);
    }
}
