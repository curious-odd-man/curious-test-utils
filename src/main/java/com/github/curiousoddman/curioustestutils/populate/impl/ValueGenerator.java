package com.github.curiousoddman.curioustestutils.populate.impl;

public interface ValueGenerator<T> {
	boolean isApplicable(ReadOnlyContext context);

	T generateValue(ReadOnlyContext context);
}
