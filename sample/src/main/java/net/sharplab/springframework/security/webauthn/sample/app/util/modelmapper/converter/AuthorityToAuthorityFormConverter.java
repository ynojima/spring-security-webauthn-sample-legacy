package net.sharplab.springframework.security.webauthn.sample.app.util.modelmapper.converter;

import net.sharplab.springframework.security.webauthn.sample.app.web.admin.AuthorityForm;
import net.sharplab.springframework.security.webauthn.sample.domain.model.Authority;
import net.sharplab.springframework.security.webauthn.sample.domain.model.Group;
import net.sharplab.springframework.security.webauthn.sample.domain.model.User;
import org.modelmapper.Converter;
import org.modelmapper.MappingException;
import org.modelmapper.spi.MappingContext;

/**
 * Converter which converts from {@link Authority} to {@link AuthorityForm}
 */
public class AuthorityToAuthorityFormConverter implements Converter<Authority, AuthorityForm> {

    /**
     * Converts the {@link MappingContext#getSource()} to an instance of
     * {@link MappingContext#getDestinationType()}.
     *
     * @param context of current mapping process
     * @throws MappingException if an error occurs while converting
     */
    @Override
    public AuthorityForm convert(MappingContext<Authority, AuthorityForm> context) {
        Authority source = context.getSource();
        AuthorityForm destination = context.getDestination();
        if(destination == null){
            destination = new AuthorityForm();
        }
        destination.setUsers(source.getUsers().stream().mapToInt(User::getId).toArray());
        destination.setGroups(source.getGroups().stream().mapToInt(Group::getId).toArray());
        return destination;
    }
}
