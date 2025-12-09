package com.github.curiousoddman.curioustestutils.populate.impl;

import java.util.Deque;
import java.util.List;

public interface ReadOnlyContext {
	ValueGenerator<?>[] getCustomGenerators();

	int getSeed();

	List<String> getPath();

	Class<?> getCurrentParameterClass();

	String getCurrentMethodName();

	Class<?> getCurrentGenericType();

	Deque<PathElement> getPathElements();
}
