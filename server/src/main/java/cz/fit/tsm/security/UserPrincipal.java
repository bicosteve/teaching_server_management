package cz.fit.tsm.security;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import cz.fit.tsm.model.User;
import cz.fit.tsm.model.UserRole;
import cz.fit.tsm.model.UserStatus;

public class UserPrincipal implements UserDetails {

	private final UUID id;
	private final String username;
	private final String passwordHash;
	private final UserRole role;
	private final UserStatus status;

	public UserPrincipal(User user) {
		this.id = user.getId();
		this.username = user.getUsername();
		this.passwordHash = user.getPasswordHash();
		this.role = user.getRole();
		this.status = user.getStatus();
	}

	public UUID getId() {
		return id;
	}

	public UserRole getRole() {
		return role;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return status != UserStatus.INACTIVE;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return status == UserStatus.ACTIVE;
	}
}
