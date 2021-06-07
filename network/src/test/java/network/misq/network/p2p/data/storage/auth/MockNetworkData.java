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

package network.misq.network.p2p.data.storage.auth;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import network.misq.network.p2p.data.NetworkData;
import network.misq.network.p2p.data.storage.MetaData;

import java.util.concurrent.TimeUnit;

@EqualsAndHashCode
@Getter
public class MockNetworkData implements NetworkData {
    private final String text;
    final MetaData metaData;

    public MockNetworkData(String text) {
        this.text = text;
        // 463 is overhead of sig/pubkeys,...
        // 582 is pubkey+sig+hash
        metaData = new MetaData(TimeUnit.DAYS.toMillis(10), 251 + 463, getClass().getSimpleName());
    }

    @Override
    public MetaData getMetaData() {
        return metaData;
    }

    @Override
    public boolean isDataInvalid() {
        return false;
    }
}