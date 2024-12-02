package org.joget.marketplace;

import java.util.ArrayList;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    protected ArrayList<ServiceRegistration<?>> registrationList;

    public void start(BundleContext context) {
        registrationList = new ArrayList<>();

        registrationList.add(context.registerService(NumberValidator.class.getName(), new NumberValidator(), null));

    }

    public void stop(BundleContext context) {
        for (ServiceRegistration<?> registration : registrationList) {
            registration.unregister();
        }
    }
}