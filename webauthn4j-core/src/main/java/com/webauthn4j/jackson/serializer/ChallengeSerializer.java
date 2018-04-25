package com.webauthn4j.jackson.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.webauthn4j.client.challenge.Challenge;

import java.io.IOException;
import java.util.Base64;

public class ChallengeSerializer extends StdSerializer<Challenge> {

    public ChallengeSerializer() {
        super(Challenge.class);
    }

    @Override
    public void serialize(Challenge value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        String challenge = Base64.getUrlEncoder().encodeToString(value.getValue());
        gen.writeString(challenge);
    }
}