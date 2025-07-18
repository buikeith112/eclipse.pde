/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.launcher;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.junit.launcher.JUnitLaunchShortcut;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDEPreferencesManager;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.launching.ILaunchingPreferenceConstants;
import org.eclipse.pde.internal.launching.IPDEConstants;
import org.eclipse.pde.internal.launching.PDELaunchingPlugin;
import org.eclipse.pde.internal.launching.launcher.BundleLauncherHelper;
import org.eclipse.pde.internal.launching.launcher.LaunchArgumentsHelper;
import org.eclipse.pde.internal.launching.launcher.LauncherUtils;
import org.eclipse.pde.launching.IPDELauncherConstants;
import org.eclipse.pde.launching.PDESourcePathProvider;

/**
 * A launch shortcut capable of launching a Plug-in JUnit test.
 * <p>
 * This class may be instantiated or subclassed by clients.
 * </p>
 *
 * @since 3.3
 */
public class JUnitWorkbenchLaunchShortcut extends JUnitLaunchShortcut {

	@Override
	protected String getLaunchConfigurationTypeId() {
		return "org.eclipse.pde.ui.JunitLaunchConfig"; //$NON-NLS-1$
	}

	@Override
	protected ILaunchConfigurationWorkingCopy createLaunchConfiguration(IJavaElement element) throws CoreException {
		ILaunchConfigurationWorkingCopy configuration = super.createLaunchConfiguration(element);
		String configName = configuration.getName();
		configuration.setAttribute(IPDELauncherConstants.RUN_IN_UI_THREAD, true);

		if (TargetPlatformHelper.usesNewApplicationModel()) {
			configuration.setAttribute(IPDEConstants.LAUNCHER_PDE_VERSION, "3.3"); //$NON-NLS-1$
		} else if (TargetPlatformHelper.getTargetVersion() >= 3.2) {
			configuration.setAttribute(IPDEConstants.LAUNCHER_PDE_VERSION, "3.2a"); //$NON-NLS-1$
		}
		configuration.setAttribute(IPDELauncherConstants.LOCATION, LaunchArgumentsHelper.getDefaultWorkspaceLocation(configName, true));
		configuration.setAttribute(IPDELauncherConstants.DOCLEAR, true);
		configuration.setAttribute(IPDEConstants.DOCLEARLOG, false);
		configuration.setAttribute(IPDELauncherConstants.ASKCLEAR, false);
		configuration.setAttribute(IPDEConstants.APPEND_ARGS_EXPLICITLY, true);

		// Program to launch
		if (LauncherUtils.requiresUI(configuration)) {
			String product = TargetPlatform.getDefaultProduct();
			if (product != null) {
				configuration.setAttribute(IPDELauncherConstants.USE_PRODUCT, true);
				configuration.setAttribute(IPDELauncherConstants.PRODUCT, product);
			}
		} else {
			configuration.setAttribute(IPDELauncherConstants.APPLICATION, IPDEConstants.CORE_TEST_APPLICATION);
		}
		PDEPreferencesManager launchingStore = PDELaunchingPlugin.getDefault().getPreferenceManager();
		if (ILaunchingPreferenceConstants.VALUE_JUNIT_LAUNCH_WITH_TESTPLUGIN
				.equals(launchingStore.getString(ILaunchingPreferenceConstants.PROP_JUNIT_LAUNCH_WITH))) {
			configuration.setAttribute(IPDELauncherConstants.USE_CUSTOM_FEATURES, false);
			IProject project = element.getJavaProject().getProject();
			IPluginModelBase model = PDECore.getDefault().getModelManager()
					.findModel(project);
			configuration.setAttribute(IPDELauncherConstants.USE_DEFAULT, model == null);
			if (model != null) {
				Set<String> wsplugins = new HashSet<>();
				appendPlugin(wsplugins, model);
				configuration.setAttribute(IPDELauncherConstants.SELECTED_WORKSPACE_BUNDLES, wsplugins);
			}
		} else {
			configuration.setAttribute(IPDELauncherConstants.USE_DEFAULT, true);
			configuration.setAttribute(IPDELauncherConstants.USE_CUSTOM_FEATURES, false); // ignored
		}
		configuration.setAttribute(IPDELauncherConstants.AUTOMATIC_INCLUDE_REQUIREMENTS,
				launchingStore.getBoolean(ILaunchingPreferenceConstants.PROP_JUNIT_AUTO_INCLUDE));
		configuration.setAttribute(IPDELauncherConstants.INCLUDE_OPTIONAL,
				launchingStore.getBoolean(ILaunchingPreferenceConstants.PROP_JUNIT_INCLUDE_OPTIONAL));
		configuration.setAttribute(IPDELauncherConstants.AUTOMATIC_ADD,
				launchingStore.getBoolean(ILaunchingPreferenceConstants.PROP_JUNIT_ADD_NEW_WORKSPACE_PLUGINS));
		configuration.setAttribute(IPDELauncherConstants.AUTOMATIC_VALIDATE,
				launchingStore.getBoolean(ILaunchingPreferenceConstants.PROP_JUNIT_VALIDATE_LAUNCH));

		// Program arguments
		String programArgs = LaunchArgumentsHelper.getInitialProgramArguments();
		if (programArgs.length() > 0) {
			configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, programArgs);
		}

		// VM arguments
		String vmArgs = LaunchArgumentsHelper.getInitialVMArguments();
		if (vmArgs.length() > 0) {
			configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, vmArgs);
		}
		configuration.setAttribute(IPDELauncherConstants.BOOTSTRAP_ENTRIES, ""); //$NON-NLS-1$

		// configuration attributes
		configuration.setAttribute(IPDELauncherConstants.CONFIG_GENERATE_DEFAULT, true);
		boolean useDefaultArea = LaunchArgumentsHelper.getDefaultJUnitWorkspaceIsContainer();
		configuration.setAttribute(IPDELauncherConstants.CONFIG_USE_DEFAULT_AREA, useDefaultArea);
		if (!useDefaultArea) {
			configuration.setAttribute(IPDELauncherConstants.CONFIG_LOCATION, LaunchArgumentsHelper.getDefaultJUnitConfigurationLocation());
		}
		configuration.setAttribute(IPDELauncherConstants.CONFIG_CLEAR_AREA, true);

		// tracing option
		configuration.setAttribute(IPDELauncherConstants.TRACING_CHECKED, IPDELauncherConstants.TRACING_NONE);
		configuration.setAttribute(IPDELauncherConstants.TRACING, false);

		// source path provider
		configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER, PDESourcePathProvider.ID);

		return configuration;
	}

	private void appendPlugin(Set<String> plugins, IPluginModelBase model) {
		final StringBuilder builder = new StringBuilder();
		builder.append(model.getPluginBase().getId());
		builder.append(BundleLauncherHelper.VERSION_SEPARATOR);
		builder.append(model.getPluginBase().getVersion());
		plugins.add(builder.toString());
	}

}
