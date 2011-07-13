/*
 * Copyright 2008-2011 Red Hat, Inc, and individual contributors.
 * 
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.torquebox.web.websockets;

import java.util.List;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.torquebox.core.runtime.PoolMetaData;

/** Deploys the default web-sockets runtime pool if required.
 * 
 * <p>
 * While settings from the <code>pooling</code> section of a <code>torquebox.yml</code>
 * are respected, this deployer ensures that a <code>websockets</code> runtime pool
 * is deployed if an application contains any web-socket endpoints.
 * </p>
 * 
 * <p>
 * By default, a shared pool is created, causing all processors for all connections
 * to share a single Ruby interpreter.  This assumes thread-safe code.  Be careful.
 * </p>
 * 
 * @author Bob McWhirter
 */
public class WebSocketsRuntimePoolProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit unit = phaseContext.getDeploymentUnit();
        
        if ( ! unit.hasAttachment( WebSocketMetaData.ATTACHMENTS_KEY ) ) {
            return;
        }
        
        List<PoolMetaData> allMetaData = unit.getAttachmentList( PoolMetaData.ATTACHMENTS_KEY );
        PoolMetaData poolMetaData = PoolMetaData.extractNamedMetaData( allMetaData, "websockets" );
        
        if ( poolMetaData == null ) {
            poolMetaData = new PoolMetaData("websockets");
            poolMetaData.setShared();
            unit.addToAttachmentList( PoolMetaData.ATTACHMENTS_KEY, poolMetaData );
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        // TODO Auto-generated method stub
        
    }

}