/*******************************************************************************
 * Copyright 2013 The MITRE Corporation 
 *   and the MIT Kerberos and Internet Trust Consortium
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.mitre.jwt.encryption.service.impl;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.mitre.jose.keystore.JWKSetKeyStore;
import org.mitre.jwt.encryption.service.JwtEncryptionAndDecryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEDecrypter;
import com.nimbusds.jose.JWEEncrypter;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jose.crypto.RSAEncrypter;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.EncryptedJWT;

/**
 * @author wkim
 *
 */
public class DefaultJwtEncryptionAndDecryptionService implements JwtEncryptionAndDecryptionService {

	private static Logger logger = LoggerFactory.getLogger(DefaultJwtEncryptionAndDecryptionService.class);

	// map of identifier to encrypter
	private Map<String, JWEEncrypter> encrypters = new HashMap<String, JWEEncrypter>();

	// map of identifier to decrypter
	private Map<String, JWEDecrypter> decrypters = new HashMap<String, JWEDecrypter>();

	private String defaultEncryptionKeyId;
	
	private String defaultDecryptionKeyId;

	private JWEAlgorithm defaultAlgorithm;

	// map of identifier to key
	private Map<String, JWK> keys = new HashMap<String, JWK>();

	/**
	 * Build this service based on the keys given. All public keys will be used to make encrypters, 
	 * all private keys will be used to make decrypters.
	 * 
	 * @param keys
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 * @throws JOSEException
	 */
	public DefaultJwtEncryptionAndDecryptionService(Map<String, JWK> keys) throws NoSuchAlgorithmException, InvalidKeySpecException, JOSEException {
		this.keys = keys;
		buildEncryptersAndDecrypters();
	}
	
	/**
	 * Build this service based on the given keystore. All keys must have a key
	 * id ({@code kid}) field in order to be used.
	 * 
	 * @param keyStore
	 * @throws NoSuchAlgorithmException
	 * @throws InvalidKeySpecException
	 * @throws JOSEException
	 */
	public DefaultJwtEncryptionAndDecryptionService(JWKSetKeyStore keyStore) throws NoSuchAlgorithmException, InvalidKeySpecException, JOSEException {
		
		// convert all keys in the keystore to a map based on key id
		for (JWK key : keyStore.getKeys()) {
			if (!Strings.isNullOrEmpty(key.getKeyID())) {
				this.keys.put(key.getKeyID(), key);
			} else {
				throw new IllegalArgumentException("Tried to load a key from a keystore without a 'kid' field: " + key);
			}
		}
		buildEncryptersAndDecrypters();
	}
	

	@PostConstruct
	public void afterPropertiesSet() throws NoSuchAlgorithmException, InvalidKeySpecException{

		if (keys == null) {
			throw new IllegalArgumentException("Encryption and decryption service must have at least one key configured.");
		}
		
		// TODO call build..() again? see default signer service.
	}

	public String getDefaultEncryptionKeyId() {
		return defaultEncryptionKeyId;
	}

	public void setDefaultEncryptionKeyId(String defaultEncryptionKeyId) {
		this.defaultEncryptionKeyId = defaultEncryptionKeyId;
	}

	public String getDefaultDecryptionKeyId() {
		return defaultDecryptionKeyId;
	}

	public void setDefaultDecryptionKeyId(String defaultDecryptionKeyId) {
		this.defaultDecryptionKeyId = defaultDecryptionKeyId;
	}

	public JWEAlgorithm getDefaultAlgorithm() {
		return defaultAlgorithm;
	}

	public void setDefaultAlgorithm(JWEAlgorithm defaultAlgorithm) {
		this.defaultAlgorithm = defaultAlgorithm;
	}

	/* (non-Javadoc)
	 * @see org.mitre.jwt.encryption.service.JwtEncryptionAndDecryptionService#encryptJwt(com.nimbusds.jwt.EncryptedJWT)
	 */
	@Override
	public void encryptJwt(EncryptedJWT jwt) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.mitre.jwt.encryption.service.JwtEncryptionAndDecryptionService#decryptJwt(com.nimbusds.jwt.EncryptedJWT)
	 */
	@Override
	public void decryptJwt(EncryptedJWT jwt) {
		// TODO Auto-generated method stub

	}

	/**
	 * Builds all the encrypters and decrypters for this service based on the key map.
	 * @throws  
	 * @throws InvalidKeySpecException 
	 * @throws NoSuchAlgorithmException 
	 * @throws JOSEException 
	 */
	private void buildEncryptersAndDecrypters() throws NoSuchAlgorithmException, InvalidKeySpecException, JOSEException {

		for (Map.Entry<String, JWK> jwkEntry : keys.entrySet()) {

			String id = jwkEntry.getKey();
			JWK jwk = jwkEntry.getValue();

			if (jwk instanceof RSAKey) {
				// build RSA encrypters and decrypters

				RSAEncrypter encrypter = new RSAEncrypter(((RSAKey) jwk).toRSAPublicKey()); // there should always at least be the public key
				encrypters.put(id, encrypter);

				if (jwk.isPrivate()) { // we can decrypt!
					RSADecrypter decrypter = new RSADecrypter(((RSAKey) jwk).toRSAPrivateKey());
					decrypters.put(id, decrypter);
				} else {
					logger.warn("No private key for key #" + jwk.getKeyID());
				}

				// TODO: add support for EC keys

			} else {
				logger.warn("Unknown key type: " + jwk);
			}

		}
	}

	@Override
	public Map<String, JWK> getAllPublicKeys() {
		Map<String, JWK> pubKeys = new HashMap<String, JWK>();

		// pull out all public keys
		for (String keyId : keys.keySet()) {
			JWK key = keys.get(keyId);
			JWK pub = key.toPublicJWK();
			if (pub != null) {
				pubKeys.put(keyId, pub);
			}
		}

		return pubKeys;
	}

	@Override
	public Collection<JWEAlgorithm> getAllEncryptionAlgsSupported() {
		Set<JWEAlgorithm> algs = new HashSet<JWEAlgorithm>();

		for (JWEEncrypter enc : encrypters.values()) {
			algs.addAll(enc.supportedAlgorithms());
		}

		for (JWEDecrypter dec : decrypters.values()) {
			algs.addAll(dec.supportedAlgorithms());
		}

		return algs;
	}

}