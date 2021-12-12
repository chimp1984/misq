/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package network.misq.network.p2p.services.mesh.peers;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import network.misq.network.p2p.node.Address;
import network.misq.network.p2p.node.Capability;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

@Getter
@ToString
@EqualsAndHashCode(exclude = "created")
public class Peer implements Serializable {
    private final Capability capability;
    private final boolean isOutboundConnection;
    private final long created;

    public Peer(Capability capability, boolean isOutboundConnection) {
        this.capability = capability;
        this.isOutboundConnection = isOutboundConnection;
        this.created = System.currentTimeMillis();
    }

    public Date getDate() {
        return new Date(created);
    }

    public Address getAddress() {
        return capability.address();
    }

    public long getAge() {
        return new Date().getTime() - created;
    }
}