/*
 * Copyright 2014 http://Bither.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.bither.bitherj.sample;

import net.bither.bitherj.BitherjApplication;
import net.bither.bitherj.ISetting;
import net.bither.bitherj.core.BitherjSettings;
import net.bither.bitherj.IRandom;

public class BitherjTestApplication extends BitherjApplication {
    private boolean doneSyncFromSpv;

    @Override
    public ISetting initSetting() {
        return new ISetting() {
            @Override
            public BitherjSettings.AppMode getAppMode() {
                return BitherjSettings.AppMode.HOT;
            }

            @Override
            public boolean getBitherjDoneSyncFromSpv() {
                return isDoneSyncFromSpv();
            }

            @Override
            public void setBitherjDoneSyncFromSpv(boolean isDone) {
                doneSyncFromSpv = isDone;
            }

            @Override
            public BitherjSettings.TransactionFeeMode getTransactionFeeMode() {
                return BitherjSettings.TransactionFeeMode.Low;
            }
        };
    }

    @Override
    public IRandom initRandom() {
        return null;
    }

    public void setDoneSyncFromSpv(boolean done) {
        doneSyncFromSpv = done;
    }

    public boolean isDoneSyncFromSpv() {
        return doneSyncFromSpv;
    }
}
