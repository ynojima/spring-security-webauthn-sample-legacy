package net.sharplab.springframework.security.webauthn.sample.app.web.admin;

import lombok.Data;
import net.sharplab.springframework.security.webauthn.sample.app.util.validator.EqualProperties;
import net.sharplab.springframework.security.webauthn.sample.app.web.AuthenticatorForm;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Form for User
 */
@Data
@EqualProperties(property = "rawPassword", comparingProperty = "rawPasswordRetyped")
public class UserForm {

    @NotEmpty
    private String userHandle;

    @NotEmpty
    private String  firstName;

    @NotEmpty
    private String  lastName;

    @NotEmpty
    @Email
    private String  emailAddress;

    @NotEmpty
    private String rawPassword;

    @NotEmpty
    private String rawPasswordRetyped;

    @Valid
    private List<AuthenticatorForm> authenticators;

    private boolean locked;
}
