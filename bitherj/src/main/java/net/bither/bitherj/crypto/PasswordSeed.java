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

package net.bither.bitherj.crypto;


import net.bither.bitherj.core.Address;
import net.bither.bitherj.exception.AddressFormatException;
import net.bither.bitherj.utils.Base58;
import net.bither.bitherj.utils.PrivateKeyUtil;
import net.bither.bitherj.qrcode.QRCodeUtil;
import net.bither.bitherj.utils.Utils;


public class PasswordSeed {
    private String address;
    private String keyStr;


    public PasswordSeed(String str) {
        int indexOfSplit = QRCodeUtil.indexOfOfPasswordSeed(str);
        this.address = QRCodeUtil.getAddressFromPasswordSeed(str);
        this.keyStr = str.substring(indexOfSplit + 1);
    }

    public PasswordSeed(Address address) {
        this.address = address.getAddress();
        this.keyStr = address.getEncryptPrivKey();
    }

    public boolean checkPassword(CharSequence password) {
        ECKey ecKey = PrivateKeyUtil.getECKeyFromSingleString(keyStr, password);
        String ecKeyAddress;
        if (ecKey == null) {
            return false;
        } else {
            ecKeyAddress = ecKey.toAddress();
            ecKey.clearPrivateKey();
        }
        return Utils.compareString(this.address,
                ecKeyAddress);

    }

    public ECKey getECKey(CharSequence password) {
        return PrivateKeyUtil.getECKeyFromSingleString(keyStr, password);
    }

    public String getAddress() {
        return this.address;
    }

    public String toPasswordSeedString() {
        try {
            String passwordSeedString = Base58.bas58ToHexWithAddress(this.address) + QRCodeUtil.QR_CODE_SPLIT
                    + QRCodeUtil.getNewVersionEncryptPrivKey(this.keyStr);
            return passwordSeedString;
        } catch (AddressFormatException e) {
            throw new RuntimeException("passwordSeed  address is format error ," + this.address);

        }

    }

}
