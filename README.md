# Spring Security WebAuthn Sample Legacy

[![license](https://img.shields.io/github/license/ynojima/spring-security-webauthn-sample-legacy.svg)](https://github.com/ynojima/spring-security-webauthn-sample-legacy/blob/master/LICENSE.txt)

A sample application for Spring Security WebAuthn

## Documentation

You can find out more details from the [reference](https://ynojima.github.io/spring-security-webauthn-sample-legacy/en/).

## Build

Spring Security WebAuthn uses a Gradle based build system.
In the instructions below, `gradlew` is invoked from the root of the source tree and serves as a cross-platform,
self-contained bootstrap mechanism for the build.

### Prerequisites

- Java8 or later

### Checkout sources

```
git clone https://github.com/ynojima/spring-security-webauthn-sample-legacy
```

### Build

```
./gradlew bootJar
```

### Execute sample application

```
./gradlew bootRun
```

![Login view](./docs/src/reference/asciidoc/en/images/login.png "Login view")

## License

Spring Security WebAuthn Sample Legacy is Open Source software released under the
[Apache 2.0 license](http://www.apache.org/licenses/LICENSE-2.0.html).
