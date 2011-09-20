package net.teration.maven.plugins.jmx;

/*
 * Copyright 2001-2005 The Apache Software Foundation. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

import java.util.Properties;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.InitialContext;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Goal which calls a JMX method.
 * 
 * @goal call
 */
public class JMXExecuteMojo extends AbstractMojo {
    /**
     * URL of the JMX Server
     * 
     * @parameter
     * @required
     */
    private String jmxUrl;

    /**
     * Path of the MBean
     * 
     * @parameter
     * @required
     */
    private String objectName;

    /**
     * Method name
     * 
     * @parameter
     * @required
     */
    private String operation;

    /**
     * Method parameters
     * 
     * @parameter
     */
    private Object[] parameters;

    public void execute() throws MojoExecutionException {
        String realURL;
        if (!jmxUrl.startsWith("service:")) {
            realURL = "service:jmx:rmi:///jndi/rmi://" + jmxUrl + "/jmxrmi";
        } else {
            realURL = jmxUrl;
        }

        try {
            // Get target's URL
            final JMXServiceURL target = new JMXServiceURL(realURL);

            MBeanServerConnection remote;
            JMXConnector connector = null;

            try {
                // Connect to target (assuming no security)
                connector = JMXConnectorFactory.connect(target);
                // Get an MBeanServerConnection on the remote VM.
                remote = connector.getMBeanServerConnection();
            } catch (final Exception e) {
                remote = (MBeanServerConnection) new InitialContext(new Properties() {
                    {
                        put("java.naming.factory.initial",
                            "org.jnp.interfaces.NamingContextFactory");
                        put("java.naming.provider.url", jmxUrl);
                    }
                }).lookup("jmx/rmi/RMIAdaptor");
            }

            String operationName;
            final String[] parameterClasses = new String[0];
            if (operation.contains("(")) {
                operationName = operation.substring(0, operation.indexOf("("));
            } else {
                operationName = operation;
            }

            remote.invoke(new ObjectName(objectName), operationName, parameters, parameterClasses);

            try {
                connector.close();
            } catch (final Exception e) {
            }
        } catch (final Exception e) {
            throw new MojoExecutionException("", e);
        }
    }
}
