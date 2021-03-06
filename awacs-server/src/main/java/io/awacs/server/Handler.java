/**
 * Copyright 2016-2017 AWACS Project.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.awacs.server;

import io.awacs.common.Releasable;
import io.awacs.common.net.Packet;

import java.net.InetSocketAddress;

/**
 * Created by pixyonly on 03/09/2017.
 */
public interface Handler extends Releasable {

    Packet onReceive(Packet recieve, InetSocketAddress remote);

    byte key();
}
