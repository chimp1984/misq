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

package network.misq.offer;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import network.misq.contract.ProtocolType;
import network.misq.network.NetworkId;
import network.misq.offer.options.OfferOption;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@EqualsAndHashCode
@Getter
public abstract class Listing implements Serializable {
    private final String id;
    private final long date;
    private final List<? extends ProtocolType> protocolTypes;
    private final NetworkId makerNetworkId;
    private final Set<OfferOption> offerOptions;

    public Listing(List<? extends ProtocolType> protocolTypes, NetworkId makerNetworkId) {
        this(protocolTypes, makerNetworkId, new HashSet<>());
    }

    public Listing(List<? extends ProtocolType> protocolTypes,
                   NetworkId makerNetworkId, Set<OfferOption> offerOptions) {
        this.offerOptions = offerOptions;
        id = UUID.randomUUID().toString();
        date = System.currentTimeMillis();
        this.protocolTypes = protocolTypes;
        this.makerNetworkId = makerNetworkId;
    }
}