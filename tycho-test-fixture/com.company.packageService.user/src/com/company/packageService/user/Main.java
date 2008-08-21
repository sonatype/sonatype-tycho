package com.company.packageService.user;

import com.company.packageService.PackageProvider;
import com.company.packageService.FragmentProvider;

public class Main {
	public static void main(String[] args) {
		String result = PackageProvider.providerName();
		if (null == result) {
			throw new Error("null PackageProvider result");
		}
		result = FragmentProvider.providerName();
		// FragmentProvider exported by host
		if (null == result) {
			throw new Error("null FragmentProvider result");
		}
	}
}