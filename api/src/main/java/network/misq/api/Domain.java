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

package network.misq.api;

import lombok.Getter;
import network.misq.api.options.KeyPairRepositoryOptionsParser;
import network.misq.api.options.NetworkServiceOptionsParser;
import network.misq.application.options.ApplicationOptions;
import network.misq.common.util.CollectionUtil;
import network.misq.id.IdentityRepository;
import network.misq.network.NetworkService;
import network.misq.network.p2p.MockNetworkService;
import network.misq.offer.OfferRepository;
import network.misq.offer.OpenOfferRepository;
import network.misq.presentation.offer.OfferEntityRepository;
import network.misq.security.KeyPairRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Creates domain specific options from program arguments and application options.
 * Creates domain instance with options and optional dependency to other domain objects.
 * Initializes the domain instances according to the requirements of their dependencies either in sequence
 * or in parallel.
 * Provides the completely setup instances to other clients (Api)
 */
@Getter
public class Domain {
    private final KeyPairRepository keyPairRepository;
    private final NetworkService networkService;
    private final OfferRepository offerRepository;
    private final OpenOfferRepository openOfferRepository;
    private final OfferEntityRepository offerEntityRepository;
    private final IdentityRepository identityRepository;

    public Domain(ApplicationOptions applicationOptions, String[] args) {
        KeyPairRepository.Options keyPairRepositoryOptions = new KeyPairRepositoryOptionsParser(applicationOptions, args).getOptions();
        keyPairRepository = new KeyPairRepository(keyPairRepositoryOptions);

        NetworkService.Options networkServiceOptions = new NetworkServiceOptionsParser(applicationOptions, args).getOptions();
        networkService = new NetworkService(networkServiceOptions, keyPairRepository);

        identityRepository = new IdentityRepository(networkService);

        // add data use case is not available yet at networkService
        MockNetworkService mockNetworkService = new MockNetworkService();
        offerRepository = new OfferRepository(mockNetworkService);
        openOfferRepository = new OpenOfferRepository(mockNetworkService);

        offerEntityRepository = new OfferEntityRepository(offerRepository, networkService);
    }

    /**
     * Initializes all domain objects.
     * We do in parallel as far as possible. If there are dependencies we chain those as sequence.
     */
    public CompletableFuture<Boolean> initialize() {
        List<CompletableFuture<Boolean>> allFutures = new ArrayList<>();
        // Assuming identityRepository depends on keyPairRepository being initialized... 
        allFutures.add(keyPairRepository.initialize().thenCompose(success -> identityRepository.initialize()));
        allFutures.add(networkService.initialize());
        allFutures.add(offerRepository.initialize());
        allFutures.add(openOfferRepository.initialize());
        allFutures.add(offerEntityRepository.initialize());
        // Once all have successfully completed our initialize is complete as well
        return CollectionUtil.allOf(allFutures)
                .thenApply(success -> success.stream().allMatch(e -> e))
                .orTimeout(10, TimeUnit.SECONDS)
                .thenCompose(CompletableFuture::completedFuture);
    }
}