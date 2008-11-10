package my.group.my.plugin2;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractUIPlugin{
    public static final String PLUGIN_ID = "my.group.my.plugin2";

    public Activator(){
    }

    @Override
    public void start(final BundleContext context) throws Exception{
        super.start(context);
    }

    @Override
    public void stop(final BundleContext context) throws Exception{
        super.stop(context);
    }
}
