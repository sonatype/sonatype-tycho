package tycho.demo.itp01.tests;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.junit.Test;

import tycho.demo.itp01.actions.SampleAction;

public class ITP01Test {

	@Test
	public void sampleAction() {
		IWorkbench workbench = PlatformUI.getWorkbench();

		SampleAction action = new SampleAction();
		action.init(workbench.getActiveWorkbenchWindow());

//		action.run(null);
	}

}
