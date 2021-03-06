# Spring Security WebAuthn

[![Actions Status](https://github.com/sharplab/spring-security-webauthn/workflows/CI/badge.svg)](https://github.com/sharplab/spring-security-webauthn/actions)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=spring-security-webauthn&metric=coverage)](https://sonarcloud.io/dashboard?id=spring-security-webauthn)
[![license](https://img.shields.io/github/license/sharplab/spring-security-webauthn.svg)](https://github.com/sharplab/spring-security-webauthn/blob/master/LICENSE.txt)


Spring Security WebAuthn provides Web Authentication specification support for your Spring application.
Users can login with WebAuthn compliant authenticator.

**This extension has been sent as [a Pull Request](https://github.com/spring-projects/spring-security/pull/6842) to [Spring-Security upstream](https://github.com/spring-projects/spring-security).
If you are interested in WebAuthn support, check [the Pull Request](https://github.com/spring-projects/spring-security/pull/6842) and join the discussion.**


## Documentation

You can find out more details from the [reference](https://sharplab.github.io/spring-security-webauthn/en/).

## Build

Spring Security WebAuthn uses a Gradle based build system.
In the instructions below, `gradlew` is invoked from the root of the source tree and serves as a cross-platform,
self-contained bootstrap mechanism for the build.

### Prerequisites

- Java8 or later
- Spring Framework 5.0 or later
- Spring Security 5.0 (Customized build)

To support multi factor authentication flow, spring-security-webauthn requires modification to spring-security.
The modification will be sent to spring-security project as a pull-request by the spring-security-webauthn becomes stable, 
but for now, not available with normal spring-security.

### Checkout sources

```
git clone https://github.com/sharplab/spring-security-webauthn
```

### Build all jars

```
./gradlew build
```

### Execute sample application

```
./gradlew samples:javaconfig:webauthn:spa:bootRun
```

![Login view](./docs/src/reference/asciidoc/en/images/login.png "Login view")

## License

Spring Security WebAuthn is Open Source software released under the
[Apache 2.0 license](http://www.apache.org/licenses/LICENSE-2.0.html).
