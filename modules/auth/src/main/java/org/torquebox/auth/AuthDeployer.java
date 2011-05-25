package org.torquebox.auth;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.security.auth.login.Configuration;

import org.jboss.as.security.plugins.SecurityDomainContext;
import org.jboss.as.security.service.JaasConfigurationService;
import org.jboss.as.security.service.SecurityDomainService;
import org.jboss.as.security.service.SecurityManagementService;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.security.ISecurityManagement;
import org.jboss.security.auth.login.AuthenticationInfo;
import org.jboss.security.config.ApplicationPolicy;
import org.torquebox.auth.AuthMetaData.TorqueBoxAuthConfig;
import org.torquebox.auth.as.AuthServices;
import org.torquebox.auth.as.AuthSubsystemAdd;
import org.torquebox.core.app.RubyApplicationMetaData;

public class AuthDeployer implements DeploymentUnitProcessor {

	@Override
	public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
		DeploymentUnit unit = phaseContext.getDeploymentUnit();
		
		// We need the application name to name our bean with
        RubyApplicationMetaData appMetaData = unit.getAttachment(RubyApplicationMetaData.ATTACHMENT_KEY);
        if (appMetaData != null) {
            String applicationName = appMetaData.getApplicationName();
            this.setApplicationName(applicationName);
            this.addTorqueBoxSecurityDomainService(phaseContext);

            // Install authenticators for every domain
            List<AuthMetaData> allMetaData = unit.getAttachmentList(AuthMetaData.ATTACHMENT_KEY);
            for( AuthMetaData authMetaData: allMetaData ) {
                if ( authMetaData != null ) {
                    Collection<TorqueBoxAuthConfig> authConfigs = authMetaData.getConfigurations();
                    for ( TorqueBoxAuthConfig config : authConfigs ) {
                        installAuthenticator(phaseContext, config);
                    }
                }
            }

        }
	}

	@Override
	public void undeploy(DeploymentUnit unit) {
		// TODO Clean up?
	}
	
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getApplicationName() {
        return this.applicationName;
    }

	private void addTorqueBoxSecurityDomainService(DeploymentPhaseContext context) {
		String domain = AuthSubsystemAdd.TORQUEBOX_DOMAIN + "-" + this.getApplicationName();
		log.info( "Adding torquebox security domain: " + domain);
		final ApplicationPolicy applicationPolicy = new ApplicationPolicy(domain);
		AuthenticationInfo authenticationInfo = new AuthenticationInfo(domain);
		
		// TODO: Can we feed usernames/passwords into the options hash?
		Map<String, Object> options = new HashMap<String, Object>();
		Map<String, String> credentials = new HashMap<String, String>();
		credentials.put("foo", "bar");
		options.put("credentials", credentials);
		AppConfigurationEntry entry = new AppConfigurationEntry(TorqueBoxLoginModule.class.getName(), LoginModuleControlFlag.REQUIRED, options);
		authenticationInfo.addAppConfigurationEntry(entry);
		applicationPolicy.setAuthenticationInfo(authenticationInfo);
		
		// TODO: Do we need to bother with a JSSESecurityDomain? Null in this case may be OK
		// TODO: Null cache type?
		final SecurityDomainService securityDomainService = new SecurityDomainService(domain, applicationPolicy, null, null); 
		final ServiceTarget target = context.getServiceTarget();
		
		ServiceBuilder<SecurityDomainContext> builder = target
		.addService(SecurityDomainService.SERVICE_NAME.append(domain), securityDomainService)
		.addDependency(SecurityManagementService.SERVICE_NAME, ISecurityManagement.class,
		        securityDomainService.getSecurityManagementInjector())
		.addDependency(JaasConfigurationService.SERVICE_NAME, Configuration.class,
		        securityDomainService.getConfigurationInjector());
		
		builder.setInitialMode(Mode.ON_DEMAND).install();
		log.info( "Finished adding torquebox security domain: " + domain);
	}

    private void installAuthenticator(DeploymentPhaseContext phaseContext, TorqueBoxAuthConfig config) {
        String name     = config.getName();
        String domain   = config.getDomain();
        if (name != null && domain != null) {
        	if (domain.equals(AuthSubsystemAdd.TORQUEBOX_DOMAIN)) {
        		// activate the service
        		log.info("Activating SecurityDomainService for " + domain);
        		ServiceController<?> torqueboxService = phaseContext.getServiceRegistry().getService(SecurityDomainService.SERVICE_NAME.append(AuthSubsystemAdd.TORQUEBOX_DOMAIN));
        		if (torqueboxService != null) torqueboxService.setMode(Mode.ACTIVE);
        	}
            ServiceName serviceName = AuthServices.authenticationService( this.getApplicationName(), name );
            log.info( "Deploying Authenticator: " + serviceName + " for security domain: " + domain);
            Authenticator authenticator = new Authenticator();
            authenticator.setAuthDomain(domain);
            ServiceBuilder<Authenticator> builder = phaseContext.getServiceTarget().addService( serviceName, authenticator );
            builder.setInitialMode( Mode.PASSIVE );
            builder.install();
        }
    }
    
    private String applicationName;
    private static final Logger log = Logger.getLogger( "org.torquebox.auth" );
}
