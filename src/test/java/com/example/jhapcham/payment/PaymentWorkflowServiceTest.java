package com.example.jhapcham.payment;

import com.example.jhapcham.Error.BusinessValidationException;
import com.example.jhapcham.order.domain.Order;
import com.example.jhapcham.payment.application.PaymentWorkflowService;
import com.example.jhapcham.payment.persistence.PaymentEventRepository;
import com.example.jhapcham.payment.persistence.PaymentRepository;
import com.example.jhapcham.user.domain.Role;
import com.example.jhapcham.user.domain.User;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class PaymentWorkflowServiceTest {

    private final PaymentWorkflowService workflow = new PaymentWorkflowService(
            mock(PaymentRepository.class),
            mock(PaymentEventRepository.class),
            mock(com.example.jhapcham.order.persistence.OrderRepository.class));

    @Test
    void adminCanPayAnyOrder() {
        User admin = user(1L, Role.ADMIN);
        Order order = new Order();
        order.setId(99L);

        workflow.assertCustomerPaymentOwnership(List.of(order), admin);
    }

    @Test
    void customerCannotPayAnotherCustomersOrder() {
        User owner = user(1L, Role.CUSTOMER);
        User actor = user(2L, Role.CUSTOMER);
        Order order = new Order();
        order.setId(99L);
        order.setUser(owner);

        assertThatThrownBy(() -> workflow.assertCustomerPaymentOwnership(List.of(order), actor))
                .isInstanceOf(BusinessValidationException.class);
    }

    private User user(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        return user;
    }
}
