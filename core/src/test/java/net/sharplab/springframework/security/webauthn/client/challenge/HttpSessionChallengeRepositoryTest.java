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

package net.sharplab.springframework.security.webauthn.client.challenge;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import javax.servlet.http.HttpSession;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for HttpSessionChallengeRepository
 */
public class HttpSessionChallengeRepositoryTest {

    private HttpSessionChallengeRepository target = new HttpSessionChallengeRepository();

    @Test
    public void generateChallenge_test() {
        Challenge challenge = target.generateChallenge();
        assertThat(challenge).isNotNull();
        assertThat(challenge.getValue()).hasSize(16);
    }

    @Test
    public void saveChallenge_test() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        String attrName = ".test-challenge";

        target.setSessionAttributeName(attrName);
        Challenge challenge = target.generateChallenge();
        target.saveChallenge(challenge, request, response);

        HttpSession session = request.getSession();
        assertThat((Challenge) session.getAttribute(attrName)).isEqualTo(challenge);
    }

    @Test
    public void saveChallenge_test_with_null() {
        MockHttpSession session = new MockHttpSession();
        MockHttpServletRequest prevRequest = new MockHttpServletRequest();
        prevRequest.setSession(session);
        MockHttpServletResponse prevResponse = new MockHttpServletResponse();

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setSession(session);

        Challenge challenge = target.generateChallenge();
        target.saveChallenge(challenge, prevRequest, prevResponse);
        target.saveChallenge(null, request, response);
        Challenge loadedChallenge = target.loadChallenge(request);

        assertThat(loadedChallenge).isNull();
    }

    @Test
    public void saveChallenge_test_without_prev_request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        Challenge challenge = target.generateChallenge();
        target.saveChallenge(null, request, response);
        Challenge loadedChallenge = target.loadChallenge(request);

        assertThat(loadedChallenge).isNull();
    }


    @Test
    public void loadChallenge_test() {
        MockHttpSession session = new MockHttpSession();
        MockHttpServletRequest prevRequest = new MockHttpServletRequest();
        prevRequest.setSession(session);
        MockHttpServletResponse prevResponse = new MockHttpServletResponse();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        String attrName = ".test-challenge";

        target.setSessionAttributeName(attrName);
        Challenge challenge = target.generateChallenge();
        target.saveChallenge(challenge, prevRequest, prevResponse);
        Challenge loadedChallenge = target.loadChallenge(request);

        assertThat(loadedChallenge).isEqualTo(challenge);
    }

    @Test
    public void loadChallenge_test_without_previous_request() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        Challenge loadedChallenge = target.loadChallenge(request);

        assertThat(loadedChallenge).isNull();
    }
}