package com.vincentwang.ecommerce.gateway.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LoginControllerTest {

    private final LoginController controller = new LoginController();

    @Test
    void returnsTheSameUuidForEquivalentUsernames() {
        var first = controller.login(new LoginController.LoginRequest(" NorthStar "));
        var second = controller.login(new LoginController.LoginRequest("northstar"));

        assertThat(first.userId()).isEqualTo("0337c503-6233-385f-ab7b-0a4c48e29513");
        assertThat(second.userId()).isEqualTo(first.userId());
        assertThat(first.username()).isEqualTo("NorthStar");
    }
}
