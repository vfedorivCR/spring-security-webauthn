/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.sharplab.springframework.security.webauthn.context;

import net.sharplab.springframework.security.webauthn.client.CollectedClientData;
import net.sharplab.springframework.security.webauthn.client.Origin;
import net.sharplab.springframework.security.webauthn.context.validator.OriginValidator;
import net.sharplab.springframework.security.webauthn.exception.BadOriginException;
import net.sharplab.springframework.security.webauthn.test.CoreTestUtil;
import org.junit.Test;

/**
 * Test for OriginValidator
 */
public class OriginValidatorTest {

    private OriginValidator target = new OriginValidator();

    @Test
    public void test() {
        Origin originA = new Origin("https://example.com:14443");
        Origin originB = new Origin("https://example.com:14443");

        CollectedClientData collectedClientData = CoreTestUtil.createClientData();
        collectedClientData.setOrigin(originA);
        RelyingParty relyingParty = new RelyingParty(originB, "example.com", CoreTestUtil.createChallenge());
        target.validate(collectedClientData, relyingParty);
    }

    @Test(expected = BadOriginException.class)
    public void test_with_not_equal_origins() {
        Origin originA = new Origin("https://example.com:14443");
        Origin originB = new Origin("http://example.com");

        CollectedClientData collectedClientData = CoreTestUtil.createClientData();
        collectedClientData.setOrigin(originA);
        RelyingParty relyingParty = new RelyingParty(originB, "example.com", CoreTestUtil.createChallenge());
        target.validate(collectedClientData, relyingParty);
    }

}