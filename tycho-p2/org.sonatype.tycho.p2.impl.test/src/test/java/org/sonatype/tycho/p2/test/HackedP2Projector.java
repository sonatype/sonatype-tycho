package org.sonatype.tycho.p2.test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.internal.p2.core.helpers.Tracing;
import org.eclipse.equinox.internal.p2.director.ApplicablePatchQuery;
import org.eclipse.equinox.internal.p2.director.DirectorActivator;
import org.eclipse.equinox.internal.p2.director.Explanation;
import org.eclipse.equinox.internal.p2.director.Messages;
import org.eclipse.equinox.internal.p2.director.QueryableArray;
import org.eclipse.equinox.internal.p2.director.TwoTierMap;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.internal.provisional.p2.metadata.IInstallableUnitPatch;
import org.eclipse.equinox.internal.provisional.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.internal.provisional.p2.metadata.IRequirementChange;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.CapabilityQuery;
import org.eclipse.equinox.internal.provisional.p2.metadata.query.InstallableUnitQuery;
import org.eclipse.equinox.internal.provisional.p2.query.Collector;
import org.eclipse.equinox.internal.provisional.p2.query.IQueryable;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.InvalidSyntaxException;
import org.sat4j.pb.IPBSolver;
import org.sat4j.pb.SolverFactory;
import org.sat4j.pb.tools.DependencyHelper;
import org.sat4j.pb.tools.WeightedObject;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVec;
import org.sat4j.specs.TimeoutException;

/**
 * This class is the interface between SAT4J and the planner. It produces a
 * boolean satisfiability problem, invokes the solver, and converts the solver result
 * back into information understandable by the planner.
 */
@SuppressWarnings({"restriction", "unchecked"})
public class HackedP2Projector {
    static boolean DEBUG = Tracing.DEBUG_PLANNER_PROJECTOR;
	private static boolean DEBUG_ENCODING = false;
	private IQueryable picker;
	private QueryableArray patches;

	private Map noopVariables; //key IU, value AbstractVariable
	private List abstractVariables;

	private TwoTierMap slice; //The IUs that have been considered to be part of the problem

	private Dictionary selectionContext;

	DependencyHelper dependencyHelper;
	private Collection solution;
	private Collection assumptions;

	private MultiStatus result;

	private Collection alreadyInstalledIUs;

	static class AbstractVariable {
		public String toString() {
			return "AbstractVariable: " + hashCode();
		}
	}

	/**
	 * Job for computing SAT failure explanation in the background.
	 */
	class ExplanationJob extends Job {
		private Set explanation;

		public ExplanationJob() {
			super(Messages.Planner_NoSolution);
			//explanations cannot be canceled directly, so don't show it to the user
			setSystem(true);
		}

		public boolean belongsTo(Object family) {
			return family == ExplanationJob.this;
		}

		public Set getExplanationResult() {
			return explanation;
		}

        protected IStatus run(IProgressMonitor monitor) {
			long start = 0;
			if (DEBUG) {
				start = System.currentTimeMillis();
				Tracing.debug("Determining cause of failure: " + start); //$NON-NLS-1$
			}
			try {
				explanation = dependencyHelper.why();
				if (DEBUG) {
					long stop = System.currentTimeMillis();
					Tracing.debug("Explanation found: " + (stop - start)); //$NON-NLS-1$
					Tracing.debug("Explanation:"); //$NON-NLS-1$
					for (Iterator i = explanation.iterator(); i.hasNext();) {
						Tracing.debug(i.next().toString());
					}
				}
			} catch (TimeoutException e) {
				if (DEBUG)
					Tracing.debug("Timeout while computing explanations"); //$NON-NLS-1$
			} finally {
				//must never have a null result, because caller is waiting on result to be non-null
				if (explanation == null)
					explanation = Collections.EMPTY_SET;
			}
			synchronized (this) {
				ExplanationJob.this.notify();
			}
			return Status.OK_STATUS;
		}

	}

	public HackedP2Projector(IQueryable q, Dictionary context) {
		picker = q;
		noopVariables = new HashMap();
		slice = new TwoTierMap();
		selectionContext = context;
		abstractVariables = new ArrayList();
		result = new MultiStatus(DirectorActivator.PI_DIRECTOR, IStatus.OK, Messages.Planner_Problems_resolving_plan, null);
		assumptions = new ArrayList();
	}

	public void encode(IInstallableUnit metaIu, IInstallableUnit[] alreadyExistingRoots, IInstallableUnit[] newRoots, IProgressMonitor monitor) {
		alreadyInstalledIUs = Arrays.asList(alreadyExistingRoots);
		try {
			long start = 0;
			if (DEBUG) {
				start = System.currentTimeMillis();
				Tracing.debug("Start projection: " + start); //$NON-NLS-1$
			}
			IPBSolver solver;
			if (DEBUG_ENCODING) {
				solver = SolverFactory.newOPBStringSolver();
			} else {
				solver = SolverFactory.newEclipseP2();
			}
			solver.setTimeoutOnConflicts(1000);
			Collector collector = picker.query(InstallableUnitQuery.ANY, new Collector(), null);
			dependencyHelper = new DependencyHelper(solver);

			Iterator iusToEncode = collector.iterator();
			if (DEBUG) {
				List iusToOrder = new ArrayList();
				while (iusToEncode.hasNext()) {
					iusToOrder.add(iusToEncode.next());
				}
				Collections.sort(iusToOrder);
				iusToEncode = iusToOrder.iterator();
			}
			while (iusToEncode.hasNext()) {
				if (monitor.isCanceled()) {
					result.merge(Status.CANCEL_STATUS);
					throw new OperationCanceledException();
				}
				IInstallableUnit iuToEncode = (IInstallableUnit) iusToEncode.next();
				if (iuToEncode != metaIu) {
					processIU(iuToEncode, false);
				}
			}
			createConstraintsForSingleton();

			createMustHave(metaIu, alreadyExistingRoots, newRoots);

			createOptimizationFunction(metaIu);
			if (DEBUG) {
				long stop = System.currentTimeMillis();
				Tracing.debug("Projection complete: " + (stop - start)); //$NON-NLS-1$
			}
			if (DEBUG_ENCODING) {
				System.out.println(solver.toString());
			}
		} catch (IllegalStateException e) {
			result.add(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, e.getMessage(), e));
		} catch (ContradictionException e) {
			result.add(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, Messages.Planner_Unsatisfiable_problem));
		}
	}

	//Create an optimization function favoring the highest version of each IU
	private void createOptimizationFunction(IInstallableUnit metaIu) {

		List weightedObjects = new ArrayList();

		Set s = slice.entrySet();
		final BigInteger POWER = BigInteger.valueOf(2);

		BigInteger maxWeight = POWER;
		for (Iterator iterator = s.iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			HashMap conflictingEntries = (HashMap) entry.getValue();
			if (conflictingEntries.size() == 1) {
				continue;
			}
			List toSort = new ArrayList(conflictingEntries.values());
			Collections.sort(toSort, Collections.reverseOrder());
			BigInteger weight = BigInteger.ONE;
			int count = toSort.size();
			for (int i = 0; i < count; i++) {
				weightedObjects.add(WeightedObject.newWO(toSort.get(i), weight));
				weight = weight.multiply(POWER);
			}
			if (weight.compareTo(maxWeight) > 0)
				maxWeight = weight;
		}

		maxWeight = maxWeight.multiply(POWER);

		// Weight the no-op variables beneath the abstract variables
		for (Iterator iterator = noopVariables.values().iterator(); iterator.hasNext();) {
			weightedObjects.add(WeightedObject.newWO(iterator.next(), maxWeight));
		}

		maxWeight = maxWeight.multiply(POWER);

		// Add the abstract variables
		BigInteger abstractWeight = maxWeight.negate();
		for (Iterator iterator = abstractVariables.iterator(); iterator.hasNext();) {
			weightedObjects.add(WeightedObject.newWO(iterator.next(), abstractWeight));
		}

		maxWeight = maxWeight.multiply(POWER);

		BigInteger optionalWeight = maxWeight.negate();
		long countOptional = 1;
		List requestedPatches = new ArrayList();
		IRequiredCapability[] reqs = metaIu.getRequiredCapabilities();
		for (int j = 0; j < reqs.length; j++) {
			if (!reqs[j].isOptional())
				continue;
			Collector matches = picker.query(new CapabilityQuery(reqs[j]), new Collector(), null);
			for (Iterator iterator = matches.iterator(); iterator.hasNext();) {
				IInstallableUnit match = (IInstallableUnit) iterator.next();
				if (match instanceof IInstallableUnitPatch) {
					requestedPatches.add(match);
					countOptional = countOptional + 1;
				} else
					weightedObjects.add(WeightedObject.newWO(match, optionalWeight));
			}
		}

		BigInteger patchWeight = maxWeight.multiply(POWER).multiply(BigInteger.valueOf(countOptional)).negate();
		for (Iterator iterator = requestedPatches.iterator(); iterator.hasNext();) {
			weightedObjects.add(WeightedObject.newWO(iterator.next(), patchWeight));
		}
		if (!weightedObjects.isEmpty()) {
			createObjectiveFunction(weightedObjects);
		}
	}

	private void createObjectiveFunction(List weightedObjects) {
		if (DEBUG) {
			StringBuffer b = new StringBuffer();
			for (Iterator i = weightedObjects.iterator(); i.hasNext();) {
				WeightedObject object = (WeightedObject) i.next();
				if (b.length() > 0)
					b.append(", "); //$NON-NLS-1$
				b.append(object.getWeight());
				b.append(' ');
				b.append(object.thing);
			}
			Tracing.debug("objective function: " + b); //$NON-NLS-1$
		}
		dependencyHelper.setObjectiveFunction((WeightedObject[]) weightedObjects.toArray(new WeightedObject[weightedObjects.size()]));
	}

	private void createMustHave(IInstallableUnit iu, IInstallableUnit[] alreadyExistingRoots, IInstallableUnit[] newRoots) throws ContradictionException {
		processIU(iu, true);
		if (DEBUG) {
			Tracing.debug(iu + "=1"); //$NON-NLS-1$
		}
		// dependencyHelper.setTrue(variable, new Explanation.IUToInstall(iu));
		assumptions.add(iu);
	}

	private void createNegation(IInstallableUnit iu, IRequiredCapability req) throws ContradictionException {
		if (DEBUG) {
			Tracing.debug(iu + "=0"); //$NON-NLS-1$
		}
		dependencyHelper.setFalse(iu, new Explanation.MissingIU(iu, req));
	}

	// Check whether the requirement is applicable
	private boolean isApplicable(IRequiredCapability req) {
		String filter = req.getFilter();
		if (filter == null)
			return true;
		try {
			return DirectorActivator.context.createFilter(filter).match(selectionContext);
		} catch (InvalidSyntaxException e) {
			return false;
		}
	}

	private boolean isApplicable(IInstallableUnit iu) {
		String enablementFilter = iu.getFilter();
		if (enablementFilter == null)
			return true;
		try {
			return DirectorActivator.context.createFilter(enablementFilter).match(selectionContext);
		} catch (InvalidSyntaxException e) {
			return false;
		}
	}

	private void expandRequirement(IRequiredCapability req, IInstallableUnit iu, List optionalAbstractRequirements, boolean isRootIu) throws ContradictionException {
		if (!isApplicable(req))
			return;
		List matches = getApplicableMatches(req);
		if (!req.isOptional()) {
			if (matches.isEmpty()) {
				missingRequirement(iu, req);
			} else {
				IInstallableUnit reqIu = (IInstallableUnit) picker.query(new CapabilityQuery(req), new Collector(), null).iterator().next();
				Explanation explanation;
				if (isRootIu) {
					if (alreadyInstalledIUs.contains(reqIu)) {
						explanation = new Explanation.IUInstalled(reqIu);
					} else {
						explanation = new Explanation.IUToInstall(reqIu);
					}
				} else {
					explanation = new Explanation.HardRequirement(iu, req);
				}
				createImplication(iu, matches, explanation);
			}
		} else {
			if (!matches.isEmpty()) {
				AbstractVariable abs = getAbstractVariable();
				createImplication(abs, matches, Explanation.OPTIONAL_REQUIREMENT);
				optionalAbstractRequirements.add(abs);
			}
		}
	}

	private void expandRequirements(IRequiredCapability[] reqs, IInstallableUnit iu, boolean isRootIu) throws ContradictionException {
		if (reqs.length == 0) {
			return;
		}
		List optionalAbstractRequirements = new ArrayList();
		for (int i = 0; i < reqs.length; i++) {
			expandRequirement(reqs[i], iu, optionalAbstractRequirements, isRootIu);
		}
		createOptionalityExpression(iu, optionalAbstractRequirements);
	}

	public void processIU(IInstallableUnit iu, boolean isRootIU) throws ContradictionException {
		iu = iu.unresolved();

		slice.put(iu.getId(), iu.getVersion(), iu);
		if (!isApplicable(iu)) {
			createNegation(iu, null);
			return;
		}

		Collector patches = getApplicablePatches(iu);
		expandLifeCycle(iu, isRootIU);
		//No patches apply, normal code path
		if (patches.size() == 0) {
			expandRequirements(iu.getRequiredCapabilities(), iu, isRootIU);
		} else {
			//Patches are applicable to the IU
			expandRequirementsWithPatches(iu, patches, isRootIU);
		}
	}

	private void expandRequirementsWithPatches(IInstallableUnit iu, Collector patches, boolean isRootIu) throws ContradictionException {
		//Unmodified dependencies
		Map unchangedRequirements = new HashMap(iu.getRequiredCapabilities().length);
		for (Iterator iterator = patches.iterator(); iterator.hasNext();) {
			IInstallableUnitPatch patch = (IInstallableUnitPatch) iterator.next();
			IRequiredCapability[][] reqs = mergeRequirements(iu, patch);
			if (reqs.length == 0)
				return;

			// Optional requirements are encoded via:
			// ABS -> (match1(req) or match2(req) or ... or matchN(req))
			// noop(IU)-> ~ABS
			// IU -> (noop(IU) or ABS)
			// Therefore we only need one optional requirement statement per IU
			List optionalAbstractRequirements = new ArrayList();
			for (int i = 0; i < reqs.length; i++) {
				//The requirement is unchanged
				if (reqs[i][0] == reqs[i][1]) {
					if (!isApplicable(reqs[i][0]))
						continue;

					List patchesAppliedElseWhere = (List) unchangedRequirements.get(reqs[i][0]);
					if (patchesAppliedElseWhere == null) {
						patchesAppliedElseWhere = new ArrayList();
						unchangedRequirements.put(reqs[i][0], patchesAppliedElseWhere);
					}
					patchesAppliedElseWhere.add(patch);
					continue;
				}

				//Generate dependency when the patch is applied
				//P1 -> (A -> D) equiv. (P1 & A) -> D
				if (isApplicable(reqs[i][1])) {
					IRequiredCapability req = reqs[i][1];
					List matches = getApplicableMatches(req);
					if (!req.isOptional()) {
						if (matches.isEmpty()) {
							missingRequirement(patch, req);
						} else {
							IInstallableUnit reqIu = (IInstallableUnit) picker.query(new CapabilityQuery(req), new Collector(), null).iterator().next();
							Explanation explanation;
							if (isRootIu) {
								if (alreadyInstalledIUs.contains(reqIu)) {
									explanation = new Explanation.IUInstalled(reqIu);
								} else {
									explanation = new Explanation.IUToInstall(reqIu);
								}
							} else {
								explanation = new Explanation.HardRequirement(iu, req, patch);
							}
							createImplication(new Object[] {patch, iu}, matches, explanation);
						}
					} else {
						if (!matches.isEmpty()) {
							AbstractVariable abs = getAbstractVariable();
							createImplication(new Object[] {patch, abs}, matches, Explanation.OPTIONAL_REQUIREMENT);
							optionalAbstractRequirements.add(abs);
						}
					}
				}
				//Generate dependency when the patch is not applied
				//-P1 -> (A -> B) ( equiv. A -> (P1 or B) )
				if (isApplicable(reqs[i][0])) {
					IRequiredCapability req = reqs[i][0];
					List matches = getApplicableMatches(req);
					if (!req.isOptional()) {
						if (matches.isEmpty()) {
							dependencyHelper.implication(new Object[] {iu}).implies(patch).named(new Explanation.HardRequirement(iu, (IInstallableUnitPatch) null));
						} else {
							matches.add(patch);
							IInstallableUnit reqIu = (IInstallableUnit) picker.query(new CapabilityQuery(req), new Collector(), null).iterator().next();

							Explanation explanation;
							if (isRootIu) {
								if (alreadyInstalledIUs.contains(reqIu)) {
									explanation = new Explanation.IUInstalled(reqIu);
								} else {
									explanation = new Explanation.IUToInstall(reqIu);
								}
							} else {
								explanation = new Explanation.HardRequirement(iu, req);
							}
							createImplication(iu, matches, explanation);
						}
					} else {
						if (!matches.isEmpty()) {
							AbstractVariable abs = getAbstractVariable();
							optionalAbstractRequirements.add(patch);
							createImplication(abs, matches, Explanation.OPTIONAL_REQUIREMENT);
							optionalAbstractRequirements.add(abs);
						}
					}
				}
			}
			createOptionalityExpression(iu, optionalAbstractRequirements);
		}
		List optionalAbstractRequirements = new ArrayList();
		for (Iterator iterator = unchangedRequirements.entrySet().iterator(); iterator.hasNext();) {
			Entry entry = (Entry) iterator.next();
			List patchesApplied = (List) entry.getValue();
			List allPatches = new ArrayList(patches.toCollection());
			allPatches.removeAll(patchesApplied);
			List requiredPatches = new ArrayList();
			for (Iterator iterator2 = allPatches.iterator(); iterator2.hasNext();) {
				IInstallableUnitPatch patch = (IInstallableUnitPatch) iterator2.next();
				requiredPatches.add(patch);
			}
			IRequiredCapability req = (IRequiredCapability) entry.getKey();
			List matches = getApplicableMatches(req);
			if (!req.isOptional()) {
				if (matches.isEmpty()) {
					missingRequirement(iu, req);
				} else {
					if (!requiredPatches.isEmpty())
						matches.addAll(requiredPatches);
					IInstallableUnit reqIu = (IInstallableUnit) picker.query(new CapabilityQuery(req), new Collector(), null).iterator().next();
					Explanation explanation;
					if (isRootIu) {
						if (alreadyInstalledIUs.contains(reqIu)) {
							explanation = new Explanation.IUInstalled(reqIu);
						} else {
							explanation = new Explanation.IUToInstall(reqIu);
						}
					} else {
						explanation = new Explanation.HardRequirement(iu, req);
					}
					createImplication(iu, matches, explanation);
				}
			} else {
				if (!matches.isEmpty()) {
					if (!requiredPatches.isEmpty())
						matches.addAll(requiredPatches);
					AbstractVariable abs = getAbstractVariable();
					createImplication(abs, matches, Explanation.OPTIONAL_REQUIREMENT);
					optionalAbstractRequirements.add(abs);
				}
			}
		}
		createOptionalityExpression(iu, optionalAbstractRequirements);
	}

	private void expandLifeCycle(IInstallableUnit iu, boolean isRootIu) throws ContradictionException {
		if (!(iu instanceof IInstallableUnitPatch))
			return;
		IInstallableUnitPatch patch = (IInstallableUnitPatch) iu;
		IRequiredCapability req = patch.getLifeCycle();
		if (req == null)
			return;
		expandRequirement(req, iu, Collections.EMPTY_LIST, isRootIu);
	}

	private void missingRequirement(IInstallableUnit iu, IRequiredCapability req) throws ContradictionException {
		result.add(new Status(IStatus.WARNING, DirectorActivator.PI_DIRECTOR, NLS.bind(Messages.Planner_Unsatisfied_dependency, iu, req)));
		createNegation(iu, req);
	}

	/**
	 * @param req
	 * @return a list of mandatory requirements if any, an empty list if req.isOptional().
	 */
	private List getApplicableMatches(IRequiredCapability req) {
		List target = new ArrayList();
		Collector matches = picker.query(new CapabilityQuery(req), new Collector(), null);
		for (Iterator iterator = matches.iterator(); iterator.hasNext();) {
			IInstallableUnit match = (IInstallableUnit) iterator.next();
			if (isApplicable(match)) {
				target.add(match);
			}
		}
		return target;
	}

	//Return a new array of requirements representing the application of the patch
	private IRequiredCapability[][] mergeRequirements(IInstallableUnit iu, IInstallableUnitPatch patch) {
		if (patch == null)
			return null;
		IRequirementChange[] changes = patch.getRequirementsChange();
		IRequiredCapability[] originalRequirements = new IRequiredCapability[iu.getRequiredCapabilities().length];
		System.arraycopy(iu.getRequiredCapabilities(), 0, originalRequirements, 0, originalRequirements.length);
		List rrr = new ArrayList();
		boolean found = false;
		for (int i = 0; i < changes.length; i++) {
			for (int j = 0; j < originalRequirements.length; j++) {
				if (originalRequirements[j] != null && changes[i].matches(originalRequirements[j])) {
					found = true;
					if (changes[i].newValue() != null)
						rrr.add(new IRequiredCapability[] {originalRequirements[j], changes[i].newValue()});
					else
						// case where a requirement is removed
						rrr.add(new IRequiredCapability[] {originalRequirements[j], null});
					originalRequirements[j] = null;
				}
				//				break;
			}
			if (!found && changes[i].applyOn() == null && changes[i].newValue() != null) //Case where a new requirement is added
				rrr.add(new IRequiredCapability[] {null, changes[i].newValue()});
		}
		//Add all the unmodified requirements to the result
		for (int i = 0; i < originalRequirements.length; i++) {
			if (originalRequirements[i] != null)
				rrr.add(new IRequiredCapability[] {originalRequirements[i], originalRequirements[i]});
		}
		return (IRequiredCapability[][]) rrr.toArray(new IRequiredCapability[rrr.size()][]);
	}

	/**
	 * Optional requirements are encoded via:
	 * ABS -> (match1(req) or match2(req) or ... or matchN(req))
	 * noop(IU)-> ~ABS
	 * IU -> (noop(IU) or ABS)
	 * @param iu
	 * @param optionalRequirements
	 * @throws ContradictionException
	 */
	private void createOptionalityExpression(IInstallableUnit iu, List optionalRequirements) throws ContradictionException {
		if (optionalRequirements.isEmpty())
			return;
		AbstractVariable noop = getNoOperationVariable(iu);
		for (Iterator i = optionalRequirements.iterator(); i.hasNext();) {
			AbstractVariable abs = (AbstractVariable) i.next();
			createIncompatibleValues(abs, noop);
		}
		optionalRequirements.add(noop);
		createImplication(iu, optionalRequirements, Explanation.OPTIONAL_REQUIREMENT);
	}

	private void createImplication(Object left, List right, Explanation name) throws ContradictionException {
		if (DEBUG) {
			Tracing.debug(name + ": " + left + "->" + right); //$NON-NLS-1$ //$NON-NLS-2$
		}
		dependencyHelper.implication(new Object[] {left}).implies(right.toArray()).named(name);
	}

	private void createImplication(Object[] left, List right, Explanation name) throws ContradictionException {
		if (DEBUG) {
			Tracing.debug(name + ": " + Arrays.asList(left) + "->" + right); //$NON-NLS-1$ //$NON-NLS-2$
		}
		dependencyHelper.implication(left).implies(right.toArray()).named(name);
	}

	//Return IUPatches that are applicable for the given iu
	private Collector getApplicablePatches(IInstallableUnit iu) {
		if (patches == null)
			patches = new QueryableArray((IInstallableUnit[]) picker.query(ApplicablePatchQuery.ANY, new Collector(), null).toArray(IInstallableUnit.class));

		return patches.query(new ApplicablePatchQuery(iu), new Collector(), null);
	}

	//Create constraints to deal with singleton
	//When there is a mix of singleton and non singleton, several constraints are generated
	private void createConstraintsForSingleton() throws ContradictionException {
		Set s = slice.entrySet();
		for (Iterator iterator = s.iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			HashMap conflictingEntries = (HashMap) entry.getValue();
			if (conflictingEntries.size() < 2)
				continue;

			Collection conflictingVersions = conflictingEntries.values();
			List singletons = new ArrayList();
			List nonSingletons = new ArrayList();
			for (Iterator conflictIterator = conflictingVersions.iterator(); conflictIterator.hasNext();) {
				IInstallableUnit iu = (IInstallableUnit) conflictIterator.next();
				if (iu.isSingleton()) {
					singletons.add(iu);
				} else {
					nonSingletons.add(iu);
				}
			}
			if (singletons.isEmpty())
				continue;

			IInstallableUnit[] singletonArray;
			if (nonSingletons.isEmpty()) {
				singletonArray = (IInstallableUnit[]) singletons.toArray(new IInstallableUnit[singletons.size()]);
				createAtMostOne(singletonArray);
			} else {
				singletonArray = (IInstallableUnit[]) singletons.toArray(new IInstallableUnit[singletons.size() + 1]);
				for (Iterator iterator2 = nonSingletons.iterator(); iterator2.hasNext();) {
					singletonArray[singletonArray.length - 1] = (IInstallableUnit) iterator2.next();
					createAtMostOne(singletonArray);
				}
			}
		}
	}

	private void createAtMostOne(IInstallableUnit[] ius) throws ContradictionException {
		if (DEBUG) {
			StringBuffer b = new StringBuffer();
			for (int i = 0; i < ius.length; i++) {
				b.append(ius[i].toString());
			}
			Tracing.debug("At most 1 of " + b); //$NON-NLS-1$
		}
		dependencyHelper.atMost(1, ius).named(new Explanation.Singleton(ius));
	}

	private void createIncompatibleValues(AbstractVariable v1, AbstractVariable v2) throws ContradictionException {
		AbstractVariable[] vars = {v1, v2};
		if (DEBUG) {
			StringBuffer b = new StringBuffer();
			for (int i = 0; i < vars.length; i++) {
				b.append(vars[i].toString());
			}
			Tracing.debug("At most 1 of " + b); //$NON-NLS-1$
		}
		dependencyHelper.atMost(1, vars).named(Explanation.OPTIONAL_REQUIREMENT);
	}

	private AbstractVariable getAbstractVariable() {
		AbstractVariable abstractVariable = new AbstractVariable();
		abstractVariables.add(abstractVariable);
		return abstractVariable;
	}

	private AbstractVariable getNoOperationVariable(IInstallableUnit iu) {
		AbstractVariable v = (AbstractVariable) noopVariables.get(iu);
		if (v == null) {
			v = new AbstractVariable();
			noopVariables.put(iu, v);
		}
		return v;
	}

	public IStatus invokeSolver(IProgressMonitor monitor) {
		if (result.getSeverity() == IStatus.ERROR)
			return result;
		// CNF filename is given on the command line
		long start = System.currentTimeMillis();
		if (DEBUG)
			Tracing.debug("Invoking solver: " + start); //$NON-NLS-1$
		try {
			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;
			if (dependencyHelper.hasASolution(assumptions)) {
				if (DEBUG) {
					Tracing.debug("Satisfiable !"); //$NON-NLS-1$
				}
				backToIU();
				long stop = System.currentTimeMillis();
				if (DEBUG)
					Tracing.debug("Solver solution found: " + (stop - start)); //$NON-NLS-1$
			} else {
				long stop = System.currentTimeMillis();
				if (DEBUG) {
					Tracing.debug("Unsatisfiable !"); //$NON-NLS-1$
					Tracing.debug("Solver solution NOT found: " + (stop - start)); //$NON-NLS-1$
				}
				result.merge(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, Messages.Planner_Unsatisfiable_problem));
			}
		} catch (TimeoutException e) {
			result.merge(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, Messages.Planner_Timeout));
		} catch (Exception e) {
			result.merge(new Status(IStatus.ERROR, DirectorActivator.PI_DIRECTOR, Messages.Planner_Unexpected_problem, e));
		}
		if (DEBUG)
			System.out.println();
		return result;
	}

	private void backToIU() {
		solution = new ArrayList();
		IVec sat4jSolution = dependencyHelper.getSolution();
		for (Iterator i = sat4jSolution.iterator(); i.hasNext();) {
			Object var = i.next();
			if (var instanceof IInstallableUnit) {
				IInstallableUnit iu = (IInstallableUnit) var;
				solution.add(iu);
			}
		}
	}

	private void printSolution(Collection state) {
		ArrayList l = new ArrayList(state);
		Collections.sort(l);
		Tracing.debug("Solution:"); //$NON-NLS-1$
		Tracing.debug("Numbers of IUs selected: " + l.size()); //$NON-NLS-1$
		for (Iterator iterator = l.iterator(); iterator.hasNext();) {
			Tracing.debug(iterator.next().toString());
		}
	}

	public Collection extractSolution() {
		if (DEBUG)
			printSolution(solution);
		return solution;
	}

	public Set getExplanationFor(IInstallableUnit iu) {
		//TODO if the iu is resolved then return null.
		//TODO if the iu is in an unknown state, then return a special value in the set
		try {
			return dependencyHelper.whyNot(iu);
		} catch (TimeoutException e) {
			return Collections.EMPTY_SET;
		}
	}

	public Set getExplanation(IProgressMonitor monitor) {
		ExplanationJob job = new ExplanationJob();
		job.schedule();
		monitor.setTaskName(Messages.Planner_NoSolution);
		IProgressMonitor pm = new InfiniteProgress(monitor);
		pm.beginTask(Messages.Planner_NoSolution, 1000);
		try {
			synchronized (job) {
				while (job.getExplanationResult() == null) {
					if (monitor.isCanceled()) {
						job.cancel();
						throw new OperationCanceledException();
					}
					pm.worked(1);
					try {
						job.wait(100);
					} catch (InterruptedException e) {
						if (DEBUG)
							Tracing.debug("Interrupted while computing explanations"); //$NON-NLS-1$
					}
				}
			}
		} finally {
			monitor.done();
		}
		return job.getExplanationResult();
	}
}