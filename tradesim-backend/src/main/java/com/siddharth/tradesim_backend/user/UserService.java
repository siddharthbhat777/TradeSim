package com.siddharth.tradesim_backend.user;

import com.siddharth.tradesim_backend.auth.AuthRepository;
import com.siddharth.tradesim_backend.auth.enums.AccountStatus;
import com.siddharth.tradesim_backend.auth.enums.Role;
import com.siddharth.tradesim_backend.auth.model.User;
import com.siddharth.tradesim_backend.common.exceptions.BusinessException;
import com.siddharth.tradesim_backend.company.enums.CompanyRepresentativeAssignmentStatus;
import com.siddharth.tradesim_backend.company.repository.CompanyRepresentativeAssignmentRepository;
import com.siddharth.tradesim_backend.order.enums.OrderStatus;
import com.siddharth.tradesim_backend.order.model.Order;
import com.siddharth.tradesim_backend.order.repository.OrderRepository;
import com.siddharth.tradesim_backend.order.service.OrderLifecycleService;
import com.siddharth.tradesim_backend.user.dto.ChangeUserRoleResponse;
import com.siddharth.tradesim_backend.user.dto.ChangeUserStatusResponse;
import com.siddharth.tradesim_backend.user.exceptions.RoleException;
import com.siddharth.tradesim_backend.user.exceptions.StatusException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final AuthRepository authRepository;
    private final OrderRepository orderRepository;
    private final OrderLifecycleService orderLifecycleService;
    private final CompanyRepresentativeAssignmentRepository companyRepresentativeAssignmentRepository;

    @Transactional
    public ChangeUserStatusResponse changeStatus(UUID userId, AccountStatus status) {
        User user = authRepository.findById(userId).orElseThrow(() -> new BusinessException("User not found"));
        if (user.getAccountStatus().equals(AccountStatus.BANNED)) throw new StatusException("Cannot change status of banned user");
        if (status.equals(AccountStatus.DEACTIVATED)) throw new StatusException("Account can only be deactivated by account owner");
        try {
            if (status.equals(AccountStatus.BANNED)) {
                List<Order> openOrders = orderRepository.findByUserIdAndStatusIn(userId, List.of(OrderStatus.OPEN, OrderStatus.PARTIALLY_FILLED));

                for (Order order : openOrders) {
                    orderLifecycleService.cancelOrder(order);
                }
            }
            user.setAccountStatus(status);
            authRepository.save(user);
            return new ChangeUserStatusResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getRole(),
                    user.getAccountStatus()
            );
        } catch (DataIntegrityViolationException e) {
            throw new StatusException("Invalid status data");
        } catch (Exception e) {
            throw new StatusException("Unable to change status");
        }
    }

    @Transactional
    public ChangeUserRoleResponse changeRole(UUID userId, Role role) {
        User user = authRepository.findById(userId).orElseThrow(() -> new BusinessException("User not found"));

        if (user.getRole() == Role.ADMIN) {
            throw new RoleException("Cannot change role of admin user");
        }

        if (role == Role.ADMIN) {
            throw new RoleException("Admin role cannot be assigned through this endpoint");
        }

        if (user.getRole() == role) {
            throw new RoleException("User already has this role");
        }

        if (user.getRole() == Role.COMPANY_REPRESENTATIVE && role == Role.USER && companyRepresentativeAssignmentRepository.existsByUserIdAndStatus(userId, CompanyRepresentativeAssignmentStatus.ACTIVE)) {
            throw new RoleException("Revoke active company representative assignments before changing role");
        }

        try {
            user.setRole(role);
            authRepository.save(user);

            return new ChangeUserRoleResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getRole(),
                    user.getAccountStatus()
            );
        } catch (DataIntegrityViolationException e) {
            throw new RoleException("Invalid role data");
        } catch (Exception e) {
            throw new RoleException("Unable to change role");
        }
    }
}